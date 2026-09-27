#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-10m：单步接取发物 116 行漂移登记翻转（REJECTED:RETAIL_TALK_ITEM → DIFF）。

前置断言（任一不满足即中止，不写盘）：
  1. 活体导出（-Dretail.talk.equivOut）与在册登记行集合完全一致（只允许值翻转，不许增删行）；
  2. 变化行数恰为 116，且全部原值 REJECTED:RETAIL_TALK_ITEM*；
  3. 变化后值域 = {DIFF:NODE_PROJECTION 106, DIFF:TRANSITION_SET 10}；
  4. 116 行与 p0c10m-talkitem-ids.txt 一致。

动作：保留原注释头，正文按活体导出重写。
用法：python3 -B p0c10m_update_drift_registry.py
"""
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent.parent
REGISTRY = REPO / 'src/test/resources/quest/retail-simple-talk-drift.tsv'
LIVE = HERE / 'p0c10m-drift-live.tsv'
IDS = HERE / 'p0c10m-talkitem-ids.txt'


def rows_of(path):
    out = {}
    for line in path.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        out[int(parts[0])] = parts[1]
    return out


def main():
    registry_header = [line for line in REGISTRY.read_text(encoding='utf-8').splitlines()
        if line.startswith('#')]
    registry = rows_of(REGISTRY)
    live = rows_of(LIVE)
    ids = {int(line) for line in IDS.read_text().split() if line.strip()}

    assert set(registry) == set(live), \
        '行集合漂移：仅登记侧 %s / 仅活体侧 %s' % (
            sorted(set(registry) - set(live))[:5], sorted(set(live) - set(registry))[:5])
    changed = {q: (registry[q], live[q]) for q in registry if registry[q] != live[q]}
    # 162 行全部原为 REJECTED:RETAIL_TALK_ITEM：116 行翻转 DIFF，46 行换成更精确的拒绝码
    # （41 cutscene / 5 item_check，precheck 重排后分类落位）。
    assert set(changed) == ids | (set(changed) - ids), '不变式自失配'
    assert ids <= set(changed) and len(changed) == 162, '变化行数 %d != 162' % len(changed)
    assert all(old.startswith('REJECTED:RETAIL_TALK_ITEM') for old, _ in changed.values()), \
        '存在非 RETAIL_TALK_ITEM 原值'
    histogram = {}
    for _, new in changed.values():
        histogram[new] = histogram.get(new, 0) + 1
    assert histogram == {'DIFF:NODE_PROJECTION': 106, 'DIFF:TRANSITION_SET': 10,
        'REJECTED:RETAIL_TALK_CUTSCENE': 41, 'REJECTED:RETAIL_ITEM_CHECK_UNRESOLVED': 5}, histogram

    body = ['%d\t%s' % (q, live[q]) for q in sorted(live)]
    REGISTRY.write_text('\n'.join(registry_header + body) + '\n', encoding='utf-8')
    print('登记翻转 %d 行：%s' % (len(changed), histogram))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
