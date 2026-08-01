#!/usr/bin/env python3
"""
Omni Backrooms asset toolchain — single entry point.

Subcommands:
    inspect  <file.fbx>                 Report meshes, transforms and animation content
    convert  <file.fbx> <out.omesh>     Convert to the engine mesh format
    verify   <file.omesh>               Validate an existing .omesh

Examples:
    python3 omni_tools.py inspect Anime_Character.fbx
    python3 omni_tools.py convert Anime_Character.fbx character.omesh
    python3 omni_tools.py verify character.omesh
"""

import struct
import sys

from fbx_reader import geometries, models_and_connections, animation_summary
from fbx_to_omesh import convert, IMPLAUSIBLE_SPAN, _rotation_matrix, _apply, _span


def cmd_inspect(path):
    anim = animation_summary(path)
    print("Animation")
    for k in ("AnimationStack", "AnimationLayer", "AnimationCurve", "AnimationCurveNode"):
        print("  %-20s %d" % (k, anim[k]))
    if anim["AnimationCurve"] == 0:
        print("  -> no baked animation; the engine animates procedurally")

    print("\nRig")
    print("  %-20s %d" % ("Deformer", anim["Deformer"]))
    print("  %-20s %d" % ("Model", anim["Model"]))

    geos = geometries(path)
    models, parent = models_and_connections(path)
    print("\nMeshes")
    total_v = total_t = 0
    for g in geos:
        verts = g.get("v")
        if not verts:
            continue
        tris, run = 0, 0
        for i in g.get("pi", []):
            run += 1
            if i < 0:
                tris += run - 2
                run = 0
        nv = len(verts) // 3
        total_v += nv
        total_t += tris

        raw = [(verts[i], verts[i + 1], verts[i + 2]) for i in range(0, len(verts), 3)]
        model = models.get(parent.get(g["id"]))
        note = ""
        if model:
            rot = _rotation_matrix(model["r"])
            moved = [_apply(rot, model["t"], model["s"], p) for p in raw]
            if _span(moved) > IMPLAUSIBLE_SPAN:
                note = ("  <- transform corrupt (span %.0f); %s"
                        % (_span(moved),
                           "local geometry usable" if _span(raw) <= IMPLAUSIBLE_SPAN else "WILL BE DROPPED"))
        print("  %-16s vert=%-6d tri=%-6d uv=%-3s normals=%-3s%s"
              % (g["name"][:16], nv, tris,
                 "yes" if "uv" in g else "no",
                 "yes" if "n" in g else "no", note))
    print("\n  total: %d vertices, %d triangles" % (total_v, total_t))


def cmd_verify(path):
    data = open(path, "rb").read()
    if data[:4] != b"OMSH":
        raise SystemExit("not an .omesh file (bad magic)")
    major, minor = struct.unpack_from("<HH", data, 4)
    vcount, icount = struct.unpack_from("<II", data, 8)
    wide = vcount > 65535
    expected = 16 + vcount * 32 + icount * (4 if wide else 2)
    print("magic    OMSH  v%d.%d" % (major, minor))
    print("vertices %d" % vcount)
    print("indices  %d  (%d triangles, %d-bit)" % (icount, icount // 3, 32 if wide else 16))
    print("size     %d bytes (expected %d) %s"
          % (len(data), expected, "OK" if len(data) == expected else "MISMATCH"))
    if len(data) != expected:
        raise SystemExit(1)

    xs, ys, zs = [], [], []
    for i in range(vcount):
        off = 16 + i * 32
        x, y, z = struct.unpack_from("<3f", data, off)
        xs.append(x); ys.append(y); zs.append(z)
    print("bounds   X[%.3f, %.3f]  Y[%.3f, %.3f]  Z[%.3f, %.3f]"
          % (min(xs), max(xs), min(ys), max(ys), min(zs), max(zs)))

    # Indices must be in range or the GPU will read garbage.
    fmt = "<I" if wide else "<H"
    step = 4 if wide else 2
    base = 16 + vcount * 32
    bad = 0
    for i in range(icount):
        idx = struct.unpack_from(fmt, data, base + i * step)[0]
        if idx >= vcount:
            bad += 1
    print("indices  %s" % ("all in range" if bad == 0 else "%d OUT OF RANGE" % bad))
    if bad:
        raise SystemExit(1)


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        raise SystemExit(1)
    cmd = sys.argv[1]
    if cmd == "inspect" and len(sys.argv) >= 3:
        cmd_inspect(sys.argv[2])
    elif cmd == "convert" and len(sys.argv) >= 4:
        convert(sys.argv[2], sys.argv[3])
    elif cmd == "verify" and len(sys.argv) >= 3:
        cmd_verify(sys.argv[2])
    else:
        print(__doc__)
        raise SystemExit(1)


if __name__ == "__main__":
    main()
