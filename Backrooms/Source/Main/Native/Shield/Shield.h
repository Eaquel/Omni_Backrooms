// ============================================================================
// Shield — everything that defends the build.
//
// Two jobs that used to live apart and belong together:
//
//   * DETECTION. Root, Frida, debuggers, hooking frameworks, emulators,
//     signature mismatch. Twenty-four flags, scanned once at start and again on
//     a monitor thread.
//   * DISGUISE. The Unity costume — see Unity.cpp, which is blunt about being a
//     filter on the front door rather than protection.
//
// They were split between the middle of Engine.cpp and a folder called
// Disguise/, which meant neither could be compiled without the NDK and neither
// had ever been run by a tool. Everything here except the signature check is
// plain POSIX, so it now builds on a host and Native_Check.py compiles it with
// warnings as errors like every other module.
//
// The signature check is the exception and stays in Engine.cpp: it calls into
// PackageManager through JNI, so there is no version of it that runs without a
// JVM. Keeping it there rather than pretending otherwise is the honest split.
// ============================================================================

#ifndef OMNI_SHIELD_SHIELD_H
#define OMNI_SHIELD_SHIELD_H

#include <atomic>
#include <cstdarg>
#include <cstdint>
#include <cstring>
#include <dirent.h>
#include <fcntl.h>
#include <functional>
#include <mutex>
#include <string>
#include <string_view>
#include <sys/prctl.h>
#include <sys/ptrace.h>
#include <sys/resource.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <thread>
#include <unistd.h>
#include <vector>

#include <arpa/inet.h>
#include <dlfcn.h>
#include <netinet/in.h>

