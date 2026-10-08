#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""分析 ai2 日志中行走者每个航段的收尾（到点前 ~15 tick）与「空踏步」形态：
按目标变化切段，打印每段末段逐 tick 位移/剩余距离/停止位置，并统计小位移连续串。
Analyze each leg's tail (last ~15 ticks) and in-place stepping: split by target changes, print
per-tick deltas/remaining at the leg end, and flag runs of tiny repetitive moves.

用法 / Usage: python3 analyze_arrivals.py <logfile> <objId>
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import analyze_aidebug_trace as at  # noqa: E402


def horiz(a, b):
    return ((a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2) ** 0.5


def main():
    path = sys.argv[1]
    obj = int(sys.argv[2])
    blocks = at.parse(path, obj)
    print(f'object {obj}: {len(blocks)} blocks')

    # 按目标变化切段
    legs = []
    cur = []
    cur_target = None
    for b in blocks:
        t = b.get('target')
        if cur and (t is None or cur_target is None or horiz(t, cur_target) > 0.05):
            legs.append((cur_target, cur))
            cur = []
        if cur is None:
            cur = []
        cur.append(b)
        if t is not None and (cur_target is None or horiz(t, cur_target) > 0.05):
            cur_target = t
    if cur:
        legs.append((cur_target, cur))

    print(f'legs(按目标切): {len(legs)}')
    for idx, (target, blks) in enumerate(legs):
        if len(blks) < 3:
            continue
        t0, t1 = blks[0]['ts'], blks[-1]['ts']
        last = blks[-1]
        end_pos = last['new']
        rem_end = horiz(end_pos, target) if target else float('nan')
        # 位移统计
        deltas = [horiz(b['owner'], b['new']) for b in blks]
        tiny = sum(1 for d in deltas if 0.005 < d < 0.05)
        print(f'--- leg#{idx} 目标=({target[0]:.2f},{target[1]:.2f}) {t0}..{t1} blocks={len(blks)} '
              f'结束点=({end_pos[0]:.2f},{end_pos[1]:.2f},{end_pos[2]:.3f}) 离目标={rem_end:.3f}m 小位移tick={tiny}')
        for b in blks[-12:]:
            o, n = b['owner'], b['new']
            rem = horiz(n, target) if target else float('nan')
            d = horiz(o, n)
            s = b.get('stream')
            st = f'{s[3]:.2f},{s[4]:.2f},{s[5]:.3f}' if s else '-'
            print(f'    {b["ts"]} Δh={d:.3f} 剩余={rem:.3f} z {o[2]:.3f}->{n[2]:.3f} 流目标=({st})')


if __name__ == '__main__':
    main()
