#!/usr/bin/env python3
"""
Entity_Check.py — the creatures.

What a monster does is the part of this game that most needs checking and used
to be the hardest to check: it is emergent, it only shows itself during a run,
and a bug in it reads as "the game felt wrong" rather than as a crash. It also
used to live inside Engine.cpp, which needs jni.h, android/* and aaudio, so
nothing about it could be built here at all.

The AI now lives in Native/Entity/, which compiles against Map/ and nothing
else. So this tool does the thing it could not do before: it builds a probe
against the real headers, puts a creature in the real Level 0, and watches.

Static half:

  * the spawn configuration is coherent — counts, speeds and intervals are
    ordered by difficulty and none of them is zero or absurd;
  * Level 0 carries the one creature it is meant to carry;
  * the entity JNI surface Kotlin declares is actually exported.

Behavioural half, run against the compiled AI:

  * sight is blocked by walls;
  * hearing scales with how loud the player is, monotonically;
  * losing sight sends it to the last known position rather than resetting it;
  * the flashlight slows it, then drives it off;
  * a creature driven off never dies, and always has a way back — the check
    that matters most, because "retreat" and "gone forever" look identical for
    the first thirty seconds and only one of them is what was asked for.

    python3 Tools/Entity_Check.py
"""
from __future__ import annotations

import glob
import os
import re
import subprocess
import sys
import tempfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
NATIVE = os.path.join(REPO, "Backrooms/Source/Main/Native")
KOTLIN = os.path.join(REPO, "Backrooms/Source/Main/Kotlin/com/omni/backrooms")

failures: list[str] = []


def check(ok: bool, what: str) -> None:
    if not ok:
        failures.append(what)


def section(title: str) -> None:
    print(f"\n── {title}")


def read(path: str) -> str:
    return open(path, encoding="utf-8").read() if os.path.exists(path) else ""


# ===========================================================================
# Static checks
# ===========================================================================

def check_spawn_config() -> None:
    section("Spawn configuration")
    text = read(os.path.join(KOTLIN, "Backrooms.kt"))
    rows = {}
    for m in re.finditer(
            r'(?:"(easy|hard)"|else)\s*->\s*SpawnConfig\('
            r'count\s*=\s*(\d+),\s*speedMult\s*=\s*([\d.]+)f,\s*'
            r'sightMult\s*=\s*([\d.]+)f,\s*spawnIntervalMs\s*=\s*([\d_]+)\)', text):
        name = m.group(1) or "normal"
        rows[name] = (int(m.group(2)), float(m.group(3)), float(m.group(4)),
                      int(m.group(5).replace("_", "")))

    check(len(rows) == 3, f"expected easy/normal/hard spawn configs, found {sorted(rows)}")
    for name, (count, speed, sight, interval) in sorted(rows.items()):
        check(count >= 1, f"{name}: spawns no creatures at all")
        check(0.1 <= speed <= 3.0, f"{name}: speedMult {speed} out of range")
        check(0.1 <= sight <= 3.0, f"{name}: sightMult {sight} out of range")
        check(interval >= 1000, f"{name}: spawn interval {interval}ms is a flood")
        print(f"   {name:7s} count={count} speed={speed} sight={sight} interval={interval}ms")

    if {"easy", "normal", "hard"} <= set(rows):
        e, n, h = rows["easy"], rows["normal"], rows["hard"]
        check(e[0] <= n[0] <= h[0], "creature count is not ordered easy <= normal <= hard")
        check(e[1] <= n[1] <= h[1], "speed is not ordered easy <= normal <= hard")
        check(e[2] <= n[2] <= h[2], "sight is not ordered easy <= normal <= hard")

        # Level 0 is meant to hold exactly one creature on every difficulty.
        # A number here that drifts back up is not a crash, it is the level
        # quietly becoming a different game.
        for name, row in sorted(rows.items()):
            check(row[0] == 1,
                  f"{name}: Level 0 is meant to carry exactly one creature, not {row[0]}")
        # And nothing may top it up mid-run.
        longest_plausible_run_ms = 30 * 60 * 1000
        for name, row in sorted(rows.items()):
            check(row[3] >= longest_plausible_run_ms // 2,
                  f"{name}: spawn interval {row[3]}ms would add creatures during a run")


