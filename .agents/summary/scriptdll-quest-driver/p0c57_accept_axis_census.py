#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-57（续片 33）接取轴普查：`XML_ONLY_ACCEPT_ROLE_SPREAD_PENDING`（34 项 / 25 任务）**复算 + 分形 + 判据**

轴的两部分（登记于 `p0c42-blocked-rows.tsv` 第 32 行，读数为 P0c-46/47 角色轴普查的 `INFO_ACCEPT_SPREAD`）：
  ①**页形**：阶段/链上 NPC 在 `unaccepted` 状态下发**接取窗续页** `SELECT1_1`（18 项 / 11 任务）；
  ②**动作形**：非接取 NPC 服务接取动作族 `ASK_QUEST_ACCEPT`/`QUEST_ACCEPT_{1,2,3,4,SIMPLE}`/
    `QUEST_REFUSE_{1,2,SIMPLE}`（16 项 / 14 任务）。

复算口径（与 P0c-46 同源，是"信息轴"的原判据）：
    对每个 (任务, NPC)，`served[ACCEPT]`（该 NPC 服务的接取动作 ∪ 接取页）非空 **且**
    `npc ∉ (真端 acquired_npc_name 解析集 ∪ 客户端 start_npc_ids)` ⇒ 记一项。
本脚本在此之上做**分形**（信息轴 → 可裁面）：

  * `SPREAD_OWNER_RESOLVED`：接取 owner **双侧唯一且相等**（真端 `acquired_npc_name` 唯一解析、客户端
    `start_npc_ids` 唯一，且两者相同）且被服务的 NPC 是**链上 NPC**（真端 talk_npc<k> ∪ reward ∪
    客户端 start∪progress ∪ end）⇒ 可裁（进裁定表，剪其接取族行）。
  * `SPREAD_OWNER_UNRESOLVED`：owner 无法唯一解析（真端哨兵 `_faction_`/`_challengetask_`/空、或客户端
    `start_npc_ids` 空、或两侧都有但不等）⇒ **fail-closed 暂缓**（登记原因，不剪）。
  * `SPREAD_NPC_UNDECLARED`：被服务的 NPC 自己不在任何声明集里（`role=UNDECLARED`）⇒ 暂缓（先定身份轴）。

守卫（逐条 fail-closed，任一不成立即不出裁定行）：
  G1 owner 双侧唯一且相等（身份轴，判例 QE-073：审计是自洽性检查、不是身份检查）；
  G2 被剪 NPC 是链上 NPC 且**另有角色**（talk_npc<k>/reward/进度/end），剪掉接取行不改变其对话入口；
  G3 覆盖守卫：被剪 NPC 的接取投影（动作 ∪ 页）⊆ owner 的接取投影（剪了不丢件）；
  G4 owner 侧**有**该任务接取族行（被剪项在 owner 上仍有出处分发）；
  G5 被剪行**不含 `QUEST_SELECT` 入口行**（P0c-54 判例：入口行删了该 NPC 对话整条消失）；
  G6 被剪行 `source == unaccepted`（只在接取相位，不碰阶段/交付相位）。

用法 / Usage:
  python3 -B p0c57_accept_axis_census.py [--registry <tsv>] [--out <tsv>] [--emit-decisions <tsv>]
