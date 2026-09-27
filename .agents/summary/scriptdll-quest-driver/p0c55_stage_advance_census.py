#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-55 阶段**推进行/页归属**普查：多阶段任务的每阶段（页链 + 推进 NPC）与真端/客户端对拍（只读）。

P0c-54 的普查按「SETPRO<k> 行 NPC == 真端 talk_npc<k>」硬轴剔除了 9 个任务（1323/1394/1484/2480/
2538/3093/4052/11010/11103）。本普查逐阶段展开它们的**真实形状**：
  * 真端轴：`talk_npc1..K`（K = 真端声明数）与 `acquired`/`reward` 的解析；
  * 客户端轴：每阶段页链 `SELECT{k+1} →（按钮）→ … → 带 HACTION_SETPRO{k} 的末页`（页名 + 推进动作号）；
  * 客户端任务书行数（QE-051 判据）与 `progress_npc_ids` 序（有则在册）；
  * 登记表现状：该阶段的 `SETPRO{k}` 推进行在哪个 NPC、下发哪些页、状态对（src→dst）。

输出 TSV（一行 = 一个「任务×阶段」）：
  quest stage node_pair retail_npc legacy_advance_npc client_chain client_advance_ok legacy_pages verdict
verdict ∈ {OK, PAGE_WRONG, ADVANCE_NPC_WRONG, BOTH_WRONG, STAGE_ROW_MISSING, CLIENT_CHAIN_INCOMPLETE}。

用法 / Usage: python3 -B p0c55_stage_advance_census.py [--out <tsv>]
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
CLIENT_CSV = REPO / 'docs/quest/client-dialog-mapping/quest-dialog-pages.csv'
CLIENT_ACTIONS = REPO / 'docs/quest/client-dialog-mapping/quest-dialog-action-details.csv'
CLIENT_INDEX = REPO / 'docs/quest/client-dialog-mapping/legacy-quest-dialog-template-index.csv'
NPCS = HERE / 'npc_name_index.tsv'

QUESTS = (1323, 1394, 1484, 2480, 2538, 3093, 4052, 11010, 11103)


