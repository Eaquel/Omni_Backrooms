// ============================================================================
// Footstep trails.
//
// A trail is what the player leaves on the carpet behind them. It is a ring
// buffer of stamps: each footfall pushes one, and every stamp fades on its own
// clock from the moment it was laid.
//
// The whole thing lives here rather than in the renderer because a trail is a
// simulation, not a decoration. Stamps outlive the frame that spawned them,
// they have to keep ageing while the player walks away from them, and the
// oldest has to be recycled without any allocation happening mid-run. That is
// state with rules, and it belongs next to the engine.
//
// The renderer's only job is to ask what is currently on the floor.
// ============================================================================

#ifndef OMNI_TRAIL_H
#define OMNI_TRAIL_H

#include <cstdint>

namespace omni {
namespace cosmetic {

/** Trails are addressed by these ids on the wire and in the store. */
constexpr const char* kDustTrail   = "Dust_Trail";
constexpr const char* kStaticTrail = "Static_Trail";
constexpr const char* kSaltTrail   = "Salt_Trail";

/** How a stamp is drawn. The renderer switches on this. */
constexpr uint8_t kMarkSole  = 0;   // a shoe print, oriented to the walk
constexpr uint8_t kMarkGlyph = 1;   // a torn block of static
constexpr uint8_t kMarkGrain = 2;   // a scatter of grains

struct TrailSpec {
    const char* id;
    float tintR, tintG, tintB;
    /** Seconds a stamp takes to fade out completely. */
    float lifetime;
    /** Stamp size in metres at birth. */
    float scale;
    /** How far the stamp grows over its life, as a multiple of scale. Dust
     *  spreads as it settles; a pressed print does not. */
    float spread;
    uint8_t mark;
};

int trailCount() noexcept;
const TrailSpec* trailAt(int index) noexcept;
int trailIndexOf(const char* id) noexcept;

/** One mark on the floor, as handed to the renderer. */
struct TrailStamp {
    float x, z;      // world metres
    float yaw;       // facing, radians
    float age;       // 0 at birth, 1 when it should be gone
    float side;      // -1 left foot, +1 right foot
};

/**
 * The live trail behind one walker.
 *
 * Fixed capacity and no allocation after construction: this is stepped every
 * frame and a trail that allocates is a trail that stutters.
 */
class TrailField {
public:
    static constexpr int kCapacity = 48;

    void setStyle(int trailIndex) noexcept;
    int  style() const noexcept { return style_; }

    /** Drops a mark. [side] alternates so prints land either side of the line
     *  of travel rather than in a single furrow down the middle. */
    void step(float x, float z, float yaw, float side) noexcept;

    /** Ages everything by [dt] seconds and retires whatever has expired. */
    void update(float dt) noexcept;

    /** Forgets every mark — on death, on a new run, on a teleport. Without this
     *  a relocated player drags a line across the map from where they were. */
    void clear() noexcept { count_ = 0; head_ = 0; }

    /**
     * Copies the live stamps out, newest last so the renderer can draw them in
     * order and have the freshest mark land on top. Writes at most [max]
     * entries and returns how many it wrote.
     */
    int collect(TrailStamp* out, int max) const noexcept;

    int liveCount() const noexcept { return count_; }

private:
    TrailStamp ring_[kCapacity]{};
    int   head_  = 0;
    int   count_ = 0;
    int   style_ = 0;
    float life_  = 6.0f;
};

} // namespace cosmetic
} // namespace omni

#endif // OMNI_TRAIL_H
