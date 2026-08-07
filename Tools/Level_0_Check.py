#!/usr/bin/env python3
"""
Level_0_Check.py — the level generator probe.

The level is an infinite pure function, which means nothing about it can be
eyeballed in an editor and a bad seed cannot be spotted until a player is
already lost in it. This exercises the generator directly on the host, over
many seeds, and asserts the properties a run actually depends on:

  * the spawn and the exit are on open floor;
  * the exit is REACHABLE from the spawn — a flood fill gets there;
  * relocated exits are reachable too, from wherever the player has wandered;
  * open space is neither so sparse the level is a maze of dead ends nor so
    dense it is one undifferentiated hall;
  * every open cell has some light, so nowhere is pitch black;
  * the light is not FLAT — there are pools under the fittings and gloom
    between them. A previous tuning measured 1.00x contrast: a uniformly lit
    light box with no pools at all, and nothing in the build could see it;
  * enough of the plan is corridor that it reads as a maze rather than as one
    continuous open floor;
  * columns concentrate in the unlit halls, and never seal a corridor.

The probe itself is C++ because the generator is: it is embedded below, written
to a temporary file and compiled on demand, which keeps Tools/ to the eight
files it is meant to have.

    python3 Tools/Level_0_Check.py [seed-count]
"""
import os
import subprocess
import sys
import tempfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
NATIVE = os.path.join(REPO, "Backrooms/Source/Main/Native")