def main() -> int:
	ap = argparse.ArgumentParser()
	ap.add_argument('--out', default=str(HERE / 'p0c55-stage-advance-census.tsv'))
	args = ap.parse_args()

	body = {int(m.group(1)): m.group(2) for m in re.finditer(r'<id id="(\d+)">(.*?)</id>',
		RETAIL_XML.read_text(encoding='utf-8'), re.S)}
	npc_ids = collections.defaultdict(set)
	for line in NPCS.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		p = line.split('\t')
		for i in p[1:]:
			if i.isdigit():
				npc_ids[p[0]].add(int(i))

	def resolve(name):
		ids = npc_ids.get((name or '').strip(), set())
		return next(iter(ids)) if len(ids) == 1 else None

	pages = collections.defaultdict(set)
	page_names = collections.defaultdict(dict)
	with CLIENT_CSV.open(encoding='utf-8-sig') as fh:
		for row in csv.DictReader(fh):
			q = int(row['quest_id'])
			pages[q].add(row['html_page_name'].upper())
			if row['page_id'].isdigit():
				page_names[q][int(row['page_id'])] = row['html_page_name'].upper()
	actions = collections.defaultdict(lambda: collections.defaultdict(set))
	with CLIENT_ACTIONS.open(encoding='utf-8-sig', newline='') as fh:
		for row in csv.DictReader(fh):
			if (row['source_variant'] == 'active' and row['page_mapping'] == 'exact'
					and row['action_id'].isdigit()):
				actions[int(row['quest_id'])][row['html_page_name'].lower()].add(int(row['action_id']))
	summary = {}
	progress = {}
	with CLIENT_INDEX.open(encoding='utf-8-sig') as fh:
		for row in csv.DictReader(fh):
			q = int(row['quest_id'])
			if q in QUESTS:
				summary.setdefault(q, 0)
				summary[q] += 1
				ids = re.findall(r'\d+', row['progress_npc_ids'] or '')
				if ids:
					progress.setdefault(q, [])
					for i in ids:
						if int(i) not in progress[q]:
							progress[q].append(int(i))

	rows = collections.defaultdict(list)
	for line in REGISTRY.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		p = line.split('\t')
		rows[int(p[0])].append(p)

	def chain(q, k):
		entry = 'SELECT%d' % (k + 1)
		if entry not in pages.get(q, set()):
			return [], None
		advance, out, cur, seen = 10000 + k - 1, [], entry, set()
		while True:
			acts = actions.get(q, {}).get(cur.lower(), set())
			if advance in acts:
				return out + [cur], advance
			cont = [(a, page_names[q][a]) for a in acts
				if a in page_names.get(q, {}) and str(page_names[q][a]).startswith('SELECT')]
			if len(cont) != 1:
				return [], None
			out.append(cur)
			cur = cont[0][1]
			if cur in seen:
				return [], None
			seen.add(cur)

	out_rows = []
	for q in QUESTS:
		b = body.get(q, '')
		talks = []
		for k in range(1, 10):
			m = re.search(r'<talk_npc%d>([^<]*)</talk_npc%d>' % (k, k), b)
			if m:
				talks.append((k, m.group(1).strip(), resolve(m.group(1).strip())))
		acq = re.search(r'<acquired_npc_name>([^<]*)</', b)
		rew = re.search(r'<reward_npc_name>([^<]*)</', b)
		for k, name, tid in talks:
			ch, adv_id = chain(q, k)
			stage_rows = [p for p in rows[q] if p[1] == 'R' and re.fullmatch(r'SETPRO%d' % k, p[4])]
			adv_npcs = sorted({p[3] for p in stage_rows})
			legacy_pages = sorted({tok.split(':')[-1] for p in rows[q] if p[1] == 'R'
				for tok in p[9].split(';') if tok.startswith('DIALOG:SHOW_QUEST_PAGE:')
				and tok.split(':')[-1].startswith('SELECT%d' % (k + 1))})
			if not ch:
				verdict = 'CLIENT_CHAIN_INCOMPLETE'
			elif not stage_rows:
				verdict = 'STAGE_ROW_MISSING'
			else:
				page_ok = ('DIALOG:SHOW_QUEST_PAGE:' + ch[0]) in \
					[p[9] for p in stage_rows if p[5] == (str(int(re.match(r'stage(\d+)', p[2]).group(1)) if
						re.match(r'stage(\d+)', p[2]) else -1))] if False else \
					any(('DIALOG:SHOW_QUEST_PAGE:' + ch[0]) in p[9].split(';') for p in rows[q]
						if p[1] == 'R' and p[4] == 'QUEST_SELECT')
				npc_ok = str(tid) == (adv_npcs[0] if len(adv_npcs) == 1 else None)
				if not npc_ok and not page_ok:
					verdict = 'BOTH_WRONG'
				elif not npc_ok:
					verdict = 'ADVANCE_NPC_WRONG'
				elif not page_ok:
					verdict = 'PAGE_WRONG'
				else:
					verdict = 'OK'
			out_rows.append((q, k, '%s->%s' % (stage_rows[0][5], stage_rows[0][6]) if stage_rows else '-',
				'%s(%s)' % (name, tid), '/'.join(adv_npcs) or '-', '>'.join(ch) or '-',
				str(adv_id) if adv_id else '-', ','.join(legacy_pages) or '-', verdict))
		out_rows.append((q, 0, '-', 'acquired=%s reward=%s' % (resolve(acq.group(1)) if acq else '-',
			resolve(rew.group(1)) if rew else '-'), '-', '-', '-',
			'summary_rows=%s progress=%s' % (summary.get(q, 0), progress.get(q, [])), 'INFO'))

	path = Path(args.out)
	with path.open('w', encoding='utf-8') as fh:
		fh.write('# P0c-55 阶段推进行/页归属普查（9 个 P0c-54 硬轴剔除任务）\n')
		fh.write('# quest\tstage\tnode_pair\tretail_stage_npc\tlegacy_advance_npc\tclient_chain\t'
			'client_advance\tlegacy_stage_pages\tverdict\n')
		for r in out_rows:
			fh.write('\t'.join(str(x) for x in r) + '\n')
	stats = collections.Counter(r[8] for r in out_rows)
	print('P0C55_CENSUS rows=%d %s out=%s' % (len(out_rows), dict(sorted(stats.items())), path))
	for r in out_rows:
		if r[8] != 'INFO':
			print('  %5s K=%s node=%s retail=%s legacy_adv=%s chain=%s adv=%s pages=%s => %s'
				% (r[0], r[1], r[2], r[3], r[4], r[5], r[6], r[7][:40], r[8]))
	return 0


if __name__ == '__main__':
	sys.exit(main())
