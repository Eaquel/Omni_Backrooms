// ============================================================================
// Avatar frames.
//
// A frame is a ring swept around the player's picture. What makes one frame
// different from another is three things, and this header is where all three
// live:
//
//   * its SILHOUETTE — the radius the tube is swept along, per angle;
//   * its PROFILE — how thick the tube is, per angle;
//   * its EMISSION — how brightly each point on the ring glows, per angle and
//     per moment.
//
// They used to live in Kotlin as a switch on a string, which meant the identity
// of a cosmetic — its name, its shape, its behaviour — was scattered through a
// UI file. Here each frame is one entry in one table, addressed by an English
// identifier that is also its wire name.
//
// Frames never obstruct the portrait they surround. Every silhouette here is
// evaluated as a radius about the ring's own centre line, and the caller places
// that centre line outside the picture; nothing in this module draws across the
// middle. See kInnerClearance.
// ============================================================================

#ifndef OMNI_FRAME_H
#define OMNI_FRAME_H

#include <cstdint>

namespace omni {
namespace cosmetic {

/**
 * Fraction of the ring's radius that must stay clear on the inside.
 *
 * The tube's inner edge is (radius - thickness). Keeping thickness under this
 * is what guarantees a frame decorates the portrait rather than crops it — the
 * whole reason the ring had been removed from the avatar altogether.
 */
constexpr float kInnerClearance = 0.34f;

/** Frames are addressed by these ids on the wire and in the store. */
constexpr const char* kFaceOfDarkness   = "Face_Of_Darkness";
constexpr const char* kEndlessDimension = "Endless_Dimension";
constexpr const char* kSoundOfRooms     = "Sound_Of_Rooms";

/**
 * One frame, fully described.
 *
 * Colours are linear 0..1. The renderer multiplies `base` by its own lighting,
 * adds `highlight` where the specular lands, and adds `glow` scaled by the
 * emission this module reports.
 */
struct FrameSpec {
    const char* id;

    float baseR, baseG, baseB;
    float glowR, glowG, glowB;
    float hiR,   hiG,   hiB;

    /** Tube thickness as a fraction of the ring radius, before per-angle
     *  shaping. Always below kInnerClearance. */
    float tubeRatio;
    /** Specular exponent. Low is a soft satin, high is a hard polished edge. */
    float shininess;
};

/** How many frames exist. Exactly three, by design. */
int frameCount() noexcept;

/** Frame at [index], or nullptr when out of range. */
const FrameSpec* frameAt(int index) noexcept;

/** Index of [id], or -1. Never crashes on an unknown or null id — an unknown
 *  cosmetic must degrade to "nothing equipped", not take the screen down. */
int frameIndexOf(const char* id) noexcept;

/**
 * Static geometry: writes [samples] * 2 floats as (radius, thickness) pairs,
 * evenly spaced around the ring starting at angle 0.
 *
 * Radius is normalised so the widest point of the silhouette is exactly 1,
 * which lets a caller scale the whole frame by a single number and know it will
 * land inside its box. Called once per frame and cached; nothing here changes
 * over time.
 */
void frameProfile(int index, int samples, float* out) noexcept;

/**
 * Emission at time [t] seconds: writes [samples] floats in 0..1, one per ring
 * position, matching the ordering of frameProfile.
 *
 * This is the part that carries each frame's character, so it is worth stating
 * what the three actually do rather than leaving it to the numbers:
 *
 *   Face_Of_Darkness  — two eyes and a grin surface out of an almost-black
 *                       ring. The eyes blink on their own irregular timing and
 *                       the grin widens and fades; between blinks there is
 *                       nothing there at all, which is what makes it a face
 *                       appearing rather than a decoration with holes in it.
 *   Endless_Dimension — pulses running inward without end. Three harmonics that
 *                       do not share a period, so the sequence never repeats
 *                       and there is no moment where it visibly starts over.
 *   Sound_Of_Rooms    — a level meter driven by an envelope built from beating
 *                       sines: the ring fills, peaks hold and decay, and quiet
 *                       stretches leave it nearly dark.
 */
void frameEmission(int index, int samples, float t, float* out) noexcept;

} // namespace cosmetic
} // namespace omni

#endif // OMNI_FRAME_H