PROBE = r"""
#include "Map/Level_0.h"

#include <algorithm>
#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <queue>
#include <unordered_set>
#include <vector>

namespace {

using omni::map::Level0Field;

struct Key {
    int x, z;
    bool operator==(const Key& o) const noexcept { return x == o.x && z == o.z; }
};
struct KeyHash {
    size_t operator()(const Key& k) const noexcept {
        return std::hash<long long>{}((static_cast<long long>(k.x) << 32) ^ (unsigned)k.z);
    }
};

/** Flood fill from [sx,sz], bounded, reporting whether [gx,gz] was reached and
 *  how many cells the connected region holds. */
struct FloodResult {
    bool reachedGoal = false;
    int  cellsVisited = 0;
    int  maxDepth = 0;
};

FloodResult flood(const Level0Field& f, int sx, int sz, int gx, int gz, int budget) {
    FloodResult out;
    if (!f.isOpen(sx, sz)) return out;

    std::unordered_set<Key, KeyHash> seen;
    std::queue<std::pair<Key, int>> q;
    q.push({{sx, sz}, 0});
    seen.insert({sx, sz});

    static const int dx[4] = {1, -1, 0, 0};
    static const int dz[4] = {0, 0, 1, -1};

    while (!q.empty() && out.cellsVisited < budget) {
        auto [cur, depth] = q.front();
        q.pop();
        out.cellsVisited++;
        out.maxDepth = std::max(out.maxDepth, depth);
        if (cur.x == gx && cur.z == gz) {
            out.reachedGoal = true;
            return out;
        }
        for (int i = 0; i < 4; ++i) {
            Key nb{cur.x + dx[i], cur.z + dz[i]};
            if (seen.count(nb)) continue;
            if (!f.isOpen(nb.x, nb.z)) continue;
            seen.insert(nb);
            q.push({nb, depth + 1});
        }
    }
    return out;
}

int failures = 0;

void check(bool ok, const char* what, unsigned long long seed) {
    if (!ok) {
        std::printf("  FAIL  seed=%llu  %s\n", seed, what);
        failures++;
    }
}

} // namespace

int main(int argc, char** argv) {
    const int seedCount = argc > 1 ? std::atoi(argv[1]) : 40;
    std::printf("Level 0 probe over %d seeds\n\n", seedCount);

    double openSum = 0, litSum = 0, darkestSum = 0, contrastSum = 0;
    double corridorSum = 0, pillarDarkSum = 0, floorPowerSum = 0;
    int    worstReachDepth = 1 << 30;

    for (int s = 0; s < seedCount; ++s) {
        const unsigned long long seed = 0x9E3779B97F4A7C15ULL * (s + 1);
        Level0Field field(seed);

        int spawnX = 0, spawnZ = 0, exitX = 0, exitZ = 0;
        field.findSpawn(spawnX, spawnZ);
        field.findExit(spawnX, spawnZ, exitX, exitZ);

        check(field.isOpen(spawnX, spawnZ), "spawn is not open floor", seed);
        check(field.isOpen(exitX, exitZ), "exit is not open floor", seed);

        // Reachability. The budget is generous: the exit sits 110-170 cells out,
        // so a legitimate route can touch tens of thousands of cells.
        FloodResult r = flood(field, spawnX, spawnZ, exitX, exitZ, 400000);
        check(r.reachedGoal, "exit NOT reachable from spawn", seed);
        if (r.reachedGoal) worstReachDepth = std::min(worstReachDepth, r.maxDepth);

        // Relocation: pretend the player wandered a long way off and ask for a
        // fresh exit, then prove that one is reachable from where they stand.
        int wanderX = spawnX + 260, wanderZ = spawnZ - 190;
        // Walk to the nearest open cell so the flood has somewhere to start.
        for (int radius = 0; radius < 40; ++radius) {
            bool found = false;
            for (int dz = -radius; dz <= radius && !found; ++dz)
                for (int dx = -radius; dx <= radius && !found; ++dx)
                    if (std::max(std::abs(dx), std::abs(dz)) == radius &&
                        field.isOpen(wanderX + dx, wanderZ + dz)) {
                        wanderX += dx; wanderZ += dz; found = true;
                    }
            if (found) break;
        }
        int relocX = 0, relocZ = 0;
        field.findExitNear(wanderX, wanderZ, 46, relocX, relocZ);
        check(field.isOpen(relocX, relocZ), "relocated exit is not open floor", seed);
        FloodResult rr = flood(field, wanderX, wanderZ, relocX, relocZ, 200000);
        check(rr.reachedGoal, "relocated exit NOT reachable", seed);

        // Density and light, sampled over a window around the spawn.
        const int half = 60;
        int open = 0, total = 0, unlit = 0;
        float darkest = 1e9f, brightest = 0.0f;
        std::vector<omni::map::CellSample> samples(26 * 26);
        for (int cz = -half; cz < half; cz += 24) {
            for (int cx = -half; cx < half; cx += 24) {
                field.sampleChunk((spawnX + cx) / 24, (spawnZ + cz) / 24, 24, samples.data());
                for (const auto& cell : samples) {
                    total++;
                    if (cell.solid) continue;
                    open++;
                    if (cell.light < 0.02f) unlit++;
                    darkest = std::min(darkest, cell.light);
                    brightest = std::max(brightest, cell.light);
                }
            }
        }
        const double openFrac = total ? double(open) / total : 0.0;
        openSum += openFrac;
        litSum += open ? 1.0 - double(unlit) / open : 0.0;
        darkestSum += darkest;

        // Tuned around the shipped generator, which measures ~0.38. Wide enough
        // to allow seed-to-seed variation, tight enough that a regression which
        // merges the rooms back into one plate (or starves them into a warren)
        // trips it.
        check(openFrac > 0.25, "floor plan too sparse (a warren of dead ends)", seed);
        check(openFrac < 0.55, "floor plan too open (one undifferentiated hall)", seed);
        check(unlit == 0, "some open cells are pitch black", seed);

        // Light has to come from the fittings, which means bright directly under
        // one and gloomy between them. A previous tuning overlapped the falloff
        // so heavily that every open cell measured the same value to two decimal
        // places — 1.00x contrast, a uniformly lit light box with no pools at
        // all. Nothing in the build could see that; this can.
        const float contrast = darkest > 0.0001f ? brightest / darkest : 0.0f;
        contrastSum += contrast;
        check(contrast > 3.0f, "lighting is too flat — no pools under the fittings", seed);

        // Corridor share and column placement, over a window around the spawn.
        //
        // Both were things the plan silently lacked and nothing could see. The
        // level measured 90% junction cells — open floor with floor on every
        // side, i.e. one continuous room rather than a maze — and columns landed
        // at a flat probability with no relationship to the lighting at all
        // (0.567 average mains health at a column against 0.548 over open
        // floor). A corridor you can walk down with walls either side, and
        // colonnades that belong to the dark halls, are both structural
        // properties, so they get structural assertions.
        // Sampled over +-110 cells. At +-70 the window held as few as fifteen
        // columns, and fifteen samples cannot support a claim about where
        // columns tend to be — three seeds tripped the check while the
        // relationship was in fact present on every one of them.
        long corridorCells = 0, junctionCells = 0, deadEnds = 0;
        double pillarPower = 0.0, floorPower = 0.0;
        long pillarCount = 0, floorCount = 0;
        for (int cz = spawnZ - 110; cz < spawnZ + 110; ++cz) {
            for (int cx = spawnX - 110; cx < spawnX + 110; ++cx) {
                if (field.isPillar(cx, cz)) { pillarPower += field.powerAt(cx, cz); pillarCount++; }
                if (!field.isOpen(cx, cz)) continue;
                floorPower += field.powerAt(cx, cz); floorCount++;
                int n = 0;
                if (field.isOpen(cx + 1, cz)) n++;
                if (field.isOpen(cx - 1, cz)) n++;
                if (field.isOpen(cx, cz + 1)) n++;
                if (field.isOpen(cx, cz - 1)) n++;
                if (n <= 1) deadEnds++; else if (n == 2) corridorCells++; else junctionCells++;
            }
        }
        const long walkable = corridorCells + junctionCells + deadEnds;
        const double corridorFrac = walkable ? double(corridorCells) / walkable : 0.0;
        corridorSum += corridorFrac;
        check(corridorFrac > 0.20,
              "hardly any corridors — the plan is one open floor, not a maze", seed);

        if (pillarCount >= 20 && floorCount > 0) {
            const double pAvg = pillarPower / pillarCount;
            const double fAvg = floorPower / floorCount;
            pillarDarkSum += pAvg; floorPowerSum += fAvg;
            // A ratio, not an absolute gap. Where a whole neighbourhood has
            // lost power both figures collapse toward zero and any fixed
            // margin between them stops being meaningful, even though the
            // columns are still sitting in the darkest part of it.
            check(pAvg < fAvg * 0.75,
                  "columns are not concentrated in the unlit halls", seed);
        }

        // Columns must never seal anything: every pillar needs floor all round.
        for (int cz = spawnZ - 70; cz < spawnZ + 70; ++cz) {
            for (int cx = spawnX - 70; cx < spawnX + 70; ++cx) {
                if (!field.isPillar(cx, cz)) continue;
                bool boxed = false;
                for (int dz = -1; dz <= 1 && !boxed; ++dz)
                    for (int dx = -1; dx <= 1; ++dx) {
                        if (dx == 0 && dz == 0) continue;
                        if (!field.isOpenBase(cx + dx, cz + dz)) { boxed = true; break; }
                    }
                check(!boxed, "a column stands in a corridor it could seal", seed);
                if (boxed) { cz = spawnZ + 70; break; }
            }
        }

        // ---- What the mesher is allowed to assume --------------------------
        //
        // The chunk mesher builds geometry per cell, and every piece of it has
        // to land on something. Floors and ceilings do by construction, and a
        // wall face is emitted exactly where an open cell meets a solid one, so
        // those cannot float. The door frame is the one piece that is placed
        // rather than derived: it stands in the plane ACROSS the passage, and
        // it only meets a wall at each end because featureAt tags a doorway
        // only where solid sits on two opposite sides.
        //
        // That assumption used to be unstated, and the frame that relied on it
        // was a single horizontal slab at 0.82 of the wall height whose two
        // long edges ended in open air — the texture left hanging in mid-air.
        // Stating it here means the mesher can be trusted to build against it.
        for (int cz = spawnZ - 90; cz < spawnZ + 90; ++cz) {
            for (int cx = spawnX - 90; cx < spawnX + 90; ++cx) {
                const uint8_t f = field.featureAt(cx, cz);
                if (f == omni::map::kFeatureNone) continue;

                if (f == omni::map::kFeaturePillar) {
                    check(!field.isOpen(cx, cz),
                          "a column is tagged on open floor, so it has no volume", seed);
                    continue;
                }

                // Everything else is a feature of a cell you can stand in.
                check(field.isOpen(cx, cz),
                      "a floor feature is tagged inside solid rock", seed);

                if (f == omni::map::kFeatureDoorway) {
                    const bool acrossX = !field.isOpen(cx, cz - 1) && !field.isOpen(cx, cz + 1);
                    const bool acrossZ = !field.isOpen(cx - 1, cz) && !field.isOpen(cx + 1, cz);
                    check(acrossX || acrossZ,
                          "a doorway with no wall on either side — its frame would "
                          "hang in the air with nothing to meet", seed);
                }
                if (f == omni::map::kFeatureAlcove) {
                    int walls = 0;
                    if (!field.isOpen(cx - 1, cz)) walls++;
                    if (!field.isOpen(cx + 1, cz)) walls++;
                    if (!field.isOpen(cx, cz - 1)) walls++;
                    if (!field.isOpen(cx, cz + 1)) walls++;
                    check(walls >= 3, "an alcove that is not a dead end", seed);
                }
            }
        }

        // ---- The two copies of the rule must agree -------------------------
        //
        // featureAt() answers one cell; sampleChunk() fills a whole chunk and
        // carries its own copy of the same conditions, because five isOpen()
        // calls per cell was the most expensive thing in the generator. The
        // mesher reads sampleChunk. Every assertion above reads featureAt.
        //
        // So a rule changed in one and not the other is invisible: the audit
        // keeps passing while the geometry is built from something else. That
        // is not hypothetical — injecting a bad doorway rule into sampleChunk
        // alone, to test the check above, passed cleanly.
        {
            const int cells = 24;
            // sampleChunk emits the chunk PLUS a one-cell apron, so the buffer
            // is (cells+2)^2 and local cell (lx,lz) sits at (lz+1)*side+(lx+1).
            // Sizing it cells*cells overruns the vector, which is how the first
            // run of this check ended in a heap corruption rather than a
            // finding.
            const int side = cells + 2;
            std::vector<omni::map::CellSample> chunk(side * side);
            for (int chz = -2; chz <= 2; ++chz) {
                for (int chx = -2; chx <= 2; ++chx) {
                    const int cxi = spawnX / cells + chx;
                    const int czi = spawnZ / cells + chz;
                    const int bx = cxi * cells, bz = czi * cells;
                    field.sampleChunk(cxi, czi, cells, chunk.data());
                    for (int lz = 0; lz < cells; ++lz) {
                        for (int lx = 0; lx < cells; ++lx) {
                            const auto& c = chunk[(lz + 1) * side + (lx + 1)];
                            const int cx = bx + lx, cz = bz + lz;
                            check(bool(c.solid) == !field.isOpen(cx, cz),
                                  "sampleChunk and isOpen disagree about solid", seed);
                            check(c.feature == field.featureAt(cx, cz),
                                  "sampleChunk and featureAt disagree about a cell's "
                                  "feature — the mesher builds from one and every check "
                                  "here reads the other", seed);
                        }
                    }
                }
            }
        }
    }

    std::printf("\nopen fraction    avg %.3f\n", openSum / seedCount);
    std::printf("lit open cells   avg %.4f\n", litSum / seedCount);
    std::printf("darkest cell     avg %.3f\n", darkestSum / seedCount);
    std::printf("light contrast   avg %.2fx\n", contrastSum / seedCount);
    std::printf("corridor cells   avg %.1f%%\n", 100.0 * corridorSum / seedCount);
    std::printf("mains at columns avg %.3f  (open floor %.3f)\n",
                pillarDarkSum / seedCount, floorPowerSum / seedCount);
    std::printf("shortest route to exit (cells): %d\n", worstReachDepth);
    std::printf("\n%s (%d failure%s)\n",
                failures ? "FAILED" : "PASSED", failures, failures == 1 ? "" : "s");
    return failures ? 1 : 0;
}
"""


def main() -> int:
    seeds = sys.argv[1] if len(sys.argv) > 1 else "40"
    with tempfile.TemporaryDirectory() as tmp:
        src = os.path.join(tmp, "probe.cpp")
        exe = os.path.join(tmp, "probe")
        with open(src, "w", encoding="utf-8") as fh:
            fh.write(PROBE)
        build = subprocess.run(
            ["g++", "-std=c++20", "-O2", "-I", NATIVE, src,
             os.path.join(NATIVE, "Map/Level_0.cpp"), "-o", exe],
            capture_output=True, text=True)
        if build.returncode != 0:
            print("level probe failed to compile:\n" + build.stderr[:3000])
            return 2
        return subprocess.run([exe, seeds]).returncode


if __name__ == "__main__":
    sys.exit(main())
