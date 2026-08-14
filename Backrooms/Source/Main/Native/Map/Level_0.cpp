


#include "Map/Level_0.h"

#include <algorithm>
#include <cmath>

namespace omni {
namespace map {

namespace {


constexpr int kSectorSize     = 24;
constexpr int kRoomsPerSector = 4;


constexpr int kArchetype[8][2] = {
    {3, 2},
    {2, 3},
    {2, 2},
    {1, 2},
    {3, 1},
    {1, 3},
    {2, 1},
    {1, 1}
};


static_assert([]{
    int worst = 0;
    for (const auto& a : kArchetype) { worst = a[0] > worst ? a[0] : worst;
                                       worst = a[1] > worst ? a[1] : worst; }
    return worst;
}() < kSectorSize / 2,
    "A room half-extent at or past half a sector can reach beyond an adjacent "
    "sector, and isOpenBase only scans one ring of neighbours — so a cell would "
    "miss the room it is standing in. This is the assertion the old kMaxRoomHalf "
    "constant was supposed to be.");

constexpr int kFixtureSpacing = 4;


constexpr int kFixtureSnap = 2;


constexpr int   kLightRadius = 4;


constexpr float kLightSigma  = 1.70f;
constexpr float kLitOutput   = 1.55f;
constexpr float kDeadOutput  = 0.05f;


constexpr float kAmbient     = 0.085f;


constexpr uint64_t kSaltRoom    = 0x1000ULL;
constexpr uint64_t kSaltPillar  = 0x9111ULL;
constexpr uint64_t kSaltCorrW   = 0x5150ULL;
constexpr uint64_t kSaltFixture = 0xF17DULL;
constexpr uint64_t kSaltFeature = 0xFEA7ULL;
constexpr uint64_t kSaltBroad   = 0xB0DAULL;
constexpr uint64_t kSaltFine    = 0xF1E5ULL;
constexpr uint64_t kSaltSpine   = 0x5B10EULL;
constexpr uint64_t kSaltExitDir = 0xE217ULL;
constexpr uint64_t kSaltExitLen = 0xD157ULL;


inline uint64_t mix64(uint64_t x) noexcept {
    x ^= x >> 33; x *= 0xFF51AFD7ED558CCDULL;
    x ^= x >> 33; x *= 0xC4CEB9FE1A85EC53ULL;
    x ^= x >> 33;
    return x;
}

inline uint64_t hashCell(int64_t a, int64_t b, uint64_t seed, uint64_t salt) noexcept {
    uint64_t h = seed ^ salt;
    h = mix64(h + static_cast<uint64_t>(a) * 0x9E3779B97F4A7C15ULL);
    h = mix64(h + static_cast<uint64_t>(b) * 0xC2B2AE3D27D4EB4FULL);
    return h;
}

inline float hashFloat(uint64_t h) noexcept {
    return static_cast<float>(h & 0xFFFFFFULL) / 16777216.0f;
}

inline int hashRange(uint64_t h, int lo, int hi) noexcept {
    if (hi <= lo) return lo;
    return lo + static_cast<int>(h % static_cast<uint64_t>(hi - lo + 1));
}


inline int floorDiv(int a, int b) noexcept {
    const int q = a / b;
    return (a % b != 0 && ((a < 0) != (b < 0))) ? q - 1 : q;
}

struct Room {
    int cx, cz, halfW, halfD;
};


inline void sectorRooms(int sx, int sz, uint64_t seed, Room out[kRoomsPerSector]) noexcept {
    for (int i = 0; i < kRoomsPerSector; ++i) {
        const uint64_t h0 = hashCell(sx, sz, seed, kSaltRoom + static_cast<uint64_t>(i) * 7ULL);
        const uint64_t h1 = mix64(h0 + 1);
        const uint64_t h2 = mix64(h0 + 2);




        const int baseX = sx * kSectorSize;
        const int baseZ = sz * kSectorSize;

        out[i].cx = baseX + hashRange(h0, 4, kSectorSize - 5);
        out[i].cz = baseZ + hashRange(h1, 4, kSectorSize - 5);











        const int arch = static_cast<int>((h2 >> 3) & 7ULL);
        out[i].halfW = kArchetype[arch][0];
        out[i].halfD = kArchetype[arch][1];
    }
}


inline bool onCorridor(int cx, int cz, int ax, int az, int bx, int bz, int halfWidth) noexcept {
    const uint64_t h = hashCell(ax * 73856093 + bx, az * 19349663 + bz, 0x5EEDULL, kSaltSpine);


    const int mx = ax + static_cast<int>((bx - ax) * (0.34f + hashFloat(h) * 0.32f));
    const int off = 3 + static_cast<int>(hashFloat(mix64(h)) * 4.0f);
    const int mz = az + ((h & 1ULL) ? off : -off);


    if (cz >= az - halfWidth && cz <= az + halfWidth &&
        cx >= std::min(ax, mx) && cx <= std::max(ax, mx)) return true;

    if (cx >= mx - halfWidth && cx <= mx + halfWidth &&
        cz >= std::min(az, mz) && cz <= std::max(az, mz)) return true;

    if (cz >= mz - halfWidth && cz <= mz + halfWidth &&
        cx >= std::min(mx, bx) && cx <= std::max(mx, bx)) return true;

    if (cx >= bx - halfWidth && cx <= bx + halfWidth &&
        cz >= std::min(mz, bz) && cz <= std::max(mz, bz)) return true;
    return false;
}

}

bool Level0Field::isPillar(int cx, int cz) const noexcept {



    const int mx = ((cx % 7) + 7) % 7;
    const int mz = ((cz % 7) + 7) % 7;
    if (mx != 0 || mz != 0) return false;













    const float power = powerAt(cx, cz);
    const float chance = 0.06f + (1.0f - power) * 0.88f;
    if (hashFloat(hashCell(cx, cz, seed_, kSaltPillar)) >= chance) return false;












    for (int dz = -1; dz <= 1; ++dz) {
        for (int dx = -1; dx <= 1; ++dx) {
            if (dx == 0 && dz == 0) continue;
            if (!isOpenBase(cx + dx, cz + dz)) return false;
        }
    }
    return true;
}

bool Level0Field::isOpen(int cx, int cz) const noexcept {

    return isOpenBase(cx, cz) && !isPillar(cx, cz);
}


bool Level0Field::isOpenBase(int cx, int cz) const noexcept {
    const int sx = floorDiv(cx, kSectorSize);
    const int sz = floorDiv(cz, kSectorSize);

    Room rooms[kRoomsPerSector];
    Room east[kRoomsPerSector];
    Room south[kRoomsPerSector];

    bool inside = false;

    for (int dz = -1; dz <= 1 && !inside; ++dz) {
        for (int dx = -1; dx <= 1; ++dx) {
            const int nx = sx + dx, nz = sz + dz;
            sectorRooms(nx, nz, seed_, rooms);

            for (int i = 0; i < kRoomsPerSector; ++i) {
                const Room& r = rooms[i];
                if (cx >= r.cx - r.halfW && cx <= r.cx + r.halfW &&
                    cz >= r.cz - r.halfD && cz <= r.cz + r.halfD) {
                    inside = true;
                    break;
                }
            }
            if (inside) break;








            const uint64_t wHash = hashCell(nx, nz, seed_, kSaltCorrW);
            const int halfWidth = (wHash % 10ULL < 8ULL) ? 0 : 1;








            bool onInternal = false;
            for (int i = 0; i + 1 < kRoomsPerSector && !onInternal; ++i) {
                onInternal = onCorridor(cx, cz,
                                        rooms[i].cx, rooms[i].cz,
                                        rooms[i + 1].cx, rooms[i + 1].cz, halfWidth);
            }
            if (onInternal) { inside = true; break; }



            sectorRooms(nx + 1, nz, seed_, east);
            sectorRooms(nx, nz + 1, seed_, south);

            if (onCorridor(cx, cz, rooms[0].cx, rooms[0].cz, east[0].cx, east[0].cz, halfWidth) ||
                onCorridor(cx, cz, rooms[0].cx, rooms[0].cz, south[0].cx, south[0].cz, halfWidth)) {
                inside = true;
                break;
            }






            bool onExtra = false;
            for (int i = 1; i < kRoomsPerSector && !onExtra; ++i) {
                const uint64_t linkHash = hashCell(nx, nz, seed_, kSaltCorrW + static_cast<uint64_t>(i) * 31ULL);
                if ((linkHash & 3ULL) != 0) continue;
                onExtra = onCorridor(cx, cz, rooms[i].cx, rooms[i].cz, east[i].cx, east[i].cz, 0) ||
                          onCorridor(cx, cz, rooms[i].cx, rooms[i].cz, south[i].cx, south[i].cz, 0);
            }
            if (onExtra) { inside = true; break; }





            {
                const uint64_t spineHash = hashCell(nx, nz, seed_, kSaltSpine);
                const int baseX = nx * kSectorSize;
                const int baseZ = nz * kSectorSize;




                const int laneZ = baseZ + hashRange(spineHash >> 4,  2, kSectorSize - 3);
                const int laneX = baseX + hashRange(spineHash >> 20, 2, kSectorSize - 3);
                if (cz == laneZ && cx >= baseX && cx < baseX + kSectorSize) { inside = true; break; }
                if (cx == laneX && cz >= baseZ && cz < baseZ + kSectorSize) { inside = true; break; }
            }
        }
    }

    return inside;
}

float Level0Field::powerAt(int cx, int cz) const noexcept {























    const float broad = noise(cx * 0.095f, cz * 0.095f, kSaltBroad);


    const float fine  = noise(cx * 0.180f, cz * 0.180f, kSaltFine);









    float health = (broad + 0.60f) / 0.66f;
    health = health < 0.0f ? 0.0f : (health > 1.0f ? 1.0f : health);

    health = health * health * (3.0f - 2.0f * health);

    return health * (0.94f + fine * 0.22f);
}

uint8_t Level0Field::zoneAt(int cx, int cz) const noexcept {



    const float p = powerAt(cx, cz);
    if (p < 0.10f) return kZoneDark;
    if (p < 0.45f) return kZoneDim;
    if (p > 0.92f) return kZoneBright;
    return kZoneNormal;
}

uint8_t Level0Field::fixtureAt(int cx, int cz) const noexcept {










    if (!isOpen(cx, cz)) return kFixtureNone;
    for (int dz = -kFixtureSnap; dz <= kFixtureSnap; ++dz) {
        for (int dx = -kFixtureSnap; dx <= kFixtureSnap; ++dx) {
            const int lx = cx + dx, lz = cz + dz;
            if (((lx % kFixtureSpacing) + kFixtureSpacing) % kFixtureSpacing != 0) continue;
            if (((lz % kFixtureSpacing) + kFixtureSpacing) % kFixtureSpacing != 0) continue;
            int fx = 0, fz = 0;
            if (!snapFixture(lx, lz, fx, fz)) continue;
            if (fx == cx && fz == cz) return fixtureFor(cx, cz, powerAt(cx, cz));
        }
    }
    return kFixtureNone;
}

uint8_t Level0Field::fixtureFor(int cx, int cz, float power) const noexcept {

    if (power < 0.06f) return kFixtureNone;
    const float u = hashFloat(hashCell(cx, cz, seed_, kSaltFixture));



    const float deadChance = 0.06f + (1.0f - power) * 0.72f;
    return u < deadChance ? kFixtureDead : kFixtureLit;
}

bool Level0Field::snapFixture(int latticeCx, int latticeCz,
                              int& outCx, int& outCz) const noexcept {
    if (isOpen(latticeCx, latticeCz)) {
        outCx = latticeCx; outCz = latticeCz;
        return true;
    }




    for (int r = 1; r <= kFixtureSnap; ++r) {
        for (int dz = -r; dz <= r; ++dz) {
            for (int dx = -r; dx <= r; ++dx) {
                if (std::max(std::abs(dx), std::abs(dz)) != r) continue;
                if (!isOpen(latticeCx + dx, latticeCz + dz)) continue;
                outCx = latticeCx + dx; outCz = latticeCz + dz;
                return true;
            }
        }
    }
    return false;
}

uint8_t Level0Field::featureAt(int cx, int cz) const noexcept {
    if (!isOpen(cx, cz)) {
        return isPillar(cx, cz) ? kFeaturePillar : kFeatureNone;
    }

    const bool wallW = !isOpen(cx - 1, cz), wallE = !isOpen(cx + 1, cz);
    const bool wallN = !isOpen(cx, cz - 1), wallS = !isOpen(cx, cz + 1);
    const int walls = (wallW ? 1 : 0) + (wallE ? 1 : 0) + (wallN ? 1 : 0) + (wallS ? 1 : 0);

    const float u = hashFloat(hashCell(cx, cz, seed_, kSaltFeature));
    if (walls == 2 && ((wallW && wallE) || (wallN && wallS))) {
        if (u < 0.28f) return kFeatureDoorway;
    } else if (walls == 3) {
        if (u < 0.32f) return kFeatureAlcove;
    } else if (walls == 0 && u < 0.008f) {
        return kFeatureHole;
    }
    return kFeatureNone;
}

void Level0Field::sampleChunk(int chunkX, int chunkZ, int cells, CellSample* out) const noexcept {
    if (!out || cells <= 0) return;





    const int outSide  = cells + 2;
    const int margin   = 1 + kLightRadius;
    const int workSide = cells + 2 * margin;
    const int baseX    = chunkX * cells - margin;
    const int baseZ    = chunkZ * cells - margin;


    constexpr int kMaxWork = 96;
    if (workSide > kMaxWork) return;

    static thread_local bool  open [kMaxWork * kMaxWork];
    static thread_local float power[kMaxWork * kMaxWork];
    static thread_local unsigned char fixture[kMaxWork * kMaxWork];



    for (int z = 0; z < workSide; ++z) {
        for (int x = 0; x < workSide; ++x) {
            const int i = z * workSide + x;
            const int cx = baseX + x, cz = baseZ + z;
            open[i]  = isOpen(cx, cz);
            power[i] = open[i] ? powerAt(cx, cz) : 0.0f;
        }
    }




















    for (int i = 0; i < workSide * workSide; ++i) fixture[i] = kFixtureNone;

    const int latLo = kFixtureSpacing * static_cast<int>(
        std::floor(static_cast<float>(baseX - kFixtureSnap) / kFixtureSpacing));
    const int latLoZ = kFixtureSpacing * static_cast<int>(
        std::floor(static_cast<float>(baseZ - kFixtureSnap) / kFixtureSpacing));
    const int latHi  = baseX + workSide + kFixtureSnap;
    const int latHiZ = baseZ + workSide + kFixtureSnap;

    for (int lz = latLoZ; lz <= latHiZ; lz += kFixtureSpacing) {
        for (int lx = latLo; lx <= latHi; lx += kFixtureSpacing) {
            int fx = 0, fz = 0;
            if (!snapFixture(lx, lz, fx, fz)) continue;
            const int gx = fx - baseX, gz = fz - baseZ;
            if (gx < 0 || gz < 0 || gx >= workSide || gz >= workSide) continue;
            const int i = gz * workSide + gx;


            if (fixture[i] != kFixtureNone) continue;
            fixture[i] = fixtureFor(fx, fz, power[i]);
        }
    }









    constexpr int kR = kLightRadius;
    float kernel[(2 * kR + 1) * (2 * kR + 1)];
    for (int dz = -kR; dz <= kR; ++dz) {
        for (int dx = -kR; dx <= kR; ++dx) {
            const float d2 = static_cast<float>(dx * dx + dz * dz);
            const float g  = std::exp(-d2 / (2.0f * kLightSigma * kLightSigma));


            const float rim = 1.0f - d2 / static_cast<float>(kR * kR + 1);
            kernel[(dz + kR) * (2 * kR + 1) + (dx + kR)] = g * (rim > 0.0f ? rim : 0.0f);
        }
    }

    for (int z = 0; z < outSide; ++z) {
        for (int x = 0; x < outSide; ++x) {


            const int wx = x + kLightRadius;
            const int wz = z + kLightRadius;
            const int wi = wz * workSide + wx;

            float sum = 0.0f;
            for (int dz = -kR; dz <= kR; ++dz) {
                const int nz = wz + dz;
                for (int dx = -kR; dx <= kR; ++dx) {
                    const int ni = nz * workSide + (wx + dx);
                    const unsigned char f = fixture[ni];
                    if (f == kFixtureNone) continue;
                    const float emit = (f == kFixtureLit) ? kLitOutput : kDeadOutput;
                    sum += emit * power[ni] * kernel[(dz + kR) * (2 * kR + 1) + (dx + kR)];
                }
            }

            CellSample& s = out[z * outSide + x];
            s.solid   = open[wi] ? 0 : 1;
            s.fixture = fixture[wi];
            s.power   = power[wi];
            s.light   = kAmbient + sum;



            if (!open[wi]) {
                const int cx = baseX + wx, cz = baseZ + wz;
                s.feature = isPillar(cx, cz) ? kFeaturePillar : kFeatureNone;
                continue;
            }
            const bool wallW = !open[wi - 1],        wallE = !open[wi + 1];
            const bool wallN = !open[wi - workSide], wallS = !open[wi + workSide];
            const int walls = (wallW ? 1 : 0) + (wallE ? 1 : 0) + (wallN ? 1 : 0) + (wallS ? 1 : 0);
            const float u = hashFloat(hashCell(baseX + wx, baseZ + wz, seed_, kSaltFeature));
            s.feature = kFeatureNone;
            if (walls == 2 && ((wallW && wallE) || (wallN && wallS))) {
                if (u < 0.28f) s.feature = kFeatureDoorway;
            } else if (walls == 3) {
                if (u < 0.32f) s.feature = kFeatureAlcove;
            } else if (walls == 0 && u < 0.008f) {
                s.feature = kFeatureHole;
            }
        }
    }
}

float Level0Field::noise(float x, float z, uint64_t salt) const noexcept {
    const int xi = static_cast<int>(std::floor(x));
    const int zi = static_cast<int>(std::floor(z));
    const float xf = x - xi, zf = z - zi;

    const float u = xf * xf * (3.0f - 2.0f * xf);
    const float v = zf * zf * (3.0f - 2.0f * zf);

    const float a = hashFloat(hashCell(xi,     zi,     seed_, salt));
    const float b = hashFloat(hashCell(xi + 1, zi,     seed_, salt));
    const float c = hashFloat(hashCell(xi,     zi + 1, seed_, salt));
    const float d = hashFloat(hashCell(xi + 1, zi + 1, seed_, salt));

    const float top = a + (b - a) * u;
    const float bot = c + (d - c) * u;
    return (top + (bot - top) * v) * 2.0f - 1.0f;
}

void Level0Field::findSpawn(int& outCx, int& outCz) const noexcept {


    for (int radius = 0; radius < 96; ++radius) {
        for (int dz = -radius; dz <= radius; ++dz) {
            for (int dx = -radius; dx <= radius; ++dx) {
                if (std::max(std::abs(dx), std::abs(dz)) != radius) continue;
                if (!isOpen(dx, dz)) continue;
                const uint8_t z = zoneAt(dx, dz);
                if (z == kZoneNormal || z == kZoneBright) {
                    outCx = dx; outCz = dz;
                    return;
                }
            }
        }
    }
    outCx = 0; outCz = 0;
}

void Level0Field::findExit(int spawnCx, int spawnCz, int& outCx, int& outCz) const noexcept {


    const int distance = 110 + static_cast<int>(
        hashFloat(hashCell(spawnCx, spawnCz, seed_, kSaltExitLen)) * 60.0f);
    findExitNear(spawnCx, spawnCz, distance, outCx, outCz);
}

void Level0Field::findExitNear(int fromCx, int fromCz, int distance, int& outCx, int& outCz) const noexcept {
    const float angle = hashFloat(hashCell(fromCx, fromCz, seed_, kSaltExitDir)) * 6.2831853f;
    const int targetX = fromCx + static_cast<int>(std::cos(angle) * distance);
    const int targetZ = fromCz + static_cast<int>(std::sin(angle) * distance);



    for (int radius = 0; radius < 64; ++radius) {
        for (int dz = -radius; dz <= radius; ++dz) {
            for (int dx = -radius; dx <= radius; ++dx) {
                if (std::max(std::abs(dx), std::abs(dz)) != radius) continue;
                if (isOpen(targetX + dx, targetZ + dz)) {
                    outCx = targetX + dx; outCz = targetZ + dz;
                    return;
                }
            }
        }
    }
    outCx = targetX; outCz = targetZ;
}

}
}
