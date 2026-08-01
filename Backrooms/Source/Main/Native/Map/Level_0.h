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

    /** First lit, open cell near the origin. */
    void findSpawn(int& outCx, int& outCz) const noexcept;
    /** A run's authored distance away from spawn, in a seed-chosen direction. */
    void findExit(int spawnCx, int spawnCz, int& outCx, int& outCz) const noexcept;

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

    uint64_t seed_;
};

} // namespace map
} // namespace omni

#endif // OMNI_MAP_LEVEL_0_H
