#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>
#include <functional>
#include <memory>
#include <numbers>
#include <optional>
#include <queue>
#include <random>
#include <ranges>
#include <span>
#include <unordered_map>
#include <unordered_set>
#include <vector>

#define TAG "OmniEntity"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)

namespace omni::entity {

constexpr float kTwoPi = 2.0f * std::numbers::pi_v<float>;

struct Vec2i {
    int x, y;
    bool operator==(const Vec2i& o) const noexcept { return x == o.x && y == o.y; }
};
struct Vec2f { float x, y; };
struct Vec3f { float x=0, y=0, z=0; };

struct Vec2iHash {
    size_t operator()(const Vec2i& v) const noexcept {
        return std::hash<int64_t>{}(static_cast<int64_t>(v.x) << 32 | static_cast<uint32_t>(v.y));
    }
};

enum class EntityType : uint8_t {
    Smiler       = 0,
    HoundDog     = 1,
    PartyGoer    = 2,
    Skin_Stealer = 3,
    WanderingOne = 4,
    Deathwatch   = 5,
    Crawler      = 6,
    FacelingDark = 7
};

enum class AIState : uint8_t {
    Idle      = 0,
    Wander    = 1,
    Alert     = 2,
    Chase     = 3,
    Attack    = 4,
    Flee      = 5,
    Stalk     = 6,
    Ambush    = 7
};

struct Blackboard {
    Vec3f lastKnownPlayerPos;
    float timeSincePlayerSeen = 999.0f;
    float alertLevel          = 0.0f;
    bool  playerInSight       = false;
    bool  heardNoise          = false;
    float noiseLevel          = 0.0f;
    int   patrolIndex         = 0;
};

struct Entity {
    Vec3f    pos;
    Vec3f    vel;
    float    speed;
    float    hearRadius;
    float    sightRadius;
    float    attackRadius;
    float    aggroRadius;
    float    wanderAngle;
    float    wanderTimer;
    float    attackCooldown;
    float    hp;
    float    maxHp;
    float    stalkTimer;
    float    ambushTimer;
    float    flickerInfluence;
    EntityType type;
    AIState  state;
    Blackboard bb;
    int      id;
    bool     active;
    std::vector<Vec3f> patrolPoints;
};

[[nodiscard]] float dist2d(const Vec3f& a, const Vec3f& b) noexcept {
    return std::hypot(a.x - b.x, a.z - b.z);
}
[[nodiscard]] float dist3d(const Vec3f& a, const Vec3f& b) noexcept {
    float dx=a.x-b.x, dy=a.y-b.y, dz=a.z-b.z;
    return std::sqrt(dx*dx+dy*dy+dz*dz);
}

class AStarPlanner {
public:
    [[nodiscard]] std::vector<Vec2i> plan(
            const Vec2i& start, const Vec2i& goal,
            const std::vector<std::vector<bool>>& grid) const {

        if (goal.y < 0 || goal.y >= static_cast<int>(grid.size()))    return {};
        if (goal.x < 0 || goal.x >= static_cast<int>(grid[0].size())) return {};
        if (!grid[goal.y][goal.x]) return {};

        using Node = std::pair<float, Vec2i>;
        std::priority_queue<Node, std::vector<Node>, std::greater<>> open;
        std::unordered_map<Vec2i, Vec2i,  Vec2iHash> from;
        std::unordered_map<Vec2i, float,  Vec2iHash> g;
        std::unordered_set<Vec2i, Vec2iHash>          closed;

        g[start] = 0.0f;
        open.emplace(heur(start, goal), start);

        constexpr std::array<Vec2i,8> dirs{
            Vec2i{0,1},{0,-1},{1,0},{-1,0},
            {1,1},{1,-1},{-1,1},{-1,-1}
        };
        constexpr std::array<float,8> costs{ 1.0f,1.0f,1.0f,1.0f, 1.414f,1.414f,1.414f,1.414f };

        while (!open.empty()) {
            auto [_, cur] = open.top(); open.pop();
            if (cur == goal) return reconstruct(from, cur);
            if (closed.contains(cur)) continue;
            closed.insert(cur);

            for (size_t i = 0; i < dirs.size(); ++i) {
                Vec2i nb{ cur.x + dirs[i].x, cur.y + dirs[i].y };
                if (nb.y < 0 || nb.y >= static_cast<int>(grid.size()))    continue;
                if (nb.x < 0 || nb.x >= static_cast<int>(grid[0].size())) continue;
                if (!grid[nb.y][nb.x]) continue;
                if (closed.contains(nb)) continue;
                float ng = g[cur] + costs[i];
                if (!g.contains(nb) || ng < g[nb]) {
                    g[nb] = ng; from[nb] = cur;
                    open.emplace(ng + heur(nb, goal), nb);
                }
            }
        }
        return {};
    }

private:
    [[nodiscard]] static float heur(const Vec2i& a, const Vec2i& b) noexcept {
        float dx = std::abs(a.x - b.x), dy = std::abs(a.y - b.y);
        return std::max(dx,dy) + (std::sqrt(2.0f) - 1.0f) * std::min(dx,dy);
    }
    [[nodiscard]] static std::vector<Vec2i> reconstruct(
            const std::unordered_map<Vec2i,Vec2i,Vec2iHash>& from,
            Vec2i cur) {
        std::vector<Vec2i> path;
        while (from.contains(cur)) { path.push_back(cur); cur = from.at(cur); }
        std::ranges::reverse(path);
        return path;
    }
};

class BehaviorTree {
public:
    static void tick(Entity& e, const Vec3f& playerPos, float dt, std::mt19937& rng) {
        updateBlackboard(e, playerPos, dt);
        AIState next = e.state;

        switch (e.type) {
            case EntityType::Smiler:       next = tickSmiler(e, dt, rng);       break;
            case EntityType::HoundDog:     next = tickHound(e, dt, rng);        break;
            case EntityType::PartyGoer:    next = tickPartyGoer(e, dt, rng);    break;
            case EntityType::Skin_Stealer: next = tickSkinStealer(e, dt, rng);  break;
            case EntityType::WanderingOne: next = tickWanderer(e, dt, rng);     break;
            case EntityType::Deathwatch:   next = tickDeathwatch(e, dt, rng);   break;
            case EntityType::Crawler:      next = tickCrawler(e, dt, rng);      break;
            case EntityType::FacelingDark: next = tickFaceling(e, dt, rng);     break;
        }

        e.state = next;
        executeState(e, playerPos, dt, rng);
    }

private:
    static void updateBlackboard(Entity& e, const Vec3f& pp, float dt) noexcept {
        float d = dist3d(e.pos, pp);
        e.bb.timeSincePlayerSeen += dt;
        e.bb.playerInSight = (d < e.sightRadius);
        e.bb.heardNoise    = (d < e.hearRadius);
        e.bb.alertLevel    = std::clamp(
            e.bb.alertLevel + (e.bb.playerInSight ? 2.0f * dt : -0.5f * dt), 0.0f, 1.0f);
        if (e.bb.playerInSight) {
            e.bb.lastKnownPlayerPos   = pp;
            e.bb.timeSincePlayerSeen  = 0.0f;
        }
    }

