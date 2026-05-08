#include <jni.h>
#include <android/log.h>
#include <arpa/inet.h>
#include <atomic>
#include <chrono>
#include <cstdint>
#include <cstring>
#include <dirent.h>
#include <dlfcn.h>
#include <fcntl.h>
#include <functional>
#include <memory>
#include <mutex>
#include <netinet/in.h>
#include <optional>
#include <ranges>
#include <span>
#include <string>
#include <string_view>
#include <sys/prctl.h>
#include <sys/ptrace.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/system_properties.h>
#include <thread>
#include <unistd.h>
#include <vector>
#include <linux/prctl.h>

#define TAG  "OmniGuard"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  TAG, __VA_ARGS__)

namespace omni::guard {

constexpr uint32_t FLAG_ROOT_BINARY    = 1u <<  0;
constexpr uint32_t FLAG_ROOT_PROPS     = 1u <<  1;
constexpr uint32_t FLAG_ROOT_PATHS     = 1u <<  2;
constexpr uint32_t FLAG_SELINUX_OFF    = 1u <<  3;
constexpr uint32_t FLAG_MAGISK         = 1u <<  4;
constexpr uint32_t FLAG_FRIDA_PORT     = 1u <<  5;
constexpr uint32_t FLAG_FRIDA_MAPS     = 1u <<  6;
constexpr uint32_t FLAG_FRIDA_THREAD   = 1u <<  7;
constexpr uint32_t FLAG_FRIDA_GADGET   = 1u <<  8;
constexpr uint32_t FLAG_PTRACE_TRACED  = 1u <<  9;
constexpr uint32_t FLAG_DEBUG_WAIT     = 1u << 10;
constexpr uint32_t FLAG_EMULATOR_PROPS = 1u << 11;
constexpr uint32_t FLAG_EMULATOR_HW    = 1u << 12;
constexpr uint32_t FLAG_EMULATOR_CPU   = 1u << 13;
constexpr uint32_t FLAG_SIG_MISMATCH   = 1u << 14;
constexpr uint32_t FLAG_MAPS_TAMPER    = 1u << 15;
constexpr uint32_t FLAG_XPOSED         = 1u << 16;
constexpr uint32_t FLAG_SUBSTRATE      = 1u << 17;
constexpr uint32_t FLAG_SHADOW_MOUNT   = 1u << 18;
constexpr uint32_t FLAG_PROC_TAMPER    = 1u << 19;
constexpr uint32_t FLAG_HOOK_INLINE    = 1u << 20;
constexpr uint32_t FLAG_ZYGISK         = 1u << 21;
constexpr uint32_t FLAG_LSPOSED        = 1u << 22;
constexpr uint32_t FLAG_KSU            = 1u << 23;

[[nodiscard]] static bool fileExists(std::string_view path) noexcept {
    struct stat st{};
    return ::stat(path.data(), &st) == 0;
}

[[nodiscard]] static std::string readSmallFile(std::string_view path) noexcept {
    int fd = ::open(path.data(), O_RDONLY | O_CLOEXEC);
    if (fd < 0) return {};
    char buf[8192]{};
    ssize_t n = ::read(fd, buf, sizeof(buf) - 1);
    ::close(fd);
    if (n <= 0) return {};
    return std::string(buf, static_cast<size_t>(n));
}

[[nodiscard]] static std::string getSysProp(const char* key) noexcept {
    char val[PROP_VALUE_MAX]{};
    __system_property_get(key, val);
    return std::string(val);
}

[[nodiscard]] static bool containsCI(std::string_view hay, std::string_view needle) noexcept {
    if (needle.size() > hay.size()) return false;
    return std::search(hay.begin(), hay.end(), needle.begin(), needle.end(),
        [](char a, char b) { return std::tolower((unsigned char)a) == std::tolower((unsigned char)b); }
    ) != hay.end();
}

[[nodiscard]] static bool portOpen(uint16_t port) noexcept {
    int fd = ::socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
    if (fd < 0) return false;
    struct timeval tv{ 0, 80'000 };
    ::setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
    ::setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof(tv));
    sockaddr_in addr{};
    addr.sin_family      = AF_INET;
    addr.sin_port        = htons(port);
    addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    bool ok = (::connect(fd, reinterpret_cast<sockaddr*>(&addr), sizeof(addr)) == 0);
    ::close(fd);
    return ok;
}

static std::string sha256Hex(const uint8_t* data, size_t len) noexcept {
    static constexpr uint32_t K[64] = {
        0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
        0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
        0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
        0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
        0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
        0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
        0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
        0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2
    };
    uint32_t h[8] = {
        0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,
        0x510e527f,0x9b05688c,0x1f83d9ab,0x5be0cd19
    };

    auto ror32 = [](uint32_t x, int n) -> uint32_t {
        return (x >> n) | (x << (32 - n));
    };
    auto processBlock = [&](const uint8_t* blk) {
        uint32_t w[64];
        for (int i = 0; i < 16; ++i)
            w[i] = ((uint32_t)blk[i*4]<<24)|((uint32_t)blk[i*4+1]<<16)|
                   ((uint32_t)blk[i*4+2]<<8)|(uint32_t)blk[i*4+3];
        for (int i = 16; i < 64; ++i) {
            uint32_t s0 = ror32(w[i-15],7)^ror32(w[i-15],18)^(w[i-15]>>3);
            uint32_t s1 = ror32(w[i-2],17)^ror32(w[i-2],19)^(w[i-2]>>10);
            w[i] = w[i-16]+s0+w[i-7]+s1;
        }
        uint32_t a=h[0],b=h[1],c=h[2],d=h[3],e=h[4],f=h[5],g=h[6],hh=h[7];
        for (int i = 0; i < 64; ++i) {
            uint32_t S1  = ror32(e,6)^ror32(e,11)^ror32(e,25);
            uint32_t ch  = (e&f)^((~e)&g);
            uint32_t t1  = hh+S1+ch+K[i]+w[i];
            uint32_t S0  = ror32(a,2)^ror32(a,13)^ror32(a,22);
            uint32_t maj = (a&b)^(a&c)^(b&c);
            uint32_t t2  = S0+maj;
            hh=g; g=f; f=e; e=d+t1; d=c; c=b; b=a; a=t1+t2;
        }
        h[0]+=a; h[1]+=b; h[2]+=c; h[3]+=d;
        h[4]+=e; h[5]+=f; h[6]+=g; h[7]+=hh;
    };

    size_t totalBlocks = (len + 8) / 64 + 1;
    std::vector<uint8_t> padded(totalBlocks * 64, 0);
    std::memcpy(padded.data(), data, len);
    padded[len] = 0x80;
    uint64_t bitLen = (uint64_t)len * 8;
    for (int i = 0; i < 8; ++i)
        padded[padded.size()-8+i] = (uint8_t)(bitLen >> (56 - i*8));
    for (size_t i = 0; i < totalBlocks; ++i)
        processBlock(padded.data() + i*64);

    char hex[65];
    for (int i = 0; i < 8; ++i)
        snprintf(hex+i*8, 9, "%08x", h[i]);
    return std::string(hex, 64);
}

class RootDetector {
public:
    [[nodiscard]] uint32_t scan() noexcept {
        uint32_t flags = 0;
        if (rootBinaries())   flags |= FLAG_ROOT_BINARY;
        if (rootProperties()) flags |= FLAG_ROOT_PROPS;
        if (rootPaths())      flags |= FLAG_ROOT_PATHS;
        if (selinuxOff())     flags |= FLAG_SELINUX_OFF;
        if (magisk())         flags |= FLAG_MAGISK;
        if (shadowMount())    flags |= FLAG_SHADOW_MOUNT;
        if (ksu())            flags |= FLAG_KSU;
        if (zygisk())         flags |= FLAG_ZYGISK;
        return flags;
    }
private:
    [[nodiscard]] bool rootBinaries() noexcept {
        static constexpr std::string_view bins[] = {
            "/sbin/su","/system/bin/su","/system/xbin/su",
            "/system/sbin/su","/vendor/bin/su","/su/bin/su",
            "/data/local/su","/data/local/bin/su","/data/local/xbin/su",
            "/system/bin/.ext/.su","/system/xbin/busybox","/system/bin/busybox",
            "/data/adb/magisk","/sbin/.magisk","/sbin/.core/mirror",
        };
        for (auto b : bins) if (fileExists(b)) return true;
        return false;
    }
    [[nodiscard]] bool rootProperties() noexcept {
        if (getSysProp("ro.debuggable") == "1") return true;
        if (getSysProp("ro.secure")     == "0") return true;
        if (containsCI(getSysProp("ro.build.tags"), "test-keys"))  return true;
        if (containsCI(getSysProp("ro.build.type"), "userdebug"))  return true;
        return false;
    }
    [[nodiscard]] bool rootPaths() noexcept {
        static constexpr std::string_view paths[] = {
            "/system/app/SuperSU.apk","/system/app/Superuser.apk",
            "/system/app/KingoUser.apk","/data/data/com.topjohnwu.magisk",
            "/data/data/eu.chainfire.supersu","/data/data/me.weishu.kernelsu",
        };
        for (auto p : paths) if (fileExists(p)) return true;
        return false;
    }
    [[nodiscard]] bool selinuxOff() noexcept {
        auto c = readSmallFile("/sys/fs/selinux/enforce");
        return !c.empty() && c[0] == '0';
    }
    [[nodiscard]] bool magisk() noexcept {
        static constexpr std::string_view mpaths[] = {
            "/sbin/.magisk","/dev/.magisk","/data/adb/magisk",
            "/data/adb/magisk.img","/sbin/magisk","/dev/magisk",
        };
        for (auto p : mpaths) if (fileExists(p)) return true;
        return containsCI(readSmallFile("/proc/self/maps"), "magisk");
    }
    [[nodiscard]] bool ksu() noexcept {
        return fileExists("/data/adb/ksu") ||
               fileExists("/data/adb/ksud") ||
               fileExists("/data/adb/modules/.ksu");
    }
    [[nodiscard]] bool zygisk() noexcept {
        auto maps = readSmallFile("/proc/self/maps");
        return containsCI(maps, "zygisk") || containsCI(maps, "riru") ||
               fileExists("/data/adb/modules/.zygisk");
    }
    [[nodiscard]] bool shadowMount() noexcept {
        auto mounts = readSmallFile("/proc/self/mounts");
        if (containsCI(mounts, "magisk"))  return true;
        if (containsCI(mounts, "supersu")) return true;
        return containsCI(mounts, "overlay") && containsCI(mounts, "/system");
    }
};

class FridaDetector {
public:
    [[nodiscard]] uint32_t scan() noexcept {
        uint32_t flags = 0;
        if (fridaPort())   flags |= FLAG_FRIDA_PORT;
        if (fridaMaps())   flags |= FLAG_FRIDA_MAPS;
        if (fridaThread()) flags |= FLAG_FRIDA_THREAD;
        if (fridaGadget()) flags |= FLAG_FRIDA_GADGET;
        return flags;
    }
private:
    [[nodiscard]] bool fridaPort() noexcept {
        static constexpr uint16_t ports[] = { 27042, 27043, 27044, 27045 };
        for (auto p : ports) if (portOpen(p)) return true;
        auto tcp  = readSmallFile("/proc/net/tcp");
        auto tcp6 = readSmallFile("/proc/net/tcp6");
        for (auto h : { "6D58", "71D4", "2717", "5039" }) {
            if (containsCI(tcp, h) || containsCI(tcp6, h)) return true;
        }
        return false;
    }
    [[nodiscard]] bool fridaMaps() noexcept {
        auto maps = readSmallFile("/proc/self/maps");
        for (auto p : { "frida","gum-js-loop","frida-agent",
                        "frida-gadget","frida-server","linjector",
                        "re.frida.server","frida-helper" })
            if (containsCI(maps, p)) return true;
        return false;
    }
    [[nodiscard]] bool fridaThread() noexcept {
        DIR* dir = opendir("/proc/self/task");
        if (!dir) return false;
        bool found = false;
        struct dirent* e;
        while ((e = readdir(dir)) != nullptr) {
            if (e->d_name[0] == '.') continue;
            std::string path = "/proc/self/task/";
            path += e->d_name;
            path += "/comm";
            auto comm = readSmallFile(path);
            if (containsCI(comm,"gum-js-loop") || containsCI(comm,"frida") ||
                containsCI(comm,"gmain")) { found = true; break; }
        }
        closedir(dir);
        return found;
    }
    [[nodiscard]] bool fridaGadget() noexcept {
        for (auto lib : { "libfrida-gadget.so","re.frida.server","libgadget.so" }) {
            void* h = dlopen(lib, RTLD_NOLOAD);
            if (h) { dlclose(h); return true; }
        }
        return false;
    }
};

class DebugDetector {
public:
    [[nodiscard]] uint32_t scan() noexcept {
        uint32_t flags = 0;
        if (ptrace_check())    flags |= FLAG_PTRACE_TRACED;
        if (debugWait())       flags |= FLAG_DEBUG_WAIT;
        if (xposed())          flags |= FLAG_XPOSED;
        if (lsposed())         flags |= FLAG_LSPOSED;
        if (substrate())       flags |= FLAG_SUBSTRATE;
        if (procStatus())      flags |= FLAG_PROC_TAMPER;
        if (inlineHook())      flags |= FLAG_HOOK_INLINE;
        return flags;
    }
private:
    [[nodiscard]] bool ptrace_check() noexcept {
        if (ptrace(PTRACE_TRACEME, 0, nullptr, nullptr) == -1) return true;
        ptrace(PTRACE_DETACH, 0, nullptr, nullptr);
        return false;
    }
    [[nodiscard]] bool debugWait() noexcept {
        auto status = readSmallFile("/proc/self/status");
        auto pos = status.find("TracerPid:");
        if (pos == std::string::npos) return false;
        std::string_view sv(status); sv = sv.substr(pos + 10);
        while (!sv.empty() && (sv[0]==' '||sv[0]=='\t')) sv.remove_prefix(1);
        return !sv.empty() && sv[0] != '0';
    }
    [[nodiscard]] bool xposed() noexcept {
        return containsCI(readSmallFile("/proc/self/maps"), "XposedBridge") ||
               fileExists("/system/framework/XposedBridge.jar");
    }
    [[nodiscard]] bool lsposed() noexcept {
        return containsCI(readSmallFile("/proc/self/maps"), "lsposed") ||
               fileExists("/data/data/org.lsposed.manager") ||
               fileExists("/data/data/io.github.lsposed.manager");
    }
    [[nodiscard]] bool substrate() noexcept {
        for (auto lib : { "libsubstrate.so","libsubstrate-dvm.so","libCydiaSubstrate.so" }) {
            void* h = dlopen(lib, RTLD_NOLOAD);
            if (h) { dlclose(h); return true; }
        }
        return containsCI(readSmallFile("/proc/self/maps"), "substrate");
    }
    [[nodiscard]] bool procStatus() noexcept {
        auto s = readSmallFile("/proc/self/status");
        return s.empty() || !containsCI(s, "Name:");
    }
    [[nodiscard]] bool inlineHook() noexcept {
        for (auto lib : { "libdobby.so","libsandHook.so","libwhale.so",
                          "libAndHook.so","libepic.so","libreactivehole.so" }) {
            void* h = dlopen(lib, RTLD_NOLOAD);
            if (h) { dlclose(h); return true; }
        }
        return false;
    }
};

class EmulatorDetector {
public:
    [[nodiscard]] uint32_t scan() noexcept {
        uint32_t flags = 0;
        if (emuProps()) flags |= FLAG_EMULATOR_PROPS;
        if (emuHw())    flags |= FLAG_EMULATOR_HW;
        if (cpuInfo())  flags |= FLAG_EMULATOR_CPU;
        return flags;
    }
private:
    [[nodiscard]] bool emuProps() noexcept {
        static constexpr const char* props[][2] = {
            {"ro.hardware","goldfish"},{"ro.hardware","ranchu"},
            {"ro.product.model","sdk"},{"ro.product.device","generic"},
            {"ro.kernel.qemu","1"},{"ro.product.manufacturer","unknown"},
            {"ro.build.product","generic"}
        };
        for (auto& [k,v] : props)
            if (containsCI(getSysProp(k), v)) return true;
        return false;
    }
    [[nodiscard]] bool emuHw() noexcept {
        for (auto f : { "/dev/socket/qemud","/dev/qemu_pipe","/sys/qemu_trace" })
            if (fileExists(f)) return true;
        return false;
    }
    [[nodiscard]] bool cpuInfo() noexcept {
        auto c = readSmallFile("/proc/cpuinfo");
        return containsCI(c,"goldfish") || containsCI(c,"ranchu");
    }
};

class SignatureVerifier {
public:
    explicit SignatureVerifier(std::string expectedHash)
        : expected_(std::move(expectedHash)) {}

