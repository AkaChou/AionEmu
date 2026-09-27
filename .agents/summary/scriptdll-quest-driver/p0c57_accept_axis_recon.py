#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-57（续片 33）**接取轴侦察：纯事实 dump，无任何裁定**（只读，不写生产产物）。

背景：P0c-55 收口后登记在册的接取侧两轴——
  * `XML_ONLY_ACCEPT_ROLE_SPREAD_PENDING`（34 项 / 25 任务）：**接取页**（`SELECT1`/`SELECT1_1`…）被铺到
    **非接取 NPC**（与 P0c-42 `ACCEPT_PAGE_NOT_EMITTED` 方向相反）；
  * `ACCEPT_ENTRY_PAGE_WRONG_PENDING`（7 条）：被剪 NPC 的**对话入口行**（`QUEST_SELECT`）下发的页是**阶段页**。
本脚本只做一件事：把登记表里**所有下发接取族页 / 阶段族页的行**连同其 owner 与所属块逐条摊开，
供下一片设计判据（判据未定：接取页的下发是否由编译器 `acceptFlow` 覆盖、行的增量效应如何证，均在下一片裁）。

事实列（无 status 判定，只有机械分类）：
  quest_id  kind  seq  npc  action  source  target  pages  family_pages  npc_role  block_accept_npcs
其中
  * `kind`/`seq` = 行在登记表里的记录类型与序号（R/B/Q/N/P）；
  * `pages` = 该行 `actions` 列下发的页（`DIALOG:SHOW_QUEST_PAGE:<PAGE>`）；
  * `family_pages` = 上述页里属于**接取族**（`SELECT1[_n]`）或**阶段族**（`SELECT2..SELECT9[_n]`）的那些，形如 `ACCEPT:SELECT1,STAGE:SELECT2_1`；
    * `npc_role` = `START_NPC`（该 NPC 在**客户端模板索引**的 `start_npc_ids` 里 = 客户端声明的接取角色）/ `NON_START_NPC`（不在）；
    * `block_accept_npcs` = 该任务全部 `B NPC_START` 块的 npc 集（`-` = 无）——**注意**：块级 `NPC_START` 是
      "该 NPC 有对话入口"（链上每个 NPC 一个），**不是接取人**（判例：1484 的 5 个 NPC 各一块）⇒ 不可当 accept owner 用；
    * `client_start` = 客户端模板索引 `legacy-quest-dialog-template-index.csv` 的 `start_type`/`start_npc_ids`
      （`-` = 该任务不在索引里）；`client_end` 同源取 `end_npc_ids`。

