#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-9：SimpleHunt 族收口落地（phase5-3 残留 23 行 + 职业可选奖励 2 行）。

裁定由 `p0c8c_gap_decisions.py`（唯一判据来源）对两份普查产出：
  * p0c9-phase53-shape-census.tsv     —— phase5-3 旧码 23 行（P0c-8a 实测差异、从未逐行裁定）
  * p0c9-class-select-census.tsv      —— 11102/28313（<class>_selectable_reward 编译器落地后）

动作（任一批次 UNRESOLVED > 0 即中止，不写盘）：
  1. 写 owner 记录 `p0c9-decisions.tsv`（quest_id / batch / verdict / basis / axes / evidence）；
  2. ADOPT_RETAIL 行追加进生产裁定表 `retail-simple-hunt-adjudicated-decisions.tsv`（去重 + 排序）；
  3. 删除 ADOPT 行 XML（Path.unlink()，不用 shell 删除）；
  4. 缺口表重写 = 既有 42 KEEP + phase5-3 批 KEEP（每行 `id<TAB>稳定码`）；
  5. phase5-3-rejections.txt 清空（留指针注释；23 行全部裁定完毕）；
  6. 28313 的 EnterWorld 存档自愈边按 P0c-6 先例追加进 p0c6-legacy-save-normalization.tsv。