namespace omni::shield {

// The only two things in here that are not portable, declared so everything
// below them is. Defined in Shield.cpp behind __ANDROID__, with host versions
// beside them — which is what lets the detectors be compiled and read on a
// machine that is not a phone.

/** One Android system property, or "" off-device. */
[[nodiscard]] std::string sysProp(const char* key) noexcept;
/** Logcat on device, stderr off it. */
void shieldLog(const char* fmt, ...) noexcept;

constexpr uint32_t
    FLAG_ROOT_BINARY=1u<<0,  FLAG_ROOT_PROPS=1u<<1,  FLAG_ROOT_PATHS=1u<<2,
    FLAG_SELINUX_OFF=1u<<3,  FLAG_MAGISK=1u<<4,      FLAG_FRIDA_PORT=1u<<5,
    FLAG_FRIDA_MAPS=1u<<6,   FLAG_FRIDA_THREAD=1u<<7,FLAG_FRIDA_GADGET=1u<<8,
    FLAG_PTRACE_TRACED=1u<<9,FLAG_DEBUG_WAIT=1u<<10, FLAG_EMULATOR_PROPS=1u<<11,
    FLAG_EMULATOR_HW=1u<<12, FLAG_EMULATOR_CPU=1u<<13,FLAG_SIG_MISMATCH=1u<<14,
    FLAG_MAPS_TAMPER=1u<<15, FLAG_XPOSED=1u<<16,     FLAG_SUBSTRATE=1u<<17,
    FLAG_SHADOW_MOUNT=1u<<18,FLAG_PROC_TAMPER=1u<<19,FLAG_HOOK_INLINE=1u<<20,
    FLAG_ZYGISK=1u<<21,      FLAG_LSPOSED=1u<<22,    FLAG_KSU=1u<<23;

[[nodiscard]] static bool fileExists(std::string_view p) noexcept { struct stat st{}; return ::stat(p.data(),&st)==0; }
[[nodiscard]] static std::string readSmallFile(std::string_view p) noexcept {
    int fd=::open(p.data(),O_RDONLY|O_CLOEXEC); if(fd<0) return {};
    char buf[8192]{}; ssize_t n=::read(fd,buf,sizeof(buf)-1); ::close(fd);
    return n>0?std::string(buf,static_cast<size_t>(n)):std::string{};
}
[[nodiscard]] static bool containsCI(std::string_view hay,std::string_view needle) noexcept {
    if(needle.size()>hay.size()) return false;
    return std::search(hay.begin(),hay.end(),needle.begin(),needle.end(),
        [](char a,char b){ return std::tolower((unsigned char)a)==std::tolower((unsigned char)b); })!=hay.end();
}
[[nodiscard]] static bool portOpen(uint16_t port) noexcept {
    int fd=::socket(AF_INET,SOCK_STREAM|SOCK_CLOEXEC,0); if(fd<0) return false;
    struct timeval tv{0,80'000};
    ::setsockopt(fd,SOL_SOCKET,SO_RCVTIMEO,&tv,sizeof(tv));
    ::setsockopt(fd,SOL_SOCKET,SO_SNDTIMEO,&tv,sizeof(tv));
    sockaddr_in addr{}; addr.sin_family=AF_INET;
    addr.sin_port=htons(port); addr.sin_addr.s_addr=htonl(INADDR_LOOPBACK);
    bool ok=(::connect(fd,reinterpret_cast<sockaddr*>(&addr),sizeof(addr))==0);
    ::close(fd); return ok;
}

inline std::string sha256Hex(const uint8_t* data,size_t len) noexcept {
    static constexpr uint32_t K[64]={
        0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
        0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
        0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
        0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
        0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
        0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
        0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
        0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2
    };
    uint32_t h[8]={0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,0x510e527f,0x9b05688c,0x1f83d9ab,0x5be0cd19};
    auto ror32=[](uint32_t x,int n)->uint32_t{ return (x>>n)|(x<<(32-n)); };
    auto processBlock=[&](const uint8_t* blk){
        uint32_t w[64];
        for(int i=0;i<16;++i) w[i]=((uint32_t)blk[i*4]<<24)|((uint32_t)blk[i*4+1]<<16)|((uint32_t)blk[i*4+2]<<8)|(uint32_t)blk[i*4+3];
        for(int i=16;i<64;++i){ uint32_t s0=ror32(w[i-15],7)^ror32(w[i-15],18)^(w[i-15]>>3); uint32_t s1=ror32(w[i-2],17)^ror32(w[i-2],19)^(w[i-2]>>10); w[i]=w[i-16]+s0+w[i-7]+s1; }
        uint32_t a=h[0],b=h[1],c=h[2],d=h[3],e=h[4],f=h[5],g=h[6],hh=h[7];
        for(int i=0;i<64;++i){
            uint32_t S1=ror32(e,6)^ror32(e,11)^ror32(e,25),ch=(e&f)^((~e)&g),t1=hh+S1+ch+K[i]+w[i];
            uint32_t S0=ror32(a,2)^ror32(a,13)^ror32(a,22),maj=(a&b)^(a&c)^(b&c),t2=S0+maj;
            hh=g;g=f;f=e;e=d+t1;d=c;c=b;b=a;a=t1+t2;
        }
        h[0]+=a;h[1]+=b;h[2]+=c;h[3]+=d;h[4]+=e;h[5]+=f;h[6]+=g;h[7]+=hh;
    };
    size_t tb=(len+8)/64+1;
    std::vector<uint8_t> padded(tb*64,0);
    std::memcpy(padded.data(),data,len); padded[len]=0x80;
    uint64_t bl=(uint64_t)len*8;
    for(int i=0;i<8;++i) padded[padded.size()-8+i]=(uint8_t)(bl>>(56-i*8));
    for(size_t i=0;i<tb;++i) processBlock(padded.data()+i*64);
    char hex[65]; for(int i=0;i<8;++i) snprintf(hex+i*8,9,"%08x",h[i]);
    return std::string(hex,64);
}

class RootDetector {
public:
    [[nodiscard]] uint32_t scan() noexcept {
        uint32_t f=0;
        if(rootBinaries())   f|=FLAG_ROOT_BINARY;
        if(rootProperties()) f|=FLAG_ROOT_PROPS;
        if(rootPaths())      f|=FLAG_ROOT_PATHS;
        if(selinuxOff())     f|=FLAG_SELINUX_OFF;
        if(magisk())         f|=FLAG_MAGISK;
        if(shadowMount())    f|=FLAG_SHADOW_MOUNT;
        if(ksu())            f|=FLAG_KSU;
        if(zygisk())         f|=FLAG_ZYGISK;
        return f;
    }
private:
    [[nodiscard]] bool rootBinaries() noexcept {
        static constexpr std::string_view bins[]={
            "/sbin/su","/system/bin/su","/system/xbin/su","/system/sbin/su","/vendor/bin/su",
            "/su/bin/su","/data/local/su","/data/local/bin/su","/data/local/xbin/su",
            "/system/bin/.ext/.su","/system/xbin/busybox","/system/bin/busybox",
            "/data/adb/magisk","/sbin/.magisk","/sbin/.core/mirror"
        };
        for(auto b: bins) if(fileExists(b)) return true;
        return false;
    }
    [[nodiscard]] bool rootProperties() noexcept {
        if(sysProp("ro.debuggable")=="1") return true;
        if(sysProp("ro.secure")=="0")     return true;
        if(containsCI(sysProp("ro.build.tags"),"test-keys"))  return true;
        if(containsCI(sysProp("ro.build.type"),"userdebug"))  return true;
        return false;
    }
    [[nodiscard]] bool rootPaths() noexcept {
        static constexpr std::string_view paths[]={
            "/system/app/SuperSU.apk","/system/app/Superuser.apk","/system/app/KingoUser.apk",
            "/data/data/com.topjohnwu.magisk","/data/data/eu.chainfire.supersu","/data/data/me.weishu.kernelsu"
        };
        for(auto p: paths) if(fileExists(p)) return true;
        return false;
    }
    [[nodiscard]] bool selinuxOff() noexcept { auto c=readSmallFile("/sys/fs/selinux/enforce"); return !c.empty()&&c[0]=='0'; }
    [[nodiscard]] bool magisk() noexcept {
        static constexpr std::string_view mp[]={"/sbin/.magisk","/dev/.magisk","/data/adb/magisk","/data/adb/magisk.img","/sbin/magisk","/dev/magisk"};
        for(auto p: mp) if(fileExists(p)) return true;
        return containsCI(readSmallFile("/proc/self/maps"),"magisk");
    }
    [[nodiscard]] bool ksu() noexcept { return fileExists("/data/adb/ksu")||fileExists("/data/adb/ksud")||fileExists("/data/adb/modules/.ksu"); }
    [[nodiscard]] bool zygisk() noexcept {
        auto m=readSmallFile("/proc/self/maps");
        return containsCI(m,"zygisk")||containsCI(m,"riru")||fileExists("/data/adb/modules/.zygisk");
    }
    [[nodiscard]] bool shadowMount() noexcept {
        auto m=readSmallFile("/proc/self/mounts");
        if(containsCI(m,"magisk")||containsCI(m,"supersu")) return true;
        return containsCI(m,"overlay")&&containsCI(m,"/system");
    }
};

class FridaDetector {
public:
    [[nodiscard]] uint32_t scan() noexcept {
        uint32_t f=0;
        if(fridaPort())   f|=FLAG_FRIDA_PORT;
        if(fridaMaps())   f|=FLAG_FRIDA_MAPS;
        if(fridaThread()) f|=FLAG_FRIDA_THREAD;
        if(fridaGadget()) f|=FLAG_FRIDA_GADGET;
        return f;
    }
private:
    [[nodiscard]] bool fridaPort() noexcept {
        static constexpr uint16_t ports[]={27042,27043,27044,27045};
        for(auto p: ports) if(portOpen(p)) return true;
        auto tcp=readSmallFile("/proc/net/tcp"),tcp6=readSmallFile("/proc/net/tcp6");
        for(auto h: {"6D58","71D4","2717","5039"}) if(containsCI(tcp,h)||containsCI(tcp6,h)) return true;
        return false;
    }
    [[nodiscard]] bool fridaMaps() noexcept {
        auto m=readSmallFile("/proc/self/maps");
        for(auto p: {"frida","gum-js-loop","frida-agent","frida-gadget","frida-server","linjector","re.frida.server","frida-helper"})
            if(containsCI(m,p)) return true;
        return false;
    }
    [[nodiscard]] bool fridaThread() noexcept {
        DIR* dir=opendir("/proc/self/task"); if(!dir) return false;
        bool found=false; struct dirent* e;
        while((e=readdir(dir))!=nullptr){
            if(e->d_name[0]=='.') continue;
            std::string path="/proc/self/task/"; path+=e->d_name; path+="/comm";
            auto comm=readSmallFile(path);
            if(containsCI(comm,"gum-js-loop")||containsCI(comm,"frida")||containsCI(comm,"gmain")){ found=true; break; }
        }
        closedir(dir); return found;
    }
    [[nodiscard]] bool fridaGadget() noexcept {
        for(auto lib: {"libfrida-gadget.so","re.frida.server","libgadget.so"}){
            void* h=dlopen(lib,RTLD_NOLOAD); if(h){ dlclose(h); return true; }
        }
        return false;
    }
};

class DebugDetector {
public:
    [[nodiscard]] uint32_t scan() noexcept {
        uint32_t f=0;
        if(ptrace_check()) f|=FLAG_PTRACE_TRACED;
        if(debugWait())    f|=FLAG_DEBUG_WAIT;
        if(xposed())       f|=FLAG_XPOSED;
        if(lsposed())      f|=FLAG_LSPOSED;
        if(substrate())    f|=FLAG_SUBSTRATE;
        if(procStatus())   f|=FLAG_PROC_TAMPER;
        if(inlineHook())   f|=FLAG_HOOK_INLINE;
        return f;
    }
private:
    [[nodiscard]] bool ptrace_check() noexcept {
        if(ptrace(PTRACE_TRACEME,0,nullptr,nullptr)==-1) return true;
        ptrace(PTRACE_DETACH,0,nullptr,nullptr); return false;
    }
    [[nodiscard]] bool debugWait() noexcept {
        auto s=readSmallFile("/proc/self/status");
        auto pos=s.find("TracerPid:"); if(pos==std::string::npos) return false;
        std::string_view sv(s); sv=sv.substr(pos+10);
        while(!sv.empty()&&(sv[0]==' '||sv[0]=='\t')) sv.remove_prefix(1);
        return !sv.empty()&&sv[0]!='0';
    }
    [[nodiscard]] bool xposed() noexcept { return containsCI(readSmallFile("/proc/self/maps"),"XposedBridge")||fileExists("/system/framework/XposedBridge.jar"); }
    [[nodiscard]] bool lsposed() noexcept {
        return containsCI(readSmallFile("/proc/self/maps"),"lsposed")||
               fileExists("/data/data/org.lsposed.manager")||fileExists("/data/data/io.github.lsposed.manager");
    }
    [[nodiscard]] bool substrate() noexcept {
        for(auto lib: {"libsubstrate.so","libsubstrate-dvm.so","libCydiaSubstrate.so"}){
            void* h=dlopen(lib,RTLD_NOLOAD); if(h){ dlclose(h); return true; }
        }
        return containsCI(readSmallFile("/proc/self/maps"),"substrate");
    }
    [[nodiscard]] bool procStatus() noexcept { auto s=readSmallFile("/proc/self/status"); return s.empty()||!containsCI(s,"Name:"); }
    [[nodiscard]] bool inlineHook() noexcept {
        for(auto lib: {"libdobby.so","libsandHook.so","libwhale.so","libAndHook.so","libepic.so","libreactivehole.so"}){
            void* h=dlopen(lib,RTLD_NOLOAD); if(h){ dlclose(h); return true; }
        }
        return false;
    }
};

class EmulatorDetector {
public:
    [[nodiscard]] uint32_t scan() noexcept {
        uint32_t f=0;
        if(emuProps()) f|=FLAG_EMULATOR_PROPS;
        if(emuHw())    f|=FLAG_EMULATOR_HW;
        if(cpuInfo())  f|=FLAG_EMULATOR_CPU;
        return f;
    }
private:
    [[nodiscard]] bool emuProps() noexcept {
        static constexpr const char* props[][2]={
            {"ro.hardware","goldfish"},{"ro.hardware","ranchu"},{"ro.product.model","sdk"},
            {"ro.product.device","generic"},{"ro.kernel.qemu","1"},
            {"ro.product.manufacturer","unknown"},{"ro.build.product","generic"}
        };
        for(auto& [k,v]: props) if(containsCI(sysProp(k),v)) return true;
        return false;
    }
    [[nodiscard]] bool emuHw() noexcept { for(auto f: {"/dev/socket/qemud","/dev/qemu_pipe","/sys/qemu_trace"}) if(fileExists(f)) return true; return false; }
    [[nodiscard]] bool cpuInfo() noexcept { auto c=readSmallFile("/proc/cpuinfo"); return containsCI(c,"goldfish")||containsCI(c,"ranchu"); }
};


class AntiTamperMonitor {
public:
    void start() {
        if(running_.load(std::memory_order_acquire)) return;
        running_.store(true,std::memory_order_release);
        thread_=std::thread(&AntiTamperMonitor::loop,this);
    }
    void stop() {
        running_.store(false,std::memory_order_release);
        if(thread_.joinable()) thread_.join();
    }
    void setCallback(std::function<void(uint32_t)> cb) { std::lock_guard lk(mtx_); cb_=std::move(cb); }
private:
    void loop() {
        prctl(PR_SET_NAME,"omni_guard_wt",0,0,0);
        setpriority(PRIO_PROCESS,0,10);
        RootDetector root; FridaDetector frida; DebugDetector debug; EmulatorDetector emu;
        int cycle=0;
        while(running_.load(std::memory_order_acquire)){
            uint32_t detected=0;
            detected|=frida.scan();
            if(cycle%2==0)  detected|=debug.scan();
            if(cycle%5==0)  detected|=root.scan();
            if(cycle%15==0) detected|=emu.scan();
            if(detected!=0){
                uint32_t prev=flags_.fetch_or(detected,std::memory_order_acq_rel);
                if((prev|detected)!=prev){ std::lock_guard lk(mtx_); if(cb_) cb_(detected); }
            }
            ++cycle;
            std::this_thread::sleep_for(std::chrono::milliseconds(1200));
        }
    }
    std::atomic<bool>     running_{false};
    std::atomic<uint32_t> flags_  {0};
    std::thread           thread_;
    std::mutex            mtx_;
    std::function<void(uint32_t)> cb_;
};

struct GuardState {
    RootDetector                       root;
    FridaDetector                      frida;
    DebugDetector                      debug;
    EmulatorDetector                   emulator;
    AntiTamperMonitor                  monitor;
    std::atomic<uint32_t>              cachedFlags{0};
    std::atomic<bool>                  initialized{false};
};

} // namespace omni::shield

#endif // OMNI_SHIELD_SHIELD_H
