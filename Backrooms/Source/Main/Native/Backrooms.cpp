#include <jni.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <algorithm>
#include <array>
#include <atomic>
#include <chrono>
#include <cmath>
#include <cstdint>
#include <functional>
#include <memory>
#include <mutex>
#include <numbers>
#include <optional>
#include <queue>
#include <random>
#include <ranges>
#include <span>
#include <thread>
#include <unordered_map>
#include <unordered_set>
#include <vector>

#define TAG_ENT  "OmniEntity"
#define TAG_SND  "OmniSound"
#define LOGI_E(...) __android_log_print(ANDROID_LOG_INFO,  TAG_ENT, __VA_ARGS__)
#define LOGI_S(...) __android_log_print(ANDROID_LOG_INFO,  TAG_SND, __VA_ARGS__)
#define LOGE_S(...) __android_log_print(ANDROID_LOG_ERROR, TAG_SND, __VA_ARGS__)

namespace omni::entity {

[[maybe_unused]] constexpr float kTwoPi = 2.0f * std::numbers::pi_v<float>;

struct Vec2i { int x,y; bool operator==(const Vec2i& o) const noexcept { return x==o.x&&y==o.y; } };
struct Vec2f { float x,y; };
struct Vec3f { float x=0,y=0,z=0; };

struct Vec2iHash {
    size_t operator()(const Vec2i& v) const noexcept {
        return std::hash<int64_t>{}(static_cast<int64_t>(v.x)<<32|static_cast<uint32_t>(v.y));
    }
};

enum class EntityType : uint8_t {
    Smiler=0,HoundDog=1,PartyGoer=2,Skin_Stealer=3,WanderingOne=4,Deathwatch=5,Crawler=6,FacelingDark=7
};

enum class AIState : uint8_t { Idle=0,Wander=1,Alert=2,Chase=3,Attack=4,Flee=5,Stalk=6,Ambush=7 };

struct Blackboard {
    Vec3f lastKnownPlayerPos;
    float timeSincePlayerSeen=999.0f;
    float alertLevel=0.0f;
    bool  playerInSight=false;
    bool  heardNoise=false;
    float noiseLevel=0.0f;
    int   patrolIndex=0;
};

struct Entity {
    Vec3f pos,vel;
    float speed,hearRadius,sightRadius,attackRadius,aggroRadius;
    float wanderAngle,wanderTimer,attackCooldown,hp,maxHp,stalkTimer,ambushTimer,flickerInfluence;
    EntityType type;
    AIState    state;
    Blackboard bb;
    int        id;
    bool       active;
    std::vector<Vec3f> patrolPoints;
};

[[nodiscard]] float dist2d(const Vec3f& a,const Vec3f& b) noexcept { return std::hypot(a.x-b.x,a.z-b.z); }
[[nodiscard]] float dist3d(const Vec3f& a,const Vec3f& b) noexcept {
    float dx=a.x-b.x,dy=a.y-b.y,dz=a.z-b.z; return std::sqrt(dx*dx+dy*dy+dz*dz);
}

class AStarPlanner {
public:
    [[nodiscard]] std::vector<Vec2i> plan(const Vec2i& start,const Vec2i& goal,const std::vector<std::vector<bool>>& grid) const {
        if(goal.y<0||goal.y>=static_cast<int>(grid.size()))    return {};
        if(goal.x<0||goal.x>=static_cast<int>(grid[0].size())) return {};
        if(!grid[goal.y][goal.x]) return {};
        using Node=std::pair<float,Vec2i>;
        auto cmp=[](const Node& a,const Node& b) noexcept { return a.first>b.first; };
        std::priority_queue<Node,std::vector<Node>,decltype(cmp)> open(cmp);
        std::unordered_map<Vec2i,Vec2i,Vec2iHash>  from;
        std::unordered_map<Vec2i,float,Vec2iHash>  g;
        std::unordered_set<Vec2i,Vec2iHash>         closed;
        g[start]=0.0f; open.emplace(heur(start,goal),start);
        constexpr std::array<Vec2i,8> dirs{Vec2i{0,1},{0,-1},{1,0},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}};
        constexpr std::array<float,8> costs{1.0f,1.0f,1.0f,1.0f,1.414f,1.414f,1.414f,1.414f};
        while(!open.empty()){
            auto [_,cur]=open.top(); open.pop();
            if(cur==goal) return reconstruct(from,cur);
            if(closed.contains(cur)) continue;
            closed.insert(cur);
            for(size_t i=0;i<dirs.size();++i){
                Vec2i nb{cur.x+dirs[i].x,cur.y+dirs[i].y};
                if(nb.y<0||nb.y>=static_cast<int>(grid.size()))    continue;
                if(nb.x<0||nb.x>=static_cast<int>(grid[0].size())) continue;
                if(!grid[nb.y][nb.x]||closed.contains(nb)) continue;
                float ng=g[cur]+costs[i];
                if(!g.contains(nb)||ng<g[nb]){ g[nb]=ng; from[nb]=cur; open.emplace(ng+heur(nb,goal),nb); }
            }
        }
        return {};
    }
private:
    [[nodiscard]] static float heur(const Vec2i& a,const Vec2i& b) noexcept {
        float dx=std::abs(a.x-b.x),dy=std::abs(a.y-b.y);
        return std::max(dx,dy)+(std::sqrt(2.0f)-1.0f)*std::min(dx,dy);
    }
    [[nodiscard]] static std::vector<Vec2i> reconstruct(const std::unordered_map<Vec2i,Vec2i,Vec2iHash>& from,Vec2i cur) {
        std::vector<Vec2i> path;
        while(from.contains(cur)){ path.push_back(cur); cur=from.at(cur); }
        std::ranges::reverse(path); return path;
    }
};