    [[nodiscard]] bool verify(JNIEnv* env, jobject ctx) noexcept {
        if (!env || !ctx || expected_.empty()) return true;

        jclass ctxCls      = env->GetObjectClass(ctx);
        jmethodID getPm    = env->GetMethodID(ctxCls,"getPackageManager","()Landroid/content/pm/PackageManager;");
        jmethodID getPkg   = env->GetMethodID(ctxCls,"getPackageName","()Ljava/lang/String;");
        if (!getPm || !getPkg) return false;

        jobject pm  = env->CallObjectMethod(ctx, getPm);
        auto    pkg = static_cast<jstring>(env->CallObjectMethod(ctx, getPkg));
        if (!pm || !pkg) return false;

        jclass  pmCls      = env->GetObjectClass(pm);
        jmethodID getInfo  = env->GetMethodID(pmCls,"getPackageInfo",
            "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
        if (!getInfo) return false;

        jobject info = env->CallObjectMethod(pm, getInfo, pkg, (jint)0x40);
        if (!info) return false;

        jclass  piCls = env->GetObjectClass(info);
        jfieldID fid  = env->GetFieldID(piCls,"signatures","[Landroid/content/pm/Signature;");
        if (!fid) return false;

        auto arr = static_cast<jobjectArray>(env->GetObjectField(info, fid));
        if (!arr || env->GetArrayLength(arr) == 0) return false;

        jobject sig0    = env->GetObjectArrayElement(arr, 0);
        jclass  sigCls  = env->GetObjectClass(sig0);
        jmethodID toB   = env->GetMethodID(sigCls,"toByteArray","()[B");
        if (!toB) return false;

        auto bytes = static_cast<jbyteArray>(env->CallObjectMethod(sig0, toB));
        if (!bytes) return false;

        jsize len = env->GetArrayLength(bytes);
        std::vector<uint8_t> buf(static_cast<size_t>(len));
        env->GetByteArrayRegion(bytes, 0, len, reinterpret_cast<jbyte*>(buf.data()));

        return sha256Hex(buf.data(), buf.size()) == expected_;
    }
private:
    std::string expected_;
};

class AntiTamperMonitor {
public:
    void start() {
        running_.store(true, std::memory_order_release);
        thread_ = std::thread([this]{ loop(); });
    }
    void stop() {
        running_.store(false, std::memory_order_release);
        if (thread_.joinable()) thread_.join();
    }
    [[nodiscard]] uint32_t flags() const noexcept {
        return flags_.load(std::memory_order_acquire);
    }
    void setCallback(std::function<void(uint32_t)> cb) {
        std::lock_guard lk(mtx_);
        cb_ = std::move(cb);
    }
private:
    void loop() {
        prctl(PR_SET_NAME, "omni_guard_wt", 0, 0, 0);
        RootDetector  root;
        FridaDetector frida;
        DebugDetector debug;
        EmulatorDetector emu;
        int cycle = 0;
        while (running_.load(std::memory_order_acquire)) {
            uint32_t detected = 0;
            detected |= frida.scan();
            if (cycle % 2  == 0) detected |= debug.scan();
            if (cycle % 5  == 0) detected |= root.scan();
            if (cycle % 15 == 0) detected |= emu.scan();
            if (detected != 0) {
                uint32_t prev = flags_.fetch_or(detected, std::memory_order_acq_rel);
                if ((prev | detected) != prev) {
                    std::lock_guard lk(mtx_);
                    if (cb_) cb_(detected);
                    LOGW("Threat flags=0x%08X", detected);
                }
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
    std::unique_ptr<SignatureVerifier> sigVerifier;
    std::atomic<uint32_t>              cachedFlags{0};
    std::atomic<bool>                  initialized{false};
};

} // namespace omni::guard

static omni::guard::GuardState gGuard;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_omni_backrooms_Native_1Bridge_initGuard(
        JNIEnv* env, jobject, jobject ctx, jstring expectedSigHash) {
    using namespace omni::guard;
    const char* raw = env->GetStringUTFChars(expectedSigHash, nullptr);
    std::string hash(raw ? raw : "");
    if (raw) env->ReleaseStringUTFChars(expectedSigHash, raw);

    gGuard.sigVerifier = std::make_unique<SignatureVerifier>(hash);

    if (!hash.empty() && ctx) {
        if (!gGuard.sigVerifier->verify(env, ctx)) {
            gGuard.cachedFlags.fetch_or(FLAG_SIG_MISMATCH, std::memory_order_acq_rel);
            LOGW("Signature FAILED expected=%s", hash.c_str());
        }
    }

    uint32_t initial = 0;
    initial |= gGuard.root.scan();
    initial |= gGuard.frida.scan();
    initial |= gGuard.debug.scan();
    initial |= gGuard.emulator.scan();
    gGuard.cachedFlags.fetch_or(initial, std::memory_order_acq_rel);

    gGuard.monitor.setCallback([](uint32_t f) {
        gGuard.cachedFlags.fetch_or(f, std::memory_order_acq_rel);
    });
    gGuard.monitor.start();
    gGuard.initialized.store(true, std::memory_order_release);
    LOGI("Guard init flags=0x%08X", gGuard.cachedFlags.load());
    return JNI_TRUE;
}

JNIEXPORT jint JNICALL
Java_com_omni_backrooms_Native_1Bridge_getGuardFlags(JNIEnv*, jobject) {
    return static_cast<jint>(gGuard.cachedFlags.load(std::memory_order_acquire));
}

JNIEXPORT jint JNICALL
Java_com_omni_backrooms_Native_1Bridge_runGuardScan(JNIEnv*, jobject) {
    using namespace omni::guard;
    uint32_t f = 0;
    f |= gGuard.root.scan();
    f |= gGuard.frida.scan();
    f |= gGuard.debug.scan();
    f |= gGuard.emulator.scan();
    gGuard.cachedFlags.fetch_or(f, std::memory_order_acq_rel);
    return static_cast<jint>(gGuard.cachedFlags.load(std::memory_order_acquire));
}

JNIEXPORT jboolean JNICALL
Java_com_omni_backrooms_Native_1Bridge_isRooted(JNIEnv*, jobject) {
    uint32_t f = gGuard.cachedFlags.load(std::memory_order_acquire);
    return (f & (omni::guard::FLAG_ROOT_BINARY | omni::guard::FLAG_ROOT_PROPS  |
                 omni::guard::FLAG_ROOT_PATHS   | omni::guard::FLAG_MAGISK      |
                 omni::guard::FLAG_SHADOW_MOUNT  | omni::guard::FLAG_ZYGISK     |
                 omni::guard::FLAG_KSU)) != 0;
}

JNIEXPORT jboolean JNICALL
Java_com_omni_backrooms_Native_1Bridge_isFridaDetected(JNIEnv*, jobject) {
    uint32_t f = gGuard.cachedFlags.load(std::memory_order_acquire);
    return (f & (omni::guard::FLAG_FRIDA_PORT  | omni::guard::FLAG_FRIDA_MAPS   |
                 omni::guard::FLAG_FRIDA_THREAD | omni::guard::FLAG_FRIDA_GADGET)) != 0;
}

JNIEXPORT jboolean JNICALL
Java_com_omni_backrooms_Native_1Bridge_isDebugged(JNIEnv*, jobject) {
    uint32_t f = gGuard.cachedFlags.load(std::memory_order_acquire);
    return (f & (omni::guard::FLAG_PTRACE_TRACED | omni::guard::FLAG_DEBUG_WAIT  |
                 omni::guard::FLAG_XPOSED         | omni::guard::FLAG_SUBSTRATE   |
                 omni::guard::FLAG_LSPOSED         | omni::guard::FLAG_HOOK_INLINE)) != 0;
}

JNIEXPORT jboolean JNICALL
Java_com_omni_backrooms_Native_1Bridge_isEmulator(JNIEnv*, jobject) {
    uint32_t f = gGuard.cachedFlags.load(std::memory_order_acquire);
    return (f & (omni::guard::FLAG_EMULATOR_PROPS | omni::guard::FLAG_EMULATOR_HW |
                 omni::guard::FLAG_EMULATOR_CPU)) != 0;
}

JNIEXPORT jboolean JNICALL
Java_com_omni_backrooms_Native_1Bridge_isSignatureValid(JNIEnv*, jobject) {
    return (gGuard.cachedFlags.load(std::memory_order_acquire) &
            omni::guard::FLAG_SIG_MISMATCH) == 0;
}

JNIEXPORT jstring JNICALL
Java_com_omni_backrooms_Native_1Bridge_getThreatReport(JNIEnv* env, jobject) {
    uint32_t f = gGuard.cachedFlags.load(std::memory_order_acquire);
    std::string r;
    auto ap = [&](uint32_t flag, const char* name) {
        if (f & flag) { if (!r.empty()) r += '|'; r += name; }
    };
    ap(omni::guard::FLAG_ROOT_BINARY,    "ROOT_BINARY");
    ap(omni::guard::FLAG_ROOT_PROPS,     "ROOT_PROPS");
    ap(omni::guard::FLAG_ROOT_PATHS,     "ROOT_PATHS");
    ap(omni::guard::FLAG_SELINUX_OFF,    "SELINUX_OFF");
    ap(omni::guard::FLAG_MAGISK,         "MAGISK");
    ap(omni::guard::FLAG_ZYGISK,         "ZYGISK");
    ap(omni::guard::FLAG_KSU,            "KSU");
    ap(omni::guard::FLAG_LSPOSED,        "LSPOSED");
    ap(omni::guard::FLAG_FRIDA_PORT,     "FRIDA_PORT");
    ap(omni::guard::FLAG_FRIDA_MAPS,     "FRIDA_MAPS");
    ap(omni::guard::FLAG_FRIDA_THREAD,   "FRIDA_THREAD");
    ap(omni::guard::FLAG_FRIDA_GADGET,   "FRIDA_GADGET");
    ap(omni::guard::FLAG_PTRACE_TRACED,  "PTRACE");
    ap(omni::guard::FLAG_DEBUG_WAIT,     "DEBUGWAIT");
    ap(omni::guard::FLAG_EMULATOR_PROPS, "EMU_PROPS");
    ap(omni::guard::FLAG_EMULATOR_HW,    "EMU_HW");
    ap(omni::guard::FLAG_EMULATOR_CPU,   "EMU_CPU");
    ap(omni::guard::FLAG_SIG_MISMATCH,   "SIG_MISMATCH");
    ap(omni::guard::FLAG_XPOSED,         "XPOSED");
    ap(omni::guard::FLAG_SUBSTRATE,      "SUBSTRATE");
    ap(omni::guard::FLAG_SHADOW_MOUNT,   "SHADOW_MOUNT");
    ap(omni::guard::FLAG_HOOK_INLINE,    "INLINE_HOOK");
    ap(omni::guard::FLAG_PROC_TAMPER,    "PROC_TAMPER");
    if (r.empty()) r = "CLEAN";
    return env->NewStringUTF(r.c_str());
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_destroyGuard(JNIEnv*, jobject) {
    gGuard.monitor.stop();
    gGuard.sigVerifier.reset();
    gGuard.initialized.store(false, std::memory_order_release);
    LOGI("Guard destroyed");
}

} // extern "C"