def check_jni_surface() -> None:
    section("Entity JNI surface")
    exported = set()
    for path in glob.glob(os.path.join(NATIVE, "**/*.cpp"), recursive=True):
        exported.update(re.findall(
            r"Java_com_omni_backrooms_NativeBridge_([A-Za-z0-9_]+)", read(path)))
    declared = set(re.findall(r"external fun\s+([A-Za-z0-9_]+)\s*\(",
                              read(os.path.join(KOTLIN, "Service.kt"))))

    wanted = {"initEntities", "spawnEntity", "tickEntities", "damageEntity", "destroyEntities"}
    for name in sorted(wanted):
        check(name in declared, f"Kotlin does not declare {name}")
        check(name in exported, f"native does not export {name}")

    # The dissolve is the eleventh float per entity. If native stops writing it
    # or Kotlin stops reading it, a driven-off creature stays visible and the
    # retreat looks like a bug rather than a mechanic.
    service = read(os.path.join(KOTLIN, "Service.kt"))
    m = re.search(r"const val FLOATS_PER_ENTITY = (\d+)", service)
    check(m is not None, "FLOATS_PER_ENTITY not found in Service.kt")
    engine = read(os.path.join(NATIVE, "Engine.cpp"))
    n = re.search(r"const int fpn = (\d+);", engine)
    check(n is not None, "the per-entity float count was not found in Engine.cpp")
    if m and n:
        check(m.group(1) == n.group(1),
              f"Kotlin reads {m.group(1)} floats per entity, native writes {n.group(1)}")
        print(f"   {len(wanted)} entity calls bound, {m.group(1)} floats per entity both sides")


# ===========================================================================
# Behavioural checks — compiled against the real AI
# ===========================================================================

