#!/usr/bin/env python3
"""
Assets_Check.py — everything that guards the game's authored content.

Four things live here because they answer the same question from different
angles: is what we ship actually what we meant to ship?

  * VECTOR DRAWABLES — every icon is hand-written XML and a typo in a path does
    not fail the build. aapt2 accepts it and the icon renders garbled, or not
    at all.
  * MESH UVs — Level 0's textures are world-anchored, so a vertex's UV must
    equal its own world position. The ceiling silently violated that for a long
    time: the emitter handed coordinates out in a fixed corner order, which is
    only right for a quad wound in that order, and the ceiling is wound the
    other way so it faces down. Every tile came out mirrored across its own
    diagonal and no two neighbours could line up.
  * INSPECTION CAMERA — the market's orbit/dolly envelope. A shot that runs off
    the end of the backdrop, or a camera that sinks under the floor, is pure
    geometry and there is no reason to find it on a phone.
  * COSMETIC CATALOGUE — the frame and trail tables in Native/. Includes the
    rule that no frame's tube may close in over the portrait it surrounds,
    which is the fault that got the ring deleted from the avatar entirely.
  * CHARACTER RIG — the bones against the mesh they are supposed to drive. The
    character shipped with four arms, because the model wore a T-posed dress
    over a body whose arms hang at its sides and the bones went on the sleeves.
    Every structural thing about that file was correct, so the check binds the
    mesh the way the game does, animates it, and measures whether the surface
    survives.

Plus an inventory pass: duplicate assets, unreferenced assets, and Title Case.

    python3 Tools/Assets_Check.py                 # check everything
    python3 Tools/Assets_Check.py --optimise      # losslessly shrink PNGs
    python3 Tools/Assets_Check.py --optimise --dry-run
"""
from __future__ import annotations

import argparse
import glob
import hashlib
import json
import math
import os
import re
import struct
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
import zlib

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "Backrooms/Source/Main/res")
ASSETS = os.path.join(REPO, "Backrooms/Source/Main/Assets")
NATIVE = os.path.join(REPO, "Backrooms/Source/Main/Native")
KOTLIN = os.path.join(REPO, "Backrooms/Source/Main/Kotlin/com/omni/backrooms")

NS = "{http://schemas.android.com/apk/res/android}"

failures: list[str] = []


def check(ok: bool, what: str) -> None:
    if not ok:
        failures.append(what)


def section(title: str) -> None:
    print(f"\n── {title}")


# ===========================================================================
# 1. Vector drawables
# ===========================================================================

# Lowercase is relative; A/a is an arc, whose numbers are radii and flags rather
# than positions. Treating either as a coordinate produces false positives — a
# first cut of this check flagged eight perfectly good icons that way.
RELATIVE_OR_ARC = re.compile(r"[mlcsqtvhaz]|A")
NUM = re.compile(r"-?\d*\.?\d+(?:[eE][-+]?\d+)?")


def check_drawables() -> None:
    section("Vector drawables")
    files = sorted(glob.glob(os.path.join(RES, "drawable/*.xml")))
    check(bool(files), "no drawables found — wrong working directory?")
    vectors = paths_checked = 0

    for f in files:
        rel = os.path.relpath(f, REPO)
        try:
            root = ET.parse(f).getroot()
        except Exception as exc:
            failures.append(f"{rel}: not well-formed XML: {exc}")
            continue
        if not root.tag.endswith("vector"):
            continue
        vectors += 1

        vw, vh = root.get(NS + "viewportWidth"), root.get(NS + "viewportHeight")
        check(vw is not None and vh is not None, f"{rel}: no viewport declared")
        if vw is None or vh is None:
            continue
        vw, vh = float(vw), float(vh)
        check(vw == 24.0 and vh == 24.0,
              f"{rel}: viewport is {vw:g}x{vh:g}, not the 24x24 the rest of the set uses")

        paths = root.findall(NS + "path") + root.findall("path")
        check(bool(paths), f"{rel}: a vector with no paths draws nothing")

        for i, p in enumerate(paths):
            d = p.get(NS + "pathData")
            check(d is not None, f"{rel}: path {i} has no pathData")
            if d is None:
                continue
            check(re.match(r"^\s*[Mm]", d) is not None,
                  f"{rel}: path {i} does not begin with a move")
            if RELATIVE_OR_ARC.search(d):
                continue
            paths_checked += 1
            coords = [float(n) for n in NUM.findall(d)]
            check(len(coords) % 2 == 0, f"{rel}: path {i} has an odd number of coordinates")
            outside = [c for c in coords if c < -0.01 or c > max(vw, vh) + 0.01]
            check(not outside,
                  f"{rel}: path {i} has {len(outside)} coordinate(s) outside the viewport")

    print(f"   {vectors} vectors, {paths_checked} paths verified against the viewport")


# ===========================================================================
# 2. Mesh UVs
# ===========================================================================

CELL = 3.2
SRC_KT = os.path.join(KOTLIN, "Backrooms.kt")


def _floor_quad(x0, z0):
    x1, z1 = x0 + CELL, z0 + CELL
    return [(x0, z0), (x1, z0), (x1, z1), (x0, z1)], [(x0, z0), (x1, z0), (x1, z1), (x0, z1)]


def _roof_quad(x0, z0, fixed=True):
    """`fixed=False` reproduces the fixed-corner-order assignment quad() makes,
    which is what the ceiling used to get."""
    x1, z1 = x0 + CELL, z0 + CELL
    pts = [(x0, z0), (x0, z1), (x1, z1), (x1, z0)]
    uvs = pts[:] if fixed else [(x0, z0), (x1, z0), (x1, z1), (x0, z1)]
    return pts, uvs


def _mismatches(fn, **kw) -> int:
    bad = 0
    for cz in range(-40, 41, 7):
        for cx in range(-40, 41, 7):
            pts, uvs = fn(cx * CELL, cz * CELL, **kw)
            for (px, pz), (u, v) in zip(pts, uvs):
                if abs(px - u) > 1e-4 or abs(pz - v) > 1e-4:
                    bad += 1
    return bad


def check_mesh_uvs() -> None:
    section("Chunk mesher UVs")
    for name, fn in (("floor", _floor_quad), ("ceiling", _roof_quad)):
        bad = _mismatches(fn)
        check(bad == 0, f"{name}: {bad} corners whose UV does not match their world position")
        print(f"   {name:8s} {'ok' if bad == 0 else f'{bad} mismatched corners'}")

    # The check must be able to fail, or it is decoration.
    regressed = _mismatches(_roof_quad, fixed=False)
    check(regressed > 0, "the UV check cannot detect the original ceiling ordering")
    print(f"   pre-fix ceiling ordering rejected ({regressed} mismatched corners)")

    if os.path.exists(SRC_KT):
        text = open(SRC_KT, encoding="utf-8").read()
        m = re.search(r"roofB = (quadUv|quad)\(", text)
        check(m is not None, "no ceiling quad emitter found in the source")
        if m:
            check(m.group(1) == "quadUv",
                  "the ceiling is emitted through quad(), which cannot express its winding")
        # Skipping either surface on a feature leaves a one-cell hole with
        # nothing behind it, which the player reads as a corrupted tile.
        for surface, pattern in (("floor", r"if \(feature != 4\)"),
                                 ("ceiling", r"if \(feature != 1\)")):
            check(re.search(pattern, text) is None,
                  f"{surface} is skipped on a feature, leaving a one-cell hole")
        print("   floor and ceiling emitted for every open cell")


# ===========================================================================
# 3. Inspection camera envelope
# ===========================================================================

MIN_DIST, MAX_DIST = 1.7, 5.2
MIN_PITCH, MAX_PITCH = -10.0, 38.0
FOVY, NEAR, FAR = 34.0, 0.1, 40.0
COVE = 14.0
CHAR_H, CHAR_R = 1.7, 0.40
ASPECTS = (0.45, 0.5625, 0.75, 1.0, 1.6, 2.2)


