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
#include <arpa/inet.h>
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

// Cells per chunk edge. 24 keeps a chunk mesh small enough to build in a frame
// while large enough that streaming happens rarely.
#define OMNI_CHUNK_CELLS 24
#include <linux/prctl.h>
#include <memory>
#include <mutex>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <numbers>
#include <optional>
#include <queue>
#include <random>
#include <ranges>
#include <span>
#include <string>
#include <string_view>
#include <sys/prctl.h>
#include <sys/ptrace.h>
#include <sys/resource.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/system_properties.h>
#include <thread>
#include <unistd.h>
#include <unordered_map>
#include <unordered_set>
#include <vector>

#define TAG_CORE  "OmniCore"
#define TAG_NET   "OmniNet"
#define TAG_GUARD "OmniGuard"
#define TAG_ENT   "OmniEntity"
#define TAG_SND   "OmniSound"

#define LOGI_C(...) __android_log_print(ANDROID_LOG_INFO,  TAG_CORE,  __VA_ARGS__)
#define LOGE_C(...) __android_log_print(ANDROID_LOG_ERROR, TAG_CORE,  __VA_ARGS__)
#define LOGI_N(...) __android_log_print(ANDROID_LOG_INFO,  TAG_NET,   __VA_ARGS__)
#define LOGE_N(...) __android_log_print(ANDROID_LOG_ERROR, TAG_NET,   __VA_ARGS__)
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
};

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
inline void resolveGridCollision(const omni::map::Level0Field& g,PhysicsBody& body,Vec3f prev,bool skipCeiling=false) noexcept {
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
        constexpr float kEye=1.7f, kHead=0.15f;
        float maxY=std::max(omni::map::Level0Field::kHeight-kEye-kHead,0.0f);
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
        float speed=std::hypot(body.vel.x,body.vel.z);
        float targetBob=body.onGround?speed*0.04f:0.0f;
        cam.bobAmount+=(targetBob-cam.bobAmount)*8.0f*dt;
        cam.bobPhase +=speed*2.5f*dt;
        float bobY=std::sin(cam.bobPhase)*cam.bobAmount;
        float bobX=std::sin(cam.bobPhase*0.5f)*cam.bobAmount*0.5f;
        cam.pos={body.pos.x+bobX,body.pos.y+1.7f+bobY,body.pos.z};
        float targetRoll=std::sin(cam.bobPhase*0.5f)*cam.bobAmount*0.8f;
        cam.rollAngle+=(targetRoll-cam.rollAngle)*6.0f*dt;
    }
    void look(CameraState& cam,float dx,float dy,float sensitivity) noexcept {
        // Screen-right is (-cos(yaw),0,sin(yaw)) (gluLookAt side = forward x up),
        // so increasing yaw swings the view LEFT. Drag-right must decrease yaw.
        cam.targetYaw  -=dx*sensitivity;
        cam.targetPitch-=dy*sensitivity;
        cam.targetPitch=std::clamp(cam.targetPitch,-89.0f,89.0f);
    }
};

} // namespace omni::core

namespace omni::net {

constexpr uint32_t kMagic=0x4F4D4E49;
constexpr int kHdrSize=12,kMaxPayload=1400,kMaxRetries=5,kTimeoutMs=8000;

enum class PktType : uint16_t {
    Ping=0x0001,Pong=0x0002,PlayerPos=0x0010,PlayerAnim=0x0011,PlayerHealth=0x0012,
    EntitySync=0x0020,EntitySpawn=0x0021,EntityRemove=0x0022,
    RoomState=0x0030,RoomJoin=0x0031,RoomLeave=0x0032,RoomReady=0x0033,
    VoiceData=0x0040,ChatMsg=0x0050,GameEvent=0x0060,LevelSeed=0x0070,
    Heartbeat=0x00FF,Ack=0x0100,Disconnect=0x01FF
};

struct Header { uint32_t magic; uint16_t type,length; uint32_t sequence; };
struct Packet  { Header header; std::vector<uint8_t> payload; sockaddr_in from; int64_t timestampMs; };
struct PeerInfo{ sockaddr_in addr; int64_t lastSeen; int ping; uint32_t lastSeq; bool connected; std::string roomId; };

[[nodiscard]] int64_t nowMs() noexcept {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now().time_since_epoch()).count();
}
[[nodiscard]] std::vector<uint8_t> buildPacket(PktType type,const void* payload,uint16_t payloadLen,uint32_t seq) {
    std::vector<uint8_t> pkt(kHdrSize+payloadLen);
    Header hdr{kMagic,static_cast<uint16_t>(type),payloadLen,seq};
    std::memcpy(pkt.data(),&hdr,kHdrSize);
    if(payload&&payloadLen>0) std::memcpy(pkt.data()+kHdrSize,payload,payloadLen);
    return pkt;
}
[[nodiscard]] std::optional<Header> parseHeader(const uint8_t* data,int len) noexcept {
    if(len<kHdrSize) return std::nullopt;
    Header hdr; std::memcpy(&hdr,data,kHdrSize);
    return hdr.magic==kMagic?std::optional<Header>{hdr}:std::nullopt;
}

class RttEstimator {
public:
    void addSample(int64_t rttMs) noexcept {
        float s=static_cast<float>(rttMs);
        srtt_  =(srtt_  ==0)?s:srtt_*0.875f+s*0.125f;
        rttvar_=(rttvar_==0)?s*0.5f:rttvar_*0.75f+std::abs(s-srtt_)*0.25f;
        rto_=std::clamp(srtt_+4.0f*rttvar_,100.0f,3000.0f);
    }
    [[nodiscard]] float rto()  const noexcept { return rto_; }
    [[nodiscard]] int   ping() const noexcept { return static_cast<int>(srtt_); }
private:
    float srtt_=0,rttvar_=0,rto_=200.0f;
};

