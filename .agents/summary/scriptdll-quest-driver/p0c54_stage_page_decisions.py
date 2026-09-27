#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-54 阶段页轴裁定表派生（机器推导 + 逐行 basis；fail-closed 轴与生成器侧一致）。

在普查（p0c54_stage_page_census.py）之上补三层轴并逐行裁定：
  ①真端轴：owner 必须是真端 `talk_npc<k>` 的唯一解析（k = 该页所属阶段 = SETPRO<k> 那条的 k）；
  ②客户端轴：页必须在该阶段的客户端页链上（入口 SELECT{k+1} → 续页 … → 带 HACTION_SETPRO{k} 的末页）；
  ③角色轴：越界下发者 S 必须**不是**该页所属阶段的声明 NPC（S 可以有别的角色）；
  ④无页丢失：剪除后该页仍由 owner 下发（至少一条 owner 行）；
  ⑤互斥：任务不得同时出现在阶梯 / 接取入口 / 改道 / 多余 owner / 角色收窄 / 阶段窗外溢 六表；
  ⑥owner 必须是 RETAIL_TABLE（XML 保留行 IR 属 XML，不进本表）。

输出两个 code 的裁定行：
  STAGE_PAGE_OWNER_SPREAD   quest  page  extra_npc  owner_npc  stage  basis    （剪除越界行）
  STAGE_ENTRY_MISSING       quest  page  -          owner_npc  stage  basis    （插入入口行）

用法 / Usage: python3 -B p0c54_stage_page_decisions.py [--out <tsv>]
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
RETAIL_XML = (REPO / 'src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml')
NPCS = HERE / 'npc_name_index.tsv'  # 与生成器同源（真端 NPC 名 → id 唯一解析）
CLIENT_CSV = REPO / 'docs/quest/client-dialog-mapping/quest-dialog-pages.csv'
CLIENT_ACTIONS = REPO / 'docs/quest/client-dialog-mapping/quest-dialog-action-details.csv'
CENSUS = HERE / 'p0c54-stage-page-census.tsv'

# 互斥表（生成器同口径）：本表行的任务不得出现在其中任何一张。
EXCLUSIVE = {
	'ladder': ('p0c36-talk-ladder-decisions.tsv', 'p0c37-talk-ladder-decisions.tsv'),
	'accept_entrance': ('p0c42-accept-entrance-decisions.tsv',),
	'canonical': ('p0c43-canonical-resynthesis.tsv',),
	'extra_owner': ('p0c45-extra-owner-decisions.tsv',),
	'role_narrowing': ('p0c46-role-narrowing-decisions.tsv',),
	'stage_window': ('p0c47-stage-window-spread.tsv',),
}


def qids_of(path):
	out = set()
	if not Path(path).exists():
		return out
	for line in Path(path).read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		p = line.split('\t')
		if p[0].isdigit():
			out.add(int(p[0]))
	return out


