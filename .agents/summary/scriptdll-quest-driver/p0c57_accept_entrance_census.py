#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-57（续片 33）**接取入口（NPC_START）角色收窄**普查 + 裁定表生成（只读取证，不写生产产物）。

轴（本片真正的根因面，比登记项更大）
--------------------------------
登记项 `XML_ONLY_ACCEPT_ROLE_SPREAD_PENDING`（34 项 / 25 任务）是**行级**读数（某个 NPC 服务接取族
动作/页）。本普查发现根因在**块级**：遗留 XML 把真端**逐步**行（`talk_npc<k>`）误读成**接取入口**，
于是**链上每个 NPC 都写了一个 `NPC_START` 块**（证据：HEAD 版 `3093.xml` 里
`<dialog type="NPC_START" npc-id="798177" .../>` 紧接注释
`<!-- retail 步骤0(zz_retail_simple_quests.xml):type="TALK" ids="798177";对话链按客户端按钮图重建。 -->`）。
编译器对**每个** `NPC_START` 块合成接取流（`acceptFlowChain` + 续页），所以每个链上 NPC 都成了
"可以接取这个任务"的人 —— 而真端 `acquired_npc_name` 与客户端 `start_npc_ids` 都只有**一个**接取人。

判据（fail-closed）
--------------
  G1 **owner 双侧唯一且相等**：真端 `acquired_npc_name` 唯一解析 == 客户端 `start_npc_ids`（全行并集）唯一；
  G2 每个被剪 NPC 是**链上 NPC**（真端 talk_npc<k>/reward ∪ 客户端 progress/end）且 ≠ owner；
  G3 被剪 NPC 的 `NPC_START` 块与 owner 的**逐字相同（去掉 npc 列）**——source/target/selection-sources/
     start-page/accept-actions 全同 ⇒ 纯重复块，剪掉不改变接取语义（owner 侧保留同一块）；
  G4 owner 自己**有** `NPC_START` 块（否则不是"多余块"而是"块缺失/替身"，另形 ⇒ 不出裁定）。
  G5 被剪 NPC 若同时服务接取族 R 行（`SELECT1_1` 续页等），随块一并剪；`QUEST_SELECT` 入口行**永不剪**。

分类：
  `ACCEPT_ENTRANCE_DUPLICATE`（可裁，进裁定表）/ `ACCEPT_ENTRANCE_VARIANT_DIFF`（块参数不同 ⇒ 变体嫌疑，暂缓）
  / `ACCEPT_ENTRANCE_OWNER_MISSING`（owner 无块 ⇒ 替身/缺失形，暂缓）。

