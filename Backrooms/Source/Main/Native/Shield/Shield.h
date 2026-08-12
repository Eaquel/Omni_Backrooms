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
//
// Being compilable was not enough on its own. A detector reads /proc, and a
// tool cannot put a Magisk mount or a frida-server socket into the machine's
// real one — so for a while these were compiled and never actually run, and
// three of them were wrong in ways that accused ordinary phones. `procRoot()`
// below is what closed that: the detectors read through it, a check stages a
// /proc on disk, and the assertions go through `RootDetector::scan()` — the
// same call the game makes.
// ============================================================================

#ifndef OMNI_SHIELD_SHIELD_H
#define OMNI_SHIELD_SHIELD_H

#include <algorithm>
#include <atomic>
#include <cctype>
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
[[nodiscard]] static std::string readSmallFile(const std::string& p) noexcept {
    int fd=::open(p.c_str(),O_RDONLY|O_CLOEXEC); if(fd<0) return {};
    char buf[8192]{}; ssize_t n=::read(fd,buf,sizeof(buf)-1); ::close(fd);
    return n>0?std::string(buf,static_cast<size_t>(n)):std::string{};
}

/**
 * Where the detectors look for /proc. `/proc` everywhere except under a test.
 *
 * The parsing below is written as pure functions so it can be asserted on, and
 * the first version of the check that came with them called those functions
 * directly. That check passed with the original bugs put back, because the
 * detectors were free to stop calling them — which is the exact shape of
 * failure this project has hit six times now: one rule in two places, and the
 * copy under test is not the copy that ships.
 *
 * With a settable root, a tool lays out a mount table and a socket table on
 * disk and runs `RootDetector::scan()` — the same call the game makes, through
 * the same file reads. Nothing can be bypassed without the check noticing.
 */
