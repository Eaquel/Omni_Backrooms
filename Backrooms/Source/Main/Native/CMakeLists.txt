cmake_minimum_required(VERSION 4.3.2)

project("il2cpp" CXX)

set(CMAKE_CXX_STANDARD 26)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_INTERPROCEDURAL_OPTIMIZATION ON)

find_library(log-lib     log)
find_library(android-lib android)
find_library(aaudio-lib  aaudio)
find_library(gles-lib    GLESv3)
find_library(egl-lib     EGL)
find_library(dl-lib      dl)
find_library(z-lib       z)
find_library(jnigraphics jnigraphics)

add_library(
    il2cpp
    SHARED
    Engine.cpp
    Map/Level_0.cpp
    Frame/Frame.cpp
    Trail/Trail.cpp
)

# Map/ holds level generation. Kept out of Engine.cpp because it is the part
# most likely to be iterated on, and it has no business sharing a translation
# unit with audio, netcode and the anti-tamper guard.
#
# Frame/ and Trail/ hold the cosmetics. A frame's silhouette and a trail's
# ageing rules are content, and content that was previously a switch on a
# string buried in a Compose file — so the identity of every cosmetic, its
# English id included, now lives in one table per folder.
target_include_directories(il2cpp PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

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
        -march=armv8-a
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
    ${aaudio-lib}
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
