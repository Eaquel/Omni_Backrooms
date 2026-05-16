cmake_minimum_required(VERSION 4.3.2)

project("omni_backrooms" CXX)

# ── C++26 zorunlu ─────────────────────────────────────────────
set(CMAKE_CXX_STANDARD            26)
set(CMAKE_CXX_STANDARD_REQUIRED   ON)
set(CMAKE_CXX_EXTENSIONS          OFF)
set(CMAKE_INTERPROCEDURAL_OPTIMIZATION ON)

# ── Sistem kütüphaneleri ──────────────────────────────────────
find_library(log-lib     log)
find_library(android-lib android)
find_library(opensl-lib  OpenSLES)
find_library(gles-lib    GLESv3)
find_library(egl-lib     EGL)
find_library(dl-lib      dl)
find_library(z-lib       z)
find_library(jnigraphics jnigraphics)   # android/bitmap.h için

add_library(
    omni_backrooms
    SHARED
    Core.cpp
    Sound.cpp
    Entity.cpp
    Network.cpp
    Guard.cpp
)

target_include_directories(omni_backrooms PRIVATE
    ${CMAKE_SOURCE_DIR}
)

# ── Hardening flags — tüm build tiplerinde aktif ─────────────
set(COMMON_FLAGS
    -fstack-protector-strong
    -fstack-clash-protection
    -U_FORTIFY_SOURCE
    -D_FORTIFY_SOURCE=3
    -fvisibility=hidden
    -fvisibility-inlines-hidden
    -ffunction-sections
    -fdata-sections
    -Wall
    -Wextra
    -Wpedantic
    -Wno-unused-parameter
    -Wno-c++26-extensions
)

# ── ABI bazlı SIMD optimizasyonları ──────────────────────────
if(ANDROID_ABI STREQUAL "arm64-v8a")
    list(APPEND COMMON_FLAGS
        -march=armv8.2-a+simd+dotprod+fp16
        -mtune=cortex-a76
        # -mfpu= arm32'e özgüdür, aarch64'te kullanılmaz
    )
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    list(APPEND COMMON_FLAGS
        -march=armv7-a
        -mfpu=neon-vfpv4
        -mfloat-abi=softfp
        -mtune=cortex-a15
    )
endif()

# ── Release / Debug ───────────────────────────────────────────
set(RELEASE_FLAGS
    -O3
    -flto=thin
    -fomit-frame-pointer
    -fstrict-aliasing
    -DNDEBUG
    -DOMNI_RELEASE=1
)

set(DEBUG_FLAGS
    -O0
    -g3
    -DOMNI_DEBUG=1
)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(omni_backrooms PRIVATE
        ${COMMON_FLAGS}
        ${RELEASE_FLAGS}
    )
    target_link_options(omni_backrooms PRIVATE
        -Wl,-z,relro
        -Wl,-z,now
        -Wl,-z,noexecstack
        -Wl,--gc-sections
        -Wl,--strip-all
        -flto=thin
    )
else()
    target_compile_options(omni_backrooms PRIVATE
        ${COMMON_FLAGS}
        ${DEBUG_FLAGS}
    )
endif()

# ── Link ──────────────────────────────────────────────────────
target_link_libraries(omni_backrooms
    ${log-lib}
    ${android-lib}
    ${opensl-lib}
    ${gles-lib}
    ${egl-lib}
    ${dl-lib}
    ${z-lib}
    ${jnigraphics}
)

set_target_properties(omni_backrooms PROPERTIES
    CXX_VISIBILITY_PRESET     hidden
    VISIBILITY_INLINES_HIDDEN ON
    POSITION_INDEPENDENT_CODE ON
)
