// ============================================================================
// The things in the corridors.
//
// This used to live in the middle of Engine.cpp, between the audio mixer and
// the socket code, which meant it could not be compiled — let alone exercised —
// without jni.h, aaudio and an Android device. A creature's behaviour is the
// part of this game that is hardest to judge by reading it and easiest to get
// subtly wrong, so it is the last thing that should have been unreachable from
// a test. It now builds on a host with nothing but Map/Level_0.cpp beside it.
//
// The behaviour itself changed in four ways that matter:
//
//   * Sight is a ray, not a radius. `d < sightRadius` meant a creature two
//     rooms away, with four walls between you, tracked you perfectly.
//   * Hearing is a budget the player spends. Crouching is genuinely quiet,
//     sprinting genuinely is not, and the difference is what makes a corridor
//     a decision rather than a corridor.
//   * Losing sight of you does not reset it. It goes to where you were, looks,
//     and gives up gradually.
//   * The flashlight drives it off — and only off. Nothing here dies. It
//     retreats, dissolves, waits at distance, and comes back when it sees you
//     again or hears you get careless.
//
// ============================================================================

#ifndef OMNI_ENTITY_ENTITY_H
#define OMNI_ENTITY_ENTITY_H

#include <cstdint>
#include <random>
#include <vector>

#include "Map/Level_0.h"

