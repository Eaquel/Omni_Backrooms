


#ifndef OMNI_ENDING_ENDING_H
#define OMNI_ENDING_ENDING_H

#include <cstdint>

namespace omni {
namespace ending {


enum class Kind : uint8_t { None = 0, Death = 1, Escape = 2 };


struct Params {

    float desaturate = 0.0f;

    float vignette = 0.0f;

    float aberration = 0.0f;

    float tear = 0.0f;

    float pull = 0.0f;

    float bloom = 0.0f;


    float exposure = 1.0f;



    float panel = 0.0f;
};


constexpr float kDeathSeconds  = 2.30f;
constexpr float kEscapeSeconds = 1.90f;


[[nodiscard]] Params evaluate(Kind kind, float t) noexcept;


[[nodiscard]] float duration(Kind kind) noexcept;

}
}

#endif
