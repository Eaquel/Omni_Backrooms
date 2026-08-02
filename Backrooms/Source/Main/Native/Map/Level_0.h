// ============================================================================
// Level 0 world model — infinite.
//
// There is no grid and no bounds. The level is a pure function of cell
// coordinates, so it can be queried anywhere and never runs out. See
// Level_0.cpp for how a purely local query still produces coherent rooms and
// connected corridors.
//
// Cells are integers; multiply by kCell for world metres.
// ============================================================================

#ifndef OMNI_MAP_LEVEL_0_H
#define OMNI_MAP_LEVEL_0_H

#include <cstdint>

namespace omni {
namespace map {

// Lighting zones, ordered dark -> bright so comparisons read naturally.
constexpr uint8_t kZoneDark   = 0;   // fixtures out entirely
constexpr uint8_t kZoneDim    = 1;   // failing ballasts, flicker
constexpr uint8_t kZoneNormal = 2;   // standard fluorescent
constexpr uint8_t kZoneBright = 3;   // blown-out hotspot

// Per-cell features.
constexpr uint8_t kFeatureNone    = 0;
constexpr uint8_t kFeatureDoorway = 1;   // threshold; no ceiling tile
constexpr uint8_t kFeaturePillar  = 2;   // structural column (solid)
constexpr uint8_t kFeatureAlcove  = 3;   // dead end
constexpr uint8_t kFeatureHole    = 4;   // missing floor

// Ceiling fixtures.
constexpr uint8_t kFixtureNone = 0;
constexpr uint8_t kFixtureLit  = 1;
constexpr uint8_t kFixtureDead = 2;

/** One cell, fully resolved. Produced in bulk by Level0Field::sampleChunk. */
struct CellSample {
    uint8_t solid;
    uint8_t feature;
    uint8_t fixture;
    /** Continuous baked illuminance. ~1.0 is a normally lit corridor, ~0.1 an
     *  unpowered one. Deliberately NOT quantised into zones: a handful of
     *  discrete tiers put a hard step on a cell edge wherever the tier changed,
     *  and that step is what read as crude banding between regions. */
    float   light;
    /** 0..1 mains health at the cell. Drives flicker amplitude on the render
     *  side, so a failing region visibly struggles instead of just being dim. */
    float   power;
};

/**
 * The world. Construct once with a seed; every query is deterministic, so two
 * clients with the same seed see byte-identical geometry without exchanging any
 * of it — which is what makes shared multiplayer worlds possible later.
 */
class Level0Field {
public:
    // 3.2 m cells. A 2.6 m ceiling is deliberately low: it is most of what
    // makes the space feel oppressive rather than merely large.
    static constexpr float kCell   = 3.2f;
    static constexpr float kHeight = 2.6f;

    explicit Level0Field(uint64_t seed = 0) noexcept : seed_(seed) {}

    void setSeed(uint64_t seed) noexcept { seed_ = seed; }
    uint64_t seed() const noexcept { return seed_; }

    /** Walkable floor. The single source of truth for collision and meshing. */
    bool isOpen(int cx, int cz) const noexcept;
    bool isSolid(int cx, int cz) const noexcept { return !isOpen(cx, cz); }

    bool    isPillar(int cx, int cz) const noexcept;
    uint8_t zoneAt(int cx, int cz) const noexcept;
    uint8_t fixtureAt(int cx, int cz) const noexcept;
    uint8_t featureAt(int cx, int cz) const noexcept;

    /** 0..1 mains health. Continuous, so lit and failed regions blend. */
    float powerAt(int cx, int cz) const noexcept;

    /**
     * Resolves a whole chunk plus a one-cell apron in a single pass, writing
     * (cells + 2)^2 samples in row-major order starting at cell (-1, -1) of the
     * chunk.
     *
     * Done in bulk for two reasons. It is far cheaper — isOpen() is evaluated
     * once per cell rather than once per neighbour test, and feature detection
     * alone used to cost five of them. More importantly it is the only way to
     * bake light spill at all: illuminance at a point is the sum of what every
     * fixture within a few cells actually throws, and a per-cell query cannot
     * see those without re-deriving the whole neighbourhood each time.
     */
    void sampleChunk(int chunkX, int chunkZ, int cells, CellSample* out) const noexcept;

    /** First lit, open cell near the origin. */
    void findSpawn(int& outCx, int& outCz) const noexcept;
    /** A run's authored distance away from spawn, in a seed-chosen direction. */
    void findExit(int spawnCx, int spawnCz, int& outCx, int& outCz) const noexcept;
    /** Anchors an exit [distance] cells from [fromCx,fromCz] in a direction
     *  derived from that cell, snapped to the nearest open floor. An endless
     *  world has no furthest point to walk to, so the door has to come to the
     *  player once they have wandered away from the one they were given. */
    void findExitNear(int fromCx, int fromCz, int distance, int& outCx, int& outCz) const noexcept;

    static float worldX(int cx) noexcept { return cx * kCell; }
    static float worldZ(int cz) noexcept { return cz * kCell; }
    static int   cellX(float wx) noexcept {
        const float q = wx / kCell;
        return static_cast<int>(q < 0 ? q - 1.0f : q);
    }
    static int   cellZ(float wz) noexcept {
        const float q = wz / kCell;
        return static_cast<int>(q < 0 ? q - 1.0f : q);
    }

private:
    float noise(float x, float z, uint64_t salt) const noexcept;
    /** Fixture kind for an already-known open cell and mains health, so the bulk
     *  sampler does not pay for isOpen/powerAt twice. */
    uint8_t fixtureFor(int cx, int cz, float power) const noexcept;

    uint64_t seed_;
};

} // namespace map
} // namespace omni

#endif // OMNI_MAP_LEVEL_0_H
