// ============================================================================
// The sound, as code.
//
// This game ships no audio files. Every sound is synthesised on the device,
// which keeps the APK small and lets a sound respond continuously to game state
// instead of being a fixed clip — a footstep on damp carpet under a dead tube
// can differ from the same footstep under a working one without anyone
// authoring two recordings.
//
// The catch is that code you cannot hear is code nobody checks. Code_To_Sound.py
// existed to close that, but it checked a Python *re-implementation* of these
// generators, and nothing anywhere asserted that the Python and the C++ agreed.
// They could drift silently, and the tool would keep reporting that a sound
// nobody had ever heard was fine.
//
// So the generators live here, in a module with no Android dependency, and the
// tool compiles this file and renders from it. What gets checked is what ships.
//
// Every function is a pure function of (time, parameters). Nothing holds state,
// nothing calls random(): the same call gives the same sample on every device
// and in every run, which is the only way a sound can be asserted on at all —
// and it also means two players standing in the same place hear the same thing.
// ============================================================================

#ifndef OMNI_SOUND_SYNTH_H
#define OMNI_SOUND_SYNTH_H

#include <cstdint>

namespace omni {
namespace sound {

constexpr int kSynthRate = 44100;

/** Deterministic 32-bit hash to the unit interval. */
[[nodiscard]] float hash01(uint32_t n) noexcept;
/** White noise at sample index i, in -1..1. */
[[nodiscard]] float white(uint32_t i) noexcept;
/** Smooth 1-D noise. For parameters that should drift rather than jump — tape
 *  speed, ballast flicker, the wobble on a creature's voice. */
[[nodiscard]] float valueNoise(float x) noexcept;

/**
 * The title sting: a dead tape being played.
 *
 * Four things happen at once, which is what stops it sounding like plain noise.
 * The transport spins up, so a low rumble slides in from below pitch. The head
 * makes contact and hiss arrives with it. The tape has dropouts — brief, hard
 * gaps, not fades — because that is what damaged tape does. And underneath it
 * all sits mains hum at 50 Hz plus its third harmonic, which is the sound of
 * equipment that is on rather than merely present.
 */
[[nodiscard]] float vhsIntro(float t, float duration) noexcept;

/** A tube's own sound. `health` 1 is a good fitting, 0 a failing ballast —
 *  which buzzes harder and stutters. */
[[nodiscard]] float fluorescentHum(float t, float health) noexcept;

/**
 * One footfall. `pace` 0 walking, 1 running; `surface` 0 carpet, 1 hard;
 * `step` the footfall's index, which is what stops every step being the same
 * waveform on a metronome.
 *
 * Heel then toe, a low body under a low-passed scuff, and a click that belongs
 * only to a hard floor. The previous one put its energy at about 1.1 kHz and
 * was over in 53 ms — a tick, not a step.
 */
[[nodiscard]] float footstep(float t, float pace, float surface, float step) noexcept;

/**
 * Not a growl. A resonance that should not be there — an inharmonic pair well
 * below speech, amplitude-modulated so it seems to breathe, getting closer to a
 * pitch the ear can hold as `proximity` rises.
 */
[[nodiscard]] float monsterVoice(float t, float proximity) noexcept;

/**
 * The empty building, underneath everything.
 *
 * `damp` 0 is a dry floor, 1 one with water getting in. This is the bed the
 * whole mix sits on and the thing whose absence made the game sound raw: what
 * played before was unfiltered white noise straight from a std::mt19937, which
 * is the single rawest signal you can put in front of a listener. Here it is a
 * beating drone, air handling low-passed a long way down, a little top so it
 * does not sound muffled, and a drip.
 */
[[nodiscard]] float roomTone(float t, float damp) noexcept;

/**
 * Her own breathing. `exertion` 0 standing still, 1 sprinting.
 *
 * In and out are deliberately different shapes — drawing in is longer and
 * quieter than pushing out — because a symmetric envelope reads as wind.
 */
[[nodiscard]] float breath(float t, float exertion) noexcept;

/** Lub and dub, the second softer and a fifth of a beat behind. `fear` drives
 *  both the rate and how much of it is felt rather than heard. */
[[nodiscard]] float heartbeat(float t, float fear) noexcept;

/** The torch switch: a contact transient and a spring ring, over in 40 ms. */
[[nodiscard]] float torchClick(float t) noexcept;

/**
 * Plays one generator through once and then stops.
 *
 * The mixer pulls a sample at a time and must never block, so this holds
 * nothing but a frame counter and which generator to run. `start` is the only
 * thing that mutates it from another thread, and the caller holds the engine
 * mutex when it does.
 *
 * The generator used to be hardcoded to vhsIntro, which meant a second one-shot
 * sound could not exist without a second class to play it.
 */
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
    /** Advances one frame. Returns 0 when finished, so it is safe to call
     *  unconditionally from the callback. */
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

} // namespace sound
} // namespace omni

#endif // OMNI_SOUND_SYNTH_H
