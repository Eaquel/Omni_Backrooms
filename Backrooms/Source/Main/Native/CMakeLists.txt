cmake_minimum_required(VERSION 4.3.2)

project("il2cpp" CXX)

set(CMAKE_CXX_STANDARD 26)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
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
    il2cpp
    SHARED
    Engine.cpp
)

target_include_directories(il2cpp PRIVATE
    ${CMAKE_SOURCE_DIR}
)

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
        -march=armv8.5-a+simd+dotprod+fp16+sha3
        -mtune=cortex-x1
        -fstack-clash-protection
        -mbranch-protection=standard
    )
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    list(APPEND BASE_FLAGS
        -march=armv7-a
        -mfpu=neon-vfpv4
        -mfloat-abi=softfp
        -mtune=cortex-a15
    )
endif()

target_compile_options(il2cpp PRIVATE ${BASE_FLAGS})

target_link_options(il2cpp PRIVATE
    -Wl,-z,relro
    -Wl,-z,now
    -Wl,-z,noexecstack
    -Wl,--gc-sections
    -Wl,--strip-all
    -flto=thin
)

target_link_libraries(il2cpp
    ${log-lib}
    ${android-lib}
    ${opensl-lib}
    ${gles-lib}
    ${egl-lib}
    ${dl-lib}
    ${z-lib}
    ${jnigraphics}
)

set_target_properties(il2cpp PROPERTIES
    CXX_VISIBILITY_PRESET     hidden
    VISIBILITY_INLINES_HIDDEN ON
)
