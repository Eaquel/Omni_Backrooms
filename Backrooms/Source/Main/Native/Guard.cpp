#include <jni.h>
#include <android/log.h>
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
#include <optional>
#include <ranges>
#include <span>
#include <string>
#include <string_view>
#include <sys/ptrace.h>
#include <sys/stat.h>
#include <sys/system_properties.h>
#include <thread>
#include <unistd.h>
#include <vector>
#include <linux/prctl.h>
#include <sys/prctl.h>

#define TAG "OmniGuard"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  TAG, __VA_ARGS__)

namespace omni::guard {

enum class ThreatLevel : uint8_t {
    Clean      = 0,
    Suspicious = 1,
    Rooted     = 2,
    Tampered   = 3,
    Debugged   = 4,
    Frida      = 5,
    Emulator   = 6
};

struct DetectionResult {
    ThreatLevel level   = ThreatLevel::Clean;
    uint32_t    flags   = 0;
    std::string details;
};

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

[[nodiscard]] bool fileExists(std::string_view path) noexcept {
    struct stat st{};
    return stat(path.data(), &st) == 0;
}

[[nodiscard]] bool canOpen(std::string_view path) noexcept {
    int fd = open(path.data(), O_RDONLY);
    if (fd < 0) return false;
    close(fd);
    return true;
}

[[nodiscard]] std::string readSmallFile(std::string_view path) noexcept {
    int fd = open(path.data(), O_RDONLY);
    if (fd < 0) return {};
    char buf[4096]{};
    ssize_t n = read(fd, buf, sizeof(buf) - 1);
    close(fd);
    if (n <= 0) return {};
    return std::string(buf, static_cast<size_t>(n));
}

[[nodiscard]] std::string getSysProp(const char* key) noexcept {
    char val[PROP_VALUE_MAX]{};
    __system_property_get(key, val);
    return std::string(val);
}

[[nodiscard]] bool containsInsensitive(std::string_view haystack, std::string_view needle) noexcept {
    if (needle.size() > haystack.size()) return false;
    auto it = std::search(haystack.begin(), haystack.end(),
                          needle.begin(),  needle.end(),
                          [](char a, char b){ return std::tolower(a) == std::tolower(b); });
    return it != haystack.end();
}

class RootDetector {
public:
    [[nodiscard]] uint32_t scan() noexcept {
        uint32_t flags = 0;
        if (checkRootBinaries())    flags |= FLAG_ROOT_BINARY;
        if (checkRootProperties())  flags |= FLAG_ROOT_PROPS;
        if (checkRootPaths())       flags |= FLAG_ROOT_PATHS;
        if (checkSelinux())         flags |= FLAG_SELINUX_OFF;
        if (checkMagisk())          flags |= FLAG_MAGISK;
        if (checkShadowMount())     flags |= FLAG_SHADOW_MOUNT;
        return flags;
    }

private:
    [[nodiscard]] bool checkRootBinaries() noexcept {
        static constexpr std::string_view bins[] = {
            "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/system/sbin/su", "/vendor/bin/su", "/su/bin/su",
            "/data/local/su", "/data/local/bin/su", "/data/local/xbin/su",
            "/system/bin/.ext/.su", "/system/usr/we-need-root/su-backup",
            "/system/xbin/busybox", "/system/bin/busybox",
            "/data/adb/magisk", "/sbin/.magisk", "/sbin/.core/mirror",
            "/sbin/.core/img", "/data/adb/ksu"
        };
        for (auto b : bins)
            if (fileExists(b)) return true;
        return false;
    }

    [[nodiscard]] bool checkRootProperties() noexcept {
        auto ro_debuggable  = getSysProp("ro.debuggable");
        auto ro_secure      = getSysProp("ro.secure");
        auto ro_build_tags  = getSysProp("ro.build.tags");
        auto ro_build_type  = getSysProp("ro.build.type");
        if (ro_debuggable == "1")           return true;
        if (ro_secure == "0")               return true;
        if (containsInsensitive(ro_build_tags, "test-keys")) return true;
        if (containsInsensitive(ro_build_type, "userdebug")) return true;
        return false;
    }

