cmake_minimum_required(VERSION 4.3.2)

project("omni_backrooms" CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

find_library(log-lib     log)
find_library(android-lib android)
find_library(opensl-lib  OpenSLES)
find_library(gles-lib    GLESv3)
find_library(egl-lib     EGL)
find_library(dl-lib      dl)

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

target_compile_options(omni_backrooms PRIVATE
    -Wall -Wextra -O2
    -fvisibility=hidden
    -fstack-protector-strong
    -D_FORTIFY_SOURCE=2
)

target_link_options(omni_backrooms PRIVATE
    -Wl,-z,relro
    -Wl,-z,now
    -Wl,-z,noexecstack
)

target_link_libraries(omni_backrooms
    ${log-lib}
    ${android-lib}
    ${opensl-lib}
    ${gles-lib}
    ${egl-lib}
    ${dl-lib}
)
