#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <algorithm>
#include <array>
#include <atomic>
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
#include <vector>

#define TAG "OmniCore"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace omni::core {

struct Vec2f {
    float x = 0, y = 0;
    constexpr Vec2f operator+(const Vec2f& o) const noexcept { return {x+o.x, y+o.y}; }
    constexpr Vec2f operator*(float s)         const noexcept { return {x*s,   y*s};   }
};

struct Vec3f { float x=0, y=0, z=0; };

enum class RoomType : uint8_t {
    Corridor = 0,
    Hub,
    DeadEnd,
    Loop,
    Stairwell,
    BackOffice,
    PoolRoom
};

struct LightState {
    float phase;
    float baseIntensity;
    float flickerSpeed;
    bool  broken;
    float breakChance;
};

struct DecalInfo {
    float u, v;
    uint8_t type;
    float rotation;
};

struct CorridorSegment {
    Vec2f     position;
    float     width;
    float     length;
    float     height;
    RoomType  roomType;
    LightState light;
    std::vector<DecalInfo> decals;
    int       connectedTo[4] = {-1,-1,-1,-1};
    uint8_t   wallDamage;
    float     moistureLevel;
    bool      hasHazard;
};

struct LevelGraph {
    std::vector<CorridorSegment> nodes;
    int seed = 0;
    int depth = 0;
};

class PerlinNoise {
public:
    explicit PerlinNoise(uint32_t seed) {
        std::mt19937 rng(seed);
        std::iota(p_.begin(), p_.begin() + 256, 0);
        std::shuffle(p_.begin(), p_.begin() + 256, rng);
        std::copy(p_.begin(), p_.begin() + 256, p_.begin() + 256);
    }

    [[nodiscard]] float noise2d(float x, float y) const noexcept {
        int xi = static_cast<int>(std::floor(x)) & 255;
        int yi = static_cast<int>(std::floor(y)) & 255;
        float xf = x - std::floor(x);
        float yf = y - std::floor(y);
        float u  = fade(xf), v = fade(yf);
        int aa = p_[p_[xi]+yi], ab = p_[p_[xi]+yi+1];
        int ba = p_[p_[xi+1]+yi], bb = p_[p_[xi+1]+yi+1];
        return std::lerp(
            std::lerp(grad(aa,xf,yf),   grad(ba,xf-1,yf),   u),
            std::lerp(grad(ab,xf,yf-1), grad(bb,xf-1,yf-1), u), v);
    }

    [[nodiscard]] float fbm(float x, float y, int octaves) const noexcept {
        float val = 0, amp = 0.5f, freq = 1.0f;
        for (int i = 0; i < octaves; ++i) {
            val  += noise2d(x * freq, y * freq) * amp;
            amp  *= 0.5f;
            freq *= 2.0f;
        }
        return val;
    }

private:
    std::array<int,512> p_{};
    [[nodiscard]] static float fade(float t) noexcept { return t*t*t*(t*(t*6-15)+10); }
    [[nodiscard]] static float grad(int h, float x, float y) noexcept {
        switch (h & 3) {
            case 0: return  x + y;
            case 1: return -x + y;
            case 2: return  x - y;
            default:return -x - y;
        }
    }
};

class CorridorGen {
public:
    explicit CorridorGen(uint64_t seed)
        : rng_(seed), perlin_(static_cast<uint32_t>(seed)) {}