def _view(d, pitch_deg):
    d = max(MIN_DIST, min(MAX_DIST, d))
    far = min(1.0, max(0.0, (d - MIN_DIST) / (MAX_DIST - MIN_DIST)))
    ty = 1.38 - 0.46 * far
    p = math.radians(pitch_deg)
    return (0.0, max(0.22, ty + math.sin(p) * d), math.cos(p) * d), (0.0, ty, 0.0)


def _basis(eye, tgt):
    f = [tgt[i] - eye[i] for i in range(3)]
    n = math.dist(eye, tgt)
    f = [c / n for c in f]
    up = (0.0, 1.0, 0.0)
    s = [f[1]*up[2]-f[2]*up[1], f[2]*up[0]-f[0]*up[2], f[0]*up[1]-f[1]*up[0]]
    ln = math.sqrt(sum(c*c for c in s))
    s = [c / ln for c in s]
    u = [s[1]*f[2]-s[2]*f[1], s[2]*f[0]-s[0]*f[2], s[0]*f[1]-s[1]*f[0]]
    return f, s, u


def _inside(eye, tgt, aspect, pt):
    f, s, u = _basis(eye, tgt)
    v = [pt[i] - eye[i] for i in range(3)]
    z = sum(v[i]*f[i] for i in range(3))
    if z < NEAR or z > FAR:
        return False
    x = sum(v[i]*s[i] for i in range(3))
    y = sum(v[i]*u[i] for i in range(3))
    hv = z * math.tan(math.radians(FOVY / 2))
    return abs(y) <= hv and abs(x) <= hv * aspect


def _edge_hits_cove(eye, tgt, aspect, sx, sy):
    f, s, u = _basis(eye, tgt)
    hv = math.tan(math.radians(FOVY / 2))
    hh = hv * aspect
    d = [f[i] + s[i]*sx*hh + u[i]*sy*hv for i in range(3)]
    n = math.sqrt(sum(c*c for c in d))
    d = [c / n for c in d]
    if d[2] < -1e-6:                                  # back wall
        t = (-COVE - eye[2]) / d[2]
        if t > 0:
            hx, hy = eye[0]+d[0]*t, eye[1]+d[1]*t
            if abs(hx) <= COVE and 0 <= hy <= COVE:
                return True, t
    if d[1] < -1e-6:                                  # floor
        t = (0.0 - eye[1]) / d[1]
        if t > 0:
            hx, hz = eye[0]+d[0]*t, eye[2]+d[2]*t
            if abs(hx) <= COVE and abs(hz) <= COVE:
                return True, t
    return False, 0.0


def check_camera() -> None:
    section("Inspection camera envelope")
    states = 0
    for aspect in ASPECTS:
        for pi in range(25):
            pitch = MIN_PITCH + (MAX_PITCH - MIN_PITCH) * pi / 24
            for di in range(25):
                d = MIN_DIST + (MAX_DIST - MIN_DIST) * di / 24
                eye, tgt = _view(d, pitch)
                states += 1
                check(eye[1] >= 0.21,
                      f"eye below the cove floor at aspect={aspect} pitch={pitch:.1f} d={d:.2f}")
                for sx, sy in ((-1, -1), (1, -1), (-1, 1), (1, 1), (0, 1), (0, -1)):
                    ok, t = _edge_hits_cove(eye, tgt, aspect, sx, sy)
                    check(ok, f"frame edge runs off the cove at aspect={aspect} "
                              f"pitch={pitch:.1f} d={d:.2f} corner=({sx},{sy})")
                    check(t <= FAR, f"cove beyond the far plane at d={d:.2f}")

    for aspect in ASPECTS:
        eye, tgt = _view(MAX_DIST, 7.0)
        for y in (0.0, CHAR_H):
            for ang in range(0, 360, 45):
                pt = (CHAR_R*math.cos(math.radians(ang)), y, CHAR_R*math.sin(math.radians(ang)))
                check(_inside(eye, tgt, aspect, pt), f"figure clipped at max dolly, aspect={aspect}")
        eye, tgt = _view(MIN_DIST, 7.0)
        for pt in ((0.0, CHAR_H, 0.0), (0.0, CHAR_H - 0.30, 0.0)):
            check(_inside(eye, tgt, aspect, pt), f"head clipped at min dolly, aspect={aspect}")

    print(f"   {states} camera states, all framing invariants hold")


# ===========================================================================
# 4. Cosmetic catalogue (Native/Frame, Native/Trail)
# ===========================================================================