class BehaviorTree {
public:
    static void tick(Entity& e,const Vec3f& playerPos,float dt,std::mt19937& rng) {
        updateBlackboard(e,playerPos,dt);
        AIState next=e.state;
        switch(e.type){
            case EntityType::Smiler:       next=tickSmiler(e,dt,rng);      break;
            case EntityType::HoundDog:     next=tickHound(e,dt,rng);       break;
            case EntityType::PartyGoer:    next=tickPartyGoer(e,dt,rng);   break;
            case EntityType::Skin_Stealer: next=tickSkinStealer(e,dt,rng); break;
            case EntityType::WanderingOne: next=tickWanderer(e,dt,rng);    break;
            case EntityType::Deathwatch:   next=tickDeathwatch(e,dt,rng);  break;
            case EntityType::Crawler:      next=tickCrawler(e,dt,rng);     break;
            case EntityType::FacelingDark: next=tickFaceling(e,dt,rng);    break;
        }
        e.state=next;
        executeState(e,playerPos,dt,rng);
    }
private:
    static void updateBlackboard(Entity& e,const Vec3f& pp,float dt) noexcept {
        float d=dist3d(e.pos,pp);
        e.bb.timeSincePlayerSeen+=dt;
        e.bb.playerInSight=(d<e.sightRadius);
        e.bb.heardNoise   =(d<e.hearRadius);
        e.bb.alertLevel=std::clamp(e.bb.alertLevel+(e.bb.playerInSight?2.0f*dt:-0.5f*dt),0.0f,1.0f);
        if(e.bb.playerInSight){ e.bb.lastKnownPlayerPos=pp; e.bb.timeSincePlayerSeen=0.0f; }
    }
    static AIState tickSmiler(Entity& e,float dt,std::mt19937&) noexcept {
        if(e.bb.playerInSight){
            e.flickerInfluence=std::min(1.0f,e.flickerInfluence+dt*0.8f);
            return dist3d(e.pos,e.bb.lastKnownPlayerPos)<e.attackRadius?AIState::Attack:AIState::Stalk;
        }
        e.flickerInfluence=std::max(0.0f,e.flickerInfluence-dt*0.3f);
        return e.bb.alertLevel>0.3f?AIState::Alert:AIState::Idle;
    }
    static AIState tickHound(Entity& e,float,std::mt19937&) noexcept {
        if(e.bb.alertLevel>0.5f) return AIState::Chase;
        if(e.bb.heardNoise)      return AIState::Alert;
        return AIState::Wander;
    }
    static AIState tickPartyGoer(Entity& e,float,std::mt19937& rng) noexcept {
        std::uniform_real_distribution<float> nd(0,1);
        if(e.bb.playerInSight&&nd(rng)<0.01f) return AIState::Attack;
        if(e.bb.playerInSight) return AIState::Chase;
        return AIState::Wander;
    }
    static AIState tickSkinStealer(Entity& e,float,std::mt19937&) noexcept {
        if(e.bb.alertLevel>0.7f) return AIState::Chase;
        if(e.bb.playerInSight)   return AIState::Stalk;
        return e.stalkTimer>0.0f?AIState::Stalk:AIState::Wander;
    }
    static AIState tickWanderer(Entity& e,float,std::mt19937& rng) noexcept {
        std::uniform_real_distribution<float> nd(0,1);
        if(e.bb.playerInSight&&nd(rng)<0.3f) return AIState::Flee;
        return AIState::Wander;
    }
    static AIState tickDeathwatch(Entity& e,float dt,std::mt19937&) noexcept {
        e.ambushTimer-=dt;
        if(e.bb.playerInSight&&e.ambushTimer<=0.0f) return AIState::Ambush;
        if(e.bb.playerInSight) return AIState::Stalk;
        return AIState::Idle;
    }
    static AIState tickCrawler(Entity& e,float,std::mt19937&) noexcept {
        return e.bb.alertLevel>0.4f?AIState::Chase:AIState::Wander;
    }
    static AIState tickFaceling(Entity& e,float,std::mt19937& rng) noexcept {
        std::uniform_real_distribution<float> nd(0,1);
        if(dist3d(e.pos,e.bb.lastKnownPlayerPos)<e.attackRadius) return AIState::Attack;
        if(e.bb.alertLevel>0.6f) return AIState::Chase;
        if(e.bb.playerInSight&&nd(rng)<0.005f) return AIState::Ambush;
        return e.bb.heardNoise?AIState::Alert:AIState::Wander;
    }
    static void executeState(Entity& e,const Vec3f& pp,float dt,std::mt19937& rng) noexcept {
        switch(e.state){
            case AIState::Idle:   break;
            case AIState::Wander: doWander(e,dt,rng);    break;
            case AIState::Alert:  doAlert(e,dt);          break;
            case AIState::Chase:  doChase(e,pp,dt);       break;
            case AIState::Attack: doAttack(e,pp,dt);      break;
            case AIState::Flee:   doFlee(e,pp,dt);        break;
            case AIState::Stalk:  doStalk(e,pp,dt);       break;
            case AIState::Ambush: doAmbush(e,pp,dt,rng);  break;
        }
        e.vel.y-=9.81f*dt;
        if(e.pos.y<=0.0f){ e.pos.y=0.0f; e.vel.y=0.0f; }
        e.pos.x+=e.vel.x*dt; e.pos.y+=e.vel.y*dt; e.pos.z+=e.vel.z*dt;
        e.vel.x*=0.85f; e.vel.z*=0.85f;
        e.attackCooldown=std::max(0.0f,e.attackCooldown-dt);
        e.stalkTimer    =std::max(0.0f,e.stalkTimer-dt);
    }
    static void doWander(Entity& e,float dt,std::mt19937& rng) noexcept {
        std::uniform_real_distribution<float> nd(-1.0f,1.0f);
        e.wanderTimer-=dt;
        if(e.wanderTimer<=0.0f){ e.wanderAngle+=nd(rng)*1.2f; e.wanderTimer=1.5f+nd(rng)*0.5f; }
        float s=e.speed*0.28f;
        e.vel.x=std::cos(e.wanderAngle)*s; e.vel.z=std::sin(e.wanderAngle)*s;
    }
    static void doAlert(Entity& e,float) noexcept {
        float dx=e.bb.lastKnownPlayerPos.x-e.pos.x, dz=e.bb.lastKnownPlayerPos.z-e.pos.z;
        float d=std::hypot(dx,dz); if(d<0.5f) return;
        float s=e.speed*0.5f; e.vel.x=(dx/d)*s; e.vel.z=(dz/d)*s;
    }
    static void doChase(Entity& e,const Vec3f& pp,float) noexcept {
        float dx=pp.x-e.pos.x, dz=pp.z-e.pos.z, d=std::hypot(dx,dz); if(d<0.01f) return;
        e.vel.x=(dx/d)*e.speed; e.vel.z=(dz/d)*e.speed;
    }
    static void doAttack(Entity& e,const Vec3f& pp,float) noexcept {
        if(e.attackCooldown>0.0f) return;
        float dx=pp.x-e.pos.x, dz=pp.z-e.pos.z, d=std::hypot(dx,dz);
        if(d<e.attackRadius){ e.attackCooldown=1.8f; float l=e.speed*3.0f; e.vel.x+=(dx/d)*l; e.vel.z+=(dz/d)*l; }
    }
    static void doFlee(Entity& e,const Vec3f& pp,float) noexcept {
        float dx=e.pos.x-pp.x, dz=e.pos.z-pp.z, d=std::hypot(dx,dz); if(d<0.01f) return;
        e.vel.x=(dx/d)*e.speed*1.4f; e.vel.z=(dz/d)*e.speed*1.4f;
    }
    static void doStalk(Entity& e,const Vec3f& pp,float dt) noexcept {
        float desired=e.sightRadius*0.7f;
        float dx=pp.x-e.pos.x, dz=pp.z-e.pos.z, d=std::hypot(dx,dz); if(d<0.01f) return;
        float err=d-desired, s=std::clamp(err*0.5f,-e.speed*0.6f,e.speed*0.6f);
        e.vel.x+=(dx/d)*s*dt*10.0f; e.vel.z+=(dz/d)*s*dt*10.0f;
        e.stalkTimer=std::max(e.stalkTimer,3.0f);
    }
    static void doAmbush(Entity& e,const Vec3f& pp,float,std::mt19937& rng) noexcept {
        float dx=pp.x-e.pos.x, dz=pp.z-e.pos.z, d=std::hypot(dx,dz); if(d<0.01f) return;
        float burst=e.speed*4.5f; e.vel.x=(dx/d)*burst; e.vel.z=(dz/d)*burst;
        e.ambushTimer=8.0f+std::uniform_real_distribution<float>(0,4)(rng);
    }
};

