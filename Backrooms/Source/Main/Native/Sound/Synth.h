


#ifndef OMNI_SOUND_SYNTH_H
#define OMNI_SOUND_SYNTH_H

#include <cstdint>

namespace omni {
namespace sound {

constexpr int kSynthRate = 44100;


[[nodiscard]] float hash01(uint32_t n) noexcept;

[[nodiscard]] float white(uint32_t i) noexcept;


[[nodiscard]] float valueNoise(float x) noexcept;


[[nodiscard]] float vhsIntro(float t, float duration) noexcept;


[[nodiscard]] float fluorescentHum(float t, float health) noexcept;


[[nodiscard]] float footstep(float t, float pace, float surface, float step) noexcept;


[[nodiscard]] float monsterVoice(float t, float proximity) noexcept;


[[nodiscard]] float roomTone(float t, float damp) noexcept;


[[nodiscard]] float distantEvent(float t) noexcept;


[[nodiscard]] float breath(float t, float exertion) noexcept;


[[nodiscard]] float heartbeat(float t, float fear) noexcept;


[[nodiscard]] float torchClick(float t) noexcept;


enum class Shot : uint8_t { Sting, TorchClick };

class OneShot {
public:
    void start(float duration, Shot which = Shot::Sting) noexcept {
        frame_ = 0;
        frames_ = static_cast<int>(duration * kSynthRate);
        which_ = which;
    }
    void stop() noexcept { frames_ = 0; }
    [[nodiscard]] bool playing() const noexcept { return frame_ < frames_; }


    [[nodiscard]] float next() noexcept {
        if (frame_ >= frames_) return 0.0f;
        const float dur = static_cast<float>(frames_) / kSynthRate;
        const float t = static_cast<float>(frame_) / kSynthRate;
        ++frame_;
        return which_ == Shot::TorchClick ? torchClick(t) : vhsIntro(t, dur);
    }
private:
    int frame_ = 0;
    int frames_ = 0;
    Shot which_ = Shot::Sting;
};

}
}

#endif
