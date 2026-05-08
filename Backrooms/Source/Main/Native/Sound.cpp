#include <jni.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <atomic>
#include <cmath>
#include <cstdint>
#include <functional>
#include <memory>
#include <mutex>
#include <numbers>
#include <optional>
#include <queue>
#include <random>
#include <ranges>
#include <span>
#include <thread>
#include <unordered_map>
#include <vector>

#define TAG "OmniSound"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace omni::sound {

constexpr int   kRate      = 44100;
constexpr int   kFrames    = 2048;
constexpr float kTwoPi    = 2.0f * std::numbers::pi_v<float>;
constexpr float kInv32767  = 1.0f / 32767.0f;

struct Vec3f { float x, y, z; };

[[nodiscard]] float dot(const Vec3f& a, const Vec3f& b) noexcept {
    return a.x * b.x + a.y * b.y + a.z * b.z;
}
[[nodiscard]] float length(const Vec3f& v) noexcept { return std::sqrt(dot(v, v)); }
[[nodiscard]] Vec3f normalize(const Vec3f& v) noexcept {
    float l = length(v);
    return l > 1e-6f ? Vec3f{v.x/l, v.y/l, v.z/l} : Vec3f{0,0,0};
}

struct SpatialParams {
    Vec3f listenerPos{0, 0, 0};
    Vec3f listenerForward{0, 0, -1};
    Vec3f listenerUp{0, 1, 0};
    float rolloffFactor = 1.0f;
    float maxDistance   = 50.0f;
    float refDistance   = 1.0f;
};

[[nodiscard]] std::pair<float,float> computePanning(
        const Vec3f& sourcePos, const SpatialParams& sp) noexcept {
    Vec3f delta{ sourcePos.x - sp.listenerPos.x,
                 sourcePos.y - sp.listenerPos.y,
                 sourcePos.z - sp.listenerPos.z };
    float dist = length(delta);
    float attenuation = sp.refDistance / std::max(
        sp.refDistance, std::min(dist, sp.maxDistance) * sp.rolloffFactor);

    Vec3f right{ sp.listenerForward.z, 0.0f, -sp.listenerForward.x };
    float pan   = dot(normalize(delta), normalize(right));
    float gainL = std::sqrt(0.5f * (1.0f - pan)) * attenuation;
    float gainR = std::sqrt(0.5f * (1.0f + pan)) * attenuation;
    return { gainL, gainR };
}

class LowPassFilter {
public:
    explicit LowPassFilter(float cutoffNorm = 0.5f) { setCutoff(cutoffNorm); }
    void setCutoff(float c) noexcept {
        float rc = 1.0f / (kTwoPi * c);
        alpha_ = 1.0f / (1.0f + rc);
    }
    float process(float x) noexcept {
        y_ = alpha_ * x + (1.0f - alpha_) * y_;
        return y_;
    }
    void reset() noexcept { y_ = 0.0f; }
private:
    float alpha_ = 0.5f, y_ = 0.0f;
};

class ReverbTail {
public:
    ReverbTail() {
        for (auto& b : delays_) b.assign(kMaxDelay, 0.0f);
    }
    float process(float in) noexcept {
        float out = 0.0f;
        for (size_t i = 0; i < delays_.size(); ++i) {
            size_t& idx = heads_[i];
            float delayed = delays_[i][idx];
            out += delayed * decays_[i];
            delays_[i][idx] = in + delayed * feedback_;
            idx = (idx + 1) % delays_[i].size();
        }
        return out * 0.25f;
    }
    void setRoomSize(float s) noexcept {
        feedback_ = std::clamp(s * 0.7f, 0.0f, 0.95f);
    }
private:
    static constexpr int kMaxDelay = 4096;
    std::array<std::vector<float>, 4> delays_;
    std::array<size_t, 4>  heads_  { 0, 0, 0, 0 };
    std::array<float, 4>   decays_ { 0.85f, 0.82f, 0.79f, 0.76f };
    float feedback_ = 0.5f;
};

class HumGenerator {
public:
    HumGenerator() = default;

    void fill(std::span<float> out) noexcept {
        for (auto& s : out) {
            float h = hum();
            float b = buzz();
            float c = crackle();
            float e = electrical();
            float raw = h * 0.5f + b * 0.25f + c * 0.08f + e * 0.17f;
            s = lpf_.process(raw) + reverb_.process(raw * 0.3f);
        }
    }