struct EntitySystem {
    std::vector<Entity> entities;
    AStarPlanner        planner;
    std::mt19937        rng{std::random_device{}()};
    Vec3f               playerPos;
    float               playerNoise=0.0f;
};

} // namespace omni::entity

namespace omni::sound {

constexpr int   kRate   = 44100;
constexpr int   kFrames = 2048;
[[maybe_unused]] constexpr float kTwoPi = 2.0f * std::numbers::pi_v<float>;
[[maybe_unused]] constexpr float kInv32767 = 1.0f/32767.0f;

struct Vec3f { float x,y,z; };
[[nodiscard]] float dot(const Vec3f& a,const Vec3f& b) noexcept { return a.x*b.x+a.y*b.y+a.z*b.z; }
[[nodiscard]] float length(const Vec3f& v) noexcept { return std::sqrt(dot(v,v)); }
[[nodiscard]] Vec3f normalize(const Vec3f& v) noexcept {
    float l=length(v); return l>1e-6f?Vec3f{v.x/l,v.y/l,v.z/l}:Vec3f{0,0,0};
}

struct SpatialParams {
    Vec3f listenerPos{0,0,0}, listenerForward{0,0,-1}, listenerUp{0,1,0};
    float rolloffFactor=1.0f, maxDistance=50.0f, refDistance=1.0f;
};

[[nodiscard]] std::pair<float,float> computePanning(const Vec3f& src,const SpatialParams& sp) noexcept {
    Vec3f delta{src.x-sp.listenerPos.x, src.y-sp.listenerPos.y, src.z-sp.listenerPos.z};
    float dist=length(delta);
    float atten=sp.refDistance/std::max(sp.refDistance,std::min(dist,sp.maxDistance)*sp.rolloffFactor);
    Vec3f right{sp.listenerForward.z,0.0f,-sp.listenerForward.x};
    float pan=dot(normalize(delta),normalize(right));
    return {std::sqrt(0.5f*(1.0f-pan))*atten, std::sqrt(0.5f*(1.0f+pan))*atten};
}

class LowPassFilter {
public:
    explicit LowPassFilter(float cutoffNorm=0.5f) { setCutoff(cutoffNorm); }
    void setCutoff(float c) noexcept { float rc=1.0f/(kTwoPi*c); alpha_=1.0f/(1.0f+rc); }
    float process(float x) noexcept { y_=alpha_*x+(1.0f-alpha_)*y_; return y_; }
    void reset() noexcept { y_=0.0f; }
private:
    float alpha_=0.5f, y_=0.0f;
};

class ReverbTail {
public:
    ReverbTail() { for(auto& b: delays_) b.assign(kMaxDelay,0.0f); }
    float process(float in) noexcept {
        float out=0.0f;
        for(size_t i=0;i<delays_.size();++i){
            size_t& idx=heads_[i]; float delayed=delays_[i][idx];
            out+=delayed*decays_[i]; delays_[i][idx]=in+delayed*feedback_;
            idx=(idx+1)%delays_[i].size();
        }
        return out*0.25f;
    }
    void setRoomSize(float s) noexcept { feedback_=std::clamp(s*0.7f,0.0f,0.95f); }
private:
    static constexpr int kMaxDelay=4096;
    std::array<std::vector<float>,4> delays_;
    std::array<size_t,4> heads_{0,0,0,0};
    std::array<float,4>  decays_{0.85f,0.82f,0.79f,0.76f};
    float feedback_=0.5f;
};

class HumGenerator {
public:
    void fill(std::span<float> out) noexcept {
        for(auto& s: out){
            float h=hum(),b=buzz(),c=crackle(),e=electrical();
            float raw=h*0.5f+b*0.25f+c*0.08f+e*0.17f;
            s=lpf_.process(raw)+reverb_.process(raw*0.3f);
        }
    }
    void setVolume(float v) noexcept { volume_=std::clamp(v,0.0f,1.0f); }
    [[nodiscard]] float volume() const noexcept { return volume_; }
private:
    float hum() noexcept {
        float v=std::sin(hp_)*0.7f+std::sin(hp_*3.0f)*0.15f+std::sin(hp_*5.0f)*0.08f+std::sin(hp_*7.0f)*0.04f+std::sin(hp_*9.0f)*0.03f;
        hp_+=kTwoPi*60.0f/kRate; if(hp_>kTwoPi) hp_-=kTwoPi; return v;
    }
    float buzz() noexcept {
        float v=std::sin(bp_)*0.6f+std::sin(bp_*2.0f)*0.4f;
        float mod=(std::sin(bp_*0.07f)+1.0f)*0.5f, am=1.0f+0.12f*std::sin(bp_*0.003f);
        bp_+=kTwoPi*120.0f/kRate; if(bp_>kTwoPi) bp_-=kTwoPi; return v*mod*am;
    }
    float crackle() noexcept {
        if(++ct_>ci_){ ct_=0; ci_=static_cast<int>(dist_(rng_)*22050+4410); ca_=true; cs_=0; }
        if(!ca_) return 0.0f;
        float env=std::exp(-cs_++*0.004f), n=dist_(rng_)*2.0f-1.0f;
        if(cs_>441) ca_=false; return n*env;
    }
    float electrical() noexcept {
        float v=std::sin(ep_)*(0.4f+0.1f*std::sin(ep_*0.001f));
        ep_+=kTwoPi*180.0f/kRate; if(ep_>kTwoPi) ep_-=kTwoPi;
        return v*(dist_(rng_)>0.97f?1.5f:1.0f);
    }
    float hp_=0,bp_=0,ep_=0,volume_=0.3f;
    int   ct_=0,ci_=8820,cs_=0; bool ca_=false;
    std::mt19937 rng_{42u};
    std::uniform_real_distribution<float> dist_{0,1};
    LowPassFilter lpf_{0.08f};
    ReverbTail    reverb_;
};

struct FootstepSynth {
    std::mt19937 rng{123u};
    std::uniform_real_distribution<float> nd{0,1};
    int   samplePos=0,stepLen=0; bool active=false; float surfaceMix=0.5f;
    void trigger(float bpm,float surface) noexcept {
        stepLen=static_cast<int>(kRate*60.0f/bpm); samplePos=0; active=true;
        surfaceMix=std::clamp(surface,0.0f,1.0f);
    }
    float next() noexcept {
        if(!active) return 0.0f;
        float t=static_cast<float>(samplePos)/stepLen, env=std::exp(-t*18.0f);
        float hit=(nd(rng)*2.0f-1.0f)*env;
        float carpet=hit*(1.0f-surfaceMix)*std::exp(-t*30.0f);
        float hard=hit*surfaceMix*(1.0f+0.5f*std::sin(t*800.0f));
        if(++samplePos>=stepLen) active=false;
        return (carpet+hard)*0.4f;
    }
};

struct MonsterSynth {
    float phase=0,modPhase=0,intensity=0; bool active=false;
    LowPassFilter lpf{0.06f}; ReverbTail rev;
    std::mt19937  rng{77u};
    std::uniform_real_distribution<float> nd{0,1};
    void trigger(float i) noexcept { intensity=std::clamp(i,0.0f,1.0f); active=true; }
    void stop()  noexcept { active=false; }
    float next() noexcept {
        if(!active) return 0.0f;
        float mod=std::sin(modPhase)*intensity*0.4f;
        float raw=std::sin(phase+mod)*0.6f+std::sin(phase*2.3f+mod*1.5f)*0.3f+(nd(rng)*2.0f-1.0f)*0.1f*intensity;
        phase   +=kTwoPi*(55.0f+intensity*40.0f)/kRate; modPhase+=kTwoPi*1.8f/kRate;
        if(phase   >kTwoPi) phase   -=kTwoPi;
        if(modPhase>kTwoPi) modPhase-=kTwoPi;
        return rev.process(lpf.process(raw))*intensity;
    }
};

struct AmbienceLayer {
    float phase1=0,phase2=0,phase3=0,breathPhase=0;
    std::mt19937 rng{55u};
    std::uniform_real_distribution<float> nd{0,1};
    LowPassFilter lpf{0.04f};
    float level=0;
    void setLevel(float l) noexcept { level=std::clamp(l,0.0f,1.0f); }
    float next() noexcept {
        float drone=std::sin(phase1)*0.5f+std::sin(phase2)*0.3f+std::sin(phase3)*0.2f;
        float breath=(std::sin(breathPhase)+1.0f)*0.5f;
        float n=(nd(rng)*2.0f-1.0f)*0.05f;
        phase1+=kTwoPi*38.0f/kRate; phase2+=kTwoPi*57.3f/kRate; phase3+=kTwoPi*76.1f/kRate;
        breathPhase+=kTwoPi*0.12f/kRate;
        if(phase1>kTwoPi) phase1-=kTwoPi; if(phase2>kTwoPi) phase2-=kTwoPi;
        if(phase3>kTwoPi) phase3-=kTwoPi; if(breathPhase>kTwoPi) breathPhase-=kTwoPi;
        return lpf.process(drone*breath+n)*level;
    }
};

struct MixBus {
    std::atomic<float> masterGain{1.0f},humGain{0.3f},footGain{0.8f},monsterGain{0.9f},ambienceGain{0.5f},musicGain{0.7f};
    LowPassFilter masterLpf{0.48f};
    float mix(float hum,float foot,float monster,float ambience) noexcept {
        float s=hum*humGain.load()+foot*footGain.load()+monster*monsterGain.load()+ambience*ambienceGain.load();
        return masterLpf.process(std::tanh(s*masterGain.load()*1.2f));
    }
};

struct SoundEngine {
    SLObjectItf              obj=nullptr;
    SLEngineItf              engine=nullptr;
    SLObjectItf              mixObj=nullptr;
    SLObjectItf              playerObj=nullptr;
    SLPlayItf                player=nullptr;
    SLAndroidSimpleBufferQueueItf queue=nullptr;
    HumGenerator   hum;
    FootstepSynth  foot;
    MonsterSynth   monster;
    AmbienceLayer  ambience;
    MixBus         bus;
    SpatialParams  spatial;
    std::vector<float>   mixBuf;
    std::vector<int16_t> outBuf;
    std::atomic<bool>    running{false};
    std::mutex           mtx;
};

} // namespace omni::sound