COSMETIC_PROBE = r"""
#include "Frame/Frame.h"
#include "Trail/Trail.h"
#include <cmath>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

static int failures = 0;
static void check(bool ok, const std::string& what) {
    if (!ok) { std::printf("  FAIL  %s\n", what.c_str()); failures++; }
}

int main() {
    using namespace omni::cosmetic;

    check(frameCount() == 3, "expected exactly 3 frames");
    const char* wantF[] = { kFaceOfDarkness, kEndlessDimension, kSoundOfRooms };
    for (int i = 0; i < 3; ++i)
        check(frameIndexOf(wantF[i]) == i, std::string("frame id not at its index: ") + wantF[i]);
    check(frameIndexOf("Halogen") == -1, "a retired id still resolves");
    check(frameIndexOf(nullptr) == -1, "null id must not resolve");
    check(frameAt(-1) == nullptr && frameAt(99) == nullptr, "out-of-range frame must be null");

    const int S = 192;
    for (int f = 0; f < frameCount(); ++f) {
        const FrameSpec* spec = frameAt(f);
        check(spec != nullptr, "frame missing");
        if (!spec) continue;
        const std::string tag = std::string(spec->id) + ": ";

        std::vector<float> prof(S * 2, 0.0f);
        frameProfile(f, S, prof.data());
        float widest = 0, narrow = 1e9f, minInner = 1e9f, maxOuter = 0;
        for (int i = 0; i < S; ++i) {
            const float r = prof[i*2], th = prof[i*2+1];
            check(std::isfinite(r) && std::isfinite(th), tag + "non-finite profile");
            check(r > 0.0f && th > 0.0f, tag + "non-positive profile");
            widest = std::fmax(widest, r); narrow = std::fmin(narrow, r);
            minInner = std::fmin(minInner, r - th);
            maxOuter = std::fmax(maxOuter, r + th);
            check(th <= kInnerClearance * r + 1e-4f, tag + "tube thicker than the clearance allows");
        }
        check(std::fabs(widest - 1.0f) < 1e-3f, tag + "profile not normalised");

        // The real bound, in the units the caller draws in.
        //
        // This used to be `minInner > 0.55f`, a number with no relationship to
        // the picture. The worst frame measured 0.673 — comfortably past that
        // check, and still drawn across the portrait, because at the avatar's
        // ring radius of 0.42 an inner edge of 0.673 lands at 0.283 of the box
        // and the photo's radius is 0.31.
        //
        // So it is stated as the geometry now: the portrait over the ring must
        // fit inside the clearance, and the outer edge must stay in the box.
        constexpr float kRingRadius    = 0.42f;   // Backrooms.kt drawFrame3D call
        constexpr float kPortraitR     = 0.31f;   // fillMaxSize(0.62f) / 2
        constexpr float kBoxHalf       = 0.50f;
        check(kPortraitR / kRingRadius <= kPortraitClearance + 1e-4f,
              tag + "the portrait is too big for the clearance the profile guarantees");
        check(minInner >= kPortraitClearance - 1e-4f,
              tag + "inner edge closes in over the portrait");
        check(maxOuter * kRingRadius <= kBoxHalf + 1e-3f,
              tag + "outer edge runs past the box it is drawn in");
        check((widest - narrow) / widest > 0.03f, tag + "outline is effectively a circle");

        float lo = 1e9f, hi = -1e9f; double moved = 0;
        std::vector<float> em(S, 0.0f), first(S, 0.0f);
        for (int step = 0; step < 900; ++step) {
            frameEmission(f, S, step * 0.11f, em.data());
            for (int i = 0; i < S; ++i) {
                check(std::isfinite(em[i]), tag + "non-finite emission");
                check(em[i] >= 0.0f && em[i] <= 1.0f, tag + "emission out of 0..1");
                lo = std::fmin(lo, em[i]); hi = std::fmax(hi, em[i]);
            }
            if (step == 0) first = em;
            else {
                double d = 0; for (int i = 0; i < S; ++i) d += std::fabs(em[i] - first[i]);
                moved = std::fmax(moved, d / S);
            }
        }
        check(hi > 0.5f, tag + "never lights up");
        check(moved > 0.05f, tag + "emission barely changes over time");
        std::printf("   %-18s variation %.3f  emission %.2f..%.2f  motion %.3f\n",
                    spec->id, (widest - narrow) / widest, lo, hi, moved);
    }

    check(trailCount() == 3, "expected 3 trails");
    const char* wantT[] = { kDustTrail, kStaticTrail, kSaltTrail };
    for (int i = 0; i < 3; ++i)
        check(trailIndexOf(wantT[i]) == i, std::string("trail id not at its index: ") + wantT[i]);
    check(trailIndexOf(nullptr) == -1, "null trail id must not resolve");

    for (int s = 0; s < trailCount(); ++s) {
        const TrailSpec* spec = trailAt(s);
        if (!spec) { check(false, "trail missing"); continue; }
        const std::string tag = std::string(spec->id) + ": ";
        check(spec->lifetime > 0.5f, tag + "lifetime too short to see");
        check(spec->scale > 0.0f, tag + "non-positive scale");

        TrailField field; field.setStyle(s);
        TrailStamp out[TrailField::kCapacity];
        float x = 0, side = 1;
        for (int i = 0; i < 400; ++i) {
            field.step(x, 0, 0, side); side = -side; x += 0.7f;
            field.update(0.4f);
            check(field.liveCount() <= TrailField::kCapacity, tag + "buffer overran capacity");
            const int n = field.collect(out, TrailField::kCapacity);
            check(n == field.liveCount(), tag + "collect disagrees with liveCount");
            for (int k = 0; k < n; ++k) {
                check(out[k].age >= 0.0f && out[k].age < 1.0f, tag + "expired stamp still live");
                check(std::fabs(out[k].side) == 1.0f, tag + "side not normalised");
                if (k) check(out[k].age <= out[k-1].age + 1e-5f, tag + "stamps out of order");
            }
        }
        const int n = field.collect(out, TrailField::kCapacity);
        bool L = false, R = false;
        for (int k = 0; k < n; ++k) (out[k].side < 0 ? L : R) = true;
        check(L && R, tag + "prints only ever land on one side");

        for (int i = 0; i < 200; ++i) field.update(0.4f);
        check(field.liveCount() == 0, tag + "stamps never expire");
        field.step(5, 5, 0, 1); field.clear();
        check(field.liveCount() == 0, tag + "clear() left stamps behind");
        std::printf("   %-14s lifetime %5.1fs  scale %.2f  spread %.2fx  mark %u\n",
                    spec->id, spec->lifetime, spec->scale, spec->spread,
                    (unsigned)spec->mark);
    }
    std::printf("%s\n", failures ? "COSMETIC FAILURES" : "   cosmetic catalogue ok");
    return failures ? 1 : 0;
}
"""


def check_cosmetics() -> None:
    section("Cosmetic catalogue")
    with tempfile.TemporaryDirectory() as tmp:
        src = os.path.join(tmp, "probe.cpp")
        exe = os.path.join(tmp, "probe")
        open(src, "w").write(COSMETIC_PROBE)
        build = subprocess.run(
            ["g++", "-std=c++20", "-O2", "-Wall", "-Wextra", "-I", NATIVE, src,
             os.path.join(NATIVE, "Frame/Frame.cpp"),
             os.path.join(NATIVE, "Trail/Trail.cpp"), "-o", exe],
            capture_output=True, text=True)
        if build.returncode != 0:
            failures.append("cosmetic probe failed to compile:\n" + build.stderr[:2000])
            return
        run = subprocess.run([exe], capture_output=True, text=True)
        sys.stdout.write(run.stdout)
        if run.returncode != 0:
            failures.append("cosmetic catalogue probe reported failures")


# ===========================================================================
# 5. Inventory: duplicates, orphans, Title Case
# ===========================================================================

def check_inventory() -> None:
    section("Asset inventory")
    files = sorted(
        os.path.join(dp, f)
        for dp, _, fns in os.walk(ASSETS) for f in fns
    )
    by_hash: dict[str, list[str]] = {}
    total = 0
    for f in files:
        data = open(f, "rb").read()
        total += len(data)
        by_hash.setdefault(hashlib.sha256(data).hexdigest(), []).append(
            os.path.relpath(f, ASSETS))

    dupes = {h: ps for h, ps in by_hash.items() if len(ps) > 1}
    for h, ps in dupes.items():
        size = os.path.getsize(os.path.join(ASSETS, ps[0]))
        failures.append(
            f"byte-identical assets shipped twice ({size//1024} KB wasted): {', '.join(ps)}")

    # Anything under Assets/ that no source ever opens is dead weight in the APK.
    referenced = set()
    for kt in glob.glob(os.path.join(KOTLIN, "*.kt")):
        text = open(kt, encoding="utf-8").read()
        referenced.update(re.findall(r'"((?:Level_0|Models|Story)/[^"]+)"', text))
    # Story files are chosen by language tag at runtime.
    langs = {os.path.basename(p) for p in glob.glob(os.path.join(ASSETS, "Story/*.json"))}
    referenced.update(f"Story/{n}" for n in langs)

    # The Unity decoys under bin/Data are unreferenced by design — nothing in
    # the app opens them, which is the whole point. check_disguise() below is
    # what holds them to account instead.
    orphans = [os.path.relpath(f, ASSETS) for f in files
               if os.path.relpath(f, ASSETS).replace(os.sep, "/") not in referenced
               and not os.path.relpath(f, ASSETS).replace(os.sep, "/").startswith("bin/Data/")]
    for o in orphans:
        failures.append(f"asset never referenced by any source: {o}")

    print(f"   {len(files)} files, {total/1024:.0f} KB, "
          f"{len(dupes)} duplicate group(s), {len(orphans)} orphan(s)")


# Acronyms the game legitimately writes in capitals. In English they sit inside
# a mixed-case sentence and pass on their own, but a Japanese or Chinese label
# like "FPS上限" has no lowercase letter anywhere to prove it is not shouting —
# so the acronym is removed before the test rather than special-cased after it.
ACRONYMS = ("VIP", "FPS", "VHS", "HUD", "APK", "SFX", "UI", "HP", "PV", "OK",
            "ID", "XP", "MS")


def check_title_case() -> None:
    section("Title Case")
    bad = 0
    for p in sorted(glob.glob(os.path.join(RES, "values*/strings.xml"))):
        text = open(p, encoding="utf-8").read()
        for key, val in re.findall(r'<string name="([A-Za-z_0-9]+)">([^<]*)</string>', text):
            # `val == val.upper()` is trivially true for a script that has no
            # case at all, so Chinese and Japanese would report every single
            # line as shouting. Only judge a string that actually contains a
            # letter with two cases to choose between.
            body = val
            for a in ACRONYMS:
                body = body.replace(a, "")
            cased = [c for c in body if c.lower() != c.upper()]
            if len(val) > 2 and cased and body == body.upper():
                failures.append(f"{os.path.basename(os.path.dirname(p))}/{key} is ALL CAPS: {val}")
                bad += 1
    # Kotlin must not force it back on at the call site.
    for kt in glob.glob(os.path.join(KOTLIN, "*.kt")):
        text = open(kt, encoding="utf-8").read()
        for m in re.finditer(r"^.*\.uppercase\(\).*$", text, re.M):
            line = m.group(0).strip()
            # A language code and a single-letter avatar initial are legitimately
            # upper case; a label is not.
            if "language" in line or "lang.tag" in line or "take(1)" in line:
                continue
            failures.append(f"{os.path.basename(kt)}: forced uppercase on a label: {line[:90]}")
            bad += 1
    print(f"   {'ok' if bad == 0 else f'{bad} violation(s)'}")


