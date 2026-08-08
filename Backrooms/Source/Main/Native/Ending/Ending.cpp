#include "Ending/Ending.h"

#include <algorithm>
#include <cmath>

namespace omni {
namespace ending {
namespace {

float clamp01(float v) noexcept { return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v); }

/** 0 below a, 1 above b, smooth between. The same curve the shaders use, so a
 *  parameter computed here and a parameter computed there have the same shape. */
float smoothstep(float a, float b, float x) noexcept {
    if (b <= a) return x < a ? 0.0f : 1.0f;
    const float t = clamp01((x - a) / (b - a));
    return t * t * (3.0f - 2.0f * t);
}

/**
 * The tape losing lock.
 *
 * Tearing is not a ramp — a tear that grows smoothly reads as a wobble. Real
 * dropout arrives in bursts, so this is a fast oscillation gated by a slower
 * one, which gives clusters of a few violent frames separated by stretches that
 * are almost clean. Deterministic in t, so the same instant tears identically
 * every time.
 */
float burst(float t) noexcept {
    const float fast = std::sin(t * 41.0f) * std::sin(t * 97.0f + 1.7f);
    const float gate = std::sin(t * 5.3f) * 0.5f + 0.5f;
    return fast * gate * gate;
}

} // namespace

float duration(Kind kind) noexcept {
    switch (kind) {
        case Kind::Death:  return kDeathSeconds;
        case Kind::Escape: return kEscapeSeconds;
        default:           return 0.0f;
    }
}

Params evaluate(Kind kind, float t) noexcept {
    Params p{};
    if (kind == Kind::None) return p;

    const float dur = duration(kind);
    const float u = clamp01(t / dur);          // 0..1 across the whole thing

    if (kind == Kind::Death) {
        // Caught. The signal fails: colour goes first, then the picture is
        // pulled into the middle and the vignette shuts over it. It is a
        // recording of the room ending, not the room ending.
        //
        // The order matters and it is the reverse of the obvious one. Closing
        // the vignette first hides everything else, so the aberration and the
        // tearing would be spent on pixels nobody can see; the frame has to
        // come apart while it is still visible, and go dark last.
        p.desaturate = smoothstep(0.00f, 0.55f, u);
        p.aberration = smoothstep(0.05f, 0.40f, u) * (1.0f - smoothstep(0.75f, 1.0f, u)) * 0.010f;
        p.tear       = smoothstep(0.10f, 0.45f, u) * (1.0f - smoothstep(0.80f, 1.0f, u))
                     * 0.035f * std::abs(burst(t));
        p.pull       = smoothstep(0.25f, 0.85f, u) * 0.55f;
        p.vignette   = smoothstep(0.35f, 0.95f, u);
        p.bloom      = 0.0f;
        // Dips below one and keeps going down: the picture is losing power,
        // not being turned off at a switch.
        p.exposure   = 1.0f - 0.72f * smoothstep(0.20f, 1.00f, u);
        p.panel      = smoothstep(0.62f, 1.00f, u);
    } else {
        // Out. The opposite shape in every term: the frame is lifted rather
        // than drained, it blows out rather than closing down, and it holds
        // its colour. An escape that used the death curve with a green tint is
        // the mistake that makes both endings feel the same.
        p.desaturate = 0.0f;
        p.aberration = smoothstep(0.00f, 0.25f, u) * (1.0f - smoothstep(0.45f, 0.9f, u)) * 0.004f;
        p.tear       = 0.0f;
        p.pull       = 0.0f;
        // Bloom peaks in the middle and comes back: the door opening onto
        // light, not a white screen you are left staring at.
        p.bloom      = std::sin(clamp01(u / 0.72f) * 3.14159265f) * 1.35f;
        p.vignette   = smoothstep(0.55f, 1.00f, u) * 0.42f;
        p.exposure   = 1.0f + 0.85f * std::sin(clamp01(u / 0.80f) * 3.14159265f);
        p.panel      = smoothstep(0.55f, 1.00f, u);
    }
    return p;
}

} // namespace ending
} // namespace omni
