


#ifndef OMNI_SHIELD_UNITY_H
#define OMNI_SHIELD_UNITY_H

namespace omni {
namespace shield {


inline constexpr const char* kUnityVersion = "2022.3.21f1";

const char* buildInfo() noexcept;
const char* runtimeStrings() noexcept;
const char* unityVersion() noexcept;

}
}

#endif
