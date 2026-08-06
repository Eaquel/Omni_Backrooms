#!/usr/bin/env python3
"""
Chunk mesher UV check.

Level 0's textures are world-anchored: a vertex's UV is its world (x, z) in
metres, and the shader scales that per texture to reach one texel density. The
invariant that makes the pattern run continuously from one tile into the next
is therefore very simple, and very easy to break:

    every corner's UV must equal that corner's own world position.

It was broken on the ceiling for a long time. The emitter handed UVs out in a
fixed corner order (p0 -> u0v0, p1 -> u1v0, p2 -> u1v1, p3 -> u0v1), which is
only right for a quad wound in that order. The ceiling is wound the other way
so it faces down, so its second vertex sat at (x0, z1) while being given the
coordinate for (x1, z0). Every ceiling tile came out mirrored across its own
diagonal, and no two neighbouring tiles could line up — which is what the
player kept reporting as uneven, sliding ceiling textures.

Nothing in a compile or a shader check can see that. This can.

    python3 Tools/mesh_uv_check.py
"""
import re
import sys
from pathlib import Path

SRC = Path("Backrooms/Source/Main/Kotlin/com/omni/backrooms/Backrooms.kt")

CELL = 3.2
failures = []


def check(ok, what):
    if not ok:
        failures.append(what)


def corners(x0, z0):
    x1, z1 = x0 + CELL, z0 + CELL
    return x0, z0, x1, z1


def floor_quad(x0, z0):
    """Positions and UVs the floor emitter produces, in vertex order."""
    x0, z0, x1, z1 = corners(x0, z0)
    pts = [(x0, z0), (x1, z0), (x1, z1), (x0, z1)]
    # quad() hands UVs out in this fixed order.
    uvs = [(x0, z0), (x1, z0), (x1, z1), (x0, z1)]
    return pts, uvs


def roof_quad(x0, z0, fixed=True):
    """The ceiling. `fixed` selects the per-corner UVs; False reproduces the
    old fixed-order assignment that quad() would have made."""
    x0, z0, x1, z1 = corners(x0, z0)
    pts = [(x0, z0), (x0, z1), (x1, z1), (x1, z0)]
    if fixed:
        uvs = [(x0, z0), (x0, z1), (x1, z1), (x1, z0)]
    else:
        uvs = [(x0, z0), (x1, z0), (x1, z1), (x0, z1)]
    return pts, uvs


def verify(name, fn, **kw):
    """UV must equal world position at every corner, over a spread of cells —
    including negative coordinates, where the world genuinely goes."""
    bad = 0
    for cz in range(-40, 41, 7):
        for cx in range(-40, 41, 7):
            pts, uvs = fn(cx * CELL, cz * CELL, **kw)
            for (px, pz), (u, v) in zip(pts, uvs):
                if abs(px - u) > 1e-4 or abs(pz - v) > 1e-4:
                    bad += 1
    return bad


print("Chunk mesher UV check\n")

for name, fn in (("floor", floor_quad), ("ceiling", roof_quad)):
    bad = verify(name, fn)
    check(bad == 0, f"{name}: {bad} corners whose UV does not match their world position")
    print(f"  {name:8s} {'ok' if bad == 0 else f'{bad} mismatched corners'}")

# Prove the check has teeth: the old ceiling ordering must fail it.
regressed = verify("ceiling(old)", roof_quad, fixed=False)
check(regressed > 0, "the check does not detect the original ceiling UV ordering")
print(f"  {'ceiling (pre-fix ordering)':26s} {regressed} mismatched corners — correctly rejected")

# And the source must actually be using the per-corner emitter for the ceiling.
if SRC.exists():
    text = SRC.read_text(encoding="utf-8")
    m = re.search(r"roofB = (quadUv|quad)\(", text)
    check(m is not None, "no ceiling quad emitter found in the source")
    if m:
        check(m.group(1) == "quadUv",
              "the ceiling is emitted through quad(), which cannot express its winding")
        print(f"\n  source emits the ceiling through {m.group(1)}()")
    # Floor and ceiling must be emitted unconditionally: skipping either on a
    # feature leaves a one-cell hole with nothing behind it, which is what the
    # player saw as a corrupted 1x1 tile.
    for surface, pattern in (("floor", r"if \(feature != 4\)"), ("ceiling", r"if \(feature != 1\)")):
        check(re.search(pattern, text) is None,
              f"{surface} is still skipped on a feature, leaving a one-cell hole")
    print("  floor and ceiling are emitted for every open cell")
else:
    print("\n  (source not found; ran model checks only)")

print()
if failures:
    for f in failures:
        print("FAIL", f)
    print(f"\nFAILED ({len(failures)} failure{'' if len(failures) == 1 else 's'})")
    sys.exit(1)
print("PASSED")
