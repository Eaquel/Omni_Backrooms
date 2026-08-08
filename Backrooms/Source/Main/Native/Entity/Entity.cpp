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

    // A quarter of a cell. Anything coarser can hop the corner of a pillar,
    // which shows up in play as a creature that sees round exactly one type of
    // obstacle and nothing else — the worst kind of bug to diagnose from a
    // video.
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
    // The beam is a cone about the look direction; the dot product of the two
    // unit vectors is the cosine of the angle between them.
    const float cosA = (dx / d) * sense.torchX + (dz / d) * sense.torchZ;
    if (cosA < kTorchCosHalf) return false;
    return hasLineOfSight(field, sense.playerPos, e.pos, kTorchRange);
}

// ---------------------------------------------------------------------------

void BehaviorTree::updateBlackboard(const map::Level0Field& field, Entity& e,
                                    const WorldSense& sense, float dt) noexcept {
    const float d = dist3d(e.pos, sense.playerPos);

    e.bb.timeSincePlayerSeen += dt;

    // Sight: in range AND nothing solid in between. The wall test is the whole
    // point — without it, the "sight radius" was a sphere that ignored the
    // level, and there was no way to break contact except by outrunning it.
    e.bb.playerInSight = (d < e.sightRadius) &&
                         hasLineOfSight(field, e.pos, sense.playerPos, e.sightRadius);

    // Hearing: the player's own noise sets how far they carry. Crouching does
    // not make them quieter by a fudge factor — it shrinks the radius that
    // exists at all, so a creature that would have heard them at 12 m hears
    // nothing at 2 m.
    const float audible = e.hearRadius * std::clamp(sense.noise, 0.0f, 1.0f);
    e.bb.heardNoise  = (d < audible);
    e.bb.noiseLevel  = audible > 0.001f ? std::clamp(1.0f - d / audible, 0.0f, 1.0f) : 0.0f;

    // Alertness rises fast on sight, more slowly on sound alone, and always
    // bleeds away — which is what lets a player wait somewhere and be forgotten.
    const float rise = e.bb.playerInSight ? 2.0f : (e.bb.heardNoise ? 0.7f * e.bb.noiseLevel : 0.0f);
    const float fall = (rise > 0.0f) ? 0.0f : 0.5f;
    e.bb.alertLevel = std::clamp(e.bb.alertLevel + (rise - fall) * dt, 0.0f, 1.0f);

    if (e.bb.playerInSight) {
        e.bb.lastKnownPlayerPos = sense.playerPos;
        e.bb.timeSincePlayerSeen = 0.0f;
    } else if (e.bb.heardNoise) {
        // Sound gives a position too, just a worse one — it is where the noise
        // came from, and it is why sprinting past a blind corner is a mistake.
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
    // Distance from where the beam caught it, NOT from the player.
    //
    // Measuring against the live player position was wrong in a way that only
    // showed up in a long simulation: a player who walked after it kept it
    // inside the "still running" branch forever, so it never parked, and — far
    // worse — a player standing next to where it went could never call it back,
    // because the return check sat below a branch that always fled first. It
    // could be driven away permanently by following it. That is precisely the
    // "gone for good" behaviour this whole state exists to avoid.
    const float gone = dist2d(e.pos, e.retreatFrom);

    if (gone < kRetreatDistance) {
        // Still running. Away from where it was seen, so a player who circles
        // round cannot herd it. Fades as it goes, so the vanishing is something
        // the player watches happen rather than a pop.
        fleeFrom(e, e.retreatFrom, dt, kRetreatSpeed);
        e.dissolve = std::min(1.0f, e.dissolve + kDissolveRate * dt);
        return true;
    }

    // Far enough. It is gone, not dead: it parks, forgets, and waits.
    e.torchExposure = 0.0f;
    e.flickerInfluence = std::max(0.0f, e.flickerInfluence - dt);

    // What brings it back. Sight is the obvious one. Noise is the interesting
    // one — it is how a player who never sees it can still call it, and it is
    // why running everywhere is punished.
    const bool called = e.bb.playerInSight || e.bb.heardNoise;

    if (!called) {
        // Stays gone, and forgets. Fading back OUT rather than snapping to 1
        // matters when the player walks past just far enough to half-call it:
        // it half-appears and sinks away again, which is the behaviour that
        // makes it feel like it is deciding.
        e.dissolve = std::min(1.0f, e.dissolve + kDissolveRate * dt);
        e.bb.alertLevel = 0.0f;
        return true;
    }

    // Reforming. This used to sit under an unconditional `dissolve = 1.0f` a
    // few lines up, so every tick undid the previous tick's progress and it
    // could never finish — a creature driven off once was gone for the rest of
    // the run, silently, with no error anywhere. Nothing on a device would have
    // shown that; a thirty-second simulation shows it immediately.
    e.dissolve = std::max(0.0f, e.dissolve - kReformRate * dt);
    if (e.dissolve <= 0.001f) {
        e.dissolve = 0.0f;
        e.state = AIState::Alert;
        e.bb.alertLevel = 0.6f;
        return false;                       // back in the world; normal rules
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
        // Reforming after a retreat that was interrupted by the player walking
        // into it. Solidify wherever it is.
        e.dissolve = std::max(0.0f, e.dissolve - kReformRate * dt);
    }

    AIState next = tickSmiler(e, dt, rng);

    // Losing sight of the player no longer wipes the slate. If it saw them
    // recently and its own tree has already given up, it goes and looks where
    // they were. This one rule is most of what reads as intelligence.
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

// --- Per-creature trees -----------------------------------------------------
// Unchanged in intent: these are the flavour on top of the shared perception
// rules above, and they are the reason eight creatures do not feel like one.

AIState BehaviorTree::tickSmiler(Entity& e, float dt, std::mt19937&) noexcept {
    if (e.bb.playerInSight) {
        e.flickerInfluence = std::min(1.0f, e.flickerInfluence + dt * 0.8f);
        return dist3d(e.pos, e.bb.lastKnownPlayerPos) < e.attackRadius
                   ? AIState::Attack : AIState::Stalk;
    }
    e.flickerInfluence = std::max(0.0f, e.flickerInfluence - dt * 0.3f);
    return e.bb.alertLevel > 0.3f ? AIState::Alert : AIState::Idle;
}








// --- Movement ---------------------------------------------------------------

void BehaviorTree::executeState(Entity& e, const WorldSense& sense,
                                float dt, std::mt19937& rng) noexcept {
    // Held in the beam it labours towards you instead of striding. The floor is
    // kMinTorchSpeedMult rather than zero because something frozen solid stops
    // being frightening the moment you notice it cannot move.
    const float lit = std::clamp(e.torchExposure / kRetreatExposure, 0.0f, 1.0f);
    const float torchDrag = 1.0f - (1.0f - kMinTorchSpeedMult) * lit;

    switch (e.state) {
        case AIState::Wander:
            doWander(e, dt, rng);
            break;
        case AIState::Chase:
            moveToward(e, sense.playerPos, dt, torchDrag);
            break;
        case AIState::Stalk:
            moveToward(e, sense.playerPos, dt, 0.55f * torchDrag);
            break;
        case AIState::Investigate:
            // Towards where it last had you, not towards where you are. It gets
            // there, finds nothing, and its own tree takes over again.
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

void BehaviorTree::doWander(Entity& e, float dt, std::mt19937& rng) noexcept {
    e.wanderTimer -= dt;
    if (e.wanderTimer <= 0) {
        std::uniform_real_distribution<float> ad(-1.0f, 1.0f);
        e.wanderAngle += ad(rng) * 1.2f;
        e.wanderTimer = 1.0f + std::uniform_real_distribution<float>(0, 2)(rng);
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
        // Standing on the player it has no direction to run in, and a zero
        // vector would leave it stuck in the beam forever. Pick one.
        dx = 1.0f; dz = 0.0f; d = 1.0f;
    }
    e.pos.x += dx / d * e.speed * speedMult * dt;
    e.pos.z += dz / d * e.speed * speedMult * dt;
}

} // namespace entity
} // namespace omni