    [[nodiscard]] LevelGraph generate(int nodeCount, int levelDepth) {
        LevelGraph graph;
        graph.seed  = static_cast<int>(rng_());
        graph.depth = levelDepth;
        graph.nodes.reserve(nodeCount);

        std::uniform_real_distribution<float> widthD(2.8f, 6.5f);
        std::uniform_real_distribution<float> lenD(6.0f, 28.0f);
        std::uniform_real_distribution<float> heightD(2.4f, 3.8f);
        std::uniform_real_distribution<float> phaseD(0.0f, 6.2831853f);
        std::uniform_real_distribution<float> intensD(0.55f, 1.0f);
        std::uniform_real_distribution<float> speedD(0.5f, 8.0f);
        std::bernoulli_distribution brokenD(0.08 + levelDepth * 0.01);
        std::bernoulli_distribution hazardD(0.05 + levelDepth * 0.02);
        std::uniform_int_distribution<int> decalCountD(0, 4);
        std::uniform_real_distribution<float> decalUvD(0.0f, 1.0f);
        std::uniform_int_distribution<int> decalTypeD(0, 7);

        Vec2f cursor{0.0f, 0.0f};
        float prevAngle = 0.0f;

        for (int i = 0; i < nodeCount; ++i) {
            CorridorSegment seg;
            seg.position     = cursor;
            seg.width        = widthD(rng_);
            seg.length       = lenD(rng_);
            seg.height       = heightD(rng_);
            seg.wallDamage   = static_cast<uint8_t>(
                std::clamp(perlin_.fbm(cursor.x * 0.1f, cursor.y * 0.1f, 4) * 255.0f, 0.0f, 255.0f));
            seg.moistureLevel= (perlin_.fbm(cursor.x * 0.05f, cursor.y * 0.07f, 3) + 1.0f) * 0.5f;
            seg.hasHazard    = hazardD(rng_);

            seg.roomType = [&]() -> RoomType {
                float r = perlin_.noise2d(i * 0.3f, levelDepth * 0.5f);
                if (r > 0.7f)       return RoomType::Hub;
                if (r > 0.5f)       return RoomType::PoolRoom;
                if (r < -0.6f)      return RoomType::DeadEnd;
                if (r < -0.3f)      return RoomType::BackOffice;
                if (i % 7 == 0)     return RoomType::Stairwell;
                return RoomType::Corridor;
            }();

            seg.light.phase        = phaseD(rng_);
            seg.light.baseIntensity= intensD(rng_);
            seg.light.flickerSpeed = speedD(rng_);
            seg.light.broken       = brokenD(rng_);
            seg.light.breakChance  = 0.002f + levelDepth * 0.001f;

            int dc = decalCountD(rng_);
            seg.decals.reserve(dc);
            for (int d = 0; d < dc; ++d) {
                seg.decals.push_back({
                    decalUvD(rng_), decalUvD(rng_),
                    static_cast<uint8_t>(decalTypeD(rng_)),
                    decalUvD(rng_) * 6.2831853f
                });
            }

            float angleDelta = perlin_.noise2d(i * 0.2f, 0.5f) * 0.8f;
            prevAngle += angleDelta;
            cursor.x += std::sin(prevAngle) * seg.length;
            cursor.y += std::cos(prevAngle) * seg.length;

            if (i > 0 && i < nodeCount - 1) {
                seg.connectedTo[0] = i - 1;
                seg.connectedTo[1] = i + 1;
            }

            graph.nodes.push_back(std::move(seg));
        }

        generateLoops(graph);
        return graph;
    }

    [[nodiscard]] float flickerIntensity(float phase, float t, bool broken) const noexcept {
        if (broken) {
            float b1 = std::sin(t * 137.4f + phase);
            float b2 = std::sin(t * 23.7f  + phase * 2.3f);
            return (b1 * b2 > 0.5f) ? 1.0f : 0.0f;
        }
        float base  = std::sin(phase + t * 47.3f);
        float noise = std::sin(t * 317.1f + phase * 2.7f) * 0.3f;
        float hum   = std::sin(t * 60.0f * 6.2831853f / 44100.0f) * 0.05f;
        float raw   = (base + noise + hum + 1.0f) * 0.5f;
        return raw > 0.85f ? 1.0f : std::lerp(0.55f, 1.0f, raw / 0.85f);
    }