PROBE = r"""
// Drives the real BehaviorTree through scenarios that are impossible to set up
// by hand on a device: an exact wall between two exact points, a player who is
// silent for a minute and then sprints, a torch held for precisely 2.4 seconds.
#include "Entity/Entity.h"

#include <cmath>
#include <cstdio>
#include <random>

using namespace omni;
using namespace omni::entity;

static Entity makeEntity(float x, float z, float sight = 20.0f, float hear = 18.0f) {
    Entity e{};
    e.pos = {x, 0.0f, z};
    e.speed = 2.4f;
    e.sightRadius = sight;
    e.hearRadius = hear;
    e.aggroRadius = 12.0f;
    e.attackRadius = 1.4f;
    e.type = EntityType::HoundDog;      // sound-and-sight driven; the plainest
    e.state = AIState::Wander;
    e.hp = e.maxHp = 100.0f;
    e.wanderTimer = 1.0f;
    e.active = true;
    return e;
}

/** A straight run of open cells along +Z, found by search so no test depends on
 *  where the generator happens to have put a room. The torch and retreat tests
 *  need real open floor: placed at arbitrary world coordinates, a creature ends
 *  up inside a wall and every line-of-sight test silently proves nothing. */
static bool findCorridor(const map::Level0Field& f, int length, int& ox, int& oz) {
    for (int r = 1; r < 80; ++r)
        for (int cx = -r; cx <= r; ++cx)
            for (int cz = -r; cz <= r; ++cz) {
                bool run = true;
                for (int k = 0; k < length && run; ++k)
                    if (!f.isOpen(cx, cz + k)) run = false;
                if (run) { ox = cx; oz = cz; return true; }
            }
    return false;
}

/** Centre of a cell, in metres. */
static Vec3f cellCentre(int cx, int cz) {
    const float c = map::Level0Field::kCell;
    return {map::Level0Field::worldX(cx) + c * 0.5f, 0.0f,
            map::Level0Field::worldZ(cz) + c * 0.5f};
}

static bool findOpenPair(const map::Level0Field& f, int& ox, int& oz) {
    return findCorridor(f, 3, ox, oz);
}

static int fails = 0;
static void expect(bool ok, const char* what) {
    if (!ok) { std::printf("FAIL %s\n", what); ++fails; }
}

// --- 1. Sight respects walls ------------------------------------------------
static void testLineOfSight(const map::Level0Field& f) {
    int ox = 0, oz = 0;
    if (!findOpenPair(f, ox, oz)) { std::printf("FAIL no open run of cells found\n"); ++fails; return; }
    Vec3f a = cellCentre(ox, oz);
    Vec3f b = cellCentre(ox, oz + 2);
    expect(hasLineOfSight(f, a, b, 50.0f), "clear corridor should have line of sight");

    // Now find a pair with something solid between them and confirm it blocks.
    bool foundBlocked = false;
    for (int cx = -40; cx <= 40 && !foundBlocked; ++cx)
        for (int cz = -40; cz <= 40 && !foundBlocked; ++cz) {
            if (!f.isOpen(cx, cz) || !f.isSolid(cx + 1, cz) || !f.isOpen(cx + 2, cz)) continue;
            Vec3f p = cellCentre(cx, cz);
            Vec3f q = cellCentre(cx + 2, cz);
            expect(!hasLineOfSight(f, p, q, 50.0f), "a wall between two cells must block sight");
            foundBlocked = true;
        }
    expect(foundBlocked, "no wall-separated cell pair found; the sight test proved nothing");

    // And distance still matters on its own.
    Vec3f far_{a.x + 400.0f, 0, a.z};
    expect(!hasLineOfSight(f, a, far_, 20.0f), "sight must still be bounded by range");

    // The raycast existing is not the same as the AI using it. Reverting
    // updateBlackboard to a bare `d < sightRadius` left every test above
    // passing, because they all called hasLineOfSight directly. This is the
    // one that actually catches it: a creature with a wall in front of it,
    // well inside its sight radius, must not see the player.
    std::mt19937 rng(9);
    bool provedBlocked = false, provedClear = false;
    for (int cx = -40; cx <= 40 && !provedBlocked; ++cx)
        for (int cz = -40; cz <= 40 && !provedBlocked; ++cz) {
            if (!f.isOpen(cx, cz) || !f.isSolid(cx + 1, cz) || !f.isOpen(cx + 2, cz)) continue;
            Vec3f here = cellCentre(cx, cz), there = cellCentre(cx + 2, cz);
            Entity blind = makeEntity(here.x, here.z, /*sight*/ 30.0f, /*hear*/ 0.0f);
            WorldSense s{}; s.playerPos = there; s.noise = 0.0f;
            BehaviorTree::tick(f, blind, s, 0.016f, rng);
            expect(!blind.bb.playerInSight,
                   "the AI must apply the wall test, not just have one available");
            provedBlocked = true;
        }
    expect(provedBlocked, "no wall-separated pair found; the AI sight test proved nothing");

    {
        Entity seeing = makeEntity(a.x, a.z, 30.0f, 0.0f);
        WorldSense s{}; s.playerPos = b; s.noise = 0.0f;
        BehaviorTree::tick(f, seeing, s, 0.016f, rng);
        expect(seeing.bb.playerInSight, "down a clear corridor it must see the player");
        provedClear = seeing.bb.playerInSight;
    }
    expect(provedClear, "the AI never sees the player even with a clear line");
}

// --- 2. Hearing scales with the player's noise ------------------------------
static void testNoiseBudget(const map::Level0Field& f) {
    std::mt19937 rng(1);
    float lastHeardAt = -1.0f;
    // For a rising noise level, the distance at which it is heard must not fall.
    for (float noise : {0.05f, 0.2f, 0.5f, 1.0f}) {
        float heardAt = 0.0f;
        for (float d = 0.5f; d < 30.0f; d += 0.25f) {
            Entity e = makeEntity(0, 0);
            WorldSense s{}; s.playerPos = {0, 0, d}; s.noise = noise;
            BehaviorTree::tick(f, e, s, 0.016f, rng);
            if (e.bb.heardNoise) heardAt = d;
        }
        expect(heardAt >= lastHeardAt,
               "a louder player must be audible at least as far as a quieter one");
        lastHeardAt = heardAt;
    }
    // Crouching has to actually buy something.
    Entity quiet = makeEntity(0, 0), loud = makeEntity(0, 0);
    WorldSense sq{}; sq.playerPos = {0, 0, 10.0f}; sq.noise = 0.15f;
    WorldSense sl = sq; sl.noise = 1.0f;
    std::mt19937 r2(2), r3(3);
    BehaviorTree::tick(f, quiet, sq, 0.016f, r2);
    BehaviorTree::tick(f, loud, sl, 0.016f, r3);
    expect(!quiet.bb.heardNoise && loud.bb.heardNoise,
           "at 10m a crouching player should be inaudible and a sprinting one heard");
}

// --- 3. It goes and looks where you were ------------------------------------
static void testInvestigate(const map::Level0Field& f) {
    std::mt19937 rng(4);
    Entity e = makeEntity(0, 0);
    // Seen, then gone: fake it by seeding the blackboard the way a sighting does.
    e.bb.lastKnownPlayerPos = {0, 0, 8.0f};
    e.bb.timeSincePlayerSeen = 0.5f;
    // Player is now far away, silent, and out of sight.
    WorldSense s{}; s.playerPos = {300.0f, 0, 300.0f}; s.noise = 0.0f;

    BehaviorTree::tick(f, e, s, 0.016f, rng);
    expect(e.state == AIState::Investigate,
           "after losing sight it should head for the last known position");

    const float before = dist2d(e.pos, e.bb.lastKnownPlayerPos);
    for (int i = 0; i < 60; ++i) BehaviorTree::tick(f, e, s, 0.016f, rng);
    expect(dist2d(e.pos, e.bb.lastKnownPlayerPos) < before,
           "investigating should close the distance to the last known position");

    // Interest is finite: past the grace period it stops caring.
    e.bb.timeSincePlayerSeen = kInvestigateGrace + 1.0f;
    e.pos = {0, 0, 0};
    BehaviorTree::tick(f, e, s, 0.016f, rng);
    expect(e.state != AIState::Investigate,
           "interest in a stale position must expire");
}

// --- 4. The torch slows it, then drives it off ------------------------------
static void testTorch(const map::Level0Field& f) {
    std::mt19937 rng(5);

    // A real corridor, so the beam is not blocked by a wall the test forgot to
    // look for. Player at the near end, creature two cells up it.
    int cx = 0, cz = 0;
    if (!findCorridor(f, 6, cx, cz)) { std::printf("FAIL no corridor found\n"); ++fails; return; }
    const Vec3f playerAt = cellCentre(cx, cz);
    const Vec3f aheadAt  = cellCentre(cx, cz + 2);

    WorldSense s{};
    s.playerPos = playerAt; s.noise = 0.5f;
    s.torchX = 0.0f; s.torchZ = 1.0f; s.torchOn = true;   // looking down +Z

    Entity e = makeEntity(aheadAt.x, aheadAt.z);
    expect(inTorchBeam(f, e, s), "a creature straight ahead should be in the beam");

    // Off to the side is not — same distance, wrong direction.
    Entity side = makeEntity(playerAt.x + dist2d(aheadAt, playerAt), playerAt.z);
    expect(!inTorchBeam(f, side, s), "a creature at 90 degrees must be outside the cone");

    // Beyond the range is not.
    Entity far_ = makeEntity(playerAt.x, playerAt.z + kTorchRange + 3.0f);
    expect(!inTorchBeam(f, far_, s), "the beam must not reach past its range");

    // With the torch off, nothing is.
    WorldSense off = s; off.torchOn = false;
    expect(!inTorchBeam(f, e, off), "a creature is never in the beam with the torch off");

    // The beam has to actually slow it. Two identical creatures, one lit and
    // one not, over the same interval: the lit one must cover less ground.
    Entity lit  = makeEntity(aheadAt.x, aheadAt.z);
    Entity dark = makeEntity(aheadAt.x, aheadAt.z);
    lit.state = dark.state = AIState::Chase;
    lit.torchExposure = kRetreatExposure * 0.9f;   // deep in the beam, not yet gone
    WorldSense noTorch = s; noTorch.torchOn = false;
    std::mt19937 ra(11), rb(11);
    const float startLit = dist2d(lit.pos, s.playerPos);
    const float startDark = dist2d(dark.pos, s.playerPos);
    for (int i = 0; i < 30; ++i) {
        BehaviorTree::tick(f, lit, s, 0.016f, ra);
        BehaviorTree::tick(f, dark, noTorch, 0.016f, rb);
    }
    const float closedLit  = startLit  - dist2d(lit.pos, s.playerPos);
    const float closedDark = startDark - dist2d(dark.pos, s.playerPos);
    expect(closedLit < closedDark * 0.75f,
           "a creature held in the beam must close on the player noticeably slower");
    std::printf("   closed %.2fm lit vs %.2fm unlit over half a second\n", closedLit, closedDark);

    // Held in the beam: exposure accumulates and it eventually breaks off.
    Entity held = makeEntity(aheadAt.x, aheadAt.z);
    float t = 0.0f;
    while (t < 10.0f && held.state != AIState::Retreat) {
        BehaviorTree::tick(f, held, s, 0.016f, rng);
        t += 0.016f;
    }
    expect(held.state == AIState::Retreat, "holding the beam on it must drive it off");
    expect(t >= kRetreatExposure * 0.9f && t <= kRetreatExposure * 1.4f,
           "it should break off at about kRetreatExposure seconds, not instantly or never");
    std::printf("   broke off after %.2fs (kRetreatExposure %.2f)\n", t, kRetreatExposure);

    // Sweeping past does nothing: exposure has to bleed off.
    Entity swept = makeEntity(aheadAt.x, aheadAt.z);
    for (int i = 0; i < 30; ++i) BehaviorTree::tick(f, swept, s, 0.016f, rng);   // ~0.5s lit
    WorldSense away = s; away.torchX = 1.0f; away.torchZ = 0.0f;
    for (int i = 0; i < 200; ++i) BehaviorTree::tick(f, swept, away, 0.016f, rng);
    expect(swept.torchExposure < 0.01f, "exposure must bleed off once the beam leaves");
    expect(swept.state != AIState::Retreat, "a sweep of the beam must not drive it off");
}

// --- 5. It never dies, and it always comes back -----------------------------
static void testRetreatAndReturn(const map::Level0Field& f) {
    std::mt19937 rng(6);
    int cx = 0, cz = 0;
    if (!findCorridor(f, 6, cx, cz)) { std::printf("FAIL no corridor found\n"); ++fails; return; }
    const Vec3f playerAt = cellCentre(cx, cz);
    const Vec3f aheadAt  = cellCentre(cx, cz + 2);

    Entity e = makeEntity(aheadAt.x, aheadAt.z);
    WorldSense s{};
    s.playerPos = playerAt; s.noise = 0.5f;
    s.torchX = 0; s.torchZ = 1.0f; s.torchOn = true;

    // Drive it off.
    for (int i = 0; i < 60 * 20 && e.state != AIState::Retreat; ++i)
        BehaviorTree::tick(f, e, s, 0.016f, rng);
    expect(e.state == AIState::Retreat, "setup: it should have been driven off");

    // Let it run. Torch off now — the player has moved on.
    WorldSense quiet = s; quiet.torchOn = false; quiet.noise = 0.0f;
    for (int i = 0; i < 60 * 40; ++i) BehaviorTree::tick(f, e, quiet, 0.016f, rng);

    expect(e.active, "a creature driven off must stay in the simulation, not die");
    expect(e.dissolve >= 0.999f, "once it is far enough away it should be fully faded");
    expect(dist2d(e.pos, e.retreatFrom) >= kRetreatDistance,
           "it should have put real distance between itself and where it was caught");
    std::printf("   ran %.1fm from where it was caught, dissolve %.2f, active %d\n",
                dist2d(e.pos, e.retreatFrom), e.dissolve, int(e.active));

    // It must not come back on its own while the player stays silent and unseen.
    WorldSense gone{};
    gone.playerPos = {e.pos.x + 200.0f, 0, e.pos.z + 200.0f};
    gone.noise = 0.0f;
    for (int i = 0; i < 60 * 30; ++i) BehaviorTree::tick(f, e, gone, 0.016f, rng);
    expect(e.dissolve >= 0.999f, "it should stay away while it has no reason to return");

    // Now the player comes back and is loud right next to it. It has to return —
    // this is the whole point, and the assertion that catches a "retreat" that
    // is really a permanent removal. Chasing it must not be able to keep it away.
    WorldSense loud{};
    loud.playerPos = {e.pos.x, 0, e.pos.z + 4.0f};
    loud.noise = 1.0f;
    int ticks = 0;
    while (ticks < 60 * 30 && e.dissolve > 0.001f) {
        BehaviorTree::tick(f, e, loud, 0.016f, rng);
        ++ticks;
    }
    expect(e.dissolve <= 0.001f, "a loud player standing next to it must bring it back");
    expect(e.state != AIState::Retreat, "once back it must leave the Retreat state");
    std::printf("   came back after %.1fs of a loud player nearby\n", ticks * 0.016f);

    // And it can be driven off again — the cycle must not latch.
    WorldSense beam{};
    beam.playerPos = {e.pos.x, 0, e.pos.z - 5.0f};
    beam.noise = 0.5f; beam.torchX = 0; beam.torchZ = 1.0f; beam.torchOn = true;
    int again = 0;
    while (again < 60 * 20 && e.state != AIState::Retreat) {
        BehaviorTree::tick(f, e, beam, 0.016f, rng);
        ++again;
    }
    expect(e.state == AIState::Retreat, "the retreat/return cycle must repeat, not latch");
}

// --- 6. Nothing runs away to infinity or produces a NaN ---------------------
static void testStability(const map::Level0Field& f) {
    std::mt19937 rng(7);
    for (int type = 0; type < kEntityTypeCount; ++type) {
        Entity e = makeEntity(0, 6.0f);
        e.type = static_cast<EntityType>(type);
        WorldSense s{}; s.playerPos = {0, 0, 0}; s.noise = 0.5f;
        for (int i = 0; i < 60 * 60; ++i) {
            // A player who wanders, so no creature sits in one contrived state.
            s.playerPos.x = std::sin(i * 0.01f) * 6.0f;
            s.playerPos.z = std::cos(i * 0.013f) * 6.0f;
            s.torchOn = (i / 300) % 2 == 0;
            s.torchX = std::sin(i * 0.02f); s.torchZ = std::cos(i * 0.02f);
            BehaviorTree::tick(f, e, s, 0.016f, rng);
        }
        expect(std::isfinite(e.pos.x) && std::isfinite(e.pos.z),
               "an entity position went non-finite over a minute of simulation");
        expect(e.active, "no creature may deactivate itself; nothing here dies");
        expect(e.dissolve >= 0.0f && e.dissolve <= 1.0f, "dissolve left 0..1");
        expect(e.torchExposure >= 0.0f, "torch exposure went negative");
    }
}

int main() {
    map::Level0Field field(20260806ULL);
    testLineOfSight(field);
    testNoiseBudget(field);
    testInvestigate(field);
    testTorch(field);
    testRetreatAndReturn(field);
    testStability(field);
    std::printf("%s\n", fails == 0 ? "PROBE-OK" : "PROBE-FAILED");
    return fails == 0 ? 0 : 1;
}
"""