    static AIState tickSmiler(Entity& e, float dt, std::mt19937&) noexcept {
        if (e.bb.playerInSight) {
            e.flickerInfluence = std::min(1.0f, e.flickerInfluence + dt * 0.8f);
            return dist3d(e.pos, e.bb.lastKnownPlayerPos) < e.attackRadius
                ? AIState::Attack : AIState::Stalk;
        }
        e.flickerInfluence = std::max(0.0f, e.flickerInfluence - dt * 0.3f);
        return e.bb.alertLevel > 0.3f ? AIState::Alert : AIState::Idle;
    }

    static AIState tickHound(Entity& e, float dt, std::mt19937&) noexcept {
        if (e.bb.alertLevel > 0.5f) return AIState::Chase;
        if (e.bb.heardNoise)         return AIState::Alert;
        return AIState::Wander;
    }

    static AIState tickPartyGoer(Entity& e, float dt, std::mt19937& rng) noexcept {
        std::uniform_real_distribution<float> nd(0.0f,1.0f);
        if (e.bb.playerInSight && nd(rng) < 0.01f) return AIState::Attack;
        if (e.bb.playerInSight) return AIState::Chase;
        return AIState::Wander;
    }

    static AIState tickSkinStealer(Entity& e, float dt, std::mt19937&) noexcept {
        if (e.bb.alertLevel > 0.7f) return AIState::Chase;
        if (e.bb.playerInSight)     return AIState::Stalk;
        return e.stalkTimer > 0.0f ? AIState::Stalk : AIState::Wander;
    }

    static AIState tickWanderer(Entity& e, float, std::mt19937& rng) noexcept {
        std::uniform_real_distribution<float> nd(0.0f,1.0f);
        if (e.bb.playerInSight && nd(rng) < 0.3f) return AIState::Flee;
        return AIState::Wander;
    }

    static AIState tickDeathwatch(Entity& e, float dt, std::mt19937&) noexcept {
        e.ambushTimer -= dt;
        if (e.bb.playerInSight && e.ambushTimer <= 0.0f) return AIState::Ambush;
        if (e.bb.playerInSight) return AIState::Stalk;
        return AIState::Idle;
    }

