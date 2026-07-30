#!/usr/bin/env python3
"""
Convert a binary FBX into the engine's .omesh format.

.omesh layout (little-endian):
    magic   : 4 bytes  "OMSH"
    version : u16 major, u16 minor
    counts  : u32 vertex_count, u32 index_count
    vertices: vertex_count * 8 floats  (px py pz  nx ny nz  u v)
    indices : index_count * (u16 if vertex_count <= 65535 else u32)

Two things this handles that a naive exporter does not:

1. Corrupt Model transforms. Blender/FBX round-trips sometimes emit a scale of
   tens of thousands on an object whose geometry is already correctly placed.
   Applying that transform throws the mesh kilometres away. So each mesh is
   tried with its transform, and if the result is implausible next to the rest
   of the model, it is retried with identity — which is what rescues the suit
   mesh in the sample character. Only if both fail is the mesh dropped, which
   correctly discards genuine strays like a leftover scene cube.

2. Axis conversion. Blender is Z-up, the engine is Y-up, so positions and
   normals are remapped (x, y, z) -> (x, z, -y).

Usage:
    python3 fbx_to_omesh.py input.fbx output.omesh [--scale-mode height|raw]
"""

import math
import struct
import sys

from fbx_reader import geometries, models_and_connections, animation_summary

# A mesh whose largest dimension exceeds this, while the character is ~2 units,
# is considered mis-transformed rather than merely large.
IMPLAUSIBLE_SPAN = 10.0


def _rotation_matrix(degrees):
    rx, ry, rz = [math.radians(a) for a in degrees]
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    mx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    my = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    mz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]

    def mul(a, b):
        return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]

    return mul(mz, mul(my, mx))


def _apply(rot, trans, scale, p, translate=True):
    x, y, z = p[0] * scale[0], p[1] * scale[1], p[2] * scale[2]
    o = (
        rot[0][0] * x + rot[0][1] * y + rot[0][2] * z,
        rot[1][0] * x + rot[1][1] * y + rot[1][2] * z,
        rot[2][0] * x + rot[2][1] * y + rot[2][2] * z,
    )
    if translate:
        return (o[0] + trans[0], o[1] + trans[1], o[2] + trans[2])
    return o


def _span(points):
    return max(max(c) - min(c) for c in zip(*points))


def _resolve_transform(geo, model, verbose):
    """Pick a usable transform for one mesh, or None to drop it."""
    verts = geo["v"]
    raw = [(verts[i], verts[i + 1], verts[i + 2]) for i in range(0, len(verts), 3)]

    identity = ([[1, 0, 0], [0, 1, 0], [0, 0, 1]], [0.0, 0.0, 0.0], [1.0, 1.0, 1.0])

    if model is not None:
        rot = _rotation_matrix(model["r"])
        candidate = (rot, model["t"], model["s"])
        transformed = [_apply(rot, model["t"], model["s"], p) for p in raw]
        if _span(transformed) <= IMPLAUSIBLE_SPAN:
            return candidate, transformed

        if verbose:
            print("    transform rejected (span %.0f); retrying with identity"
                  % _span(transformed))

    # Fall back to the raw local geometry.
    if _span(raw) <= IMPLAUSIBLE_SPAN:
        if verbose:
            print("    identity accepted (local geometry already positioned)")
        return identity, raw

    if verbose:
        print("    dropped: implausible in both local and transformed space")
    return None, None