def check_behaviour() -> None:
    section("Behaviour (compiled against the real AI)")
    with tempfile.TemporaryDirectory() as tmp:
        src = os.path.join(tmp, "probe.cpp")
        with open(src, "w", encoding="utf-8") as f:
            f.write(PROBE)
        exe = os.path.join(tmp, "probe")
        r = subprocess.run(
            ["g++", "-std=c++20", "-O2", "-Wall", "-Wextra", "-I", NATIVE,
             src,
             os.path.join(NATIVE, "Entity/Entity.cpp"),
             os.path.join(NATIVE, "Map/Level_0.cpp"),
             "-o", exe],
            capture_output=True, text=True)
        if r.returncode != 0:
            failures.append(f"the AI probe does not build:\n{r.stderr[:2000]}")
            return

        run = subprocess.run([exe], capture_output=True, text=True, timeout=180)
        for line in run.stdout.splitlines():
            if line.startswith("FAIL "):
                failures.append(f"behaviour: {line[5:]}")
            elif line.startswith("   "):
                print(line)
        if run.returncode != 0 and "PROBE-FAILED" not in run.stdout:
            failures.append(f"the AI probe crashed: {run.stderr[:800]}")
        elif "PROBE-OK" in run.stdout:
            print("   every behavioural assertion held")


def main() -> int:
    check_spawn_config()
    check_jni_surface()
    check_behaviour()

    print()
    for f in failures:
        print("FAIL", f)
    print("PASSED" if not failures else
          f"FAILED ({len(failures)} failure{'' if len(failures) == 1 else 's'})")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