    static AIState tickCrawler(Entity& e, float, std::mt19937&) noexcept {
        if (e.bb.alertLevel > 0.4f) return AIState::Chase;
        return AIState::Wander;
    }

    static AIState tickFaceling(Entity& e, float dt, std::mt19937& rng) noexcept {
        std::uniform_real_distribution<float> nd(0.0f,1.0f);
        if (dist3d(e.pos, e.bb.lastKnownPlayerPos) < e.attackRadius) return AIState::Attack;
        if (e.bb.alertLevel > 0.6f) return AIState::Chase;
        if (e.bb.playerInSight && nd(rng) < 0.005f) return AIState::Ambush;
        return e.bb.heardNoise ? AIState::Alert : AIState::Wander;
    }

    static void executeState(Entity& e, const Vec3f& pp, float dt, std::mt19937& rng) noexcept {
        switch (e.state) {
            case AIState::Idle: break;
            case AIState::Wander:    doWander(e, dt, rng);     break;
            case AIState::Alert:     doAlert(e, dt);            break;
            case AIState::Chase:     doChase(e, pp, dt);        break;
            case AIState::Attack:    doAttack(e, pp, dt);       break;
            case AIState::Flee:      doFlee(e, pp, dt);         break;
            case AIState::Stalk:     doStalk(e, pp, dt);        break;
            case AIState::Ambush:    doAmbush(e, pp, dt, rng);  break;
        }
        e.vel.y -= 9.81f * dt;
        if (e.pos.y <= 0.0f) { e.pos.y = 0.0f; e.vel.y = 0.0f; }
        e.pos.x += e.vel.x * dt;
        e.pos.y += e.vel.y * dt;
        e.pos.z += e.vel.z * dt;
        e.vel.x *= 0.85f;
        e.vel.z *= 0.85f;
        e.attackCooldown = std::max(0.0f, e.attackCooldown - dt);
        e.stalkTimer     = std::max(0.0f, e.stalkTimer - dt);
    }

    static void doWander(Entity& e, float dt, std::mt19937& rng) noexcept {
        std::uniform_real_distribution<float> nd(-1.0f, 1.0f);
        e.wanderTimer -= dt;
        if (e.wanderTimer <= 0.0f) {
            e.wanderAngle += nd(rng) * 1.2f;
            e.wanderTimer  = 1.5f + nd(rng) * 0.5f;
        }
        float s = e.speed * 0.28f;
        e.vel.x = std::cos(e.wanderAngle) * s;
        e.vel.z = std::sin(e.wanderAngle) * s;
    }

    static void doAlert(Entity& e, float dt) noexcept {
        float dx = e.bb.lastKnownPlayerPos.x - e.pos.x;
        float dz = e.bb.lastKnownPlayerPos.z - e.pos.z;
        float d  = std::hypot(dx, dz);
        if (d < 0.5f) return;
        float s = e.speed * 0.5f;
        e.vel.x = (dx/d) * s;
        e.vel.z = (dz/d) * s;
    }

    static void doChase(Entity& e, const Vec3f& pp, float dt) noexcept {
        float dx = pp.x - e.pos.x, dz = pp.z - e.pos.z;
        float d  = std::hypot(dx, dz);
        if (d < 0.01f) return;
        e.vel.x = (dx/d) * e.speed;
        e.vel.z = (dz/d) * e.speed;
    }

    static void doAttack(Entity& e, const Vec3f& pp, float dt) noexcept {
        if (e.attackCooldown > 0.0f) return;
        float dx = pp.x - e.pos.x, dz = pp.z - e.pos.z;
        float d  = std::hypot(dx, dz);
        if (d < e.attackRadius) {
            e.attackCooldown = 1.8f;
            float lunge = e.speed * 3.0f;
            e.vel.x += (dx/d) * lunge;
            e.vel.z += (dz/d) * lunge;
        }
    }

    static void doFlee(Entity& e, const Vec3f& pp, float dt) noexcept {
        float dx = e.pos.x - pp.x, dz = e.pos.z - pp.z;
        float d  = std::hypot(dx, dz);
        if (d < 0.01f) return;
        e.vel.x = (dx/d) * e.speed * 1.4f;
        e.vel.z = (dz/d) * e.speed * 1.4f;
    }

    static void doStalk(Entity& e, const Vec3f& pp, float dt) noexcept {
        float desired = e.sightRadius * 0.7f;
        float dx = pp.x - e.pos.x, dz = pp.z - e.pos.z;
        float d  = std::hypot(dx, dz);
        if (d < 0.01f) return;
        float err = d - desired;
        float s   = std::clamp(err * 0.5f, -e.speed * 0.6f, e.speed * 0.6f);
        e.vel.x += (dx/d) * s * dt * 10.0f;
        e.vel.z += (dz/d) * s * dt * 10.0f;
        e.stalkTimer = std::max(e.stalkTimer, 3.0f);
    }

