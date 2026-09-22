#!/usr/bin/env python3
"""审计 Reshanta(400010000) 的飞行刷点(fly="1")：查客户端 PATH 可行走地面高度。

目的：判断 `fly="1" resolve_z="true"` 刷点（零售 Aerial_Spawn=TRUE）相对
客户端可行走地面的悬空高度，确认"怪物在空中"是否与客户端地面一致。

只读；复用 audit_path_projection.py 的 PATH 读取实现。
用法：python3 .agents/summary/spawn-z-audit/audit_aerial_spawns.py
"""
import importlib.util
import math
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
_spec = importlib.util.spec_from_file_location(
    "audit_path_projection", os.path.join(HERE, "audit_path_projection.py"))
mod = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(mod)

WORLD = 400010000
XML = "src/main/resources/aion/data/static_data/spawns/Npcs/400010000_Reshanta.xml"


def scan_ground(reader, x, y, z, span=40.0, step=0.5):
    """扫描 refZ，找出该格子上的 PATH 节点 z 集合（忽略 0.7m 容差限制）。"""
    found = set()
    ref = z - span
    while ref <= z + 5.0:
        got = reader.project(x, y, ref, mod.nan_terrain)
        if got is not None:
            found.add(round(got, 4))
        ref += step
    return sorted(found)


def main():
    reader = mod.PathReader(WORLD)
    txt = open(XML, encoding="utf-8").read()
    rows = []
    for bm in re.finditer(r'<spawn npc_id="(\d+)"[^>]*>(.*?)</spawn>', txt, re.S):
        npc = int(bm.group(1))
        for sm in re.finditer(r'<spot ([^>]*)/>', bm.group(2)):
            attrs = sm.group(1)
            g = lambda k: (re.search(k + r'="([^"]+)"', attrs).group(1)
                           if re.search(k + r'="([^"]+)"', attrs) else None)
            x, y, z = float(g("x")), float(g("y")), float(g("z"))
            row = dict(npc=npc, x=x, y=y, z=z, fly=g("fly") == "1",
                       resolve_z=g("resolve_z") == "true")
            if not row["fly"]:
                continue
            row["engine"] = reader.project(x, y, z, mod.nan_terrain)
            nodes = scan_ground(reader, x, y, z)
            row["nodes"] = nodes
            row["nearest"] = min(nodes, key=lambda n: abs(n - z)) if nodes else None
            rows.append(row)
    print("world=%d  fly spots=%d" % (WORLD, len(rows)))
    print("%-8s %11s %11s %11s  %-9s %-9s %s" %
          ("npc", "x", "y", "spawnZ", "path(exact)", "path(near)", "delta=spawnZ-ground"))
    for r in rows:
        near = r["nearest"]
        delta = "%.2f" % (r["z"] - near) if near is not None else "n/a"
        print("%-8d %11.3f %11.3f %11.3f  %-9s %-9s %s  nodes=%s" %
              (r["npc"], r["x"], r["y"], r["z"],
               "null" if r["engine"] is None else r["engine"], near, delta, r["nodes"]))
    with_ground = [r for r in rows if r["nearest"] is not None]
    if with_ground:
        ds = sorted(r["z"] - r["nearest"] for r in with_ground)
        print("\n有 PATH 地面的飞行点: %d/%d, 悬空高度 min=%.2f median=%.2f max=%.2f" %
              (len(with_ground), len(rows), ds[0], ds[len(ds) // 2], ds[-1]))


if __name__ == "__main__":
    main()


def neighbourhood(x0=2628.407715, y0=1541.468750, radius=250.0, threshold=5.0):
    """扫描附近刷点（含非飞行），列出相对 PATH 地面悬空 > threshold 的点。"""
    reader = mod.PathReader(WORLD)
    txt = open(XML, encoding="utf-8").read()
    rows = []
    for bm in re.finditer(r'<spawn npc_id="(\d+)"[^>]*>(.*?)</spawn>', txt, re.S):
        npc = int(bm.group(1))
        for sm in re.finditer(r'<spot ([^>]*)/>', bm.group(2)):
            attrs = sm.group(1)
            g = lambda k: (re.search(k + r'="([^"]+)"', attrs).group(1)
                           if re.search(k + r'="([^"]+)"', attrs) else None)
            x, y, z = float(g("x")), float(g("y")), float(g("z"))
            if math.hypot(x - x0, y - y0) > radius:
                continue
            nodes = scan_ground(reader, x, y, z)
            near = min(nodes, key=lambda n: abs(n - z)) if nodes else None
            delta = (z - near) if near is not None else None
            if delta is None or delta > threshold:
                rows.append((npc, x, y, z, near, delta, g("fly") == "1",
                             g("resolve_z") == "true"))
    print("\n半径 %.0fm 内相对 PATH 地面悬空 > %.0fm 的刷点：%d" % (radius, threshold, len(rows)))
    for npc, x, y, z, near, delta, fly, rz in sorted(rows, key=lambda r: r[0]):
        print("  npc=%-7d x=%9.3f y=%9.3f z=%9.3f ground=%-9s delta=%-7s fly=%s resolve_z=%s" %
              (npc, x, y, z, near, ("%.2f" % delta) if delta is not None else "n/a", fly, rz))
