#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-57 爆炸半径：逐任务块对拍 pre/post 登记表（本片改动面 = 接取入口块 + 接取族行）。
P0c-57 blast radius: per-quest block diff between the pre and post registries."""
import collections
import pathlib
import sys

HERE = pathlib.Path(__file__).resolve().parent
PRE = HERE / 'p0c57-registry-pre.tsv'
POST = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else '/tmp/p0c43-dryrun.tsv')


def blocks(path):
    out = collections.OrderedDict()
    for line in path.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        out.setdefault(int(line.split('\t')[0]), []).append(line.split('\t'))
    return out


pre, post = blocks(PRE), blocks(POST)
rows = []
for q in sorted(set(pre) | set(post)):
    a = collections.Counter('\t'.join(r) for r in pre.get(q, []))
    b = collections.Counter('\t'.join(r) for r in post.get(q, []))
    added, removed = b - a, a - b
    if not added and not removed:
        continue
    kinds = collections.Counter()
    for line, n in removed.items():
        p = line.split('\t')
        kinds['%s:%s' % (p[1], p[2] if p[1] == 'B' else p[4])] += n
    rows.append((q, len(pre.get(q, [])), len(post.get(q, [])), sum(added.values()),
        sum(removed.values()), dict(kinds)))

print('CHANGED %s' % [r[0] for r in rows])
print('ADDED %s / REMOVED %s' % ([r[0] for r in rows if r[3]], [r[0] for r in rows if r[4]]))
print('%-7s %6s %6s %6s %6s  %s' % ('quest', 'pre', 'post', 'add', 'del', 'removed_kinds'))
for r in rows:
    print('%-7d %6d %6d %6d %6d  %s' % (r[0], r[1], r[2], r[3], r[4], r[5]))
tot_add = sum(r[3] for r in rows)
tot_del = sum(r[4] for r in rows)
print('TOTAL quests=%d added=%d removed=%d net=%d' % (len(rows), tot_add, tot_del, tot_add - tot_del))
lines_pre = sum(1 for ln in PRE.read_text(encoding='utf-8').splitlines() if not ln.startswith('#'))
lines_post = sum(1 for ln in POST.read_text(encoding='utf-8').splitlines() if not ln.startswith('#'))
print('LINES pre=%d post=%d' % (lines_pre, lines_post))
