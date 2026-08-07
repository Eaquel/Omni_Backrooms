#include "Shield/Shield.h"

#include <cstdio>

#if defined(__ANDROID__)
#include <android/log.h>
#include <sys/system_properties.h>
#endif

namespace omni::shield {

/**
 * One Android system property.
 *
 * `__system_property_get` is the obvious call and it is the wrong one: it has
 * been deprecated since API 26 and returns nothing at all on recent releases
 * for properties the app is not allowed to read, which is silently the same
 * answer as "this device is clean". The find/read_callback pair is what
 * actually works, and getting this wrong would have disabled emulator
 * detection without failing anything.
 *
 * Off-device this returns empty, which is the right answer: a build machine is
 * not an emulator, and the detectors reading "" simply do not fire.
 */
std::string sysProp(const char* key) noexcept {
#if defined(__ANDROID__)
    char val[PROP_VALUE_MAX]{};
    if (const auto* info = __system_property_find(key)) {
        __system_property_read_callback(
            info,
            [](void* cookie, const char*, const char* value, uint32_t) {
                std::strncpy(static_cast<char*>(cookie), value, PROP_VALUE_MAX - 1);
            },
            val);
    }
    return std::string(val);
#else
    (void) key;
    return {};
#endif
}

void shieldLog(const char* fmt, ...) noexcept {
    std::va_list args;
    va_start(args, fmt);
#if defined(__ANDROID__)
    __android_log_vprint(ANDROID_LOG_INFO, "OmniShield", fmt, args);
#else
    std::fputs("[shield] ", stderr);
    std::vfprintf(stderr, fmt, args);
    std::fputc('\n', stderr);
#endif
    va_end(args);
}

} // namespace omni::shield
