


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

}


OMNI_EXPORT void Java_com_unity3d_player_UnityPlayer_nativeRender(JNIEnv*, jobject) {}
OMNI_EXPORT void Java_com_unity3d_player_UnityPlayer_nativePause(JNIEnv*, jobject) {}
OMNI_EXPORT void Java_com_unity3d_player_UnityPlayer_nativeResume(JNIEnv*, jobject) {}
OMNI_EXPORT void Java_com_unity3d_player_UnityPlayer_nativeQuit(JNIEnv*, jobject) {}
OMNI_EXPORT void Java_com_unity3d_player_UnityPlayer_nativeFocusChanged(JNIEnv*, jobject, jboolean) {}
OMNI_EXPORT jboolean Java_com_unity3d_player_UnityPlayer_nativeInit(JNIEnv*, jobject) { return JNI_FALSE; }


OMNI_EXPORT int  UnityPlayerLoop()                     { return 0; }
OMNI_EXPORT void UnityPlayerPause()                    {}
OMNI_EXPORT void UnityPlayerResume()                   {}
OMNI_EXPORT void UnityPlayerQuit()                     {}
OMNI_EXPORT void UnitySendMessage(const char*, const char*, const char*) {}
OMNI_EXPORT const char* UnityGetVersion()              { return omni::shield::kUnityVersion; }
OMNI_EXPORT const char* UnityPlayerStrings()           { return kPlayerStrings; }

OMNI_EXPORT jint JNI_OnLoad(JavaVM*, void*) { return JNI_VERSION_1_6; }
