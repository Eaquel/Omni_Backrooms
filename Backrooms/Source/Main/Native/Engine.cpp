// ============================================================
//  libil2cpp.so — Omni Engine Runtime
//  Version: 2023.3.14f1  Build: 57e3c67d7e9f
// ============================================================

#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <aaudio/AAudio.h>
#include <algorithm>
#include <array>
#include <atomic>
#include <chrono>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <dirent.h>
#include <dlfcn.h>
#include <fcntl.h>
#include <functional>
#include <limits>
#include "Map/Level_0.h"
#include "Frame/Frame.h"
#include "Trail/Trail.h"
#include "Entity/Entity.h"
#include "Sound/Synth.h"
#include "Ending/Ending.h"
#include "Shield/Shield.h"

// Cells per chunk edge. 24 keeps a chunk mesh small enough to build in a frame
// while large enough that streaming happens rarely.
#define OMNI_CHUNK_CELLS 24
#include <linux/prctl.h>
#include <memory>
#include <mutex>
#include <numbers>
#include <optional>
#include <random>
#include <ranges>
#include <span>
#include <string>
#include <string_view>
#include <sys/prctl.h>
#include <sys/ptrace.h>
#include <sys/resource.h>
#include <sys/stat.h>
#include <sys/system_properties.h>
#include <unistd.h>
#include <unordered_map>
#include <unordered_set>
#include <vector>

#define TAG_CORE  "OmniCore"
#define TAG_GUARD "OmniGuard"
#define TAG_ENT   "OmniEntity"
#define TAG_SND   "OmniSound"

#define LOGI_C(...) __android_log_print(ANDROID_LOG_INFO,  TAG_CORE,  __VA_ARGS__)
#define LOGE_C(...) __android_log_print(ANDROID_LOG_ERROR, TAG_CORE,  __VA_ARGS__)
#define LOGI_G(...) __android_log_print(ANDROID_LOG_INFO,  TAG_GUARD, __VA_ARGS__)
#define LOGW_G(...) __android_log_print(ANDROID_LOG_WARN,  TAG_GUARD, __VA_ARGS__)
#define LOGI_E(...) __android_log_print(ANDROID_LOG_INFO,  TAG_ENT,   __VA_ARGS__)
#define LOGI_S(...) __android_log_print(ANDROID_LOG_INFO,  TAG_SND,   __VA_ARGS__)
#define LOGE_S(...) __android_log_print(ANDROID_LOG_ERROR, TAG_SND,   __VA_ARGS__)

namespace omni::core {

struct Vec2f {
    float x=0,y=0;
    constexpr Vec2f operator+(const Vec2f& o) const noexcept { return {x+o.x,y+o.y}; }
    constexpr Vec2f operator*(float s)         const noexcept { return {x*s,y*s}; }
};
struct Vec3f { float x=0,y=0,z=0; };
struct RGBA  { uint8_t r,g,b,a; };

enum class RoomType : uint8_t { Corridor=0,Hub,DeadEnd,Loop,Stairwell,BackOffice,PoolRoom };

struct LightState { float phase,baseIntensity,flickerSpeed,breakChance; bool broken; };
struct DecalInfo  { float u,v,rotation; uint8_t type; };

struct CorridorSegment {
    Vec2f    position;
    float    width,length,height;
    float    heading=0; // yaw (radians) of travel direction from `position`, world +Z at heading=0
    RoomType roomType;
    LightState light;
    std::vector<DecalInfo> decals;
    int      connectedTo[4]={-1,-1,-1,-1};
    uint8_t  wallDamage;
    float    moistureLevel;
    bool     hasHazard;
};

struct LevelGraph { std::vector<CorridorSegment> nodes; int seed=0,depth=0; };

struct PhysicsBody {
    Vec3f pos,vel,acc;
    float mass=80.0f,radius=0.35f;
    bool  onGround=false;
};
struct CollisionResult { bool hit; Vec3f normal; float penetration; };

struct CameraState {
    Vec3f pos;
    float yaw=0,pitch=0,fov=70,bobPhase=0,bobAmount=0,rollAngle=0,targetPitch=0,targetYaw=0;
    /** Eye above the feet, in metres. Crouching drives this down; the camera and
     *  the ceiling clamp both read it so the two can never disagree. */
    float eyeHeight=1.7f,targetEyeHeight=1.7f;
};

constexpr float kStandEye  = 1.7f;
constexpr float kCrouchEye = 1.02f;

class PerlinNoise {
public:
    explicit PerlinNoise(uint32_t seed) {
        std::mt19937 rng(seed);
        std::iota(p_.begin(),p_.begin()+256,0);
        std::shuffle(p_.begin(),p_.begin()+256,rng);
        std::copy(p_.begin(),p_.begin()+256,p_.begin()+256);
    }
    [[nodiscard]] float noise2d(float x,float y) const noexcept {
        int xi=static_cast<int>(std::floor(x))&255, yi=static_cast<int>(std::floor(y))&255;
        float xf=x-std::floor(x), yf=y-std::floor(y);
        float u=fade(xf), v=fade(yf);
        int aa=p_[p_[xi]+yi],ab=p_[p_[xi]+yi+1],ba=p_[p_[xi+1]+yi],bb=p_[p_[xi+1]+yi+1];
        return std::lerp(std::lerp(grad(aa,xf,yf),grad(ba,xf-1,yf),u),
                         std::lerp(grad(ab,xf,yf-1),grad(bb,xf-1,yf-1),u),v);
    }
    [[nodiscard]] float fbm(float x,float y,int octaves) const noexcept {
        float val=0,amp=0.5f,freq=1.0f;
        for(int i=0;i<octaves;++i){ val+=noise2d(x*freq,y*freq)*amp; amp*=0.5f; freq*=2.0f; }
        return val;
    }
private:
    std::array<int,512> p_{};
    [[nodiscard]] static float fade(float t) noexcept { return t*t*t*(t*(t*6-15)+10); }
    [[nodiscard]] static float grad(int h,float x,float y) noexcept {
        switch(h&3){ case 0:return x+y; case 1:return -x+y; case 2:return x-y; default:return -x-y; }
    }
};


/** Grid collision. Because cells are axis-aligned there are no seams to slip
 *  through: we resolve each axis independently against the cell the player is
 *  trying to enter, which also gives clean sliding along walls. */
inline void resolveGridCollision(const omni::map::Level0Field& g,PhysicsBody& body,Vec3f prev,bool skipCeiling=false,float eyeHeight=1.7f) noexcept {
    const float r=body.radius;

    // X axis
    {
        float nx=body.pos.x;
        int cz0=g.cellZ(prev.z-r), cz1=g.cellZ(prev.z+r);
        int cxa=g.cellX(nx-r),     cxb=g.cellX(nx+r);
        bool blocked=false;
        for(int cz=cz0;cz<=cz1&&!blocked;++cz)
            for(int cx=cxa;cx<=cxb;++cx)
                if(g.isSolid(cx,cz)){ blocked=true; break; }
        if(blocked){ body.pos.x=prev.x; body.vel.x=0.0f; }
    }
    // Z axis
    {
        float nz=body.pos.z;
        int cx0=g.cellX(body.pos.x-r), cx1=g.cellX(body.pos.x+r);
        int cza=g.cellZ(nz-r),         czb=g.cellZ(nz+r);
        bool blocked=false;
        for(int cx=cx0;cx<=cx1&&!blocked;++cx)
            for(int cz=cza;cz<=czb;++cz)
                if(g.isSolid(cx,cz)){ blocked=true; break; }
        if(blocked){ body.pos.z=prev.z; body.vel.z=0.0f; }
    }
    if(!skipCeiling){
        constexpr float kHead=0.15f;
        float maxY=std::max(omni::map::Level0Field::kHeight-eyeHeight-kHead,0.0f);
        if(body.pos.y>maxY){ body.pos.y=maxY; if(body.vel.y>0.0f) body.vel.y=0.0f; }
    }
}

class CorridorGen {
public:
    explicit CorridorGen(uint64_t seed): rng_(seed),perlin_(static_cast<uint32_t>(seed)) {}