# The costume's budget. Everything in it is dead weight in the APK by
# definition, so it has to stay small enough that the trade is obviously worth
# making. Fifty kilobytes is roughly one texture's worth of nothing.
DISGUISE_BUDGET_BYTES = 50 * 1024


README_LANGS_TAGS = [
    ("en", "English"), ("tr", "Türkçe"), ("de", "Deutsch"), ("es", "Español"),
    ("fr", "Français"), ("it", "Italiano"), ("pt", "Português"), ("ru", "Русский"),
    ("ja", "日本語"), ("zh", "中文"),
]

README_LANGS = [
    ("README.md",    "English"),
    ("README.tr.md", "Türkçe"),
    ("README.de.md", "Deutsch"),
    ("README.es.md", "Español"),
    ("README.fr.md", "Français"),
    ("README.it.md", "Italiano"),
    ("README.pt.md", "Português"),
    ("README.ru.md", "Русский"),
    ("README.ja.md", "日本語"),
    ("README.zh.md", "中文"),
]


def check_story() -> None:
    """
    Every story file that exists must be the same story.

    The loader merges per chapter against English, so a translation missing a
    chapter does not crash — it silently serves that one chapter in English in
    the middle of a Japanese read-through, which is the kind of thing nobody
    reports because it looks deliberate.

    `meta` is deliberately not compared: StoryFileMono declares only `version`
    and `chapters`, and the parser ignores unknown keys, so nothing in the game
    ever reads it. Three of these files have never had one and it has never
    mattered — asserting on it would be inventing work.

    A language with no file used to be tolerated here, because four of them
    legitimately had none and the per-chapter fallback covered it. All ten now
    exist, so that allowance has outlived its reason: a deleted file would be a
    regression, and the check that was written to keep the gap visible would
    have quietly welcomed it back. The story ships in the same ten languages the
    interface does, and that is now asserted.
    """
    section("Story")
    story = os.path.join(ASSETS, "Story")
    base_path = os.path.join(story, "en.json")
    if not os.path.exists(base_path):
        failures.append("Story/en.json is missing — there is nothing to fall back to")
        return
    base = json.load(open(base_path, encoding="utf-8"))

    def shape(doc):
        return {c["id"]: (len(c.get("paragraphs", [])), tuple(sorted(c)),
                          len(c.get("survival_tips", [])), len(c.get("entities", [])),
                          len(c.get("dialogues", [])), c.get("unlocked"))
                for c in doc["chapters"]}

    want = shape(base)
    present = set()
    for path in sorted(glob.glob(os.path.join(story, "*.json"))):
        tag = os.path.splitext(os.path.basename(path))[0]
        present.add(tag)
        try:
            doc = json.load(open(path, encoding="utf-8"))
        except json.JSONDecodeError as e:
            failures.append(f"Story/{tag}.json does not parse — {e}")
            continue
        got = shape(doc)
        check(doc.get("language") == tag,
              f"Story/{tag}.json declares language '{doc.get('language')}'")
        missing = sorted(set(want) - set(got))
        if missing:
            failures.append(f"Story/{tag}.json is missing chapter(s) {missing}")
        for cid in sorted(set(want) & set(got)):
            if want[cid] != got[cid]:
                failures.append(
                    f"Story/{tag}.json chapter {cid} does not match English: "
                    f"{got[cid]} vs {want[cid]}")
        print(f"   {tag}  {len(got)} chapters"
              f"{'' if not missing and all(want[c] == got[c] for c in set(want) & set(got)) else '  ← see failures'}")

    wanted = {t for t, _ in README_LANGS_TAGS}
    for tag in sorted(wanted - present):
        failures.append(
            f"Story/{tag}.json is missing — the interface ships in {tag} but the "
            f"story would silently read in English")
    for tag in sorted(present - wanted):
        failures.append(f"Story/{tag}.json is a language the interface does not offer")
    print(f"   {len(present)} languages, {len(want)} chapters each")


def check_readmes() -> None:
    """
    Ten READMEs that have to stay one document.

    GitHub renders README.md and nothing else — no content negotiation, no way
    to branch on Accept-Language — so a link bar is the only honest way to offer
    a translated page. (The game itself does detect the device language. That
    distinction is worth keeping straight and the English README states it.)

    The failure mode of a link bar is quiet and total: one file gets a new
    section, the other nine do not, and a reader in Japanese is looking at last
    month's project without any way to know. So the bar is checked to be
    complete and correctly self-marked in every file, and the section headings
    are checked to match, which is the cheapest proxy for "these are still the
    same document" that does not require reading the prose.
    """
    section("READMEs")
    names = [n for n, _ in README_LANGS]
    heads: dict[str, list[str]] = {}

    for name, endonym in README_LANGS:
        path = os.path.join(REPO, name)
        if not os.path.exists(path):
            failures.append(f"{name} is missing — the language bar links to it")
            continue
        text = open(path, encoding="utf-8").read()

        # Every other language must be one click away.
        for other, other_endonym in README_LANGS:
            if other == name:
                check(f"**{other_endonym}**" in text,
                      f"{name} does not mark {other_endonym} as the current language")
            else:
                check(f"]({other})" in text,
                      f"{name} does not link to {other} in its language bar")

        heads[name] = re.findall(r"^## (.+)$", text, re.M)
        print(f"   {name:14s} {len(text):6d} B  {len(heads[name])} sections")

    if "README.md" in heads:
        want = len(heads["README.md"])
        for name in names:
            if name in heads and len(heads[name]) != want:
                failures.append(
                    f"{name} has {len(heads[name])} sections, README.md has {want} "
                    f"— the translations have drifted out of step")

    fixes = {}
    for name in names:
        path = os.path.join(REPO, name)
        if not os.path.exists(path):
            continue
        text = open(path, encoding="utf-8").read()
        body = text.split("\n## ")
        for part in body:
            if part.startswith(("Recent fixes", "Son düzeltmeler", "Zuletzt behoben",
                                "Correcciones recientes", "Corrections récentes",
                                "Correzioni recenti", "Correções recentes",
                                "Недавние исправления", "最近の修正", "近期修复")):
                fixes[name] = len(re.findall(r"^- \*\*", part, re.M))
    if fixes:
        counts = set(fixes.values())
        check(len(counts) == 1,
              f"the fix lists are different lengths across languages: {fixes}")
        print(f"   {len(fixes)} fix lists, {counts.pop()} entries each")


# ===========================================================================
# 7. The character rig
# ===========================================================================

CHAR_MESH = os.path.join(ASSETS, "Models/Anime_Character.omesh")

# A vertex may move this far away from a neighbour it is joined to before the
# surface has visibly come apart. Measured, not chosen: the shipped rig's worst
# case across idle, walk and run is 2.9cm on a model one unit tall, and the
# four-armed rig this check was written for managed 11.2cm.
MAX_TEAR = 0.05


