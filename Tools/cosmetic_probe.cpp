// ============================================================================
// Frame and trail catalogue probe.
//
// Cosmetics are content, and content that only ever runs on a phone is content
// nobody checks. These are the properties the UI silently depends on:
//
//   * exactly three frames and three trails, each with the English id the store
//     and the wire format use;
//   * every frame's silhouette normalised so its widest point is 1 — a caller
//     scales the whole ring by one number and must know it lands inside its box;
//   * every frame's tube thin enough to leave the portrait clear. This is the
//     one the player actually reported: a ring that closes in over the picture
//     is why the frame had been deleted from the avatar entirely;
//   * emission always in 0..1, at every position, over a long stretch of time —
//     an out-of-range value blows out to white on one facet and reads as a
//     rendering fault;
//   * emission that actually MOVES, so a frame is animated rather than a still;
//   * trail stamps that age, retire, and never outlive their buffer.
//
// Build and run (one line):
//     g++ -std=c++20 -O2 -I Backrooms/Source/Main/Native
//         Tools/cosmetic_probe.cpp
//         Backrooms/Source/Main/Native/Frame/Frame.cpp
//         Backrooms/Source/Main/Native/Trail/Trail.cpp
//         -o /tmp/cosmetic_probe && /tmp/cosmetic_probe
// ============================================================================

#include "Frame/Frame.h"
#include "Trail/Trail.h"

#include <cmath>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

namespace {

int failures = 0;

void check(bool ok, const std::string& what) {
    if (!ok) { std::printf("  FAIL  %s\n", what.c_str()); failures++; }
}

} // namespace

