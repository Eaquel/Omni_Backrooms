#!/usr/bin/env python3
"""
Vector drawable check.

Every lobby and HUD icon in this project is hand-written vector XML — there is
no design tool in the loop — and the failure mode is silent. A path with a typo
in it does not crash: aapt2 accepts it, the icon renders as a garbled shape or
as nothing at all, and the first anyone knows is when a button looks wrong on a
phone.

This asserts what can be checked without rendering:

  * the file is well-formed XML and is actually a <vector>;
  * it declares a viewport, and the declared size is square 24x24 like the rest
    of the set (a mismatched viewport silently rescales the artwork);
  * every <path> carries pathData, and every pathData starts with a move;
  * for paths written entirely in ABSOLUTE commands, every coordinate lands
    inside the viewport — artwork outside it is clipped away with no warning.

The absolute-only restriction matters. An arc's parameters are radii and flags,
and a relative command's numbers are deltas; treating either as a position
produces false positives, which is exactly what a first cut of this check did —
it flagged eight icons that were perfectly fine.

    python3 Tools/drawable_check.py
"""
import glob
import re
import sys
import xml.etree.ElementTree as ET

NS = "{http://schemas.android.com/apk/res/android}"
DRAWABLES = "Backrooms/Source/Main/res/drawable/*.xml"

failures = []


def check(ok, what):
    if not ok:
        failures.append(what)


# Path command letters. Lowercase is relative; A/a is an arc, whose numbers are
# radii and flags rather than positions.
ABSOLUTE_POS_CMDS = set("MLCSQTHV")
RELATIVE_OR_ARC = re.compile(r"[mlcsqtvhaz]|A")

NUM = re.compile(r"-?\d*\.?\d+(?:[eE][-+]?\d+)?")


def absolute_coords(d):
    """Coordinates from a path built only of absolute, non-arc commands.
    Returns None when the path uses anything we cannot interpret positionally."""
    if RELATIVE_OR_ARC.search(d):
        return None
    return [float(n) for n in NUM.findall(d)]


files = sorted(glob.glob(DRAWABLES))
if not files:
    print("no drawables found — wrong working directory?")
    sys.exit(1)

vectors = 0
absolute_checked = 0

for f in files:
    try:
        root = ET.parse(f).getroot()
    except Exception as e:
        failures.append(f"{f}: not well-formed XML: {e}")
        continue
    if not root.tag.endswith("vector"):
        continue
    vectors += 1

    vw, vh = root.get(NS + "viewportWidth"), root.get(NS + "viewportHeight")
    check(vw is not None and vh is not None, f"{f}: no viewport declared")
    if vw is None or vh is None:
        continue
    vw, vh = float(vw), float(vh)
    check(vw == 24.0 and vh == 24.0,
          f"{f}: viewport is {vw:g}x{vh:g}, not the 24x24 the rest of the set uses")

    paths = root.findall(NS + "path") + root.findall("path")
    check(len(paths) > 0, f"{f}: a vector with no paths draws nothing")

    for i, p in enumerate(paths):
        d = p.get(NS + "pathData")
        check(d is not None, f"{f}: path {i} has no pathData")
        if d is None:
            continue
        check(re.match(r"^\s*[Mm]", d) is not None,
              f"{f}: path {i} does not begin with a move")
        coords = absolute_coords(d)
        if coords is None:
            continue
        absolute_checked += 1
        check(len(coords) % 2 == 0,
              f"{f}: path {i} has an odd number of coordinates")
        outside = [c for c in coords if c < -0.01 or c > max(vw, vh) + 0.01]
        check(not outside,
              f"{f}: path {i} has {len(outside)} coordinate(s) outside the viewport "
              f"(first {outside[0] if outside else ''})")

print(f"{vectors} vector drawable(s) checked, "
      f"{absolute_checked} path(s) verified against the viewport\n")
for x in failures:
    print("FAIL", x)
print("PASSED" if not failures else f"FAILED ({len(failures)} failure"
                                    f"{'' if len(failures) == 1 else 's'})")
sys.exit(1 if failures else 0)