    void setVolume(float v) noexcept { volume_ = std::clamp(v, 0.0f, 1.0f); }
    [[nodiscard]] float volume() const noexcept { return volume_; }

private:
    float hum() noexcept {
        float v = std::sin(hp_) * 0.7f
                + std::sin(hp_ * 3.0f) * 0.15f
                + std::sin(hp_ * 5.0f) * 0.08f
                + std::sin(hp_ * 7.0f) * 0.04f
                + std::sin(hp_ * 9.0f) * 0.03f;
        hp_ += kTwoPi * 60.0f / kRate;
        if (hp_ > kTwoPi) hp_ -= kTwoPi;
        return v;
    }

    float buzz() noexcept {
        float v   = std::sin(bp_) * 0.6f + std::sin(bp_ * 2.0f) * 0.4f;
        float mod = (std::sin(bp_ * 0.07f) + 1.0f) * 0.5f;
        float am  = 1.0f + 0.12f * std::sin(bp_ * 0.003f);
        bp_ += kTwoPi * 120.0f / kRate;
        if (bp_ > kTwoPi) bp_ -= kTwoPi;
        return v * mod * am;
    }

    float crackle() noexcept {
        if (++ct_ > ci_) {
            ct_ = 0;
            ci_ = static_cast<int>(dist_(rng_) * 22050 + 4410);
            ca_ = true; cs_ = 0;
        }
        if (!ca_) return 0.0f;
        float env = std::exp(-cs_++ * 0.004f);
        float n   = dist_(rng_) * 2.0f - 1.0f;
        if (cs_ > 441) ca_ = false;
        return n * env;
    }

    float electrical() noexcept {
        float v = std::sin(ep_) * (0.4f + 0.1f * std::sin(ep_ * 0.001f));
        ep_ += kTwoPi * 180.0f / kRate;
        if (ep_ > kTwoPi) ep_ -= kTwoPi;
        return v * (dist_(rng_) > 0.97f ? 1.5f : 1.0f);
    }

    float hp_ = 0.0f, bp_ = 0.0f, ep_ = 0.0f;
    float volume_ = 0.3f;
    int   ct_ = 0, ci_ = 8820, cs_ = 0;
    bool  ca_ = false;
    std::mt19937 rng_{42u};
    std::uniform_real_distribution<float> dist_{0.0f, 1.0f};
    LowPassFilter lpf_{0.08f};
    ReverbTail    reverb_;
};

struct FootstepSynth {
    std::mt19937 rng{123u};
    std::uniform_real_distribution<float> nd{0.0f, 1.0f};
    int   samplePos  = 0;
    int   stepLen    = 0;
    bool  active     = false;
    float surfaceMix = 0.5f;

    void trigger(float bpm, float surface) noexcept {
        stepLen    = static_cast<int>(kRate * 60.0f / bpm);
        samplePos  = 0;
        active     = true;
        surfaceMix = std::clamp(surface, 0.0f, 1.0f);
    }

    float next() noexcept {
        if (!active) return 0.0f;
        float t   = static_cast<float>(samplePos) / stepLen;
        float env = std::exp(-t * 18.0f);
        float hit = (nd(rng) * 2.0f - 1.0f) * env;
        float carpet = hit * (1.0f - surfaceMix) * std::exp(-t * 30.0f);
        float hard   = hit * surfaceMix * (1.0f + 0.5f * std::sin(t * 800.0f));
        if (++samplePos >= stepLen) active = false;
        return (carpet + hard) * 0.4f;
    }
};

struct MonsterSynth {
    float phase    = 0.0f;
    float modPhase = 0.0f;
    float intensity= 0.0f;
    bool  active   = false;
    LowPassFilter lpf{0.06f};
    ReverbTail    rev;
    std::mt19937  rng{77u};
    std::uniform_real_distribution<float> nd{0.0f, 1.0f};

    void trigger(float i) noexcept { intensity = std::clamp(i, 0.0f, 1.0f); active = true; }
    void stop()  noexcept { active = false; }

    float next() noexcept {
        if (!active) return 0.0f;
        float mod = std::sin(modPhase) * intensity * 0.4f;
        float raw = std::sin(phase + mod) * 0.6f
                  + std::sin(phase * 2.3f + mod * 1.5f) * 0.3f
                  + (nd(rng) * 2.0f - 1.0f) * 0.1f * intensity;
        phase    += kTwoPi * (55.0f + intensity * 40.0f) / kRate;
        modPhase += kTwoPi * 1.8f / kRate;
        if (phase    > kTwoPi) phase    -= kTwoPi;
        if (modPhase > kTwoPi) modPhase -= kTwoPi;
        return rev.process(lpf.process(raw)) * intensity;
    }
};

