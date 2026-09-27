#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-54 阶段页轴普查：入口页缺失 + 跨 NPC 扩散（只读取证，不改任何产物）。

形状（本片判据的取证基础 / evidence basis for the P0c-54 stage-page axis）：
  * 客户端每个阶段 k（1..K）有一条**页链**：入口页 SELECT{k+1} ──(按钮动作=页号共号空间)──>
    续页 SELECT{k+1}_x … ──> 带 HACTION_SETPRO{k} 的末页（「结束对话。」）；
  * 页链上的每一页都必须**由阶段 NPC（真端 talk_npc{k}）**的 R 行下发；
  * 两种缺陷形：(A) 入口页从未被任何行下发（⇒ 审计 CLIENT_PAGE_UNREACHED，玩家看不到阶段对话起点）；
    (B) 某页被**非本阶段 NPC**（接取 NPC / 别的阶段 NPC）下发（⇒ 越界页，按钮在那边无路由）。

输出：TSV，一行一个 (quest, stage, page) 事实：
  quest_id stage page expected_npc emitted_by status detail
其中 emitted_by 形如 `npc:seq:src>dst|npc:seq:...`（- 表示无行下发）。
status ∈ {OK, MISSING_ENTRY, WRONG_NPC, MISSING_AND_WRONG}。

用法 / Usage:
  python3 -B p0c54_stage_page_census.py [--registry <tsv>] [--out <tsv>]
"""
from __future__ import annotations

import argparse
import collections
import csv
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent.parent
CLIENT_CSV = REPO / 'docs/quest/client-dialog-mapping/quest-dialog-pages.csv'
CLIENT_ACTIONS = REPO / 'docs/quest/client-dialog-mapping/quest-dialog-action-details.csv'
DEFAULT_REGISTRY = (REPO / 'src/main/resources/aion/data/static_data/quest_retail'
    / 'quest_client_talk_chain_steps.tsv')

SHOW = 'DIALOG:SHOW_QUEST_PAGE:'


def load_client():
	"""页名/页号/按钮动作三表（与生成器同口径：active + exact 行）。"""
	pages = collections.defaultdict(set)
	page_ids = collections.defaultdict(dict)          # action id -> page NAME（共号空间）
	id_by_name = collections.defaultdict(dict)        # page NAME -> page id
	with CLIENT_CSV.open(encoding='utf-8-sig') as fh:
		rd = csv.DictReader(fh)
		for row in rd:
			qid = int(row['quest_id'])
			name = row['html_page_name'].upper()
			pages[qid].add(name)
			if row['page_id'].isdigit():
				page_ids[qid][int(row['page_id'])] = name
				id_by_name[qid][name] = int(row['page_id'])
	actions = collections.defaultdict(lambda: collections.defaultdict(set))
	with CLIENT_ACTIONS.open(encoding='utf-8-sig', newline='') as fh:
		for row in csv.DictReader(fh):
			if (row['source_variant'] == 'active' and row['page_mapping'] == 'exact'
					and row['action_id'].isdigit()):
				actions[int(row['quest_id'])][row['html_page_name'].lower()].add(int(row['action_id']))
	return pages, page_ids, id_by_name, actions


def stage_chain(qid, stage, actions, page_ids, pages):
	"""阶段 k 的客户端页链（入口 → … → 带 SETPRO 的末页）。返回 (chain, button_page, note)。"""
	entry = 'SELECT%d' % (stage + 1)
	if entry not in pages.get(qid, set()):
		return [], None, 'ENTRY_NOT_IN_CLIENT'
	advance = 10000 + stage - 1
	chain, current, visited = [], entry, set()
	while True:
		acts = actions.get(qid, {}).get(current.lower(), set())
		if advance in acts:
			return chain + [current], current, ''
		cont = [(a, page_ids[qid][a]) for a in acts
			if a in page_ids.get(qid, {}) and str(page_ids[qid][a]).startswith('SELECT')]
		if len(cont) != 1:
			return chain + [current], None, 'CHAIN_BROKEN(%d cont)' % len(cont)
		chain.append(current)
		current = cont[0][1]
		if current in visited:
			return chain + [current], None, 'CHAIN_LOOP'
		visited.add(current)


def main() -> int:
	ap = argparse.ArgumentParser()
	ap.add_argument('--registry', default=str(DEFAULT_REGISTRY))
	ap.add_argument('--out', default=str(HERE / 'p0c54-stage-page-census.tsv'))
	args = ap.parse_args()

	pages, page_ids, id_by_name, actions = load_client()
	rows = collections.defaultdict(list)
	stages = collections.defaultdict(set)
	for line in Path(args.registry).read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		p = line.split('\t')
		qid = int(p[0])
		rows[qid].append(p)
		# 阶段集合以 **SETPRO<k> 行**为准（末阶段的 dst 是 reward、无 s{k} 节点；单阶段任务更是压平形
		# 没有 s1 节点）——用 s{k} 节点取阶段会漏掉末阶段与压平形（本普查首版即如此假绿）。
		if p[1] == 'R' and re.fullmatch(r'SETPRO\d+', p[4]):
			stages[qid].add(int(p[4][6:]))

	out, stats = [], collections.Counter()
	for qid in sorted(stages):
		# 阶段 NPC 参照 = 该阶段 SETPRO<k> 行的 npc（P0c-47 已验其 == 真端 talk_npc<k>）。
		stage_npc = {}
		for p in rows[qid]:
			if p[1] == 'R' and re.fullmatch(r'SETPRO\d+', p[4]):
				stage_npc[int(p[4][6:])] = p[3]
		for k in sorted(stages[qid]):
			chain, button, note = stage_chain(qid, k, actions, page_ids, pages)
			if not chain:
				stats['STAGE_NO_CLIENT_PAGES'] += 1
				out.append((qid, k, '-', stage_npc.get(k, '?'), '-', 'ENTRY_NOT_IN_CLIENT', note))
				continue
			expect = stage_npc.get(k, '?')
			for page in dict.fromkeys(chain):
				# 精确令牌匹配（页名有前缀关系：SELECT2 ⊂ SELECT2_1 —— 子串匹配会假绿）。
				token = SHOW + page
				emitters = [p for p in rows[qid]
					if p[1] == 'R' and token in p[9].split(';')]
				by = '|'.join('%s:%s:%s>%s' % (p[3], p[2], p[5], p[6]) for p in emitters) or '-'
				owner = [p for p in emitters if p[3] == expect]
				if not emitters:
					status = 'MISSING_ENTRY' if page == chain[0] else 'MISSING_CONT'
				elif not owner:
					status = 'WRONG_NPC'
				elif len(emitters) > len(owner):
					status = 'SPREAD_DUPLICATE'
				else:
					status = 'OK'
				stats[status] += 1
				if status != 'OK':
					out.append((qid, k, page, expect, by, status, note))
	path = Path(args.out)
	with path.open('w', encoding='utf-8') as fh:
		fh.write('# P0c-54 阶段页轴普查（入口页缺失 + 跨 NPC 扩散）: registry=%s\n' % args.registry)
		fh.write('# quest_id\tstage\tpage\texpected_npc\temitted_by\tstatus\tdetail\n')
		for r in out:
			fh.write('%s\t%s\t%s\t%s\t%s\t%s\t%s\n' % r)
	print('P0C54_CENSUS rows=%d %s out=%s' % (len(out), dict(sorted(stats.items())), path))
	return 0


if __name__ == '__main__':
	sys.exit(main())