class ReliableChannel {
public:
    struct PendingPacket { std::vector<uint8_t> data; int64_t sentAt; int retries; uint32_t seq; };
    [[nodiscard]] uint32_t nextSeq() noexcept { return seq_++; }
    void onAck(uint32_t seq,int64_t now) noexcept {
        auto it=pending_.find(seq); if(it==pending_.end()) return;
        rtt_.addSample(now-it->second.sentAt); pending_.erase(it);
    }
    void track(uint32_t seq,const std::vector<uint8_t>& pkt,int64_t sentAt) { pending_[seq]={pkt,sentAt,0,seq}; }
    [[nodiscard]] std::vector<PendingPacket*> getRetransmits(int64_t now) {
        std::vector<PendingPacket*> out;
        for(auto& [s,p]: pending_)
            if(now-p.sentAt>static_cast<int64_t>(rtt_.rto())&&p.retries<kMaxRetries){ p.retries++; p.sentAt=now; out.push_back(&p); }
        return out;
    }
    [[nodiscard]] int ping() const noexcept { return rtt_.ping(); }
private:
    std::unordered_map<uint32_t,PendingPacket> pending_;
    RttEstimator rtt_; uint32_t seq_=1;
};

class JitterBuffer {
public:
    explicit JitterBuffer(int td=80): targetDelay_(td) {}
    void push(Packet pkt) { std::lock_guard lk(mtx_); buf_.push(std::move(pkt)); }
    [[nodiscard]] std::optional<Packet> pop(int64_t now) {
        std::lock_guard lk(mtx_);
        if(buf_.empty()||now-buf_.front().timestampMs<targetDelay_) return std::nullopt;
        Packet pkt=std::move(buf_.front()); buf_.pop(); return pkt;
    }
    [[nodiscard]] int depth() const { std::lock_guard lk(mtx_); return static_cast<int>(buf_.size()); }
private:
    mutable std::mutex mtx_; std::queue<Packet> buf_; int targetDelay_;
};

class UdpSocket {
public:
    bool open() noexcept {
        fd_=socket(AF_INET,SOCK_DGRAM,IPPROTO_UDP); if(fd_<0) return false;
        int opt=1; setsockopt(fd_,SOL_SOCKET,SO_REUSEADDR,&opt,sizeof(opt));
        int s=256*1024; setsockopt(fd_,SOL_SOCKET,SO_SNDBUF,&s,sizeof(s)); setsockopt(fd_,SOL_SOCKET,SO_RCVBUF,&s,sizeof(s));
        fcntl(fd_,F_SETFL,O_NONBLOCK); return true;
    }
    bool bind(uint16_t port) noexcept {
        sockaddr_in a{}; a.sin_family=AF_INET; a.sin_addr.s_addr=INADDR_ANY; a.sin_port=htons(port);
        return ::bind(fd_,reinterpret_cast<sockaddr*>(&a),sizeof(a))==0;
    }
    int sendTo(const void* d,int n,const sockaddr_in& dst) noexcept {
        return static_cast<int>(sendto(fd_,d,n,0,reinterpret_cast<const sockaddr*>(&dst),sizeof(dst)));
    }
    int recvFrom(void* buf,int maxN,sockaddr_in& from) noexcept {
        socklen_t fl=sizeof(from);
        return static_cast<int>(recvfrom(fd_,buf,maxN,0,reinterpret_cast<sockaddr*>(&from),&fl));
    }
    void close() noexcept { if(fd_>=0){ ::close(fd_); fd_=-1; } }
    [[nodiscard]] bool isOpen() const noexcept { return fd_>=0; }
private:
    int fd_=-1;
};

struct VoicePacket { uint32_t peerId; uint16_t seqNum; std::vector<uint8_t> pcmData; };

class VoiceMultiplexer {
public:
    void push(uint32_t peerId,const uint8_t* data,int len,uint16_t seq) {
        std::lock_guard lk(mtx_);
        auto& jb=jb_[peerId]; if(!jb) jb=std::make_unique<JitterBuffer>(60);
        Packet p; p.payload.assign(data,data+len); p.timestampMs=nowMs()-seq; jb->push(std::move(p));
    }
    [[nodiscard]] std::optional<VoicePacket> pop(uint32_t peerId) {
        std::lock_guard lk(mtx_);
        auto it=jb_.find(peerId); if(it==jb_.end()) return std::nullopt;
        auto pkt=it->second->pop(nowMs()); if(!pkt) return std::nullopt;
        return VoicePacket{peerId,0,std::move(pkt->payload)};
    }
private:
    mutable std::mutex mtx_;
    std::unordered_map<uint32_t,std::unique_ptr<JitterBuffer>> jb_;
};

struct NetState {
    UdpSocket                             sock;
    ReliableChannel                       reliable;
    VoiceMultiplexer                      voice;
    std::unordered_map<uint32_t,PeerInfo> peers;
    std::queue<Packet>                    recvQueue;
    std::mutex                            recvMtx;
    std::atomic<bool>                     running{false};
    std::thread                           recvThread;
    std::atomic<int>                      localPing{0};
    std::atomic<uint32_t>                 localId{0};
    std::string                           currentRoom;
    uint8_t                               recvBuf[kHdrSize+kMaxPayload];
};

} // namespace omni::net

