#!/usr/bin/env python3
"""203111（Spiros，对象 29823）行走顺滑度判读：逐 tick 步长/间隔/流式段长/方向/贴地误差。

数据来源：log/aidebug.log `ai2 log` 窗口（2026-10-07 10:22:43–10:23:14），
提取行格式见同目录 README 或 awk 命令行；输入文件为「newX= / walkGroundStream / Setting step…」行。

用法：python3 analyze_203111_jitter.py <提取文件>
"""

import math
import re
import sys
from collections import Counter

TS = re.compile(r"^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3}) \d+ - ")
NEW = re.compile(r"newX=([-\d.]+) newY=([-\d.]+) newZ=([-\d.]+) mask=(-?\d+)")
STREAM = re.compile(
    r"walkGroundStream from=([-\d.]+),([-\d.]+),([-\d.]+) to=([-\d.]+),([-\d.]+),([-\d.]+)"
)


def parse(path):
    ticks = []  # (t_ms, x, y, z, mask)
    streams = []  # (t_ms, fx, fy, fz, tx, ty, tz)
    marks = []  # (t_ms, text)
    import datetime as dt

    def ms(ts):
        return dt.datetime.strptime(ts, "%Y-%m-%d %H:%M:%S,%f").timestamp() * 1000

    for line in open(path, encoding="utf-8"):
        m = TS.match(line)
        if not m:
            continue
        t = ms(m.group(1))
        if (mm := NEW.search(line)):
            ticks.append((t, float(mm.group(1)), float(mm.group(2)), float(mm.group(3)), int(mm.group(4))))
        elif (sm := STREAM.search(line)):
            streams.append((t, *[float(sm.group(i)) for i in range(1, 7)]))
        else:
            marks.append((t, line.strip().split(" - ", 1)[-1]))
    return ticks, streams, marks


def main(path):
    ticks, streams, marks = parse(path)
    print(f"ticks={len(ticks)} streams={len(streams)} marks={len(marks)}")
    print(f"窗口 {ticks[0][0]:.0f} → {ticks[-1][0]:.0f} ms")

    steps = []
    for (t0, x0, y0, z0, _), (t1, x1, y1, z1, mask1) in zip(ticks, ticks[1:]):
        dt_ms = t1 - t0
        d = math.hypot(x1 - x0, y1 - y0)
        steps.append((t1, dt_ms, d, z1 - z0, mask1))
    intervals = Counter(round(s[1]) for s in steps)
    print("tick 间隔分布(ms):", dict(sorted(intervals.items())))
    ds = [s[2] for s in steps]
    print(f"步长: min={min(ds):.4f} max={max(ds):.4f} mean={sum(ds)/len(ds):.4f}")
    print("步长 >0.02 偏离均值的 tick 数:", sum(1 for d in ds if abs(d - sum(ds)/len(ds)) > 0.02))
    # 每秒速度
    for lo, hi in [(0.0, 10.0), (10.0, 20.0), (20.0, 31.0)]:
        seg = [s for s in steps if lo * 1000 <= s[0] - ticks[0][0] < hi * 1000]
        if seg:
            dist = sum(s[2] for s in seg)
            dur = sum(s[1] for s in seg) / 1000.0
            print(f"  [{lo:4.0f},{hi:4.0f})s: 距离 {dist:6.3f}m / 时长 {dur:5.2f}s = {dist/dur:.3f} m/s（{len(seg)} tick）")

    print("\n流式段:")
    slens = []
    for t, fx, fy, fz, tx, ty, tz in streams:
        slens.append(math.hypot(tx - fx, ty - fy))
    if slens:
        print(f"  段长: min={min(slens):.4f} max={max(slens):.4f} mean={sum(slens)/len(slens):.4f}")
        print("  段长分布:", dict(sorted(Counter(round(s, 2) for s in slens).items())))
    # 流式与 tick 的配对：每个 tick 是否有包、包起点是否 = tick 位置
    tick_by_ms = {round(t): (x, y, z) for t, x, y, z, _ in ticks}
    missing = 0
    for t, x, y, z, _ in ticks:
        near = [s for s in streams if abs(s[0] - t) < 60]
        if not near:
            missing += 1
    print(f"  无对应流式包的 tick 数: {missing}")
    mism = []
    for t, fx, fy, fz, tx, ty, tz in streams:
        best = min(ticks, key=lambda k: abs(k[0] - t))
        if abs(best[0] - t) > 60:
            mism.append((t, "无近邻 tick"))
            continue
        d = math.hypot(best[1] - fx, best[2] - fy)
        if d > 0.01:
            mism.append((t, f"起点与 tick 位置差 {d:.3f}m"))
    print(f"  流式包起点≠对应 tick 位置的条数: {len(mism)}")
    for m in mism[:10]:
        print("   ", m)

    # 贴地误差：tick 的 z 与流式目标 z（同点）无关；这里看 z 的逐 tick 变化是否平滑
    zs = [t[3] for t in ticks]
    dz = [b - a for a, b in zip(zs, zs[1:])]
    print(f"\nZ 逐 tick 变化: min={min(dz):+.4f} max={max(dz):+.4f} 单调下降占比={sum(1 for d in dz if d <= 0.0001)/len(dz):.2%}")

    print("\n标记行:")
    for t, txt in marks:
        print(f"  {t - ticks[0][0]:8.0f}ms  {txt}")


if __name__ == "__main__":
    main(sys.argv[1])