namespace omni {
namespace entity {

struct Vec3f { float x = 0, y = 0, z = 0; };

/**
 * Level 0 holds one thing.
 *
 * There were eight, and seven of them could never be dispatched once the
 * spawner stopped choosing between them — seven behaviour trees reachable only
 * by a cast. Kept as an enum with one entry because the wire format and the
 * JNI surface both carry a type byte, and a bare 0 there says nothing.
 */
enum class EntityType : uint8_t { Smiler = 0 };
constexpr int kEntityTypeCount = 1;

// Retreat and Investigate are new. Everything below them keeps its old number
// because Kotlin reads state 4 to decide whether the player is being hit, and
// a renumbering there is a silent gameplay change.
enum class AIState : uint8_t {
    Idle = 0, Wander = 1, Alert = 2, Chase = 3, Attack = 4,
    Flee = 5, Stalk = 6, Ambush = 7, Retreat = 8, Investigate = 9
};

// --- Tuning -----------------------------------------------------------------
// In the header so the checker can assert on the numbers rather than on a
// transcription of them.

/** How far the beam reaches, in metres, and the cosine of its half-angle. */
constexpr float kTorchRange   = 9.0f;
constexpr float kTorchCosHalf = 0.906f;          // ≈25°

/** Seconds held in the beam before it breaks off. Short enough that the torch
 *  feels like a weapon, long enough that you have to stand your ground. */
constexpr float kRetreatExposure = 2.2f;
/** Exposure bleeds off this fast once the beam leaves it, so sweeping past
 *  does nothing and only deliberate aim accumulates. */
constexpr float kExposureDecay = 0.8f;

/**
 * What being right on top of the player costs it, per second.
 *
 * Nothing but the beam and taking damage ever broke it off, so with the torch
 * down an encounter had no end: simulated over eight seeds and five minutes,
 * half of them had the creature at a MEDIAN distance of 1.4 metres for the
 * entire run, in sight 100% of the time. That is not a stalker, it is a
 * passenger.
 *
 * Charged on CONTACT rather than on a landed strike, and the difference
 * matters: a creature sitting exactly at its attack radius oscillates across
 * the boundary and spends most of its time in Stalk, so a cost inside
 * doAttack() never fires. Measured -- adding it there changed the numbers by
 * nothing at all. Distance is the honest trigger.
 *
 * 1.05/s against the 0.8/s that exposure bleeds off, so a quarter of a metre
 * of net gain per second: about nine seconds of contact before it withdraws.
 */
constexpr float kContactExposure = 1.05f;
/** Within this of the player it counts as contact. A little over the attack
 *  radius, so hovering just outside striking distance still wears it out. */
constexpr float kContactRange = 2.4f;

/**
 * How far it may drift before the level puts it back, and how long it has to
 * stay out there first.
 *
 * The other half of the same measurement: on the seeds where it never acquired
 * the player it wandered to a median 34-51 m and stayed there, never seeing
 * them once in five minutes, with a re-spawn interval of one hour behind it.
 * "The creature is almost absent" was both of those at once -- glued on or
 * gone, with nothing in between.
 *
 * Coming back is not teleporting into view: it re-enters on a ring well past
 * the torch's reach, out of sight, and has to find the player again.
 *
 * 45 m, not the 85 first tried: on the seeds where it never acquired the player
 * its MEDIAN distance was 34-51 m, so a leash at 85 never fired once and the
 * measurement came back byte-identical. The number has to sit inside the range
 * the behaviour actually produces.
 */
/**
 * How strongly wandering drifts toward the player, 0 none and 1 a beeline.
 *
 * This is the rest of "the creature is almost absent". On the seeds where it
 * never acquired the player it was not far away — a median of 33-51 m, well
 * inside the same floor — it simply wandered at random, and random search in an
 * infinite maze does not find anything. Measured over five simulated minutes it
 * saw the player 0% of the time on three seeds of eight.
 *
 * A drift, not a hunt. At 0.16 it takes a minute or two to close from across
 * the floor and it does not walk a straight line to you, so it still arrives
 * from a direction you did not expect. What it removes is the possibility of it
 * never arriving at all.
 */
constexpr float kWanderDrift = 0.16f;

constexpr float kLeashDistance = 45.0f;
constexpr float kLeashPatience = 18.0f;
constexpr float kReentryRadius = 38.0f;

/** How quickly it fades out while retreating and back in while returning.
 *  Returning is slower on purpose: it should be a dread you notice building. */
constexpr float kDissolveRate = 1.6f;
constexpr float kReformRate   = 0.9f;

/** Retreat speed multiplier, and how far it gets before it stops running. */
constexpr float kRetreatSpeed    = 1.9f;
constexpr float kRetreatDistance = 26.0f;

/** Seconds it will keep hunting the place it last saw you. */
constexpr float kInvestigateGrace = 6.0f;
/** Close enough to the last known position to count as having looked there. */
constexpr float kArriveRadius = 1.6f;

/** Slowest the beam can make it, as a fraction of its own speed. Not zero:
 *  something that stops dead is a prop, and you stop being afraid of props. */
constexpr float kMinTorchSpeedMult = 0.25f;

// --- Perception -------------------------------------------------------------

/** What the player is doing this tick, from the creature's point of view. */
struct WorldSense {
    Vec3f playerPos;
    /** 0 silent, ~0.15 crouched, ~0.5 walking, 1 sprinting. Scales hearing. */
    float noise = 0.5f;
    /** Where the torch points, normalised on XZ. Ignored when off. */
    float torchX = 0.0f, torchZ = 1.0f;
    bool  torchOn = false;
};

struct Blackboard {
    Vec3f lastKnownPlayerPos;
    float timeSincePlayerSeen = 999.0f;
    float alertLevel = 0.0f;
    bool  playerInSight = false;
    bool  heardNoise = false;
    float noiseLevel = 0.0f;
    int   patrolIndex = 0;
};

struct Entity {
    Vec3f pos, vel;
    float speed = 0, hearRadius = 0, sightRadius = 0, attackRadius = 0, aggroRadius = 0;
    float wanderAngle = 0, wanderTimer = 0, attackCooldown = 0;
    /** Seconds spent beyond kLeashDistance from the player. */
    float leashTimer = 0;
    float hp = 100, maxHp = 100;
    float stalkTimer = 0, ambushTimer = 0, flickerInfluence = 0;

    /** Seconds of accumulated time in the beam. */
    float torchExposure = 0.0f;
    /** 0 solid, 1 gone. Drives the billboard; also the flag for "parked". */
    float dissolve = 0.0f;
    /** Where it was standing when the beam broke it. How far it has come is
     *  measured from here and not from the player, so a player who chases it
     *  cannot push it away indefinitely — and, more importantly, a player who
     *  walks up to where it went cannot stop it from ever coming back. */
    Vec3f retreatFrom;

