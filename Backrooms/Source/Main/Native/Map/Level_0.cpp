// ============================================================================
// Level 0 — "The Lobby". Infinite.
//
// The level is no longer an array. It is a pure function of world coordinates:
//
//     isOpen(cellX, cellZ) -> bool
//
// Nothing is stored, so nothing bounds how far you can walk. The previous
// 96x96 grid had a hard edge reachable in about a minute; this has none.
//
// How a *local* function produces coherent rooms
// ----------------------------------------------
// The world divides into sectors. Each sector deterministically spawns a few
// room plates from hash(sectorX, sectorZ, seed) — same inputs, same rooms, on
// every device and every visit. A cell is open if it falls inside any room
// belonging to its own sector or the eight around it.
//
// One ring of neighbours is enough because room half-extents are capped below
// half the sector size, so no room can reach past an adjacent sector. That cap
// is what keeps the query O(1) rather than O(world).
//
// Connectivity comes from corridors carved from each sector's anchor room to
// its east and south neighbours. Both sides of a boundary derive the identical
// endpoints from the same hashes, so runs meet exactly with no seam.
// ============================================================================

#include "Map/Level_0.h"

#include <algorithm>
#include <cmath>

namespace omni {
namespace map {

namespace {

// Rooms are capped well below sector size so the one-ring scan above is
// guaranteed to find every room that could overlap a given cell.
constexpr int kSectorSize     = 24;
constexpr int kRoomsPerSector = 3;
constexpr int kMaxRoomHalf    = 9;   // must stay < kSectorSize / 2
constexpr int kFixtureSpacing = 5;

// Salts keep the different attribute layers from correlating with each other.
constexpr uint64_t kSaltRoom    = 0x1000ULL;
constexpr uint64_t kSaltPillar  = 0x9111ULL;
constexpr uint64_t kSaltCorrW   = 0x5150ULL;
constexpr uint64_t kSaltFixture = 0xF17DULL;
constexpr uint64_t kSaltFeature = 0xFEA7ULL;
constexpr uint64_t kSaltBroad   = 0xB0DAULL;
constexpr uint64_t kSaltFine    = 0xF1E5ULL;
constexpr uint64_t kSaltExitDir = 0xE217ULL;
constexpr uint64_t kSaltExitLen = 0xD157ULL;

/** 64-bit mix. Cheap, well-distributed, and identical across platforms — the
 *  last part matters because every client must agree on the same world. */
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

/** Floor division that behaves for negatives. Essential here: the world runs
 *  in every direction, so cell -1 must land in sector -1, not sector 0. */
inline int floorDiv(int a, int b) noexcept {
    const int q = a / b;
    return (a % b != 0 && ((a < 0) != (b < 0))) ? q - 1 : q;
}

struct Room {
    int cx, cz, halfW, halfD;
};

/** The rooms belonging to one sector. Pure function of sector coords + seed. */
inline void sectorRooms(int sx, int sz, uint64_t seed, Room out[kRoomsPerSector]) noexcept {
    for (int i = 0; i < kRoomsPerSector; ++i) {
        const uint64_t h0 = hashCell(sx, sz, seed, kSaltRoom + static_cast<uint64_t>(i) * 7ULL);
        const uint64_t h1 = mix64(h0 + 1);
        const uint64_t h2 = mix64(h0 + 2);
        const uint64_t h3 = mix64(h0 + 3);

        const int baseX = sx * kSectorSize;
        const int baseZ = sz * kSectorSize;

        out[i].cx = baseX + hashRange(h0, 4, kSectorSize - 5);
        out[i].cz = baseZ + hashRange(h1, 4, kSectorSize - 5);

        // Wide and shallow, alternating the long axis, so the plan reads as an
        // office floor plate rather than a warren of cubes.
        const bool wideOnX  = (h2 & 1ULL) != 0;
        const int  longHalf  = hashRange(h2 >> 1, 5, kMaxRoomHalf);
        const int  shortHalf = hashRange(h3, 3, 6);
        out[i].halfW = wideOnX ? longHalf : shortHalf;
        out[i].halfD = wideOnX ? shortHalf : longHalf;
    }
}

/** True if the cell lies within an L-shaped corridor between two points. */
inline bool onCorridor(int cx, int cz, int ax, int az, int bx, int bz, int halfWidth) noexcept {
    if (cz >= az - halfWidth && cz <= az + halfWidth &&
        cx >= std::min(ax, bx) && cx <= std::max(ax, bx)) {
        return true;
    }
    if (cx >= bx - halfWidth && cx <= bx + halfWidth &&
        cz >= std::min(az, bz) && cz <= std::max(az, bz)) {
        return true;
    }
    return false;
}

} // namespace

bool Level0Field::isPillar(int cx, int cz) const noexcept {
    // Regular lattice with deterministic dropout, so columns read as building
    // structure rather than as scattered obstacles.
    if (cx % 7 != 0 || cz % 7 != 0) return false;
    return hashFloat(hashCell(cx, cz, seed_, kSaltPillar)) < 0.70f;
}

bool Level0Field::isOpen(int cx, int cz) const noexcept {
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

            // Corridors to the east and south neighbours. Both sides of a
            // boundary compute the same endpoints, so the runs line up.
            sectorRooms(nx + 1, nz, seed_, east);
            sectorRooms(nx, nz + 1, seed_, south);
            const int halfWidth = hashRange(hashCell(nx, nz, seed_, kSaltCorrW), 1, 2);

            if (onCorridor(cx, cz, rooms[0].cx, rooms[0].cz, east[0].cx, east[0].cz, halfWidth) ||
                onCorridor(cx, cz, rooms[0].cx, rooms[0].cz, south[0].cx, south[0].cz, halfWidth)) {
                inside = true;
                break;
            }
        }
    }