用法：python3 -B p0c9_retire_rows.py [--dry-run]
"""
import os
import argparse
import importlib.util
from collections import Counter
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
QUESTS = REPO / 'src/main/resources/aion/data/static_data/quest_definition/quests/%d.xml'
GAPS = HERE / 'simplehunt-dialog-route-gaps.txt'
PHASE53 = HERE / 'phase5-3-rejections.txt'
OUT = HERE / 'p0c9-decisions.tsv'
PROD_DECISIONS = REPO / 'src/test/resources/quest/retail-simple-hunt-adjudicated-decisions.tsv'
# P0c-11 起：旧存档自愈边登记的规范位置在 test resources（受 RetailNonIrAxisGateTest 守）。
P0C6_NORMALIZATION = REPO / 'src/test/resources/quest/retail-legacy-save-normalization.tsv'
BATCHES = (
    ('phase53', HERE / 'p0c9-phase53-shape-census.tsv'),
    ('class-select', HERE / 'p0c9-class-select-census.tsv'),
)
# 28313 的 EnterWorld 边（普查实测原文）→ P0c-6 登记表行。/ 28313's EnterWorld edges (probed).
P0C6_ROWS_28313 = (
    ('28313', '(none)', 'reward', 'enter-world',
     'status=REWARD var0=3', 'var0:=1 var1:=1 var2:=1',
     '真端形状：6 位 SECTION 网格即计数；REWARD 投影恒为满段，客户端 SECTION 门控推导任务书行，服务端无行号可漂移'),
    ('28313', '(none)', 'a1b1(START/4161)', 'enter-world',
     'status=START var0=3', 'var0:=1 var1:=1 var2:=1',
     '真端形状：饱和段投影即报告门控；旧"换武器步数归一"边不再表达（可选 DB 归一化）'),
    ('28313', '(none)', 'a1b0(START/65)', 'enter-world',
     'status=START var0=2', 'var0:=1 var1:=1 var2:=0',
     '真端形状：同上；计数在网格打包值中，登录期改写会破坏网格形状'),
)


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
    names = m.npc_names()
    client, gates = m.client_evidence(names)
    ctx = {'client': client, 'gates': gates, 'spawned': m.spawned_ids(), 'names': names,
        'retail': m.retail_rows(), 'report_pages': m.report_pages(), 'client_actions': m.client_action_ids(),
        'registered_pages': m.registered_page_ids()}

    decisions = {}
    for batch, census in BATCHES:
        rows = m.read_census(census)
        for quest_id, row in rows.items():
            verdict, basis, evidence, kinds = m.decide(quest_id, row, ctx)
            decisions[quest_id] = (batch, verdict, basis, evidence,
                dict(Counter(k.split(' x')[0] for k in kinds)))
        unresolved = sorted(q for q, v in decisions.items()
            if v[1] == 'UNRESOLVED' and v[0] == batch)
        assert not unresolved, '批次 %s 仍有未归类差异：%s（禁止落地）' % (batch, unresolved[:10])
    adopt = sorted(q for q, v in decisions.items() if v[1] == 'ADOPT_RETAIL')
    keep = sorted(q for q, v in decisions.items() if v[1] == 'KEEP_XML')
    print('裁定 %d 行 = ADOPT %d + KEEP %d（UNRESOLVED 0）' % (len(decisions), len(adopt), len(keep)))

    text = ('# P0c-9 SimpleHunt 族收口逐行裁定（phase5-3 残留 + 职业可选奖励归零）\n'
        '# 判据与逐行断言见 .agents/summary/scriptdll-quest-driver/p0c8c_gap_decisions.py 头注释\n'
        '# quest_id\tbatch\tverdict\tbasis\taxes\tevidence\n')
    for quest_id, (batch, verdict, basis, evidence, kinds) in sorted(decisions.items()):
        text += '%d\t%s\t%s\t%s\t-\t%s；类别 %s\n' % (quest_id, batch, verdict, basis,
            evidence.replace('\t', ' '), kinds)
    if args.dry_run:
        print(text[:600])
        return 0
    OUT.write_text(text, encoding='utf-8')
    print('owner 记录 -> %s（%d 行）' % (OUT, len(decisions)))

    # 2) 生产裁定表追加（去重 + 排序，既有行零搅动）
    existing = PROD_DECISIONS.read_text(encoding='utf-8').splitlines()
    header = [line for line in existing if line.startswith('#')]
    body = [line for line in existing if line.strip() and not line.startswith('#')]
    body = [line for line in body if int(line.split('\t')[0]) not in set(adopt)]
    body.extend('%d\t%s\t%s\t-\t%s' % (q, decisions[q][1], decisions[q][2],
        decisions[q][3].replace('\t', ' ')) for q in adopt)
    body.sort(key=lambda line: int(line.split('\t')[0]))
    if not any('p0c9_retire_rows' in line for line in header):
        header.append('# 生成：.agents/summary/scriptdll-quest-driver/p0c9_retire_rows.py'
            '（P0c-9 族收口：phase5-3 残留裁定 + 职业可选奖励归零）')
    PROD_DECISIONS.write_text('\n'.join(header + body) + '\n', encoding='utf-8')
    print('生产裁定表 -> %s（%d 行）' % (PROD_DECISIONS, len(body)))

    # 3) 删除 ADOPT 行 XML（Path.unlink，不用 shell 删除）+ 同步 catalog
    deleted = 0
    for quest_id in adopt:
        path = Path(str(QUESTS) % quest_id)
        if path.exists():
            path.unlink()
            deleted += 1
    print('删除 XML %d 个' % deleted)
    catalog_path = REPO / 'src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml'
    catalog_lines = catalog_path.read_text(encoding='utf-8').splitlines()
    kept = [line for line in catalog_lines
        if not (line.strip().startswith('<definition ') and
            any('id="%d"' % q in line.split('resource=')[0] for q in adopt))]
    removed = len(catalog_lines) - len(kept)
    catalog_path.write_text('\n'.join(kept) + '\n', encoding='utf-8')
    print('catalog -> 移除 %d 条（余 %d）' % (removed, sum(1 for l in kept if '<definition' in l)))

    # 4) 缺口表 = 既有 KEEP + 本批 KEEP（按 id 去重、排序）
    gap_header = [line for line in GAPS.read_text(encoding='utf-8').splitlines() if line.startswith('#')]
    keep_rows = {}
    for line in GAPS.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        keep_rows[int(parts[0])] = parts[1].strip()
    for q in keep:
        keep_rows[q] = decisions[q][2]  # basis（稳定码）；decisions 元组 = (batch, verdict, basis, …)
    gap_header.append('# P0c-9（2026-09-24）：phase5-3 残留 23 行逐行裁定（16 ADOPT 退役 / 7 行并码 '
        'KILL_COVERAGE_LOSS），职业可选奖励 2 行随编译器落地转 ADOPT；本表 %d 行 KEEP。'
        % len(keep_rows))
    lines = gap_header + ['%d\t%s' % (q, keep_rows[q]) for q in sorted(keep_rows)]
    GAPS.write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print('缺口表 -> %d 行 KEEP（%s）' % (len(keep_rows),
        dict(Counter(keep_rows.values()))))

    # 5) phase5-3 拒绝表清空（23 行全部裁定完毕，留指针）
    PHASE53.write_text(
        '# phase5-3 旧拒绝表：已清空。\n'
        '# P0c-9（2026-09-24）：23 行经 p0c8c 判据机逐行裁定（16 ADOPT 退役 / 7 KEEP 并码入缺口表），\n'
        '# 逐行证据见 p0c9-decisions.tsv（batch=phase53）与 p0c9-phase53-shape-census.tsv。\n',
        encoding='utf-8')
    print('phase5-3 拒绝表 -> 清空（23 行全部裁定）')

    # 6) 28313 EnterWorld 边 → P0c-6 归一化登记表（可选 DB 归一化）
    norm_lines = P0C6_NORMALIZATION.read_text(encoding='utf-8').splitlines()
    known = {line.split('\t')[0] for line in norm_lines if line.strip() and not line.startswith('#')}
    added = 0
    for row in P0C6_ROWS_28313:
        if row[0] in known:
            continue
        norm_lines.append('\t'.join(row))
        added += 1
    P0C6_NORMALIZATION.write_text('\n'.join(norm_lines) + '\n', encoding='utf-8')
    print('p0c6 归一化登记 -> 追加 %d 行（28313）' % added)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