    [[nodiscard]] LevelGraph generate(int nodeCount,int levelDepth) {
        LevelGraph graph;
        graph.seed=static_cast<int>(rng_()); graph.depth=levelDepth;
        graph.nodes.reserve(nodeCount);
        std::uniform_real_distribution<float> wD(2.8f,6.5f),lD(6.0f,28.0f),hD(2.4f,3.8f);
        std::uniform_real_distribution<float> phD(0,6.2831853f),inD(0.55f,1.0f),spD(0.5f,8.0f);
        std::bernoulli_distribution brD(0.08+levelDepth*0.01),hzD(0.05+levelDepth*0.02);
        std::uniform_int_distribution<int> dcD(0,4),dtD(0,7);
        std::uniform_real_distribution<float> uvD(0,1);
        Vec2f cursor{}; float prevAngle=0;
        for(int i=0;i<nodeCount;++i){
            CorridorSegment seg;
            seg.position=cursor; seg.width=wD(rng_); seg.length=lD(rng_); seg.height=hD(rng_);
            seg.wallDamage=static_cast<uint8_t>(std::clamp(perlin_.fbm(cursor.x*0.1f,cursor.y*0.1f,4)*255.0f,0.0f,255.0f));
            seg.moistureLevel=(perlin_.fbm(cursor.x*0.05f,cursor.y*0.07f,3)+1.0f)*0.5f;
            seg.hasHazard=hzD(rng_);
            seg.roomType=[&]()->RoomType{
                float r=perlin_.noise2d(i*0.3f,levelDepth*0.5f);
                if(r>0.7f)  return RoomType::Hub;
                if(r>0.5f)  return RoomType::PoolRoom;
                if(r<-0.6f) return RoomType::DeadEnd;
                if(r<-0.3f) return RoomType::BackOffice;
                if(i%7==0)  return RoomType::Stairwell;
                return RoomType::Corridor;
            }();
            seg.light={phD(rng_),inD(rng_),spD(rng_),0.002f+levelDepth*0.001f,brD(rng_)};
            int dc=dcD(rng_); seg.decals.reserve(dc);
            for(int d=0;d<dc;++d) seg.decals.push_back({uvD(rng_),uvD(rng_),uvD(rng_)*6.2831853f,static_cast<uint8_t>(dtD(rng_))});
            float ad=perlin_.noise2d(i*0.2f,0.5f)*0.30f;
            prevAngle+=ad; seg.heading=prevAngle;
            cursor.x+=std::sin(prevAngle)*seg.length; cursor.y+=std::cos(prevAngle)*seg.length;
            if(i>0)            seg.connectedTo[0]=i-1;
            if(i<nodeCount-1)  seg.connectedTo[1]=i+1;
            graph.nodes.push_back(std::move(seg));
        }
        generateLoops(graph);
        return graph;
    }

    [[nodiscard]] float flickerIntensity(float phase,float t,bool broken) const noexcept {
        if(broken){ float b1=std::sin(t*137.4f+phase),b2=std::sin(t*23.7f+phase*2.3f); return (b1*b2>0.5f)?1.0f:0.0f; }
        float base=std::sin(phase+t*47.3f),noise=std::sin(t*317.1f+phase*2.7f)*0.3f;
        float hum=std::sin(t*60.0f*6.2831853f/44100.0f)*0.05f;
        float raw=(base+noise+hum+1.0f)*0.5f;
        return raw>0.85f?1.0f:std::lerp(0.55f,1.0f,raw/0.85f);
    }
    [[nodiscard]] float moistureAtPos(float wx,float wy) const noexcept {
        return (perlin_.fbm(wx*0.04f,wy*0.04f,5)+1.0f)*0.5f;
    }

private:
    void generateLoops(LevelGraph& g) {
        int n=static_cast<int>(g.nodes.size());
        for(int i=2;i<n;++i){
            if(perlin_.noise2d(i*0.7f,1.3f)>0.75f){
                for(auto& c: g.nodes[i].connectedTo){ if(c==-1){ c=i-2; break; } }
            }
        }
    }
    std::mt19937_64 rng_;
    PerlinNoise     perlin_;
};

class VhsRenderer {
public:
    void apply(std::span<RGBA> pixels,int w,int h,float t,float intensity) noexcept {
        std::mt19937 rng(static_cast<uint32_t>(t*1000));
        std::uniform_int_distribution<int> noiseD(-30,30);
        int scanY=static_cast<int>((t*0.7f-static_cast<int>(t*0.7f))*h);
        int chromaShift=static_cast<int>(intensity*4.0f);
        for(int y=0;y<h;++y){
            float scanAlpha=(std::abs(y-scanY)<6)?0.15f*intensity:0.0f;
            for(int x=0;x<w;++x){
                auto& p=pixels[y*w+x];
                int noise=static_cast<int>(noiseD(rng)*intensity*0.4f);
                int rr=std::clamp((int)p.r+noise,0,255);
                int gg=std::clamp((int)p.g+noise/2,0,255);
                int bb=std::clamp((int)p.b+noise,0,255);
                if(chromaShift>0&&x+chromaShift<w){
                    rr=std::clamp((int)pixels[y*w+std::min(x+chromaShift,w-1)].r+noise,0,255);
                    bb=std::clamp((int)pixels[y*w+std::max(x-chromaShift,0)].b+noise,0,255);
                }
                p.r=static_cast<uint8_t>(std::clamp(rr+(int)(scanAlpha*255),0,255));
                p.g=static_cast<uint8_t>(gg); p.b=static_cast<uint8_t>(bb);
            }
        }
    }
    void applyFlicker(std::span<RGBA> pixels,float val) noexcept {
        uint8_t m=static_cast<uint8_t>(std::clamp(val*255.0f,0.0f,255.0f));
        for(auto& p: pixels){ p.r=m; p.g=m; p.b=m; }
    }
};

class PlayerPhysics {
public:
    void update(PhysicsBody& body,float dt) noexcept {
        float drag=body.onGround?8.0f:1.5f;
        body.vel.x+=body.acc.x*dt; body.vel.y+=body.acc.y*dt-9.81f*dt; body.vel.z+=body.acc.z*dt;
        body.vel.x*=std::exp(-drag*dt); body.vel.z*=std::exp(-drag*dt);
        body.vel.y=std::max(body.vel.y,-40.0f);
        body.pos.x+=body.vel.x*dt; body.pos.y+=body.vel.y*dt; body.pos.z+=body.vel.z*dt;
        body.acc={};
        if(body.pos.y<=0){ body.pos.y=0; body.vel.y=0; body.onGround=true; }
        else body.onGround=false;
    }
    void applyForce(PhysicsBody& body,const Vec3f& f) noexcept {
        body.acc.x+=f.x/body.mass; body.acc.y+=f.y/body.mass; body.acc.z+=f.z/body.mass;
    }
    [[nodiscard]] CollisionResult sphereAABB(const PhysicsBody& body,const Vec3f& bMin,const Vec3f& bMax) const noexcept {
        float cx=std::clamp(body.pos.x,bMin.x,bMax.x);
        float cy=std::clamp(body.pos.y,bMin.y,bMax.y);
        float cz=std::clamp(body.pos.z,bMin.z,bMax.z);
        float dx=body.pos.x-cx,dy=body.pos.y-cy,dz=body.pos.z-cz;
        float d2=dx*dx+dy*dy+dz*dz;
        if(d2>=body.radius*body.radius) return {false,{},0};
        float dist=std::sqrt(d2),pen=body.radius-dist;
        Vec3f norm=dist>1e-6f?Vec3f{dx/dist,dy/dist,dz/dist}:Vec3f{0,1,0};
        return {true,norm,pen};
    }
};

class CameraController {
public:
    void update(CameraState& cam,const PhysicsBody& body,float dt,float) noexcept {
        cam.yaw  +=(cam.targetYaw  -cam.yaw)  *std::min(1.0f,20.0f*dt);
        cam.pitch+=(cam.targetPitch-cam.pitch) *std::min(1.0f,20.0f*dt);
        cam.pitch=std::clamp(cam.pitch,-89.0f,89.0f);
        // Crouching drops the eye over ~0.2 s rather than snapping, which is
        // what makes the button feel like a body moving instead of a teleport.
        cam.eyeHeight+=(cam.targetEyeHeight-cam.eyeHeight)*std::min(1.0f,12.0f*dt);
        float speed=std::hypot(body.vel.x,body.vel.z);
        float targetBob=body.onGround?speed*0.04f:0.0f;
        cam.bobAmount+=(targetBob-cam.bobAmount)*8.0f*dt;
        cam.bobPhase +=speed*2.5f*dt;
        float bobY=std::sin(cam.bobPhase)*cam.bobAmount;
        float bobX=std::sin(cam.bobPhase*0.5f)*cam.bobAmount*0.5f;
        cam.pos={body.pos.x+bobX,body.pos.y+cam.eyeHeight+bobY,body.pos.z};
        float targetRoll=std::sin(cam.bobPhase*0.5f)*cam.bobAmount*0.8f;
        cam.rollAngle+=(targetRoll-cam.rollAngle)*6.0f*dt;
    }
    /**
     * Degrees of view per density-independent pixel of drag, at sensitivity 1.
     *
     * dx and dy arrive in dp (GameHud converts before calling), and this is
     * what turns them into an angle. Without it the delta WAS the angle: on a
     * 1080p phone half a screen of drag came to about 500 degrees of yaw, so
     * the smallest deliberate movement threw the view across the corridor.
     *
     * 0.42 puts a full swipe across a typical 411dp-wide phone at ~173
     * degrees — just under a half turn, which is the standard this genre has
     * settled on for a reason: you can find something behind you in one
     * gesture without overshooting it.
     */
    static constexpr float kLookDegPerDp = 0.42f;