    [[nodiscard]] float moistureAtPos(float wx, float wy) const noexcept {
        return (perlin_.fbm(wx * 0.04f, wy * 0.04f, 5) + 1.0f) * 0.5f;
    }

private:
    void generateLoops(LevelGraph& g) {
        int n = static_cast<int>(g.nodes.size());
        for (int i = 2; i < n; ++i) {
            if (perlin_.noise2d(i * 0.7f, 1.3f) > 0.75f) {
                int target = i - 2;
                auto& seg = g.nodes[i];
                for (auto& c : seg.connectedTo) {
                    if (c == -1) { c = target; break; }
                }
            }
        }
    }

    std::mt19937_64 rng_;
    PerlinNoise     perlin_;
};

struct RGBA { uint8_t r, g, b, a; };

class VhsRenderer {
public:
    VhsRenderer() : rng_(std::random_device{}()) {}

    void apply(std::span<RGBA> px, int w, int h, float t, float intensity) {
        scanlines(px, w, h, intensity);
        staticNoise(px, intensity);
        hShift(px, w, h, t, intensity);
        chromaBleed(px, w, h, intensity);
        vignette(px, w, h, intensity);
        colorGrade(px, w, h);
    }

    void applyFlicker(std::span<RGBA> px, float flickerVal) noexcept {
        uint8_t scale = static_cast<uint8_t>(flickerVal * 255.0f);
        for (auto& p : px) {
            p.r = static_cast<uint8_t>((p.r * scale) >> 8);
            p.g = static_cast<uint8_t>((p.g * scale) >> 8);
            p.b = static_cast<uint8_t>((p.b * scale) >> 8);
        }
    }

private:
    void scanlines(std::span<RGBA> px, int w, int h, float intensity) noexcept {
        float strength = 0.10f + intensity * 0.06f;
        for (int y = 1; y < h; y += 2) {
            float f = 1.0f - strength * (1.0f + std::sin(y * 0.05f) * 0.2f);
            for (int x = 0; x < w; ++x) {
                auto& p = px[y * w + x];
                p.r = static_cast<uint8_t>(p.r * f);
                p.g = static_cast<uint8_t>(p.g * f);
                p.b = static_cast<uint8_t>(p.b * f);
            }
        }
    }

    void staticNoise(std::span<RGBA> px, float intensity) {
        std::uniform_int_distribution<int>   nd(-20, 20);
        std::uniform_real_distribution<float> cd(0.0f, 1.0f);
        float threshold = 0.96f - intensity * 0.04f;
        for (auto& p : px) {
            if (cd(rng_) > threshold) continue;
            int n = nd(rng_);
            p.r = static_cast<uint8_t>(std::clamp(p.r + n, 0, 255));
            p.g = static_cast<uint8_t>(std::clamp(p.g + n, 0, 255));
            p.b = static_cast<uint8_t>(std::clamp(p.b + n, 0, 255));
        }
    }

    void hShift(std::span<RGBA> px, int w, int h, float t, float intensity) {
        std::vector<RGBA> row(w);
        for (int y = 0; y < h; ++y) {
            float n = std::sin(y * 0.31f + t * 5.7f) * std::sin(t * 2.3f + y * 0.007f);
            float jitter = (std::sin(t * 397.1f + y * 0.8f) > 0.97f) ? 8.0f : 0.0f;
            int shift = static_cast<int>((n * 3.5f + jitter) * intensity);
            if (shift == 0) continue;
            std::copy(px.begin() + y * w, px.begin() + y * w + w, row.begin());
            for (int x = 0; x < w; ++x)
                px[y * w + x] = row[(x - shift + w) % w];
        }
    }

