#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""用实机 trace（trace-23942-latest.txt，799737 别名 objectId 23942）复核 v2 短段下发
在台阶上的「弦线-地面」偏差：模拟客户端「收到包即吸附到 from，再沿弦线走向 to」的位置序列，
量化每个下发周期内脚部相对地面的下沉/浮空深度。

地面模型复用 probe_route_ground.py（PHYSICAL 网格面 ∪ 地形图，取带内最高面）。
"""
import importlib.util
import math
import re

spec = importlib.util.spec_from_file_location('prg', 'probe_route_ground.py')
prg = importlib.util.module_from_spec(spec)
spec.loader.exec_module(prg)


def parse_ticks(path):
    """把 ai2 log 片段解析成 tick 列表：{t, owner, dest, new, stream(from,to)}。"""
    pat = re.compile(r'^2026-10-06 (\d\d:\d\d:\d\d,\d+) 23942 - (.*)$')
    ticks = []
    ents = []
    for line in open(path):
        m = pat.match(line.strip())
        if not m:
            continue
        t, msg = m.group(1), m.group(2)
        if msg.startswith(('Setting AI substate', 'Setting step')):
            ents.append((t, msg))
            continue
        if msg.startswith('OLD targetDestX'):
            continue
        m2 = re.match(r'targetDestX: ([\-\d.]+) targetDestY: ([\-\d.]+) targetDestZ ([\-\d.]+)', msg)
        if m2:
            ticks.append({'t': t, 'dest': tuple(map(float, m2.groups()))})
            continue
        m3 = re.match(r'ownerX=([\-\d.]+) ownerY=([\-\d.]+) ownerZ=([\-\d.]+)', msg)
        if m3 and ticks:
            ticks[-1]['owner'] = tuple(map(float, m3.groups()))
            continue
        m4 = re.match(r'newX=([\-\d.]+) newY=([\-\d.]+) newZ=([\-\d.]+) mask=([\-\d]+)', msg)
        if m4 and ticks:
            ticks[-1]['new'] = tuple(map(float, m4.groups()[:3]))
            continue
        m5 = re.match(r'walkGroundStream from=([\-\d.]+),([\-\d.]+),([\-\d.]+) to=([\-\d.]+),([\-\d.]+),([\-\d.]+)', msg)
        if m5 and ticks:
            v = tuple(map(float, m5.groups()))
            ticks[-1]['stream'] = (v[:3], v[3:])
    return ticks, ents


def main():
    print('loading ground model ...', flush=True)
    grid, tri = prg.load_world_meshes()
    terrain_z = prg.load_terrain()
    ground = prg.make_ground(grid, terrain_z)

    def gz(x, y, ref):
        return ground(x, y, ref)

    ticks, ents = parse_ticks('trace-23942-latest.txt')
    print('ticks:', len(ticks), 'events:', len(ents))

    # 服务器位置贴地误差
    serr = []
    for k in ticks:
        if 'new' not in k:
            continue
        x, y, z = k['new']
        g = gz(x, y, z - 1)
        if g is not None:
            serr.append((z - g, x, y))
    serr.sort()
    print('\n== 服务端 newZ 贴地误差（z - ground） ==')
    print('  n=%d  min=%.4f  max=%.4f  最负 3 处: %s' % (
        len(serr), serr[0][0], serr[-1][0],
        ['(%.3f @ %.2f,%.2f)' % (e, x, y) for e, x, y in serr[:3]]))

    # 客户端弦线模拟：吸附 from → 沿弦走 w（= 下一 tick 的 from 与本 tick from 的距离）
    print('\n== 客户端周期内偏差（沿弦线走 w=下周期前进量；负=入地，正=浮空） ==')
    worst = []
    for i, k in enumerate(ticks):
        if 'stream' not in k or 'new' not in k:
            continue
        frm, to = k['stream']
        seg = math.dist(frm, to)
        nxt = None
        for j in range(i + 1, len(ticks)):
            if 'stream' in ticks[j]:
                nxt = ticks[j]['stream'][0]
                break
        w = math.dist(frm, nxt) if nxt else 0.0
        # 沿弦采样 0..w
        lo, hi = 0.0, 0.0
        samples = 7
        for s in range(samples + 1):
            d = w * s / samples
            x = frm[0] + (to[0] - frm[0]) * d / seg
            y = frm[1] + (to[1] - frm[1]) * d / seg
            z = frm[2] + (to[2] - frm[2]) * d / seg
            g = gz(x, y, z - 1)
            if g is None:
                continue
            dev = z - g
            lo = min(lo, dev)
            hi = max(hi, dev)
        worst.append((lo, hi, k['t'], frm, to, seg, w))
    worst.sort()
    print('  周期数: %d' % len(worst))
    for lo, hi, t, frm, to, seg, w in worst[:12]:
        print('  %s seg=%.2f w=%.3f dip=%+.3f float=%+.3f  from=(%.2f,%.2f,%.2f) to=(%.2f,%.2f,%.2f)'
              % (t, seg, w, lo, hi, frm[0], frm[1], frm[2], to[0], to[1], to[2]))

    # 只统计「入地」分布的直方
    dips = [w[0] for w in worst]
    dips_neg = [d for d in dips if d < -0.01]
    print('\n  入地周期: %d / %d，最深 %.3f m，中位数(负样本) %.3f m'
          % (len(dips_neg), len(dips), min(dips), sorted(dips_neg)[len(dips_neg)//2] if dips_neg else 0))

    # 地面剖面（沿实际行走折线，逐 0.5m 采样）
    print('\n== 沿行走折线地面剖面（0.5m 采样，仅打印相邻高差>=0.05m 处） ==')
    pts = [k['new'] for k in ticks if 'new' in k]
    prev_g = None
    for i in range(len(pts) - 1):
        a, b = pts[i], pts[i + 1]
        seg = math.dist(a, b)
        n = max(1, int(seg / 0.5))
        for s in range(n):
            d = s * seg / n
            x = a[0] + (b[0] - a[0]) * d / seg
            y = a[1] + (b[1] - a[1]) * d / seg
            z = a[2] + (b[2] - a[2]) * d / seg
            g = gz(x, y, z - 1)
            if g is None:
                continue
            if prev_g is not None and abs(g - prev_g) >= 0.05:
                print('  (%.2f,%.2f) 地面 %.3f -> %.3f  Δ=%+.3f' % (x, y, prev_g, g, g - prev_g))
            prev_g = g


if __name__ == '__main__':
    main()
