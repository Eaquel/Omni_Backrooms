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

/** Distance between two positions on a unit circle, taking the short way. */
inline float ringDist(float a, float b) noexcept {
    float d = std::fabs(a - b);
    if (d > 0.5f) d = 1.0f - d;
    return d;
}

/** A bump of half-width [w] centred on [c], over the wrapped 0..1 ring. */
inline float bump(float u, float c, float w) noexcept {
    if (w <= 0.0f) return 0.0f;
    return 1.0f - smoothstep01(ringDist(u, c) / w);
}

// ---------------------------------------------------------------------------
// The catalogue.
//
// Three frames, deliberately. A fourth "plain ring" entry used to sit here and
// it was the one nobody would ever equip: it existed only because the list had
// grown a default, and it made the picker read as three ideas and a spare.
// ---------------------------------------------------------------------------
const FrameSpec kFrames[] = {
    // Karanlığın Yüzü. Almost no base colour at all — the ring is a hole in the
    // screen, and everything you see of it is the glow when the face surfaces.
    {
        kFaceOfDarkness,
        0.045f, 0.040f, 0.055f,     // base: barely there
        1.000f, 0.240f, 0.180f,     // glow: the eyes
        0.620f, 0.560f, 0.600f,     // highlight: cold sheen on the rim
        0.150f, 26.0f
    },
    // Sonsuz Boyut. A portal edge: cold, hard, with a bright cyan running
    // through it.
    {
        kEndlessDimension,
        0.075f, 0.105f, 0.145f,
        0.420f, 0.900f, 1.000f,
        0.780f, 0.930f, 1.000f,
        0.115f, 54.0f
    },
    // Odaların Sesi. Warm and dull like the level's own walls, so the only
    // thing that moves on it is the sound.
    {
        kSoundOfRooms,
        0.230f, 0.200f, 0.125f,
        1.000f, 0.820f, 0.380f,
        1.000f, 0.930f, 0.720f,
        0.130f, 34.0f
    },
};

constexpr int kCount = static_cast<int>(sizeof(kFrames) / sizeof(kFrames[0]));

// ---------------------------------------------------------------------------
// Silhouettes. Each returns an unnormalised radius at ring position u (0..1);
// frameProfile scales the result so the widest point is 1.
// ---------------------------------------------------------------------------

/** A hood. Wide and round across the top, drawn in to a narrow chin below —
 *  the outline of a head before there is a face on it. */
float silhouetteFaceOfDarkness(float u) noexcept {
    const float a = u * kTau;
    // Sitting the taper on cos(a) alone gives an egg; the second harmonic is
    // what puts cheekbones on it.
    return 1.0f + 0.085f * std::cos(a) - 0.055f * std::cos(2.0f * a);
}

/** A hexagon with softened vertices: six straight runs, which is what makes it
 *  read as a constructed portal rather than a hoop. */
float silhouetteEndlessDimension(float u) noexcept {
    const float a = u * kTau;
    const float sector = kTau / 6.0f;
    const float local = a - sector * std::floor(a / sector) - sector * 0.5f;
    const float hex = std::cos(sector * 0.5f) / std::cos(local);
    // Blend a little circle back in so the corners take a highlight instead of
    // going perfectly sharp, which at thumbnail size just aliases.
    return hex * 0.88f + 0.12f;
}

/** An oscilloscope trace bent into a circle. Three non-harmonic components, so
 *  the waveform does not close into an obvious rosette. */
float silhouetteSoundOfRooms(float u) noexcept {
    const float a = u * kTau;
    return 1.0f
         + 0.070f * std::sin(a * 7.0f)
         + 0.045f * std::sin(a * 11.0f + 1.7f)
         + 0.028f * std::sin(a * 17.0f + 0.4f);
}

// ---------------------------------------------------------------------------
// Emissions.
// ---------------------------------------------------------------------------

