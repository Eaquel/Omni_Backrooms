


#ifndef OMNI_MAP_LEVEL_0_H
#define OMNI_MAP_LEVEL_0_H

#include <cstdint>

namespace omni {
namespace map {


constexpr uint8_t kZoneDark   = 0;
constexpr uint8_t kZoneDim    = 1;
constexpr uint8_t kZoneNormal = 2;
constexpr uint8_t kZoneBright = 3;


constexpr uint8_t kFeatureNone    = 0;
constexpr uint8_t kFeatureDoorway = 1;
constexpr uint8_t kFeaturePillar  = 2;
constexpr uint8_t kFeatureAlcove  = 3;
constexpr uint8_t kFeatureHole    = 4;


constexpr uint8_t kFixtureNone = 0;
constexpr uint8_t kFixtureLit  = 1;
constexpr uint8_t kFixtureDead = 2;


struct CellSample {
    uint8_t solid;
    uint8_t feature;
    uint8_t fixture;



    float   light;


    float   power;
};


class Level0Field {
public:










    static constexpr float kCell   = 3.2f;
    static constexpr float kHeight = 3.0f;

    explicit Level0Field(uint64_t seed = 0) noexcept : seed_(seed) {}

    void setSeed(uint64_t seed) noexcept { seed_ = seed; }
    uint64_t seed() const noexcept { return seed_; }


    bool isOpen(int cx, int cz) const noexcept;
    bool isSolid(int cx, int cz) const noexcept { return !isOpen(cx, cz); }

    bool isOpenBase(int cx, int cz) const noexcept;

    bool    isPillar(int cx, int cz) const noexcept;
    uint8_t zoneAt(int cx, int cz) const noexcept;
    uint8_t fixtureAt(int cx, int cz) const noexcept;
    uint8_t featureAt(int cx, int cz) const noexcept;


    float powerAt(int cx, int cz) const noexcept;




    void sampleChunk(int chunkX, int chunkZ, int cells, CellSample* out) const noexcept;


    void findSpawn(int& outCx, int& outCz) const noexcept;

    void findExit(int spawnCx, int spawnCz, int& outCx, int& outCz) const noexcept;



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


    uint8_t fixtureFor(int cx, int cz, float power) const noexcept;



    bool snapFixture(int latticeCx, int latticeCz, int& outCx, int& outCz) const noexcept;

    uint64_t seed_;
};

}
}

#endif