def _read_bone_table() -> tuple[list, list, list]:
    """Pull head/tail/radius straight out of Backrooms.kt.

    Parsed rather than copied so the check cannot drift away from the rig it is
    checking. A duplicated table would agree with itself forever.
    """
    src = open(os.path.join(KOTLIN, "Backrooms.kt"), encoding="utf-8").read()
    obj = src[src.index("internal object Skeleton {"):]
    obj = obj[:obj.index("\ninternal class PoseBuilder")]

    def triples(name: str) -> list:
        block = obj[obj.index(f"val {name} = arrayOf("):]
        block = block[:block.index("\n    )")]
        out = []
        for row in re.findall(r"floatArrayOf\(([^)]*)\)", block):
            out.append([float(v.strip().rstrip("f")) for v in row.split(",")])
        return out

    radius_block = obj[obj.index("val radius = floatArrayOf("):]
    radius_block = radius_block[:radius_block.index(")")]
    radius = [float(v.strip().rstrip("f"))
              for v in radius_block.split("(")[1].split(",") if v.strip()]
    return triples("head"), triples("tail"), radius


def _load_omesh(path: str):
    d = open(path, "rb").read()
    magic, _maj, _min, vc, ic = struct.unpack_from("<IHHII", d, 0)
    if magic != 0x48534D4F:
        raise ValueError(f"bad magic 0x{magic:08x}")
    stride = 8
    pos = [struct.unpack_from("<3f", d, 16 + (v * stride) * 4) for v in range(vc)]
    idx = struct.unpack_from(f"<{ic}H", d, 16 + vc * stride * 4)
    return pos, list(idx)


def _dist_to_capsule(p, a, b) -> float:
    abx, aby, abz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
    apx, apy, apz = p[0] - a[0], p[1] - a[1], p[2] - a[2]
    den = abx * abx + aby * aby + abz * abz
    t = 0.0 if den <= 1e-8 else max(0.0, min(1.0, (apx * abx + apy * aby + apz * abz) / den))
    dx, dy, dz = apx - abx * t, apy - aby * t, apz - abz * t
    return math.sqrt(dx * dx + dy * dy + dz * dz)


def _components(nodes, tris):
    par = list(range(len(nodes)))

    def find(a):
        while par[a] != a:
            par[a] = par[par[a]]
            a = par[a]
        return a

    for t in tris:
        a, b, c = (find(t[0]), find(t[1]), find(t[2]))
        if a != b:
            par[a] = b
        if find(t[1]) != find(t[2]):
            par[find(t[1])] = find(t[2])
    groups: dict[int, list[int]] = {}
    for i in range(len(nodes)):
        groups.setdefault(find(i), []).append(i)
    return sorted(groups.values(), key=len, reverse=True)


def check_character() -> None:
    """
    The rig, proved by animating it and looking for the seams.

    This exists because the character shipped with four arms and no tool could
    see it. The mesh held two of them: a body whose arms hang at its sides, and
    a dress whose sleeves stuck straight out in a T-pose. The bones had been
    laid along the sleeves, so the rig swung empty cloth while the arms the
    player sees stayed welded to the hips.

    Nothing structural was wrong — the file parsed, the bone count was right,
    the weights summed to one — so the only way to catch it is to do what the
    game does and then measure the result. Bind the mesh, run the skeleton
    through the poses the game actually uses, and check whether the surface
    comes apart. A rig whose bones are on the wrong geometry cannot survive
    that, and neither can a mesh that changes under a bone table nobody moved.
    """
    section("Character rig")
    if not os.path.exists(CHAR_MESH):
        failures.append("Anime_Character.omesh is missing")
        return

    head, tail, radius = _read_bone_table()
    bones = len(radius)
    check(len(head) == bones and len(tail) == bones,
          f"the bone table is ragged: {len(head)} heads, {len(tail)} tails, {bones} radii")
    if len(head) != bones or len(tail) != bones:
        return

    pos, idx = _load_omesh(CHAR_MESH)
    tris = [tuple(idx[i:i + 3]) for i in range(0, len(idx), 3)]

    # --- weld, exactly as bindMesh does -----------------------------------
    node_of, nodes, by_key = [], [], {}
    for p in pos:
        key = (round(p[0] * 10000), round(p[1] * 10000), round(p[2] * 10000))
        n = by_key.get(key)
        if n is None:
            n = len(nodes)
            by_key[key] = n
            nodes.append(p)
        node_of.append(n)
    wtris = [(node_of[a], node_of[b], node_of[c]) for a, b, c in tris]

    # --- duplicate shells --------------------------------------------------
    # Two shells of the same size a rigid millimetre apart are one shell
    # authored twice. They z-fight, and they cost real vertices to do it.
    comps = _components(nodes, wtris)
    dup = 0
    for i in range(len(comps)):
        for j in range(i + 1, len(comps)):
            if len(comps[i]) != len(comps[j]) or len(comps[i]) < 8:
                continue
            a = [nodes[k] for k in comps[i]]
            b = [nodes[k] for k in comps[j]]
            offs = [min(math.dist(p, q) for q in b) for p in a[:40]]
            if max(offs) < 0.004 and max(offs) - min(offs) < 1e-4:
                dup += 1
    check(dup == 0,
          f"{dup} shell(s) in Anime_Character.omesh are rigid copies of another "
          f"shell a millimetre away — they z-fight and cost vertices twice")
    print(f"   {len(pos)} verts, {len(tris)} tris, {len(nodes)} welded, "
          f"{len(comps)} shells, {dup} duplicated")

    # --- geodesic bind, as the game does it -------------------------------
    adj: list[list[tuple[int, float]]] = [[] for _ in nodes]
    seen = set()
    for a, b, c in wtris:
        for u, v in ((a, b), (b, c), (c, a)):
            if u == v:
                continue
            e = (u, v) if u < v else (v, u)
            if e in seen:
                continue
            seen.add(e)
            w = math.dist(nodes[u], nodes[v])
            adj[u].append((v, w))
            adj[v].append((u, w))

    import heapq
    INF = float("inf")
    geo = []
    for b in range(bones):
        d = [_dist_to_capsule(n, head[b], tail[b]) for n in nodes]
        seeds = [i for i, x in enumerate(d) if x <= 0.6 * radius[b]]
        if not seeds:
            seeds = [min(range(len(nodes)), key=lambda i: d[i])]
        dist = [INF] * len(nodes)
        h = []
        for s in seeds:
            dist[s] = d[s]
            h.append((d[s], s))
        heapq.heapify(h)
        while h:
            du, u = heapq.heappop(h)
            if du > dist[u]:
                continue
            for v, w in adj[u]:
                nd = du + w
                if nd < dist[v]:
                    dist[v] = nd
                    heapq.heappush(h, (nd, v))
        geo.append(dist)

    # A shell no bone's influence can crawl onto is bound rigidly to whichever
    # bone is nearest. For an eye or an eyelash sitting on the skull that is
    # exactly right. For anything further from its bone than that bone is wide
    # it is not: the geometry will swing about a pivot it does not belong to,
    # and it is the shape of the bug this check was written for — a sleeve
    # 29cm out on the X axis, rigidly attached to a chest bone 13cm wide.
    stranded = []
    for comp in comps:
        if all(any(geo[b][n] < INF for b in range(bones)) for n in comp):
            continue
        near, reach = None, INF
        for b in range(bones):
            far = max(_dist_to_capsule(nodes[n], head[b], tail[b]) for n in comp)
            if far < reach:
                near, reach = b, far
        if reach > radius[near]:
            stranded.append((len(comp), near, reach))
    for n, b, reach in stranded:
        failures.append(
            f"{n} vertices are out of reach of every bone and {reach * 100:.1f}cm "
            f"from bone {b}, the nearest — rigidly attached, they will swing "
            f"about a joint they are not part of")
    loose = sum(1 for n in range(len(nodes)) if all(geo[b][n] == INF for b in range(bones)))
    print(f"   {loose} vertex(es) rigidly bound (detail shells), {len(stranded)} stranded")

    wts, overreach = [], []
    for n in range(len(nodes)):
        row = []
        for b in range(bones):
            g = geo[b][n]
            if g == INF:
                continue
            s = 0.35 * radius[b]
            q = math.sqrt(g * g + s * s) / radius[b]
            row.append((1.0 / (q * q * q * q), b))
        row.sort(reverse=True)
        row = row[:4]
        tot = sum(w for w, _ in row)
        wts.append([(w / tot, b) for w, b in row] if tot > 1e-8 else [(1.0, 0)])
        if row:
            owner = row[0][1]
            overreach.append(
                (_dist_to_capsule(nodes[n], head[owner], tail[owner]) / radius[owner], n, owner))

    # A bone that has to reach more than twice its own radius through the air to
    # claim a vertex is not that vertex's bone. This is the test that sees a
    # limb the rig is not driving: when the dress's sleeves stood out in a
    # T-pose, 160 of their vertices were owned by a chest bone 13cm wide from
    # 29cm away, and nothing else about the file was wrong. Every vertex of a
    # rig whose bones sit on their own geometry stays under 1.4.
    overreach.sort(reverse=True)
    bad = [o for o in overreach if o[0] > 2.0]
    if bad:
        ratio, n, owner = bad[0]
        failures.append(
            f"{len(bad)} vertices are driven by a bone that is nowhere near them — "
            f"worst is {ratio:.1f}x bone {owner}'s radius away, at "
            f"{tuple(round(c, 3) for c in nodes[n])}. A limb the skeleton does not "
            f"actually cover will sit still while the rest of the body moves")
    print(f"   furthest vertex from its own bone: {overreach[0][0]:.2f}x that bone's radius")

    # --- animate, and look for the seams ----------------------------------
    worst, worst_at, torn = 0.0, None, 0
    for label, mats in _rig_poses(head, radius, bones):
        moved = []
        for n, p in enumerate(nodes):
            x = y = z = 0.0
            for w, b in wts[n]:
                m = mats[b]
                x += w * (m[0] * p[0] + m[1] * p[1] + m[2] * p[2] + m[3])
                y += w * (m[4] * p[0] + m[5] * p[1] + m[6] * p[2] + m[7])
                z += w * (m[8] * p[0] + m[9] * p[1] + m[10] * p[2] + m[11])
            moved.append((x, y, z))
        for u, v in seen:
            rest = math.dist(nodes[u], nodes[v])
            if rest <= 1e-4:
                continue
            grew = abs(math.dist(moved[u], moved[v]) - rest)
            if grew > 0.01:
                torn += 1
            if grew > worst:
                worst, worst_at = grew, (label, nodes[u])
    where = f" ({worst_at[0]} pose, near {tuple(round(c, 3) for c in worst_at[1])})" if worst_at else ""
    check(worst <= MAX_TEAR,
          f"the character comes apart when animated: an edge stretches "
          f"{worst * 100:.1f}cm{where}, over the {MAX_TEAR * 100:.0f}cm limit")
    print(f"   worst seam {worst * 100:.2f}cm{where}, {torn} edge(s) over 1cm")


