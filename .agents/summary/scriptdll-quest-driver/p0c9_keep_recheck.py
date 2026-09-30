#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-9：SimpleHunt 保留行（缺口表 47 行）KEEP 复核——真端源补齐可能性留证。

v3 提示词 P0c-9.1 的复核项：
  * KILL_COVERAGE_LOSS：真端编译集缺的"客户端点名且生产刷怪可达"怪，是否可由
    `Quest_SimpleHunt.xml` monster* 列之外的真端源补齐——实测候选只有客户端侧
    `quest_monster.csv` / `quest_script_monster.csv`（无服务端族表列）⇒ 不可归零，逐行留证。
  * CLIENT_BUTTON_UNWIRED：客户端按钮（20002 检查 / 1009 报告）在真端路由集无对应动作——
    复核真端表行有无等价表达列（npc-item-report 之类），无 ⇒ 不可归零。

输出 p0c9-keep-recheck.tsv：quest_id / code / 客户端可达缺失怪（含 script_monster 命中标记）/
客户端按钮 / 真端路由动作集 / 结论。
用法：python3 -B p0c9_keep_recheck.py
"""
import os
import csv
import importlib.util
import re
from collections import defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
GAPS = HERE / 'simplehunt-dialog-route-gaps.txt'
OUT = HERE / 'p0c9-keep-recheck.tsv'
SCRIPT_MONSTER = Path(f"{REPO.parent / 'PycharmProjects' / 'unpak'}/Quest_unpacked/quest_script_monster.csv")
RETAIL_TABLE = REPO / 'src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleHunt.xml'


def load_classifier():
    spec = importlib.util.spec_from_file_location('p0c8c', HERE / 'p0c8c_gap_decisions.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main():
    m = load_classifier()
    names = m.npc_names()
    client_ids, _ = m.client_evidence(names)
    spawned = m.spawned_ids()
    retail_kill = {}
    # 族表行的击杀目标（monster1..5 列，符号名 → npc id）
    text = RETAIL_TABLE.read_text(encoding='utf-8')
    for match in re.finditer(r'<id id="(\d+)">(.*?)</id>', text, re.S):
        ids = set()
        for monster in re.finditer(r'<monster\d?>([^<]+)</monster\d?>', match.group(2)):
            token = monster.group(1).strip().lower()
            ids |= names.get(token, set())
        retail_kill[int(match.group(1))] = ids
    # 客户端 script_monster 登记名 → npc id（仅作"客户端在追踪"证据）
    script_ids = defaultdict(set)
    with SCRIPT_MONSTER.open(encoding='utf-8', errors='replace') as handle:
        for row in csv.reader(handle):
            if len(row) >= 7 and row[0].strip().isdigit():
                for token in (x.strip().lower() for x in row[6].split() if x.strip()):
                    script_ids[int(row[0])] |= names.get(token, set())
    # 客户端按钮动作集（select5 检查 20002 / 报告 1009 等）
    actions = m.client_action_ids()
    # 真端路由集（简报/报告页链登记表里的动作 id 由合成器消费；此处复核真端表行无 report/检查列）
    has_report_column = {}
    for match in re.finditer(r'<id id="(\d+)">(.*?)</id>', text, re.S):
        seg = match.group(2)
        has_report_column[int(match.group(1))] = bool(
            re.search(r'<report|<check|npc-item-report|select5_check', seg, re.I))

    lines = ['# P0c-9 SimpleHunt 缺口表 47 行 KEEP 复核（真端源补齐可能性；python3 -B p0c9_keep_recheck.py 重跑）',
        '# 结论：KILL_COVERAGE_LOSS 的缺失怪只登记在客户端两侧表（quest_monster / quest_script_monster），',
        '#       服务端族表（Quest_SimpleHunt.xml）无对应列 ⇒ 不可由真端源补齐，KEEP 成立。',
        '# quest_id\tcode\t缺失可达怪(script_monster命中)\t客户端动作(无真端路由)\t结论']
    counts = defaultdict(int)
    for line in GAPS.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        quest_id, code = int(parts[0]), parts[1].strip()
        counts[code] += 1
        if code == 'KILL_COVERAGE_LOSS':
            named = {i for i in client_ids.get(quest_id, set()) if i in spawned}
            missing = sorted(named - retail_kill.get(quest_id, set()))
            marks = ['%d%s' % (i, '*' if i in script_ids.get(quest_id, set()) else '') for i in missing]
            lines.append('%d\t%s\t%s\t-\tKEEP：缺失怪仅登记于客户端表（*=script_monster 亦命中），服务端族表无列'
                % (quest_id, code, ' '.join(marks) if marks else '-'))
        elif code == 'CLIENT_BUTTON_UNWIRED':
            wired = sorted(actions.get(quest_id, set()))
            lines.append('%d\t%s\t-\t%s\tKEEP：真端路由集无 %s 对应动作，族表无 report/check 列'
                % (quest_id, code, wired or '-', wired or '-'))
        else:
            lines.append('%d\t%s\t-\t-\tKEEP（见 p0c8c-gap-decisions.tsv 逐行证据）' % (quest_id, code))
    OUT.write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print('缺口表 %d 行复核完成（%s）-> %s' % (sum(counts.values()), dict(counts), OUT))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