int main() {
    using namespace omni::cosmetic;

    std::printf("Cosmetic catalogue probe\n\n");

    // ---- Frames ------------------------------------------------------------
    check(frameCount() == 3, "expected exactly 3 frames, got " + std::to_string(frameCount()));

    const char* wantFrames[] = { kFaceOfDarkness, kEndlessDimension, kSoundOfRooms };
    for (int i = 0; i < 3; ++i) {
        check(frameIndexOf(wantFrames[i]) == i,
              std::string("frame id not at its index: ") + wantFrames[i]);
    }
    check(frameIndexOf("Halogen") == -1, "a retired id still resolves");
    check(frameIndexOf(nullptr) == -1, "null id must not resolve");
    check(frameAt(-1) == nullptr && frameAt(99) == nullptr, "out-of-range frame must be null");

    constexpr int kS = 192;
    for (int f = 0; f < frameCount(); ++f) {
        const FrameSpec* spec = frameAt(f);
        check(spec != nullptr, "frame " + std::to_string(f) + " missing");
        if (!spec) continue;
        const std::string tag = std::string(spec->id) + ": ";

        std::vector<float> prof(kS * 2, 0.0f);
        frameProfile(f, kS, prof.data());

        float widest = 0.0f, narrowest = 1e9f, thickest = 0.0f, minInner = 1e9f;
        for (int i = 0; i < kS; ++i) {
            const float r = prof[i * 2];
            const float th = prof[i * 2 + 1];
            check(std::isfinite(r) && std::isfinite(th), tag + "non-finite profile");
            check(r > 0.0f, tag + "non-positive radius");
            check(th > 0.0f, tag + "non-positive thickness");
            if (r > widest) widest = r;
            if (r < narrowest) narrowest = r;
            if (th > thickest) thickest = th;
            // The clearance rule, stated as the renderer will use it.
            const float inner = r - th;
            if (inner < minInner) minInner = inner;
            check(th <= kInnerClearance * r + 1e-4f,
                  tag + "tube thicker than the clearance allows at sample " + std::to_string(i));
        }
        check(std::fabs(widest - 1.0f) < 1e-3f,
              tag + "profile not normalised, widest = " + std::to_string(widest));
        check(minInner > 0.55f,
              tag + "inner edge closes in over the portrait, min inner = " + std::to_string(minInner));

        // A silhouette that is a plain circle is not a silhouette. Each frame
        // has to be recognisable by outline alone at thumbnail size.
        const float variation = (widest - narrowest) / widest;
        check(variation > 0.03f,
              tag + "outline is effectively a circle, variation = " + std::to_string(variation));

        // Emission: in range everywhere, over a long stretch of time.
        float lo = 1e9f, hi = -1e9f;
        std::vector<float> em(kS, 0.0f);
        std::vector<float> first(kS, 0.0f);
        double moved = 0.0;
        for (int step = 0; step < 900; ++step) {
            const float t = step * 0.11f;
            frameEmission(f, kS, t, em.data());
            for (int i = 0; i < kS; ++i) {
                check(std::isfinite(em[i]), tag + "non-finite emission");
                if (em[i] < lo) lo = em[i];
                if (em[i] > hi) hi = em[i];
                check(em[i] >= 0.0f && em[i] <= 1.0f,
                      tag + "emission out of 0..1: " + std::to_string(em[i]));
            }
            if (step == 0) first = em;
            else {
                double d = 0.0;
                for (int i = 0; i < kS; ++i) d += std::fabs(em[i] - first[i]);
                if (d / kS > moved) moved = d / kS;
            }
        }
        check(hi > 0.5f, tag + "never lights up, peak = " + std::to_string(hi));
        check(moved > 0.05f,
              tag + "emission barely changes over time, max mean delta = " + std::to_string(moved));
        std::printf("  %-18s outline variation %.3f  emission %.2f..%.2f  motion %.3f\n",
                    spec->id, variation, lo, hi, moved);
    }

    // ---- Trails ------------------------------------------------------------
    std::printf("\n");
    check(trailCount() == 3, "expected 3 trails, got " + std::to_string(trailCount()));
    const char* wantTrails[] = { kDustTrail, kStaticTrail, kSaltTrail };
    for (int i = 0; i < 3; ++i) {
        check(trailIndexOf(wantTrails[i]) == i,
              std::string("trail id not at its index: ") + wantTrails[i]);
    }
    check(trailIndexOf(nullptr) == -1, "null trail id must not resolve");

    for (int s = 0; s < trailCount(); ++s) {
        const TrailSpec* spec = trailAt(s);
        if (!spec) { check(false, "trail missing"); continue; }
        const std::string tag = std::string(spec->id) + ": ";
        check(spec->lifetime > 0.5f, tag + "lifetime too short to see");
        check(spec->scale > 0.0f, tag + "non-positive scale");

        TrailField field;
        field.setStyle(s);

        // Walk in a straight line, dropping a print every 0.4 s.
        TrailStamp out[TrailField::kCapacity];
        float x = 0.0f;
        float side = 1.0f;
        for (int i = 0; i < 400; ++i) {
            field.step(x, 0.0f, 0.0f, side);
            side = -side;
            x += 0.7f;
            field.update(0.4f);
            check(field.liveCount() <= TrailField::kCapacity,
                  tag + "buffer overran its capacity");
            const int n = field.collect(out, TrailField::kCapacity);
            check(n == field.liveCount(), tag + "collect disagrees with liveCount");
            for (int k = 0; k < n; ++k) {
                check(out[k].age >= 0.0f && out[k].age < 1.0f,
                      tag + "expired stamp still live, age = " + std::to_string(out[k].age));
                check(std::fabs(out[k].side) == 1.0f, tag + "side not normalised");
            }
            // Chronological: oldest first, so the renderer can draw in order.
            for (int k = 1; k < n; ++k) {
                check(out[k].age <= out[k - 1].age + 1e-5f,
                      tag + "stamps out of chronological order");
            }
        }
        // Prints must land either side of the line of travel, not in one furrow.
        const int n = field.collect(out, TrailField::kCapacity);
        bool sawLeft = false, sawRight = false;
        for (int k = 0; k < n; ++k) {
            if (out[k].side < 0.0f) sawLeft = true; else sawRight = true;
        }
        check(sawLeft && sawRight, tag + "prints only ever land on one side");

        // Everything must age out once the walker stops.
        for (int i = 0; i < 200; ++i) field.update(0.4f);
        check(field.liveCount() == 0,
              tag + "stamps never expire, " + std::to_string(field.liveCount()) + " left");

        // Clearing has to actually clear, or a teleported player drags a line
        // across the map from wherever they were.
        field.step(5.0f, 5.0f, 0.0f, 1.0f);
        field.clear();
        check(field.liveCount() == 0, tag + "clear() left stamps behind");

        std::printf("  %-14s lifetime %5.1fs  scale %.2f  spread %.2fx  mark %u\n",
                    spec->id, spec->lifetime, spec->scale, spec->spread,
                    static_cast<unsigned>(spec->mark));
    }

    std::printf("\n%s (%d failure%s)\n",
                failures ? "FAILED" : "PASSED", failures, failures == 1 ? "" : "s");
    return failures ? 1 : 0;
}
