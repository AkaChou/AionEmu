#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""忠实模拟客户端在 v2 下发下的可见轨迹，量化「入地/拉起来」。

模型（依据台前反编译对账结论）：
- 收到 SM_MOVE：把自身位置吸附到包的 from，并以掩码速度朝 to 走直线，到点即停；
- 未收到包：继续朝上一个 to 走，走到即停；
- 包仅在以下 tick 发出：换腿（destinationChanged，from=当前服务端位置, to=新航点）、
  流式补发（from=服务端位置, to=前视点）、方向/掩码变化。

输入：trace-23942-latest.txt（799737）。地面模型：probe_route_ground.py。
"""
import importlib.util
import math
import re

spec = importlib.util.spec_from_file_location('prg', 'probe_route_ground.py')
prg = importlib.util.module_from_spec(spec)
spec.loader.exec_module(prg)

pat = re.compile(r'^2026-10-06 (\d\d:\d\d:\d\d,\d+) 23942 - (.*)$')


def parse(path):
    ticks = []
    pending_owner = None
    for line in open(path):
        m = pat.match(line.strip())
        if not m:
            continue
        t, msg = m.group(1), m.group(2)
        if msg.startswith('OLD targetDestX'):
            continue
        m2 = re.match(r'targetDestX: ([\-\d.]+) targetDestY: ([\-\d.]+) targetDestZ ([\-\d.]+)', msg)
        if m2:
            d = {'t': t, 'segs': t.split(','), 'dest': tuple(map(float, m2.groups()))}
            if pending_owner is not None:
                d['owner'] = pending_owner
                pending_owner = None
            ticks.append(d)
            continue
        m3 = re.match(r'ownerX=([\-\d.]+) ownerY=([\-\d.]+) ownerZ=([\-\d.]+)', msg)
        if m3:
            pending_owner = tuple(map(float, m3.groups()))
            continue
        m4 = re.match(r'newX=([\-\d.]+) newY=([\-\d.]+) newZ=([\-\d.]+) mask=([\-\d]+)', msg)
        if m4 and ticks:
            ticks[-1]['new'] = tuple(map(float, m4.groups()[:3]))
            continue
        m5 = re.match(r'walkGroundStream from=([\-\d.]+),([\-\d.]+),([\-\d.]+) to=([\-\d.]+),([\-\d.]+),([\-\d.]+)', msg)
        if m5 and ticks:
            v = tuple(map(float, m5.groups()))
            ticks[-1]['stream'] = (v[:3], v[3:])
    return ticks


def tsec(segs):
    h = int(segs[0][:2]) * 3600 + int(segs[0][3:5]) * 60 + int(segs[0][6:8])
    return h + int(segs[1]) / 1000.0


def main():
    print('loading ground ...', flush=True)
    grid, _ = prg.load_world_meshes()
    ground = prg.make_ground(grid, prg.load_terrain())
    gz = lambda x, y, ref: ground(x, y, ref)

    ticks = parse('trace-23942-latest.txt')
    print('ticks:', len(ticks))

    # 客户端状态
    pos = None          # 当前客户端位置
    target = None       # 当前走向
    events = []         # (time, kind, detail)
    max_dev = 0
    dev_hist = []

    for i, k in enumerate(ticks):
        if 'new' not in k:
            continue
        dt = tsec(ticks[i + 1]['segs']) - tsec(k['segs']) if i + 1 < len(ticks) else 0.2
        if pos is None:
            pos = k['new']
        # 收到包？
        pkt = None
        if 'stream' in k:
            pkt = k['stream']          # (from, to)
        elif 'dest' in k and (i == 0 or ticks[i - 1].get('dest') != k['dest']):
            # 换腿首 tick：from=新位置（服务端已推进的位置）, to=新航点
            pkt = (k['new'], k['dest'])
        elif i > 0 and 'dest' in ticks[i - 1] and 'dest' in k and ticks[i - 1]['dest'] != k['dest']:
            pass
        if pkt:
            frm, to = pkt
            snap = math.dist(pos, frm)
            if snap > 0.02:
                events.append((k['t'], 'SNAP', snap, pos, frm))
            pos = frm
            target = to
        # 客户端推进（速度 = 服务端本 tick 前进量 / dt, 按均匀速度走 dt）
        if target is not None:
            spd = math.dist(k['owner'], k['new']) / dt if 'owner' in k and dt > 0 else 0.0
            remain = math.dist(pos, target)
            step = min(spd * dt, remain)
            if remain > 1e-6:
                f = step / remain
                pos = tuple(pos[j] + (target[j] - pos[j]) * f for j in range(3))
        g = gz(pos[0], pos[1], pos[2] - 1)
        if g is not None:
            dev = pos[2] - g
            dev_hist.append((abs(dev), dev, k['t'], pos))
    dev_hist.sort(reverse=True)
    print('\n== 客户端相对地面偏差 top 20（负=入地） ==')
    for a, dev, t, p in dev_hist[:20]:
        print('  %s dev=%+.3f at (%.2f,%.2f,%.3f)' % (t, dev, p[0], p[1], p[2]))
    neg = [d for a, d, t, p in dev_hist if d < -0.02]
    print('  |dev|>2cm: %d / %d; 最大入地 %.3f, 最大浮空 %.3f'
          % (len(neg), len(dev_hist), min(d for a, d, t, p in dev_hist), max(d for a, d, t, p in dev_hist)))
    print('\n== 吸附事件（客户端被拉动的瞬间，这是用户看到的"跳"） ==')
    for t, kind, snap, p0, p1 in events:
        if snap > 0.02:
            dz = p1[2] - p0[2]
            print('  %s snap=%.3f dz=%+.3f  (%.2f,%.2f,%.3f)->(%.2f,%.2f,%.3f)'
                  % (t, snap, dz, p0[0], p0[1], p0[2], p1[0], p1[1], p1[2]))


if __name__ == '__main__':
    main()
