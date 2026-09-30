#!/usr/bin/env python3
"""P5：DataDriven 采纳行裁定（ADOPT_RETAIL）。

输入 = 漂移登记 retail-data-driven-drift.tsv（ADOPTED / REJECTED:code）+ 形状普查
p5-datadriven-shape-census.tsv（acquire / step_sequence）。
ADOPTED 行按形状给 basis：
- 步骤链全是 hunt          → DD_TALK_HUNT_GRID（P5-1：复用 hunt 计数网格）；
- 步骤链全是 collectitem   → DD_TALK_COLLECT_CANONICAL（P5-2：采集族规范形 = 交付检查对 + npc-complete）；
- 无进度块（交付即完成）     → DD_TALK_SIMPLE（P5-3 wave A：极简对话信件）。
- 带 EnterArea/Hunt 的交织链 → DD_TALK_HUNT_CHAIN_CLIENT_ROWS（真端有序步骤与客户端行、
  SECTION 计数/对话按钮及区域登记对齐）。
REJECTED → 不进裁定（未覆盖形状留 FAMILY_PENDING 待后续批）。

输出：p5-datadriven-decisions.tsv。
"""
import os
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
DRIFT = REPO / 'src/test/resources/quest/retail-data-driven-drift.tsv'
CENSUS = REPO / '.agents/summary/scriptdll-quest-driver/p5-datadriven-shape-census.tsv'
OUT = REPO / '.agents/summary/scriptdll-quest-driver/p5-datadriven-decisions.tsv'

EVIDENCE = {
	'hunt': ('Pure Hunt progress: hunt counter grid reused; client gates '
		'(quest_monster.csv SECTION chains) verified per row; canonical npc-start/report/complete flows; '
		'acquire is a plain NPC accept (Talk) or the world quest_area area grant (P0c-4 binding)'),
	'collectitem': ('Talk acquire + pure CollectItem progress: collect-family canonical shape '
		'(work items from retail metadata, CHECK_USER_HAS_QUEST_ITEM(39)/SIMPLE(20002) pairs, npc-complete '
		'8..23 with choice binding); reward projection = client quest_summary last row (QE-051)'),
	'noprog': ('Talk acquire without any progress block: the report NPC dialogue completes the quest '
		'(minimal talk letter: select_none accept window + select_success, buttons 20000/20001/1009 only); '
		'reward projection = client quest_summary last row (QE-051)'),
	'talk': ('Talk acquire with a pure Talk chain: step i pairs with the client select{i} page, '
		'SETPRO{i} advances intermediate stages, the final SET_SUCCEED reaches the reward state '
		'(1876 chain contract XML is the shape authority; identical var0 6-bit ladder)'),
	'pvp': ('Pure PVP counter progress: the grid emits the world-wildcard KillInWorld(0) event '
		'per step (retail typed-definition wildcard); acquire is a plain NPC accept or the world '
		'quest_area area grant (P0c-4 binding), report flows reuse the canonical hunt report pages'),
	'talkhunt': ('Sequential talk+hunt chain from the retail ordered steps: the talk step carries '
		'its Aion 5.8 client letter-page ladder, hunt counters use the client SECTION_n gates, '
		'and SECTION_0 advances one journal row per completed step'),
	'talkcollecthunt': ('Sequential three-kind chain from the retail ordered steps: client '
		'letter-page ladders, grouped item hand-in checks and SECTION_n hunt counters advance '
		'the SECTION_0 journal row; the reward projects the client final row'),
	'clientrows': ('Retail ordered talk/collectitem/hunt/enterarea steps, including world or area '
		'acquisition, align one-for-one with Aion 5.8 client journal rows; client letter pages, '
		'item hand-in buttons, SECTION_n hunt gates and sensory area registry determine transitions'),
	'talkcollect': ('Sequential talk+collectitem chain on a single var0 ladder: the talk step '
		'advances via its client letter-page ladder, the collectitem step via the group hand-in '
		'check (action 39) that consumes the whole requirement set; the final SET_SUCCEED grants '
		'the quest work items and the reward row is the client journal last row (QE-051)'),
}
AXES = {
	'hunt': 'retail-grid-vs-legacy-shell',
	'collectitem': 'node-projection+transition-set (legacy XML: width=1 var0, 1-based reward row, no 20002 route)',
	'noprog': 'visit-completes (legacy shells carry no route evidence to compare)',
	'talk': 'stage-ladder (1876 contract XML is the shape authority; identical var0 6-bit ladder)',
	'pvp': 'grid-counter (KillInWorld(0) wildcard edges; SECTION client gating identical to hunt grids)',
	'talkhunt': 'client-journal-row ladder (letter pages + SECTION_n hunt counters)',
	'talkcollect': 'single-ladder (talk letter stages + group hand-in checks advancing one row per step)',
	'talkcollecthunt': 'client-journal-row ladder (letter pages + group hand-in + SECTION_n hunt counters)',
	'clientrows': 'mixed client rows (enterarea stages + letter pages + hand-in + SECTION_n hunt counters)',
}
BASIS = {
	'hunt': {'talk': 'DD_TALK_HUNT_GRID', 'area': 'DD_AREA_HUNT_GRID'},
	'collectitem': {'talk': 'DD_TALK_COLLECT_CANONICAL'},
	'noprog': {'talk': 'DD_TALK_SIMPLE'},
	'talk': {'talk': 'DD_TALK_CHAIN'},
	'pvp': {'talk': 'DD_PVP_GRID', 'area': 'DD_PVP_GRID'},
	'talkhunt': {'talk': 'DD_TALK_HUNT_CHAIN'},
	'talkcollect': {'talk': 'DD_TALK_COLLECT_CHAIN'},
	'talkcollecthunt': {'talk': 'DD_TALK_COLLECT_HUNT_CHAIN'},
	'clientrows': {'talk': 'DD_TALK_HUNT_CHAIN_CLIENT_ROWS'},
}