def _rig_poses(head, radius, bones):
    """The poses the game drives, reduced to bone matrices.

    Only the arms and legs move here. That is deliberate: a rig fails at the
    joints, and a pose that does not bend anything proves nothing.
    """
    HIPS, SPINE, CHEST, HEAD = 0, 1, 2, 3
    LIMB = ((4, 5), (6, 7), (8, 9), (10, 11))
    parent = [-1, HIPS, SPINE, CHEST, CHEST, 4, CHEST, 6, HIPS, 8, HIPS, 10]

    def rot(rx, ry, rz):
        cx, sx = math.cos(rx), math.sin(rx)
        cy, sy = math.cos(ry), math.sin(ry)
        cz, sz = math.cos(rz), math.sin(rz)
        return [[cz * cy, cz * sy * sx - sz * cx, cz * sy * cx + sz * sx],
                [sz * cy, sz * sy * sx + cz * cx, sz * sy * cx - cz * sx],
                [-sy, cy * sx, cy * cx]]

    def compose(angles):
        mats = [None] * bones
        for b in range(bones):
            R = rot(*[math.radians(a) for a in angles[b]])
            h = head[b]
            t = [h[i] - sum(R[i][k] * h[k] for k in range(3)) for i in range(3)]
            L = [R[0][0], R[0][1], R[0][2], t[0],
                 R[1][0], R[1][1], R[1][2], t[1],
                 R[2][0], R[2][1], R[2][2], t[2]]
            p = parent[b]
            if p < 0:
                mats[b] = L
            else:
                P = mats[p]
                m = []
                for r in range(3):
                    for c in range(4):
                        v = sum(P[r * 4 + k] * L[k * 4 + c] for k in range(3))
                        if c == 3:
                            v += P[r * 4 + 3]
                        m.append(v)
                mats[b] = m
        return mats

    out = []
    for label, t, gait in (("idle", 0.15, 0.05), ("walk", 0.70, 1.0), ("run", 1.90, 1.6)):
        stride = t * 6.4
        ang = [[0.0, 0.0, 0.0] for _ in range(bones)]
        ang[SPINE] = [0.0, math.sin(stride + 0.4) * 2.5 * gait, 0.0]
        ang[CHEST] = [0.0, -math.sin(stride) * 4.5 * gait, 0.0]
        for side, (up, fore) in enumerate(LIMB[:2]):
            s = -1.0 if side == 0 else 1.0
            ph = stride + (math.pi if side else 0.0)
            ang[up] = [math.sin(ph) * 23.0 * gait, -11.0 * s, 0.0]
            ang[fore] = [math.sin(ph - 0.85) * 17.0 * gait + 6.0, 0.0, 0.0]
        for side, (thigh, shin) in enumerate(LIMB[2:]):
            s = -1.0 if side == 0 else 1.0
            ph = stride + (math.pi if side else 0.0)
            ang[thigh] = [math.sin(ph) * 30.0 * gait, 0.0, 3.0 * s * gait]
            ang[shin] = [-max(0.0, -math.sin(ph - 0.6)) * 24.0 * gait, 0.0, 0.0]
        out.append((label, compose(ang)))
    return out


def check_entity_silhouettes() -> None:
    """
    One creature, and the code that drew eight is actually gone.

    The roster was eight lore creatures cycled by the spawner, all drawing the
    same face and separated only by a tint the shader multiplies by 0.055. They
    were given eight silhouettes; then the call was made that Level 0 should
    hold one thing you never get a good look at, so seven were deleted outright.

    Deleted, not disabled — a roster with dead entries is a roster somebody
    re-enables by accident, and the per-type plumbing left behind (uType, an
    eight-arm tint table, a spawner indexing EntityType.entries) is exactly the
    kind of thing that quietly comes back. So this asserts the absence.
    """
    section("Entity")
    src = open(SRC_KT, encoding="utf-8").read()
    svc = open(os.path.join(KOTLIN, "Service.kt"), encoding="utf-8").read()

    roster = re.search(r"enum class EntityType\((.*?)\n\}", src, re.S)
    check(roster is not None, "EntityType roster not found")
    if not roster:
        return
    names = re.findall(r"^\s+([A-Z_]+)\s*\(", roster.group(1), re.M)
    check(names == ["SMILER"],
          f"the roster is {names}, not the single Smiler Level 0 is supposed to hold")

    frag = re.search(r"OMNI_BILLBOARD_FRAG = \"\"\"(.*?)\"\"\"", src, re.S)
    check(frag is not None, "the billboard fragment shader was not found")
    if not frag:
        return
    body = frag.group(1)

    check("uniform float uType" not in body and "uType" not in src,
          "uType is still plumbed through, so the shader can still branch per "
          "creature — there is only one")
    check(not re.search(r"EntityType\.entries", svc + src),
          "something still indexes EntityType.entries as if the roster were a list")

    # The smoke is the creature now, so the field that makes it has to be there.
    for token, why in (("fbm(", "no fractal noise, so the smoke is a single octave"),
                       ("curl", "no curl, so the smoke drifts in a straight line"),
                       ("density", "the body is coverage rather than density, which "
                                   "is what made the old one read as a cut-out")):
        check(token in body, f"the Smiler shader has {why}")
    print(f"   roster {names}, smoke field present, no per-type branching left")


