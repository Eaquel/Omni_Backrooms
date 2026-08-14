#!/usr/bin/env python3

import math
import struct
import sys
import zlib


class _Reader:

    def __init__(self, data):
        self.d = data
        self.p = 0

    def u8(self):
        v = self.d[self.p]
        self.p += 1
        return v

    def u32(self):
        v = struct.unpack_from("<I", self.d, self.p)[0]
        self.p += 4
        return v

    def u64(self):
        v = struct.unpack_from("<Q", self.d, self.p)[0]
        self.p += 8
        return v

    def raw(self, n):
        v = self.d[self.p:self.p + n]
        self.p += n
        return v


def _read_prop(r):
    t = chr(r.u8())
    if t == "Y":
        v = struct.unpack_from("<h", r.d, r.p)[0]
        r.p += 2
        return v
    if t == "C":
        return bool(r.u8())
    if t == "I":
        v = struct.unpack_from("<i", r.d, r.p)[0]
        r.p += 4
        return v
    if t == "F":
        v = struct.unpack_from("<f", r.d, r.p)[0]
        r.p += 4
        return v
    if t == "D":
        v = struct.unpack_from("<d", r.d, r.p)[0]
        r.p += 8
        return v
    if t == "L":
        v = struct.unpack_from("<q", r.d, r.p)[0]
        r.p += 8
        return v
    if t in "fdlib":
        count = r.u32()
        encoding = r.u32()
        clen = r.u32()
        buf = r.raw(clen)
        if encoding == 1:
            buf = zlib.decompress(buf)
        fmt = {"f": "f", "d": "d", "l": "q", "i": "i", "b": "?"}[t]
        return list(struct.unpack_from("<%d%s" % (count, fmt), buf, 0))
    if t in ("S", "R"):
        return r.raw(r.u32())
    raise ValueError("unknown FBX property type %r at offset %d" % (t, r.p))


def _read_node(r, version):
    if version >= 7500:
        end, nprops, _plen = r.u64(), r.u64(), r.u64()
    else:
        end, nprops, _plen = r.u32(), r.u32(), r.u32()
    name_len = r.u8()
    name = r.raw(name_len).decode("utf-8", "replace")

    if end == 0:
        return None

    props = [_read_prop(r) for _ in range(nprops)]
    children = []
    while r.p < end:
        child = _read_node(r, version)
        if child is None:
            break
        children.append(child)
    r.p = end
    return {"name": name, "props": props, "children": children}


def parse(path):
    data = open(path, "rb").read()
    if data[:20] != b"Kaydara FBX Binary  ":
        raise ValueError("not a binary FBX file: %s" % path)
    version = struct.unpack_from("<I", data, 23)[0]
    r = _Reader(data)
    r.p = 27
    roots = []
    while r.p < len(data) - 160:
        node = _read_node(r, version)
        if node is None:
            break
        roots.append(node)
    return version, roots


def walk(nodes, want=None, out=None):
    for n in nodes:
        if want is None or n["name"] in want:
            if out is not None:
                out.append(n)
        walk(n["children"], want, out)
    return out


def _clean(name):
    if isinstance(name, bytes):
        name = name.decode("utf-8", "replace")
    return name.split("\x00")[0]


def geometries(path):
    _version, roots = parse(path)
    objs = walk(roots, want={"Objects"}, out=[])
    if not objs:
        return []

    result = []
    for g in objs[0]["children"]:
        if g["name"] != "Geometry":
            continue
        rec = {"id": g["props"][0], "name": _clean(g["props"][1])}
        for c in g["children"]:
            if c["name"] == "Vertices":
                rec["v"] = c["props"][0]
            elif c["name"] == "PolygonVertexIndex":
                rec["pi"] = c["props"][0]
            elif c["name"] == "LayerElementUV":
                for u in c["children"]:
                    if u["name"] == "UV":
                        rec["uv"] = u["props"][0]
                    elif u["name"] == "UVIndex":
                        rec["uvi"] = u["props"][0]
                    elif u["name"] == "MappingInformationType":
                        rec["uvmap"] = u["props"][0].decode()
                    elif u["name"] == "ReferenceInformationType":
                        rec["uvref"] = u["props"][0].decode()
            elif c["name"] == "LayerElementNormal":
                for u in c["children"]:
                    if u["name"] == "Normals":
                        rec["n"] = u["props"][0]
                    elif u["name"] == "MappingInformationType":
                        rec["nmap"] = u["props"][0].decode()
        result.append(rec)
    return result


def models_and_connections(path):
    _version, roots = parse(path)
    objs = walk(roots, want={"Objects"}, out=[])
    cons = walk(roots, want={"Connections"}, out=[])

    models = {}
    if objs:
        for m in objs[0]["children"]:
            if m["name"] != "Model":
                continue
            translation, rotation, scaling = [0.0] * 3, [0.0] * 3, [1.0] * 3
            for c in m["children"]:
                if c["name"] not in ("Properties70", "Properties60"):
                    continue
                for p in c["children"]:
                    if not p["props"]:
                        continue
                    key = p["props"][0]
                    key = key.decode() if isinstance(key, bytes) else str(key)
                    vals = [x for x in p["props"] if isinstance(x, float)]
                    if len(vals) < 3:
                        continue
                    if key == "Lcl Translation":
                        translation = vals[-3:]
                    elif key == "Lcl Rotation":
                        rotation = vals[-3:]
                    elif key == "Lcl Scaling":
                        scaling = vals[-3:]
            models[m["props"][0]] = {
                "name": _clean(m["props"][1]),
                "t": translation,
                "r": rotation,
                "s": scaling,
            }

    parent = {}
    if cons:
        for c in cons[0]["children"]:
            if c["name"] != "C":
                continue
            p = c["props"]
            if len(p) >= 3 and p[0] in (b"OO", "OO"):
                parent[p[1]] = p[2]
    return models, parent


def animation_summary(path):
    _version, roots = parse(path)
    objs = walk(roots, want={"Objects"}, out=[])
    counts = {}
    if objs:
        for c in objs[0]["children"]:
            counts[c["name"]] = counts.get(c["name"], 0) + 1
    return {
        "AnimationStack": counts.get("AnimationStack", 0),
        "AnimationLayer": counts.get("AnimationLayer", 0),
        "AnimationCurve": counts.get("AnimationCurve", 0),
        "AnimationCurveNode": counts.get("AnimationCurveNode", 0),
        "Deformer": counts.get("Deformer", 0),
        "Geometry": counts.get("Geometry", 0),
        "Model": counts.get("Model", 0),
    }


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

                    pos = (px, pz, -py)
                    nrm = (nx, nz, -ny)
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
        return 1
    cmd = sys.argv[1]
    if cmd == "inspect" and len(sys.argv) >= 3:
        cmd_inspect(sys.argv[2])
    elif cmd == "verify" and len(sys.argv) >= 3:
        cmd_verify(sys.argv[2])
    elif cmd == "convert" and len(sys.argv) >= 4:
        mode = "height"
        if "--scale-mode" in sys.argv:
            mode = sys.argv[sys.argv.index("--scale-mode") + 1]
        convert(sys.argv[2], sys.argv[3], scale_mode=mode)
    else:
        print(__doc__)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