    [[nodiscard]] bool checkRootPaths() noexcept {
        static constexpr std::string_view paths[] = {
            "/system/app/SuperSU.apk", "/system/app/Superuser.apk",
            "/system/app/KingoUser.apk", "/system/app/360SuperUser.apk",
            "/data/data/eu.chainfire.supersu", "/data/data/com.noshufou.android.su",
            "/data/data/com.koushikdutta.superuser", "/data/data/com.zachspong.temprootremovejb",
            "/data/data/com.ramdroid.appquarantine", "/data/data/com.topjohnwu.magisk"
        };
        for (auto p : paths)
            if (fileExists(p)) return true;
        return false;
    }

    [[nodiscard]] bool checkSelinux() noexcept {
        auto content = readSmallFile("/sys/fs/selinux/enforce");
        if (content.empty()) return false;
        return content[0] == '0';
    }

    [[nodiscard]] bool checkMagisk() noexcept {
        static constexpr std::string_view magiskPaths[] = {
            "/sbin/.magisk", "/dev/.magisk", "/data/adb/magisk",
            "/data/adb/magisk.img", "/cache/.magisk", "/dev/magisk",
            "/sbin/magisk"
        };
        for (auto p : magiskPaths)
            if (fileExists(p)) return true;
        auto maps = readSmallFile("/proc/self/maps");
        if (containsInsensitive(maps, "magisk")) return true;
        return false;
    }

    [[nodiscard]] bool checkShadowMount() noexcept {
        auto mounts = readSmallFile("/proc/self/mounts");
        if (containsInsensitive(mounts, "magisk")) return true;
        if (containsInsensitive(mounts, "supersu")) return true;
        if (containsInsensitive(mounts, "overlay") &&
            containsInsensitive(mounts, "/system")) return true;
        return false;
    }
};

class FridaDetector {
public:
    [[nodiscard]] uint32_t scan() noexcept {
        uint32_t flags = 0;
        if (checkFridaPort())   flags |= FLAG_FRIDA_PORT;
        if (checkFridaMaps())   flags |= FLAG_FRIDA_MAPS;
        if (checkFridaThread()) flags |= FLAG_FRIDA_THREAD;
        if (checkFridaGadget()) flags |= FLAG_FRIDA_GADGET;
        return flags;
    }

private:
    [[nodiscard]] bool checkFridaPort() noexcept {
        auto tcp  = readSmallFile("/proc/net/tcp");
        auto tcp6 = readSmallFile("/proc/net/tcp6");
        static constexpr std::string_view fridaPorts[] = {
            "6D58", "71D4", "2717", "5039"
        };
        for (auto p : fridaPorts) {
            if (containsInsensitive(tcp,  p)) return true;
            if (containsInsensitive(tcp6, p)) return true;
        }
        return false;
    }

    [[nodiscard]] bool checkFridaMaps() noexcept {
        auto maps = readSmallFile("/proc/self/maps");
        static constexpr std::string_view fridaPatterns[] = {
            "frida", "gum-js-loop", "frida-agent",
            "frida-gadget", "frida-server", "linjector",
            "re.frida.server", "frida-helper"
        };
        for (auto p : fridaPatterns)
            if (containsInsensitive(maps, p)) return true;
        return false;
    }

    [[nodiscard]] bool checkFridaThread() noexcept {
        DIR* dir = opendir("/proc/self/task");
        if (!dir) return false;
        struct dirent* entry;
        bool found = false;
        while ((entry = readdir(dir)) != nullptr) {
            if (entry->d_name[0] == '.') continue;
            std::string commPath = "/proc/self/task/";
            commPath += entry->d_name;
            commPath += "/comm";
            auto comm = readSmallFile(commPath);
            if (containsInsensitive(comm, "gum-js-loop") ||
                containsInsensitive(comm, "frida") ||
                containsInsensitive(comm, "gmain")) {
                found = true; break;
            }
        }
        closedir(dir);
        return found;
    }

