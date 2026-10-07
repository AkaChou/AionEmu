#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""203111（对象 29823）Z 抖动归因：把服务端逐 tick Z 与离线地面模型（GeoMap.getZ 复刻）对比。

用法：python3 analyze_203111_z.py <提取文件>   （提取文件 = analyze_203111_jitter.py 的同款输入）
输出：逐 tick 的 (t, x, y, z, groundZ, z-ground, Δz, Δground, dt)
"""

import datetime as dt
import math
import re
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import probe_route_ground as prg  # noqa: E402

TS = re.compile(r"^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3}) \d+ - ")
NEW = re.compile(r"newX=([-\d.]+) newY=([-\d.]+) newZ=([-\d.]+) mask=(-?\d+)")
STREAM = re.compile(
    r"walkGroundStream from=([-\d.]+),([-\d.]+),([-\d.]+) to=([-\d.]+),([-\d.]+),([-\d.]+)"
)


def ms(ts):
    return dt.datetime.strptime(ts, "%Y-%m-%d %H:%M:%S,%f").timestamp() * 1000


def main(path):
    grid, tri_count = prg.load_world_meshes()
    terrain = prg.load_terrain()
    print(f"grid triangles: {tri_count}", flush=True)
    ground = prg.make_ground(grid, terrain)

    ticks, streams = [], []
    for line in open(path, encoding="utf-8"):
        m = TS.match(line)
        if not m:
            continue
        t = ms(m.group(1))
        if (n := NEW.search(line)):
            ticks.append((t, float(n.group(1)), float(n.group(2)), float(n.group(3))))
        elif (s := STREAM.search(line)):
            streams.append((t, *[float(s.group(i)) for i in range(1, 7)]))

    rows = []
    for t, x, y, z in ticks:
        gz = ground(x, y, z)
        rows.append([t, x, y, z, gz])

    print(f"{'t(ms)':>9} {'x':>10} {'y':>10} {'z':>10} {'ground':>10} {'z-g':>8} {'dz':>8} {'dg':>8} {'dt':>5}")
    prev = None
    for t, x, y, z, gz in rows:
        dt_ms = 0 if prev is None else round(t - prev[0])
        dz = 0 if prev is None else z - prev[3]
        dg = 0 if prev is None else gz - prev[4]
        mark = ""
        if prev is not None and dz > 0.004:
            mark = "  <-- Z 上跳"
        print(f"{t - rows[0][0]:9.0f} {x:10.4f} {y:10.4f} {z:10.4f} {gz:10.4f} {z - gz:8.4f} {dz:8.4f} {dg:8.4f} {dt_ms:5d}{mark}")
        prev = (t, x, y, z, gz)

    # 统计
    dev = [r[3] - r[4] for r in rows]
    print(f"\n服务端 Z − 地面: min={min(dev):.4f} max={max(dev):.4f}")
    ups = [(r[0] - rows[0][0], r[3] - p[3]) for r, p in zip(rows[1:], rows) if r[3] - p[3] > 0.004]
    print(f"Z 上跳 (>4mm) 次数: {len(ups)} / {len(rows)} tick")
    for t, d in ups[:15]:
        print(f"   t={t:8.0f}ms  Δz=+{d:.4f}")
    # 流式目标 Z 与实际地面
    print("\n流式包目标 Z vs 该点地面:")
    bad = 0
    for t, fx, fy, fz, tx, ty, tz in streams:
        gz = ground(tx, ty, tz)
        if abs(tz - gz) > 0.005:
            bad += 1
            if bad <= 10:
                print(f"   t={t - rows[0][0]:8.0f}ms to=({tx:.3f},{ty:.3f},{tz:.4f}) 地面={gz:.4f} 差={tz - gz:+.4f}")
    print(f"   目标 Z 与地面差 >5mm 的包: {bad}/{len(streams)}")


if __name__ == "__main__":
    main(sys.argv[1])
