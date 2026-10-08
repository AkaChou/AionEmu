#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""离线探针：spiros 路径 13→14 腿（y≈1478）与 11:54 日志中 Z 反向热点的地面剖面。
Offline probe of the ground profile along spiros' leg 13->14 (y ~= 1478) and the Z-reversal
hotspots seen in the 11:54 trace.

复刻 GeoMap.getZ(worldId, x, y, z, 100, 1)（= [z-100, z+2] 带内最高面，与
NpcMoveController.resolveGroundZ 的 fallbackZ-1 调用一致），并额外列出带内的**全部**面，
用于判断「抖动」是真实地形凸起还是棱线/双面。
数据源与复刻逻辑复用 ../npc-walker-stairs/probe_route_ground.py（2026-10-06 已 17/17 与实机对拍）。
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "npc-walker-stairs"))
import probe_route_ground as prg  # noqa: E402

# 实机轨迹（2026-10-08 11:54 run, spiros 203111）关键采样值，用于对拍
TRACE = [
    # (x, y, z_owner, 说明)
    (1670.65, 1477.99, 121.056, "leg13 起点附近（爬坡末）"),
    (1672.87, 1478.00, 121.311, "爬坡平台"),
    (1673.64, 1478.00, 121.276, "反向点（pass2 dip）"),
    (1675.85, 1478.01, 121.432, "反向点（peak）"),
    (1676.63, 1478.02, 121.391, "反向点（peak 后）"),
    (1679.35, 1478.03, 121.363, "leg14 终点"),
    (1688.54, 1497.83, 121.190, "12.7cm 反向点"),
    (1688.39, 1498.03, 121.311, "12.7cm 反向点下一 tick"),
]

# 对比：同一 x 两次经过的不同 y（pass1 y=1478.01-02, pass2 y=1477.99-00）
SEAM_XS = [1672.99, 1673.64, 1675.85, 1676.63]


def stack_surfaces(grid, terrain_z, px, py, z_top, depth):
    """列出 [z_top-depth, z_top+2] 带内全部面（网格三角 + 地形），按 z 降序。"""
    lo, hi = z_top - depth, z_top + 2.0
    x0, y0 = prg.ROI[0], prg.ROI[1]
    cell = (int((px - x0) // 2.0), int((py - y0) // 2.0))
    found = []
    for a, b, c in grid.get(cell, ()):
        den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
        if abs(den) < 1e-9:
            continue
        l1 = ((b[1] - c[1]) * (px - c[0]) + (c[0] - b[0]) * (py - c[1])) / den
        l2 = ((c[1] - a[1]) * (px - c[0]) + (a[0] - c[0]) * (py - c[1])) / den
        l3 = 1.0 - l1 - l2
        if l1 < -1e-6 or l2 < -1e-6 or l3 < -1e-6:
            continue
        z = l1 * a[2] + l2 * b[2] + l3 * c[2]
        if lo <= z <= hi:
            found.append((z, "mesh"))
    tz = terrain_z(px, py, lo, hi)
    if tz is not None:
        found.append((tz, "terr"))
    found.sort(key=lambda t: -t[0])
    dedup = []
    for z, kind in found:
        if dedup and abs(dedup[-1][0] - z) < 0.002:
            if kind == "terr" or dedup[-1][1] == "mesh":
                dedup[-1] = (dedup[-1][0], dedup[-1][1])
            continue
        dedup.append((z, kind))
    return dedup


def main():
    print("loading meshes ...", flush=True)
    grid, tri_count = prg.load_world_meshes()
    print("ROI triangles: %d" % tri_count, flush=True)
    print("loading terrain ...", flush=True)
    terrain_z = prg.load_terrain()

    print("\n== A. 实机采样点对拍（复刻 resolveGroundZ: 带 [z-101, z+1]，取最高面） ==")
    for x, y, z, note in TRACE:
        s = stack_surfaces(grid, terrain_z, x, y, z, 1.0)
        top = s[0][0] if s else None
        second = (" 次面=%.3f(%s)" % (s[1][0], s[1][1])) if len(s) > 1 else ""
        delta = ("Δ=%+.3f" % (top - z)) if top is not None else ""
        print("  (%8.2f,%8.2f) 实机z=%7.3f 复刻顶面=%s %s%s  - %s"
              % (x, y, z, ("%.3f" % top) if top is not None else "None", delta, second, note))

    print("\n== B. 棱线敏感性：同 x、y 扫 1477.90→1478.10（两次经过 y 差 2cm） ==")
    for x in SEAM_XS:
        print("  x=%.2f:" % x)
        last_top = None
        for i in range(11):
            y = 1477.90 + i * 0.02
            s = stack_surfaces(grid, terrain_z, x, y, 121.6, 1.0)
            top = ("%.3f" % s[0][0]) if s else "None"
            extra = ""
            if len(s) > 1:
                extra = " (+%d 个次面: %s)" % (len(s) - 1, ", ".join("%.3f/%s" % t for t in s[1:4]))
            mark = ""
            if last_top and s:
                last_top = float(last_top)
                if abs(float(top) - last_top) >= 0.03:
                    mark = "   <<< 跳变"
            print("    y=%.2f 顶面=%s%s%s" % (y, top, extra, mark))
            last_top = top

    print("\n== C. leg 13→14 弦线剖面（起点终点取实机地面，检查与弦线偏差>10cm 段） ==")
    x_start, y_start = 1670.65, 1477.99
    x_end, y_end = 1679.35, 1478.03

    def top_at(px, py):
        s = stack_surfaces(grid, terrain_z, px, py, 122.0, 1.0)
        return s[0][0] if s else None

    z0, z1 = top_at(x_start, y_start), top_at(x_end, y_end)
    print("  端点顶面: start=%.3f end=%.3f" % (z0, z1))
    worst = (0.0, None)
    for i in range(0, 37):
        t = i / 36.0
        px = x_start + (x_end - x_start) * t
        py = y_start + (y_end - y_start) * t
        zt = top_at(px, py)
        if zt is None:
            continue
        chord = z0 + (z1 - z0) * t
        dev = zt - chord
        flag = "  <<<" if abs(dev) > 0.10 else ""
        if abs(dev) > abs(worst[0]):
            worst = (dev, (px, py))
        if i % 3 == 0 or flag:
            print("    t=%.2f (%8.2f,%8.2f) 地面=%.3f 弦=%+.3f 偏差=%+.3f%s" % (t, px, py, zt, chord, dev, flag))
    print("  最大偏差 %+.3f m @ (%.2f,%.2f)（LoS 容差 0.10m；超过即 GeoMap 口径也应判「需要沿 Path」）" % (worst[0], worst[1][0], worst[1][1]))


if __name__ == "__main__":
    main()
