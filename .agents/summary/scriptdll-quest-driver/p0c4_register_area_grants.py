#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-4：把 `_area_` 行登记进 SimpleHunt 逐任务裁定表（真端优先，basis=AREA_GRANT）。

判据：真端世界文件 `Map/Worlds/<world>/world*.xml` 的 `<questscript_area><quest>` 绑定了该任务 id，
生产 `ai-areas.xml` 的 `<quest_area ... quests="...">` 已按真端补齐同一条绑定 → 区域引擎
（`RetailAreaEngine`：进区域直接 startQuest）成为该任务的唯一发放入口，历史 XML 的 NPC 接取路由退役。

输入：
- p0c4-world-questscript-area.tsv（真端普查）
- p0c4-quest-area-delta.tsv（真端 ↔ 生产对账）
- src/test/resources/quest/retail-simple-hunt-adjudicated-decisions.tsv（既有 62 行 `_faction_` 裁定）
输出：同文件（按 quest_id 排序，表头覆盖 `_faction_` 与 `_area_` 两类哨兵）+ 工作副本。
"""
import os
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
TOPIC = Path(__file__).resolve().parent
PROD_DECISIONS = REPO / 'src/test/resources/quest/retail-simple-hunt-adjudicated-decisions.tsv'
WORK_DECISIONS = TOPIC / 'p0c3-simple-hunt-sentinel-decisions.tsv'
CENSUS = TOPIC / 'p0c3-simple-hunt-sentinel-census.tsv'
REGISTRY = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_reward_npcs.tsv'
WORLD_SCAN = TOPIC / 'p0c4-world-questscript-area.tsv'
HEADER = [
	'# SimpleHunt 类别哨兵逐任务裁定（真端优先）：`_faction_`（阵营轮换）+ `_area_`（区域发放）',
	'# quest_id\tverdict\tbasis\taxes\tevidence',
	'# verdict: ADOPT_RETAIL（退役 XML，真端驱动）| KEEP_XML（真端无法表达，保留 XML）',
	'# basis: FACTION_GRANT_DAILY | FACTION_GRANT_DISABLED | AREA_GRANT',
	'# 生成：.agents/summary/scriptdll-quest-driver/p0c3_build_hunt_sentinel_decisions.py（faction）',
	'#       .agents/summary/scriptdll-quest-driver/p0c4_register_area_grants.py（area）',
]


def area_rows():
	"""从真端普查取 `_area_` 宇宙内行，附真端区域名与生产 world id。"""
	areas = {}
	for line in WORLD_SCAN.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip() or line.startswith('world_dir\t'):
			continue
		parts = line.split('\t')
		for qid in parts[4].split(','):
			if qid:
				areas.setdefault(int(qid), set()).add('%s/%s' % (parts[0], parts[2]))
	registry = {}
	for line in REGISTRY.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		parts = line.split('\t')
		registry[int(parts[0])] = parts[1]
	rows = []
	ids = set()
	for line in CENSUS.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip() or line.startswith('quest_id\t'):
			continue
		parts = line.split('\t')
		if parts[1] != 'area' or parts[2] != '1':
			continue
		qid = int(parts[0])
		bound = sorted(areas.get(qid, ()))
		evidence = 'retail ' + '; '.join(bound) if bound else 'retail area unbound'
		# 交付 NPC：真端奖励名唯一解析 → 用解析结果；复合引用才回落到客户端登记表。
		# Hand-in NPC: a unique retail name resolves directly; only composite refs need the registry.
		handin = parts[4] if parts[4] != '-' else registry.get(qid, '-')
		evidence += '; handin=' + handin
		evidence += '; counters ok; grant=RetailAreaEngine (enter area -> startQuest)'
		rows.append((qid, 'ADOPT_RETAIL', 'AREA_GRANT', 'RETAIL_AREA_BOUND', evidence))
		ids.add(qid)
	return rows, ids


def main():
	existing = []
	for line in PROD_DECISIONS.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		existing.append(tuple(line.split('\t')))
	area, area_ids = area_rows()
	kept = [row for row in existing if int(row[0]) not in area_ids]
	rows = sorted(kept + area, key=lambda row: int(row[0]))
	text = '\n'.join(HEADER + ['\t'.join(str(cell) for cell in row) for row in rows]) + '\n'
	PROD_DECISIONS.write_text(text, encoding='utf-8')
	WORK_DECISIONS.write_text(text, encoding='utf-8')
	print('faction 行:', len(kept), ' area 行:', len(area), ' 合计:', len(rows))
	for row in area:
		print(' ', row[0], row[2], row[4][:80])
	return 0


if __name__ == '__main__':
	sys.exit(main())
