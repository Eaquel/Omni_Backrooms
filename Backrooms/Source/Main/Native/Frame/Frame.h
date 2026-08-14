


#ifndef OMNI_FRAME_H
#define OMNI_FRAME_H

#include <cstdint>

namespace omni {
namespace cosmetic {


constexpr float kInnerClearance = 0.34f;


constexpr float kPortraitClearance = 0.76f;


constexpr float kProfileFloor = 0.84f;


constexpr const char* kFaceOfDarkness   = "Face_Of_Darkness";
constexpr const char* kEndlessDimension = "Endless_Dimension";
constexpr const char* kSoundOfRooms     = "Sound_Of_Rooms";


struct FrameSpec {
    const char* id;

    float baseR, baseG, baseB;
    float glowR, glowG, glowB;
    float hiR,   hiG,   hiB;



    float tubeRatio;

    float shininess;
};


int frameCount() noexcept;


const FrameSpec* frameAt(int index) noexcept;


int frameIndexOf(const char* id) noexcept;


void frameProfile(int index, int samples, float* out) noexcept;


void frameEmission(int index, int samples, float t, float* out) noexcept;

}
}

#endif