struct AmbienceLayer {
    float phase1 = 0.0f, phase2 = 0.0f, phase3 = 0.0f;
    float breathPhase = 0.0f;
    std::mt19937 rng{55u};
    std::uniform_real_distribution<float> nd{0.0f, 1.0f};
    LowPassFilter lpf{0.04f};
    float level = 0.0f;

    void setLevel(float l) noexcept { level = std::clamp(l, 0.0f, 1.0f); }

    float next() noexcept {
        float drone = std::sin(phase1) * 0.5f
                    + std::sin(phase2) * 0.3f
                    + std::sin(phase3) * 0.2f;
        float breath = (std::sin(breathPhase) + 1.0f) * 0.5f;
        float n = (nd(rng) * 2.0f - 1.0f) * 0.05f;

        phase1     += kTwoPi * 38.0f / kRate;
        phase2     += kTwoPi * 57.3f / kRate;
        phase3     += kTwoPi * 76.1f / kRate;
        breathPhase+= kTwoPi * 0.12f / kRate;

        if (phase1 > kTwoPi) phase1 -= kTwoPi;
        if (phase2 > kTwoPi) phase2 -= kTwoPi;
        if (phase3 > kTwoPi) phase3 -= kTwoPi;
        if (breathPhase > kTwoPi) breathPhase -= kTwoPi;

        return lpf.process(drone * breath + n) * level;
    }
};

struct MixBus {
    std::atomic<float> masterGain{1.0f};
    std::atomic<float> humGain   {0.3f};
    std::atomic<float> footGain  {0.8f};
    std::atomic<float> monsterGain{0.9f};
    std::atomic<float> ambienceGain{0.5f};
    std::atomic<float> musicGain  {0.7f};
    LowPassFilter masterLpf{0.48f};

    float mix(float hum, float foot, float monster, float ambience) noexcept {
        float s = hum      * humGain.load()
                + foot     * footGain.load()
                + monster  * monsterGain.load()
                + ambience * ambienceGain.load();
        s *= masterGain.load();
        return masterLpf.process(std::tanh(s * 1.2f));
    }
};

struct Engine {
    SLObjectItf              obj       = nullptr;
    SLEngineItf              engine    = nullptr;
    SLObjectItf              mixObj    = nullptr;
    SLObjectItf              playerObj = nullptr;
    SLPlayItf                player    = nullptr;
    SLAndroidSimpleBufferQueueItf queue = nullptr;

    HumGenerator   hum;
    FootstepSynth  foot;
    MonsterSynth   monster;
    AmbienceLayer  ambience;
    MixBus         bus;
    SpatialParams  spatial;

    std::vector<float>   mixBuf;
    std::vector<int16_t> outBuf;
    std::atomic<bool>    running{false};
    std::mutex           mtx;
};

} // namespace omni::sound

static omni::sound::Engine gSound;

