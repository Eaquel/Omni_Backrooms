#include <jni.h>
#include <android/log.h>
#include <arpa/inet.h>
#include <atomic>
#include <chrono>
#include <cstring>
#include <fcntl.h>
#include <functional>
#include <memory>
#include <mutex>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <optional>
#include <queue>
#include <random>
#include <span>
#include <sys/socket.h>
#include <thread>
#include <unordered_map>
#include <vector>
#include <unistd.h>

#define TAG "OmniNet"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace omni::net {

constexpr uint32_t kMagic       = 0x4F4D4E49;
constexpr int      kHdrSize     = 12;
constexpr int      kMaxPayload  = 1400;
constexpr int      kMaxRetries  = 5;
constexpr int      kPingIntervalMs = 2000;
constexpr int      kTimeoutMs      = 8000;

enum class PktType : uint16_t {
    Ping         = 0x0001,
    Pong         = 0x0002,
    PlayerPos    = 0x0010,
    PlayerAnim   = 0x0011,
    PlayerHealth = 0x0012,
    EntitySync   = 0x0020,
    EntitySpawn  = 0x0021,
    EntityRemove = 0x0022,
    RoomState    = 0x0030,
    RoomJoin     = 0x0031,
    RoomLeave    = 0x0032,
    RoomReady    = 0x0033,
    VoiceData    = 0x0040,
    ChatMsg      = 0x0050,
    GameEvent    = 0x0060,
    LevelSeed    = 0x0070,
    Heartbeat    = 0x00FF,
    Ack          = 0x0100,
    Disconnect   = 0x01FF
};

struct Header {
    uint32_t magic;
    uint16_t type;
    uint16_t length;
    uint32_t sequence;
};

struct Packet {
    Header             header;
    std::vector<uint8_t> payload;
    sockaddr_in        from;
    int64_t            timestampMs;
};

struct PeerInfo {
    sockaddr_in addr;
    int64_t     lastSeen;
    int         ping;
    uint32_t    lastSeq;
    bool        connected;
    std::string roomId;
};

[[nodiscard]] std::vector<uint8_t> buildPacket(
        PktType type,
        const void* payload,
        uint16_t payloadLen,
        uint32_t seq) {

    std::vector<uint8_t> pkt(kHdrSize + payloadLen);
    Header hdr{ kMagic, static_cast<uint16_t>(type), payloadLen, seq };
    std::memcpy(pkt.data(), &hdr, kHdrSize);
    if (payload && payloadLen > 0)
        std::memcpy(pkt.data() + kHdrSize, payload, payloadLen);
    return pkt;
}

[[nodiscard]] std::optional<Header> parseHeader(const uint8_t* data, int len) noexcept {
    if (len < kHdrSize) return std::nullopt;
    Header hdr;
    std::memcpy(&hdr, data, kHdrSize);
    if (hdr.magic != kMagic) return std::nullopt;
    return hdr;
}

[[nodiscard]] int64_t nowMs() noexcept {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now().time_since_epoch()).count();
}

class RttEstimator {
public:
    void addSample(int64_t rttMs) noexcept {
        float sample = static_cast<float>(rttMs);
        srtt_  = (srtt_ == 0.0f) ? sample
                                 : srtt_ * 0.875f + sample * 0.125f;
        rttvar_= (rttvar_ == 0.0f) ? sample * 0.5f
                                   : rttvar_ * 0.75f + std::abs(sample - srtt_) * 0.25f;
        rto_   = std::clamp(srtt_ + 4.0f * rttvar_, 100.0f, 3000.0f);
    }
    [[nodiscard]] float rto()   const noexcept { return rto_; }
    [[nodiscard]] int   ping()  const noexcept { return static_cast<int>(srtt_); }
private:
    float srtt_ = 0, rttvar_ = 0, rto_ = 200.0f;
};

class ReliableChannel {
public:
    struct PendingPacket {
        std::vector<uint8_t> data;
        int64_t              sentAt;
        int                  retries;
        uint32_t             seq;
    };

    [[nodiscard]] uint32_t nextSeq() noexcept { return seq_++; }

    void onAck(uint32_t seq, int64_t nowMs_) noexcept {
        auto it = pending_.find(seq);
        if (it == pending_.end()) return;
        int64_t rtt = nowMs_ - it->second.sentAt;
        rtt_.addSample(rtt);
        pending_.erase(it);
    }

