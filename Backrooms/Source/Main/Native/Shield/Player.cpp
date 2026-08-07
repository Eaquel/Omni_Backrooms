// ============================================================================
// libunity.so.
//
// The costume's second layer, and the only part of it that is a real artefact
// rather than a string: an actual ELF shared object, sitting in lib/arm64-v8a/
// next to libil2cpp.so, exporting what a Unity player exports.
//
// It matters more than its size suggests. libil2cpp.so alone is a file with a
// suggestive name; libil2cpp.so beside libunity.so is a pair, and the pair is
// what every Unity APK on a phone actually looks like. Tools that fingerprint
// by listing lib/ stop at the first match.
//
// Nothing in the app loads this. It is packaged and never opened, which is
// exactly what makes it safe: it cannot break the game, and if it is ever
// dlopen'd by something else, every entry point returns the value that means
// "nothing happened".
// ============================================================================

#include "Shield/Unity.h"

#include <jni.h>

#define OMNI_EXPORT extern "C" __attribute__((visibility("default"), used))
#define OMNI_KEEP   __attribute__((used, retain, section(".rodata.unity")))

namespace {

OMNI_KEEP const char kPlayerStrings[] =
    "UnityPlayer\0"
    "com/unity3d/player/UnityPlayer\0"
    "com/unity3d/player/UnityPlayerActivity\0"
    "AndroidPlayer\0"
    "Unity Technologies\0"
    "PlayerPrefs\0"
    "Unable to find main\0"
    "Could not allocate memory: System out of memory!\0"
    "GfxDevice: creating device client; threaded=1\0"
    "Initialize engine version: 2022.3.21f1\0"
    "Renderer: Adreno (TM) 730\0"
    "Vendor:   Qualcomm\0"
    "Version:  OpenGL ES 3.2 V@0676.0\0";

} // namespace

// The JNI surface com.unity3d.player.UnityPlayer binds to. The names are the
// point; the bodies are deliberately nothing.
OMNI_EXPORT void Java_com_unity3d_player_UnityPlayer_nativeRender(JNIEnv*, jobject) {}
OMNI_EXPORT void Java_com_unity3d_player_UnityPlayer_nativePause(JNIEnv*, jobject) {}
OMNI_EXPORT void Java_com_unity3d_player_UnityPlayer_nativeResume(JNIEnv*, jobject) {}
OMNI_EXPORT void Java_com_unity3d_player_UnityPlayer_nativeQuit(JNIEnv*, jobject) {}
OMNI_EXPORT void Java_com_unity3d_player_UnityPlayer_nativeFocusChanged(JNIEnv*, jobject, jboolean) {}
OMNI_EXPORT jboolean Java_com_unity3d_player_UnityPlayer_nativeInit(JNIEnv*, jobject) { return JNI_FALSE; }

// The C entry points the player library exposes to native plugins.
OMNI_EXPORT int  UnityPlayerLoop()                     { return 0; }
OMNI_EXPORT void UnityPlayerPause()                    {}
OMNI_EXPORT void UnityPlayerResume()                   {}
OMNI_EXPORT void UnityPlayerQuit()                     {}
OMNI_EXPORT void UnitySendMessage(const char*, const char*, const char*) {}
OMNI_EXPORT const char* UnityGetVersion()              { return omni::shield::kUnityVersion; }
OMNI_EXPORT const char* UnityPlayerStrings()           { return kPlayerStrings; }

OMNI_EXPORT jint JNI_OnLoad(JavaVM*, void*) { return JNI_VERSION_1_6; }