    static void doAmbush(Entity& e, const Vec3f& pp, float dt, std::mt19937& rng) noexcept {
        float dx = pp.x - e.pos.x, dz = pp.z - e.pos.z;
        float d  = std::hypot(dx, dz);
        if (d < 0.01f) return;
        float burst = e.speed * 4.5f;
        e.vel.x = (dx/d) * burst;
        e.vel.z = (dz/d) * burst;
        e.ambushTimer = 8.0f + std::uniform_real_distribution<float>(0,4)(rng);
    }
};

struct EntitySystem {
    std::vector<Entity> entities;
    AStarPlanner        planner;
    std::mt19937        rng{std::random_device{}()};
    Vec3f               playerPos;
    float               playerNoise = 0.0f;
};

} // namespace omni::entity

static omni::entity::EntitySystem gEntitySys;

extern "C" {

JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_initEntities(JNIEnv*, jobject) {
    gEntitySys.entities.clear();
    LOGI("EntitySystem initialized");
}

JNIEXPORT jint JNICALL
Java_com_omni_backrooms_Native_1Bridge_spawnEntity(
        JNIEnv*, jobject,
        jfloat x, jfloat y, jfloat z,
        jfloat speed, jfloat hear, jfloat sight, jfloat aggro,
        jint typeId) {

    using namespace omni::entity;
    int id = static_cast<int>(gEntitySys.entities.size());
    Entity e{};
    e.pos          = {x, y, z};
    e.speed        = speed;
    e.hearRadius   = hear;
    e.sightRadius  = sight;
    e.aggroRadius  = aggro;
    e.attackRadius = 1.4f;
    e.type         = static_cast<EntityType>(typeId % 8);
    e.state        = AIState::Wander;
    e.hp = e.maxHp = 100.0f;
    e.wanderTimer  = 1.0f;
    e.ambushTimer  = 5.0f + std::uniform_real_distribution<float>(0,5)(gEntitySys.rng);
    e.active       = true;
    e.id           = id;
    gEntitySys.entities.push_back(std::move(e));
    return id;
}

JNIEXPORT jfloatArray JNICALL
Java_com_omni_backrooms_Native_1Bridge_tickEntities(
        JNIEnv* env, jobject,
        jfloat px, jfloat py, jfloat pz, jfloat dt) {

    using namespace omni::entity;
    gEntitySys.playerPos = {px, py, pz};

    for (auto& e : gEntitySys.entities) {
        if (!e.active) continue;
        BehaviorTree::tick(e, gEntitySys.playerPos, dt, gEntitySys.rng);
    }

    int floatsPerEntity = 10;
    auto count = static_cast<jsize>(gEntitySys.entities.size() * floatsPerEntity);
    auto arr   = env->NewFloatArray(count);
    if (!arr) return nullptr;

    std::vector<float> flat; flat.reserve(count);
    for (const auto& e : gEntitySys.entities) {
        flat.push_back(e.pos.x);
        flat.push_back(e.pos.y);
        flat.push_back(e.pos.z);
        flat.push_back(static_cast<float>(e.state));
        flat.push_back(e.bb.alertLevel);
        flat.push_back(e.hp / e.maxHp);
        flat.push_back(e.flickerInfluence);
        flat.push_back(e.bb.playerInSight ? 1.0f : 0.0f);
        flat.push_back(static_cast<float>(e.type));
        flat.push_back(e.active ? 1.0f : 0.0f);
    }
    env->SetFloatArrayRegion(arr, 0, count, flat.data());
    return arr;
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_damageEntity(JNIEnv*, jobject, jint id, jfloat amount) {
    if (id < 0 || id >= static_cast<int>(gEntitySys.entities.size())) return;
    auto& e = gEntitySys.entities[id];
    e.hp = std::max(0.0f, e.hp - amount);
    if (e.hp <= 0.0f) e.active = false;
}

JNIEXPORT jfloat JNICALL
Java_com_omni_backrooms_Native_1Bridge_getTotalFlickerInfluence(JNIEnv*, jobject) {
    float total = 0.0f;
    for (const auto& e : gEntitySys.entities) {
        if (!e.active) continue;
        float d = omni::entity::dist3d(e.pos, gEntitySys.playerPos);
        float falloff = std::max(0.0f, 1.0f - d / 15.0f);
        total += e.flickerInfluence * falloff;
    }
    return std::clamp(total, 0.0f, 1.0f);
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_destroyEntities(JNIEnv*, jobject) {
    gEntitySys.entities.clear();
}

} // extern "C"
