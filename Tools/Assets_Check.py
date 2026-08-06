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

Plus an inventory pass: duplicate assets, unreferenced assets, and Title Case.

    python3 Tools/Assets_Check.py                 # check everything
    python3 Tools/Assets_Check.py --optimise      # losslessly shrink PNGs
    python3 Tools/Assets_Check.py --optimise --dry-run
"""
from __future__ import annotations

import argparse
import glob
import hashlib
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
        float widest = 0, narrow = 1e9f, minInner = 1e9f;
        for (int i = 0; i < S; ++i) {
            const float r = prof[i*2], th = prof[i*2+1];
            check(std::isfinite(r) && std::isfinite(th), tag + "non-finite profile");
            check(r > 0.0f && th > 0.0f, tag + "non-positive profile");
            widest = std::fmax(widest, r); narrow = std::fmin(narrow, r);
            minInner = std::fmin(minInner, r - th);
            check(th <= kInnerClearance * r + 1e-4f, tag + "tube thicker than the clearance allows");
        }
        check(std::fabs(widest - 1.0f) < 1e-3f, tag + "profile not normalised");
        check(minInner > 0.55f, tag + "inner edge closes in over the portrait");
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

    orphans = [os.path.relpath(f, ASSETS) for f in files
               if os.path.relpath(f, ASSETS).replace(os.sep, "/") not in referenced]
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

    print()
    for f in failures:
        print("FAIL", f)
    print("PASSED" if not failures else
          f"FAILED ({len(failures)} failure{'' if len(failures) == 1 else 's'})")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
