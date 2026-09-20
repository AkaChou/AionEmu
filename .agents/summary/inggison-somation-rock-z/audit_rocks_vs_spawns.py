#!/usr/bin/env python3
"""审计：作者 Z 明显高于地形的刷点，其脚下是否有 geo 碰撞网格面（岩石/平台/桥面/地板）。

复现 SpawnEngine.projectedSpawnZ 的输入（PATH 由 audit_path_projection 复现），
再用 numpy 复现 GeoMap.getZ 的向下投射（PHYSICAL 碰撞面 + 地形高度图），
找出“作者把 NPC 摆在网格碰撞面上、但地形兜底会把它压到下层地面”的刷点。

用法：python3 .agents/summary/inggison-somation-rock-z/audit_rocks_vs_spawns.py [world_id ...]
"""
import gzip
import importlib.util
import math
import os
import re
import struct
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
AUDIT = os.path.join(HERE, "..", "spawn-z-audit")
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
GEO = os.path.join(ROOT, "src/main/resources/aion/geo")
SPAWNS = os.path.join(ROOT, "src/main/resources/aion/data/static_data/spawns/Npcs")
PHYSICAL = 1
DELTA = 1.0        # 作者 Z 与碰撞面的贴合容差（米）
TERRAIN_GAP = 1.0  # 作者 Z 高于地形的判定阈值（米）


def load_module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


pp = load_module("audit_path_projection", os.path.join(AUDIT, "audit_path_projection.py"))


def parse_placements(world):
    path = os.path.join(GEO, "%d.geo.gz" % world)
    if not os.path.isfile(path):
        return []
    with gzip.open(path, "rb") as f:
        data = f.read()
    pos, total = 0, len(data)
    out = []
    while pos < total:
        (n,) = struct.unpack_from(">H", data, pos)
        pos += 2
        name = data[pos:pos + n].decode("utf-8", "replace")
        pos += n
        loc = struct.unpack_from(">3f", data, pos)
        pos += 12
        matrix = struct.unpack_from(">9f", data, pos)
        pos += 36
        scale = struct.unpack_from(">3f", data, pos)
        pos += 12
        pos += 4  # type + id(u16) + level
        out.append((name.lower(), loc, matrix, scale))
    return out


def parse_models(wanted):
    out = {}
    with open(os.path.join(GEO, "models.mesh"), "rb") as f:
        data = f.read()
    pos, total = 0, len(data)
    while pos < total:
        (n,) = struct.unpack_from(">H", data, pos)
        pos += 2
        name = data[pos:pos + n].decode("utf-8", "replace")
        pos += n
        (mc,) = struct.unpack_from(">B", data, pos)
        pos += 1
        models = []
        for _ in range(mc):
            (vc,) = struct.unpack_from(">H", data, pos)
            pos += 2
            nf = vc * 3
            verts = np.frombuffer(data, dtype=">f4", count=nf, offset=pos).astype(np.float32).reshape(-1, 3)
            pos += 4 * nf
            (tc,) = struct.unpack_from(">H", data, pos)
            pos += 2
            isz = data[pos]
            pos += 1
            ni = tc * 3
            if isz == 1:
                idx = np.frombuffer(data, dtype=np.uint8, count=ni, offset=pos).astype(np.int32)
                pos += ni
            else:
                idx = np.frombuffer(data, dtype=">u2", count=ni, offset=pos).astype(np.int32)
                pos += 2 * ni
            material = data[pos]
            intentions = data[pos + 1]
            pos += 2
            models.append((verts, idx.reshape(-1, 3), material, intentions))
        key = name.lower()
        if key in wanted:
            out[key] = models
    return out


def world_triangles(world, placements, models):
    tris = []
    names = []
    for name, loc, matrix, scale in placements:
        mods = models.get(name)
        if not mods:
            continue
        loc = np.array(loc, dtype=np.float32)
        rot = np.array(matrix, dtype=np.float32).reshape(3, 3)
        sc = np.array(scale, dtype=np.float32)
        for verts, idx, material, intentions in mods:
            if not (intentions & PHYSICAL):
                continue
            v = (verts * sc) @ rot.T + loc
            t = v[idx]
            tris.append(t)
            names.extend([name] * len(idx))
    if not tris:
        return None, []
    return np.concatenate(tris, axis=0), names


