// ============================================================================
// How a run ends.
//
// The end of a run used to be a Compose card on a black scrim: a rounded
// rectangle, a border, a pulsing title, and the level you had just been
// standing in painted over at 88% black behind it. That reads as a dialog, and
// a dialog is what you dismiss. The moment a horror game earns is the one where
// the thing you were looking at stops working -- so nothing here draws a panel.
// It takes the frame the player is already looking at and takes it apart.
//
// The transition is a pure function of (kind, seconds since it started). No
// state, no random(), nothing that depends on frame rate: the same instant of
// the same ending looks the same on every device and in every run, which is
// what lets a tool assert on it at all. Engine.cpp samples it once a frame and
// hands the result to the post shader as uniforms; the shader does no timing of
// its own, so what the check measures is what the screen shows.
//
// The localised text stays in Compose. Translation belongs where the ten string
// files are, and there is nothing AAA about drawing glyphs by hand.
// ============================================================================

#ifndef OMNI_ENDING_ENDING_H
#define OMNI_ENDING_ENDING_H

#include <cstdint>

namespace omni {
namespace ending {

/** Which ending. Caught by the thing, or out through the door. */
enum class Kind : uint8_t { None = 0, Death = 1, Escape = 2 };

/**
 * Everything the post shader needs, for one instant.
 *
 * All in 0..1 unless said otherwise, and all zero at t = 0 so the first frame
 * of an ending is exactly the frame before it. An ending that starts by
 * snapping to a state is one the eye reads as a cut.
 */
struct Params {
    /** Toward grey. Death drains the colour out; an escape keeps it. */
    float desaturate = 0.0f;
    /** How far in the vignette has closed. 1 is fully shut. */
    float vignette = 0.0f;
    /** Lateral RGB split, in fractions of screen width. Small numbers. */
    float aberration = 0.0f;
    /** Horizontal tearing, as the tape loses lock. Amplitude in UV. */
    float tear = 0.0f;
    /** Radial smear toward the centre. */
    float pull = 0.0f;
    /** Extra bloom. An escape blows out; a death does not. */
    float bloom = 0.0f;
    /** Overall exposure multiplier. Below 1 the frame is dying, above 1 it is
     *  being lifted out. */
    float exposure = 1.0f;
    /** How much of the screen the caller should now let the stats occupy.
     *  Drives the Compose card's own fade, so the panel cannot appear before
     *  the frame behind it has finished falling apart. */
    float panel = 0.0f;
};

/** Seconds from the trigger to the panel being fully up. */
constexpr float kDeathSeconds  = 2.30f;
constexpr float kEscapeSeconds = 1.90f;

/** The whole transition, at `t` seconds in. Clamped at both ends, so a caller
 *  that keeps sampling past the end gets the settled state rather than
 *  something that has run off. */
[[nodiscard]] Params evaluate(Kind kind, float t) noexcept;

/** How long `kind` runs for. Zero for None. */
[[nodiscard]] float duration(Kind kind) noexcept;

} // namespace ending
} // namespace omni

#endif // OMNI_ENDING_ENDING_H
