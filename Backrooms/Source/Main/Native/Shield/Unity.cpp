


#include "Shield/Unity.h"

namespace {


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

}


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

}
}