def surfaces_at(tris, names, x, y, ztop, zbottom):
    """所有包含 (x,y) 的三角形在 (x,y) 处的 z（等价于 GeoMap.getZ 的下投命中集合）。"""
    if tris is None or len(tris) == 0:
        return []
    x0, x1 = tris[:, :, 0].min(axis=1), tris[:, :, 0].max(axis=1)
    y0, y1 = tris[:, :, 1].min(axis=1), tris[:, :, 1].max(axis=1)
    cand = np.nonzero((x0 <= x) & (x1 >= x) & (y0 <= y) & (y1 >= y))[0]
    out = []
    for i in cand:
        a, b, c = tris[i]
        den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
        if abs(den) < 1e-9:
            continue
        l1 = ((b[1] - c[1]) * (x - c[0]) + (c[0] - b[0]) * (y - c[1])) / den
        l2 = ((c[1] - a[1]) * (x - c[0]) + (a[0] - c[0]) * (y - c[1])) / den
        l3 = 1.0 - l1 - l2
        if l1 < -1e-6 or l2 < -1e-6 or l3 < -1e-6:
            continue
        z = l1 * a[2] + l2 * b[2] + l3 * c[2]
        if zbottom <= z <= ztop:
            out.append((float(z), names[i]))
    out.sort(reverse=True)
    return out


def spawn_spots(world):
    path = os.path.join(SPAWNS, "%d_*.xml" % world)
    import glob
    files = glob.glob(path)
    spots = []
    for f in files:
        txt = open(f, encoding="utf-8").read()
        for bm in re.finditer(r'<spawn npc_id="(\d+)"[^>]*>(.*?)</spawn>', txt, re.S):
            npc = int(bm.group(1))
            for sm in re.finditer(r'<spot ([^>]*)/>', bm.group(2)):
                attrs = sm.group(1)
                def g(k):
                    m = re.search(k + r'="([^"]+)"', attrs)
                    return m.group(1) if m else None
                spots.append(dict(npc=npc, x=float(g("x")), y=float(g("y")), z=float(g("z")),
                                  fly=g("fly") == "1", resolve_z=g("resolve_z") == "true",
                                  file=os.path.basename(f)))
    return spots


def main():
    worlds = [int(a) for a in sys.argv[1:]] or [210050000]
    reader_cache = {}
    for world in worlds:
        spots = spawn_spots(world)
        reader = reader_cache.setdefault(world, pp.PathReader(world))
        try:
            terrain = pp.TerrainProvider(world)
        except FileNotFoundError:
            terrain = None
        placements = parse_placements(world)
        if not placements:
            print("world %d: no geo placements" % world)
            continue
        models = parse_models({p[0] for p in placements})
        tris, names = world_triangles(world, placements, models)
        print("world %d: spots=%d placements=%d meshes=%d triangles=%d terrain=%s"
              % (world, len(spots), len(placements), len(models), 0 if tris is None else len(tris),
                 "yes" if terrain else "no"))

        flagged = []
        for s in spots:
            if s["fly"] or s["resolve_z"]:
                continue
            tz = terrain(s["x"], s["y"]) if terrain else math.nan
            if not math.isfinite(tz):
                continue
            if s["z"] - tz <= TERRAIN_GAP:
                continue  # 地形即站位面，兜底正确
            if reader.project(s["x"], s["y"], s["z"], terrain) is not None:
                continue  # PATH 命中，走的是可行走地面
            surf = surfaces_at(tris, names, s["x"], s["y"], s["z"] + 2.0, s["z"] - 100.0)
            near = [(z, n) for z, n in surf if abs(z - s["z"]) <= DELTA]
            if near:
                flagged.append((s, tz, near[0]))
        print("  作者 Z > 地形 %.1fm 且 PATH 未命中: %d; 其中脚下有贴合碰撞面: %d"
              % (TERRAIN_GAP, len([s for s in spots if not s["fly"] and not s["resolve_z"]
                                   and terrain and math.isfinite(terrain(s["x"], s["y"]))
                                   and s["z"] - terrain(s["x"], s["y"]) > TERRAIN_GAP
                                   and reader.project(s["x"], s["y"], s["z"], terrain) is None]),
                 len(flagged)))
        for s, tz, (gz, mesh) in flagged:
            print("    npc=%-7d (%.2f, %.2f) z=%8.3f 地形=%8.3f 网格面=z %8.3f  %s"
                  % (s["npc"], s["x"], s["y"], s["z"], tz, gz, mesh))


if __name__ == "__main__":
    main()
