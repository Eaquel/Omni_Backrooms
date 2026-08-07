// ============================================================================
// The Unity costume.
//
// Be clear about what this is and is not.
//
// It is not protection. Anyone who unzips the APK and looks properly will see a
// 300 KB libil2cpp.so where a real one would be twenty megabytes, a
// global-metadata.dat measured in hundreds of bytes rather than hundreds of
// kilobytes, and no Unity classes in the dex at all. Five minutes with any
// decompiler ends the illusion, and nothing here would stop somebody who has
// decided to attack the game.
//
// What it is: a filter on the front door. The people who go looking for a
// game's memory offsets overwhelmingly work from tooling that fingerprints the
// engine first and then loads a preset — a Unity preset, an Unreal preset, a
// Godot preset. Presenting as Unity sends that tooling down a path with no
// exit: there is no Mono runtime to walk, no IL2CPP metadata to resolve, no
// UnityPlayerActivity to hook. The cheats do not fail loudly, they fail
// confusingly, and the marginal attacker gives up rather than starting over
// from scratch against something they now have to identify themselves.
//
// So the goal is coherence, not depth: everything visible from the outside
// should agree on one story, and the story should be a boring, ordinary
// Unity 2022 LTS Android build.
//
// This file is what makes libil2cpp.so itself carry that story. The decoy
// assets under Assets/bin/Data are the other half, and Assets_Check.py asserts
// both stay in step.
// ============================================================================

#include "Shield/Unity.h"

namespace {

// -O3 with --gc-sections and --strip-all will remove anything nothing reaches.
// `used` keeps the symbol, `retain` keeps its section, and putting them in a
// named section keeps them contiguous so they read as one block in `strings`
// rather than as text scattered through the binary.
#define OMNI_KEEP __attribute__((used, retain, section(".rodata.unity")))

OMNI_KEEP const char kBuildInfo[] =
    "Unity Version: 2022.3.21f1 (a2b2e1b8b0d5)\n"
    "Build Type: Release\n"
    "Scripting Backend: IL2CPP\n"
    "Api Compatibility Level: NET Standard 2.1\n"
    "Target Architecture: ARM64\n"
    "Graphics API: Vulkan, OpenGLES3\n"
    "Stripping Level: Medium\n";

OMNI_KEEP const char kRuntimeStrings[] =
    "il2cpp: Method not found\0"
    "il2cpp: Type load exception\0"
    "IL2CPP encountered a managed exception\0"
    "UnityEngine.CoreModule\0"
    "Assembly-CSharp\0"
    "libil2cpp/vm/MetadataCache.cpp\0"
    "libil2cpp/vm/Runtime.cpp\0"
    "libil2cpp/os/Android/ThreadImpl.cpp\0"
    "GlobalMetadata sanity check failed\0"
    "The file 'global-metadata.dat' could not be loaded\0";

} // namespace

// The exported surface. A real libil2cpp.so exports the whole il2cpp_* C API,
// because libunity.so calls into it; a tool that resolves these and finds them
// is looking at what it expects to be looking at. They are deliberately inert —
// il2cpp_init returning 0 is "already initialised", which is the quietest
// possible answer.
//
// Everything else in this project is compiled -fvisibility=hidden, so these
// need the attribute explicitly. That is the point: this is the only part of
// the binary that is meant to be found.
#define OMNI_EXPORT extern "C" __attribute__((visibility("default"), used))

OMNI_EXPORT int  il2cpp_init(const char*)                 { return 0; }
OMNI_EXPORT int  il2cpp_init_utf16(const wchar_t*)        { return 0; }
OMNI_EXPORT void il2cpp_shutdown()                        {}
OMNI_EXPORT void il2cpp_set_config_dir(const char*)       {}
OMNI_EXPORT void il2cpp_set_data_dir(const char*)         {}
OMNI_EXPORT void* il2cpp_thread_attach(void*)             { return nullptr; }
OMNI_EXPORT void il2cpp_thread_detach(void*)              {}
OMNI_EXPORT void* il2cpp_domain_get()                     { return nullptr; }
OMNI_EXPORT void* il2cpp_domain_assembly_open(void*, const char*) { return nullptr; }
OMNI_EXPORT void* il2cpp_class_from_name(void*, const char*, const char*) { return nullptr; }
OMNI_EXPORT void* il2cpp_class_get_method_from_name(void*, const char*, int) { return nullptr; }
OMNI_EXPORT void* il2cpp_runtime_invoke(void*, void*, void**, void**) { return nullptr; }
OMNI_EXPORT void* il2cpp_object_new(void*)                { return nullptr; }
OMNI_EXPORT void* il2cpp_string_new(const char*)          { return nullptr; }
OMNI_EXPORT void il2cpp_gc_disable()                      {}
OMNI_EXPORT void il2cpp_gc_enable()                       {}
OMNI_EXPORT unsigned il2cpp_gc_get_used_size()            { return 0; }
OMNI_EXPORT unsigned il2cpp_gc_get_heap_size()            { return 0; }

namespace omni {
namespace shield {

const char* buildInfo() noexcept    { return kBuildInfo; }
const char* runtimeStrings() noexcept { return kRuntimeStrings; }
const char* unityVersion() noexcept { return kUnityVersion; }

} // namespace shield
} // namespace omni
