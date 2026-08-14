#include "Sound/Synth.h"

#include <algorithm>
#include <cmath>
#include <numbers>

namespace omni {
namespace sound {
namespace {

constexpr float kTwoPi = 2.0f * std::numbers::pi_v<float>;

float lerp(float a, float b, float t) noexcept { return a + (b - a) * t; }


uint32_t sampleIndex(float t) noexcept {
    return static_cast<uint32_t>(std::lround(t * kSynthRate));
}

}

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



    const auto n = static_cast<uint32_t>(static_cast<int32_t>(i));
    return lerp(white(n), white(n + 1u), f);
}

float vhsIntro(float t, float duration) noexcept {
    const uint32_t n = sampleIndex(t);
    const float envIn  = std::min(1.0f, t / 0.35f);
    const float envOut = std::min(1.0f, std::max(0.0f, (duration - t) / 0.6f));
    const float env = envIn * envOut;


    const float spin = 1.0f - std::exp(-t * 3.2f);
    const float rumble = std::sin(kTwoPi * (22.0f + 26.0f * spin) * t) * 0.45f * spin;


    const float hiss = white(n) * (0.16f + 0.10f * valueNoise(t * 7.0f)) * spin;


    const float drop = valueNoise(t * 11.0f + 3.1f) > -0.55f ? 1.0f : 0.12f;


    const float hum = (std::sin(kTwoPi * 50.0f * t) * 0.09f +
                       std::sin(kTwoPi * 150.0f * t) * 0.035f) * spin;


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

float footstep(float t, float pace, float surface, float step) noexcept {
    if (t < 0.0f || t > 0.42f) return 0.0f;
    const uint32_t n = sampleIndex(t);
    const float p = std::clamp(pace, 0.0f, 1.0f);
    const float s = std::clamp(surface, 0.0f, 1.0f);










    const auto si = static_cast<uint32_t>(step);
    const float j0 = hash01(si * 2654435761u);
    const float j1 = hash01(si * 40503u + 17u);
    const float j2 = hash01(si * 2246822519u + 5u);





    const float f0 = (54.0f + 14.0f * s) * (0.90f + 0.20f * j0);
    const float bodyDecay = (13.0f + 9.0f * s) * (0.88f + 0.24f * j1);
    const float bodyEnv = std::exp(-t * bodyDecay) * (1.0f - std::exp(-t * 900.0f));
    const float body = std::sin(kTwoPi * f0 * t * (1.0f - 0.35f * t)) * bodyEnv;



    const float tt = t - (0.026f + 0.010f * j2);
    const float toeEnv = tt > 0.0f
                       ? std::exp(-tt * (bodyDecay * 1.9f)) * (1.0f - std::exp(-tt * 1400.0f))
                       : 0.0f;
    const float toe = std::sin(kTwoPi * f0 * 1.6f * tt) * toeEnv * 0.42f;




    float lp = 0.0f;
    for (uint32_t k = 0; k < 8; ++k) lp += white(n - k + si * 977u);
    lp /= 8.0f;
    const float scuff = lp * std::exp(-t * (26.0f + 14.0f * s)) * (0.22f + 0.30f * s);




    const float click = std::exp(-t * 300.0f) * white(n + 7u + si * 31u) * s * s * 0.55f;

    const float gain = (0.62f + 0.45f * p) * (0.86f + 0.28f * j2);
    return (body * 0.72f + toe + scuff + click) * gain;
}

float monsterVoice(float t, float proximity) noexcept {
    const float p = std::clamp(proximity, 0.0f, 1.0f);
    const float f0 = 41.0f + 14.0f * p;
    const float f1 = f0 * 1.4983f;
    const float breath = 0.55f + 0.45f * std::sin(kTwoPi * (0.7f + 0.5f * p) * t);
    const float body = std::sin(kTwoPi * f0 * t) * 0.55f +
                       std::sin(kTwoPi * f1 * t) * 0.30f;
    const float grit = white(sampleIndex(t)) * 0.06f * p;
    return (body * breath + grit) * (0.25f + 0.75f * p);
}

float roomTone(float t, float damp) noexcept {
    const uint32_t n = sampleIndex(t);
    const float d = std::clamp(damp, 0.0f, 1.0f);




    const float drone = std::sin(kTwoPi * 47.0f * t) * 0.055f +
                        std::sin(kTwoPi * 47.2f * t) * 0.045f;





    float lp = 0.0f;
    for (uint32_t k = 0; k < 12; ++k) lp += white(n - k);
    lp /= 12.0f;
    const float air = lp * 0.085f;


    const float top = white(n + 991u) * 0.012f;



    const float cyc = t - 3.4f * std::floor(t / 3.4f);
    const float ring = std::exp(-cyc * 26.0f);
    const float drip = std::sin(kTwoPi * (1180.0f - 260.0f * cyc) * cyc) * ring * 0.16f * d;

    return drone + air + top + drip;
}

float distantEvent(float t) noexcept {











    constexpr float kPeriod = 17.3f;

    const float idx = std::floor(t / kPeriod);
    const float u   = t - idx * kPeriod;



    const uint32_t e = static_cast<uint32_t>(static_cast<int32_t>(idx));
    const float when = 1.5f + hash01(e * 2654435761u) * 11.0f;
    const float dt   = u - when;
    if (dt < 0.0f || dt > 3.2f) return 0.0f;

    const uint32_t n = sampleIndex(t);
    const int kind = static_cast<int>(hash01(e * 40503u + 11u) * 4.0f) & 3;




    float lp = 0.0f;
    for (uint32_t k = 0; k < 24; ++k) lp += white(n - k + e * 7919u);
    lp /= 24.0f;

    float sig = 0.0f;
    if (kind == 0) {

        const float a = std::exp(-dt * 14.0f) * (1.0f - std::exp(-dt * 700.0f));
        const float b = dt > 0.09f ? std::exp(-(dt - 0.09f) * 9.0f) * 0.55f : 0.0f;
        sig = (std::sin(kTwoPi * 96.0f * dt) * a + std::sin(kTwoPi * 61.0f * dt) * b) * 0.5f
            + lp * (a + b) * 0.30f;
    } else if (kind == 1) {

        const float a = std::exp(-dt * 6.0f) * (1.0f - std::exp(-dt * 900.0f));
        sig = (std::sin(kTwoPi * 214.0f * dt) * 0.6f +
               std::sin(kTwoPi * 337.0f * dt) * 0.3f) * a * 0.42f;
    } else if (kind == 2) {

        const float env = std::sin(std::numbers::pi_v<float> * std::min(dt / 1.4f, 1.0f));
        sig = lp * env * 0.34f;
    } else {

        const float env = std::sin(std::numbers::pi_v<float> * std::min(dt / 2.6f, 1.0f));
        sig = (std::sin(kTwoPi * 38.0f * dt) * 0.5f +
               std::sin(kTwoPi * 57.3f * dt) * 0.25f) * env * 0.40f;
    }

    return sig * 0.55f;
}

float breath(float t, float exertion) noexcept {



    const float e = std::clamp(exertion, 0.0f, 1.0f);
    const float rate = 0.30f + 0.85f * e;
    const float ph = t * rate - std::floor(t * rate);

    const float in  = std::sin(std::numbers::pi_v<float> * std::min(ph / 0.55f, 1.0f));
    const float out = ph > 0.55f
                    ? std::sin(std::numbers::pi_v<float> * (ph - 0.55f) / 0.45f)
                    : 0.0f;
    const float env = in * 0.55f + out * 1.0f;



    const uint32_t n = sampleIndex(t);
    float lp = 0.0f;
    for (uint32_t k = 0; k < 5; ++k) lp += white(n - k);
    lp /= 5.0f;
    const float formant = std::sin(kTwoPi * 620.0f * t) * 0.25f +
                          std::sin(kTwoPi * 1180.0f * t) * 0.12f;

    return (lp * 0.7f + lp * formant) * env * (0.12f + 0.5f * e);
}

float heartbeat(float t, float fear) noexcept {



    const float f = std::clamp(fear, 0.0f, 1.0f);
    const float bpm = 58.0f + 62.0f * f;
    const float period = 60.0f / bpm;
    const float ph = (t - period * std::floor(t / period)) / period;

    auto thump = [](float u, float gain) noexcept -> float {
        if (u < 0.0f) return 0.0f;
        const float env = std::exp(-u * 26.0f) * (1.0f - std::exp(-u * 420.0f));
        return std::sin(kTwoPi * (52.0f - 20.0f * u) * u) * env * gain;
    };
    const float lub = thump(ph * period, 1.0f);
    const float dub = thump((ph - 0.22f) * period, 0.62f);
    return (lub + dub) * (0.15f + 0.85f * f);
}

float torchClick(float t) noexcept {


    if (t < 0.0f || t > 0.06f) return 0.0f;
    const uint32_t n = sampleIndex(t);
    const float snap = white(n) * std::exp(-t * 620.0f);
    const float ring = std::sin(kTwoPi * 2400.0f * t) * std::exp(-t * 150.0f) * 0.35f;
    return (snap * 0.8f + ring) * 0.5f;
}

}
}