    [[nodiscard]] bool checkFridaGadget() noexcept {
        void* h = dlopen("libfrida-gadget.so", RTLD_NOLOAD);
        if (h) { dlclose(h); return true; }
        h = dlopen("re.frida.server", RTLD_NOLOAD);
        if (h) { dlclose(h); return true; }
        return false;
    }
};

class DebugDetector {
public:
    [[nodiscard]] uint32_t scan() noexcept {
        uint32_t flags = 0;
        if (checkPtrace())    flags |= FLAG_PTRACE_TRACED;
        if (checkDebugWait()) flags |= FLAG_DEBUG_WAIT;
        if (checkXposed())    flags |= FLAG_XPOSED;
        if (checkSubstrate()) flags |= FLAG_SUBSTRATE;
        if (checkProcStatus())flags |= FLAG_PROC_TAMPER;
        return flags;
    }

private:
    [[nodiscard]] bool checkPtrace() noexcept {
        if (ptrace(PTRACE_TRACEME, 0, nullptr, nullptr) == -1) return true;
        ptrace(PTRACE_DETACH, 0, nullptr, nullptr);
        return false;
    }

    [[nodiscard]] bool checkDebugWait() noexcept {
        auto status = readSmallFile("/proc/self/status");
        if (containsInsensitive(status, "TracerPid:\t0")) return false;
        auto pos = status.find("TracerPid:");
        if (pos == std::string::npos) return false;
        std::string_view sv(status);
        sv = sv.substr(pos + 10);
        while (!sv.empty() && (sv[0] == ' ' || sv[0] == '\t')) sv.remove_prefix(1);
        return !sv.empty() && sv[0] != '0';
    }

    [[nodiscard]] bool checkXposed() noexcept {
        auto maps = readSmallFile("/proc/self/maps");
        return containsInsensitive(maps, "xposed") ||
               fileExists("/system/framework/XposedBridge.jar") ||
               fileExists("/system/lib/libxposed_art.so");
    }

    [[nodiscard]] bool checkSubstrate() noexcept {
        void* h = dlopen("libsubstrate.so", RTLD_NOLOAD);
        if (h) { dlclose(h); return true; }
        h = dlopen("libsubstrate-dvm.so", RTLD_NOLOAD);
        if (h) { dlclose(h); return true; }
        return containsInsensitive(readSmallFile("/proc/self/maps"), "substrate");
    }

    [[nodiscard]] bool checkProcStatus() noexcept {
        auto status = readSmallFile("/proc/self/status");
        if (status.empty()) return true;
        if (!containsInsensitive(status, "Name:")) return true;
        return false;
    }
};

class EmulatorDetector {
public:
    [[nodiscard]] uint32_t scan() noexcept {
        uint32_t flags = 0;
        if (checkEmulatorProps()) flags |= FLAG_EMULATOR_PROPS;
        if (checkEmulatorHw())    flags |= FLAG_EMULATOR_HW;
        if (checkCpuInfo())       flags |= FLAG_EMULATOR_CPU;
        return flags;
    }

private:
    [[nodiscard]] bool checkEmulatorProps() noexcept {
        static constexpr const char* emulatorProps[][2] = {
            { "ro.hardware",               "goldfish"    },
            { "ro.hardware",               "ranchu"      },
            { "ro.product.model",          "sdk"         },
            { "ro.product.device",         "generic"     },
            { "ro.product.name",           "sdk"         },
            { "ro.kernel.qemu",            "1"           },
            { "ro.product.manufacturer",   "unknown"     },
            { "ro.build.product",          "generic"     }
        };
        for (auto& [key, val] : emulatorProps) {
            auto v = getSysProp(key);
            if (containsInsensitive(v, val)) return true;
        }
        return false;
    }

    [[nodiscard]] bool checkEmulatorHw() noexcept {
        static constexpr std::string_view emulatorFiles[] = {
            "/dev/socket/qemud", "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace", "/system/bin/qemu-props"
        };
        for (auto f : emulatorFiles)
            if (fileExists(f)) return true;
        return false;
    }