    void chromaBleed(std::span<RGBA> px, int w, int h, float intensity) noexcept {
        float alpha = 0.12f + intensity * 0.08f;
        for (int y = 0; y < h; ++y) {
            for (int x = 1; x < w; ++x) {
                auto& c = px[y * w + x];
                const auto& p = px[y * w + x - 1];
                c.r = static_cast<uint8_t>(c.r * (1.0f - alpha) + p.r * alpha);
            }
            for (int x = w - 2; x >= 0; --x) {
                auto& c = px[y * w + x];
                const auto& p = px[y * w + x + 1];
                c.b = static_cast<uint8_t>(c.b * (1.0f - alpha * 0.5f) + p.b * alpha * 0.5f);
            }
        }
    }

    void vignette(std::span<RGBA> px, int w, int h, float intensity) noexcept {
        float cx = w * 0.5f, cy = h * 0.5f;
        float maxR = std::hypot(cx, cy);
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                float r = std::hypot(x - cx, y - cy) / maxR;
                float v = 1.0f - r * r * (0.6f + intensity * 0.4f);
                v = std::max(v, 0.0f);
                auto& p = px[y * w + x];
                p.r = static_cast<uint8_t>(p.r * v);
                p.g = static_cast<uint8_t>(p.g * v);
                p.b = static_cast<uint8_t>(p.b * v);
            }
        }
    }

    void colorGrade(std::span<RGBA> px, int w, int h) noexcept {
        for (auto& p : px) {
            p.r = static_cast<uint8_t>(std::min(255, static_cast<int>(p.r * 0.95f + 12)));
            p.g = static_cast<uint8_t>(std::min(255, static_cast<int>(p.g * 1.05f)));
            p.b = static_cast<uint8_t>(std::min(255, static_cast<int>(p.b * 0.82f)));
        }
    }

    std::mt19937 rng_;
};

struct PhysicsBody {
    Vec3f pos, vel, acc;
    float radius = 0.4f;
    float mass   = 70.0f;
    bool  onGround = false;
};

struct CollisionResult {
    bool  hit;
    Vec3f normal;
    float penetration;
};

class PlayerPhysics {
public:
    void update(PhysicsBody& body, float dt) noexcept {
        float drag = body.onGround ? 8.0f : 1.5f;
        body.vel.x += body.acc.x * dt;
        body.vel.y += body.acc.y * dt - 9.81f * dt;
        body.vel.z += body.acc.z * dt;
        body.vel.x *= std::exp(-drag * dt);
        body.vel.z *= std::exp(-drag * dt);
        body.vel.y  = std::max(body.vel.y, -40.0f);
        body.pos.x += body.vel.x * dt;
        body.pos.y += body.vel.y * dt;
        body.pos.z += body.vel.z * dt;
        body.acc = {0,0,0};
        if (body.pos.y <= 0.0f) {
            body.pos.y   = 0.0f;
            body.vel.y   = 0.0f;
            body.onGround= true;
        } else {
            body.onGround= false;
        }
    }

    void applyForce(PhysicsBody& body, const Vec3f& force) noexcept {
        body.acc.x += force.x / body.mass;
        body.acc.y += force.y / body.mass;
        body.acc.z += force.z / body.mass;
    }

    [[nodiscard]] CollisionResult sphereAABB(
            const PhysicsBody& body,
            const Vec3f& boxMin, const Vec3f& boxMax) const noexcept {
        float cx = std::clamp(body.pos.x, boxMin.x, boxMax.x);
        float cy = std::clamp(body.pos.y, boxMin.y, boxMax.y);
        float cz = std::clamp(body.pos.z, boxMin.z, boxMax.z);
        float dx = body.pos.x - cx;
        float dy = body.pos.y - cy;
        float dz = body.pos.z - cz;
        float d2 = dx*dx + dy*dy + dz*dz;
        if (d2 >= body.radius * body.radius)
            return { false, {}, 0.0f };
        float dist = std::sqrt(d2);
        float pen  = body.radius - dist;
        Vec3f norm = dist > 1e-6f
            ? Vec3f{ dx/dist, dy/dist, dz/dist }
            : Vec3f{ 0, 1, 0 };
        return { true, norm, pen };
    }
};

