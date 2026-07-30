"""
Minimal binary-FBX reader.

Written because the alternative was linking a full asset-import library into the
game just to read one character file. FBX is a node tree of typed properties; for
getting geometry out, that is only a few hundred lines.

Supports FBX 7.x binary (the version Blender exports). ASCII FBX is not handled.

Usage:
    from fbx_reader import parse, geometries, models_and_connections
"""

import struct
import zlib


class _Reader:
    """Cursor over the raw file bytes."""

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
    """One typed property. Array types may be zlib-deflated."""
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
    # Record headers widened to 64-bit in FBX 7500.
    if version >= 7500:
        end, nprops, _plen = r.u64(), r.u64(), r.u64()
    else:
        end, nprops, _plen = r.u32(), r.u32(), r.u32()
    name_len = r.u8()
    name = r.raw(name_len).decode("utf-8", "replace")

    # A zeroed header is the null record that terminates a child list.
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
    """Returns (version, [root nodes])."""
    data = open(path, "rb").read()
    if data[:20] != b"Kaydara FBX Binary  ":
        raise ValueError("not a binary FBX file: %s" % path)
    version = struct.unpack_from("<I", data, 23)[0]
    r = _Reader(data)
    r.p = 27
    roots = []
    # The trailing ~160 bytes are footer padding, not nodes.
    while r.p < len(data) - 160:
        node = _read_node(r, version)
        if node is None:
            break
        roots.append(node)
    return version, roots


def walk(nodes, want=None, out=None):
    """Depth-first collect of nodes whose name is in `want`."""
    for n in nodes:
        if want is None or n["name"] in want:
            if out is not None:
                out.append(n)
        walk(n["children"], want, out)
    return out


def _clean(name):
    """FBX object names are 'Name\\x00\\x01Class'; keep the readable part."""
    if isinstance(name, bytes):
        name = name.decode("utf-8", "replace")
    return name.split("\x00")[0]


def geometries(path):
    """Every Geometry node with its vertices, polygon indices, UVs and normals."""
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
    """Model transforms keyed by id, plus a child-id -> parent-id map."""
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
    """Counts animation nodes. Useful for answering 'does this file animate?'."""
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