    EntityType type = EntityType::Smiler;
    AIState    state = AIState::Wander;
    Blackboard bb;
    int  id = 0;
    bool active = false;
};

[[nodiscard]] float dist2d(const Vec3f& a, const Vec3f& b) noexcept;
[[nodiscard]] float dist3d(const Vec3f& a, const Vec3f& b) noexcept;

/**
 * True when nothing solid stands between the two points.
 *
 * Marched in steps well under a cell so a corner cannot be cut diagonally.
 * Endpoints are not tested: an entity resolved into a wall for one frame should
 * not be permanently blind, and the player standing in a doorway should not
 * flicker in and out of view.
 */
[[nodiscard]] bool hasLineOfSight(const map::Level0Field& field,
                                  const Vec3f& from, const Vec3f& to,
                                  float maxDist) noexcept;

/** The beam test: on, in range, inside the cone, and not through a wall. */
[[nodiscard]] bool inTorchBeam(const map::Level0Field& field,
                               const Entity& e, const WorldSense& sense) noexcept;

class BehaviorTree {
public:
    static void tick(const map::Level0Field& field, Entity& e,
                     const WorldSense& sense, float dt, std::mt19937& rng) noexcept;

private:
    static void updateBlackboard(const map::Level0Field& field, Entity& e,
                                 const WorldSense& sense, float dt) noexcept;
    static void updateTorch(const map::Level0Field& field, Entity& e,
                            const WorldSense& sense, float dt) noexcept;
    static bool runRetreat(Entity& e, float dt) noexcept;

    static AIState tickSmiler(Entity& e, float dt, std::mt19937&) noexcept;

    static void executeState(Entity& e, const WorldSense& sense,
                             float dt, std::mt19937& rng) noexcept;
    static void doWander(Entity& e, const Vec3f& player,
                         float dt, std::mt19937& rng) noexcept;
    static void moveToward(Entity& e, const Vec3f& t, float dt, float speedMult) noexcept;
    static void doAttack(Entity& e, const Vec3f& t, float dt) noexcept;
    static void fleeFrom(Entity& e, const Vec3f& t, float dt, float speedMult) noexcept;
};

struct EntitySystem {
    std::vector<Entity> entities;
    WorldSense          sense;
    map::Level0Field    field{0};
    std::mt19937        rng{12345};

    void tick(float dt) noexcept {
        for (auto& e : entities) {
            if (!e.active) continue;
            BehaviorTree::tick(field, e, sense, dt, rng);
            leash(e, dt);
        }
    }

    /**
     * Brings a creature that has wandered out of the run back into it.
     *
     * The world is infinite and the player walks; without this the one Smiler
     * a run spawns can simply be left behind, and the periodic re-spawn that
     * was supposed to cover that is on a one-hour interval, which is longer
     * than any run. Re-entry is deliberately out of sight and out of the
     * torch's reach: it comes back to the level, not into the room.
     */
    void leash(Entity& e, float dt) noexcept {
        const float dx = e.pos.x - sense.playerPos.x;
        const float dz = e.pos.z - sense.playerPos.z;
        const float d  = std::hypot(dx, dz);

        // Standing on the player wears it out whatever state it thinks it is in.
        if (d < kContactRange) e.torchExposure += kContactExposure * dt;

        if (d < kLeashDistance) { e.leashTimer = 0.0f; return; }
        e.leashTimer += dt;
        if (e.leashTimer < kLeashPatience) return;
        e.leashTimer = 0.0f;

        // A ring around the player, snapped onto real floor. Bearing comes from
        // the rng so two re-entries do not arrive from the same side.
        const float a = std::uniform_real_distribution<float>(0.0f, 6.2831853f)(rng);
        float rx = sense.playerPos.x + std::cos(a) * kReentryRadius;
        float rz = sense.playerPos.z + std::sin(a) * kReentryRadius;
        for (int r = 0; r < 24; ++r) {
            for (int ddz = -r; ddz <= r; ++ddz) {
                for (int ddx = -r; ddx <= r; ++ddx) {
                    if (std::max(std::abs(ddx), std::abs(ddz)) != r) continue;
                    const int cx = map::Level0Field::cellX(rx) + ddx;
                    const int cz = map::Level0Field::cellZ(rz) + ddz;
                    if (!field.isOpen(cx, cz)) continue;
                    e.pos.x = map::Level0Field::worldX(cx) + map::Level0Field::kCell * 0.5f;
                    e.pos.z = map::Level0Field::worldZ(cz) + map::Level0Field::kCell * 0.5f;
                    e.state = AIState::Wander;
                    e.bb.playerInSight = false;
                    e.torchExposure = 0.0f;
                    return;
                }
            }
        }
    }
};

} // namespace entity
} // namespace omni

#endif // OMNI_ENTITY_ENTITY_H