struct CameraState {
    Vec3f pos;
    float yaw   = 0.0f;
    float pitch = 0.0f;
    float fov   = 70.0f;
    float bobPhase  = 0.0f;
    float bobAmount = 0.0f;
    float rollAngle = 0.0f;
    float targetPitch = 0.0f;
    float targetYaw   = 0.0f;
};

class CameraController {
public:
    void update(CameraState& cam, const PhysicsBody& body, float dt, float sensitivity) noexcept {
        cam.yaw   += (cam.targetYaw   - cam.yaw)   * std::min(1.0f, 20.0f * dt);
        cam.pitch += (cam.targetPitch - cam.pitch)  * std::min(1.0f, 20.0f * dt);
        cam.pitch  = std::clamp(cam.pitch, -89.0f, 89.0f);

        float speed = std::hypot(body.vel.x, body.vel.z);
        float targetBob = body.onGround ? speed * 0.04f : 0.0f;
        cam.bobAmount += (targetBob - cam.bobAmount) * 8.0f * dt;
        cam.bobPhase  += speed * 2.5f * dt;

        float bobY = std::sin(cam.bobPhase) * cam.bobAmount;
        float bobX = std::sin(cam.bobPhase * 0.5f) * cam.bobAmount * 0.5f;

        cam.pos.x = body.pos.x + bobX;
        cam.pos.y = body.pos.y + 1.7f + bobY;
        cam.pos.z = body.pos.z;

        float targetRoll = std::sin(cam.bobPhase * 0.5f) * cam.bobAmount * 0.8f;
        cam.rollAngle += (targetRoll - cam.rollAngle) * 6.0f * dt;
    }

    void look(CameraState& cam, float dx, float dy, float sensitivity) noexcept {
        cam.targetYaw   += dx * sensitivity;
        cam.targetPitch -= dy * sensitivity;
        cam.targetPitch  = std::clamp(cam.targetPitch, -89.0f, 89.0f);
    }
};

} // namespace omni::core

static omni::core::CorridorGen*     gCorridor  = nullptr;
static omni::core::VhsRenderer*     gVhs       = nullptr;
static omni::core::PlayerPhysics*   gPhysics   = nullptr;
static omni::core::CameraController* gCamera   = nullptr;
static omni::core::CameraState       gCamState;
static omni::core::PhysicsBody       gPlayerBody;

