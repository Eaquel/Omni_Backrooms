cmake_minimum_required(VERSION 4.3.2)

project("omni_backrooms" CXX)

set(CMAKE_CXX_STANDARD            26)
set(CMAKE_CXX_STANDARD_REQUIRED   ON)
set(CMAKE_CXX_EXTENSIONS          OFF)
set(CMAKE_INTERPROCEDURAL_OPTIMIZATION ON)

find_library(log-lib     log)
find_library(android-lib android)
find_library(opensl-lib  OpenSLES)
find_library(gles-lib    GLESv3)
find_library(egl-lib     EGL)
find_library(dl-lib      dl)
find_library(z-lib       z)

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

set(HARDENING_FLAGS
    -fstack-protector-strong
    -fstack-clash-protection
    -D_FORTIFY_SOURCE=3
    -fvisibility=hidden
    -fvisibility-inlines-hidden
    -ffunction-sections
    -fdata-sections
    -Wall
    -Wextra
    -Wpedantic
    -Wno-unused-parameter
)

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
        ${HARDENING_FLAGS}
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
        ${HARDENING_FLAGS}
        ${DEBUG_FLAGS}
    )
endif()

target_link_libraries(omni_backrooms
    ${log-lib}
    ${android-lib}
    ${opensl-lib}
    ${gles-lib}
    ${egl-lib}
    ${dl-lib}
    ${z-lib}
)

set_target_properties(omni_backrooms PROPERTIES
    CXX_VISIBILITY_PRESET     hidden
    VISIBILITY_INLINES_HIDDEN ON
    POSITION_INDEPENDENT_CODE ON
)
