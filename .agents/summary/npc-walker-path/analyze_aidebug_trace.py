#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""解析 ai2 log（log/aidebug.log）中单对象的移动块，量化行走 Z 抖动。
Parse per-object movement blocks from the ai2 log (log/aidebug.log) and quantify walk Z jitter.

用法 / Usage:
  python3 analyze_aidebug_trace.py log/aidebug.log [objId] [--region xmin,xmax,ymin,ymax]
"""
import re
import sys
from collections import defaultdict

LINE_RE = re.compile(r'^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d+) (\d+) - (.*)$')
OLD_RE = re.compile(r'OLD targetDestX: ([\d.-]+) targetDestY: ([\d.-]+) targetDestZ ([\d.-]+)')
OWNER_RE = re.compile(r'ownerX=([\d.-]+) ownerY=([\d.-]+) ownerZ=([\d.-]+)')
TARGET_RE = re.compile(r'targetDestX: ([\d.-]+) targetDestY: ([\d.-]+) targetDestZ ([\d.-]+)')
DIST_RE = re.compile(r'futureDist: ([\d.-]+) dist: ([\d.-]+)')
NEW_RE = re.compile(r'newX=([\d.-]+) newY=([\d.-]+) newZ=([\d.-]+)')
STREAM_RE = re.compile(r'walkGroundStream from=([\d.-]+),([\d.-]+),([\d.-]+) to=([\d.-]+),([\d.-]+),([\d.-]+)')


def parse(path, obj_id):
    blocks = []
    cur = None
    with open(path, 'r', encoding='utf-8', errors='replace') as fh:
        for line in fh:
            m = LINE_RE.match(line.rstrip('\n'))
            if not m:
                continue
            ts, oid, msg = m.group(1), int(m.group(2)), m.group(3)
            if oid != obj_id:
                continue
            if msg.startswith('moveToDestination'):
                if cur and cur.get('owner') and cur.get('new'):
                    blocks.append(cur)
                cur = {'ts': ts, 'owner': None, 'new': None, 'stream': None, 'target': None, 'dist': None}
                continue
            if cur is None:
                continue
            for key, rx in (('target', TARGET_RE), ('owner', OWNER_RE), ('new', NEW_RE)):
                m2 = rx.search(msg)
                if m2 and cur.get(key) is None:
                    cur[key] = tuple(float(v) for v in m2.groups())
                    break
            m3 = DIST_RE.search(msg)
            if m3:
                cur['dist'] = float(m3.group(2))
            m4 = STREAM_RE.search(msg)
            if m4:
                cur['stream'] = tuple(float(v) for v in m4.groups())
        if cur and cur.get('owner') and cur.get('new'):
            blocks.append(cur)
    return blocks


def horiz(a, b):
    return ((a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2) ** 0.5


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else 'log/aidebug.log'
    obj_id = int(sys.argv[2]) if len(sys.argv) > 2 and not sys.argv[2].startswith('--') else 30821
    region = None
    for i, a in enumerate(sys.argv):
        if a == '--region':
            region = tuple(float(v) for v in sys.argv[i + 1].split(','))

    blocks = parse(path, obj_id)
    if not blocks:
        print(f'object {obj_id}: no movement blocks')
        return
    print(f'object {obj_id}: {len(blocks)} blocks, {blocks[0]["ts"]} .. {blocks[-1]["ts"]}')

    # 连续块链：owner(i+1) ≈ new(i)
    chains = 0
    ticks = []
    for i, b in enumerate(blocks):
        if i and horiz(blocks[i - 1]['new'], b['owner']) < 0.05:
            ticks.append(b)
        else:
            chains += 1
            ticks.append(b)
    print(f'chains(含间断): {chains}')

    # 逐 tick：行走（水平位移 ≥2cm）与垂直变化
    reversals = []
    walk_ticks = 0
    total_dist = 0.0
    zmin = min(b['new'][2] for b in blocks)
    zmax = max(b['new'][2] for b in blocks)
    prev_dz = 0.0
    prev = None
    for i, b in enumerate(blocks):
        if prev is not None and horiz(prev['new'], b['owner']) < 0.05:
            h = horiz(b['owner'], b['new'])
            total_dist += h
            dz = b['new'][2] - b['owner'][2]
            if h >= 0.02:
                walk_ticks += 1
                if prev_dz != 0 and dz != 0 and (dz > 0) != (prev_dz > 0):
                    amp = abs(dz) + abs(prev_dz)
                    reversals.append((amp, b))
                prev_dz = dz
            else:
                prev_dz = 0.0
        prev = b

    print(f'walk ticks(水平≥2cm): {walk_ticks}, 水平里程 {total_dist:.1f} m, ownerZ 范围 {zmin:.2f}..{zmax:.2f}')
    for th in (0.01, 0.02, 0.03, 0.04, 0.05, 0.08):
        n = sum(1 for amp, _ in reversals if amp >= th)
        print(f'  行走中 Z 反向（相邻两 tick 合计 Δz ≥ {th * 100:.0f}cm）: {n}')

    worst = sorted(reversals, key=lambda t: -t[0])[:15]
    print('最严重反向位置:')
    for amp, b in worst:
        o, n = b['owner'], b['new']
        print(f'  {b["ts"]}  Δz={amp * 100:.1f}cm  owner=({o[0]:.2f},{o[1]:.2f},{o[2]:.3f}) -> new=({n[0]:.2f},{n[1]:.2f},{n[2]:.3f})')

    if region:
        xmin, xmax, ymin, ymax = region
        print(f'--- 区域 {region} 内的逐 tick Z 剖面（前 200 条）---')
        shown = 0
        for b in blocks:
            o, n = b['owner'], b['new']
            if xmin <= n[0] <= xmax and ymin <= n[1] <= ymax:
                s = b['stream']
                sz = f'{s[5]:.3f}' if s else '-'
                print(f'{b["ts"]}  ({n[0]:.2f},{n[1]:.2f}) z {o[2]:.3f} -> {n[2]:.3f} (Δ{(n[2] - o[2]) * 100:+.1f}cm) 流目标z {sz}')
                shown += 1
                if shown >= 200:
                    break


if __name__ == '__main__':
    main()
