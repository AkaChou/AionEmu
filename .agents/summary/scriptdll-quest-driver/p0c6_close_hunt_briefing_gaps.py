#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-6：关闭 SimpleHunt `talk_npc1` 简报行的历史对话框轴缺口登记。

`build_retention_list.py` 的判定优先级是"降级 → 编译器拒绝 → 旧证据（phase5-3 / 对话框缺口表）→
家族分支（含逐任务裁定）"；因此已由裁定（`ADOPT_RETAIL`，basis=BRIEFING_STEP）收口的行，
必须先从两份旧的缺口证据文件里移除，否则会一直被压在 `XML_RETENTION/SEMANTIC_GAP:DIALOG_ROUTE`。

范围：真端表带 `talk_npc1` ∩ 本服宇宙（保留清单）行 = 23 行（22 行在
`simplehunt-dialog-route-gaps.txt`，1 行 24155 在 `phase5-3-rejections.txt`）。

判据（逐行均可复核）：
- 客户端任务书存在 select2 简报链（`quest_client_briefing_chains.tsv`，末按钮 SETPRO1/SETPRO2）；
- 客户端击杀行门控 `SECTION_5==0`（`Quest_unpacked/quest_monster.csv`）；
- `talk_npc1` 唯一解析到 NPC；报告页由客户端登记表给出（简报行 = select5/2375）。

输出：
- 就地裁剪 `simplehunt-dialog-route-gaps.txt` / `phase5-3-rejections.txt`（各留一行说明抬头）；
- `p0c6-briefing-gap-closure.tsv`（quest_id / 来源文件 / 原编码 / 新裁定路径）。
用法：python3 -B p0c6_close_hunt_briefing_gaps.py [--dry-run]
"""
import argparse
import re
from pathlib import Path

REPO = Path('/Users/mc/IdeaProjects/AionEmu-test')
HERE = Path(__file__).resolve().parent
TABLE = REPO / 'src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleHunt.xml'
RETENTION = REPO / 'src/test/resources/quest/retail-xml-retention.tsv'
GAPS = HERE / 'simplehunt-dialog-route-gaps.txt'
PHASE53 = HERE / 'phase5-3-rejections.txt'
CLOSURE = HERE / 'p0c6-briefing-gap-closure.tsv'

GAPS_NOTE = ('# P0c-6（2026-09-24）：移除 22 个 SimpleHunt `talk_npc1` 简报行——简报链\n'
	'# （select2 页链 + SECTION_5 标志位）已接线，逐行裁定 ADOPT_RETAIL，证据见\n'
	'# retail-simple-hunt-adjudicated-decisions.tsv 与 reports/2026-09-24-P0c6-*.zh-CN.md。\n')
PHASE53_HEADER = ('# P0c-6（2026-09-24）：移除 24155（NODE_FIELDS_MISMATCH——START 节点 [var0,var5]\n'
	'# 正是简报接线后的网格形状，缺口已闭合，见同上报告）。\n')


def briefing_ids():
	"""真端表带 talk_npc1 且在本服宇宙内的任务 id。 / Retail talk_npc1 rows inside the universe."""
	universe = set()
	for line in RETENTION.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		universe.add(int(line.split('\t')[0]))
	ids = []
	for match in re.finditer(r'<id id="(\d+)">([\s\S]*?)</id>', TABLE.read_text(encoding='utf-8')):
		quest_id = int(match.group(1))
		if quest_id in universe and 'talk_npc1' in match.group(2):
			ids.append(quest_id)
	return sorted(ids)


def main():
	parser = argparse.ArgumentParser()
	parser.add_argument('--dry-run', action='store_true')
	args = parser.parse_args()

	ids = briefing_ids()
	rows = []

	gap_lines = GAPS.read_text(encoding='utf-8').splitlines()
	gap_ids = {int(token) for line in gap_lines for token in line.split() if token.isdigit()}
	removed = sorted(set(ids) & gap_ids)
	# 保留既有注释行（M2-c 批次 2 的出处说明），只裁剪 id 行。
	# Keep the existing comment lines (M2-c batch 2 provenance); only prune the id lines.
	gap_comments = [line for line in gap_lines if line.startswith('#')]
	kept_gap_ids = [int(token) for line in gap_lines for token in line.split()
		if token.isdigit() and int(token) not in set(ids)]
	for quest_id in removed:
		rows.append((quest_id, 'simplehunt-dialog-route-gaps.txt', 'DIALOG_ROUTE', 'BRIEFING_STEP'))

	phase_lines = PHASE53.read_text(encoding='utf-8').splitlines()
	phase_kept, phase_removed = [], []
	for line in phase_lines:
		match = re.match(r'(\d+):(\w+)', line)
		if match and int(match.group(1)) in set(ids):
			phase_removed.append((int(match.group(1)), match.group(2), line))
		else:
			phase_kept.append(line)
	for quest_id, code, _ in phase_removed:
		rows.append((quest_id, 'phase5-3-rejections.txt', code, 'BRIEFING_STEP'))

	rows.sort(key=lambda row: row[0])
	text = ('# P0c-6 简报行缺口关闭账（quest_id / 来源文件 / 原编码 / 裁定路径）\n'
		+ ''.join('\t'.join(str(cell) for cell in row) + '\n' for row in rows))
	if args.dry_run:
		print('dry-run：将移除', len(rows), '行')
		for row in rows:
			print(' ', row)
		print('gaps 剩余', len(kept_gap_ids), '| phase5-3 剩余', len(phase_kept))
		return 0

	GAPS.write_text('\n'.join(gap_comments + [GAPS_NOTE.rstrip('\n')])
		+ '\n' + '\n'.join(str(qid) for qid in kept_gap_ids) + '\n', encoding='utf-8')
	PHASE53.write_text(PHASE53_HEADER + '\n'.join(phase_kept) + '\n', encoding='utf-8')
	CLOSURE.write_text(text, encoding='utf-8')
	print('关闭账 ->', CLOSURE, '（%d 行：gaps %d / phase5-3 %d）'
		% (len(rows), len(removed), len(phase_removed)))
	print('gaps 剩余', len(kept_gap_ids), '| phase5-3 剩余', len(phase_kept))
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
