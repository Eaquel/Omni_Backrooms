cmake_minimum_required(VERSION 3.22)

project("omni_backrooms" CXX)

# C++26 standardı -std=c++26 flag ile doğrudan iletiliyor (aşağıda).
# CMAKE_CXX_STANDARD=26 kullanılmıyor: CMake 3.22 CXX26 dialect'ini tanımıyor
# ve "does not know the compile flags" hatasını veriyor.
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_INTERPROCEDURAL_OPTIMIZATION ON)

find_library(log-lib     log)
find_library(android-lib android)
find_library(opensl-lib  OpenSLES)
find_library(gles-lib    GLESv3)
find_library(egl-lib     EGL)
find_library(dl-lib      dl)
find_library(z-lib       z)
find_library(jnigraphics jnigraphics)

add_library(
    omni_backrooms
    SHARED
    Engine.cpp
    Backrooms.cpp
)

target_include_directories(omni_backrooms PRIVATE
    ${CMAKE_SOURCE_DIR}
)

# C++26 flag — CMake dialect mekanizması bypass edilerek doğrudan iletiliyor
target_compile_options(omni_backrooms PRIVATE -std=c++26)

set(BASE_FLAGS
    -O3
    -DNDEBUG
    -DOMNI_RELEASE=1
    -fstack-protector-strong
    -U_FORTIFY_SOURCE
    -D_FORTIFY_SOURCE=3
    -fvisibility=hidden
    -fvisibility-inlines-hidden
    -ffunction-sections
    -fdata-sections
    -fstrict-aliasing
    -fomit-frame-pointer
    -Wall
    -Wextra
    -Wpedantic
    -Wno-unused-parameter
)

if(ANDROID_ABI STREQUAL "arm64-v8a")
    list(APPEND BASE_FLAGS
        -march=armv8.2-a+simd+dotprod+fp16
        -mtune=cortex-a76
        -fstack-clash-protection
    )
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    list(APPEND BASE_FLAGS
        -march=armv7-a
        -mfpu=neon-vfpv4
        -mfloat-abi=softfp
        -mtune=cortex-a15
    )
endif()

target_compile_options(omni_backrooms PRIVATE
    ${BASE_FLAGS}
)

target_link_options(omni_backrooms PRIVATE
    -Wl,-z,relro
    -Wl,-z,now
    -Wl,-z,noexecstack
    -Wl,--gc-sections
    -Wl,--strip-all
    -flto=thin
)

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
)
