#!/usr/bin/env python3
"""离线复现 GeoMap.getZ：列出 (2157.0432, 277.79797) 处 210050000 的所有碰撞面。

数据源：src/main/resources/aion/geo/models.mesh + geo/210050000.geo.gz
格式复现自 GeoWorldLoader.loadMeshs / loadWorldObjects + Geometry.setTransform。

只读；用于判断“石头”是否存在于服务端 geo 碰撞面中。
用法：python3 .agents/summary/inggison-somation-rock-z/geo_surface_probe.py
"""
import gzip
import os
import struct
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
GEO = os.path.join(ROOT, "src/main/resources/aion/geo")
WORLD = int(sys.argv[1]) if len(sys.argv) > 1 else 210050000
PX = float(sys.argv[2]) if len(sys.argv) > 2 else 2157.0432
PY = float(sys.argv[3]) if len(sys.argv) > 3 else 277.79797
ZMIN = float(sys.argv[4]) if len(sys.argv) > 4 else 380.0
ZMAX = float(sys.argv[5]) if len(sys.argv) > 5 else 520.0
PHYSICAL = 1


def parse_models(path, wanted):
    """返回 {name: [ (verts[float], tris[tuple3], material, intentions) ]}（wanted 为 name 集合）。"""
    out = {}
    with open(path, "rb") as f:
        data = f.read()
    pos = 0
    total = len(data)
    while pos < total:
        (name_len,) = struct.unpack_from(">H", data, pos)
        pos += 2
        name = data[pos:pos + name_len].decode("utf-8", "replace")
        pos += name_len
        (model_count,) = struct.unpack_from(">B", data, pos)
        pos += 1
        models = []
        for _ in range(model_count):
            (vc,) = struct.unpack_from(">H", data, pos)
            pos += 2
            n_floats = vc * 3
            verts = struct.unpack_from(">%df" % n_floats, data, pos)
            pos += 4 * n_floats
            (tc,) = struct.unpack_from(">H", data, pos)
            pos += 2
            index_size = data[pos]
            pos += 1
            n_idx = tc * 3
            if index_size == 1:
                idx = struct.unpack_from(">%dB" % n_idx, data, pos)
                pos += n_idx
            else:
                idx = struct.unpack_from(">%dH" % n_idx, data, pos)
                pos += 2 * n_idx
            material = data[pos]
            intentions = data[pos + 1]
            pos += 2
            models.append((verts, idx, material, intentions))
        key = name.lower()
        if key in wanted:
            out[key] = models
    return out


def parse_placements(path):
    with gzip.open(path, "rb") as f:
        data = f.read()
    pos, total = 0, len(data)
    while pos < total:
        (name_len,) = struct.unpack_from(">H", data, pos)
        pos += 2
        name = data[pos:pos + name_len].decode("utf-8", "replace")
        pos += name_len
        loc = struct.unpack_from(">3f", data, pos)
        pos += 12
        matrix = struct.unpack_from(">9f", data, pos)
        pos += 36
        scale = struct.unpack_from(">3f", data, pos)
        pos += 12
        otype = data[pos]
        _id = struct.unpack_from(">H", data, pos + 1)[0]
        _level = data[pos + 3]
        pos += 4
        yield name, loc, matrix, scale, otype


def rotate(m, v):
    x, y, z = v
    return (m[0] * x + m[1] * y + m[2] * z,
            m[3] * x + m[4] * y + m[5] * z,
            m[6] * x + m[7] * y + m[8] * z)


def tris_at(loc, matrix, scale, verts, idx):
    """返回包含 (PX,PY) 的三角形 (z, area) 列表。"""
    world = []
    for i in range(0, len(verts), 3):
        v = (verts[i] * scale[0], verts[i + 1] * scale[1], verts[i + 2] * scale[2])
        r = rotate(matrix, v)
        world.append((r[0] + loc[0], r[1] + loc[1], r[2] + loc[2]))
    hits = []
    for t in range(0, len(idx), 3):
        a, b, c = world[idx[t]], world[idx[t + 1]], world[idx[t + 2]]
        # 退化/垂直三角形跳过
        den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
        if abs(den) < 1e-9:
            continue
        l1 = ((b[1] - c[1]) * (PX - c[0]) + (c[0] - b[0]) * (PY - c[1])) / den
        l2 = ((c[1] - a[1]) * (PX - c[0]) + (a[0] - c[0]) * (PY - c[1])) / den
        l3 = 1.0 - l1 - l2
        eps = 1e-6
        if l1 < -eps or l2 < -eps or l3 < -eps:
            continue
        z = l1 * a[2] + l2 * b[2] + l3 * c[2]
        if ZMIN <= z <= ZMAX:
            hits.append((z, t // 3))
    return hits


def main():
    wanted = set()
    placements = []
    for name, loc, matrix, scale, otype in parse_placements(os.path.join(GEO, "%d.geo.gz" % WORLD)):
        wanted.add(name.lower())
        placements.append((name, loc, matrix, scale, otype))
    print("placed objects: %d, distinct meshes: %d" % (len(placements), len(wanted)))

    models = parse_models(os.path.join(GEO, "models.mesh"), wanted)
    print("meshes resolved: %d / %d" % (len(models), len(wanted)))

    rows = []
    for name, loc, matrix, scale, otype in placements:
        key = name.lower()
        mods = models.get(key)
        if not mods:
            continue
        for mi, (verts, idx, material, intentions) in enumerate(mods):
            if not (intentions & PHYSICAL):
                continue
            for z, tri in tris_at(loc, matrix, scale, verts, idx):
                rows.append((z, name, mi, material, tri, tuple(round(v, 3) for v in loc)))

    rows.sort()
    print("\nsurfaces overlapping (%.4f, %.4f) in [%.0f, %.0f]:" % (PX, PY, ZMIN, ZMAX))
    prev = None
    for z, name, mi, material, tri, loc in rows:
        if prev is not None and abs(z - prev) < 1e-4:
            continue
        prev = z
        print("  z=%10.5f  mesh=%-42s model=%d mat=%3d tri=%-5d loc=%s" % (z, name, mi, material, tri, loc))
    print("\ntotal triangle hits: %d" % len(rows))


if __name__ == "__main__":
    sys.setrecursionlimit(10000)
    main()
