// The Unity costume — see Shield/Unity.cpp for what it is for and, more importantly,
// what it is not.
//
// One version string, in one place. Assets_Check.py reads it from here and
// asserts the decoy files under Assets/bin/Data carry the same one: a binary
// claiming 2022.3.21f1 next to a boot.config claiming something else is more
// suspicious than no disguise at all.

#ifndef OMNI_SHIELD_UNITY_H
#define OMNI_SHIELD_UNITY_H

namespace omni {
namespace shield {

/** The engine version everything visible from outside must agree on. */
inline constexpr const char* kUnityVersion = "2022.3.21f1";

const char* buildInfo() noexcept;
const char* runtimeStrings() noexcept;
const char* unityVersion() noexcept;

} // namespace shield
} // namespace omni

#endif // OMNI_SHIELD_UNITY_H
