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
constexpr int kRoomsPerSector = 4;
// Rooms used to run to half-extent 9 — nineteen cells across, most of a sector.
// Four of those per sector overlapped into one continuous plate, and the level
// measured two-thirds open floor: a hall you walk across, not a place you get
// lost in. Capped at 5 they stay distinct, which is what puts walls between
// them and turns the plan back into rooms joined by corridors.
constexpr int kMaxRoomHalf    = 5;   // must stay < kSectorSize / 2

// Troffers every four cells — 12.8 m.
//
// The previous 3-cell spacing paired with a 1.9-cell falloff made the level
// measure 1.00x contrast: every open cell received 1.73, to two decimal places,
// everywhere. The lights overlapped so heavily that the floor plan was lit like
// a photographic light box, which is why it read as "bright wherever there is a
// fluorescent" — there was no wherever, it was uniformly bright.
//
// Spacing 4 with sigma 0.95 measures 8.5x: 1.16 directly under a fitting,
// falling to 0.14 midway between two. That is a pool of light under each
// troffer with genuine gloom in between, which is what the lobby actually
// looks like.
constexpr int kFixtureSpacing = 4;

// How far a lattice point will look for floor to hang its fitting over. Two
// cells reaches every corner of a 4x4 block, so a block with any floor in it
// gets a tube.
constexpr int kFixtureSnap = 2;

// How far one tube meaningfully reaches, in cells. Sets the working margin
// sampleChunk needs around the chunk it is asked for.
constexpr int   kLightRadius = 4;
// Gaussian falloff width, in cells.
//
// This was 0.95, chosen against a lattice that only lit cells whose coordinates
// were multiples of four. At that width a cell two from a tube — the midpoint
// between two fittings, the single most common place to be standing — receives
// exp(-4 / 1.805) = 0.11 of its output. Combined with the lattice that produced
// a level where most of the floor was unlit; on its own it still leaves the
// halfway point at a tenth. Wide enough that adjacent pools meet, which is what
// a run of ceiling troffers actually does, and the contrast between under-a-tube
// and between-two stays around 4x.
constexpr float kLightSigma  = 1.70f;
constexpr float kLitOutput   = 1.05f;
constexpr float kDeadOutput  = 0.05f;
/** Floor under everything. Even an unpowered corridor is not pitch black: light
 *  bleeds in from the powered ones and off every surface.
 *
 *  It was 0.055, which the scene shader turns into 0.09 + 0.45*0.055*1.3 = 0.12
 *  of albedo on a wall — a wall you cannot see. The point of a failed section is
 *  that you reach for the torch, not that the screen goes off; at 0.20 the
 *  geometry is just legible and the torch is still worth having. Contrast
 *  against a lit pool stays about 7x, so this is a floor, not a second light. */
constexpr float kAmbient     = 0.20f;

// Salts keep the different attribute layers from correlating with each other.
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
        // There was an h3 here, left over from when room dimensions were drawn
        // continuously at random. The archetype catalogue below replaced that,
        // and nothing has read h3 since.

        const int baseX = sx * kSectorSize;
        const int baseZ = sz * kSectorSize;

        out[i].cx = baseX + hashRange(h0, 4, kSectorSize - 5);
        out[i].cz = baseZ + hashRange(h1, 4, kSectorSize - 5);

        // Rooms come from a fixed catalogue rather than from a fresh random
        // size each time.
        //
        // This is the déjà vu. Continuous randomness makes every room subtly
        // unlike every other, and the eye reads that as "somewhere new" no
        // matter how similar it is. Drawing from eight archetypes means you
        // genuinely walk into the *same room* again half a mile away — same
        // proportions, same column placement — and cannot tell whether you have
        // looped back. That doubt is the whole feeling this place trades on,
        // and it is what actual Level 0 does: identical rooms, forever.
        static const int kArchetype[8][2] = {
            {3, 2},   // long hall
            {2, 3},   // long hall, turned
            {2, 2},   // square bay
            {1, 2},   // small office
            {3, 1},   // narrow run
            {1, 3},   // narrow run, turned
            {2, 1},   // stub
            {1, 1}    // closet
        };
        const int arch = static_cast<int>((h2 >> 3) & 7ULL);
        out[i].halfW = kArchetype[arch][0];
        out[i].halfD = kArchetype[arch][1];
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
    // A building's columns sit on a structural bay, not wherever a hash says so.
    // Locking them to a 7-cell lattice is what makes a long sightline read as a
    // colonnade receding into the haze rather than as scattered obstacles.
    const int mx = ((cx % 7) + 7) % 7;
    const int mz = ((cz % 7) + 7) % 7;
    if (mx != 0 || mz != 0) return false;

    // Columns belong to the dark halls.
    //
    // They used to be scattered at a flat 70% wherever the lattice fell, and
    // measurement said as much: the average mains health at a column was 0.567
    // against 0.548 over open floor — statistically no relationship at all. So
    // the pillared bays and the dead-lighting regions were two unrelated
    // things sprinkled over the same map.
    //
    // Tying the two together gives the level a place with a character: the
    // parts where the power has failed are also the parts that open out into
    // deep colonnaded halls, and the lit parts are ordinary offices. That is
    // the reading of "the dark sections are the ones with the columns".
    const float power = powerAt(cx, cz);
    const float chance = 0.06f + (1.0f - power) * 0.88f;
    if (hashFloat(hashCell(cx, cz, seed_, kSaltPillar)) >= chance) return false;

    // A column may only stand where there is floor all round it.
    //
    // This is a correctness guard, not a styling one. Pillars punch holes back
    // into open floor, and a corridor is as narrow as three cells — so a column
    // landing in one could seal it, and sealing the wrong corridor can cut the
    // exit off from the player entirely with no way for them to know. Requiring
    // eight open neighbours means a column can only ever appear in the middle of
    // a room, where going round it is trivial.
    //
    // Only reached for lattice cells that already passed the hash, so this costs
    // about a tenth of a percent on top of an ordinary query.
    for (int dz = -1; dz <= 1; ++dz) {
        for (int dx = -1; dx <= 1; ++dx) {
            if (dx == 0 && dz == 0) continue;
            if (!isOpenBase(cx + dx, cz + dz)) return false;
        }
    }
    return true;
}

