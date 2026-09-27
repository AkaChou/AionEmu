#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-55 阶段腿**逐阶段重建**普查（只读）：真端 `talk_npc<k>` × 客户端页链 × 登记表现状三方对拍。

背景（P0c-54 的硬轴剔除）：9 个任务（1323/1394/1484/2480/2538/3093/4052/11010/11103）的阶段腿在
XML 期被压成「每阶段同一个 `SETPRO1` + 每阶段同一组 `SELECT2/SELECT2_1` 页」——真端声明 K 个
`talk_npc<k>`、客户端每阶段各有自己的页链（`SELECT{k+1}` → 按钮 → `SELECT{k+1}_1` → `HACTION_SETPRO{k}`），
而登记表 2..K 阶段的页/推进动作仍是第一阶段的值 ⇒ 客户端页 1693/1694/2034/2035/2375 从未被下发
（`QuestDialogOrderAudit` 的 `CLIENT_PAGE_UNREACHED` 34 行）。

锚点（owner 优先，不靠页名自证）：
  * 阶段 k 的**腿** = 登记表里 owner == 真端 `talk_npc<k>` 的 `SETPRO<k>` 行；K 条腿必须逐跳相接
    （`started → … → packed K 节点`）；不在链上的 `SETPRO` 行 ≠ 领奖窗行（`SHOW_SELECT_QUEST_REWARD_WINDOW1`）即越界；
  * 阶段 k 的**页行** = 该 owner 在腿的源状态上（`leg_k.src`）下发页的行，页序列必须等于客户端页链；
  * 阶段页被「非 walk 归属」的行下发 = 扩散（P0c-54 同域剪除）。
verdict ∈ {OK, PAGE_CHAIN_WRONG, LEG_MISSING, LEG_DUPLICATE, LEG_ACTION_WRONG, LEG_CHAIN_BROKEN,
           STAGE_SPREAD, CHAIN_INCOMPLETE, REPORT_OK, REPORT_MISSING, REPORT_PAGE_WRONG}。

用法 / Usage: python3 -B p0c55_stage_leg_census.py [--registry <tsv>] [--out <tsv>]
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
RETAIL_XML = REPO / 'src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml'
REGISTRY = (REPO / 'src/main/resources/aion/data/static_data/quest_retail'
	/ 'quest_client_talk_chain_steps.tsv')
SUMMARY_ROWS = (REPO / 'src/main/resources/aion/data/static_data/quest_retail'
	/ 'quest_client_summary_rows.tsv')
CLIENT_CSV = REPO / 'docs/quest/client-dialog-mapping/quest-dialog-pages.csv'
CLIENT_ACTIONS = REPO / 'docs/quest/client-dialog-mapping/quest-dialog-action-details.csv'
NPCS = HERE / 'npc_name_index.tsv'
DECISIONS = HERE / 'p0c55-stage-leg-decisions.tsv'

PAGE_TOKEN = re.compile(r'DIALOG:SHOW_QUEST_PAGE:([A-Z0-9_]+)')
WINDOW_TOKEN = 'DIALOG:SHOW_QUEST_PAGE:SHOW_SELECT_QUEST_REWARD_WINDOW'


def load_decisions(path):
	"""裁定表：quest_id \t K \t report_page \t basis（basis 只作留痕，不参与判定）。"""
	rows = collections.OrderedDict()
	for line in path.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		p = line.split('\t')
		rows[int(p[0])] = (int(p[1]), p[2].strip().upper(), p[3])
	return rows


def load_npc_index(path):
	index = collections.defaultdict(set)
	for line in path.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		p = line.split('\t')
		for token in p[1:]:
			if token.isdigit():
				index[p[0]].add(int(token))
	return index


def load_client(path_pages, path_actions):
	"""页册：pages[q][PAGE] = page_id；actions[q][PAGE] = [(action_id, constant, button)]。"""
	pages = collections.defaultdict(dict)
	with path_pages.open(encoding='utf-8-sig') as fh:
		for row in csv.DictReader(fh):
			if row['page_mapping'] != 'exact' or not row['page_id'].isdigit():
				continue
			pages[int(row['quest_id'])][row['html_page_name'].upper()] = int(row['page_id'])
	actions = collections.defaultdict(lambda: collections.defaultdict(list))
	with path_actions.open(encoding='utf-8-sig') as fh:
		for row in csv.DictReader(fh):
			if (row['source_variant'] != 'active' or row['action_mapping'] != 'exact'
					or not row['action_id'].isdigit()):
				continue
			actions[int(row['quest_id'])][row['html_page_name'].upper()].append(
				(int(row['action_id']), row['action_constant']))
	return pages, actions