用法 / Usage: python3 -B p0c57_accept_axis_recon.py [--registry <tsv>] [--out <tsv>]
"""
from __future__ import annotations

import argparse
import collections
import csv
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent.parent
REGISTRY = (REPO / 'src/main/resources/aion/data/static_data/quest_retail'
	/ 'quest_client_talk_chain_steps.tsv')
TEMPLATE_INDEX = REPO / 'docs/quest/client-dialog-mapping/legacy-quest-dialog-template-index.csv'

PAGE_TOKEN = re.compile(r'DIALOG:SHOW_QUEST_PAGE:([A-Z0-9_]+)')
ACCEPT_PAGE = re.compile(r'SELECT1(_\d+)?$')
STAGE_PAGE = re.compile(r'SELECT([2-9])(_\d+)?$')


def main() -> int:
	ap = argparse.ArgumentParser()
	ap.add_argument('--registry', default=str(REGISTRY))
	ap.add_argument('--out', default=str(HERE / 'p0c57-accept-axis-recon.tsv'))
	args = ap.parse_args()

	# 客户端模板索引：start 角色是**客户端声明的接取人**（轴权威；`start_type` 可为 TALK/FOBJ/ITEM…）。
	client = {}
	with TEMPLATE_INDEX.open(encoding='utf-8-sig') as fh:
		for row in csv.DictReader(fh):
			if not row['quest_id'].isdigit():
				continue
			starts = {t.strip() for t in (row.get('start_npc_ids') or '').split(',') if t.strip().isdigit()}
			ends = {t.strip() for t in (row.get('end_npc_ids') or '').split(',') if t.strip().isdigit()}
			client[int(row['quest_id'])] = (row.get('start_type', '').strip() or '-',
				starts, ends, row.get('template_type', '').strip() or '-')

	blocks = collections.OrderedDict()
	for line in Path(args.registry).read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		rec = line.split('\t')
		blocks.setdefault(int(rec[0]), []).append(rec)

	rows = []
	stats = collections.Counter()
	for qid, recs in blocks.items():
		accept_npcs = sorted({r[3] for r in recs if r[1] == 'B' and r[2] == 'NPC_START' and r[3].isdigit()})
		stype, starts, ends, ttype = client.get(qid, ('-', set(), set(), '-'))
		for r in recs:
			if r[1] != 'R':
				continue
			pages = [m.group(1) for m in PAGE_TOKEN.finditer(r[9])]
			if not pages:
				continue
			fam = []
			for p in pages:
				if ACCEPT_PAGE.match(p):
					fam.append('ACCEPT:' + p)
				elif STAGE_PAGE.match(p):
					fam.append('STAGE:' + p)
			if not fam:
				continue
			role = 'START_NPC' if r[3] in starts else 'NON_START_NPC'
			rows.append([str(qid), r[1], r[2], r[3], r[4], r[5], r[6], ','.join(pages),
				','.join(fam), role, ','.join(accept_npcs) or '-',
				'%s:%s' % (stype, ','.join(sorted(starts)) or '-'),
				','.join(sorted(ends)) or '-', ttype])
			stats['accept_family' if any(f.startswith('ACCEPT:') for f in fam) else 'stage_family_only'] += 1
			if any(f.startswith('ACCEPT:') for f in fam):
				stats['accept_' + role] += 1

	out = Path(args.out)
	with out.open('w', encoding='utf-8') as fh:
		fh.write('# P0c-57（续片 33）接取轴侦察：登记表内下发接取族/阶段族页的行（纯事实，无裁定）\n')
		fh.write('# 生成：p0c57_accept_axis_recon.py；登记表 = %s\n' % args.registry)
		fh.write('# 客户端索引 = %s\n' % TEMPLATE_INDEX)
		fh.write('# 列：quest_id\tkind\tseq\tnpc\taction\tsource\ttarget\tpages\tfamily_pages\tnpc_role'
			'\tblock_npc_start_npcs\tclient_start\tclient_end\ttemplate_type\n')
		fh.write('# 说明（纯事实，勿当裁定）\n')
		fh.write('#  1. 判据 = 行下发页里属**接取族**（`SELECT1[_n]`）或**阶段族**（`SELECT2..SELECT9[_n]`）者；\n'
			'#     `npc_role` 取客户端模板索引 `start_npc_ids` 成员关系（START_NPC / NON_START_NPC）。\n')
		fh.write('#  2. **块级 `B NPC_START` 不是接取人**——链上每个 NPC 各一块（判例：1484 的 5 个 NPC 各一块）；\n'
			'#     拿它当 accept owner 会把扩散面判成 0（本片首版实测踩到，见 `block_npc_start_npcs` 列多值）。\n')
		fh.write('#  3. **接取页扩散的机械基集**（`ACCEPT:*` ∧ `NON_START_NPC`）= %d 项 / %d 任务；\n'
			% (stats.get('accept_NON_START_NPC', 0),
				len({r[0] for r in rows if r[9] == 'NON_START_NPC' and 'ACCEPT:' in r[8]})))
		fh.write('#     与登记项 `XML_ONLY_ACCEPT_ROLE_SPREAD_PENDING`（34 项 / 25 任务，P0c-42/47 普查口径）**不一致** ⇒\n'
			'#     下一片第一步 = **复算登记口径**并解释差值（本集是登记例的**子集**：登记给出的\n'
			'#     `3087@700419/798144`、`2538@204805/790002`、`2914@204236`、`3037@798199`、`3041@700378`、\n'
			'#     `3093@203784/798177/798179`、`11010@730323/798906/798931`、`11103@798963/798973` 全部命中），\n'
			'#     差值疑为口径差（页面级 vs 行级 / 接取权威源不同），**不得**据此直接动手。\n')
		fh.write('#  4. 35017/45010/45017 的客户端索引是 `start_type=TALK` 且 `start_npc_ids` **空**（`end_npc_ids` 有值）\n'
			'#     ⇒ 这族的接取人客户端未解析；其"`QUEST_SELECT` 行下发**阶段页**"的 7 条属另一轴\n'
			'#     （`ACCEPT_ENTRY_PAGE_WRONG_PENDING`），与本文件的 `ACCEPT:*` 集**不相交**（此处只见 `STAGE:*`）。\n')
		for row in rows:
			fh.write('\t'.join(row) + '\n')
	print('P0C57_RECON_OK rows=%d quests=%d %s' % (len(rows), len({r[0] for r in rows}),
		dict(sorted(stats.items()))))
	print('P0C57_RECON_ACCEPT_SPREAD rows=%d quests=%d' % (
		stats.get('accept_NON_START_NPC', 0),
		len({r[0] for r in rows if r[9] == 'NON_START_NPC' and 'ACCEPT:' in r[8]})))
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
