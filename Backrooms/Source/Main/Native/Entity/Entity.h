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
    static void doWander(Entity& e, float dt, std::mt19937& rng) noexcept;
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
        }
    }
};

} // namespace entity
} // namespace omni

#endif // OMNI_ENTITY_ENTITY_H
