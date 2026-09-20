#!/usr/bin/env python3
"""805334 LF4_Somation_E (210050000) 高点刷点探针（只读）。

目的：解释运行时 curZ=473.55777 / terrainZ=473.57777 / geoZ=473.58194 / spawnZ=489.7741
     之间 2cm 的来源，并列出该石头附近的同族刷点。

用法：python3 .agents/summary/inggison-somation-rock-z/probe_2157_277.py
"""
import importlib.util
import math
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
AUDIT = os.path.join(HERE, "..", "spawn-z-audit")


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


pp = load("audit_path_projection", os.path.join(AUDIT, "audit_path_projection.py"))

WORLD = 210050000
X, Y, Z = 2157.0432, 277.79797, 489.7741
XML = "src/main/resources/aion/data/static_data/spawns/Npcs/210050000_Inggison.xml"

terrain = pp.TerrainProvider(WORLD)
reader = pp.PathReader(WORLD)

gx, gy = int(X * 2), int(Y * 2)
cx, cy = gx * 0.5 + 0.25, gy * 0.5 + 0.25

print("world=%d point=(%.4f, %.4f) spawnZ=%.4f" % (WORLD, X, Y, Z))
print("terrain@exact(%.4f, %.4f)   = %.5f" % (X, Y, terrain(X, Y)))
print("terrain@cellcenter(%.4f,%.4f) = %.5f" % (cx, cy, terrain(cx, cy)))
print("PATH project(ref=spawnZ) = %s" % reader.project(X, Y, Z, terrain))

# 扫描参考高度，找出该格子 PATH 节点的真实 z（忽略 0.7m 容差）
nodes = set()
ref = 380.0
while ref <= 495.0:
    got = reader.project(X, Y, ref, terrain)
    if got is not None:
        nodes.add(round(got, 5))
    ref += 0.25
print("PATH nodes on this cell (ignoring tolerance): %s" % sorted(nodes))

# 附近刷点（半径 80m）——同族筛查
txt = open(XML, encoding="utf-8").read()
near = []
for bm in re.finditer(r'<spawn npc_id="(\d+)"[^>]*>(.*?)</spawn>', txt, re.S):
    npc = int(bm.group(1))
    for sm in re.finditer(r'<spot ([^>]*)/>', bm.group(2)):
        attrs = sm.group(1)
        def g(k):
            m = re.search(k + r'="([^"]+)"', attrs)
            return m.group(1) if m else None
        x, y, z = float(g("x")), float(g("y")), float(g("z"))
        d = math.hypot(x - X, y - Y)
        if d <= 80.0:
            near.append((d, npc, x, y, z, terrain(x, y), g("fly"), g("resolve_z"), g("random_walk")))

print("\nnearby spots within 80m (d, npc, x, y, z, terrain, fly, resolve_z, randwalk):")
for row in sorted(near):
    d, npc, x, y, z, tz, fly, rz, rw = row
    dz = "n/a" if not math.isfinite(tz) else "%+.2f" % (z - tz)
    print("  d=%6.2f npc=%-7d (%.2f, %.2f) z=%9.4f terrain=%9.4f dz=%s fly=%s resolve_z=%s rw=%s"
          % (d, npc, x, y, z, tz, dz, fly, rz, rw))
