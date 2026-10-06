#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""前视距离 L 参数扫描：用 trace-23942-latest.txt 的真实服务端轨迹（与 L 无关）模拟
「客户端吸附 from → 沿 from→to(L) 弦线走一个 tick → 被下一包吸附」的可见轨迹，
统计每个 tick 的竖向修正（pop）幅度与相对地面的偏差，比较 L=1.0m（现状）与更短前视。
"""
import importlib.util
import math

import sim_client as sc

spec = importlib.util.spec_from_file_location('prg', 'probe_route_ground.py')
prg = importlib.util.module_from_spec(spec)
spec.loader.exec_module(prg)


def run(L, ticks, gz, adaptive=False):
    pos = None
    pops = []
    devs = []
    prev_from = None
    for i, k in enumerate(ticks):
        if 'new' not in k or 'dest' not in k:
            continue
        frm = k['new']
        dest = k['dest']
        if pos is None:
            pos = frm
        # 客户端在一个 tick 内的位移 = 相邻服务端位置的距离
        nxt = None
        for j in range(i + 1, len(ticks)):
            if 'new' in ticks[j]:
                nxt = ticks[j]['new']
                break
        w = math.dist(frm, nxt) if nxt else 0.0
        look = max(0.15, adaptive * w) if adaptive else L
        # to = dest 方向上前视 L 处（贴地）
        remain = math.dist(frm, dest)
        if remain <= look:
            to = dest
        else:
            f = look / remain
            to = tuple(frm[j] + (dest[j] - frm[j]) * f for j in range(3))
            g = gz(to[0], to[1], to[2] - 1)
            if g is not None:
                to = (to[0], to[1], g)
        # 吸附
        pos = frm
        # 走一步
        seg = math.dist(frm, to)
        if seg > 1e-6 and w > 0:
            f = min(w, seg) / seg
            pos = tuple(frm[j] + (to[j] - frm[j]) * f for j in range(3))
        # pop = 下一 tick 吸附时的竖向修正
        if nxt is not None:
            pops.append((nxt[2] - pos[2], k['t'], math.dist(pos, nxt)))
        g = gz(pos[0], pos[1], pos[2] - 1)
        if g is not None:
            devs.append((pos[2] - g, k['t']))
    return pops, devs


def main():
    print('loading ...')
    grid, _ = prg.load_world_meshes()
    gz = prg.make_ground(grid, prg.load_terrain())
    ticks = sc.parse('trace-23942-latest.txt')
    # 去掉换腿 tick（dest 变化）与之前的静止段影响：只保留与前一 tick 同 dest 的连续移动
    move = [k for k in ticks if 'new' in k and 'dest' in k]
    print('ticks:', len(move))
    for L in ('adaptive 1.3×w', 1.0, 0.6, 0.4, 0.3, 0.21):
        adaptive = isinstance(L, str)
        pops, devs = run(1.0 if adaptive else L, move, gz, adaptive=adaptive)
        dz = sorted(abs(d) for d, t, s in pops if s > 1e-9)
        big = [p for p in pops if abs(p[0]) > 0.04]
        dev_sorted = sorted(d for d, t in devs)
        print('L=%s  pop|dz|: p50=%.3f p90=%.3f max=%.3f  >4cm 次数=%d  dev: min=%.3f max=%.3f'
              % (L, dz[len(dz)//2], dz[int(len(dz)*0.9)], dz[-1], len(big), dev_sorted[0], dev_sorted[-1]))
        for d, t, s in [p for p in pops if abs(p[0]) > 0.06][:6]:
            print('    %s pop dz=%+.3f (move %.3f)' % (t, d, s))


if __name__ == '__main__':
    main()
