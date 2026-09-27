#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-46：登记表爆炸半径（前端快照 vs 现值）——逐任务块列出 added/removed 行。

用法：python3 -B p0c45_blast_radius.py [--pre p0c46-registry-pre.tsv] [--out p0c46-blast-radius.txt]
退出码 0 = 有差异（正常）；1 = 完全无差异（说明改动没落盘）。
"""
from __future__ import annotations

import argparse
import collections
import pathlib

TOPIC = pathlib.Path(__file__).resolve().parent
REPO = TOPIC.parents[2]
NOW = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv'


def blocks(path):
    groups = collections.OrderedDict()
    for line in path.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        groups.setdefault(int(line.split('\t')[0]), []).append(line)
    return groups


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--pre', default=str(TOPIC / 'p0c46-registry-pre.tsv'))
    ap.add_argument('--out', default=str(TOPIC / 'p0c46-blast-radius.txt'))
    args = ap.parse_args()
    pre = blocks(pathlib.Path(args.pre))
    now = blocks(NOW)
    changed = sorted(q for q in set(pre) & set(now) if pre[q] != now[q])
    added = sorted(set(now) - set(pre))
    removed = sorted(set(pre) - set(now))
    out = ['# P0c-46 登记表爆炸半径（pre=%s）' % pathlib.Path(args.pre).name,
        'BLOCKS %d -> %d' % (len(pre), len(now)),
        'CHANGED %s' % changed, 'ADDED %s' % added, 'REMOVED %s' % removed, '']
    for q in changed:
        a, r = set(pre[q]), set(now[q])
        out.append('=== quest %d：+%d/-%d 行 ===' % (q, len(a - r), len(r - a)))
        for line in sorted(a - r):
            out.append('- %s' % line)
        for line in sorted(r - a):
            out.append('+ %s' % line)
        out.append('')
    text = '\n'.join(out)
    pathlib.Path(args.out).write_text(text + '\n', encoding='utf-8')
    print(text[:4000])
    print('BLAST -> %s' % args.out)
    return 0 if (changed or added or removed) else 1


if __name__ == '__main__':
    raise SystemExit(main())
