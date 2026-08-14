#include "Trail/Trail.h"

#include <cmath>
#include <cstring>

namespace omni {
namespace cosmetic {
namespace {


const TrailSpec kTrails[] = {


    { kDustTrail,   0.72f, 0.66f, 0.50f,  7.0f, 0.30f, 1.9f, kMarkSole  },


    { kStaticTrail, 0.60f, 0.86f, 0.95f,  2.6f, 0.34f, 1.05f, kMarkGlyph },


    { kSaltTrail,   0.94f, 0.92f, 0.86f, 14.0f, 0.22f, 1.25f, kMarkGrain },
};

constexpr int kCount = static_cast<int>(sizeof(kTrails) / sizeof(kTrails[0]));

}

int trailCount() noexcept { return kCount; }

const TrailSpec* trailAt(int index) noexcept {
    if (index < 0 || index >= kCount) return nullptr;
    return &kTrails[index];
}

int trailIndexOf(const char* id) noexcept {
    if (id == nullptr) return -1;
    for (int i = 0; i < kCount; ++i) {
        if (std::strcmp(kTrails[i].id, id) == 0) return i;
    }
    return -1;
}

void TrailField::setStyle(int trailIndex) noexcept {
    const TrailSpec* spec = trailAt(trailIndex);
    if (spec == nullptr) return;



    if (trailIndex != style_) clear();
    style_ = trailIndex;
    life_  = spec->lifetime > 0.01f ? spec->lifetime : 0.01f;
}

void TrailField::step(float x, float z, float yaw, float side) noexcept {


    const float offset = 0.16f;
    const float px = x + std::cos(yaw) * offset * side;
    const float pz = z - std::sin(yaw) * offset * side;

    TrailStamp& s = ring_[head_];
    s.x = px; s.z = pz; s.yaw = yaw; s.age = 0.0f;
    s.side = side < 0.0f ? -1.0f : 1.0f;

    head_ = (head_ + 1) % kCapacity;
    if (count_ < kCapacity) ++count_;
}

void TrailField::update(float dt) noexcept {
    if (dt <= 0.0f || count_ == 0) return;
    const float rate = dt / life_;




    for (int i = 0; i < count_; ++i) {
        const int idx = (head_ - count_ + i + kCapacity * 2) % kCapacity;
        ring_[idx].age += rate;
    }
    while (count_ > 0) {
        const int oldest = (head_ - count_ + kCapacity * 2) % kCapacity;
        if (ring_[oldest].age < 1.0f) break;
        --count_;
    }
}

int TrailField::collect(TrailStamp* out, int max) const noexcept {
    if (out == nullptr || max <= 0 || count_ == 0) return 0;
    const int n = count_ < max ? count_ : max;


    const int skip = count_ - n;
    for (int i = 0; i < n; ++i) {
        const int idx = (head_ - count_ + skip + i + kCapacity * 2) % kCapacity;
        out[i] = ring_[idx];
    }
    return n;
}

}
}