bool Level0Field::isOpen(int cx, int cz) const noexcept {
    // Pillars punch back into otherwise open floor.
    return isOpenBase(cx, cz) && !isPillar(cx, cz);
}

/** The floor plan before columns are subtracted from it. Kept separate so
 *  isPillar() can ask whether a cell has floor all round it without
 *  recursing back through itself. */
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

            // Mostly single-cell corridors — 3.2 m, one lane.
            //
            // Width is what decides whether a run reads as a corridor at all. A
            // three-wide run has open floor on every side of its centre line, so
            // it is indistinguishable from a room; measured, the level was 90%
            // junction cells and only 10% corridor. Biasing hard toward width 1
            // is what puts walls back on either side of you.
            const uint64_t wHash = hashCell(nx, nz, seed_, kSaltCorrW);
            const int halfWidth = (wHash % 10ULL < 8ULL) ? 0 : 1;

            // Internal circulation: the sector's own rooms are chained together
            // rather than all hanging off one hub. Real floor plates have halls
            // that thread between rooms, and without these the plan read as a
            // few disconnected blobs joined by a single trunk.
            //
            // Every one of these runs stays inside the sector that owns it, so
            // the one-ring scan still sees every corridor that can reach a cell.
            bool onInternal = false;
            for (int i = 0; i + 1 < kRoomsPerSector && !onInternal; ++i) {
                onInternal = onCorridor(cx, cz,
                                        rooms[i].cx, rooms[i].cz,
                                        rooms[i + 1].cx, rooms[i + 1].cz, halfWidth);
            }
            if (onInternal) { inside = true; break; }

            // Corridors to the east and south neighbours. Both sides of a
            // boundary compute the same endpoints, so the runs line up.
            sectorRooms(nx + 1, nz, seed_, east);
            sectorRooms(nx, nz + 1, seed_, south);

            if (onCorridor(cx, cz, rooms[0].cx, rooms[0].cz, east[0].cx, east[0].cz, halfWidth) ||
                onCorridor(cx, cz, rooms[0].cx, rooms[0].cz, south[0].cx, south[0].cz, halfWidth)) {
                inside = true;
                break;
            }

            // Further links across the same boundaries, from the sector's other
            // rooms. One trunk per edge made every route a forced march down the
            // same hallway; several parallel runs give the maze real
            // alternatives, and alternatives are what let you come back on
            // yourself without realising it.
            bool onExtra = false;
            for (int i = 1; i < kRoomsPerSector && !onExtra; ++i) {
                const uint64_t linkHash = hashCell(nx, nz, seed_, kSaltCorrW + static_cast<uint64_t>(i) * 31ULL);
                if ((linkHash & 3ULL) != 0) continue;      // roughly one in four
                onExtra = onCorridor(cx, cz, rooms[i].cx, rooms[i].cz, east[i].cx, east[i].cz, 0) ||
                          onCorridor(cx, cz, rooms[i].cx, rooms[i].cz, south[i].cx, south[i].cz, 0);
            }
            if (onExtra) { inside = true; break; }

            // A spine: one straight run the full width of the sector, on a line
            // the sector chooses for itself. Long uninterrupted sightlines are
            // most of what makes this place feel enormous, and a plan built only
            // from room-to-room hops never produces one.
            {
                const uint64_t spineHash = hashCell(nx, nz, seed_, kSaltSpine);
                const int baseX = nx * kSectorSize;
                const int baseZ = nz * kSectorSize;
                // One lane on each axis, so the spines of neighbouring sectors
                // form a lattice. A lattice is what makes it possible to walk a
                // full circle and arrive somewhere you have already been —
                // a tree of dead ends never does.
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
    // Two scales of value noise: broad regions where the mains have failed, and
    // finer hotspots. Returned as a continuous 0..1 rather than bucketed, so a
    // failing region fades in over tens of metres instead of switching on a
    // cell boundary.
    // The broad scale was 0.018 per cell — one wavelength every 55 cells, 178
    // metres. A player sees about two wavelengths at a time, and two samples of
    // anything is not a distribution: measured over six seeds, one had no failed
    // mains anywhere near the spawn and another had a third of its floor
    // unpowered. Two players in the same game were not in the same kind of
    // place.
    //
    // Swept against how much of the floor needs the torch, over sixty seeds:
    //
    //   178 m  min  n/a   p90  n/a   max  n/a   (measured before the rewrite)
    //    71 m  min  8.2   p90 38.0   max 45.4
    //    53 m  min  8.8   p90 33.6   max 45.5
    //    43 m  min  9.0   p90 33.8   max 38.4
    //    34 m  min 12.7   p90 30.5   max 34.9
    //
    // 34 m is where the spread stops shrinking and the darkest seed stops being
    // a different game from the brightest. A failed section is ten cells across
    // — a couple of rooms, something you walk through rather than get lost in —
    // and there are always several of them near you.
    const float broad = noise(cx * 0.095f, cz * 0.095f, kSaltBroad);
    // The hotspot layer has to stay finer than the layer it is perturbing, or
    // the two beat against each other and neither reads as its own scale.
    const float fine  = noise(cx * 0.180f, cz * 0.180f, kSaltFine);

    // Remap so most of the world is healthy and failure is the exception, with
    // a wide transition band on either side of the threshold.
    //
    // The threshold was -0.42, which left a fifth of the floor with no mains at
    // all — swept over ten seeds it measured 21.6% on average and 38.8% on the
    // worst. A fifth is not an exception. At -0.60 it is 10% on average, 7.3%
    // to 18.7% across seeds: dead sections you come across, rather than a world
    // that is dark as often as not.
    float health = (broad + 0.60f) / 0.66f;          // -0.60 -> 0, +0.06 -> 1
    health = health < 0.0f ? 0.0f : (health > 1.0f ? 1.0f : health);
    // Smoothstep, so the ramp has no visible kink where it meets the flats.
    health = health * health * (3.0f - 2.0f * health);
    // Hotspots push a little past full, which the caller clamps into a bloom.
    return health * (0.94f + fine * 0.22f);
}

