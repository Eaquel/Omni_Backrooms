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
    Entity/Entity.cpp
    Sound/Synth.cpp
    Shield/Shield.cpp
    Shield/Unity.cpp
)

# Map/ holds level generation. Kept out of Engine.cpp because it is the part
# most likely to be iterated on, and it has no business sharing a translation
# unit with audio, netcode and the anti-tamper guard.
#
# Entity/ holds the creatures. Their behaviour is the least inspectable code
# in the game and was the only part that could not be built without a device;
# on its own it compiles against Map/ alone, so Entity_Check.py can put a
# creature in a corridor and watch what it does.
#
# Sound/ holds every generator, because this game ships no audio files at all
# and a sound you cannot hear is a sound nobody checks. Code_To_Sound.py renders
# this exact translation unit, so the waveform that is asserted on is the one
# that plays.
#
# Shield/ is everything that defends the build: the root/Frida/debugger/emulator
# detectors that used to sit in the middle of Engine.cpp, and the Unity costume
# that used to sit in Disguise/. Splitting them meant neither could be compiled
# without the NDK and neither had ever been run by a tool; together they build
# on a host, so Native_Check.py compiles them with warnings as errors like every
# other module. The one piece that could not move is the signature check, which
# calls PackageManager over JNI and so stays in Engine.cpp.
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

# libunity.so — the second half of the costume, and the only part of it that is
# a real artefact rather than a string. libil2cpp.so on its own is a file with a
# suggestive name; the two side by side in lib/arm64-v8a/ are what every Unity
# APK on a phone actually looks like, and tools that fingerprint by listing that
# directory stop at the first match.
#
# Nothing in the app loads it. It is packaged and never opened.
add_library(
    unity
    SHARED
    Shield/Player.cpp
)

target_include_directories(unity PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
target_compile_options(unity PRIVATE ${BASE_FLAGS})

# Deliberately NOT --gc-sections here. Everything in this library is unreachable
# by definition, so section GC would collect the whole point of it.
target_link_options(unity PRIVATE
    -Wl,-z,relro
    -Wl,-z,now
    -Wl,-z,noexecstack
    -Wl,--strip-all
)

target_link_libraries(unity ${log-lib})

set_target_properties(unity PROPERTIES
    CXX_VISIBILITY_PRESET     hidden
    VISIBILITY_INLINES_HIDDEN ON
)