def acquire_axis(kind, acquire):
	"""系统发放（EnterArea/none）= area 轴；其余（Talk 接取）= talk 轴。/ System grant = area axis."""
	if kind in ('hunt', 'pvp') and acquire in ('enterarea', 'none'):
		return 'area'
	return 'talk'


def shapes():
	out = {}
	for line in CENSUS.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		parts = line.split('\t')
		steps = [step.strip().lower() for step in parts[2].replace('-', '').split('>') if step.strip()]
		acquire = parts[1].strip().lower() if len(parts) > 1 else 'talk'
		if not steps:
			out[parts[0]] = (acquire, 'noprog')
		elif all(step == 'hunt' for step in steps):
			out[parts[0]] = (acquire, 'hunt')
		elif all(step == 'collectitem' for step in steps):
			out[parts[0]] = (acquire, 'collectitem')
		elif all(step == 'talk' for step in steps):
			out[parts[0]] = (acquire, 'talk')
		elif all(step == 'pvp' for step in steps):
			out[parts[0]] = (acquire, 'pvp')
		elif all(step in ('talk', 'hunt') for step in steps):
			# 混合 {talk, hunt} 链（切片 1）：talk 步挂信件页梯、hunt 步挂逐杀链态。
			# Mixed {talk, hunt} chain (slice 1): talks carry letter ladders, hunts carry
			# per-kill chain states.
			out[parts[0]] = (acquire, 'talkhunt')
		elif all(step in ('talk', 'collectitem') for step in steps):
			# 混合 {talk, collectitem} 链（切片 2）：单 var0 阶梯，talk 步页梯推进、
			# collectitem 步 39 检查整组过/扣。
			# Mixed {talk, collectitem} chain (slice 2): a single var0 ladder — talks advance
			# via letter ladders, collectitem steps via the group hand-in check.
			out[parts[0]] = (acquire, 'talkcollect')
		elif all(step in ('talk', 'collectitem', 'hunt') for step in steps):
			# 三族混合链（切片 3）：talk 信件页梯 + collectitem 整组检查 + hunt 计数段同链。
			# Three-kind mixed chain (slice 3): letter ladders, the group hand-in check and hunt
			# counter stages in one chain.
			out[parts[0]] = (acquire, 'talkcollecthunt')
		elif any(step == 'enterarea' for step in steps) and all(
				step in ('talk', 'collectitem', 'hunt', 'enterarea') for step in steps):
			# EnterArea 占客户端行，不消耗对话阶段；带 Hunt 的混合链由同一行阶梯合成器覆盖。
			# EnterArea occupies a client row without consuming a talk stage; the mixed-chain
			# compiler covers the remaining hunt and hand-in steps on the same row ladder.
			out[parts[0]] = (acquire, 'clientrows')
	return out


def main():
	shape = shapes()
	rows = ['# P5 DataDriven 逐任务裁定（真端优先；由 p5_build_datadriven_decisions.py 生成）',
		'# quest_id\tverdict\tbasis\taxes\tevidence']
	counts = {}
	for line in DRIFT.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		quest_id, cls = line.split('\t')[0], line.split('\t')[1]
		if cls != 'ADOPTED':
			counts['skip'] = counts.get('skip', 0) + 1
			continue
		shape_row = shape.get(quest_id)
		if shape_row is None:
			counts['unshaped'] = counts.get('unshaped', 0) + 1
			continue
		acquire, kind = shape_row
		axis = acquire_axis(kind, acquire)
		basis = BASIS.get(kind, {}).get(axis)
		if basis is None:
			counts['unshaped'] = counts.get('unshaped', 0) + 1
			continue
		rows.append(f'{quest_id}\tADOPT_RETAIL\t{basis}\t{AXES[kind]}\t{EVIDENCE[kind]}')
		counts[basis] = counts.get(basis, 0) + 1
	OUT.write_text('\n'.join(rows) + '\n', encoding='utf-8')
	print(f'decisions={len(rows) - 2} verdicts={counts}')
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