static void onQueue(SLAndroidSimpleBufferQueueItf bq, void*) {
    using namespace omni::sound;
    if (!gSound.running.load()) return;

    std::lock_guard<std::mutex> lock(gSound.mtx);
    auto& mb  = gSound.mixBuf;
    auto& ob  = gSound.outBuf;

    std::vector<float> humBuf(kFrames), footBuf(kFrames), monBuf(kFrames), ambBuf(kFrames);
    gSound.hum.fill(humBuf);

    for (int i = 0; i < kFrames; ++i) {
        footBuf[i] = gSound.foot.next();
        monBuf[i]  = gSound.monster.next();
        ambBuf[i]  = gSound.ambience.next();
        float s    = gSound.bus.mix(
            humBuf[i] * gSound.hum.volume(),
            footBuf[i], monBuf[i], ambBuf[i]);
        int16_t pcm = static_cast<int16_t>(std::clamp(s * 32767.0f, -32767.0f, 32767.0f));
        ob[i * 2]     = pcm;
        ob[i * 2 + 1] = pcm;
    }

    (*bq)->Enqueue(bq, ob.data(), static_cast<SLuint32>(ob.size() * sizeof(int16_t)));
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_omni_backrooms_Native_1Bridge_initSound(JNIEnv*, jobject) {
    using namespace omni::sound;
    gSound.mixBuf.assign(kFrames, 0.0f);
    gSound.outBuf.assign(kFrames * 2, 0);
    gSound.ambience.setLevel(0.4f);

    if (slCreateEngine(&gSound.obj, 0, nullptr, 0, nullptr, nullptr) != SL_RESULT_SUCCESS) {
        LOGE("slCreateEngine failed"); return JNI_FALSE;
    }
    (*gSound.obj)->Realize(gSound.obj, SL_BOOLEAN_FALSE);
    (*gSound.obj)->GetInterface(gSound.obj, SL_IID_ENGINE, &gSound.engine);

    const SLInterfaceID mIds[] = {};
    const SLboolean     mReq[] = {};
    (*gSound.engine)->CreateOutputMix(gSound.engine, &gSound.mixObj, 0, mIds, mReq);
    (*gSound.mixObj)->Realize(gSound.mixObj, SL_BOOLEAN_FALSE);

    SLDataLocator_AndroidSimpleBufferQueue bqLoc{ SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2 };
    SLDataFormat_PCM fmt{
        SL_DATAFORMAT_PCM, 2, SL_SAMPLINGRATE_44_1,
        SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
        SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT, SL_BYTEORDER_LITTLEENDIAN
    };
    SLDataSource src{ &bqLoc, &fmt };
    SLDataLocator_OutputMix outLoc{ SL_DATALOCATOR_OUTPUTMIX, gSound.mixObj };
    SLDataSink   sink{ &outLoc, nullptr };
    const SLInterfaceID ids[] = { SL_IID_ANDROIDSIMPLEBUFFERQUEUE };
    const SLboolean     req[] = { SL_BOOLEAN_TRUE };

    (*gSound.engine)->CreateAudioPlayer(gSound.engine, &gSound.playerObj, &src, &sink, 1, ids, req);
    (*gSound.playerObj)->Realize(gSound.playerObj, SL_BOOLEAN_FALSE);
    (*gSound.playerObj)->GetInterface(gSound.playerObj, SL_IID_PLAY,  &gSound.player);
    (*gSound.playerObj)->GetInterface(gSound.playerObj, SL_IID_ANDROIDSIMPLEBUFFERQUEUE, &gSound.queue);
    (*gSound.queue)->RegisterCallback(gSound.queue, onQueue, nullptr);
    (*gSound.player)->SetPlayState(gSound.player, SL_PLAYSTATE_PLAYING);

    gSound.running.store(true);
    onQueue(gSound.queue, nullptr);
    LOGI("Sound engine initialized (stereo 44100 Hz, %d frames)", kFrames);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_setMasterVolume(JNIEnv*, jobject, jfloat v) {
    gSound.bus.masterGain.store(std::clamp(v, 0.0f, 1.0f));
}
JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_setHumVolume(JNIEnv*, jobject, jfloat v) {
    gSound.hum.setVolume(v);
    gSound.bus.humGain.store(std::clamp(v, 0.0f, 1.0f));
}
JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_setFootstepVolume(JNIEnv*, jobject, jfloat v) {
    gSound.bus.footGain.store(std::clamp(v, 0.0f, 1.0f));
}
JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_setMonsterVolume(JNIEnv*, jobject, jfloat v) {
    gSound.bus.monsterGain.store(std::clamp(v, 0.0f, 1.0f));
}
JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_setAmbienceLevel(JNIEnv*, jobject, jfloat v) {
    gSound.ambience.setLevel(v);
}
JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_triggerFootstep(JNIEnv*, jobject, jfloat bpm, jfloat surface) {
    std::lock_guard<std::mutex> lock(gSound.mtx);
    gSound.foot.trigger(bpm, surface);
}
JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_triggerMonster(JNIEnv*, jobject, jfloat intensity) {
    std::lock_guard<std::mutex> lock(gSound.mtx);
    gSound.monster.trigger(intensity);
}
JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_stopMonster(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(gSound.mtx);
    gSound.monster.stop();
}
JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_setListenerPos(JNIEnv*, jobject, jfloat x, jfloat y, jfloat z) {
    std::lock_guard<std::mutex> lock(gSound.mtx);
    gSound.spatial.listenerPos = {x, y, z};
}
JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_setSpatialRolloff(JNIEnv*, jobject, jfloat ref, jfloat maxDist) {
    std::lock_guard<std::mutex> lock(gSound.mtx);
    gSound.spatial.refDistance = ref;
    gSound.spatial.maxDistance = maxDist;
}
JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_destroySound(JNIEnv*, jobject) {
    gSound.running.store(false);
    std::this_thread::sleep_for(std::chrono::milliseconds(80));
    if (gSound.playerObj) { (*gSound.playerObj)->Destroy(gSound.playerObj); gSound.playerObj = nullptr; }
    if (gSound.mixObj)    { (*gSound.mixObj)->Destroy(gSound.mixObj);       gSound.mixObj    = nullptr; }
    if (gSound.obj)       { (*gSound.obj)->Destroy(gSound.obj);             gSound.obj       = nullptr; }
    LOGI("Sound engine destroyed");
}

} // extern "C"
