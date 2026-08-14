#include "Entity/Entity.h"

#include <algorithm>
#include <cmath>

namespace omni {
namespace entity {

float dist2d(const Vec3f& a, const Vec3f& b) noexcept {
    return std::hypot(a.x - b.x, a.z - b.z);
}

float dist3d(const Vec3f& a, const Vec3f& b) noexcept {
    const float dx = a.x - b.x, dy = a.y - b.y, dz = a.z - b.z;
    return std::sqrt(dx * dx + dy * dy + dz * dz);
}

bool hasLineOfSight(const map::Level0Field& field,
                    const Vec3f& from, const Vec3f& to, float maxDist) noexcept {
    const float dx = to.x - from.x, dz = to.z - from.z;
    const float d = std::hypot(dx, dz);
    if (d > maxDist) return false;
    if (d < 0.001f)  return true;





    const float step = map::Level0Field::kCell * 0.25f;
    const int   steps = static_cast<int>(d / step);
    const float ux = dx / d, uz = dz / d;

    for (int i = 1; i < steps; ++i) {
        const float t = static_cast<float>(i) * step;
        const int cx = map::Level0Field::cellX(from.x + ux * t);
        const int cz = map::Level0Field::cellZ(from.z + uz * t);
        if (field.isSolid(cx, cz)) return false;
    }
    return true;
}

bool inTorchBeam(const map::Level0Field& field,
                 const Entity& e, const WorldSense& sense) noexcept {
    if (!sense.torchOn) return false;
    const float dx = e.pos.x - sense.playerPos.x;
    const float dz = e.pos.z - sense.playerPos.z;
    const float d = std::hypot(dx, dz);
    if (d > kTorchRange || d < 0.001f) return false;


    const float cosA = (dx / d) * sense.torchX + (dz / d) * sense.torchZ;
    if (cosA < kTorchCosHalf) return false;
    return hasLineOfSight(field, sense.playerPos, e.pos, kTorchRange);
}


void BehaviorTree::updateBlackboard(const map::Level0Field& field, Entity& e,
                                    const WorldSense& sense, float dt) noexcept {
    const float d = dist3d(e.pos, sense.playerPos);

    e.bb.timeSincePlayerSeen += dt;




    e.bb.playerInSight = (d < e.sightRadius) &&
                         hasLineOfSight(field, e.pos, sense.playerPos, e.sightRadius);





    const float audible = e.hearRadius * std::clamp(sense.noise, 0.0f, 1.0f);
    e.bb.heardNoise  = (d < audible);
    e.bb.noiseLevel  = audible > 0.001f ? std::clamp(1.0f - d / audible, 0.0f, 1.0f) : 0.0f;



    const float rise = e.bb.playerInSight ? 2.0f : (e.bb.heardNoise ? 0.7f * e.bb.noiseLevel : 0.0f);
    const float fall = (rise > 0.0f) ? 0.0f : 0.5f;
    e.bb.alertLevel = std::clamp(e.bb.alertLevel + (rise - fall) * dt, 0.0f, 1.0f);

    if (e.bb.playerInSight) {
        e.bb.lastKnownPlayerPos = sense.playerPos;
        e.bb.timeSincePlayerSeen = 0.0f;
    } else if (e.bb.heardNoise) {


        e.bb.lastKnownPlayerPos = sense.playerPos;
    }
}

void BehaviorTree::updateTorch(const map::Level0Field& field, Entity& e,
                               const WorldSense& sense, float dt) noexcept {
    if (inTorchBeam(field, e, sense)) {
        e.torchExposure += dt;
    } else {
        e.torchExposure = std::max(0.0f, e.torchExposure - kExposureDecay * dt);
    }
}

bool BehaviorTree::runRetreat(Entity& e, float dt) noexcept {









    const float gone = dist2d(e.pos, e.retreatFrom);

    if (gone < kRetreatDistance) {



        fleeFrom(e, e.retreatFrom, dt, kRetreatSpeed);
        e.dissolve = std::min(1.0f, e.dissolve + kDissolveRate * dt);
        return true;
    }


    e.torchExposure = 0.0f;
    e.flickerInfluence = std::max(0.0f, e.flickerInfluence - dt);




    const bool called = e.bb.playerInSight || e.bb.heardNoise;

    if (!called) {




        e.dissolve = std::min(1.0f, e.dissolve + kDissolveRate * dt);
        e.bb.alertLevel = 0.0f;
        return true;
    }






    e.dissolve = std::max(0.0f, e.dissolve - kReformRate * dt);
    if (e.dissolve <= 0.001f) {
        e.dissolve = 0.0f;
        e.state = AIState::Alert;
        e.bb.alertLevel = 0.6f;
        return false;
    }
    return true;
}

void BehaviorTree::tick(const map::Level0Field& field, Entity& e,
                        const WorldSense& sense, float dt, std::mt19937& rng) noexcept {
    updateBlackboard(field, e, sense, dt);
    updateTorch(field, e, sense, dt);

    if (e.state == AIState::Retreat) {
        if (runRetreat(e, dt)) return;
    } else if (e.torchExposure >= kRetreatExposure) {
        e.state = AIState::Retreat;
        e.retreatFrom = e.pos;
        return;
    } else if (e.dissolve > 0.0f) {


        e.dissolve = std::max(0.0f, e.dissolve - kReformRate * dt);
    }

    AIState next = tickSmiler(e, dt, rng);




    if (!e.bb.playerInSight &&
        e.bb.timeSincePlayerSeen < kInvestigateGrace &&
        (next == AIState::Wander || next == AIState::Idle)) {
        next = (dist2d(e.pos, e.bb.lastKnownPlayerPos) > kArriveRadius)
                   ? AIState::Investigate
                   : AIState::Alert;
    }

    e.state = next;
    executeState(e, sense, dt, rng);
}


AIState BehaviorTree::tickSmiler(Entity& e, float dt, std::mt19937&) noexcept {
    if (e.bb.playerInSight) {
        e.flickerInfluence = std::min(1.0f, e.flickerInfluence + dt * 0.8f);
        return dist3d(e.pos, e.bb.lastKnownPlayerPos) < e.attackRadius
                   ? AIState::Attack : AIState::Stalk;
    }
    e.flickerInfluence = std::max(0.0f, e.flickerInfluence - dt * 0.3f);












    return e.bb.alertLevel > 0.3f ? AIState::Alert : AIState::Wander;
}


void BehaviorTree::executeState(Entity& e, const WorldSense& sense,
                                float dt, std::mt19937& rng) noexcept {



    const float lit = std::clamp(e.torchExposure / kRetreatExposure, 0.0f, 1.0f);
    const float torchDrag = 1.0f - (1.0f - kMinTorchSpeedMult) * lit;

    switch (e.state) {
        case AIState::Wander:
            doWander(e, sense.playerPos, dt, rng);
            break;
        case AIState::Chase:
            moveToward(e, sense.playerPos, dt, torchDrag);
            break;
        case AIState::Stalk:
            moveToward(e, sense.playerPos, dt, 0.55f * torchDrag);
            break;
        case AIState::Investigate:


            moveToward(e, e.bb.lastKnownPlayerPos, dt, 0.75f * torchDrag);
            break;
        case AIState::Attack:
            doAttack(e, sense.playerPos, dt);
            break;
        case AIState::Flee:
            fleeFrom(e, sense.playerPos, dt, 1.2f);
            break;
        default:
            break;
    }
}

void BehaviorTree::doWander(Entity& e, const Vec3f& player,
                            float dt, std::mt19937& rng) noexcept {
    e.wanderTimer -= dt;
    if (e.wanderTimer <= 0) {
        std::uniform_real_distribution<float> ad(-1.0f, 1.0f);
        e.wanderAngle += ad(rng) * 1.2f;
        e.wanderTimer = 1.0f + std::uniform_real_distribution<float>(0, 2)(rng);












        const float bearing = std::atan2(player.x - e.pos.x, player.z - e.pos.z);
        float delta = bearing - e.wanderAngle;
        while (delta >  3.14159265f) delta -= 6.28318531f;
        while (delta < -3.14159265f) delta += 6.28318531f;
        e.wanderAngle += delta * kWanderDrift;
    }
    e.vel.x = std::sin(e.wanderAngle) * e.speed * 0.4f;
    e.vel.z = std::cos(e.wanderAngle) * e.speed * 0.4f;
    e.pos.x += e.vel.x * dt;
    e.pos.z += e.vel.z * dt;
}

void BehaviorTree::moveToward(Entity& e, const Vec3f& t, float dt, float speedMult) noexcept {
    const float dx = t.x - e.pos.x, dz = t.z - e.pos.z;
    const float d = std::hypot(dx, dz);
    if (d < 0.1f) return;
    e.pos.x += dx / d * e.speed * speedMult * dt;
    e.pos.z += dz / d * e.speed * speedMult * dt;
}

void BehaviorTree::doAttack(Entity& e, const Vec3f& t, float dt) noexcept {
    if (e.attackCooldown > 0) { e.attackCooldown -= dt; return; }
    moveToward(e, t, dt, 1.0f);
    e.attackCooldown = 0.8f;
}

void BehaviorTree::fleeFrom(Entity& e, const Vec3f& t, float dt, float speedMult) noexcept {
    float dx = e.pos.x - t.x, dz = e.pos.z - t.z;
    float d = std::hypot(dx, dz);
    if (d < 0.1f) {


        dx = 1.0f; dz = 0.0f; d = 1.0f;
    }
    e.pos.x += dx / d * e.speed * speedMult * dt;
    e.pos.z += dz / d * e.speed * speedMult * dt;
}

}
}