    // Pillars punch back into otherwise open floor.
    return inside && !isPillar(cx, cz);
}

uint8_t Level0Field::zoneAt(int cx, int cz) const noexcept {
    // Two scales of value noise: broad regions of failure, finer hotspots.
    const float broad = noise(cx * 0.018f, cz * 0.018f, kSaltBroad);
    const float fine  = noise(cx * 0.060f, cz * 0.060f, kSaltFine);

    if (broad < -0.42f) return kZoneDark;
    if (broad < -0.20f) return kZoneDim;
    if (fine  >  0.34f) return kZoneBright;
    return kZoneNormal;
}

uint8_t Level0Field::fixtureAt(int cx, int cz) const noexcept {
    if (cx % kFixtureSpacing != 0 || cz % kFixtureSpacing != 0) return kFixtureNone;
    if (!isOpen(cx, cz)) return kFixtureNone;

    const uint8_t zone = zoneAt(cx, cz);
    if (zone == kZoneDark) return kFixtureNone;

    const float u = hashFloat(hashCell(cx, cz, seed_, kSaltFixture));
    const bool dead = (zone == kZoneDim && u < 0.45f) || u < 0.13f;
    return dead ? kFixtureDead : kFixtureLit;
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

float Level0Field::noise(float x, float z, uint64_t salt) const noexcept {
    const int xi = static_cast<int>(std::floor(x));
    const int zi = static_cast<int>(std::floor(z));
    const float xf = x - xi, zf = z - zi;
    // Smoothstep the interpolants so there are no visible grid seams.
    const float u = xf * xf * (3.0f - 2.0f * xf);
    const float v = zf * zf * (3.0f - 2.0f * zf);

    const float a = hashFloat(hashCell(xi,     zi,     seed_, salt));
    const float b = hashFloat(hashCell(xi + 1, zi,     seed_, salt));
    const float c = hashFloat(hashCell(xi,     zi + 1, seed_, salt));
    const float d = hashFloat(hashCell(xi + 1, zi + 1, seed_, salt));

    const float top = a + (b - a) * u;
    const float bot = c + (d - c) * u;
    return (top + (bot - top) * v) * 2.0f - 1.0f;   // -> [-1, 1]
}

void Level0Field::findSpawn(int& outCx, int& outCz) const noexcept {
    // Spiral outward from the origin to the first lit, open cell. Bounded so a
    // pathological seed cannot spin; the fallback is still walkable ground.
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
    // With an endless world there is no "furthest cell" to discover, so the run
    // length is authored: a fixed distance in a seed-chosen direction.
    const int distance = 110 + static_cast<int>(
        hashFloat(hashCell(spawnCx, spawnCz, seed_, kSaltExitLen)) * 60.0f);
    findExitNear(spawnCx, spawnCz, distance, outCx, outCz);
}

void Level0Field::findExitNear(int fromCx, int fromCz, int distance, int& outCx, int& outCz) const noexcept {
    const float angle = hashFloat(hashCell(fromCx, fromCz, seed_, kSaltExitDir)) * 6.2831853f;
    const int targetX = fromCx + static_cast<int>(std::cos(angle) * distance);
    const int targetZ = fromCz + static_cast<int>(std::sin(angle) * distance);

    // Snap onto real floor. A door embedded in solid fill would be unreachable,
    // which for the only way out of the level is the worst possible failure.
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

} // namespace map
} // namespace omni