    void track(uint32_t seq, const std::vector<uint8_t>& pkt, int64_t sentAt) {
        pending_[seq] = { pkt, sentAt, 0, seq };
    }

    [[nodiscard]] std::vector<PendingPacket*> getRetransmits(int64_t now) {
        std::vector<PendingPacket*> out;
        for (auto& [s, p] : pending_) {
            if (now - p.sentAt > static_cast<int64_t>(rtt_.rto())) {
                if (p.retries < kMaxRetries) {
                    p.retries++;
                    p.sentAt = now;
                    out.push_back(&p);
                }
            }
        }
        return out;
    }

    [[nodiscard]] int ping() const noexcept { return rtt_.ping(); }

private:
    std::unordered_map<uint32_t, PendingPacket> pending_;
    RttEstimator rtt_;
    uint32_t     seq_ = 1;
};

class JitterBuffer {
public:
    explicit JitterBuffer(int targetDelayMs = 80) : targetDelay_(targetDelayMs) {}

    void push(Packet pkt) {
        std::lock_guard<std::mutex> lk(mtx_);
        buf_.push(std::move(pkt));
    }

    [[nodiscard]] std::optional<Packet> pop(int64_t now) {
        std::lock_guard<std::mutex> lk(mtx_);
        if (buf_.empty()) return std::nullopt;
        if (now - buf_.front().timestampMs < targetDelay_) return std::nullopt;
        auto pkt = std::move(const_cast<Packet&>(buf_.front()));
        buf_.pop();
        return pkt;
    }

    [[nodiscard]] int depth() const {
        std::lock_guard<std::mutex> lk(mtx_);
        return static_cast<int>(buf_.size());
    }

private:
    mutable std::mutex mtx_;
    std::queue<Packet> buf_;
    int                targetDelay_;
};

class UdpSocket {
public:
    bool open() noexcept {
        fd_ = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
        if (fd_ < 0) { LOGE("socket() failed"); return false; }
        int opt = 1;
        setsockopt(fd_, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
        int sndbuf = 256 * 1024, rcvbuf = 256 * 1024;
        setsockopt(fd_, SOL_SOCKET, SO_SNDBUF, &sndbuf, sizeof(sndbuf));
        setsockopt(fd_, SOL_SOCKET, SO_RCVBUF, &rcvbuf, sizeof(rcvbuf));
        fcntl(fd_, F_SETFL, O_NONBLOCK);
        return true;
    }

    bool bind(uint16_t port) noexcept {
        sockaddr_in a{};
        a.sin_family = AF_INET;
        a.sin_addr.s_addr = INADDR_ANY;
        a.sin_port = htons(port);
        return ::bind(fd_, reinterpret_cast<sockaddr*>(&a), sizeof(a)) == 0;
    }

    int sendTo(const void* d, int n, const sockaddr_in& dst) noexcept {
        return static_cast<int>(
            sendto(fd_, d, n, 0, reinterpret_cast<const sockaddr*>(&dst), sizeof(dst)));
    }

    int recvFrom(void* buf, int maxN, sockaddr_in& from) noexcept {
        socklen_t fromLen = sizeof(from);
        return static_cast<int>(
            recvfrom(fd_, buf, maxN, 0, reinterpret_cast<sockaddr*>(&from), &fromLen));
    }

    void close() noexcept { if (fd_ >= 0) { ::close(fd_); fd_ = -1; } }
    [[nodiscard]] bool isOpen() const noexcept { return fd_ >= 0; }

private:
    int fd_ = -1;
};

struct VoicePacket {
    uint32_t peerId;
    uint16_t seqNum;
    std::vector<uint8_t> pcmData;
};

class VoiceMultiplexer {
public:
    void push(uint32_t peerId, const uint8_t* data, int len, uint16_t seq) {
        std::lock_guard<std::mutex> lk(mtx_);
        auto& jb = jitterBuffers_[peerId];
        if (!jb) jb = std::make_unique<JitterBuffer>(60);
        Packet p;
        p.payload.assign(data, data + len);
        p.timestampMs = nowMs() - seq;
        jb->push(std::move(p));
    }

