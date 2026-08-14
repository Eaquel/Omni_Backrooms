


#ifndef OMNI_ENTITY_ENTITY_H
#define OMNI_ENTITY_ENTITY_H

#include <cstdint>
#include <random>
#include <vector>

#include "Map/Level_0.h"

namespace omni {
namespace entity {

struct Vec3f { float x = 0, y = 0, z = 0; };


enum class EntityType : uint8_t { Smiler = 0 };
constexpr int kEntityTypeCount = 1;


enum class AIState : uint8_t {
    Idle = 0, Wander = 1, Alert = 2, Chase = 3, Attack = 4,
    Flee = 5, Stalk = 6, Ambush = 7, Retreat = 8, Investigate = 9
};


constexpr float kTorchRange   = 9.0f;
constexpr float kTorchCosHalf = 0.906f;


constexpr float kRetreatExposure = 2.2f;


constexpr float kExposureDecay = 0.8f;


constexpr float kContactExposure = 1.05f;


constexpr float kContactRange = 2.4f;


constexpr float kWanderDrift = 0.16f;

constexpr float kLeashDistance = 45.0f;
constexpr float kLeashPatience = 18.0f;
constexpr float kReentryRadius = 38.0f;


constexpr float kDissolveRate = 1.6f;
constexpr float kReformRate   = 0.9f;


constexpr float kRetreatSpeed    = 1.9f;
constexpr float kRetreatDistance = 26.0f;


constexpr float kInvestigateGrace = 6.0f;

constexpr float kArriveRadius = 1.6f;


constexpr float kMinTorchSpeedMult = 0.25f;


struct WorldSense {
    Vec3f playerPos;

    float noise = 0.5f;

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

    float leashTimer = 0;
    float hp = 100, maxHp = 100;
    float stalkTimer = 0, ambushTimer = 0, flickerInfluence = 0;


    float torchExposure = 0.0f;

    float dissolve = 0.0f;



    Vec3f retreatFrom;

    EntityType type = EntityType::Smiler;
    AIState    state = AIState::Wander;
    Blackboard bb;
    int  id = 0;
    bool active = false;
};

[[nodiscard]] float dist2d(const Vec3f& a, const Vec3f& b) noexcept;
[[nodiscard]] float dist3d(const Vec3f& a, const Vec3f& b) noexcept;


[[nodiscard]] bool hasLineOfSight(const map::Level0Field& field,
                                  const Vec3f& from, const Vec3f& to,
                                  float maxDist) noexcept;


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




    void leash(Entity& e, float dt) noexcept {
        const float dx = e.pos.x - sense.playerPos.x;
        const float dz = e.pos.z - sense.playerPos.z;
        const float d  = std::hypot(dx, dz);


        if (d < kContactRange) e.torchExposure += kContactExposure * dt;

        if (d < kLeashDistance) { e.leashTimer = 0.0f; return; }
        e.leashTimer += dt;
        if (e.leashTimer < kLeashPatience) return;
        e.leashTimer = 0.0f;



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

}
}

#endif