def convert(fbx_path, out_path, scale_mode="height", verbose=True):
    anim = animation_summary(fbx_path)
    if verbose:
        print("Animation content: %d stacks, %d curves"
              % (anim["AnimationStack"], anim["AnimationCurve"]))
        if anim["AnimationCurve"] == 0:
            print("  -> no baked animation in this file; the engine will drive "
                  "motion procedurally")

    geos = geometries(fbx_path)
    models, parent = models_and_connections(fbx_path)

    vertices = []
    indices = []
    lookup = {}
    kept, dropped = [], []

    for geo in geos:
        if not geo.get("v") or not geo.get("pi"):
            continue
        name = geo["name"]
        if verbose:
            print("  %s" % name)

        model = models.get(parent.get(geo["id"]))
        transform, positions = _resolve_transform(geo, model, verbose)
        if transform is None:
            dropped.append(name)
            continue
        kept.append(name)
        rot, trans, scale = transform

        normals = geo.get("n")
        uvs = geo.get("uv")
        uv_index = geo.get("uvi")
        uv_ref = geo.get("uvref", "IndexToDirect")

        polygon = []
        for corner, raw_index in enumerate(geo["pi"]):
            is_last = raw_index < 0
            vertex_index = ~raw_index if is_last else raw_index
            polygon.append((vertex_index, corner))
            if not is_last:
                continue

            # Fan-triangulate the polygon.
            for t in range(1, len(polygon) - 1):
                for vidx, pv in (polygon[0], polygon[t], polygon[t + 1]):
                    px, py, pz = positions[vidx]

                    if normals and len(normals) > pv * 3 + 2:
                        nx, ny, nz = _apply(
                            rot, trans, scale,
                            (normals[pv * 3], normals[pv * 3 + 1], normals[pv * 3 + 2]),
                            translate=False,
                        )
                        length = math.sqrt(nx * nx + ny * ny + nz * nz) or 1.0
                        nx, ny, nz = nx / length, ny / length, nz / length
                    else:
                        nx, ny, nz = 0.0, 0.0, 1.0

                    if uvs:
                        ui = uv_index[pv] if (uv_index and uv_ref != "Direct" and pv < len(uv_index)) else pv
                        u, v = (uvs[ui * 2], uvs[ui * 2 + 1]) if ui * 2 + 1 < len(uvs) else (0.0, 0.0)
                    else:
                        u = v = 0.0

                    # Z-up (Blender) -> Y-up (engine).
                    pos = (px, pz, -py)
                    nrm = (nx, nz, -ny)
                    # V is flipped for GL's texture origin.
                    key = (
                        round(pos[0], 5), round(pos[1], 5), round(pos[2], 5),
                        round(nrm[0], 4), round(nrm[1], 4), round(nrm[2], 4),
                        round(u, 5), round(v, 5),
                    )
                    j = lookup.get(key)
                    if j is None:
                        j = len(vertices)
                        lookup[key] = j
                        vertices.append((pos[0], pos[1], pos[2], nrm[0], nrm[1], nrm[2], u, 1.0 - v))
                    indices.append(j)
            polygon = []

    if not vertices:
        raise SystemExit("no usable geometry found")

    if scale_mode == "height":
        ys = [v[1] for v in vertices]
        xs = [v[0] for v in vertices]
        zs = [v[2] for v in vertices]
        height = max(ys) - min(ys)
        s = 1.0 / height if height > 1e-6 else 1.0
        cx = (min(xs) + max(xs)) / 2.0
        cz = (min(zs) + max(zs)) / 2.0
        floor = min(ys)
        vertices = [
            ((v[0] - cx) * s, (v[1] - floor) * s, (v[2] - cz) * s, v[3], v[4], v[5], v[6], v[7])
            for v in vertices
        ]

    wide = len(vertices) > 65535
    blob = bytearray(b"OMSH")
    blob += struct.pack("<HH", 1, 0)
    blob += struct.pack("<II", len(vertices), len(indices))
    for v in vertices:
        blob += struct.pack("<8f", *v)
    fmt = "<I" if wide else "<H"
    for i in indices:
        blob += struct.pack(fmt, i)

    open(out_path, "wb").write(bytes(blob))

    if verbose:
        print("\nkept:    %s" % ", ".join(kept))
        print("dropped: %s" % (", ".join(dropped) if dropped else "none"))
        print("wrote %s: %d vertices, %d triangles, %d bytes, %d-bit indices"
              % (out_path, len(vertices), len(indices) // 3, len(blob), 32 if wide else 16))
    return len(vertices), len(indices) // 3


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print(__doc__)
        raise SystemExit(1)
    mode = "height"
    if "--scale-mode" in sys.argv:
        mode = sys.argv[sys.argv.index("--scale-mode") + 1]
    convert(sys.argv[1], sys.argv[2], scale_mode=mode)