inline std::string& procRoot() noexcept { static std::string root="/proc"; return root; }
[[nodiscard]] inline std::string procPath(std::string_view rel) {
    std::string p=procRoot(); p+='/'; p.append(rel); return p;
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

// ---------------------------------------------------------------------------
// Reading a /proc file, as opposed to searching it.
//
// Three detectors below used to draw their conclusions from containsCI() over
// a whole /proc file. That is a substring search, and a substring search does
// not know what a field is — so "is an overlay mounted over /system" became
// "does the word overlay appear anywhere AND does the string /system appear
// anywhere", which are two different lines on every stock Android 11+ device,
// and "is Frida listening on 27042" became "does this four-character run of
// hex appear anywhere in a file that is nothing but hex".
//
// The fix is not a longer needle. It is parsing the field the question is
// actually about. These three are pure functions of the file's text, with no
// I/O in them, which is also what lets Native_Check.py feed them mount tables
// and socket tables copied off real devices and assert on the answers.
//
// Each takes an optional `why`: the line the verdict came from. A flag word
// tells a player their device is unauthorised and tells us nothing we can act
// on — the whole reason this round of fixes was possible is that a screenshot
// happened to carry the hex.
// ---------------------------------------------------------------------------

/** Splits `line` on runs of spaces, filling up to `want` views. Returns how
 *  many fields were found. */
static size_t splitFields(std::string_view line,std::string_view* out,size_t want) noexcept {
    size_t got=0,at=0;
    while(got<want&&at<line.size()){
        while(at<line.size()&&(line[at]==' '||line[at]=='\t')) ++at;
        const size_t s=at;
        while(at<line.size()&&line[at]!=' '&&line[at]!='\t') ++at;
        if(at>s) out[got++]=line.substr(s,at-s);
    }
    return got;
}

/**
 * True if the mount table shows something laid over the system image.
 *
 * A line in /proc/self/mounts is `source target fstype options freq passno`.
 * What makes a mount suspicious is an overlay whose *target* is a read-only
 * system partition — one line, one field, not two searches.
 *
 * The target must be the partition root exactly. A subdirectory of one is not
 * enough: /vendor/overlay is where a device keeps its runtime resource
 * overlays and is stock furniture, and the first version of this rewrite
 * flagged it, which is the same false positive again wearing better clothes.
 * On Android "/" is the system partition, so it belongs in the list.
 *
 * Bind-mounting individual files under /system is the older root technique and
 * does not show as an overlay at all; those lines carry the manager's name in
 * the source, which is the check above.
 */
[[nodiscard]] inline bool mountsShadowSystem(std::string_view mounts,std::string* why=nullptr) noexcept {
    static constexpr std::string_view kGuarded[]={
        "/","/system","/system_ext","/vendor","/product","/odm"
    };
    size_t pos=0;
    while(pos<mounts.size()){
        size_t end=mounts.find('\n',pos);
        if(end==std::string_view::npos) end=mounts.size();
        const std::string_view line=mounts.substr(pos,end-pos);
        pos=end+1;
        std::string_view f[3];
        if(splitFields(line,f,3)<3) continue;
        // A root manager naming itself in the source or the target is
        // conclusive wherever it is mounted.
        for(auto n: {"magisk","supersu","kernelsu"})
            if(containsCI(f[0],n)||containsCI(f[1],n)){
                if(why) *why=std::string(line);
                return true;
            }
        if(!containsCI(f[2],"overlay")&&!containsCI(f[0],"overlay")) continue;
        for(auto g: kGuarded)
            if(f[1]==g){ if(why) *why=std::string(line); return true; }
    }
    return false;
}

/**
 * True if the table holds a LISTENING socket on one of `ports`.
 *
 * /proc/net/tcp columns: `sl local_address rem_address st ...`, where the
 * address is `HHHHHHHH:PPPP` and st 0A is TCP_LISTEN. The port is the half
 * after the colon — searching the whole line for its hex matches inode
 * numbers, sequence numbers and remote addresses just as happily.
 */
[[nodiscard]] inline bool netTableListens(std::string_view table,const uint16_t* ports,
                                          size_t count,std::string* why=nullptr) noexcept {
    constexpr std::string_view kListen="0A";
    size_t pos=0;
    bool header=true;
    while(pos<table.size()){
        size_t end=table.find('\n',pos);
        if(end==std::string_view::npos) end=table.size();
        const std::string_view line=table.substr(pos,end-pos);
        pos=end+1;
        if(header){ header=false; continue; }   // "sl local_address rem_address st ..."
        std::string_view f[4];
        if(splitFields(line,f,4)<4) continue;
        if(f[3]!=kListen) continue;
        const size_t colon=f[1].rfind(':');
        if(colon==std::string_view::npos) continue;
        const std::string_view hex=f[1].substr(colon+1);
        if(hex.size()!=4) continue;
        uint32_t port=0;
        bool ok=true;
        for(char c: hex){
            const int d=(c>='0'&&c<='9')?c-'0':(c>='a'&&c<='f')?c-'a'+10:(c>='A'&&c<='F')?c-'A'+10:-1;
            if(d<0){ ok=false; break; }
            port=port*16+static_cast<uint32_t>(d);
        }
        if(!ok) continue;
        for(size_t i=0;i<count;++i)
            if(port==ports[i]){ if(why) *why="listening on "+std::to_string(port); return true; }
    }
    return false;
}

/** The value of TracerPid in a /proc/<pid>/status blob, or 0 when there is no
 *  such line — which is the same answer as "nothing is tracing us". */
[[nodiscard]] inline int tracerPidOf(std::string_view status) noexcept {
    const size_t pos=status.find("TracerPid:");
    if(pos==std::string_view::npos) return 0;
    std::string_view sv=status.substr(pos+10);
    while(!sv.empty()&&(sv[0]==' '||sv[0]=='\t')) sv.remove_prefix(1);
    int v=0;
    for(char c: sv){ if(c<'0'||c>'9') break; v=v*10+(c-'0'); }
    return v;
}

/** True if the process is parked in tracing-stop — a debugger has it held,
 *  rather than merely being attached. `State:` is a single letter, and `t` is
 *  the one that means stopped by a tracer. */
[[nodiscard]] inline bool inTracingStop(std::string_view status) noexcept {
    const size_t pos=status.find("State:");
    if(pos==std::string_view::npos) return false;
    std::string_view sv=status.substr(pos+6);
    while(!sv.empty()&&(sv[0]==' '||sv[0]=='\t')) sv.remove_prefix(1);
    return !sv.empty()&&sv[0]=='t';
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
        why_.clear();   // evidence belongs to this scan, not the last one
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
    /** The mount line a SHADOW_MOUNT verdict came from, empty otherwise. */
    [[nodiscard]] const std::string& why() const noexcept { return why_; }
private:
    std::string why_;
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
        return containsCI(readSmallFile(procPath("self/maps")),"magisk");
    }
    [[nodiscard]] bool ksu() noexcept { return fileExists("/data/adb/ksu")||fileExists("/data/adb/ksud")||fileExists("/data/adb/modules/.ksu"); }
    [[nodiscard]] bool zygisk() noexcept {
        auto m=readSmallFile(procPath("self/maps"));
        return containsCI(m,"zygisk")||containsCI(m,"riru")||fileExists("/data/adb/modules/.zygisk");
    }
    [[nodiscard]] bool shadowMount() noexcept {
        return mountsShadowSystem(readSmallFile(procPath("self/mounts")),&why_);
    }
};