用法 / Usage: python3 -B p0c57_accept_entrance_census.py [--registry <tsv>] [--out <tsv>] [--emit-decisions <tsv>]
"""
from __future__ import annotations

import argparse
import collections
import csv
import pathlib
import re

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parents[2]
REGISTRY = (ROOT / 'src/main/resources/aion/data/static_data/quest_retail'
	/ 'quest_client_talk_chain_steps.tsv')
RETAIL_XML = ROOT / 'src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml'
NPC_DIR = ROOT / 'src/main/resources/aion/data/static_data/npcs'
CLIENT_INDEX = ROOT / 'docs/quest/client-dialog-mapping/legacy-quest-dialog-template-index.csv'

ACCEPT_ACTIONS = {
	'ASK_QUEST_ACCEPT', 'QUEST_ACCEPT_1', 'QUEST_ACCEPT_2', 'QUEST_ACCEPT_3', 'QUEST_ACCEPT_4',
	'QUEST_ACCEPT_SIMPLE', 'QUEST_REFUSE_1', 'QUEST_REFUSE_2', 'QUEST_REFUSE_SIMPLE',
}
ACCEPT_PAGES = re.compile(r'^(SELECT1|SELECT1_\d+|SHOW_ASK_QUEST_ACCEPT_WINDOW)$')
SENTINELS = {'', '-', '0', 'NONE', '_None_', '_none_', 'None'}
HDR = ['quest_id', 'owner', 'owner_name', 'shape', 'prune_npcs', 'prune_roles', 'owner_block',
	'prune_blocks', 'accept_rows', 'note']


def load_retail():
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


def load_client():
	out = collections.defaultdict(lambda: {'start': set(), 'end': set(), 'progress': set()})
	with CLIENT_INDEX.open(encoding='utf-8-sig') as fh:
		for r in csv.DictReader(fh):
			q = int(r['quest_id'])
			for col, key in (('start_npc_ids', 'start'), ('end_npc_ids', 'end'),
					('progress_npc_ids', 'progress')):
				out[q][key] |= {int(x) for x in re.findall(r'\d+', r[col] or '')}
	return out


def load_registry(path):
	reg = collections.defaultdict(list)
	for line in path.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		reg[int(line.split('\t')[0])].append(line.split('\t'))
	return reg


def accept_rows_of(npc, recs):
	"""该 NPC 的接取族 R 行（动作 ∈ 接取动作族 或 下发页 ∈ 接取页族）。"""
	out = []
	for p in recs:
		if p[1] != 'R' or p[3] != npc:
			continue
		pages = [m.group(1) for m in re.finditer(r'PAGE:([A-Z0-9_]+)', p[9])]
		if p[4] in ACCEPT_ACTIONS or any(ACCEPT_PAGES.match(pg) for pg in pages):
			out.append(p)
	return out


def main() -> int:
	ap = argparse.ArgumentParser()
	ap.add_argument('--registry', default=str(REGISTRY))
	ap.add_argument('--out', default=str(HERE / 'p0c57-accept-entrance-census.tsv'))
	ap.add_argument('--emit-decisions', default=str(HERE / 'p0c57-accept-entrance-decisions.tsv'))
	args = ap.parse_args()

	retail, npcnames, client, reg = load_retail(), load_npc_names(), load_client(), load_registry(
		pathlib.Path(args.registry))

	def resolve(raw):
		"""真端名 → id 集（**int**：与客户端 role 列同型，避免 QE-076 的 str/int 比较恒假）。"""
		name = (raw or '').strip()
		if name in SENTINELS:
			return set()
		ids = set(npcnames.get(name, set()))
		if not ids and name.lower().startswith('npc_'):
			ids = set(npcnames.get(name[4:], set()))
		return {int(x) for x in ids if x.isdigit()}

	rows, decisions = [], []
	stats = collections.Counter()
	for q in sorted(reg):
		recs = reg[q]
		rb, cb = retail.get(q, {}), client[q]
		acq = resolve(rb.get('acquired', ''))
		chain = acq | resolve(rb.get('reward', '')) | cb['start'] | cb['progress'] | cb['end']
		for name in rb.get('talk', []):
			chain |= resolve(name)
		blocks = [p for p in recs if p[1] == 'B' and p[2] == 'NPC_START']
		if not blocks:
			continue
		if not (len(acq) == 1 and len(cb['start']) == 1 and acq == cb['start']):
			stats['DECL_UNRESOLVED'] += 1
			rows.append([q, '', rb.get('acquired', ''), 'ACCEPT_ENTRANCE_DECL_UNRESOLVED',
				','.join(p[3] for p in blocks), '-', '-', '-', '-',
				'retail_acq=%s client_start=%s' % (','.join(str(x) for x in sorted(acq)) or '-',
					','.join(str(x) for x in sorted(cb['start'])) or '-')])
			continue
		owner = str(next(iter(acq)))
		chain = {str(x) for x in chain}
		owner_name = rb.get('acquired', '')
		bmap = {p[3]: p for p in blocks}
		if owner not in bmap:
			stats['OWNER_MISSING'] += 1
			rows.append([q, owner, owner_name, 'ACCEPT_ENTRANCE_OWNER_MISSING',
				','.join(p[3] for p in blocks), '-', '-', '-', '-',
				'owner 无 NPC_START 块（替身/缺失形）'])
			continue
		odd = [p for p in blocks if p[3] != owner]
		if not odd:
			continue
		sig = lambda p: (p[4], p[5], p[6])
		same = all(sig(p) == sig(bmap[owner]) for p in odd)
		roles = ','.join('CHAIN' if p[3] in chain else 'UNDECLARED' for p in odd)
		arows = sorted({str(p[2]) for p in odd for p in accept_rows_of(p[3], recs)})
		if not same:
			stats['VARIANT_DIFF'] += 1
			rows.append([q, owner, owner_name, 'ACCEPT_ENTRANCE_VARIANT_DIFF',
				','.join(p[3] for p in odd), roles, bmap[owner][6],
				'|'.join('%s:%s' % (p[3], p[6]) for p in odd), ','.join(arows) or '-',
				'块参数与 owner 不同 ⇒ 变体嫌疑，暂缓'])
			continue
		if any(p[3] not in chain for p in odd):
			stats['NPC_UNDECLARED'] += 1
			rows.append([q, owner, owner_name, 'ACCEPT_ENTRANCE_NPC_UNDECLARED',
				','.join(p[3] for p in odd), roles, bmap[owner][6], '-', ','.join(arows) or '-',
				'被剪 NPC 不在任何声明集 ⇒ 身份轴未定，暂缓'])
			continue
		stats['DUPLICATE'] += 1
		rows.append([q, owner, owner_name, 'ACCEPT_ENTRANCE_DUPLICATE',
			','.join(p[3] for p in odd), roles, bmap[owner][6],
			'|'.join('%s:%s' % (p[3], p[6]) for p in odd), ','.join(arows) or '-',
			'G1..G4 全过 ⇒ 进裁定表'])
		for p in odd:
			decisions.append([q, 'LEGACY_ACCEPT_ENTRANCE_SPREAD', p[3], owner, owner_name,
				'CHAIN', 'NPC_START', ','.join(str(x[2]) for x in accept_rows_of(p[3], recs)) or '-',
				p[6]])

	pathlib.Path(args.out).write_text('\n'.join(['\t'.join(HDR)] +
		['\t'.join(str(x) for x in r) for r in rows]) + '\n', encoding='utf-8')
	hdr = ['# P0c-57 接取入口角色收窄裁定（code=LEGACY_ACCEPT_ENTRANCE_SPREAD）',
		'# 判据（生成器逐轴 fail-closed 复算）：',
		'#   G1 owner 双侧唯一且相等（真端 acquired_npc_name == 客户端 start_npc_ids）；',
		'#   G2 被剪 NPC 是链上 NPC（talk_npc<k>/reward/progress/end）且 ≠ owner；',
		'#   G3 被剪块的 source/target/extra（selection-sources|start-page|accept-actions）与 owner 块逐字相同；',
		'#   G4 owner 自己有 NPC_START 块（同一块保留 ⇒ 接取流不丢）；块随行的 ACT 行（接取族）一并剪，',
		'#      `QUEST_SELECT` 入口行永不剪。',
		'# ' + '\t'.join(['quest_id', 'code', 'prune_npc', 'owner_npc', 'owner_name', 'role',
			'drop_blocks', 'drop_rows', 'block_extra'])]
	pathlib.Path(args.emit_decisions).write_text('\n'.join(hdr + ['\t'.join(str(x) for x in d)
		for d in decisions]) + '\n', encoding='utf-8')
	print('ACCEPT_ENTRANCE_CENSUS %s | decisions=%d quests=%d' % (dict(sorted(stats.items())),
		len(decisions), len({d[0] for d in decisions})))
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