    [[nodiscard]] std::optional<VoicePacket> pop(uint32_t peerId) {
        std::lock_guard<std::mutex> lk(mtx_);
        auto it = jitterBuffers_.find(peerId);
        if (it == jitterBuffers_.end()) return std::nullopt;
        auto pkt = it->second->pop(nowMs());
        if (!pkt) return std::nullopt;
        return VoicePacket{ peerId, 0, std::move(pkt->payload) };
    }

private:
    mutable std::mutex mtx_;
    std::unordered_map<uint32_t, std::unique_ptr<JitterBuffer>> jitterBuffers_;
};

struct NetState {
    UdpSocket                                    sock;
    ReliableChannel                              reliable;
    VoiceMultiplexer                             voice;
    std::unordered_map<uint32_t, PeerInfo>       peers;
    std::queue<Packet>                           recvQueue;
    std::mutex                                   recvMtx;
    std::atomic<bool>                            running{false};
    std::thread                                  recvThread;
    std::atomic<int>                             localPing{0};
    std::atomic<uint32_t>                        localId{0};
    std::string                                  currentRoom;
    uint8_t                                      recvBuf[kHdrSize + kMaxPayload];
};

} // namespace omni::net

static omni::net::NetState gNet;

static void recvLoop() {
    using namespace omni::net;
    while (gNet.running.load()) {
        sockaddr_in from{};
        int n = gNet.sock.recvFrom(gNet.recvBuf, sizeof(gNet.recvBuf), from);
        if (n <= 0) { std::this_thread::sleep_for(std::chrono::milliseconds(1)); continue; }

        auto hdrOpt = parseHeader(gNet.recvBuf, n);
        if (!hdrOpt) continue;
        auto& hdr = *hdrOpt;

        Packet pkt;
        pkt.header      = hdr;
        pkt.from        = from;
        pkt.timestampMs = nowMs();
        if (hdr.length > 0 && n >= kHdrSize + hdr.length)
            pkt.payload.assign(gNet.recvBuf + kHdrSize, gNet.recvBuf + kHdrSize + hdr.length);

        if (static_cast<PktType>(hdr.type) == PktType::Pong) {
            int64_t rtt = nowMs() - static_cast<int64_t>(pkt.payload.size() >= 8
                ? *reinterpret_cast<int64_t*>(pkt.payload.data()) : 0);
            gNet.localPing.store(static_cast<int>(rtt));
            gNet.reliable.onAck(hdr.sequence, nowMs());
            continue;
        }

        if (static_cast<PktType>(hdr.type) == PktType::VoiceData && hdr.length > 4) {
            uint32_t peerId = *reinterpret_cast<uint32_t*>(pkt.payload.data());
            gNet.voice.push(peerId, pkt.payload.data() + 4, hdr.length - 4, 0);
            continue;
        }

        if (static_cast<PktType>(hdr.type) == PktType::Ack) {
            gNet.reliable.onAck(hdr.sequence, nowMs());
            continue;
        }

        uint32_t peerId = from.sin_addr.s_addr ^ from.sin_port;
        auto& peer = gNet.peers[peerId];
        peer.addr     = from;
        peer.lastSeen = nowMs();
        peer.lastSeq  = hdr.sequence;

        std::lock_guard<std::mutex> lk(gNet.recvMtx);
        gNet.recvQueue.push(std::move(pkt));
    }
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_omni_backrooms_Native_1Bridge_initSocket(JNIEnv*, jobject, jint port) {
    if (!gNet.sock.open()) return JNI_FALSE;
    if (port > 0 && !gNet.sock.bind(static_cast<uint16_t>(port))) {
        LOGE("bind port %d failed", port);
        gNet.sock.close();
        return JNI_FALSE;
    }
    gNet.running.store(true);
    gNet.recvThread = std::thread(recvLoop);
    LOGI("UDP socket ready (port=%d)", port);
    return JNI_TRUE;
}

JNIEXPORT jbyteArray JNICALL
Java_com_omni_backrooms_Native_1Bridge_buildPosPacket(
        JNIEnv* env, jobject, jfloat x, jfloat y, jfloat z, jfloat yaw, jfloat pitch) {
    struct { float x,y,z,yaw,pitch; uint32_t id; } pl{
        x, y, z, yaw, pitch, gNet.localId.load()
    };
    uint32_t seq = gNet.reliable.nextSeq();
    auto pkt = omni::net::buildPacket(omni::net::PktType::PlayerPos, &pl, sizeof(pl), seq);
    auto arr = env->NewByteArray(static_cast<jsize>(pkt.size()));
    env->SetByteArrayRegion(arr, 0, static_cast<jsize>(pkt.size()),
                            reinterpret_cast<const jbyte*>(pkt.data()));
    return arr;
}

JNIEXPORT jbyteArray JNICALL
Java_com_omni_backrooms_Native_1Bridge_buildPingPacket(JNIEnv* env, jobject) {
    int64_t ts = omni::net::nowMs();
    uint32_t seq = gNet.reliable.nextSeq();
    auto pkt = omni::net::buildPacket(omni::net::PktType::Ping, &ts, sizeof(ts), seq);
    auto arr = env->NewByteArray(static_cast<jsize>(pkt.size()));
    env->SetByteArrayRegion(arr, 0, static_cast<jsize>(pkt.size()),
                            reinterpret_cast<const jbyte*>(pkt.data()));
    return arr;
}

JNIEXPORT jbyteArray JNICALL
Java_com_omni_backrooms_Native_1Bridge_buildVoicePacket(
        JNIEnv* env, jobject, jbyteArray pcmData, jint pcmLen) {
    std::vector<uint8_t> payload(4 + pcmLen);
    uint32_t id = gNet.localId.load();
    std::memcpy(payload.data(), &id, 4);
    env->GetByteArrayRegion(pcmData, 0, pcmLen, reinterpret_cast<jbyte*>(payload.data() + 4));
    uint32_t seq = gNet.reliable.nextSeq();
    auto pkt = omni::net::buildPacket(omni::net::PktType::VoiceData, payload.data(),
                                      static_cast<uint16_t>(payload.size()), seq);
    auto arr = env->NewByteArray(static_cast<jsize>(pkt.size()));
    env->SetByteArrayRegion(arr, 0, static_cast<jsize>(pkt.size()),
                            reinterpret_cast<const jbyte*>(pkt.data()));
    return arr;
}

JNIEXPORT jobjectArray JNICALL
Java_com_omni_backrooms_Native_1Bridge_drainRecvQueue(JNIEnv* env, jobject) {
    std::vector<omni::net::Packet> pkts;
    {
        std::lock_guard<std::mutex> lk(gNet.recvMtx);
        while (!gNet.recvQueue.empty()) {
            pkts.push_back(std::move(gNet.recvQueue.front()));
            gNet.recvQueue.pop();
            if (pkts.size() >= 64) break;
        }
    }
    jclass byteArrCls = env->FindClass("[B");
    auto result = env->NewObjectArray(static_cast<jsize>(pkts.size()), byteArrCls, nullptr);
    for (int i = 0; i < static_cast<int>(pkts.size()); ++i) {
        auto& p = pkts[i];
        int total = omni::net::kHdrSize + static_cast<int>(p.payload.size());
        auto arr  = env->NewByteArray(total);
        std::vector<uint8_t> raw(total);
        std::memcpy(raw.data(), &p.header, omni::net::kHdrSize);
        if (!p.payload.empty())
            std::memcpy(raw.data() + omni::net::kHdrSize, p.payload.data(), p.payload.size());
        env->SetByteArrayRegion(arr, 0, total, reinterpret_cast<const jbyte*>(raw.data()));
        env->SetObjectArrayElement(result, i, arr);
        env->DeleteLocalRef(arr);
    }
    return result;
}

JNIEXPORT jint JNICALL
Java_com_omni_backrooms_Native_1Bridge_getLocalPing(JNIEnv*, jobject) {
    return gNet.localPing.load();
}

JNIEXPORT jint JNICALL
Java_com_omni_backrooms_Native_1Bridge_getPeerCount(JNIEnv*, jobject) {
    int64_t threshold = omni::net::nowMs() - omni::net::kTimeoutMs;
    int count = 0;
    for (auto& [id, peer] : gNet.peers)
        if (peer.lastSeen > threshold) ++count;
    return count;
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_setLocalId(JNIEnv*, jobject, jint id) {
    gNet.localId.store(static_cast<uint32_t>(id));
}

JNIEXPORT jlong JNICALL
Java_com_omni_backrooms_Native_1Bridge_nowMs(JNIEnv*, jobject) {
    return omni::net::nowMs();
}

JNIEXPORT void JNICALL
Java_com_omni_backrooms_Native_1Bridge_destroySocket(JNIEnv*, jobject) {
    gNet.running.store(false);
    if (gNet.recvThread.joinable()) gNet.recvThread.join();
    gNet.sock.close();
    gNet.peers.clear();
    LOGI("Network destroyed");
}

} // extern "C"
