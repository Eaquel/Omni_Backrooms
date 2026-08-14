#include "Frame/Frame.h"

#include <cmath>
#include <cstring>

namespace omni {
namespace cosmetic {
namespace {

constexpr float kTau = 6.28318530718f;

inline float clamp01(float v) noexcept {
    return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
}

inline float smoothstep01(float x) noexcept {
    const float t = clamp01(x);
    return t * t * (3.0f - 2.0f * t);
}


inline float ringDist(float a, float b) noexcept {
    float d = std::fabs(a - b);
    if (d > 0.5f) d = 1.0f - d;
    return d;
}


inline float bump(float u, float c, float w) noexcept {
    if (w <= 0.0f) return 0.0f;
    return 1.0f - smoothstep01(ringDist(u, c) / w);
}


const FrameSpec kFrames[] = {


    {
        kFaceOfDarkness,
        0.045f, 0.040f, 0.055f,
        1.000f, 0.240f, 0.180f,
        0.620f, 0.560f, 0.600f,
        0.150f, 26.0f
    },


    {
        kEndlessDimension,
        0.075f, 0.105f, 0.145f,
        0.420f, 0.900f, 1.000f,
        0.780f, 0.930f, 1.000f,
        0.115f, 54.0f
    },


    {
        kSoundOfRooms,
        0.230f, 0.200f, 0.125f,
        1.000f, 0.820f, 0.380f,
        1.000f, 0.930f, 0.720f,
        0.130f, 34.0f
    },
};

constexpr int kCount = static_cast<int>(sizeof(kFrames) / sizeof(kFrames[0]));


float silhouetteFaceOfDarkness(float u) noexcept {
    const float a = u * kTau;


    return 1.0f + 0.085f * std::cos(a) - 0.055f * std::cos(2.0f * a);
}


float silhouetteEndlessDimension(float u) noexcept {
    const float a = u * kTau;
    const float sector = kTau / 6.0f;
    const float local = a - sector * std::floor(a / sector) - sector * 0.5f;
    const float hex = std::cos(sector * 0.5f) / std::cos(local);


    return hex * 0.88f + 0.12f;
}


float silhouetteSoundOfRooms(float u) noexcept {
    const float a = u * kTau;
    return 1.0f
         + 0.070f * std::sin(a * 7.0f)
         + 0.045f * std::sin(a * 11.0f + 1.7f)
         + 0.028f * std::sin(a * 17.0f + 0.4f);
}


float emissionFaceOfDarkness(float u, float t) noexcept {



    const float blink = [&] {


        const float g = std::sin(t * 0.9f) * std::sin(t * 0.37f + 1.1f);
        return g > -0.55f ? 1.0f : 0.06f;
    }();

    const float presence = smoothstep01(std::sin(t * 0.31f) * 0.5f + 0.62f);

    const float eyeL = bump(u, 0.375f, 0.045f);
    const float eyeR = bump(u, 0.625f, 0.045f);
    const float eyes = (eyeL + eyeR) * blink;



    const float open = 0.5f * (1.0f + std::sin(t * 0.53f + 0.8f));
    const float grin = bump(u, 0.0f, 0.085f + 0.055f * open);
    const float teeth = 0.55f + 0.45f * (std::sin(u * kTau * 26.0f) > 0.0f ? 1.0f : 0.25f);

    return clamp01((eyes + grin * teeth * 0.85f) * presence);
}

float emissionEndlessDimension(float u, float t) noexcept {



    float sum = 0.0f;
    sum += 0.55f * std::pow(0.5f + 0.5f * std::sin((u * 3.0f - t * 0.21f) * kTau), 6.0f);
    sum += 0.40f * std::pow(0.5f + 0.5f * std::sin((u * 5.0f - t * 0.134f) * kTau), 8.0f);
    sum += 0.30f * std::pow(0.5f + 0.5f * std::sin((u * 8.0f - t * 0.0871f) * kTau), 10.0f);

    return clamp01(0.06f + sum);
}

float emissionSoundOfRooms(float u, float t) noexcept {


    const float env = clamp01(
        0.34f
        + 0.42f * std::sin(t * 1.7f) * std::sin(t * 0.41f)
        + 0.24f * std::sin(t * 3.9f + 2.0f) * std::sin(t * 0.23f));



    const float fromBottom = ringDist(u, 0.0f) * 2.0f;
    const float lit = 1.0f - smoothstep01((fromBottom - env) / 0.06f);


    const float seg = std::sin(u * kTau * 24.0f);
    const float cell = seg > -0.35f ? 1.0f : 0.15f;


    const float peakPos = env * 0.5f;
    const float peak = bump(u, peakPos, 0.02f) + bump(u, 1.0f - peakPos, 0.02f);

    return clamp01(lit * cell * 0.9f + peak * 0.8f);
}

}

int frameCount() noexcept { return kCount; }

const FrameSpec* frameAt(int index) noexcept {
    if (index < 0 || index >= kCount) return nullptr;
    return &kFrames[index];
}

int frameIndexOf(const char* id) noexcept {
    if (id == nullptr) return -1;
    for (int i = 0; i < kCount; ++i) {
        if (std::strcmp(kFrames[i].id, id) == 0) return i;
    }
    return -1;
}

void frameProfile(int index, int samples, float* out) noexcept {
    if (out == nullptr || samples <= 0) return;
    const FrameSpec* spec = frameAt(index);
    if (spec == nullptr) return;


    float widest = 0.0f;
    for (int i = 0; i < samples; ++i) {
        const float u = static_cast<float>(i) / static_cast<float>(samples);
        float r;
        switch (index) {
            case 0:  r = silhouetteFaceOfDarkness(u);   break;
            case 1:  r = silhouetteEndlessDimension(u); break;
            default: r = silhouetteSoundOfRooms(u);     break;
        }
        out[i * 2] = r;
        if (r > widest) widest = r;
    }
    if (widest <= 0.0f) widest = 1.0f;








    float narrowest = 1e9f;
    for (int i = 0; i < samples; ++i) {
        const float r = out[i * 2] / widest;
        if (r < narrowest) narrowest = r;
    }
    const float span = 1.0f - narrowest;

    for (int i = 0; i < samples; ++i) {
        const float u = static_cast<float>(i) / static_cast<float>(samples);
        float r = out[i * 2] / widest;
        if (span > 1e-4f) {
            r = kProfileFloor + (r - narrowest) / span * (1.0f - kProfileFloor);
        } else {
            r = 1.0f;
        }
        out[i * 2] = r;

        float thickness = spec->tubeRatio;
        if (index == 2) {


            thickness *= 1.0f + 0.35f * std::sin(u * kTau * 7.0f);
        } else if (index == 0) {


            thickness *= 1.0f + 0.30f * std::cos(u * kTau);
        }




        const float cap = kInnerClearance * out[i * 2];
        if (thickness > cap) thickness = cap;
        const float room = out[i * 2] - kPortraitClearance;
        if (thickness > room) thickness = room > 0.0f ? room : 0.0f;
        out[i * 2 + 1] = thickness;
    }
}

void frameEmission(int index, int samples, float t, float* out) noexcept {
    if (out == nullptr || samples <= 0) return;
    if (frameAt(index) == nullptr) return;
    for (int i = 0; i < samples; ++i) {
        const float u = static_cast<float>(i) / static_cast<float>(samples);
        switch (index) {
            case 0:  out[i] = emissionFaceOfDarkness(u, t);   break;
            case 1:  out[i] = emissionEndlessDimension(u, t); break;
            default: out[i] = emissionSoundOfRooms(u, t);     break;
        }
    }
}

}
}