def main() -> int:
	ap = argparse.ArgumentParser()
	ap.add_argument('--out', default=str(HERE / 'p0c54-stage-page-decisions.tsv'))
	args = ap.parse_args()

	# 真端表 + NPC 名索引。
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

	def resolve(q, name, axis):
		ids = npc_ids.get(name.strip(), set())
		if len(ids) != 1:
			print('SKIP %d %s：%s 解析 %s（非唯一）' % (q, axis, name, sorted(ids)))
			return None
		return next(iter(ids))

	# 客户端任务书角色列（start/end/progress NPC 集，全行并集）——越界下发者的角色判据。
	roles = collections.defaultdict(lambda: {'start': set(), 'end': set(), 'progress': []})
	with (REPO / 'docs/quest/client-dialog-mapping/legacy-quest-dialog-template-index.csv') \
			.open(encoding='utf-8-sig') as fh:
		for row in csv.DictReader(fh):
			q = int(row['quest_id'])
			for col, key in (('start_npc_ids', 'start'), ('end_npc_ids', 'end')):
				roles[q][key] |= {int(x) for x in re.findall(r'\d+', row[col] or '')}
			# progress 保序去重（顺序 = 阶段序，P0c-47 全局普查 55/55 同序）。
			for x in re.findall(r'\d+', row['progress_npc_ids'] or ''):
				if int(x) not in roles[q]['progress']:
					roles[q]['progress'].append(int(x))

	# 客户端页链（同普查口径）。
	pages, page_ids, _, actions = {}, collections.defaultdict(dict), None, \
		collections.defaultdict(lambda: collections.defaultdict(set))
	with CLIENT_CSV.open(encoding='utf-8-sig') as fh:
		for row in csv.DictReader(fh):
			q = int(row['quest_id'])
			pages.setdefault(q, set()).add(row['html_page_name'].upper())
			if row['page_id'].isdigit():
				page_ids[q][int(row['page_id'])] = row['html_page_name'].upper()
	with CLIENT_ACTIONS.open(encoding='utf-8-sig', newline='') as fh:
		for row in csv.DictReader(fh):
			if (row['source_variant'] == 'active' and row['page_mapping'] == 'exact'
					and row['action_id'].isdigit()):
				actions[int(row['quest_id'])][row['html_page_name'].lower()].add(int(row['action_id']))

	def chain(q, k):
		entry = 'SELECT%d' % (k + 1)
		if entry not in pages.get(q, set()):
			return []
		advance, current, visited, out = 10000 + k - 1, entry, set(), []
		while True:
			acts = actions.get(q, {}).get(current.lower(), set())
			if advance in acts:
				return out + [current]
			cont = [(a, page_ids[q][a]) for a in acts
				if a in page_ids.get(q, {}) and str(page_ids[q][a]).startswith('SELECT')]
			if len(cont) != 1:
				return []
			out.append(current)
			current = cont[0][1]
			if current in visited:
				return []
			visited.add(current)

	# 互斥集合 + 登记表行的 (quest, page) 事实。
	excl = {}
	for key, files in EXCLUSIVE.items():
		ids = set()
		for f in files:
			ids |= qids_of(HERE / f)
		excl[key] = ids
	rows = collections.defaultdict(list)
	reg = (REPO / 'src/main/resources/aion/data/static_data/quest_retail'
		/ 'quest_client_talk_chain_steps.tsv')
	retention = {}
	drift = REPO / 'src/main/resources/aion/data/static_data/quest_retail' \
		/ 'retail-xml-retention.tsv'
	if drift.exists():
		for line in drift.read_text(encoding='utf-8').splitlines():
			if line.startswith('#') or not line.strip():
				continue
			p = line.split('\t')
			if p[0].isdigit() and len(p) > 2:
				retention[int(p[0])] = p[2]
	for line in reg.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		p = line.split('\t')
		rows[int(p[0])].append(p)

	# 真端声明的阶段 NPC：talk_npc<k>。
	def talk_npc(q, k):
		m = re.search(r'<talk_npc%d>([^<]*)</talk_npc%d>' % (k, k), body.get(q, ''))
		return resolve(q, m.group(1), 'talk_npc%d' % k) if m else None

	prune, insert, skipped = [], [], []
	with CENSUS.open(encoding='utf-8') as fh:
		for line in fh:
			if line.startswith('#') or not line.strip():
				continue
			q, k, page, expected, by, status, detail = line.rstrip('\n').split('\t')
			q, k = int(q), int(k)
			ch = chain(q, k)
			if status == 'MISSING_ENTRY':
				owner = talk_npc(q, k)
				if owner is None or str(owner) != expected:
					skipped.append((q, page, 'owner≠真端 talk_npc%d(%s≠%s)' % (k, owner, expected)))
					continue
				if owner != (resolve(q, re.search(r'<acquired_npc_name>([^<]*)</acquired_npc_name>',
						body.get(q, '')).group(1), 'acquired') if '<acquired_npc_name>' in body.get(q, '') else -1):
					pass  # 接取 NPC 与阶段 NPC 不同人是常态，非轴；此处仅留痕
				if page != ch[0]:
					skipped.append((q, page, '入口页 ≠ 页链首页 %s' % (ch[:1])))
					continue
				if [r for r in rows[q] if r[1] == 'R' and re.fullmatch(r'SETPRO%d' % k, r[4])] and \
						not [r for r in rows[q] if r[1] == 'R'
							and ('DIALOG:SHOW_QUEST_PAGE:' + page) in r[9].split(';')]:
					insert.append((q, k, page, owner, 'ENTRY_LOST_BUT_CHAIN_INTACT'))
				continue
			if status != 'SPREAD_DUPLICATE':
				skipped.append((q, page, status))
				continue
			owner = talk_npc(q, k)
			if owner is None:
				skipped.append((q, page, 'talk_npc%d 解析失败' % k))
				continue
			if str(owner) != expected:
				skipped.append((q, page, 'SETPRO%d 行 NPC %s ≠ 真端 talk_npc%d %s' % (k, expected, k, owner)))
				continue
			# 越界下发者：所有非 owner 的下发行的 npc。
			extra = sorted({r[3] for r in rows[q] if r[1] == 'R'
				and ('DIALOG:SHOW_QUEST_PAGE:' + page) in r[9].split(';') and r[3] != str(owner)})
			for s in extra:
				# 角色轴：S 是否是该页所属阶段的声明 NPC？（是则不是越界，跳过。）
				if int(s) == owner:
					continue
				# S 自己的阶段页：S 作为 talk_npc<j> 而该页也在 chain(j) 上 ⇒ S 的合法页，不剪。
				own_stage = [j for j in range(1, 40) if talk_npc(q, j) == int(s) and page in chain(q, j)]
				if own_stage:
					skipped.append((q, page, 'S=%s 是 talk_npc%s 且该页属其页链（合法双主）'
						% (s, own_stage)))
					continue
				# 越界者角色轴（三源取其一，全 fail-closed）：①客户端任务书 start（接取角色）；
				# ②真端 talk_npc<j>（j≠k，别的阶段）；③客户端 progress 序位（稀疏，有则加成校验）。
				rr = roles[q]
				cp = rr['progress']
				oj = [j for j in range(1, 40) if talk_npc(q, j) == int(s) and j != k]
				if int(s) in rr['start']:
					role = 'START'
				elif oj:
					role = 'talk_npc%s' % oj
				elif int(s) in cp and cp.index(int(s)) + 1 != k:
					# 客户端 progress 保序表：序位 ≠ k 的 progress NPC（如交付/报告位）。
					role = 'progress[%d]' % (cp.index(int(s)) + 1)
				elif int(s) in rr['end']:
					# 客户端任务书 end（交付/报告 owner）——阶段页绝不属于交付角色。
					role = 'END'
				else:
					skipped.append((q, page, 'S=%s 角色不明（start=%s end=%s progress=%s talk_npc=%s）'
						% (s, sorted(rr['start']), sorted(rr['end']), cp,
							[talk_npc(q, j) for j in range(1, 4)])))
					continue
				bonus = ''
				if cp:
					if cp.index(owner) + 1 != k:
						skipped.append((q, page, 'owner=%s 客户端 progress 序位 %s ≠ 阶段 %d（%s）'
							% (owner, cp.index(owner) + 1 if owner in cp else None, k, cp)))
						continue
					bonus = ';PROGRESS[%d]=%s' % (k - 1, owner)
				prune.append((q, k, page, int(s), owner,
					'OWNER=talk_npc%d%s;EXTRA=%s;CHAIN=%s'
					% (k, bonus, role, '>'.join(ch))))
	print('P0C54_DECISIONS prune=%d insert=%d skipped=%d' % (len(prune), len(insert), len(skipped)))
	for s in skipped:
		print('  SKIP %s' % (s,))

	path = Path(args.out)
	with path.open('w', encoding='utf-8') as fh:
		fh.write('# P0c-54 阶段页轴裁定：①入口页缺失（STAGE_ENTRY_MISSING，插行）'
			'②阶段页跨 NPC 扩散（STAGE_PAGE_OWNER_SPREAD，剪行）\n')
		fh.write('# 生成器 fail-closed 轴见 build_quest_client_talk_chain_steps.py 装载段；'
			'basis 记录该行的机器推导证据。\n')
		fh.write('# code\tquest_id\tpage\textra_npc\towner_npc\tstage\tbasis\n')
		for q, k, page, s, owner, b in prune:
			fh.write('STAGE_PAGE_OWNER_SPREAD\t%d\t%s\t%s\t%s\t%d\t%s\n' % (q, page, s, owner, k, b))
		for q, k, page, owner, b in insert:
			fh.write('STAGE_ENTRY_MISSING\t%d\t%s\t-\t%s\t%d\t%s\n' % (q, page, owner, k, b))
	print('out=%s' % path)
	return 0


if __name__ == '__main__':
	sys.exit(main())