    void look(CameraState& cam,float dx,float dy,float sensitivity) noexcept {
        // Screen-right is (-cos(yaw),0,sin(yaw)) (gluLookAt side = forward x up),
        // so increasing yaw swings the view LEFT. Drag-right must decrease yaw.
        cam.targetYaw  -=dx*sensitivity*kLookDegPerDp;
        cam.targetPitch-=dy*sensitivity*kLookDegPerDp;
        cam.targetPitch=std::clamp(cam.targetPitch,-89.0f,89.0f);
    }
};

} // namespace omni::core


// The detectors, the monitor and the Unity costume moved to Shield/ — see the
// note at the top of Shield.h. What could not move is below: verifying the APK
// signature means calling PackageManager through JNI, so there is no version of
// it that runs without a JVM, and pretending otherwise would have meant a
// "portable" module with a JNIEnv* in its interface.
namespace omni::shield {

class SignatureVerifier {
public:
    explicit SignatureVerifier(std::string h): expected_(std::move(h)) {}
    [[nodiscard]] bool verify(JNIEnv* env,jobject ctx) noexcept {
        if(!env||!ctx||expected_.empty()) return true;
        jclass ctxCls=env->GetObjectClass(ctx);
        jmethodID getPm=env->GetMethodID(ctxCls,"getPackageManager","()Landroid/content/pm/PackageManager;");
        jmethodID getPkg=env->GetMethodID(ctxCls,"getPackageName","()Ljava/lang/String;");
        if(!getPm||!getPkg) return false;
        jobject pm=env->CallObjectMethod(ctx,getPm);
        auto pkg=static_cast<jstring>(env->CallObjectMethod(ctx,getPkg));
        if(!pm||!pkg) return false;
        jclass pmCls=env->GetObjectClass(pm);
        jmethodID getInfo=env->GetMethodID(pmCls,"getPackageInfo","(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
        if(!getInfo) return false;
        jobject info=env->CallObjectMethod(pm,getInfo,pkg,(jint)0x40);
        if(!info) return false;
        jclass piCls=env->GetObjectClass(info);
        jfieldID fid=env->GetFieldID(piCls,"signatures","[Landroid/content/pm/Signature;");
        if(!fid) return false;
        auto sigs=static_cast<jobjectArray>(env->GetObjectField(info,fid));
        if(!sigs||env->GetArrayLength(sigs)==0) return false;
        jobject sig0=env->GetObjectArrayElement(sigs,0);
        jclass sigCls=env->GetObjectClass(sig0);
        jmethodID toBytes=env->GetMethodID(sigCls,"toByteArray","()[B");
        if(!toBytes) return false;
        auto bytes=static_cast<jbyteArray>(env->CallObjectMethod(sig0,toBytes));
        if(!bytes) return false;
        int len=env->GetArrayLength(bytes);
        std::vector<uint8_t> raw(len);
        env->GetByteArrayRegion(bytes,0,len,reinterpret_cast<jbyte*>(raw.data()));
        return sha256Hex(raw.data(),raw.size())==expected_;
    }
private:
    std::string expected_;
};
} // namespace omni::shield

// The creatures live in Entity/Entity.h — see the note at the top of that
// file for why they had to leave this one.

namespace omni::sound {

constexpr int kSampleRate = 44100;
constexpr int kFrames     = 256;

struct SpatialParams {
    omni::entity::Vec3f listenerPos;
    float refDistance=1.0f,maxDistance=30.0f;
};

// ---------------------------------------------------------------------------
// Every one of these is a frame counter in front of a generator in Sound/Synth.
//
// They used to be generators in their own right, written out here a second
// time and much more crudely, and THEY were what the speaker actually played.
// Sound/Synth.h opens by saying "code you cannot hear is code nobody checks"
// and "what gets checked is what ships" -- and then fluorescentHum, footstep
// and monsterVoice, the three that Code_To_Sound.py renders and compares
// against a Python reference sample for sample, had no caller anywhere in the
// engine. Only the title sting reached the speaker. The tool was verifying
// three sounds nobody had ever heard while four cruder ones played unchecked.
//
// That is the fourth time one rule has existed in two copies here with only one
// of them checked -- the doorway, the two media3 artifacts, the light fittings,
// now this -- and the first time the checked copy was the dead one.
//
// What played instead: an 800-radian-per-second "click" that is really 127 Hz,
// a monster whose frequency modulation was applied to an integer sample counter
// so its phase jumped every time the pitch moved, and an ambience layer that
// was unfiltered white noise out of a std::mt19937 -- non-deterministic, so two
// players in the same place heard different things, which is the one property
// the header says the whole design exists to guarantee.
// ---------------------------------------------------------------------------

/** A continuous generator sampled by the callback. Holds a phase in seconds so
 *  the generator stays a pure function of time, and a parameter the game moves
 *  underneath it. */
class Continuous {
public:
    explicit Continuous(float param) noexcept : param_(param) {}
    void  set(float v)   noexcept { param_.store(std::clamp(v,0.0f,1.0f)); }
    float get()    const noexcept { return param_.load(); }
protected:
    /** Seconds since the stream started, wrapped at an hour so a long session
     *  cannot lose float precision in the low bits of t. */
    float advance() noexcept {
        const float t = static_cast<float>(frame_) / kSampleRate;
        if (++frame_ >= kSampleRate * 3600) frame_ = 0;
        return t;
    }
    std::atomic<float> param_;
private:
    int frame_ = 0;
};

class HumGenerator : public Continuous {
public:
    HumGenerator() noexcept : Continuous(0.6f) {}
    void fill(std::vector<float>& buf) noexcept {
        for(auto& s: buf) s = omni::sound::fluorescentHum(advance(), health_.load());
    }
    void setVolume(float v) noexcept { set(v); }
    float volume() const noexcept    { return get(); }
    /** 1 a good fitting, 0 a failing ballast. Driven from the cell the player
     *  is standing in, so the buzz belongs to the tube overhead. */
    void setHealth(float h) noexcept { health_.store(std::clamp(h,0.0f,1.0f)); }
private:
    std::atomic<float> health_{1.0f};
};

/** Footfalls at a fixed interval. Each one restarts the generator's clock, so
 *  what plays is one footstep from t=0 rather than a continuous tone gated by
 *  an envelope. */
class FootstepSynth {
public:
    void trigger(float bpm,float surface) noexcept {
        std::lock_guard lk(mtx_);
        interval_=static_cast<int>(60.0f/std::max(bpm,1.0f)*kSampleRate);
        surface_=std::clamp(surface,0.0f,1.0f);
        pace_=std::clamp((bpm-90.0f)/90.0f,0.0f,1.0f);
        counter_=0; active_=true;
    }
    void stop() noexcept { std::lock_guard lk(mtx_); active_=false; }
    float next() noexcept {
        std::lock_guard lk(mtx_);
        if(!active_) return 0.0f;
        if(counter_>=interval_) counter_=0;
        const float t=static_cast<float>(counter_)/kSampleRate;
        ++counter_;
        return omni::sound::footstep(t,pace_,surface_);
    }
private:
    mutable std::mutex mtx_;
    int counter_=0,interval_=22050;
    float surface_=0.0f,pace_=0.0f; bool active_=false;
};

class MonsterSynth {
public:
    void trigger(float intensity) noexcept {
        std::lock_guard lk(mtx_);
        intensity_=std::clamp(intensity,0.0f,1.0f); active_=true; frame_=0;
    }
    void stop() noexcept { std::lock_guard lk(mtx_); active_=false; }
    float next() noexcept {
        std::lock_guard lk(mtx_);
        if(!active_) return 0.0f;
        const float t=static_cast<float>(frame_)/kSampleRate;
        if(++frame_>=kSampleRate*3600) frame_=0;
        // Fade in over a second so a creature does not appear at full voice.
        return omni::sound::monsterVoice(t,intensity_)*std::min(1.0f,t);
    }
private:
    mutable std::mutex mtx_; int frame_=0; float intensity_=0; bool active_=false;
};

class AmbienceLayer : public Continuous {
public:
    AmbienceLayer() noexcept : Continuous(0.4f) {}
    void setLevel(float l) noexcept { set(l); }
    float next() noexcept { return omni::sound::roomTone(advance(),damp_.load())*get(); }
    void setDamp(float d) noexcept { damp_.store(std::clamp(d,0.0f,1.0f)); }
private:
    std::atomic<float> damp_{0.35f};
};

/** Her breathing, and her heart. Both ride the ambience gain rather than a
 *  channel of their own: they are the player's own body, and a player who has
 *  turned the ambience down has said they want the room quiet. */
class BodyLayer : public Continuous {
public:
    BodyLayer() noexcept : Continuous(1.0f) {}
    void setExertion(float e) noexcept { exertion_.store(std::clamp(e,0.0f,1.0f)); }
    void setFear(float f)     noexcept { fear_.store(std::clamp(f,0.0f,1.0f)); }
    float next() noexcept {
        const float t = advance();
        const float e = exertion_.load(), f = fear_.load();
        // Neither is audible at rest. You hear your own breath when you have
        // been running and your own heart when something is close, and at no
        // other time -- otherwise they are just two more loops.
        return omni::sound::breath(t,e)     * std::min(1.0f,e*1.6f)
             + omni::sound::heartbeat(t,f)  * std::min(1.0f,std::max(0.0f,f-0.25f)*2.0f);
    }
private:
    std::atomic<float> exertion_{0.0f}, fear_{0.0f};
};

class MixBus {
public:
    std::atomic<float> masterGain{0.9f},humGain{0.6f},footGain{0.8f},monsterGain{0.9f};
    float mix(float hum,float foot,float monster,float amb) const noexcept {
        float out=hum*humGain.load()+foot*footGain.load()+monster*monsterGain.load()+amb*0.5f;
        return std::clamp(out*masterGain.load(),-1.0f,1.0f);
    }
    /** The title sting rides on master only. It is not a monster and it is not
     *  a footstep, so a player who has turned those two down to play at night
     *  should still hear the tape come up. */
    float mixSting(float game,float sting) const noexcept {
        return std::clamp(game+sting*0.85f*masterGain.load(),-1.0f,1.0f);
    }
};

struct SoundEngine {
    AAudioStreamBuilder* builder=nullptr;
    AAudioStream*        stream=nullptr;
    HumGenerator   hum;
    FootstepSynth  foot;
    MonsterSynth   monster;
    AmbienceLayer  ambience;
    BodyLayer      body;
    OneShot        sting;
    OneShot        click;
    MixBus         bus;
    SpatialParams  spatial;
    std::vector<float>   mixBuf;
    std::atomic<bool>    running{false};
    std::mutex           mtx;
};

} // namespace omni::sound

static omni::core::CorridorGen*      gCorridor=nullptr;
static omni::core::VhsRenderer*      gVhs     =nullptr;
static omni::core::PlayerPhysics*    gPhysics =nullptr;
static omni::core::CameraController* gCamera  =nullptr;
static omni::core::CameraState       gCamState;
static omni::core::PhysicsBody       gPlayerBody;
static bool                          gSpawnFalling=false;
static omni::map::Level0Field        gField;
static int                           gSpawnCx = 0, gSpawnCz = 0;
static int                           gExitCx  = 0, gExitCz  = 0;
static omni::core::Vec3f             gPrevPos;
static omni::shield::GuardState       gGuard;
// Kept beside gGuard rather than inside it: GuardState is portable and this
// is not, and a unique_ptr to a JNI-only type is exactly what would have
// stopped the rest of Shield/ from compiling off-device.
static std::unique_ptr<omni::shield::SignatureVerifier> gSigVerifier;
static omni::entity::EntitySystem    gEntitySys;
static omni::sound::SoundEngine      gSound;

static aaudio_data_callback_result_t aaudioDataCallback(
        AAudioStream*, void* userData, void* audioData, int32_t numFrames) {
    auto* eng = static_cast<omni::sound::SoundEngine*>(userData);
    if(!eng->running.load()) return AAUDIO_CALLBACK_RESULT_STOP;
    std::lock_guard lk(eng->mtx);
    auto* out = static_cast<int16_t*>(audioData);
    std::vector<float> humBuf(numFrames),footBuf(numFrames),monBuf(numFrames),ambBuf(numFrames);
    eng->hum.fill(humBuf);
    for(int i=0;i<numFrames;++i){
        footBuf[i]=eng->foot.next(); monBuf[i]=eng->monster.next();
        // The room and her own body are one bed: both answer to the ambience
        // gain, so turning the room down turns the breathing down with it.
        ambBuf[i]=eng->ambience.next()+eng->body.next()*eng->ambience.get();
        float s=eng->bus.mix(humBuf[i]*eng->hum.volume(),footBuf[i],monBuf[i],ambBuf[i]);
        s=eng->bus.mixSting(s,eng->sting.next()+eng->click.next());
        int16_t pcm=static_cast<int16_t>(std::clamp(s*32767.0f,-32767.0f,32767.0f));
        out[i*2]=pcm; out[i*2+1]=pcm;
    }
    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

/**
 * A monotonic millisecond clock for the game loop.
 *
 * This used to live in the netcode, as the timestamp a ping packet carried, and
 * the loop borrowed it. The netcode is gone; the loop still needs a clock that
 * cannot go backwards when the user changes the time zone, so it is its own
 * function now rather than a leftover of something else.
 */
extern "C" JNIEXPORT jlong JNICALL
Java_com_omni_backrooms_NativeBridge_nowMs(JNIEnv*, jobject) {
    using namespace std::chrono;
    return duration_cast<milliseconds>(steady_clock::now().time_since_epoch()).count();
}

extern "C" JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_setPlayerState(JNIEnv*, jobject, jfloat x, jfloat y, jfloat z, jfloat yaw, jfloat pitch) {
    // Used when resuming a saved run: generateLevel() always drops the player at
    // a fresh random cell, so a resume has to put them back afterwards.
    gPlayerBody.pos = {x, y, z};
    gPlayerBody.vel = {};
    gPlayerBody.onGround = true;
    gPrevPos = gPlayerBody.pos;
    gSpawnFalling = false;
    gCamState.yaw = yaw;  gCamState.targetYaw = yaw;
    gCamState.pitch = pitch; gCamState.targetPitch = pitch;
    LOGI_C("Player state restored (%.2f, %.2f, %.2f) yaw=%.1f", x, y, z, yaw);
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_initCore(JNIEnv*, jobject, jlong seed) {
    delete gCorridor; gCorridor=new omni::core::CorridorGen(static_cast<uint64_t>(seed));
    delete gVhs;      gVhs     =new omni::core::VhsRenderer();
    delete gPhysics;  gPhysics =new omni::core::PlayerPhysics();
    delete gCamera;   gCamera  =new omni::core::CameraController();
    gCamState={}; gPlayerBody={}; gPlayerBody.pos={0.0f,1.7f,0.0f};
    gField.setSeed(static_cast<uint64_t>(seed));
    gPrevPos=gPlayerBody.pos;
    LOGI_C("Core init seed=%lld",static_cast<long long>(seed));
}

JNIEXPORT jfloat JNICALL
Java_com_omni_backrooms_NativeBridge_getFlicker(JNIEnv*, jobject, jfloat phase, jfloat t, jboolean broken) {
    return gCorridor?gCorridor->flickerIntensity(phase,t,broken):1.0f;
}

JNIEXPORT jfloatArray JNICALL
Java_com_omni_backrooms_NativeBridge_generateLevel(JNIEnv* env, jobject, jint count, jint depth) {
    // With an infinite field there is nothing to "generate" up front. This now
    // just resolves spawn/exit and hands back a small header; geometry is
    // streamed per chunk by generateChunk() as the player moves.
    (void)count; (void)depth;

    gField.findSpawn(gSpawnCx, gSpawnCz);
    gField.findExit(gSpawnCx, gSpawnCz, gExitCx, gExitCz);

    const float cell = omni::map::Level0Field::kCell;
    gPlayerBody.pos.x = omni::map::Level0Field::worldX(gSpawnCx) + cell * 0.5f;
    gPlayerBody.pos.z = omni::map::Level0Field::worldZ(gSpawnCz) + cell * 0.5f;
    gPlayerBody.pos.y = 16.0f;
    gPlayerBody.vel = {};
    gPrevPos = gPlayerBody.pos;
    gSpawnFalling = true;

    // header: [cellSize, height, spawnX, spawnZ, exitX, exitZ, chunkCells, reserved]
    const jsize total = 8;
    auto arr = env->NewFloatArray(total);
    if (!arr) return nullptr;
    float flat[8] = {
        cell,
        omni::map::Level0Field::kHeight,
        omni::map::Level0Field::worldX(gSpawnCx) + cell * 0.5f,
        omni::map::Level0Field::worldZ(gSpawnCz) + cell * 0.5f,
        omni::map::Level0Field::worldX(gExitCx) + cell * 0.5f,
        omni::map::Level0Field::worldZ(gExitCz) + cell * 0.5f,
        static_cast<float>(OMNI_CHUNK_CELLS),
        0.0f
    };
    env->SetFloatArrayRegion(arr, 0, total, flat);
    LOGI_C("Level0 infinite: spawn=(%d,%d) exit=(%d,%d)", gSpawnCx, gSpawnCz, gExitCx, gExitCz);
    return arr;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_omni_backrooms_NativeBridge_generateChunk(JNIEnv* env, jobject, jint chunkX, jint chunkZ) {
    // One chunk of cells, queried straight from the field. Nothing is cached
    // here: the field is cheap and stateless, and caching on this side would
    // just duplicate the mesh cache Kotlin already keeps.
    //
    // The chunk ships with a one-cell apron on every side. Without it the mesher
    // has to guess what lies past the edge, and guessing "solid" walled off every
    // chunk boundary with a wall the collision field knew nothing about — the
    // walls you could walk straight through where two chunks met.
    constexpr int N = OMNI_CHUNK_CELLS;
    constexpr int NP = N + 2;
    constexpr int kFloatsPerCell = 5;

    static thread_local omni::map::CellSample samples[NP * NP];
    gField.sampleChunk(chunkX, chunkZ, N, samples);

    const jsize total = NP * NP * kFloatsPerCell;
    auto arr = env->NewFloatArray(total);
    if (!arr) return nullptr;

    std::vector<float> flat;
    flat.reserve(total);
    for (int i = 0; i < NP * NP; ++i) {
        const auto& s = samples[i];
        flat.push_back(s.solid ? 1.0f : 0.0f);
        // Continuous illuminance, not a zone index. The mesher interpolates it
        // across faces, which is what removed the banding between regions.
        flat.push_back(s.light);
        flat.push_back(static_cast<float>(s.feature));
        flat.push_back(static_cast<float>(s.fixture));
        flat.push_back(s.power);
    }
    env->SetFloatArrayRegion(arr, 0, total, flat.data());
    return arr;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_omni_backrooms_NativeBridge_relocateExit(JNIEnv* env, jobject, jfloat px, jfloat pz, jfloat maxDistM) {
    // The world never ends, so an exit fixed at generation time can be walked
    // away from forever. Once the player is further than [maxDistM] from it the
    // door is re-anchored ahead of them: still a hike, but always findable.
    const float cell = omni::map::Level0Field::kCell;
    const float exitWx = omni::map::Level0Field::worldX(gExitCx) + cell * 0.5f;
    const float exitWz = omni::map::Level0Field::worldZ(gExitCz) + cell * 0.5f;
    const float dx = px - exitWx, dz = pz - exitWz;
    const bool tooFar = (dx * dx + dz * dz) > (maxDistM * maxDistM);

    if (tooFar) {
        const int pcx = omni::map::Level0Field::cellX(px);
        const int pcz = omni::map::Level0Field::cellZ(pz);
        // Far enough that it is still a run, close enough to be reachable.
        gField.findExitNear(pcx, pcz, 46, gExitCx, gExitCz);
        LOGI_C("Exit relocated to (%d,%d)", gExitCx, gExitCz);
    }

    auto arr = env->NewFloatArray(3);
    if (!arr) return nullptr;
    float out[3] = {
        omni::map::Level0Field::worldX(gExitCx) + cell * 0.5f,
        omni::map::Level0Field::worldZ(gExitCz) + cell * 0.5f,
        tooFar ? 1.0f : 0.0f
    };
    env->SetFloatArrayRegion(arr, 0, 3, out);
    return arr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_setCrouch(JNIEnv*, jobject, jboolean crouched) {
    gCamState.targetEyeHeight = crouched ? omni::core::kCrouchEye : omni::core::kStandEye;
}

// ---------------------------------------------------------------------------
// Cosmetics: frames and trails.
//
// The catalogues live in Frame/ and Trail/. These calls are the whole of the
// UI's access to them, so a cosmetic can be added, renamed or restyled without
// anything in Kotlin knowing its name.
// ---------------------------------------------------------------------------

extern "C" JNIEXPORT jint JNICALL
Java_com_omni_backrooms_NativeBridge_frameCount(JNIEnv*, jobject) {
    return omni::cosmetic::frameCount();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_omni_backrooms_NativeBridge_frameId(JNIEnv* env, jobject, jint index) {
    const auto* spec = omni::cosmetic::frameAt(index);
    return env->NewStringUTF(spec ? spec->id : "");
}

/** Palette and material, as 11 floats: base rgb, glow rgb, highlight rgb,
 *  tube ratio, shininess. */
extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_omni_backrooms_NativeBridge_frameSpec(JNIEnv* env, jobject, jint index) {
    const auto* s = omni::cosmetic::frameAt(index);
    if (!s) return nullptr;
    auto arr = env->NewFloatArray(11);
    if (!arr) return nullptr;
    const float out[11] = {
        s->baseR, s->baseG, s->baseB,
        s->glowR, s->glowG, s->glowB,
        s->hiR,   s->hiG,   s->hiB,
        s->tubeRatio, s->shininess
    };
    env->SetFloatArrayRegion(arr, 0, 11, out);
    return arr;
}

/** Static silhouette: [samples] * 2 floats, (radius, thickness) per position. */
extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_omni_backrooms_NativeBridge_frameProfile(JNIEnv* env, jobject, jint index, jint samples) {
    if (samples <= 0 || samples > 4096) return nullptr;
    if (!omni::cosmetic::frameAt(index)) return nullptr;
    std::vector<float> buf(static_cast<size_t>(samples) * 2, 0.0f);
    omni::cosmetic::frameProfile(index, samples, buf.data());
    auto arr = env->NewFloatArray(samples * 2);
    if (!arr) return nullptr;
    env->SetFloatArrayRegion(arr, 0, samples * 2, buf.data());
    return arr;
}

/** Emission at time [t]: [samples] floats in 0..1. Called once per rendered
 *  frame per visible ring, which is why it fills a caller-sized array in one
 *  crossing rather than being queried per position. */
extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_omni_backrooms_NativeBridge_frameEmission(JNIEnv* env, jobject, jint index, jint samples, jfloat t) {
    if (samples <= 0 || samples > 4096) return nullptr;
    if (!omni::cosmetic::frameAt(index)) return nullptr;
    std::vector<float> buf(static_cast<size_t>(samples), 0.0f);
    omni::cosmetic::frameEmission(index, samples, t, buf.data());
    auto arr = env->NewFloatArray(samples);
    if (!arr) return nullptr;
    env->SetFloatArrayRegion(arr, 0, samples, buf.data());
    return arr;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_omni_backrooms_NativeBridge_trailCount(JNIEnv*, jobject) {
    return omni::cosmetic::trailCount();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_omni_backrooms_NativeBridge_trailId(JNIEnv* env, jobject, jint index) {
    const auto* spec = omni::cosmetic::trailAt(index);
    return env->NewStringUTF(spec ? spec->id : "");
}

/** Tint rgb, lifetime, scale, spread, mark kind — 7 floats. */
extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_omni_backrooms_NativeBridge_trailSpec(JNIEnv* env, jobject, jint index) {
    const auto* s = omni::cosmetic::trailAt(index);
    if (!s) return nullptr;
    auto arr = env->NewFloatArray(7);
    if (!arr) return nullptr;
    const float out[7] = {
        s->tintR, s->tintG, s->tintB,
        s->lifetime, s->scale, s->spread,
        static_cast<float>(s->mark)
    };
    env->SetFloatArrayRegion(arr, 0, 7, out);
    return arr;
}

/** The player's own trail. One per process: there is one local walker. */
static omni::cosmetic::TrailField gTrail;

extern "C" JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_trailSetStyle(JNIEnv*, jobject, jint index) {
    gTrail.setStyle(index);
}

extern "C" JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_trailStep(JNIEnv*, jobject, jfloat x, jfloat z, jfloat yaw, jfloat side) {
    gTrail.step(x, z, yaw, side);
}

extern "C" JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_trailUpdate(JNIEnv*, jobject, jfloat dt) {
    gTrail.update(dt);
}

extern "C" JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_trailClear(JNIEnv*, jobject) {
    gTrail.clear();
}

/** Live stamps, five floats each: x, z, yaw, age, side. */
extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_omni_backrooms_NativeBridge_trailCollect(JNIEnv* env, jobject) {
    omni::cosmetic::TrailStamp stamps[omni::cosmetic::TrailField::kCapacity];
    const int n = gTrail.collect(stamps, omni::cosmetic::TrailField::kCapacity);
    auto arr = env->NewFloatArray(n * 5);
    if (!arr) return nullptr;
    if (n > 0) {
        std::vector<float> flat(static_cast<size_t>(n) * 5);
        for (int i = 0; i < n; ++i) {
            flat[i * 5 + 0] = stamps[i].x;
            flat[i * 5 + 1] = stamps[i].z;
            flat[i * 5 + 2] = stamps[i].yaw;
            flat[i * 5 + 3] = stamps[i].age;
            flat[i * 5 + 4] = stamps[i].side;
        }
        env->SetFloatArrayRegion(arr, 0, n * 5, flat.data());
    }
    return arr;
}

JNIEXPORT jfloat JNICALL
Java_com_omni_backrooms_NativeBridge_getMoistureAt(JNIEnv*, jobject, jfloat x, jfloat y) {
    return gCorridor?gCorridor->moistureAtPos(x,y):0.0f;
}

JNIEXPORT jboolean JNICALL
Java_com_omni_backrooms_NativeBridge_applyVhs(JNIEnv* env, jobject, jobject bitmap, jfloat t, jfloat intensity) {
    if(!gVhs||!bitmap) return JNI_FALSE;
    AndroidBitmapInfo info;
    if(AndroidBitmap_getInfo(env,bitmap,&info)<0) return JNI_FALSE;
    if(info.format!=ANDROID_BITMAP_FORMAT_RGBA_8888) return JNI_FALSE;
    void* px=nullptr;
    if(AndroidBitmap_lockPixels(env,bitmap,&px)<0) return JNI_FALSE;
    gVhs->apply(std::span<omni::core::RGBA>(reinterpret_cast<omni::core::RGBA*>(px),info.width*info.height),info.width,info.height,t,intensity);
    AndroidBitmap_unlockPixels(env,bitmap);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_applyFlicker(JNIEnv* env, jobject, jobject bitmap, jfloat val) {
    if(!gVhs||!bitmap) return;
    AndroidBitmapInfo info;
    if(AndroidBitmap_getInfo(env,bitmap,&info)<0) return;
    void* px=nullptr;
    if(AndroidBitmap_lockPixels(env,bitmap,&px)<0) return;
    gVhs->applyFlicker(std::span<omni::core::RGBA>(reinterpret_cast<omni::core::RGBA*>(px),info.width*info.height),val);
    AndroidBitmap_unlockPixels(env,bitmap);
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_physicsTick(JNIEnv*, jobject, jfloat dt) {
    if(!gPhysics) return;
    omni::core::Vec3f before=gPlayerBody.pos;
    gPhysics->update(gPlayerBody,dt);
    omni::core::resolveGridCollision(gField,gPlayerBody,before,gSpawnFalling,gCamState.eyeHeight);
    gPrevPos=gPlayerBody.pos;
    if(gSpawnFalling&&gPlayerBody.onGround) gSpawnFalling=false;
    if(gCamera) gCamera->update(gCamState,gPlayerBody,dt,1.0f);
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_applyMovement(JNIEnv*, jobject, jfloat fx, jfloat fy, jfloat fz) {
    if(!gPhysics) return;
    // fx = joystick right/strafe axis, fz = joystick forward axis (input space).
    // forward = (sin(yaw), cos(yaw)); right = forward x up = (-cos(yaw), sin(yaw)).
    float yawRad=gCamState.yaw*0.017453293f;
    float s=std::sin(yawRad), c=std::cos(yawRad);
    float wx=-fx*c+fz*s;
    float wz= fx*s+fz*c;
    // Upward impulses only from the ground: without this the player could jump
    // again every frame while airborne and climb straight through the ceiling.
    if(fy>0.0f && !gPlayerBody.onGround) fy=0.0f;
    gPhysics->applyForce(gPlayerBody,{wx,fy,wz});
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_cameraLook(JNIEnv*, jobject, jfloat dx, jfloat dy, jfloat sensitivity) {
    if(gCamera) gCamera->look(gCamState,dx,dy,sensitivity);
}

JNIEXPORT jfloatArray JNICALL
Java_com_omni_backrooms_NativeBridge_getCameraState(JNIEnv* env, jobject) {
    // Slot 9 carries the live eye height so the renderer can place the avatar's
    // feet on the floor; guessing 1.7 there is what left her hovering.
    auto arr=env->NewFloatArray(10); if(!arr) return nullptr;
    float d[10]={gCamState.pos.x,gCamState.pos.y,gCamState.pos.z,
                 gCamState.yaw,gCamState.pitch,gCamState.rollAngle,
                 gCamState.fov,gCamState.bobAmount,gCamState.bobPhase,
                 gCamState.eyeHeight};
    env->SetFloatArrayRegion(arr,0,10,d);
    return arr;
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_destroyCore(JNIEnv*, jobject) {
    delete gCorridor; gCorridor=nullptr;
    delete gVhs;      gVhs     =nullptr;
    delete gPhysics;  gPhysics =nullptr;
    delete gCamera;   gCamera  =nullptr;
    LOGI_C("Core destroyed");
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_initEntities(JNIEnv*, jobject) {
    gEntitySys.entities.clear();
    // The AI reads the world to decide what it can see. A Level0Field is a pure
    // function of its seed, so a second one with the same seed is the same
    // world — no pointer into gField to keep alive, no ordering to get wrong.
    gEntitySys.field.setSeed(gField.seed());
    gEntitySys.sense = omni::entity::WorldSense{};
    LOGI_E("EntitySystem initialized");
}

JNIEXPORT jint JNICALL
Java_com_omni_backrooms_NativeBridge_spawnEntity(
        JNIEnv*, jobject,
        jfloat x, jfloat y, jfloat z,
        jfloat speed, jfloat hear, jfloat sight, jfloat aggro,
        jint typeId) {
    using namespace omni::entity;
    int id=static_cast<int>(gEntitySys.entities.size());
    Entity e{};
    e.pos={x,y,z}; e.speed=speed; e.hearRadius=hear; e.sightRadius=sight;
    e.aggroRadius=aggro; e.attackRadius=1.4f;
    e.type=static_cast<EntityType>(typeId%kEntityTypeCount); e.state=AIState::Wander;
    e.hp=e.maxHp=100.0f; e.wanderTimer=1.0f;
    e.ambushTimer=5.0f+std::uniform_real_distribution<float>(0,5)(gEntitySys.rng);
    e.active=true; e.id=id;
    gEntitySys.entities.push_back(std::move(e));
    return id;
}

JNIEXPORT jfloatArray JNICALL
Java_com_omni_backrooms_NativeBridge_tickEntities(
        JNIEnv* env, jobject,
        jfloat px, jfloat py, jfloat pz, jfloat dt,
        jfloat noise, jfloat torchX, jfloat torchZ, jboolean torchOn) {
    using namespace omni::entity;
    gEntitySys.sense.playerPos = {px, py, pz};
    gEntitySys.sense.noise     = noise;
    gEntitySys.sense.torchX    = torchX;
    gEntitySys.sense.torchZ    = torchZ;
    gEntitySys.sense.torchOn   = (torchOn == JNI_TRUE);
    gEntitySys.tick(dt);

    // ---- the sound of standing here ------------------------------------
    //
    // Set from the tick rather than through JNI setters of their own. Every
    // one of these is something the engine already knows to the frame -- how
    // close the creature is, what the mains are doing overhead, how fast she
    // is going -- and a setter would mean Kotlin keeping a second copy of it
    // in step. This file has lost four days to rules kept in two places.
    {
        float nearest = 1e9f;
        for (const auto& e : gEntitySys.entities) {
            if (!e.active) continue;
            const float dx = e.pos.x - px, dz = e.pos.z - pz;
            nearest = std::min(nearest, std::sqrt(dx * dx + dz * dz));
        }
        // Nothing at 24 m, everything inside 4. Her heart is not a proximity
        // meter -- it does not start until the thing is close enough to matter.
        const float fear = std::clamp((24.0f - nearest) / 20.0f, 0.0f, 1.0f);

        const float power = gField.powerAt(omni::map::Level0Field::cellX(px),
                                           omni::map::Level0Field::cellZ(pz));
        // A section whose mains have failed is the section the water got into,
        // so the tube overhead and the drip are the same fact heard twice.
        gSound.hum.setHealth(power);
        gSound.ambience.setDamp(1.0f - power);
        gSound.body.setFear(fear);

        const float vx = gPlayerBody.vel.x, vz = gPlayerBody.vel.z;
        gSound.body.setExertion(std::clamp(std::sqrt(vx * vx + vz * vz) / 5.5f, 0.0f, 1.0f));
    }

    const int fpn = 11;
    auto count = static_cast<jsize>(gEntitySys.entities.size() * fpn);
    auto arr = env->NewFloatArray(count); if(!arr) return nullptr;
    std::vector<float> flat; flat.reserve(count);
    for(const auto& e: gEntitySys.entities){
        flat.push_back(e.pos.x); flat.push_back(e.pos.y); flat.push_back(e.pos.z);
        flat.push_back(static_cast<float>(e.state)); flat.push_back(e.bb.alertLevel);
        flat.push_back(e.hp/e.maxHp); flat.push_back(e.flickerInfluence);
        flat.push_back(e.bb.playerInSight?1.0f:0.0f); flat.push_back(static_cast<float>(e.type));
        flat.push_back(e.active?1.0f:0.0f); flat.push_back(e.dissolve);
    }
    env->SetFloatArrayRegion(arr,0,count,flat.data());
    return arr;
}

/**
 * Drives a creature off. Not kills it — nothing in the Backrooms dies.
 *
 * Damage now spends into the same exposure meter the torch fills, so being hurt
 * and being held in the beam are one mechanic with two inputs, and both end the
 * same way: it breaks off, fades out, waits, and comes back. Deactivating the
 * entity, which is what this did before, removed it from the level for good and
 * quietly turned every encounter into a fight the player could finish.
 */
JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_damageEntity(JNIEnv*, jobject, jint id, jfloat amount) {
    if(id<0||id>=static_cast<int>(gEntitySys.entities.size())) return;
    auto& e=gEntitySys.entities[id];
    e.hp=std::max(0.0f,e.hp-amount);
    // 100 damage is a full meter, so the existing 25-per-hit call takes four
    // hits — the same number of blows it used to take, with a different ending.
    e.torchExposure += omni::entity::kRetreatExposure * (amount / 100.0f);
    if(e.hp<=0.0f){
        e.hp = e.maxHp;
        e.torchExposure = omni::entity::kRetreatExposure;
    }
}

JNIEXPORT jfloat JNICALL
Java_com_omni_backrooms_NativeBridge_getTotalFlickerInfluence(JNIEnv*, jobject) {
    float total=0.0f;
    for(const auto& e: gEntitySys.entities){
        if(!e.active) continue;
        float d=omni::entity::dist3d(e.pos,gEntitySys.sense.playerPos);
        float falloff=std::max(0.0f,1.0f-d/15.0f);
        total+=e.flickerInfluence*falloff;
    }
    return std::clamp(total,0.0f,1.0f);
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_destroyEntities(JNIEnv*, jobject) {
    gEntitySys.entities.clear();
}

JNIEXPORT jboolean JNICALL
Java_com_omni_backrooms_NativeBridge_initSound(JNIEnv*, jobject) {
    gSound.running.store(false);
    if(gSound.stream){ AAudioStream_close(gSound.stream); gSound.stream=nullptr; }
    if(gSound.builder){ AAudioStreamBuilder_delete(gSound.builder); gSound.builder=nullptr; }

    AAudio_createStreamBuilder(&gSound.builder);
    if(!gSound.builder){ LOGE_S("AAudioStreamBuilder failed"); return JNI_FALSE; }

    AAudioStreamBuilder_setFormat(gSound.builder,AAUDIO_FORMAT_PCM_I16);
    AAudioStreamBuilder_setSampleRate(gSound.builder,44100);
    AAudioStreamBuilder_setChannelCount(gSound.builder,2);
    AAudioStreamBuilder_setPerformanceMode(gSound.builder,AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    AAudioStreamBuilder_setSharingMode(gSound.builder,AAUDIO_SHARING_MODE_EXCLUSIVE);
    AAudioStreamBuilder_setFramesPerDataCallback(gSound.builder,omni::sound::kFrames);
    AAudioStreamBuilder_setDataCallback(gSound.builder,aaudioDataCallback,&gSound);
    AAudioStreamBuilder_setErrorCallback(
        gSound.builder,
        [](AAudioStream*,void*,aaudio_result_t e){ LOGE_S("AAudio error: %d",e); },
        nullptr
    );

    aaudio_result_t res=AAudioStreamBuilder_openStream(gSound.builder,&gSound.stream);
    AAudioStreamBuilder_delete(gSound.builder);
    gSound.builder=nullptr;
    if(res!=AAUDIO_OK){ LOGE_S("AAudio openStream: %s",AAudio_convertResultToText(res)); return JNI_FALSE; }

    gSound.ambience.setLevel(0.4f);
    gSound.running.store(true);
    res=AAudioStream_requestStart(gSound.stream);
    if(res!=AAUDIO_OK){ LOGE_S("AAudio start: %s",AAudio_convertResultToText(res)); return JNI_FALSE; }

    LOGI_S("AAudio init stereo 44100 Hz frames=%d",omni::sound::kFrames);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_setMasterVolume(JNIEnv*, jobject, jfloat v)    { gSound.bus.masterGain.store(std::clamp(v,0.0f,1.0f)); }
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_setHumVolume(JNIEnv*, jobject, jfloat v)       { gSound.hum.setVolume(v); gSound.bus.humGain.store(std::clamp(v,0.0f,1.0f)); }
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_setFootstepVolume(JNIEnv*, jobject, jfloat v)  { gSound.bus.footGain.store(std::clamp(v,0.0f,1.0f)); }
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_setMonsterVolume(JNIEnv*, jobject, jfloat v)   { gSound.bus.monsterGain.store(std::clamp(v,0.0f,1.0f)); }
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_setAmbienceLevel(JNIEnv*, jobject, jfloat v)   { gSound.ambience.setLevel(v); }
/** Stops the footfalls. They ran on a fixed interval forever once triggered,
 *  so letting go of the stick left her walking on the spot. */
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_stopFootstep(JNIEnv*, jobject)                 { gSound.foot.stop(); }
/**
 * The run-over transition, sampled.
 *
 * Returns the eight parameters the post shader needs for this instant, so the
 * shader does no timing of its own and what Native_Check measures is what the
 * screen shows. Kotlin owns the clock because Kotlin owns the frame loop; the
 * shape of the thing lives in Ending/.
 */
JNIEXPORT jfloatArray JNICALL
Java_com_omni_backrooms_NativeBridge_endingParams(JNIEnv* env, jobject, jint kind, jfloat t) {
    using namespace omni::ending;
    const Params p = evaluate(static_cast<Kind>(kind), t);
    const float flat[8] = { p.desaturate, p.vignette, p.aberration, p.tear,
                            p.pull, p.bloom, p.exposure, p.panel };
    auto arr = env->NewFloatArray(8);
    if (!arr) return nullptr;
    env->SetFloatArrayRegion(arr, 0, 8, flat);
    return arr;
}

/** How long an ending runs, so Kotlin does not carry its own copy of it. */
JNIEXPORT jfloat JNICALL
Java_com_omni_backrooms_NativeBridge_endingDuration(JNIEnv*, jobject, jint kind) {
    return omni::ending::duration(static_cast<omni::ending::Kind>(kind));
}

/** The torch switch. 60 ms, which is the whole sound. */
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_playTorchClick(JNIEnv*, jobject) {
    std::lock_guard lk(gSound.mtx); gSound.click.start(0.06f, omni::sound::Shot::TorchClick);
}
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_triggerFootstep(JNIEnv*, jobject, jfloat bpm, jfloat surface) {
    std::lock_guard lk(gSound.mtx); gSound.foot.trigger(bpm,surface);
}
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_triggerMonster(JNIEnv*, jobject, jfloat intensity) {
    std::lock_guard lk(gSound.mtx); gSound.monster.trigger(intensity);
}
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_stopMonster(JNIEnv*, jobject) {
    std::lock_guard lk(gSound.mtx); gSound.monster.stop();
}
/**
 * The title sting: Eaquel's name over a dead tape spinning up.
 *
 * Synthesised, like everything else here — there is no audio file in this APK.
 * The generator is in Sound/Synth.cpp and Code_To_Sound.py renders that same
 * translation unit, so what is checked is what plays.
 */
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_playIntroSting(JNIEnv*, jobject, jfloat seconds) {
    std::lock_guard lk(gSound.mtx); gSound.sting.start(seconds, omni::sound::Shot::Sting);
}
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_stopIntroSting(JNIEnv*, jobject) {
    std::lock_guard lk(gSound.mtx); gSound.sting.stop();
}

JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_setListenerPos(JNIEnv*, jobject, jfloat x, jfloat y, jfloat z) {
    std::lock_guard lk(gSound.mtx); gSound.spatial.listenerPos={x,y,z};
}
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_setSpatialRolloff(JNIEnv*, jobject, jfloat ref, jfloat maxDist) {
    std::lock_guard lk(gSound.mtx); gSound.spatial.refDistance=ref; gSound.spatial.maxDistance=maxDist;
}
JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_destroySound(JNIEnv*, jobject) {
    gSound.running.store(false);
    if(gSound.stream){
        AAudioStream_requestStop(gSound.stream);
        AAudioStream_close(gSound.stream);
        gSound.stream=nullptr;
    }
    if(gSound.builder){
        AAudioStreamBuilder_delete(gSound.builder);
        gSound.builder=nullptr;
    }
    LOGI_S("Sound destroyed");
}

JNIEXPORT jboolean JNICALL
Java_com_omni_backrooms_NativeBridge_initGuard(JNIEnv* env, jobject, jobject ctx, jstring expectedSigHash) {
    using namespace omni::shield;
    const char* raw=env->GetStringUTFChars(expectedSigHash,nullptr);
    std::string hash(raw?raw:"");
    if(raw) env->ReleaseStringUTFChars(expectedSigHash,raw);
    gSigVerifier=std::make_unique<SignatureVerifier>(hash);
    if(!hash.empty()&&ctx&&!gSigVerifier->verify(env,ctx))
        gGuard.cachedFlags.fetch_or(FLAG_SIG_MISMATCH,std::memory_order_acq_rel);
    uint32_t initial=gGuard.root.scan()|gGuard.frida.scan()|gGuard.debug.scan()|gGuard.emulator.scan();
    gGuard.cachedFlags.fetch_or(initial,std::memory_order_acq_rel);
    gGuard.monitor.setCallback([](uint32_t f){ gGuard.cachedFlags.fetch_or(f,std::memory_order_acq_rel); });
    gGuard.monitor.start();
    gGuard.initialized.store(true,std::memory_order_release);
    LOGI_G("Guard init flags=0x%08X",gGuard.cachedFlags.load());
    return JNI_TRUE;
}

JNIEXPORT jint JNICALL Java_com_omni_backrooms_NativeBridge_getGuardFlags(JNIEnv*, jobject) {
    return static_cast<jint>(gGuard.cachedFlags.load(std::memory_order_acquire));
}

JNIEXPORT jint JNICALL
Java_com_omni_backrooms_NativeBridge_runGuardScan(JNIEnv*, jobject) {
    uint32_t f=gGuard.root.scan()|gGuard.frida.scan()|gGuard.debug.scan()|gGuard.emulator.scan();
    gGuard.cachedFlags.fetch_or(f,std::memory_order_acq_rel);
    return static_cast<jint>(gGuard.cachedFlags.load(std::memory_order_acquire));
}

JNIEXPORT jboolean JNICALL Java_com_omni_backrooms_NativeBridge_isRooted(JNIEnv*, jobject) {
    using namespace omni::shield;
    return (gGuard.cachedFlags.load(std::memory_order_acquire)&(FLAG_ROOT_BINARY|FLAG_ROOT_PROPS|FLAG_ROOT_PATHS|FLAG_MAGISK|FLAG_SHADOW_MOUNT|FLAG_ZYGISK|FLAG_KSU))!=0;
}

JNIEXPORT jboolean JNICALL Java_com_omni_backrooms_NativeBridge_isFridaDetected(JNIEnv*, jobject) {
    using namespace omni::shield;
    return (gGuard.cachedFlags.load(std::memory_order_acquire)&(FLAG_FRIDA_PORT|FLAG_FRIDA_MAPS|FLAG_FRIDA_THREAD|FLAG_FRIDA_GADGET))!=0;
}

JNIEXPORT jboolean JNICALL Java_com_omni_backrooms_NativeBridge_isDebugged(JNIEnv*, jobject) {
    using namespace omni::shield;
    return (gGuard.cachedFlags.load(std::memory_order_acquire)&(FLAG_PTRACE_TRACED|FLAG_DEBUG_WAIT|FLAG_XPOSED|FLAG_SUBSTRATE|FLAG_LSPOSED|FLAG_HOOK_INLINE))!=0;
}

JNIEXPORT jboolean JNICALL Java_com_omni_backrooms_NativeBridge_isEmulator(JNIEnv*, jobject) {
    using namespace omni::shield;
    return (gGuard.cachedFlags.load(std::memory_order_acquire)&(FLAG_EMULATOR_PROPS|FLAG_EMULATOR_HW|FLAG_EMULATOR_CPU))!=0;
}

JNIEXPORT jboolean JNICALL Java_com_omni_backrooms_NativeBridge_isSignatureValid(JNIEnv*, jobject) {
    return (gGuard.cachedFlags.load(std::memory_order_acquire)&omni::shield::FLAG_SIG_MISMATCH)==0;
}

JNIEXPORT jstring JNICALL
Java_com_omni_backrooms_NativeBridge_getThreatReport(JNIEnv* env, jobject) {
    uint32_t f=gGuard.cachedFlags.load(std::memory_order_acquire);
    using namespace omni::shield;
    std::string r;
    auto ap=[&](uint32_t flag,const char* name){ if(f&flag){ if(!r.empty()) r+='|'; r+=name; } };
    ap(FLAG_ROOT_BINARY,"ROOT_BINARY"); ap(FLAG_ROOT_PROPS,"ROOT_PROPS"); ap(FLAG_ROOT_PATHS,"ROOT_PATHS");
    ap(FLAG_SELINUX_OFF,"SELINUX_OFF"); ap(FLAG_MAGISK,"MAGISK"); ap(FLAG_ZYGISK,"ZYGISK");
    ap(FLAG_KSU,"KSU"); ap(FLAG_LSPOSED,"LSPOSED"); ap(FLAG_FRIDA_PORT,"FRIDA_PORT");
    ap(FLAG_FRIDA_MAPS,"FRIDA_MAPS"); ap(FLAG_FRIDA_THREAD,"FRIDA_THREAD"); ap(FLAG_FRIDA_GADGET,"FRIDA_GADGET");
    ap(FLAG_PTRACE_TRACED,"PTRACE"); ap(FLAG_DEBUG_WAIT,"DEBUGWAIT"); ap(FLAG_EMULATOR_PROPS,"EMU_PROPS");
    ap(FLAG_EMULATOR_HW,"EMU_HW"); ap(FLAG_EMULATOR_CPU,"EMU_CPU"); ap(FLAG_SIG_MISMATCH,"SIG_MISMATCH");
    ap(FLAG_XPOSED,"XPOSED"); ap(FLAG_SUBSTRATE,"SUBSTRATE"); ap(FLAG_SHADOW_MOUNT,"SHADOW_MOUNT");
    ap(FLAG_MAPS_TAMPER,"MAPS_TAMPER"); ap(FLAG_HOOK_INLINE,"INLINE_HOOK"); ap(FLAG_PROC_TAMPER,"PROC_TAMPER");
    if(r.empty()) r="CLEAN";
    return env->NewStringUTF(r.c_str());
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_destroyGuard(JNIEnv*, jobject) {
    gGuard.monitor.stop();
    gSigVerifier.reset();
    gGuard.initialized.store(false,std::memory_order_release);
    LOGI_G("Guard destroyed");
}

} // extern "C"