extern "C" {

JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_initCore(JNIEnv*, jobject, jlong seed) {
    delete gCorridor; gCorridor = new omni::core::CorridorGen(static_cast<uint64_t>(seed));
    delete gVhs;      gVhs      = new omni::core::VhsRenderer();
    delete gPhysics;  gPhysics  = new omni::core::PlayerPhysics();
    delete gCamera;   gCamera   = new omni::core::CameraController();
    gCamState   = {};
    gPlayerBody = {};
    gPlayerBody.pos = {0.0f, 1.7f, 0.0f};
    LOGI("Core initialized (seed=%lld)", static_cast<long long>(seed));
}

JNIEXPORT jfloat JNICALL
Java_com_omni_backrooms_Native_1Bridge_getFlicker(JNIEnv*, jobject, jfloat phase, jfloat t, jboolean broken) {
    return gCorridor ? gCorridor->flickerIntensity(phase, t, broken) : 1.0f;
}

JNIEXPORT jfloatArray JNICALL
Java_com_omni_backrooms_Native_1Bridge_generateLevel(JNIEnv* env, jobject, jint count, jint depth) {
    if (!gCorridor) return nullptr;
    auto graph = gCorridor->generate(count, depth);
    auto& nodes = graph.nodes;
    int floatsPerNode = 14;
    auto total = static_cast<jsize>(nodes.size() * floatsPerNode);
    auto arr   = env->NewFloatArray(total);
    if (!arr) return nullptr;
    std::vector<float> flat; flat.reserve(total);
    for (const auto& s : nodes) {
        flat.push_back(s.position.x);
        flat.push_back(s.position.y);
        flat.push_back(s.width);
        flat.push_back(s.length);
        flat.push_back(s.height);
        flat.push_back(s.light.phase);
        flat.push_back(s.light.baseIntensity);
        flat.push_back(s.light.flickerSpeed);
        flat.push_back(s.light.broken ? 1.0f : 0.0f);
        flat.push_back(static_cast<float>(s.roomType));
        flat.push_back(static_cast<float>(s.wallDamage) / 255.0f);
        flat.push_back(s.moistureLevel);
        flat.push_back(s.hasHazard ? 1.0f : 0.0f);
        flat.push_back(static_cast<float>(s.decals.size()));
    }
    env->SetFloatArrayRegion(arr, 0, total, flat.data());
    return arr;
}

JNIEXPORT jfloat JNICALL
Java_com_omni_backrooms_Native_1Bridge_getMoistureAt(JNIEnv*, jobject, jfloat x, jfloat y) {
    return gCorridor ? gCorridor->moistureAtPos(x, y) : 0.0f;
}

JNIEXPORT jboolean JNICALL
Java_com_omni_backrooms_Native_1Bridge_applyVhs(JNIEnv* env, jobject, jobject bitmap, jfloat t, jfloat intensity) {
    if (!gVhs || !bitmap) return JNI_FALSE;
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) return JNI_FALSE;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) return JNI_FALSE;
    void* px = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &px) < 0) return JNI_FALSE;
    std::span<omni::core::RGBA> span(reinterpret_cast<omni::core::RGBA*>(px), info.width * info.height);
    gVhs->apply(span, info.width, info.height, t, intensity);
    AndroidBitmap_unlockPixels(env, bitmap);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_applyFlicker(JNIEnv* env, jobject, jobject bitmap, jfloat val) {
    if (!gVhs || !bitmap) return;
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) return;
    void* px = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &px) < 0) return;
    std::span<omni::core::RGBA> span(reinterpret_cast<omni::core::RGBA*>(px), info.width * info.height);
    gVhs->applyFlicker(span, val);
    AndroidBitmap_unlockPixels(env, bitmap);
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_physicsTick(JNIEnv*, jobject, jfloat dt) {
    if (!gPhysics) return;
    gPhysics->update(gPlayerBody, dt);
    if (gCamera) gCamera->update(gCamState, gPlayerBody, dt, 1.0f);
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_applyMovement(JNIEnv*, jobject, jfloat fx, jfloat fy, jfloat fz) {
    if (!gPhysics) return;
    gPhysics->applyForce(gPlayerBody, {fx, fy, fz});
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_cameraLook(JNIEnv*, jobject, jfloat dx, jfloat dy, jfloat sensitivity) {
    if (!gCamera) return;
    gCamera->look(gCamState, dx, dy, sensitivity);
}

JNIEXPORT jfloatArray JNICALL
Java_com_omni_backrooms_Native_1Bridge_getCameraState(JNIEnv* env, jobject) {
    auto arr = env->NewFloatArray(9);
    if (!arr) return nullptr;
    float data[9] = {
        gCamState.pos.x, gCamState.pos.y, gCamState.pos.z,
        gCamState.yaw, gCamState.pitch, gCamState.rollAngle,
        gCamState.fov, gCamState.bobAmount, gCamState.bobPhase
    };
    env->SetFloatArrayRegion(arr, 0, 9, data);
    return arr;
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_destroyCore(JNIEnv*, jobject) {
    delete gCorridor; gCorridor = nullptr;
    delete gVhs;      gVhs      = nullptr;
    delete gPhysics;  gPhysics  = nullptr;
    delete gCamera;   gCamera   = nullptr;
    LOGI("Core destroyed");
}

} // extern "C"