def check_texture_sizes() -> None:
    """
    Every texture power-of-two and no larger than 1024.

    Not house style: a texture whose sides are not powers of two cannot carry a
    full mipmap chain, and without mipmaps a wall seen at a glancing angle
    aliases into a shimmer that no amount of filtering fixes. It also blocks
    every block-compressed GPU format. Floor.png was 1536x1024 and Wall.png
    1448x1086, which is 5.2MB of an APK spent on two textures that could not be
    mipmapped.

    1024 is the cap because nothing in this game is ever seen closer than that
    resolution can serve: the character is at most ~500px tall on screen, and
    the level textures tile every 3.2 metres.
    """
    section("Texture sizes")
    limit = 1024
    for path in sorted(glob.glob(os.path.join(ASSETS, "**/*.png"), recursive=True)):
        with open(path, "rb") as f:
            head = f.read(26)
        if head[:8] != b"\x89PNG\r\n\x1a\n":
            failures.append(f"{os.path.relpath(path, REPO)} is not a PNG")
            continue
        w, h = struct.unpack(">II", head[16:24])
        rel = os.path.relpath(path, ASSETS)
        po2 = w & (w - 1) == 0 and h & (h - 1) == 0
        size_kb = os.path.getsize(path) // 1024
        check(po2, f"{rel} is {w}x{h}, which is not a power of two — it cannot "
                   f"carry a mipmap chain, so it will shimmer at a distance")
        check(w <= limit and h <= limit,
              f"{rel} is {w}x{h}, over the {limit}x{limit} cap")
        print(f"   {rel:30s} {w}x{h}  {size_kb:5d} KB"
              f"{'' if po2 and w <= limit and h <= limit else '  ← see failures'}")


def check_shield() -> None:
    """
    Everything visible from outside must tell the same story.

    A disguise is worth nothing the moment it contradicts itself. A binary
    claiming Unity 2022.3.21f1 next to a boot.config claiming 2021.3.4f1, or a
    global-metadata.dat whose magic number is wrong, is louder than no disguise
    at all: it says somebody tried, which is an invitation.

    So: one version string, defined once in Shield/Unity.h, and asserted to
    appear byte for byte in every decoy. Plus the structural things a
    fingerprinting tool actually reads — the IL2CPP sanity magic, the presence
    of both libraries in the CMake build, the il2cpp_* export surface.

    None of this is protection and the note at the top of Shield/Unity.cpp
    says so. The detectors that ARE protection live beside it in Shield/.
    It is a filter on the front door, and a filter with a hole in it is a door.
    """
    section("Shield — Unity costume")
    header = os.path.join(NATIVE, "Shield/Unity.h")
    if not os.path.exists(header):
        failures.append("Shield/Unity.h is missing")
        return
    m = re.search(r'kUnityVersion\s*=\s*"([^"]+)"', open(header, encoding="utf-8").read())
    check(m is not None, "Shield/Unity.h does not define kUnityVersion")
    if not m:
        return
    version = m.group(1).encode()
    print(f"   version {version.decode()}")

    total = 0
    data = os.path.join(ASSETS, "bin/Data")
    for rel in ("boot.config", "globalgamemanagers",
                "il2cpp_data/Metadata/global-metadata.dat"):
        path = os.path.join(data, rel)
        if not os.path.exists(path):
            failures.append(f"decoy missing: bin/Data/{rel}")
            continue
        blob = open(path, "rb").read()
        total += len(blob)
        print(f"   bin/Data/{rel:44s} {len(blob):5d} B")

        if rel == "il2cpp_data/Metadata/global-metadata.dat":
            # The first eight bytes are the only part of this file anything
            # ever checks, and they are the part that has to be right.
            sanity, ver = struct.unpack("<Ii", blob[:8])
            check(sanity == 0xFAB11BAF,
                  f"global-metadata.dat sanity is 0x{sanity:08X}, IL2CPP writes 0xFAB11BAF")
            check(24 <= ver <= 31,
                  f"global-metadata.dat claims format version {ver}, which no Unity release emits")
        else:
            check(version in blob,
                  f"bin/Data/{rel} does not carry {version.decode()} — the decoys disagree")

    # The C++ side has to claim it too, or `strings` on the binary contradicts
    # the files sitting next to it.
    unity_cpp = open(os.path.join(NATIVE, "Shield/Unity.cpp"), encoding="utf-8").read()
    check(version.decode() in unity_cpp,
          "Shield/Unity.cpp does not embed the version from Unity.h")
    exports = re.findall(r"OMNI_EXPORT[^\n]*?\b(il2cpp_[a-z0-9_]+)\s*\(", unity_cpp)
    check(len(exports) >= 12,
          f"only {len(exports)} il2cpp_* exports; a real libil2cpp.so exports the whole C API")

    player = os.path.join(NATIVE, "Shield/Player.cpp")
    check(os.path.exists(player), "Shield/Player.cpp is missing — there is no libunity.so")
    cmake = open(os.path.join(NATIVE, "CMakeLists.txt"), encoding="utf-8").read()
    check("unity" in re.findall(r"add_library\(\s*(\w+)", cmake),
          "CMakeLists.txt does not build a libunity.so target")

    check(total <= DISGUISE_BUDGET_BYTES,
          f"the decoys total {total} B, over the {DISGUISE_BUDGET_BYTES} B budget")
    print(f"   {len(exports)} il2cpp_* exports, {total} B of decoys "
          f"({total * 100 // DISGUISE_BUDGET_BYTES}% of budget)")


def check_locales() -> None:
    """
    Every language must be whole.

    A half-translated locale is worse than no locale: Android falls back to the
    default per *string*, so a missing key shows English in the middle of a
    Japanese menu with nothing to warn you. Nobody notices until a player does.

    The format specifiers matter just as much. `getString` on a string with a
    stray %d and no argument throws, and it throws only in the language nobody
    on the team reads — which is exactly how the Turkish room-size label sat
    there rendering a literal "%d".

    The count of languages is asserted too, so silently dropping one is a
    failure rather than a quiet regression.
    """
    section("Locales")
    default = os.path.join(RES, "values/strings.xml")
    root = ET.parse(default).getroot()
    base = {e.get("name"): (e.text or "") for e in root.findall("string")}
    translatable = {e.get("name") for e in root.findall("string")
                    if e.get("translatable") != "false"}

    # AppLanguage is what the picker offers; the resources are what it can
    # actually resolve. They have to agree or a chip switches to nothing.
    service = open(os.path.join(KOTLIN, "Service.kt"), encoding="utf-8").read()
    enum = re.search(r"enum class AppLanguage\b.*?;", service, re.S)
    offered = re.findall(r'\(\s*"([a-z]{2})"\s*,', enum.group(0)) if enum else []

    spec = re.compile(r"%(?:\d+\$)?[a-zA-Z]")
    present = {"en"}                       # values/ is the English default
    for path in sorted(glob.glob(os.path.join(RES, "values-*/strings.xml"))):
        tag = os.path.basename(os.path.dirname(path)).split("-", 1)[1]
        if not re.fullmatch(r"[a-z]{2}", tag):
            continue                       # values-night and friends
        present.add(tag)
        try:
            loc = {e.get("name"): (e.text or "")
                   for e in ET.parse(path).getroot().findall("string")}
        except ET.ParseError as e:
            failures.append(f"{tag}: strings.xml does not parse — {e}")
            continue

        missing = sorted(translatable - set(loc))
        unknown = sorted(set(loc) - set(base))
        empty = sorted(k for k, v in loc.items() if not v.strip())
        drift = sorted(k for k in loc if k in base
                       and sorted(spec.findall(base[k])) != sorted(spec.findall(loc[k])))
        for label, items in (("untranslated", missing), ("not in the default", unknown),
                             ("empty", empty), ("format specifiers differ from English", drift)):
            if items:
                shown = ", ".join(items[:6]) + (f" (+{len(items) - 6} more)" if len(items) > 6 else "")
                failures.append(f"{tag}: {len(items)} {label}: {shown}")
        print(f"   {tag}  {len(loc):3d} strings"
              f"{'' if not (missing or unknown or empty or drift) else '  ← see failures'}")

    for tag in sorted(set(offered) - present):
        failures.append(f"AppLanguage offers '{tag}' but there is no values-{tag}/strings.xml")
    for tag in sorted(present - set(offered)):
        failures.append(f"values-{tag}/ exists but AppLanguage does not offer '{tag}'")

    check(len(present) == 10, f"the game is meant to ship 10 languages, found {len(present)}")
    print(f"   {len(present)} languages, {len(translatable)} translatable strings each")