    [[nodiscard]] bool checkCpuInfo() noexcept {
        auto cpuInfo = readSmallFile("/proc/cpuinfo");
        return containsInsensitive(cpuInfo, "goldfish") ||
               containsInsensitive(cpuInfo, "ranchu");
    }
};

class SignatureVerifier {
public:
    explicit SignatureVerifier(std::string_view expectedHash)
        : expected_(expectedHash) {}

    [[nodiscard]] bool verify(JNIEnv* env, jobject ctx) noexcept {
        if (!env || !ctx) return false;

        jclass ctxCls = env->GetObjectClass(ctx);
        if (!ctxCls) return false;

        jmethodID getPkgMgr = env->GetMethodID(ctxCls, "getPackageManager",
            "()Landroid/content/pm/PackageManager;");
        jmethodID getPkgName= env->GetMethodID(ctxCls, "getPackageName",
            "()Ljava/lang/String;");
        if (!getPkgMgr || !getPkgName) return false;

        jobject pm = env->CallObjectMethod(ctx, getPkgMgr);
        jstring pkg= static_cast<jstring>(env->CallObjectMethod(ctx, getPkgName));
        if (!pm || !pkg) return false;

        jclass pmCls = env->GetObjectClass(pm);
        jmethodID getPkgInfo = env->GetMethodID(pmCls, "getPackageInfo",
            "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
        if (!getPkgInfo) return false;

        jobject pkgInfo = env->CallObjectMethod(pm, getPkgInfo, pkg, 0x40 /* GET_SIGNATURES */);
        if (!pkgInfo) return false;

        jclass pkgInfoCls = env->GetObjectClass(pkgInfo);
        jfieldID sigsFid   = env->GetFieldID(pkgInfoCls, "signatures",
            "[Landroid/content/pm/Signature;");
        if (!sigsFid) return false;

        auto sigsArr = static_cast<jobjectArray>(env->GetObjectField(pkgInfo, sigsFid));
        if (!sigsArr || env->GetArrayLength(sigsArr) == 0) return false;

        jobject sig0 = env->GetObjectArrayElement(sigsArr, 0);
        jclass sigCls = env->GetObjectClass(sig0);
        jmethodID toByteArrayMid = env->GetMethodID(sigCls, "toByteArray", "()[B");
        if (!toByteArrayMid) return false;

        auto byteArr = static_cast<jbyteArray>(env->CallObjectMethod(sig0, toByteArrayMid));
        if (!byteArr) return false;

        jsize len = env->GetArrayLength(byteArr);
        std::vector<uint8_t> bytes(len);
        env->GetByteArrayRegion(byteArr, 0, len, reinterpret_cast<jbyte*>(bytes.data()));

        uint64_t hash = 14695981039346656037ULL;
        for (auto b : bytes) {
            hash ^= static_cast<uint64_t>(b);
            hash *= 1099511628211ULL;
        }

        char hexBuf[17];
        snprintf(hexBuf, sizeof(hexBuf), "%016llx",
                 static_cast<unsigned long long>(hash));
        return expected_ == std::string_view(hexBuf);
    }

private:
    std::string expected_;
};

class AntiTamperMonitor {
public:
    void startContinuousMonitoring() {
        running_.store(true);
        thread_ = std::thread([this] { monitorLoop(); });
    }

    void stop() {
        running_.store(false);
        if (thread_.joinable()) thread_.join();
    }

    [[nodiscard]] uint32_t getAccumulatedFlags() const noexcept {
        return flags_.load();
    }

    [[nodiscard]] bool isThreatDetected() const noexcept {
        return flags_.load() != 0;
    }