"""
from __future__ import annotations

import argparse
import collections
import csv
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[3]
RETAIL_XML = ROOT / 'src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml'
NPC_DIR = ROOT / 'src/main/resources/aion/data/static_data/npcs'
INDEX_CSV = ROOT / 'docs/quest/client-dialog-mapping/legacy-quest-dialog-template-index.csv'
DEFAULT_REG = (ROOT / 'src/main/resources/aion/data/static_data/quest_retail'
	/ 'quest_client_talk_chain_steps.tsv')
REGISTERED = pathlib.Path(__file__).resolve().parent / 'p0c47-role-axis-census-post.tsv'

ACCEPT_ACTIONS = {
	'ASK_QUEST_ACCEPT', 'QUEST_ACCEPT_1', 'QUEST_ACCEPT_2', 'QUEST_ACCEPT_3', 'QUEST_ACCEPT_4',
	'QUEST_ACCEPT_SIMPLE', 'QUEST_REFUSE_1', 'QUEST_REFUSE_2', 'QUEST_REFUSE_SIMPLE',
}
ACCEPT_PAGES = re.compile(r'^(SELECT1|SELECT1_\d+|SHOW_ASK_QUEST_ACCEPT_WINDOW)$')
SENTINELS = {'', '-', '0', 'NONE', '_None_', '_none_', 'None'}
HDR = ['quest_id', 'npc_id', 'shape', 'role', 'owner_client', 'owner_retail', 'owner', 'served',
	'rows', 'guards', 'note']


def load_retail_names():
	out = {}
	for m in re.finditer(r'<id id="(\d+)">(.*?)</id>', RETAIL_XML.read_text(encoding='utf-8'), re.S):
		qid, body = int(m.group(1)), m.group(2)

		def g(tag):
			mm = re.search(r'<%s>([^<]*)</%s>' % (tag, tag), body)
			return mm.group(1).strip() if mm else ''
		out[qid] = {'acquired': g('acquired_npc_name'), 'reward': g('reward_npc_name'),
			'talk': [g('talk_npc%d' % k) for k in range(1, 64) if g('talk_npc%d' % k)]}
	return out


def load_npc_names():
	idx = collections.defaultdict(set)
	for fl in sorted(NPC_DIR.glob('npc_template_*.xml')):
		for m in re.finditer(r'<npc_template\b[^>]*>', fl.read_text(encoding='utf-8', errors='ignore')):
			t = m.group()
			n = re.search(r'name_desc="([^"]*)"', t)
			i = re.search(r'\bnpc_id="(\d+)"', t)
			if not (n and i):
				continue
			idx[n.group(1)].add(i.group(1))
			if n.group(1).lower().startswith('npc_'):
				idx[n.group(1)[4:]].add(i.group(1))
	return idx


def load_client_index():
	out = collections.defaultdict(lambda: {'start': set(), 'end': set(), 'progress': set()})
	with INDEX_CSV.open(encoding='utf-8-sig') as fh:
		for r in csv.DictReader(fh):
			q = int(r['quest_id'])
			out[q]['start'] |= set(re.findall(r'\d+', r['start_npc_ids'] or ''))
			out[q]['end'] |= set(re.findall(r'\d+', r['end_npc_ids'] or ''))
			out[q]['progress'] |= set(re.findall(r'\d+', r['progress_npc_ids'] or ''))
	return out


def load_registry(path):
	reg = collections.defaultdict(list)
	for line in path.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		reg[int(line.split('\t')[0])].append(line.split('\t'))
	return reg


def accept_items_of_row(p):
	"""该行服务的接取族项：`act:<动作>`（动作列）∪ `page:<页>`（actions/page_check 列）。"""
	items = set()
	if len(p) > 4 and p[4] in ACCEPT_ACTIONS:
		items.add('act:' + p[4])
	for cell in (p[9] if len(p) > 9 else '', p[11] if len(p) > 11 else ''):
		for mm in re.finditer(r'PAGE:([A-Z0-9_]+)', cell):
			if ACCEPT_PAGES.match(mm.group(1)):
				items.add('page:' + mm.group(1))
		if cell and '=' in cell:
			for part in cell.split(';'):
				if '=' in part and ACCEPT_PAGES.match(part.split('=')[0].strip()):
					items.add('page:' + part.split('=')[0].strip())
	return items


def load_registered():
	pairs = set()
	for line in REGISTERED.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		p = line.split('\t')
		if p[2] == 'INFO_ACCEPT_SPREAD':
			pairs.add((int(p[0]), int(p[1])))
	return pairs


def census(registry_path):
	retail, npcnames = load_retail_names(), load_npc_names()
	client, reg = load_client_index(), load_registry(registry_path)
	rows = []

	def resolve(raw):
		name = (raw or '').strip()
		if name in SENTINELS:
			return set(), 'EMPTY'
		ids = set(npcnames.get(name, set()))
		if not ids and name.lower().startswith('npc_'):
			ids = set(npcnames.get(name[4:], set()))
		return (ids, 'OK') if ids else (set(), 'UNRESOLVED')

	for q in sorted(reg):
		rb, cb = retail.get(q, {}), client.get(q, {'start': set(), 'end': set(), 'progress': set()})
		acq, acq_s = resolve(rb.get('acquired', ''))
		rew, rew_s = resolve(rb.get('reward', ''))
		talk = set()
		for t in rb.get('talk', []):
			talk |= resolve(t)[0]
		served = collections.defaultdict(set)
		rowmap = collections.defaultdict(list)
		entry_rows = collections.defaultdict(list)
		for p in reg[q]:
			if p[1] != 'R':
				continue
			items = accept_items_of_row(p)
			if not items:
				continue
			served[p[3]] |= items
			rowmap[p[3]].append(p)
			if p[4] == 'QUEST_SELECT':
				entry_rows[p[3]].append(p)
		decl = acq | cb['start']
		chain_decl = acq | talk | cb['start'] | cb['progress'] | rew | cb['end']
		for npc in sorted(served):
			if npc in decl:
				continue
			owner = ''
			shape = ''
			guards = []
			# owner 双侧唯一且相等（G1）
			if len(acq) == 1 and len(cb['start']) == 1 and acq == cb['start']:
				owner = sorted(acq)[0]
			if not owner:
				shape = 'SPREAD_OWNER_UNRESOLVED'
				note = 'retail_acq=%s/%s client_start=%s' % (rb.get('acquired', ''),
					acq_s, ','.join(sorted(cb['start'])) or '-')
				rows.append([q, npc, shape, 'CHAIN' if npc in chain_decl else 'UNDECLARED', '', '',
					owner, '|'.join(sorted(served[npc])), '-', '-', note])
				continue
			if npc not in chain_decl:
				shape = 'SPREAD_NPC_UNDECLARED'
				rows.append([q, npc, shape, 'UNDECLARED', ','.join(sorted(cb['start'])), '',
					owner, '|'.join(sorted(served[npc])), '-', '-', 'npc 不在任何声明集'])
				continue
			# G3 覆盖：被剪投影 ⊆ owner 的接取投影
			owner_items = set()
			for p in reg[q]:
				if p[1] == 'R' and p[3] == owner:
					owner_items |= accept_items_of_row(p)
			g3 = served[npc] <= owner_items
			g4 = bool(owner_items)
			g5 = not entry_rows[npc]
			g6 = all(p[5] == 'unaccepted' for p in rowmap[npc])
			guards = ['G2=CHAIN', 'G3=%s' % ('OK' if g3 else 'FAIL:extra=%s'
				% ','.join(sorted(served[npc] - owner_items))), 'G4=%s' % ('OK' if g4 else 'FAIL'),
				'G5=%s' % ('OK' if g5 else 'FAIL:entry=%s' % len(entry_rows[npc])),
				'G6=%s' % ('OK' if g6 else 'FAIL:src=%s' % ','.join(sorted({p[5] for p in rowmap[npc]})))]
			shape = ('SPREAD_PRUNE_CANDIDATE' if (g3 and g4 and g5 and g6)
				else 'SPREAD_GUARD_FAIL')
			rows.append([q, npc, shape, 'CHAIN', ','.join(sorted(cb['start'])), rb.get('acquired', ''),
				owner, '|'.join(sorted(served[npc])),
				'|'.join('%s:%s:%s>%s:%s' % (p[2], p[4], p[5], p[6], p[9]) for p in rowmap[npc]),
				';'.join(guards), 'retail=%s client=%s' % (acq_s, 'OK' if cb['start'] else 'EMPTY')])
	return rows


def main() -> int:
	ap = argparse.ArgumentParser()
	ap.add_argument('--registry', default=str(DEFAULT_REG))
	ap.add_argument('--out', default=str(pathlib.Path(__file__).resolve().parent / 'p0c57-accept-axis-census.tsv'))
	ap.add_argument('--emit-decisions', default='')
	args = ap.parse_args()

	rows = census(pathlib.Path(args.registry))
	pairs = {(int(r[0]), int(r[1])) for r in rows}
	registered = load_registered()
	print('RECOMPUTED pairs=%d quests=%d | REGISTERED pairs=%d quests=%d' % (
		len(pairs), len({q for q, _n in pairs}), len(registered), len({q for q, _n in registered})))
	print('DIFF only-recomputed=%s | only-registered=%s' % (sorted(pairs - registered),
		sorted(registered - pairs)))
	if pairs != registered:
		print('FAIL-CLOSED: 复算集与登记集不一致（先解释差值再动手）')
	stats = collections.Counter(r[2] for r in rows)
	print('SHAPES %s' % dict(sorted(stats.items())))

	lines = ['# P0c-57（续片 33）接取轴普查：复算登记 34 项 + 分形（可裁面 / 暂缓面）: registry=%s' % args.registry,
		'\t'.join(HDR)]
	lines += ['\t'.join(str(x) for x in r) for r in rows]
	pathlib.Path(args.out).write_text('\n'.join(lines) + '\n', encoding='utf-8')
	if args.emit_decisions:
		dec = ['# P0c-57 接取轴裁定（code=LEGACY_ACCEPT_ROLE_SPREAD）',
			'# 判据（生成器逐轴 fail-closed 复算）：G1 owner 双侧唯一且相等 / G2 被剪 NPC 是链上 NPC /',
			'#   G3 接取投影 ⊆ owner / G4 owner 侧有接取族行 / G5 被剪行不含 QUEST_SELECT 入口行 /',
			'#   G6 被剪行 source == unaccepted（只在接取相位）。',
			'# ' + '\t'.join(['quest_id', 'code', 'prune_npc', 'owner_npc', 'owner_name', 'served'])]
		for r in rows:
			if r[2] == 'SPREAD_PRUNE_CANDIDATE':
				dec.append('\t'.join([str(r[0]), 'LEGACY_ACCEPT_ROLE_SPREAD', str(r[1]),
					str(r[6]), '', r[7]]))
		pathlib.Path(args.emit_decisions).write_text('\n'.join(dec) + '\n', encoding='utf-8')
		print('DECISIONS_WRITTEN %s' % args.emit_decisions)
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