uint8_t Level0Field::zoneAt(int cx, int cz) const noexcept {
    // Kept as a coarse classification for gameplay code that wants a category
    // rather than a number. Rendering uses the continuous value; bucketing it
    // is exactly what put hard steps between regions.
    const float p = powerAt(cx, cz);
    if (p < 0.10f) return kZoneDark;
    if (p < 0.45f) return kZoneDim;
    if (p > 0.92f) return kZoneBright;
    return kZoneNormal;
}

uint8_t Level0Field::fixtureAt(int cx, int cz) const noexcept {
    // The placement rule lives in exactly one place — snapFixture — and both
    // this and sampleChunk go through it. It was written out twice before, as a
    // modulo test here and a modulo test there, and when the bulk sampler
    // learned to snap onto floor this one did not: every caller asking a single
    // cell would have got the old lattice's answer while the mesh it was drawn
    // against used the new one.
    //
    // A cell carries a fitting when it is the cell its own block's lattice point
    // snapped to. Only cells within the snap radius of a lattice point can
    // qualify, so the search is over at most a handful of candidates.
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
    // No mains, no fitting worth drawing.
    if (power < 0.06f) return kFixtureNone;
    const float u = hashFloat(hashCell(cx, cz, seed_, kSaltFixture));
    // Failure probability rises smoothly as the mains weaken, instead of
    // switching at a zone boundary — so a dying region thins out gradually,
    // one dead tube at a time, the way a real one does.
    const float deadChance = 0.06f + (1.0f - power) * 0.72f;
    return u < deadChance ? kFixtureDead : kFixtureLit;
}

