#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-10n：单步过场行漂移登记翻转（41 行，两档口径）。

前置断言（任一不满足即中止，不写盘）：
  1. 变化行集恰为 41（13 采纳 + 28 craft KEEP），全部原值 REJECTED:RETAIL_TALK_CUTSCENE；
  2. 13 采纳行：retention 已标 RETAIL_TABLE（classify 走冻结证据路径，活体导出对该 13 行显示
     旧冻结值——不采信），新值按退役时点探针分类冻结算 DIFF:NODE_PROJECTION
     （p0c10n-cutscene-probe.tsv：node 投影差屏蔽过渡集差，movie=1/xml=0 已单列取证）；
  3. 28 craft 行：retention 仍 XML_RETENTION（活体现算），新值 = 活体导出 DIFF:TRANSITION_SET；
  4. 其余行（含 12 无触发行）零变化。

用法：python3 -B p0c10n_update_drift_registry.py
"""
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent.parent
REGISTRY = REPO / 'src/test/resources/quest/retail-simple-talk-drift.tsv'
LIVE = HERE / 'p0c10n-drift-live2.tsv'
DECISIONS = HERE / 'p0c10n-cutscene-decisions.tsv'
RETENTION = REPO / 'src/test/resources/quest/retail-xml-retention.tsv'


def rows_of(path):
    out = {}
    for line in path.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        out[int(parts[0])] = parts[1]
    return out


def main():
    header = [line for line in REGISTRY.read_text(encoding='utf-8').splitlines()
        if line.startswith('#')]
    registry = rows_of(REGISTRY)
    live = rows_of(LIVE)
    retention = rows_of(RETENTION)
    adopt, keep = set(), set()
    for line in DECISIONS.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        p = line.split('\t')
        (adopt if p[1] == 'ADOPT_RETAIL' else keep).add(int(p[0]))
    assert len(adopt) == 13 and len(keep) == 28

    for q in adopt | keep:
        assert registry.get(q) == 'REJECTED:RETAIL_TALK_CUTSCENE', \
            '%s 原值异常：%s' % (q, registry.get(q))
        assert retention[q] == ('RETAIL_TABLE' if q in adopt else 'XML_RETENTION'), \
            '%s retention 与裁定不符：%s' % (q, retention[q])
    for q in adopt:
        assert live[q] == 'REJECTED:RETAIL_TALK_CUTSCENE', \
            '%s 采纳行活体值应为冻结旧值（retired 路径）：%s' % (q, live[q])
    for q in keep:
        assert live[q] == 'DIFF:TRANSITION_SET', '%s craft 行活体值异常：%s' % (q, live[q])

    changed = 0
    body = []
    for q in sorted(registry):
        new = registry[q]
        if q in adopt:
            new = 'DIFF:NODE_PROJECTION'
        elif q in keep:
            new = live[q]
        if new != registry[q]:
            changed += 1
        body.append('%d\t%s' % (q, new))
    assert changed == 41, '变化行数 %d != 41' % changed
    REGISTRY.write_text('\n'.join(header + body) + '\n', encoding='utf-8')
    print('登记翻转 %d 行：13 采纳冻结 DIFF:NODE_PROJECTION + 28 craft DIFF:TRANSITION_SET' % changed)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
