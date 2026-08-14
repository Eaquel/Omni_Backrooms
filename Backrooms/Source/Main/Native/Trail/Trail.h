


#ifndef OMNI_TRAIL_H
#define OMNI_TRAIL_H

#include <cstdint>

namespace omni {
namespace cosmetic {


constexpr const char* kDustTrail   = "Dust_Trail";
constexpr const char* kStaticTrail = "Static_Trail";
constexpr const char* kSaltTrail   = "Salt_Trail";


constexpr uint8_t kMarkSole  = 0;
constexpr uint8_t kMarkGlyph = 1;
constexpr uint8_t kMarkGrain = 2;

struct TrailSpec {
    const char* id;
    float tintR, tintG, tintB;

    float lifetime;

    float scale;


    float spread;
    uint8_t mark;
};

int trailCount() noexcept;
const TrailSpec* trailAt(int index) noexcept;
int trailIndexOf(const char* id) noexcept;


struct TrailStamp {
    float x, z;
    float yaw;
    float age;
    float side;
};


class TrailField {
public:
    static constexpr int kCapacity = 48;

    void setStyle(int trailIndex) noexcept;
    int  style() const noexcept { return style_; }



    void step(float x, float z, float yaw, float side) noexcept;


    void update(float dt) noexcept;



    void clear() noexcept { count_ = 0; head_ = 0; }




    int collect(TrailStamp* out, int max) const noexcept;

    int liveCount() const noexcept { return count_; }

private:
    TrailStamp ring_[kCapacity]{};
    int   head_  = 0;
    int   count_ = 0;
    int   style_ = 0;
    float life_  = 6.0f;
};

}
}

#endif