class FridaDetector {
public:
    [[nodiscard]] uint32_t scan() noexcept {
        why_.clear();
        uint32_t f=0;
        if(fridaPort())   f|=FLAG_FRIDA_PORT;
        if(fridaMaps())   f|=FLAG_FRIDA_MAPS;
        if(fridaThread()) f|=FLAG_FRIDA_THREAD;
        if(fridaGadget()) f|=FLAG_FRIDA_GADGET;
        return f;
    }
    /** What a FRIDA_PORT verdict saw, empty otherwise. This one earns its
     *  keep more than the others: a Frida flag is CRITICAL, and CRITICAL kills
     *  the process. Nothing should be able to do that without leaving a line
     *  behind saying why. */
    [[nodiscard]] const std::string& why() const noexcept { return why_; }
private:
    std::string why_;
    /**
     * Frida's server listens on 27042 and counts up from there.
     *
     * The hex fallback that used to sit here searched /proc/net/tcp for the
     * literals "6D58", "71D4", "2717" and "5039". Those are 27992, 29140,
     * 10007 and 20537 — not Frida's ports at all, and someone had written the
     * decimal digits where the hex belonged. Worse than being wrong, they were
     * matched as substrings against a file that is almost entirely hex: on
     * this build machine's tiny 19-socket table, 220 of the 65536 possible
     * four-hex-digit needles already occur somewhere, and a phone carries many
     * times that many sockets. Sooner or later one lands in an inode number
     * and the game kills itself in a player's hands.
     */
    [[nodiscard]] bool fridaPort() noexcept {
        static constexpr uint16_t ports[]={27042,27043,27044,27045};
        for(auto p: ports)
            if(portOpen(p)){ why_="connected to loopback "+std::to_string(p); return true; }
        for(auto rel: {"net/tcp","net/tcp6"})
            if(netTableListens(readSmallFile(procPath(rel)),ports,4,&why_)){ why_=std::string(rel)+" "+why_; return true; }
        return false;
    }
    [[nodiscard]] bool fridaMaps() noexcept {
        auto m=readSmallFile(procPath("self/maps"));
        for(auto p: {"frida","gum-js-loop","frida-agent","frida-gadget","frida-server","linjector","re.frida.server","frida-helper"})
            if(containsCI(m,p)) return true;
        return false;
    }
    [[nodiscard]] bool fridaThread() noexcept {
        const std::string taskDir=procPath("self/task");
        DIR* dir=opendir(taskDir.c_str()); if(!dir) return false;
        bool found=false; struct dirent* e;
        while((e=readdir(dir))!=nullptr){
            if(e->d_name[0]=='.') continue;
            std::string path=taskDir; path+='/'; path+=e->d_name; path+="/comm";
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
    /**
     * Is a debugger attached right now?
     *
     * This used to call PTRACE_TRACEME and then PTRACE_DETACH to undo it. The
     * undo never worked: PTRACE_DETACH needs the tracee's real pid and was
     * being handed 0, so it failed with ESRCH every time and the process was
     * left traced by its parent. The next scan's TRACEME then returned EPERM —
     * "already traced" — and the detector reported a debugger. On every device,
     * about five seconds in, permanently, because the flag word is sticky.
     * That is one of the two bits behind the 0x40200 a player photographed.
     *
     * The damage was not only the false positive. A process stuck as a tracee
     * routes its signals to a tracer that never waits, so a genuine SIGSEGV
     * hangs instead of crashing and Crashlytics gets nothing.
     *
     * TracerPid is the authoritative answer to the same question and reading
     * it changes nothing about the process. Occupying the tracer slot on
     * purpose — a single TRACEME at startup, never undone — is a real
     * technique, but it buys a little friction for a debugger at the cost of
     * every future crash report, which is a bad trade for a game.
     */
    [[nodiscard]] bool ptrace_check() noexcept {
        return tracerPidOf(readSmallFile(procPath("self/status")))!=0;
    }
    /** Held by a debugger, rather than merely attached to one. Distinct from
     *  the above, which is what gives the two flags separate meanings. */
    [[nodiscard]] bool debugWait() noexcept {
        return inTracingStop(readSmallFile(procPath("self/status")));
    }
    [[nodiscard]] bool xposed() noexcept { return containsCI(readSmallFile(procPath("self/maps")),"XposedBridge")||fileExists("/system/framework/XposedBridge.jar"); }
    [[nodiscard]] bool lsposed() noexcept {
        return containsCI(readSmallFile(procPath("self/maps")),"lsposed")||
               fileExists("/data/data/org.lsposed.manager")||fileExists("/data/data/io.github.lsposed.manager");
    }
    [[nodiscard]] bool substrate() noexcept {
        for(auto lib: {"libsubstrate.so","libsubstrate-dvm.so","libCydiaSubstrate.so"}){
            void* h=dlopen(lib,RTLD_NOLOAD); if(h){ dlclose(h); return true; }
        }
        return containsCI(readSmallFile(procPath("self/maps")),"substrate");
    }
    [[nodiscard]] bool procStatus() noexcept { auto s=readSmallFile(procPath("self/status")); return s.empty()||!containsCI(s,"Name:"); }
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
    [[nodiscard]] bool cpuInfo() noexcept { auto c=readSmallFile(procPath("cpuinfo")); return containsCI(c,"goldfish")||containsCI(c,"ranchu"); }
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