bool Level0Field::snapFixture(int latticeCx, int latticeCz,
                              int& outCx, int& outCz) const noexcept {
    if (isOpen(latticeCx, latticeCz)) {
        outCx = latticeCx; outCz = latticeCz;
        return true;
    }
    // Rings outward. Within a ring the order is fixed and the first open cell
    // wins, so the answer depends only on the coordinates — never on which
    // chunk asked, which is what keeps a tube from moving when you walk far
    // enough away for the chunk under it to be rebuilt from a different corner.
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

    // The emitted grid is the chunk plus a one-cell apron. Light gathering needs
    // to see kLightRadius beyond even that, because a tube just outside the
    // apron still lights cells inside it — and if we guessed instead, the guess
    // would show up as a seam exactly on the chunk boundary.
    const int outSide  = cells + 2;
    const int margin   = 1 + kLightRadius;
    const int workSide = cells + 2 * margin;
    const int baseX    = chunkX * cells - margin;
    const int baseZ    = chunkZ * cells - margin;

    // Bounded so this stays on the stack: 24 + 2*5 = 34 -> 1156 cells.
    constexpr int kMaxWork = 96;
    if (workSide > kMaxWork) return;

    static thread_local bool  open [kMaxWork * kMaxWork];
    static thread_local float power[kMaxWork * kMaxWork];
    static thread_local unsigned char fixture[kMaxWork * kMaxWork];

    // Pass 1 — occupancy and mains. One isOpen() per cell, once. The old
    // per-cell path evaluated it seven times over for every cell it resolved.
    for (int z = 0; z < workSide; ++z) {
        for (int x = 0; x < workSide; ++x) {
            const int i = z * workSide + x;
            const int cx = baseX + x, cz = baseZ + z;
            open[i]  = isOpen(cx, cz);
            power[i] = open[i] ? powerAt(cx, cz) : 0.0f;
        }
    }

    // Pass 2 — fittings, one per lattice block, snapped onto floor.
    //
    // This used to be a single modulo test: a cell carried a fitting if it was
    // open AND both its coordinates were multiples of the spacing. The lattice
    // is global and the floor plan is not, so whether a corridor was lit came
    // down to its coordinate parity. A one-cell corridor running along z = 7
    // never touches a lattice row, so it received no fitting anywhere along its
    // length — not a dim one, none — and the only light reaching it was the
    // 0.055 ambient floor. Measured over six seeds, 70% of all open floor sat
    // under 0.15 illuminance and 54% under 0.08, which the scene shader renders
    // at 9% of albedo. The longest unbroken walk through cells you cannot see
    // in was 60 cells: 192 metres of black corridor.
    //
    // The block is still the unit — one fitting per 4x4, so the density and the
    // spacing are unchanged — but the fitting now looks for the floor instead of
    // waiting for the floor to arrive under it. Rings outward from the lattice
    // point and takes the first open cell, in a fixed order, so it stays a pure
    // function of the coordinates: two clients with the same seed still place
    // every tube identically without exchanging anything.
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
            // Two lattice points can snap to the same cell where the floor is
            // thin. One tube, not two stacked on each other.
            if (fixture[i] != kFixtureNone) continue;
            fixture[i] = fixtureFor(fx, fz, power[i]);
        }
    }

    // Pass 3 — gather. Illuminance at a cell is what every tube within reach
    // actually throws at it, summed. This is the whole point of the rewrite:
    // brightness now comes FROM the lights, so a corridor is bright because it
    // has working tubes over it and dims as they thin out, with the falloff
    // doing the blending. Nothing anywhere quantises it into a band.
    //
    // Precomputed falloff by squared cell distance — the kernel is tiny and
    // the same for every cell, so there is no reason to call exp() per pair.
    constexpr int kR = kLightRadius;
    float kernel[(2 * kR + 1) * (2 * kR + 1)];
    for (int dz = -kR; dz <= kR; ++dz) {
        for (int dx = -kR; dx <= kR; ++dx) {
            const float d2 = static_cast<float>(dx * dx + dz * dz);
            const float g  = std::exp(-d2 / (2.0f * kLightSigma * kLightSigma));
            // Hard cut at the radius, faded to zero at the rim so the truncation
            // itself cannot become a visible ring.
            const float rim = 1.0f - d2 / static_cast<float>(kR * kR + 1);
            kernel[(dz + kR) * (2 * kR + 1) + (dx + kR)] = g * (rim > 0.0f ? rim : 0.0f);
        }
    }

    for (int z = 0; z < outSide; ++z) {
        for (int x = 0; x < outSide; ++x) {
            // Output cell (x-1, z-1) of the chunk maps to work index offset by
            // the extra light margin.
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

            // Features read straight off the cached occupancy — five isOpen()
            // calls per cell used to be the single most expensive thing here.
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