namespace omni::guard {

constexpr uint32_t
    FLAG_ROOT_BINARY=1u<<0,  FLAG_ROOT_PROPS=1u<<1,  FLAG_ROOT_PATHS=1u<<2,
    FLAG_SELINUX_OFF=1u<<3,  FLAG_MAGISK=1u<<4,      FLAG_FRIDA_PORT=1u<<5,
    FLAG_FRIDA_MAPS=1u<<6,   FLAG_FRIDA_THREAD=1u<<7,FLAG_FRIDA_GADGET=1u<<8,
    FLAG_PTRACE_TRACED=1u<<9,FLAG_DEBUG_WAIT=1u<<10, FLAG_EMULATOR_PROPS=1u<<11,
    FLAG_EMULATOR_HW=1u<<12, FLAG_EMULATOR_CPU=1u<<13,FLAG_SIG_MISMATCH=1u<<14,
    FLAG_MAPS_TAMPER=1u<<15, FLAG_XPOSED=1u<<16,     FLAG_SUBSTRATE=1u<<17,
    FLAG_SHADOW_MOUNT=1u<<18,FLAG_PROC_TAMPER=1u<<19,FLAG_HOOK_INLINE=1u<<20,
    FLAG_ZYGISK=1u<<21,      FLAG_LSPOSED=1u<<22,    FLAG_KSU=1u<<23;

[[nodiscard]] static bool fileExists(std::string_view p) noexcept { struct stat st{}; return ::stat(p.data(),&st)==0; }
[[nodiscard]] static std::string readSmallFile(std::string_view p) noexcept {
    int fd=::open(p.data(),O_RDONLY|O_CLOEXEC); if(fd<0) return {};
    char buf[8192]{}; ssize_t n=::read(fd,buf,sizeof(buf)-1); ::close(fd);
    return n>0?std::string(buf,static_cast<size_t>(n)):std::string{};
}
[[nodiscard]] static std::string getSysPropAsync(const char* key) noexcept {
    char val[PROP_VALUE_MAX]{};
    auto propInfo=__system_property_find(key);
    if(propInfo){
        __system_property_read_callback(propInfo,[](void* cookie,const char*,const char* value,uint32_t){
            auto* out=static_cast<char*>(cookie);
            std::strncpy(out,value,PROP_VALUE_MAX-1);
        },val);
    }
    return std::string(val);
}
[[nodiscard]] static bool containsCI(std::string_view hay,std::string_view needle) noexcept {
    if(needle.size()>hay.size()) return false;
    return std::search(hay.begin(),hay.end(),needle.begin(),needle.end(),
        [](char a,char b){ return std::tolower((unsigned char)a)==std::tolower((unsigned char)b); })!=hay.end();
}
[[nodiscard]] static bool portOpen(uint16_t port) noexcept {
    int fd=::socket(AF_INET,SOCK_STREAM|SOCK_CLOEXEC,0); if(fd<0) return false;
    struct timeval tv{0,80'000};
    ::setsockopt(fd,SOL_SOCKET,SO_RCVTIMEO,&tv,sizeof(tv));
    ::setsockopt(fd,SOL_SOCKET,SO_SNDTIMEO,&tv,sizeof(tv));
    sockaddr_in addr{}; addr.sin_family=AF_INET;
    addr.sin_port=htons(port); addr.sin_addr.s_addr=htonl(INADDR_LOOPBACK);
    bool ok=(::connect(fd,reinterpret_cast<sockaddr*>(&addr),sizeof(addr))==0);
    ::close(fd); return ok;
}

static std::string sha256Hex(const uint8_t* data,size_t len) noexcept {
    static constexpr uint32_t K[64]={
        0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
        0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
        0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
        0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
        0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
        0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
        0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
        0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2
    };
    uint32_t h[8]={0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,0x510e527f,0x9b05688c,0x1f83d9ab,0x5be0cd19};
    auto ror32=[](uint32_t x,int n)->uint32_t{ return (x>>n)|(x<<(32-n)); };
    auto processBlock=[&](const uint8_t* blk){
        uint32_t w[64];
        for(int i=0;i<16;++i) w[i]=((uint32_t)blk[i*4]<<24)|((uint32_t)blk[i*4+1]<<16)|((uint32_t)blk[i*4+2]<<8)|(uint32_t)blk[i*4+3];
        for(int i=16;i<64;++i){ uint32_t s0=ror32(w[i-15],7)^ror32(w[i-15],18)^(w[i-15]>>3); uint32_t s1=ror32(w[i-2],17)^ror32(w[i-2],19)^(w[i-2]>>10); w[i]=w[i-16]+s0+w[i-7]+s1; }
        uint32_t a=h[0],b=h[1],c=h[2],d=h[3],e=h[4],f=h[5],g=h[6],hh=h[7];
        for(int i=0;i<64;++i){
            uint32_t S1=ror32(e,6)^ror32(e,11)^ror32(e,25),ch=(e&f)^((~e)&g),t1=hh+S1+ch+K[i]+w[i];
            uint32_t S0=ror32(a,2)^ror32(a,13)^ror32(a,22),maj=(a&b)^(a&c)^(b&c),t2=S0+maj;
            hh=g;g=f;f=e;e=d+t1;d=c;c=b;b=a;a=t1+t2;
        }
        h[0]+=a;h[1]+=b;h[2]+=c;h[3]+=d;h[4]+=e;h[5]+=f;h[6]+=g;h[7]+=hh;
    };
    size_t tb=(len+8)/64+1;
    std::vector<uint8_t> padded(tb*64,0);
    std::memcpy(padded.data(),data,len); padded[len]=0x80;
    uint64_t bl=(uint64_t)len*8;
    for(int i=0;i<8;++i) padded[padded.size()-8+i]=(uint8_t)(bl>>(56-i*8));
    for(size_t i=0;i<tb;++i) processBlock(padded.data()+i*64);
    char hex[65]; for(int i=0;i<8;++i) snprintf(hex+i*8,9,"%08x",h[i]);
    return std::string(hex,64);
}

class RootDetector {
public:
    [[nodiscard]] uint32_t scan() noexcept {
        uint32_t f=0;
        if(rootBinaries())   f|=FLAG_ROOT_BINARY;
        if(rootProperties()) f|=FLAG_ROOT_PROPS;
        if(rootPaths())      f|=FLAG_ROOT_PATHS;
        if(selinuxOff())     f|=FLAG_SELINUX_OFF;
        if(magisk())         f|=FLAG_MAGISK;
        if(shadowMount())    f|=FLAG_SHADOW_MOUNT;
        if(ksu())            f|=FLAG_KSU;
        if(zygisk())         f|=FLAG_ZYGISK;
        return f;
    }
private:
    [[nodiscard]] bool rootBinaries() noexcept {
        static constexpr std::string_view bins[]={
            "/sbin/su","/system/bin/su","/system/xbin/su","/system/sbin/su","/vendor/bin/su",
            "/su/bin/su","/data/local/su","/data/local/bin/su","/data/local/xbin/su",
            "/system/bin/.ext/.su","/system/xbin/busybox","/system/bin/busybox",
            "/data/adb/magisk","/sbin/.magisk","/sbin/.core/mirror"
        };
        for(auto b: bins) if(fileExists(b)) return true;
        return false;
    }
    [[nodiscard]] bool rootProperties() noexcept {
        if(getSysPropAsync("ro.debuggable")=="1") return true;
        if(getSysPropAsync("ro.secure")=="0")     return true;
        if(containsCI(getSysPropAsync("ro.build.tags"),"test-keys"))  return true;
        if(containsCI(getSysPropAsync("ro.build.type"),"userdebug"))  return true;
        return false;
    }
    [[nodiscard]] bool rootPaths() noexcept {
        static constexpr std::string_view paths[]={
            "/system/app/SuperSU.apk","/system/app/Superuser.apk","/system/app/KingoUser.apk",
            "/data/data/com.topjohnwu.magisk","/data/data/eu.chainfire.supersu","/data/data/me.weishu.kernelsu"
        };
        for(auto p: paths) if(fileExists(p)) return true;
        return false;
    }
    [[nodiscard]] bool selinuxOff() noexcept { auto c=readSmallFile("/sys/fs/selinux/enforce"); return !c.empty()&&c[0]=='0'; }
    [[nodiscard]] bool magisk() noexcept {
        static constexpr std::string_view mp[]={"/sbin/.magisk","/dev/.magisk","/data/adb/magisk","/data/adb/magisk.img","/sbin/magisk","/dev/magisk"};
        for(auto p: mp) if(fileExists(p)) return true;
        return containsCI(readSmallFile("/proc/self/maps"),"magisk");
    }
    [[nodiscard]] bool ksu() noexcept { return fileExists("/data/adb/ksu")||fileExists("/data/adb/ksud")||fileExists("/data/adb/modules/.ksu"); }
    [[nodiscard]] bool zygisk() noexcept {
        auto m=readSmallFile("/proc/self/maps");
        return containsCI(m,"zygisk")||containsCI(m,"riru")||fileExists("/data/adb/modules/.zygisk");
    }
    [[nodiscard]] bool shadowMount() noexcept {
        auto m=readSmallFile("/proc/self/mounts");
        if(containsCI(m,"magisk")||containsCI(m,"supersu")) return true;
        return containsCI(m,"overlay")&&containsCI(m,"/system");
    }
};

class FridaDetector {
public:
    [[nodiscard]] uint32_t scan() noexcept {
        uint32_t f=0;
        if(fridaPort())   f|=FLAG_FRIDA_PORT;
        if(fridaMaps())   f|=FLAG_FRIDA_MAPS;
        if(fridaThread()) f|=FLAG_FRIDA_THREAD;
        if(fridaGadget()) f|=FLAG_FRIDA_GADGET;
        return f;
    }
private:
    [[nodiscard]] bool fridaPort() noexcept {
        static constexpr uint16_t ports[]={27042,27043,27044,27045};
        for(auto p: ports) if(portOpen(p)) return true;
        auto tcp=readSmallFile("/proc/net/tcp"),tcp6=readSmallFile("/proc/net/tcp6");
        for(auto h: {"6D58","71D4","2717","5039"}) if(containsCI(tcp,h)||containsCI(tcp6,h)) return true;
        return false;
    }
    [[nodiscard]] bool fridaMaps() noexcept {
        auto m=readSmallFile("/proc/self/maps");
        for(auto p: {"frida","gum-js-loop","frida-agent","frida-gadget","frida-server","linjector","re.frida.server","frida-helper"})
            if(containsCI(m,p)) return true;
        return false;
    }
    [[nodiscard]] bool fridaThread() noexcept {
        DIR* dir=opendir("/proc/self/task"); if(!dir) return false;
        bool found=false; struct dirent* e;
        while((e=readdir(dir))!=nullptr){
            if(e->d_name[0]=='.') continue;
            std::string path="/proc/self/task/"; path+=e->d_name; path+="/comm";
            auto comm=readSmallFile(path);
            if(containsCI(comm,"gum-js-loop")||containsCI(comm,"frida")||containsCI(comm,"gmain")){ found=true; break; }
        }
        closedir(dir); return found;
    }
    [[nodiscard]] bool fridaGadget() noexcept {
        for(auto lib: {"libfrida-gadget.so","re.frida.server","libgadget.so"}){
            void* h=dlopen(lib,RTLD_NOLOAD); if(h){ dlclose(h); return true; }
        }
        return false;
    }
};

class DebugDetector {
public:
    [[nodiscard]] uint32_t scan() noexcept {
        uint32_t f=0;
        if(ptrace_check()) f|=FLAG_PTRACE_TRACED;
        if(debugWait())    f|=FLAG_DEBUG_WAIT;
        if(xposed())       f|=FLAG_XPOSED;
        if(lsposed())      f|=FLAG_LSPOSED;
        if(substrate())    f|=FLAG_SUBSTRATE;
        if(procStatus())   f|=FLAG_PROC_TAMPER;
        if(inlineHook())   f|=FLAG_HOOK_INLINE;
        return f;
    }
private:
    [[nodiscard]] bool ptrace_check() noexcept {
        if(ptrace(PTRACE_TRACEME,0,nullptr,nullptr)==-1) return true;
        ptrace(PTRACE_DETACH,0,nullptr,nullptr); return false;
    }
    [[nodiscard]] bool debugWait() noexcept {
        auto s=readSmallFile("/proc/self/status");
        auto pos=s.find("TracerPid:"); if(pos==std::string::npos) return false;
        std::string_view sv(s); sv=sv.substr(pos+10);
        while(!sv.empty()&&(sv[0]==' '||sv[0]=='\t')) sv.remove_prefix(1);
        return !sv.empty()&&sv[0]!='0';
    }
    [[nodiscard]] bool xposed() noexcept { return containsCI(readSmallFile("/proc/self/maps"),"XposedBridge")||fileExists("/system/framework/XposedBridge.jar"); }
    [[nodiscard]] bool lsposed() noexcept {
        return containsCI(readSmallFile("/proc/self/maps"),"lsposed")||
               fileExists("/data/data/org.lsposed.manager")||fileExists("/data/data/io.github.lsposed.manager");
    }
    [[nodiscard]] bool substrate() noexcept {
        for(auto lib: {"libsubstrate.so","libsubstrate-dvm.so","libCydiaSubstrate.so"}){
            void* h=dlopen(lib,RTLD_NOLOAD); if(h){ dlclose(h); return true; }
        }
        return containsCI(readSmallFile("/proc/self/maps"),"substrate");
    }
    [[nodiscard]] bool procStatus() noexcept { auto s=readSmallFile("/proc/self/status"); return s.empty()||!containsCI(s,"Name:"); }
    [[nodiscard]] bool inlineHook() noexcept {
        for(auto lib: {"libdobby.so","libsandHook.so","libwhale.so","libAndHook.so","libepic.so","libreactivehole.so"}){
            void* h=dlopen(lib,RTLD_NOLOAD); if(h){ dlclose(h); return true; }
        }
        return false;
    }
};

class EmulatorDetector {
public:
    [[nodiscard]] uint32_t scan() noexcept {
        uint32_t f=0;
        if(emuProps()) f|=FLAG_EMULATOR_PROPS;
        if(emuHw())    f|=FLAG_EMULATOR_HW;
        if(cpuInfo())  f|=FLAG_EMULATOR_CPU;
        return f;
    }
private:
    [[nodiscard]] bool emuProps() noexcept {
        static constexpr const char* props[][2]={
            {"ro.hardware","goldfish"},{"ro.hardware","ranchu"},{"ro.product.model","sdk"},
            {"ro.product.device","generic"},{"ro.kernel.qemu","1"},
            {"ro.product.manufacturer","unknown"},{"ro.build.product","generic"}
        };
        for(auto& [k,v]: props) if(containsCI(getSysPropAsync(k),v)) return true;
        return false;
    }
    [[nodiscard]] bool emuHw() noexcept { for(auto f: {"/dev/socket/qemud","/dev/qemu_pipe","/sys/qemu_trace"}) if(fileExists(f)) return true; return false; }
    [[nodiscard]] bool cpuInfo() noexcept { auto c=readSmallFile("/proc/cpuinfo"); return containsCI(c,"goldfish")||containsCI(c,"ranchu"); }
};

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

class AntiTamperMonitor {
public:
    void start() {
        if(running_.load(std::memory_order_acquire)) return;
        running_.store(true,std::memory_order_release);
        thread_=std::thread(&AntiTamperMonitor::loop,this);
    }
    void stop() {
        running_.store(false,std::memory_order_release);
        if(thread_.joinable()) thread_.join();
    }
    void setCallback(std::function<void(uint32_t)> cb) { std::lock_guard lk(mtx_); cb_=std::move(cb); }
private:
    void loop() {
        prctl(PR_SET_NAME,"omni_guard_wt",0,0,0);
        setpriority(PRIO_PROCESS,0,10);
        RootDetector root; FridaDetector frida; DebugDetector debug; EmulatorDetector emu;
        int cycle=0;
        while(running_.load(std::memory_order_acquire)){
            uint32_t detected=0;
            detected|=frida.scan();
            if(cycle%2==0)  detected|=debug.scan();
            if(cycle%5==0)  detected|=root.scan();
            if(cycle%15==0) detected|=emu.scan();
            if(detected!=0){
                uint32_t prev=flags_.fetch_or(detected,std::memory_order_acq_rel);
                if((prev|detected)!=prev){ std::lock_guard lk(mtx_); if(cb_) cb_(detected); }
            }
            ++cycle;
            std::this_thread::sleep_for(std::chrono::milliseconds(1200));
        }
    }
    std::atomic<bool>     running_{false};
    std::atomic<uint32_t> flags_  {0};
    std::thread           thread_;
    std::mutex            mtx_;
    std::function<void(uint32_t)> cb_;
};

struct GuardState {
    RootDetector                       root;
    FridaDetector                      frida;
    DebugDetector                      debug;
    EmulatorDetector                   emulator;
    AntiTamperMonitor                  monitor;
    std::unique_ptr<SignatureVerifier> sigVerifier;
    std::atomic<uint32_t>              cachedFlags{0};
    std::atomic<bool>                  initialized{false};
};

} // namespace omni::guard

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
        if(e.bb.playerInSight&&nd(rng)<0.003f) return AIState::Attack;
        return AIState::Wander;
    }
    static AIState tickDeathwatch(Entity& e,float dt,std::mt19937&) noexcept {
        e.ambushTimer-=dt;
        if(e.ambushTimer<=0&&e.bb.playerInSight) return AIState::Attack;
        return e.bb.playerInSight?AIState::Ambush:AIState::Stalk;
    }
    static AIState tickCrawler(Entity& e,float,std::mt19937&) noexcept {
        if(e.bb.alertLevel>0.6f) return AIState::Chase;
        if(e.bb.heardNoise)      return AIState::Alert;
        return AIState::Wander;
    }
    static AIState tickFaceling(Entity& e,float,std::mt19937&) noexcept {
        if(e.bb.playerInSight) return e.bb.alertLevel>0.8f?AIState::Chase:AIState::Stalk;
        return e.bb.alertLevel>0.4f?AIState::Alert:AIState::Idle;
    }
    static void executeState(Entity& e,const Vec3f& target,float dt,std::mt19937& rng) noexcept {
        switch(e.state){
            case AIState::Wander:  doWander(e,dt,rng); break;
            case AIState::Chase:
            case AIState::Stalk:   moveToward(e,target,dt,e.state==AIState::Stalk?0.55f:1.0f); break;
            case AIState::Attack:  doAttack(e,target,dt); break;
            case AIState::Flee:    fleFrom(e,target,dt); break;
            default: break;
        }
    }
    static void doWander(Entity& e,float dt,std::mt19937& rng) noexcept {
        e.wanderTimer-=dt;
        if(e.wanderTimer<=0){
            std::uniform_real_distribution<float> ad(-1.0f,1.0f);
            e.wanderAngle+=ad(rng)*1.2f;
            e.wanderTimer=1.0f+std::uniform_real_distribution<float>(0,2)(rng);
        }
        e.vel.x=std::sin(e.wanderAngle)*e.speed*0.4f;
        e.vel.z=std::cos(e.wanderAngle)*e.speed*0.4f;
        e.pos.x+=e.vel.x*dt; e.pos.z+=e.vel.z*dt;
    }
    static void moveToward(Entity& e,const Vec3f& t,float dt,float speedMult) noexcept {
        float dx=t.x-e.pos.x,dz=t.z-e.pos.z;
        float d=std::hypot(dx,dz); if(d<0.1f) return;
        e.pos.x+=dx/d*e.speed*speedMult*dt;
        e.pos.z+=dz/d*e.speed*speedMult*dt;
    }
    static void doAttack(Entity& e,const Vec3f& t,float dt) noexcept {
        if(e.attackCooldown>0){ e.attackCooldown-=dt; return; }
        moveToward(e,t,dt,1.0f); e.attackCooldown=0.8f;
    }
    static void fleFrom(Entity& e,const Vec3f& t,float dt) noexcept {
        float dx=e.pos.x-t.x,dz=e.pos.z-t.z;
        float d=std::hypot(dx,dz); if(d<0.1f) return;
        e.pos.x+=dx/d*e.speed*1.2f*dt;
        e.pos.z+=dz/d*e.speed*1.2f*dt;
    }
};

