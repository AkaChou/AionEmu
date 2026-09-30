#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-8c：按逐行裁定落地（ADOPT 退役 XML / KEEP 留 XML 并登记稳定码）。

裁定与逐行证据由 `p0c8c_gap_decisions.py` 产出（本脚本直接复用其分类器，禁止二套判据）。

动作：
  1. 断言 0 行 UNRESOLVED（有未归类差异就中止，不落地）；
  2. 写 `.agents/summary/scriptdll-quest-driver/p0c8c-gap-decisions.tsv`（owner 记录）；
  3. 把 ADOPT_RETAIL 行追加进生产裁定表 `src/test/resources/quest/retail-simple-hunt-adjudicated-decisions.tsv`
     （去重 + 按 id 排序 + 保留既有行）；
  4. 删除这些任务的 XML（`Path.unlink()`，不用 shell 删除）；
  5. 重写缺口表 `simplehunt-dialog-route-gaps.txt`：只剩 KEEP 行，每行 `id<TAB>稳定码`
     （`build_retention_list.py` 与门禁据此登记 `SEMANTIC_GAP:<码>`）。

用法：python3 -B p0c8c_retire_gap_rows.py [--dry-run]
"""
import os
import argparse
import importlib.util
from collections import Counter
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
QUESTS = REPO / 'src/main/resources/aion/data/static_data/quest/definitions/quests/%d.xml'
GAPS = HERE / 'simplehunt-dialog-route-gaps.txt'
OUT = HERE / 'p0c8c-gap-decisions.tsv'
PROD_DECISIONS = REPO / 'src/test/resources/quest/retail-simple-hunt-adjudicated-decisions.tsv'


def load_classifier():
    spec = importlib.util.spec_from_file_location('p0c8c', HERE / 'p0c8c_gap_decisions.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()
    m = load_classifier()
    rows = m.read_census()
    names = m.npc_names()
    client, gates = m.client_evidence(names)
    ctx = {'client': client, 'gates': gates, 'spawned': m.spawned_ids(), 'names': names,
        'retail': m.retail_rows(), 'report_pages': m.report_pages(), 'client_actions': m.client_action_ids()}
    decisions = {q: m.decide(q, r, ctx) for q, r in rows.items()}
    unresolved = sorted(q for q, v in decisions.items() if v[0] == 'UNRESOLVED')
    assert not unresolved, '未归类差异仍在：%s（禁止落地）' % unresolved[:10]
    adopt = sorted(q for q, v in decisions.items() if v[0] == 'ADOPT_RETAIL')
    keep = sorted(q for q, v in decisions.items() if v[0] == 'KEEP_XML')
    print('裁定 %d 行 = ADOPT %d + KEEP %d（UNRESOLVED 0）' % (len(decisions), len(adopt), len(keep)))

    text = ('# P0c-8c SimpleHunt 缺口表剩余行逐行裁定（真端优先 + 客户端契约作强二证）\n'
        '# 判据与逐行断言见 .agents/summary/scriptdll-quest-driver/p0c8c_gap_decisions.py 头注释\n'
        '# quest_id\tverdict\tbasis\taxes\tevidence\n')
    for quest_id, value in sorted(decisions.items()):
        evidence = ('%s；类别 %s' % (value[2], dict(Counter(k.split(' x')[0] for k in value[3])))).replace('\t', ' ')
        text += '%d\t%s\t%s\t-\t%s\n' % (quest_id, value[0], value[1], evidence)
    if args.dry_run:
        print(text[:500])
        return 0
    OUT.write_text(text, encoding='utf-8')

    # 3) 生产裁定表追加
    existing = PROD_DECISIONS.read_text(encoding='utf-8').splitlines()
    header = [line for line in existing if line.startswith('#')]
    body = [line for line in existing if line.strip() and not line.startswith('#')]
    body = [line for line in body if int(line.split('\t')[0]) not in set(adopt)]
    body.extend('%d\t%s\t%s\t-\t%s' % (q, decisions[q][0], decisions[q][1],
        (decisions[q][2]).replace('\t', ' ')) for q in adopt)
    body.sort(key=lambda line: int(line.split('\t')[0]))
    if not any('p0c8c_gap_decisions' in line for line in header):
        header.append('# 生成：.agents/summary/scriptdll-quest-driver/p0c8c_gap_decisions.py'
            '（P0c-8c 缺口表剩余行逐行裁定）')
    PROD_DECISIONS.write_text('\n'.join(header + body) + '\n', encoding='utf-8')
    print('裁定表 -> %s（%d 行）' % (PROD_DECISIONS, len(body)))

    # 4) 删除 XML（Path.unlink，不用 shell 删除）
    deleted = 0
    for quest_id in adopt:
        path = Path(str(QUESTS) % quest_id)
        if path.exists():
            path.unlink()
            deleted += 1
    print('删除 XML %d 个' % deleted)

    # 5) 缺口表只留 KEEP 行 + 稳定码
    gap_header = [line for line in GAPS.read_text(encoding='utf-8').splitlines() if line.startswith('#')]
    gap_header.append('# P0c-8c（2026-09-24）：%d 行 ADOPT_RETAIL 已退役（见 p0c8c-gap-decisions.tsv；'
        '真端侧冻结 IR 由 equivalence 门禁冻结模式重算）；本表只剩 %d 行 KEEP_XML，'
        '每行 `id<TAB>稳定码`（保留清单登记 SEMANTIC_GAP:<码>）。'
        % (len(adopt), len(keep)))
    lines = gap_header + ['%d\t%s' % (q, decisions[q][1]) for q in keep]
    GAPS.write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print('缺口表 -> %d 行 KEEP（%s）' % (len(keep), dict(Counter(decisions[q][1] for q in keep))))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