static omni::entity::EntitySystem gEntitySys;
static omni::sound::SoundEngine   gSound;

static void onQueue(SLAndroidSimpleBufferQueueItf bq, void*) {
    using namespace omni::sound;
    if(!gSound.running.load()) return;
    std::lock_guard<std::mutex> lock(gSound.mtx);
    auto& ob=gSound.outBuf;
    std::vector<float> humBuf(kFrames),footBuf(kFrames),monBuf(kFrames),ambBuf(kFrames);
    gSound.hum.fill(humBuf);
    for(int i=0;i<kFrames;++i){
        footBuf[i]=gSound.foot.next(); monBuf[i]=gSound.monster.next(); ambBuf[i]=gSound.ambience.next();
        float s=gSound.bus.mix(humBuf[i]*gSound.hum.volume(),footBuf[i],monBuf[i],ambBuf[i]);
        int16_t pcm=static_cast<int16_t>(std::clamp(s*32767.0f,-32767.0f,32767.0f));
        ob[i*2]=pcm; ob[i*2+1]=pcm;
    }
    (*bq)->Enqueue(bq,ob.data(),static_cast<SLuint32>(ob.size()*sizeof(int16_t)));
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_initEntities(JNIEnv*, jobject) {
    gEntitySys.entities.clear();
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
    e.type=static_cast<EntityType>(typeId%8); e.state=AIState::Wander;
    e.hp=e.maxHp=100.0f; e.wanderTimer=1.0f;
    e.ambushTimer=5.0f+std::uniform_real_distribution<float>(0,5)(gEntitySys.rng);
    e.active=true; e.id=id;
    gEntitySys.entities.push_back(std::move(e));
    return id;
}

JNIEXPORT jfloatArray JNICALL
Java_com_omni_backrooms_NativeBridge_tickEntities(
        JNIEnv* env, jobject,
        jfloat px, jfloat py, jfloat pz, jfloat dt) {
    using namespace omni::entity;
    gEntitySys.playerPos={px,py,pz};
    for(auto& e: gEntitySys.entities){ if(!e.active) continue; BehaviorTree::tick(e,gEntitySys.playerPos,dt,gEntitySys.rng); }
    const int fpn=10;
    auto count=static_cast<jsize>(gEntitySys.entities.size()*fpn);
    auto arr=env->NewFloatArray(count); if(!arr) return nullptr;
    std::vector<float> flat; flat.reserve(count);
    for(const auto& e: gEntitySys.entities){
        flat.push_back(e.pos.x); flat.push_back(e.pos.y); flat.push_back(e.pos.z);
        flat.push_back(static_cast<float>(e.state)); flat.push_back(e.bb.alertLevel);
        flat.push_back(e.hp/e.maxHp); flat.push_back(e.flickerInfluence);
        flat.push_back(e.bb.playerInSight?1.0f:0.0f); flat.push_back(static_cast<float>(e.type));
        flat.push_back(e.active?1.0f:0.0f);
    }
    env->SetFloatArrayRegion(arr,0,count,flat.data());
    return arr;
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_damageEntity(JNIEnv*, jobject, jint id, jfloat amount) {
    if(id<0||id>=static_cast<int>(gEntitySys.entities.size())) return;
    auto& e=gEntitySys.entities[id];
    e.hp=std::max(0.0f,e.hp-amount);
    if(e.hp<=0.0f) e.active=false;
}

JNIEXPORT jfloat JNICALL
Java_com_omni_backrooms_NativeBridge_getTotalFlickerInfluence(JNIEnv*, jobject) {
    float total=0.0f;
    for(const auto& e: gEntitySys.entities){
        if(!e.active) continue;
        float d=omni::entity::dist3d(e.pos,gEntitySys.playerPos);
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
    using namespace omni::sound;
    gSound.mixBuf.assign(kFrames,0.0f); gSound.outBuf.assign(kFrames*2,0);
    gSound.ambience.setLevel(0.4f);
    // TODO: migrate to AAudio (NDK ≥29). OpenSL ES deprecated on API 30+.
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
    if(slCreateEngine(&gSound.obj,0,nullptr,0,nullptr,nullptr)!=SL_RESULT_SUCCESS){ LOGE_S("slCreateEngine failed"); return JNI_FALSE; }
#pragma clang diagnostic pop
    (*gSound.obj)->Realize(gSound.obj,SL_BOOLEAN_FALSE);
    (*gSound.obj)->GetInterface(gSound.obj,SL_IID_ENGINE,&gSound.engine);
    // Use nullptr directly — zero-length arrays are a Clang extension (-Wzero-length-array)
    (*gSound.engine)->CreateOutputMix(gSound.engine,&gSound.mixObj,0,nullptr,nullptr);
    (*gSound.mixObj)->Realize(gSound.mixObj,SL_BOOLEAN_FALSE);
    SLDataLocator_AndroidSimpleBufferQueue bqLoc{SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,2};
    SLDataFormat_PCM fmt{SL_DATAFORMAT_PCM,2,SL_SAMPLINGRATE_44_1,SL_PCMSAMPLEFORMAT_FIXED_16,SL_PCMSAMPLEFORMAT_FIXED_16,SL_SPEAKER_FRONT_LEFT|SL_SPEAKER_FRONT_RIGHT,SL_BYTEORDER_LITTLEENDIAN};
    SLDataSource src{&bqLoc,&fmt};
    SLDataLocator_OutputMix outLoc{SL_DATALOCATOR_OUTPUTMIX,gSound.mixObj};
    SLDataSink sink{&outLoc,nullptr};
    const SLInterfaceID ids[]={SL_IID_ANDROIDSIMPLEBUFFERQUEUE}; const SLboolean req[]={SL_BOOLEAN_TRUE};
    (*gSound.engine)->CreateAudioPlayer(gSound.engine,&gSound.playerObj,&src,&sink,1,ids,req);
    (*gSound.playerObj)->Realize(gSound.playerObj,SL_BOOLEAN_FALSE);
    (*gSound.playerObj)->GetInterface(gSound.playerObj,SL_IID_PLAY,&gSound.player);
    (*gSound.playerObj)->GetInterface(gSound.playerObj,SL_IID_ANDROIDSIMPLEBUFFERQUEUE,&gSound.queue);
    (*gSound.queue)->RegisterCallback(gSound.queue,onQueue,nullptr);
    (*gSound.player)->SetPlayState(gSound.player,SL_PLAYSTATE_PLAYING);
    gSound.running.store(true);
    onQueue(gSound.queue,nullptr);
    LOGI_S("Sound init stereo 44100 Hz frames=%d",kFrames);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_setMasterVolume(JNIEnv*, jobject, jfloat v) { gSound.bus.masterGain.store(std::clamp(v,0.0f,1.0f)); }
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_setHumVolume(JNIEnv*, jobject, jfloat v)    { gSound.hum.setVolume(v); gSound.bus.humGain.store(std::clamp(v,0.0f,1.0f)); }
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_setFootstepVolume(JNIEnv*, jobject, jfloat v){ gSound.bus.footGain.store(std::clamp(v,0.0f,1.0f)); }
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_setMonsterVolume(JNIEnv*, jobject, jfloat v) { gSound.bus.monsterGain.store(std::clamp(v,0.0f,1.0f)); }
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_setAmbienceLevel(JNIEnv*, jobject, jfloat v) { gSound.ambience.setLevel(v); }

JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_triggerFootstep(JNIEnv*, jobject, jfloat bpm, jfloat surface) {
    std::lock_guard<std::mutex> lock(gSound.mtx); gSound.foot.trigger(bpm,surface);
}
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_triggerMonster(JNIEnv*, jobject, jfloat intensity) {
    std::lock_guard<std::mutex> lock(gSound.mtx); gSound.monster.trigger(intensity);
}
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_stopMonster(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(gSound.mtx); gSound.monster.stop();
}
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_setListenerPos(JNIEnv*, jobject, jfloat x, jfloat y, jfloat z) {
    std::lock_guard<std::mutex> lock(gSound.mtx); gSound.spatial.listenerPos={x,y,z};
}
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_setSpatialRolloff(JNIEnv*, jobject, jfloat ref, jfloat maxDist) {
    std::lock_guard<std::mutex> lock(gSound.mtx); gSound.spatial.refDistance=ref; gSound.spatial.maxDistance=maxDist;
}
JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_destroySound(JNIEnv*, jobject) {
    gSound.running.store(false);
    std::this_thread::sleep_for(std::chrono::milliseconds(80));
    if(gSound.playerObj){ (*gSound.playerObj)->Destroy(gSound.playerObj); gSound.playerObj=nullptr; }
    if(gSound.mixObj)   { (*gSound.mixObj)->Destroy(gSound.mixObj);       gSound.mixObj   =nullptr; }
    if(gSound.obj)      { (*gSound.obj)->Destroy(gSound.obj);             gSound.obj      =nullptr; }
    LOGI_S("Sound destroyed");
}

} // extern "C"