float emissionFaceOfDarkness(float u, float t) noexcept {
    // Eyes sit either side of the top of the ring; the grin spans the bottom.
    // Fixed positions, because a face that slides around the ring stops being a
    // face and becomes a pattern.
    const float blink = [&] {
        // Irregular: two beating sines gate the eyes, so the blink never lands
        // on a countable rhythm.
        const float g = std::sin(t * 0.9f) * std::sin(t * 0.37f + 1.1f);
        return g > -0.55f ? 1.0f : 0.06f;
    }();
    // A slow swell, so between appearances the ring goes properly dark.
    const float presence = smoothstep01(std::sin(t * 0.31f) * 0.5f + 0.62f);

    const float eyeL = bump(u, 0.375f, 0.045f);
    const float eyeR = bump(u, 0.625f, 0.045f);
    const float eyes = (eyeL + eyeR) * blink;

    // The grin: a wide arc across the bottom that widens as it brightens, with
    // teeth cut into it.
    const float open = 0.5f * (1.0f + std::sin(t * 0.53f + 0.8f));
    const float grin = bump(u, 0.0f, 0.085f + 0.055f * open);
    const float teeth = 0.55f + 0.45f * (std::sin(u * kTau * 26.0f) > 0.0f ? 1.0f : 0.25f);

    return clamp01((eyes + grin * teeth * 0.85f) * presence);
}

float emissionEndlessDimension(float u, float t) noexcept {
    // Three pulse trains at incommensurable speeds and spacings. Because their
    // periods share no common multiple, the ring never returns to a state it
    // has been in, which is the entire point of the name.
    float sum = 0.0f;
    sum += 0.55f * std::pow(0.5f + 0.5f * std::sin((u * 3.0f - t * 0.21f) * kTau), 6.0f);
    sum += 0.40f * std::pow(0.5f + 0.5f * std::sin((u * 5.0f - t * 0.134f) * kTau), 8.0f);
    sum += 0.30f * std::pow(0.5f + 0.5f * std::sin((u * 8.0f - t * 0.0871f) * kTau), 10.0f);
    // A dim floor so the portal edge is always faintly alive between pulses.
    return clamp01(0.06f + sum);
}

float emissionSoundOfRooms(float u, float t) noexcept {
    // Envelope: what the "room" is doing right now. Beating sines give long
    // quiet stretches and occasional swells without any stored state.
    const float env = clamp01(
        0.34f
        + 0.42f * std::sin(t * 1.7f) * std::sin(t * 0.41f)
        + 0.24f * std::sin(t * 3.9f + 2.0f) * std::sin(t * 0.23f));

    // A meter filling from the bottom of the ring in both directions, so it
    // reads as a level rather than as a spinning marker.
    const float fromBottom = ringDist(u, 0.0f) * 2.0f;      // 0 at bottom, 1 opposite
    const float lit = 1.0f - smoothstep01((fromBottom - env) / 0.06f);

    // Segment the meter into discrete cells, the way a real one is.
    const float seg = std::sin(u * kTau * 24.0f);
    const float cell = seg > -0.35f ? 1.0f : 0.15f;

    // Peak hold: a bright pip parked just past the current level, decaying.
    const float peakPos = env * 0.5f;
    const float peak = bump(u, peakPos, 0.02f) + bump(u, 1.0f - peakPos, 0.02f);

    return clamp01(lit * cell * 0.9f + peak * 0.8f);
}

} // namespace

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

    // First pass: raw radii, and the widest of them.
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

    // Second pass: normalise, lift the narrow points off the middle, shape the
    // tube.
    //
    // The lift is a remap of [narrowest, widest] onto [kProfileFloor, 1] rather
    // than a clamp: a clamp would flatten every sample below the floor into one
    // straight arc and the silhouette would lose exactly the part that gives it
    // its shape. The remap keeps the shape and only compresses its range.
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
            // Sound_Of_Rooms swells and pinches around the ring, so the tube
            // itself carries some of the waveform too.
            thickness *= 1.0f + 0.35f * std::sin(u * kTau * 7.0f);
        } else if (index == 0) {
            // Face_Of_Darkness is heaviest across the brow and thins toward the
            // chin, which is most of what makes the silhouette read as a head.
            thickness *= 1.0f + 0.30f * std::cos(u * kTau);
        }
        // Never allowed to close in over the portrait. Two caps, and the
        // tighter wins: a proportional one so a tube stays in proportion to its
        // own radius, and an absolute one so the inner edge clears the picture
        // no matter what the silhouette is doing at this sample.
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

} // namespace cosmetic
} // namespace omni
