#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""M5-b2b：采集族（item_collecting）客户端动作码/页 id 统计。

数据源：docs/quest/client-dialog-mapping/legacy-quest-dialog-template-index.csv
        （由 compact 真端脚本 + 客户端 HTML 派生的"客户端出口"索引）
交叉：生产保留清单 retail-xml-retention.tsv（owner=RETAIL_TABLE 即真端驱动/已退役）
输出：m5b2b-collect-client-action-codes.tsv
"""
import os
import collections
import csv
import pathlib
import sys

REPO = next(p for p in pathlib.Path(__file__).resolve().parents if (p / "pom.xml").is_file())
IDX = REPO / 'docs/quest/client-dialog-mapping/legacy-quest-dialog-template-index.csv'
RET = REPO / 'src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv'
OUT = pathlib.Path(__file__).with_name('m5b2b-collect-client-action-codes.tsv')

FAMILY = 'item_collecting'


def owners():
    out = {}
    for line in RET.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        p = line.split('\t')
        if len(p) >= 3:
            out[int(p[0])] = (p[1], p[2])
    return out


def main():
    own = owners()
    rows = []
    with IDX.open(encoding='utf-8-sig', newline='') as fh:
        for r in csv.DictReader(fh):
            if r.get('template_type') != FAMILY:
                continue
            try:
                qid = int(r['quest_id'])
            except (KeyError, ValueError):
                continue
            rows.append((qid, r))

    action = collections.Counter()
    page = collections.Counter()
    per_quest = []
    retired = 0
    for qid, r in sorted(rows, key=lambda item: item[0]):
        owner, family = own.get(qid, ('?', '?'))
        if owner == 'RETAIL_TABLE':
            retired += 1
        aid = r.get('report_action_id') or ''
        oid = r.get('report_open_action_id') or ''
        pid = r.get('report_page_id') or ''
        wid = r.get('reward_page_id') or ''
        pid1 = r.get('start_page_id') or ''
        action[f'{aid}:{r.get("report_action","")}'] += 1
        action[f'{oid}:{r.get("report_open_action","")}'] += 1
        page[f'{pid}:{r.get("report_page","")}'] += 1
        page[f'{wid}:{r.get("reward_page","")}'] += 1
        page[f'{pid1}:{r.get("start_page","")}'] += 1
        per_quest.append((qid, owner, r.get('report_open_action_id'), r.get('report_page_id'),
                          r.get('report_action_id'), r.get('reward_page_id')))

    lines = ['# M5-b2b 采集族客户端动作码/页 id（来源 legacy-quest-dialog-template-index.csv）',
             '# quest_id\towner\treport_open_action\treport_page\treport_action\treward_page']
    for row in per_quest:
        lines.append('\t'.join('' if v is None else str(v) for v in row))
    OUT.write_text('\n'.join(lines) + '\n', encoding='utf-8')

    print(f'family={FAMILY} rows={len(rows)} retired(RETAIL_TABLE)={retired} -> {OUT}')
    print('--- 动作码分布（action_id:HACTION_* → 次数）')
    for k, n in action.most_common():
        print(f'  {n:4d}  {k}')
    print('--- 页 id 分布（page_id:PAGE → 次数）')
    for k, n in page.most_common():
        print(f'  {n:4d}  {k}')
    return 0


if __name__ == '__main__':
    sys.exit(main())