struct EntitySystem {
    std::vector<Entity> entities;
    Vec3f               playerPos;
    std::mt19937        rng{12345};
};

} // namespace omni::entity

namespace omni::sound {

constexpr int kSampleRate = 44100;
constexpr int kFrames     = 256;

struct SpatialParams {
    omni::entity::Vec3f listenerPos;
    float refDistance=1.0f,maxDistance=30.0f;
};

class HumGenerator {
public:
    void fill(std::vector<float>& buf) noexcept {
        for(auto& s: buf){
            s=(std::sin(phase_)*0.35f+std::sin(phase_*3.0f)*0.08f+std::sin(phase_*5.0f)*0.04f)*vol_.load();
            phase_+=2.0f*std::numbers::pi_v<float>*60.0f/kSampleRate;
        }
    }
    void setVolume(float v) noexcept { vol_.store(std::clamp(v,0.0f,1.0f)); }
    float volume() const noexcept    { return vol_.load(); }
private:
    float phase_=0.0f;
    std::atomic<float> vol_{0.6f};
};

class FootstepSynth {
public:
    void trigger(float bpm,float surface) noexcept {
        std::lock_guard lk(mtx_);
        interval_=static_cast<int>(60.0f/bpm*kSampleRate);
        surface_=surface; counter_=0; active_=true;
    }
    float next() noexcept {
        std::lock_guard lk(mtx_);
        if(!active_) return 0.0f;
        if(counter_>=interval_){ counter_=0; }
        float t=static_cast<float>(counter_)/kSampleRate;
        float env=std::exp(-t*30.0f*(1.0f+surface_*2.0f));
        float click=std::sin(t*800.0f*(1.0f-surface_*0.5f))*env*0.6f;
        ++counter_; return click;
    }
private:
    mutable std::mutex mtx_;
    int counter_=0,interval_=22050; float surface_=0; bool active_=false;
};

class MonsterSynth {
public:
    void trigger(float intensity) noexcept { std::lock_guard lk(mtx_); intensity_=std::clamp(intensity,0.0f,1.0f); active_=true; phase_=0; }
    void stop()                   noexcept { std::lock_guard lk(mtx_); active_=false; }
    float next() noexcept {
        std::lock_guard lk(mtx_);
        if(!active_) return 0.0f;
        float f=80.0f+intensity_*120.0f+std::sin(phase_*0.01f)*20.0f;
        float s=std::sin(phase_*2.0f*std::numbers::pi_v<float>*f/kSampleRate);
        float rumble=std::sin(phase_*2.0f*std::numbers::pi_v<float>*40.0f/kSampleRate)*0.3f;
        float env=std::min(1.0f,static_cast<float>(phase_)/kSampleRate)*intensity_;
        ++phase_;
        return (s+rumble)*env*0.5f;
    }
private:
    mutable std::mutex mtx_; int phase_=0; float intensity_=0; bool active_=false;
};

class AmbienceLayer {
public:
    void setLevel(float l) noexcept { level_.store(std::clamp(l,0.0f,1.0f)); }
    float next() noexcept {
        float n=dist_(rng_)*level_.load()*0.08f;
        float hum=std::sin(humPhase_)*0.04f*level_.load();
        humPhase_+=2.0f*std::numbers::pi_v<float>*50.0f/kSampleRate;
        return n+hum;
    }
private:
    std::mt19937 rng_{9999};
    std::uniform_real_distribution<float> dist_{-1.0f,1.0f};
    std::atomic<float> level_{0.4f};
    float humPhase_=0;
};

class MixBus {
public:
    std::atomic<float> masterGain{0.9f},humGain{0.6f},footGain{0.8f},monsterGain{0.9f};
    float mix(float hum,float foot,float monster,float amb) const noexcept {
        float out=hum*humGain.load()+foot*footGain.load()+monster*monsterGain.load()+amb*0.5f;
        return std::clamp(out*masterGain.load(),-1.0f,1.0f);
    }
};

struct SoundEngine {
    AAudioStreamBuilder* builder=nullptr;
    AAudioStream*        stream=nullptr;
    HumGenerator   hum;
    FootstepSynth  foot;
    MonsterSynth   monster;
    AmbienceLayer  ambience;
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
static omni::net::NetState           gNet;
static omni::guard::GuardState       gGuard;
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
        footBuf[i]=eng->foot.next(); monBuf[i]=eng->monster.next(); ambBuf[i]=eng->ambience.next();
        float s=eng->bus.mix(humBuf[i]*eng->hum.volume(),footBuf[i],monBuf[i],ambBuf[i]);
        int16_t pcm=static_cast<int16_t>(std::clamp(s*32767.0f,-32767.0f,32767.0f));
        out[i*2]=pcm; out[i*2+1]=pcm;
    }
    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

static void recvLoop() {
    using namespace omni::net;
    setpriority(PRIO_PROCESS,0,10);
    while(gNet.running.load()){
        sockaddr_in from{};
        int n=gNet.sock.recvFrom(gNet.recvBuf,sizeof(gNet.recvBuf),from);
        if(n<=0){ std::this_thread::sleep_for(std::chrono::milliseconds(1)); continue; }
        auto hdrOpt=parseHeader(gNet.recvBuf,n); if(!hdrOpt) continue;
        auto& hdr=*hdrOpt;
        Packet pkt; pkt.header=hdr; pkt.from=from; pkt.timestampMs=nowMs();
        if(hdr.length>0&&n>=kHdrSize+hdr.length)
            pkt.payload.assign(gNet.recvBuf+kHdrSize,gNet.recvBuf+kHdrSize+hdr.length);
        if(static_cast<PktType>(hdr.type)==PktType::Pong){
            int64_t rtt=nowMs()-(pkt.payload.size()>=8?*reinterpret_cast<int64_t*>(pkt.payload.data()):0);
            gNet.localPing.store(static_cast<int>(rtt));
            gNet.reliable.onAck(hdr.sequence,nowMs()); continue;
        }
        if(static_cast<PktType>(hdr.type)==PktType::VoiceData&&hdr.length>4){
            uint32_t peerId=*reinterpret_cast<uint32_t*>(pkt.payload.data());
            gNet.voice.push(peerId,pkt.payload.data()+4,hdr.length-4,0); continue;
        }
        if(static_cast<PktType>(hdr.type)==PktType::Ack){
            gNet.reliable.onAck(hdr.sequence,nowMs()); continue;
        }
        uint32_t peerId=from.sin_addr.s_addr^from.sin_port;
        auto& peer=gNet.peers[peerId];
        peer.addr=from; peer.lastSeen=nowMs(); peer.lastSeq=hdr.sequence;
        std::lock_guard lk(gNet.recvMtx);
        gNet.recvQueue.push(std::move(pkt));
    }
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
    constexpr int N = OMNI_CHUNK_CELLS;
    const int baseX = chunkX * N;
    const int baseZ = chunkZ * N;

    const jsize total = N * N * 4;
    auto arr = env->NewFloatArray(total);
    if (!arr) return nullptr;

    std::vector<float> flat;
    flat.reserve(total);
    for (int z = 0; z < N; ++z) {
        for (int x = 0; x < N; ++x) {
            const int cx = baseX + x, cz = baseZ + z;
            flat.push_back(gField.isSolid(cx, cz) ? 1.0f : 0.0f);
            flat.push_back(static_cast<float>(gField.zoneAt(cx, cz)));
            flat.push_back(static_cast<float>(gField.featureAt(cx, cz)));
            flat.push_back(static_cast<float>(gField.fixtureAt(cx, cz)));
        }
    }
    env->SetFloatArrayRegion(arr, 0, total, flat.data());
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
    omni::core::resolveGridCollision(gField,gPlayerBody,before,gSpawnFalling);
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
    auto arr=env->NewFloatArray(9); if(!arr) return nullptr;
    float d[9]={gCamState.pos.x,gCamState.pos.y,gCamState.pos.z,
                gCamState.yaw,gCamState.pitch,gCamState.rollAngle,
                gCamState.fov,gCamState.bobAmount,gCamState.bobPhase};
    env->SetFloatArrayRegion(arr,0,9,d);
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
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_triggerFootstep(JNIEnv*, jobject, jfloat bpm, jfloat surface) {
    std::lock_guard lk(gSound.mtx); gSound.foot.trigger(bpm,surface);
}
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_triggerMonster(JNIEnv*, jobject, jfloat intensity) {
    std::lock_guard lk(gSound.mtx); gSound.monster.trigger(intensity);
}
JNIEXPORT void JNICALL Java_com_omni_backrooms_NativeBridge_stopMonster(JNIEnv*, jobject) {
    std::lock_guard lk(gSound.mtx); gSound.monster.stop();
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
Java_com_omni_backrooms_NativeBridge_initSocket(JNIEnv*, jobject, jint port) {
    if(!gNet.sock.open()) return JNI_FALSE;
    if(port>0&&!gNet.sock.bind(static_cast<uint16_t>(port))){ gNet.sock.close(); return JNI_FALSE; }
    gNet.running.store(true);
    gNet.recvThread=std::thread(recvLoop);
    LOGI_N("UDP ready port=%d",port);
    return JNI_TRUE;
}

JNIEXPORT jbyteArray JNICALL
Java_com_omni_backrooms_NativeBridge_buildPosPacket(JNIEnv* env, jobject, jfloat x, jfloat y, jfloat z, jfloat yaw, jfloat pitch) {
    struct { float x,y,z,yaw,pitch; uint32_t id; } pl{x,y,z,yaw,pitch,gNet.localId.load()};
    auto pkt=omni::net::buildPacket(omni::net::PktType::PlayerPos,&pl,sizeof(pl),gNet.reliable.nextSeq());
    auto arr=env->NewByteArray(static_cast<jsize>(pkt.size()));
    env->SetByteArrayRegion(arr,0,static_cast<jsize>(pkt.size()),reinterpret_cast<const jbyte*>(pkt.data()));
    return arr;
}

JNIEXPORT jbyteArray JNICALL
Java_com_omni_backrooms_NativeBridge_buildPingPacket(JNIEnv* env, jobject) {
    int64_t ts=omni::net::nowMs();
    auto pkt=omni::net::buildPacket(omni::net::PktType::Ping,&ts,sizeof(ts),gNet.reliable.nextSeq());
    auto arr=env->NewByteArray(static_cast<jsize>(pkt.size()));
    env->SetByteArrayRegion(arr,0,static_cast<jsize>(pkt.size()),reinterpret_cast<const jbyte*>(pkt.data()));
    return arr;
}

JNIEXPORT jbyteArray JNICALL
Java_com_omni_backrooms_NativeBridge_buildVoicePacket(JNIEnv* env, jobject, jbyteArray pcmData, jint pcmLen) {
    std::vector<uint8_t> payload(4+pcmLen);
    uint32_t id=gNet.localId.load(); std::memcpy(payload.data(),&id,4);
    env->GetByteArrayRegion(pcmData,0,pcmLen,reinterpret_cast<jbyte*>(payload.data()+4));
    auto pkt=omni::net::buildPacket(omni::net::PktType::VoiceData,payload.data(),static_cast<uint16_t>(payload.size()),gNet.reliable.nextSeq());
    auto arr=env->NewByteArray(static_cast<jsize>(pkt.size()));
    env->SetByteArrayRegion(arr,0,static_cast<jsize>(pkt.size()),reinterpret_cast<const jbyte*>(pkt.data()));
    return arr;
}

JNIEXPORT jobjectArray JNICALL
Java_com_omni_backrooms_NativeBridge_drainRecvQueue(JNIEnv* env, jobject) {
    std::vector<omni::net::Packet> pkts;
    {
        std::lock_guard lk(gNet.recvMtx);
        while(!gNet.recvQueue.empty()){
            pkts.push_back(std::move(gNet.recvQueue.front())); gNet.recvQueue.pop();
            if(pkts.size()>=64) break;
        }
    }
    jclass cls=env->FindClass("[B");
    auto result=env->NewObjectArray(static_cast<jsize>(pkts.size()),cls,nullptr);
    for(int i=0;i<static_cast<int>(pkts.size());++i){
        auto& p=pkts[i];
        int total=omni::net::kHdrSize+static_cast<int>(p.payload.size());
        auto arr=env->NewByteArray(total);
        std::vector<uint8_t> raw(total);
        std::memcpy(raw.data(),&p.header,omni::net::kHdrSize);
        if(!p.payload.empty()) std::memcpy(raw.data()+omni::net::kHdrSize,p.payload.data(),p.payload.size());
        env->SetByteArrayRegion(arr,0,total,reinterpret_cast<const jbyte*>(raw.data()));
        env->SetObjectArrayElement(result,i,arr);
        env->DeleteLocalRef(arr);
    }
    return result;
}

JNIEXPORT jint  JNICALL Java_com_omni_backrooms_NativeBridge_getLocalPing(JNIEnv*, jobject)          { return gNet.localPing.load(); }
JNIEXPORT void  JNICALL Java_com_omni_backrooms_NativeBridge_setLocalId(JNIEnv*, jobject, jint id)   { gNet.localId.store(static_cast<uint32_t>(id)); }
JNIEXPORT jlong JNICALL Java_com_omni_backrooms_NativeBridge_nowMs(JNIEnv*, jobject)                 { return omni::net::nowMs(); }

JNIEXPORT jint JNICALL
Java_com_omni_backrooms_NativeBridge_getPeerCount(JNIEnv*, jobject) {
    int64_t threshold=omni::net::nowMs()-omni::net::kTimeoutMs;
    int count=0;
    for(auto& [id,peer]: gNet.peers) if(peer.lastSeen>threshold) ++count;
    return count;
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_NativeBridge_destroySocket(JNIEnv*, jobject) {
    gNet.running.store(false);
    if(gNet.recvThread.joinable()) gNet.recvThread.join();
    gNet.sock.close(); gNet.peers.clear();
    LOGI_N("Network destroyed");
}

JNIEXPORT jboolean JNICALL
Java_com_omni_backrooms_NativeBridge_initGuard(JNIEnv* env, jobject, jobject ctx, jstring expectedSigHash) {
    using namespace omni::guard;
    const char* raw=env->GetStringUTFChars(expectedSigHash,nullptr);
    std::string hash(raw?raw:"");
    if(raw) env->ReleaseStringUTFChars(expectedSigHash,raw);
    gGuard.sigVerifier=std::make_unique<SignatureVerifier>(hash);
    if(!hash.empty()&&ctx&&!gGuard.sigVerifier->verify(env,ctx))
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
    using namespace omni::guard;
    return (gGuard.cachedFlags.load(std::memory_order_acquire)&(FLAG_ROOT_BINARY|FLAG_ROOT_PROPS|FLAG_ROOT_PATHS|FLAG_MAGISK|FLAG_SHADOW_MOUNT|FLAG_ZYGISK|FLAG_KSU))!=0;
}

JNIEXPORT jboolean JNICALL Java_com_omni_backrooms_NativeBridge_isFridaDetected(JNIEnv*, jobject) {
    using namespace omni::guard;
    return (gGuard.cachedFlags.load(std::memory_order_acquire)&(FLAG_FRIDA_PORT|FLAG_FRIDA_MAPS|FLAG_FRIDA_THREAD|FLAG_FRIDA_GADGET))!=0;
}

JNIEXPORT jboolean JNICALL Java_com_omni_backrooms_NativeBridge_isDebugged(JNIEnv*, jobject) {
    using namespace omni::guard;
    return (gGuard.cachedFlags.load(std::memory_order_acquire)&(FLAG_PTRACE_TRACED|FLAG_DEBUG_WAIT|FLAG_XPOSED|FLAG_SUBSTRATE|FLAG_LSPOSED|FLAG_HOOK_INLINE))!=0;
}

JNIEXPORT jboolean JNICALL Java_com_omni_backrooms_NativeBridge_isEmulator(JNIEnv*, jobject) {
    using namespace omni::guard;
    return (gGuard.cachedFlags.load(std::memory_order_acquire)&(FLAG_EMULATOR_PROPS|FLAG_EMULATOR_HW|FLAG_EMULATOR_CPU))!=0;
}

JNIEXPORT jboolean JNICALL Java_com_omni_backrooms_NativeBridge_isSignatureValid(JNIEnv*, jobject) {
    return (gGuard.cachedFlags.load(std::memory_order_acquire)&omni::guard::FLAG_SIG_MISMATCH)==0;
}

JNIEXPORT jstring JNICALL
Java_com_omni_backrooms_NativeBridge_getThreatReport(JNIEnv* env, jobject) {
    uint32_t f=gGuard.cachedFlags.load(std::memory_order_acquire);
    using namespace omni::guard;
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
    gGuard.sigVerifier.reset();
    gGuard.initialized.store(false,std::memory_order_release);
    LOGI_G("Guard destroyed");
}

} // extern "C"