def stage_chain(q, stage, pages, actions):
	"""客户端阶段页链：入口 `SELECT{k+1}` →（唯一延续按钮）→ … → 带 `HACTION_SETPRO{k}` 的页。"""
	entry = 'SELECT%d' % (stage + 1)
	if entry not in pages.get(q, {}):
		return [], None
	advance_id = 10000 + stage - 1
	chain, current, visited = [], entry, set()
	while True:
		buttons = actions.get(q, {}).get(current, [])
		if any(a == advance_id for a, _c in buttons):
			return chain + [current], advance_id
		cont = [c for _a, c in buttons if c.startswith('HACTION_SELECT')]
		if len(cont) != 1:
			return [], None
		target = cont[0][len('HACTION_'):]
		if target not in pages.get(q, {}):
			return [], None
		chain.append(current)
		current = target
		if current in visited:
			return [], None
		visited.add(current)


def main() -> int:
	ap = argparse.ArgumentParser()
	ap.add_argument('--registry', default=str(REGISTRY))
	ap.add_argument('--out', default=str(HERE / 'p0c55-stage-leg-census.tsv'))
	args = ap.parse_args()

	decisions = load_decisions(DECISIONS)
	body = {int(m.group(1)): m.group(2) for m in
		re.finditer(r'<id id="(\d+)">(.*?)</id>', RETAIL_XML.read_text(encoding='utf-8'), re.S)}
	npc_index = load_npc_index(NPCS)
	pages, actions = load_client(CLIENT_CSV, CLIENT_ACTIONS)
	summary = {}
	for line in SUMMARY_ROWS.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		p = line.split('\t')
		summary[int(p[0])] = int(p[1])

	blocks = collections.OrderedDict()
	for line in Path(args.registry).read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		blocks.setdefault(int(line.split('\t')[0]), []).append(line.split('\t'))

	def resolve(name):
		ids = npc_index.get((name or '').strip(), set())
		return next(iter(ids)) if len(ids) == 1 else None

	def page_of(rec):
		return [m.group(1) for m in PAGE_TOKEN.finditer(rec[9])]

	out_rows = []
	stats = collections.Counter()
	for q, (k_declared, want_report, basis) in decisions.items():
		recs = blocks.get(q, [])
		b = body.get(q, '')
		nodes = {r[2]: (r[3], r[4]) for r in recs if r[1] == 'N'}
		packed_k = [name for name, (status, packed) in nodes.items() if packed == str(k_declared)
			and status in ('START', 'REWARD')]
		talks = []
		for k in range(1, k_declared + 1):
			m = re.search(r'<talk_npc%d>([^<]*)</talk_npc%d>' % (k, k), b)
			name = m.group(1).strip() if m else ''
			talks.append((name, resolve(name)))
		rew = re.search(r'<reward_npc_name>([^<]*)</reward_npc_name>', b)
		reward_name = rew.group(1).strip() if rew else ''
		reward_id = resolve(reward_name)
		note = 'summary=%s(K+1=%d) packed%d=%s' % (summary.get(q), k_declared + 1, k_declared,
			','.join(sorted(packed_k)) or '-')
		chains, stage_pages, bad_chain = {}, {}, False
		for k in range(1, k_declared + 1):
			chain, advance = stage_chain(q, k, pages, actions)
			chains[k] = (chain, advance)
			bad_chain = bad_chain or not chain
			for name in chain:
				stage_pages[name] = k
		rp = want_report if want_report in pages.get(q, {}) else None
		if rp is None or rp in stage_pages:
			bad_chain = True
		if bad_chain:
			out_rows.append([q, 0, '-', '%s/%s' % (reward_name, reward_id), '-', '-', '-', '-',
				'CHAIN_INCOMPLETE', note])
			stats['CHAIN_INCOMPLETE'] += 1
			continue
		# 腿：owner == talk_npc<k> 的 SETPRO 行，且按链位逐跳相接（`src` = 前一跳的 dst；首跳 src=started）。
		# accept 相位（src=unaccepted）的 SETPRO 行不是腿；talk1 == talk3 的合法重复由链位消歧。
		legs = {}
		leg_recs = {}
		cursor = 'started'
		for k in range(1, k_declared + 1):
			hits = [r for r in recs if r[1] == 'R' and r[3] == str(talks[k - 1][1])
				and re.fullmatch(r'SETPRO\d+', r[4]) and r[5] == cursor]
			legs[k] = hits
			if len(hits) == 1:
				leg_recs[k] = hits[0]
				cursor = hits[0][6]
			else:
				break
		# walk 归属：(owner, src) 集合，用于扩散判定
		legit = set()
		entry_rows_seen = set()
		for k, rec in leg_recs.items():
			legit.add((str(talks[k - 1][1]), rec[5]))
		for k in range(1, k_declared + 1):
			chain, advance = chains[k]
			tname, tid = talks[k - 1]
			hits = legs.get(k, [])
			leg = leg_recs.get(k)
			if leg is None:
				status = 'LEG_DUPLICATE' if len(hits) > 1 else 'LEG_MISSING'
				stats[status] += 1
				out_rows.append([q, k, '-', '%s/%s' % (tname, tid),
					'>'.join('%s(%s)' % (p, pages[q][p]) for p in chain), '-',
					';'.join('r%s' % r[2] for r in hits) or '-', '-', status, note])
				continue
			src, dst = leg[5], leg[6]
			want_action = 'SETPRO%d' % k
			owner_rows = [r for r in recs if r[1] == 'R' and r[3] == str(tid) and r[5] == src
				and page_of(r)]
			# 页行按动作类分派：页名动作行 = 续页行；其余（QUEST_SELECT/USE_OBJECT…）= 入口页行。
			entry_rows = [r for r in owner_rows if not re.fullmatch(r'SELECT\d+(_\d+)?', r[4])]
			cont_rows = [r for r in owner_rows if re.fullmatch(r'SELECT\d+(_\d+)?', r[4])]
			want_entry = chain[0]
			want_cont = chain[-1] if len(chain) > 1 else None
			ok_pages = (bool(entry_rows) and all(page_of(r) == [want_entry] for r in entry_rows)
				and len(cont_rows) <= 1
				and all(page_of(r) == [want_cont] for r in cont_rows))
			got_pages = [p for r in entry_rows + cont_rows for p in page_of(r)]
			spread = [r for r in recs if r[1] == 'R'
				and any(p in stage_pages for p in page_of(r))
				and (r[3], r[5]) not in legit]
			orphan_legs = [r for r in recs if r[1] == 'R' and re.fullmatch(r'SETPRO\d+', r[4])
				and r[5] != 'unaccepted' and r not in leg_recs.values()
				and WINDOW_TOKEN not in r[9]]
			verdict = []
			if not ok_pages:
				verdict.append('PAGE_CHAIN_WRONG')
			if leg[4] != want_action:
				verdict.append('LEG_ACTION_WRONG')
			if k < k_declared and (k + 1) in leg_recs and dst != leg_recs[k + 1][5]:
				verdict.append('LEG_CHAIN_BROKEN')
			if spread or orphan_legs:
				verdict.append('STAGE_SPREAD')
			status = 'OK' if not verdict else '+'.join(sorted(set(verdict)))
			stats[status] += 1
			out_rows.append([q, k, '%s->%s' % (src, dst), '%s/%s(row%d)' % (tname, tid, k),
				'>'.join('%s(%s)' % (p, pages[q][p]) for p in chain), '%s@%s' % (leg[4], leg[3]),
				'%s@%s' % (','.join('%s(%s)' % (p, pages[q][p]) for p in got_pages),
					','.join(sorted({r[3] for r in owner_rows}))),
				';'.join('r%s:%s@%s[%s]' % (r[2], ','.join(page_of(r)) or '-', r[3], r[5])
					for r in spread + orphan_legs) or '-', status, note])
		# 领奖页：owner == reward_npc，动作 QUEST_SELECT，状态 = packed K 节点；塌缩形 = 领奖 NPC 的
		# QUEST_SELECT 行下发的却是阶段页（且该 (owner, src) 不是任何阶段的 walk 归属）。
		report_rows = [r for r in recs if r[1] == 'R' and r[3] == str(reward_id) and rp in page_of(r)
			and r[4] == 'QUEST_SELECT']
		extra_report = [r for r in recs if r[1] == 'R' and r[3] == str(reward_id) and rp in page_of(r)
			and r[4] != 'QUEST_SELECT']
		collapse_rows = [r for r in recs if r[1] == 'R' and r[3] == str(reward_id)
			and r[4] == 'QUEST_SELECT' and any(p in stage_pages for p in page_of(r))
			and (r[3], r[5]) not in legit]
		if len(report_rows) == 1 and not extra_report:
			rstatus = 'REPORT_OK'
		elif report_rows or extra_report:
			rstatus = 'REPORT_PAGE_WRONG'
		else:
			rstatus = 'REPORT_PAGE_WRONG' if collapse_rows else 'REPORT_MISSING'
		stats[rstatus] += 1
		detail = ';'.join('r%s:%s@%s[%s->%s]' % (r[2], ','.join(page_of(r)) or '-', r[3], r[5], r[6])
			for r in report_rows + extra_report + collapse_rows) or '-'
		out_rows.append([q, 0, '-', '%s/%s' % (reward_name, reward_id),
			'want=%s(%s)@btn1009' % (rp, pages[q][rp]), 'packedK=%s' % ','.join(sorted(packed_k)),
			detail, '-', rstatus, note])

	path = Path(args.out)
	with path.open('w', encoding='utf-8') as fh:
		fh.write('# P0c-55 阶段腿逐阶段重建普查（真端 talk_npc<k> × 客户端页链 × 登记表现状）\n')
		fh.write('# quest\tstage\tnode_pair\tretail_stage_npc\tclient_chain\tleg_action@owner'
			'\towner_pages\tspread_or_orphan_rows\tverdict\tnote\n')
		for r in out_rows:
			fh.write('\t'.join(str(x) for x in r) + '\n')
	print('P0C55_CENSUS rows=%d %s out=%s' % (len(out_rows), dict(sorted(stats.items())), path))
	for r in out_rows:
		if r[8] not in ('OK', 'REPORT_OK'):
			print('  %5s K=%-2s %-46s %-28s %s' % (r[0], r[1], r[4][:46], r[5][:28], r[8]))
	return 0


if __name__ == '__main__':
	sys.exit(main())