# ===========================================================================
# 6. PNG optimiser (lossless)
# ===========================================================================

BPP = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}


def _png_parse(raw: bytes):
    if raw[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG")
    i, idat, ihdr, ancillary = 8, b"", None, 0
    while i < len(raw):
        ln = struct.unpack(">I", raw[i:i+4])[0]
        typ = raw[i+4:i+8]
        body = raw[i+8:i+8+ln]
        if typ == b"IHDR":
            ihdr = body
        elif typ == b"IDAT":
            idat += body
        elif typ != b"IEND":
            ancillary += ln + 12
        i += 12 + ln
    return ihdr, idat, ancillary


def _unfilter(ihdr: bytes, idat: bytes):
    w, h, bd, ct, _, _, il = struct.unpack(">IIBBBBB", ihdr)
    if bd != 8 or il != 0:
        raise ValueError(f"unsupported: bitdepth={bd} interlace={il}")
    bpp = BPP[ct]
    stride = w * bpp
    data = zlib.decompress(idat)
    rows, prev, p = [], bytearray(stride), 0
    for _ in range(h):
        f = data[p]; p += 1
        line = bytearray(data[p:p+stride]); p += stride
        if f == 1:
            for x in range(bpp, stride):
                line[x] = (line[x] + line[x-bpp]) & 0xFF
        elif f == 2:
            for x in range(stride):
                line[x] = (line[x] + prev[x]) & 0xFF
        elif f == 3:
            for x in range(stride):
                a = line[x-bpp] if x >= bpp else 0
                line[x] = (line[x] + ((a + prev[x]) >> 1)) & 0xFF
        elif f == 4:
            for x in range(stride):
                a = line[x-bpp] if x >= bpp else 0
                b = prev[x]
                c = prev[x-bpp] if x >= bpp else 0
                pa, pb, pc = abs(b-c), abs(a-c), abs(a+b-2*c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 0xFF
        rows.append(bytes(line))
        prev = line
    return w, h, ct, bpp, rows


def _refilter(rows, bpp, stride, filt):
    """Applies one filter type to every row. Trying a handful of whole-image
    filters and keeping the smallest beats per-row heuristics often enough, and
    it is the difference between a tool that runs in seconds and one that runs
    in minutes of pure-Python byte loops."""
    out = bytearray()
    prev = bytearray(stride)
    for line in rows:
        out.append(filt)
        if filt == 0:
            out += line
        elif filt == 1:
            enc = bytearray(line[:bpp])
            for x in range(bpp, stride):
                enc.append((line[x] - line[x-bpp]) & 0xFF)
            out += enc
        elif filt == 2:
            out += bytes((line[x] - prev[x]) & 0xFF for x in range(stride))
        elif filt == 3:
            enc = bytearray(stride)
            for x in range(stride):
                a = line[x-bpp] if x >= bpp else 0
                enc[x] = (line[x] - ((a + prev[x]) >> 1)) & 0xFF
            out += enc
        else:
            enc = bytearray(stride)
            for x in range(stride):
                a = line[x-bpp] if x >= bpp else 0
                b = prev[x]
                c = prev[x-bpp] if x >= bpp else 0
                pa, pb, pc = abs(b-c), abs(a-c), abs(a+b-2*c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                enc[x] = (line[x] - pr) & 0xFF
            out += enc
        prev = line
    return bytes(out)


def _emit(ihdr: bytes, comp: bytes) -> bytes:
    def chunk(typ: bytes, body: bytes) -> bytes:
        return (struct.pack(">I", len(body)) + typ + body +
                struct.pack(">I", zlib.crc32(typ + body) & 0xFFFFFFFF))
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) +
            chunk(b"IDAT", comp) + chunk(b"IEND", b""))


def optimise_png(path: str, dry_run: bool) -> tuple[int, int]:
    raw = open(path, "rb").read()
    ihdr, idat, ancillary = _png_parse(raw)
    w, h, ct, bpp, rows = _unfilter(ihdr, idat)
    stride = w * bpp

    best = None
    for filt in (0, 1, 2, 3, 4):
        filtered = _refilter(rows, bpp, stride, filt)
        for strategy in (zlib.Z_DEFAULT_STRATEGY, zlib.Z_FILTERED):
            co = zlib.compressobj(9, zlib.DEFLATED, 15, 9, strategy)
            comp = co.compress(filtered) + co.flush()
            if best is None or len(comp) < len(best):
                best = comp
    out = _emit(ihdr, best)

    # Never ship a "smaller" file that is not the same picture.
    _, ridat, _ = _png_parse(out)
    _, _, _, _, rrows = _unfilter(ihdr, ridat)
    if rrows != rows:
        raise AssertionError(f"{path}: re-encode changed the pixels — refusing")

    if len(out) < len(raw) and not dry_run:
        open(path, "wb").write(out)
    return len(raw), min(len(out), len(raw))


def run_optimise(dry_run: bool) -> None:
    section("PNG optimiser (lossless)" + (" — dry run" if dry_run else ""))
    pngs = sorted(glob.glob(os.path.join(ASSETS, "**/*.png"), recursive=True))
    before = after = 0
    for p in pngs:
        try:
            b, a = optimise_png(p, dry_run)
        except Exception as exc:
            print(f"   {os.path.relpath(p, ASSETS):28s} skipped: {exc}")
            continue
        before += b
        after += a
        pct = 100 * (1 - a / b) if b else 0
        print(f"   {os.path.relpath(p, ASSETS):28s} {b/1024:8.0f} KB -> {a/1024:8.0f} KB  ({pct:4.1f}%)")
    if before:
        print(f"   {'TOTAL':28s} {before/1024:8.0f} KB -> {after/1024:8.0f} KB  "
              f"({100*(1-after/before):4.1f}%, {(before-after)/1024:.0f} KB saved)")


# ===========================================================================

def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--optimise", action="store_true",
                    help="losslessly re-encode the PNGs under Assets/")
    ap.add_argument("--dry-run", action="store_true",
                    help="with --optimise, report the saving without writing")
    args = ap.parse_args()

    if args.optimise:
        run_optimise(args.dry_run)
        return 0

    check_drawables()
    check_mesh_uvs()
    check_camera()
    check_cosmetics()
    check_inventory()
    check_title_case()
    check_locales()
    check_character()
    check_entity_silhouettes()
    check_texture_sizes()
    check_shield()
    check_story()
    check_readmes()

    print()
    for f in failures:
        print("FAIL", f)
    print("PASSED" if not failures else
          f"FAILED ({len(failures)} failure{'' if len(failures) == 1 else 's'})")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
