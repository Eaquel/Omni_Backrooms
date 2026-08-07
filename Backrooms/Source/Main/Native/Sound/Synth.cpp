#include "Sound/Synth.h"

#include <algorithm>
#include <cmath>
#include <numbers>

namespace omni {
namespace sound {
namespace {

constexpr float kTwoPi = 2.0f * std::numbers::pi_v<float>;

float lerp(float a, float b, float t) noexcept { return a + (b - a) * t; }

/**
 * The sample index for a time in seconds.
 *
 * Truncating `t * kSynthRate` is wrong and quietly so. `float(i)/44100*44100`
 * does not always land back on i — it lands a hair under — so a plain cast
 * returned i-1 for 347 of the first 4410 samples. Eight per cent of the noise
 * was a repeat of the previous sample, which correlates it and dulls the hiss
 * in a way that is easy to hear and impossible to see. Rounding recovers the
 * index exactly for any t this game plays.
 */
uint32_t sampleIndex(float t) noexcept {
    return static_cast<uint32_t>(std::lround(t * kSynthRate));
}

} // namespace

float hash01(uint32_t n) noexcept {
    n = (n ^ 61u) ^ (n >> 16);
    n = n + (n << 3);
    n ^= n >> 4;
    n = n * 0x27D4EB2Du;
    n ^= n >> 15;
    return static_cast<float>(n & 0xFFFFFFu) / static_cast<float>(0xFFFFFF);
}

float white(uint32_t i) noexcept { return hash01(i) * 2.0f - 1.0f; }

float valueNoise(float x) noexcept {
    const float i = std::floor(x);
    float f = x - i;
    f = f * f * (3.0f - 2.0f * f);
    // The cast to uint32_t must wrap the same way Python's & 0xFFFFFFFF does,
    // which is why the index goes through int32 first: a negative time would
    // otherwise land on a different sample in each language.
    const auto n = static_cast<uint32_t>(static_cast<int32_t>(i));
    return lerp(white(n), white(n + 1u), f);
}

float vhsIntro(float t, float duration) noexcept {
    const uint32_t n = sampleIndex(t);
    const float envIn  = std::min(1.0f, t / 0.35f);
    const float envOut = std::min(1.0f, std::max(0.0f, (duration - t) / 0.6f));
    const float env = envIn * envOut;

    // Transport spinning up: pitch rises and settles.
    const float spin = 1.0f - std::exp(-t * 3.2f);
    const float rumble = std::sin(kTwoPi * (22.0f + 26.0f * spin) * t) * 0.45f * spin;

    // Head contact hiss, shaped by a slow drift so it breathes.
    const float hiss = white(n) * (0.16f + 0.10f * valueNoise(t * 7.0f)) * spin;

    // Dropouts: hard gates, irregular, short.
    const float drop = valueNoise(t * 11.0f + 3.1f) > -0.55f ? 1.0f : 0.12f;

    // Mains hum — 50 Hz and its third, the way a real earth loop sounds.
    const float hum = (std::sin(kTwoPi * 50.0f * t) * 0.09f +
                       std::sin(kTwoPi * 150.0f * t) * 0.035f) * spin;

    // Tape wow: the whole signal's pitch wavers slightly.
    const float wow = 1.0f + valueNoise(t * 1.7f) * 0.012f;
    const float body = std::sin(kTwoPi * 190.0f * t * wow) * 0.08f * spin;

    return (rumble + hiss + hum + body) * drop * env;
}

float fluorescentHum(float t, float health) noexcept {
    const float fail = 1.0f - std::clamp(health, 0.0f, 1.0f);
    const float hum = std::sin(kTwoPi * 100.0f * t) * 0.10f +
                      std::sin(kTwoPi * 300.0f * t) * 0.05f * (0.3f + fail);
    const float buzz = white(sampleIndex(t)) * 0.02f * fail;
    const float stutter = valueNoise(t * 9.0f) > -0.7f + fail * 0.5f ? 1.0f : 0.25f;
    return (hum + buzz) * stutter;
}

float footstep(float t, float pace, float surface) noexcept {
    if (t < 0.0f) return 0.0f;
    const uint32_t n = sampleIndex(t);
    const float decay = std::exp(-t * (34.0f + 22.0f * surface));
    const float thud = std::sin(kTwoPi * (78.0f - 18.0f * surface) * t) * decay;
    const float scuff = white(n) * decay * (0.30f + 0.45f * surface);
    const float click = std::exp(-t * 260.0f) * white(n + 7u) * surface * 0.7f;
    return (thud * 0.6f + scuff * 0.35f + click) * (0.7f + 0.5f * pace);
}

float monsterVoice(float t, float proximity) noexcept {
    const float p = std::clamp(proximity, 0.0f, 1.0f);
    const float f0 = 41.0f + 14.0f * p;
    const float f1 = f0 * 1.4983f;                  // deliberately not a simple ratio
    const float breath = 0.55f + 0.45f * std::sin(kTwoPi * (0.7f + 0.5f * p) * t);
    const float body = std::sin(kTwoPi * f0 * t) * 0.55f +
                       std::sin(kTwoPi * f1 * t) * 0.30f;
    const float grit = white(sampleIndex(t)) * 0.06f * p;
    return (body * breath + grit) * (0.25f + 0.75f * p);
}

} // namespace sound
} // namespace omni