    void setCallback(std::function<void(uint32_t)> cb) {
        std::lock_guard<std::mutex> lk(mtx_);
        callback_ = std::move(cb);
    }

private:
    void monitorLoop() {
        RootDetector  root;
        FridaDetector frida;
        DebugDetector debug;
        EmulatorDetector emu;

        int cycle = 0;
        while (running_.load()) {
            uint32_t detected = 0;

            if (cycle % 1 == 0) detected |= frida.scan();
            if (cycle % 2 == 0) detected |= debug.scan();
            if (cycle % 5 == 0) detected |= root.scan();
            if (cycle % 10== 0) detected |= emu.scan();

            if (detected != 0) {
                uint32_t prev = flags_.fetch_or(detected);
                if ((prev | detected) != prev) {
                    std::lock_guard<std::mutex> lk(mtx_);
                    if (callback_) callback_(detected);
                    LOGW("Threat detected: flags=0x%08X", detected);
                }
            }

            ++cycle;
            std::this_thread::sleep_for(std::chrono::milliseconds(800));
        }
    }

    std::atomic<bool>     running_{false};
    std::atomic<uint32_t> flags_  {0};
    std::thread           thread_;
    std::mutex            mtx_;
    std::function<void(uint32_t)> callback_;
};

struct GuardState {
    RootDetector     root;
    FridaDetector    frida;
    DebugDetector    debug;
    EmulatorDetector emulator;
    AntiTamperMonitor monitor;
    std::unique_ptr<SignatureVerifier> sigVerifier;
    std::atomic<uint32_t> cachedFlags{0};
    std::atomic<bool> initialized{false};
};

} // namespace omni::guard

static omni::guard::GuardState gGuard;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_omni_backrooms_Native_1Bridge_initGuard(
        JNIEnv* env, jobject, jobject ctx, jstring expectedSigHash) {

    using namespace omni::guard;

    const char* hashCStr = env->GetStringUTFChars(expectedSigHash, nullptr);
    std::string hash(hashCStr);
    env->ReleaseStringUTFChars(expectedSigHash, hashCStr);

    gGuard.sigVerifier = std::make_unique<SignatureVerifier>(hash);

    if (!hash.empty() && ctx) {
        bool sigOk = gGuard.sigVerifier->verify(env, ctx);
        if (!sigOk) {
            gGuard.cachedFlags.fetch_or(FLAG_SIG_MISMATCH);
            LOGW("Signature verification FAILED");
        }
    }

    uint32_t initial = 0;
    initial |= gGuard.root.scan();
    initial |= gGuard.frida.scan();
    initial |= gGuard.debug.scan();
    initial |= gGuard.emulator.scan();
    gGuard.cachedFlags.fetch_or(initial);

    gGuard.monitor.setCallback([](uint32_t newFlags) {
        gGuard.cachedFlags.fetch_or(newFlags);
    });
    gGuard.monitor.startContinuousMonitoring();

    gGuard.initialized.store(true);
    LOGI("Guard initialized (flags=0x%08X)", gGuard.cachedFlags.load());
    return JNI_TRUE;
}

JNIEXPORT jint JNICALL
Java_com_omni_backrooms_Native_1Bridge_getGuardFlags(JNIEnv*, jobject) {
    return static_cast<jint>(gGuard.cachedFlags.load());
}

JNIEXPORT jint JNICALL
Java_com_omni_backrooms_Native_1Bridge_runGuardScan(JNIEnv*, jobject) {
    using namespace omni::guard;
    uint32_t flags = 0;
    flags |= gGuard.root.scan();
    flags |= gGuard.frida.scan();
    flags |= gGuard.debug.scan();
    flags |= gGuard.emulator.scan();
    gGuard.cachedFlags.fetch_or(flags);
    return static_cast<jint>(gGuard.cachedFlags.load());
}

JNIEXPORT jboolean JNICALL
Java_com_omni_backrooms_Native_1Bridge_isRooted(JNIEnv*, jobject) {
    uint32_t f = gGuard.cachedFlags.load();
    return (f & (omni::guard::FLAG_ROOT_BINARY |
                 omni::guard::FLAG_ROOT_PROPS   |
                 omni::guard::FLAG_ROOT_PATHS    |
                 omni::guard::FLAG_MAGISK        |
                 omni::guard::FLAG_SHADOW_MOUNT)) != 0
        ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_omni_backrooms_Native_1Bridge_isFridaDetected(JNIEnv*, jobject) {
    uint32_t f = gGuard.cachedFlags.load();
    return (f & (omni::guard::FLAG_FRIDA_PORT   |
                 omni::guard::FLAG_FRIDA_MAPS    |
                 omni::guard::FLAG_FRIDA_THREAD  |
                 omni::guard::FLAG_FRIDA_GADGET)) != 0
        ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_omni_backrooms_Native_1Bridge_isDebugged(JNIEnv*, jobject) {
    uint32_t f = gGuard.cachedFlags.load();
    return (f & (omni::guard::FLAG_PTRACE_TRACED |
                 omni::guard::FLAG_DEBUG_WAIT     |
                 omni::guard::FLAG_XPOSED         |
                 omni::guard::FLAG_SUBSTRATE)) != 0
        ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_omni_backrooms_Native_1Bridge_isEmulator(JNIEnv*, jobject) {
    uint32_t f = gGuard.cachedFlags.load();
    return (f & (omni::guard::FLAG_EMULATOR_PROPS |
                 omni::guard::FLAG_EMULATOR_HW    |
                 omni::guard::FLAG_EMULATOR_CPU)) != 0
        ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_omni_backrooms_Native_1Bridge_isSignatureValid(JNIEnv*, jobject) {
    uint32_t f = gGuard.cachedFlags.load();
    return (f & omni::guard::FLAG_SIG_MISMATCH) == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_omni_backrooms_Native_1Bridge_getThreatReport(JNIEnv* env, jobject) {
    uint32_t f = gGuard.cachedFlags.load();
    std::string report;
    auto append = [&](uint32_t flag, const char* name) {
        if (f & flag) { if (!report.empty()) report += "|"; report += name; }
    };
    append(omni::guard::FLAG_ROOT_BINARY,   "ROOT_BINARY");
    append(omni::guard::FLAG_ROOT_PROPS,    "ROOT_PROPS");
    append(omni::guard::FLAG_ROOT_PATHS,    "ROOT_PATHS");
    append(omni::guard::FLAG_SELINUX_OFF,   "SELINUX_OFF");
    append(omni::guard::FLAG_MAGISK,        "MAGISK");
    append(omni::guard::FLAG_FRIDA_PORT,    "FRIDA_PORT");
    append(omni::guard::FLAG_FRIDA_MAPS,    "FRIDA_MAPS");
    append(omni::guard::FLAG_FRIDA_THREAD,  "FRIDA_THREAD");
    append(omni::guard::FLAG_FRIDA_GADGET,  "FRIDA_GADGET");
    append(omni::guard::FLAG_PTRACE_TRACED, "PTRACE");
    append(omni::guard::FLAG_DEBUG_WAIT,    "DEBUGWAIT");
    append(omni::guard::FLAG_EMULATOR_PROPS,"EMU_PROPS");
    append(omni::guard::FLAG_EMULATOR_HW,   "EMU_HW");
    append(omni::guard::FLAG_EMULATOR_CPU,  "EMU_CPU");
    append(omni::guard::FLAG_SIG_MISMATCH,  "SIG_MISMATCH");
    append(omni::guard::FLAG_XPOSED,        "XPOSED");
    append(omni::guard::FLAG_SUBSTRATE,     "SUBSTRATE");
    append(omni::guard::FLAG_SHADOW_MOUNT,  "SHADOW_MOUNT");
    append(omni::guard::FLAG_PROC_TAMPER,   "PROC_TAMPER");
    if (report.empty()) report = "CLEAN";
    return env->NewStringUTF(report.c_str());
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_destroyGuard(JNIEnv*, jobject) {
    gGuard.monitor.stop();
    gGuard.sigVerifier.reset();
    gGuard.initialized.store(false);
    LOGI("Guard destroyed");
}

} // extern "C"
