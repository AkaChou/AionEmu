#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-6：SimpleHunt 真端驱动行的"击杀目标刷怪可达性"普查，用来给 14112/14123 的 KEEP_XML 裁定定性。

问题：真端家族表没有刷怪列。如果某行的击杀目标/交付 NPC 在本服 `spawns/**` 里没有静态 spot，
删掉 XML 会不会让任务不可完成？本普查把"已驱动行"分成三类：

  * NO_STATIC_SPAWN / XML_NO_SPAWN   —— 目标无静态刷怪，且退役 XML **也没有**刷怪边
    （AionEmu 从来没让它出现过：实例 boss 由 Java 实例处理器刷，或本就是既有缺口）
    → 退役不改变可达性，**不是**本切片的缺口；
  * NO_STATIC_SPAWN / XML_SPAWN_EDGE —— 目标无静态刷怪，退役 XML 的 after-commit 刷怪边是它进世界的
    唯一途径 → 删掉 XML 就是回归（14112 的交付 NPC、14123 的击杀目标）→ KEEP_XML；
  * STATIC_SPAWN                     —— 目标有静态 spot。

输出：p0c6-spawn-reachability-census.tsv
用法：python3 -B p0c6_spawn_reachability_census.py
"""
import os
import collections
import glob
import re
import subprocess
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
HERE = Path(__file__).resolve().parent
RETAIL_TABLE = REPO / 'src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleHunt.xml'
RETENTION = REPO / 'src/test/resources/quest/retail-xml-retention.tsv'
NPCS = REPO / 'src/main/resources/aion/data/static_data/npcs'
SPAWNS = REPO / 'src/main/resources/aion/data/static_data/spawns'
QUESTS_REL = 'src/main/resources/aion/data/static_data/quest_definition/quests'
OUT = HERE / 'p0c6-spawn-reachability-census.tsv'


def npc_index():
	"""name_desc/name（小写）→ npc_id 集合。 / Lower-cased npc names to ids."""
	index = collections.defaultdict(set)
	for path in NPCS.glob('npc_template_*.xml'):
		for match in re.finditer(r'<npc_template\b[^>]*>',
				path.read_text(encoding='utf-8', errors='ignore')):
			attrs = dict(re.findall(r'(\w[\w-]*)="([^"]*)"', match.group(0)))
			if 'npc_id' in attrs:
				index[attrs.get('name_desc', '').lower()].add(int(attrs['npc_id']))
				index[attrs.get('name', '').lower()].add(int(attrs['npc_id']))
	return index


def static_spawns():
	ids = set()
	for path in SPAWNS.rglob('*.xml'):
		ids.update(int(x) for x in re.findall(r'<spawn\s+npc_id="(\d+)"',
			path.read_text(encoding='utf-8', errors='ignore')))
	return ids


def head_xml(quest_id):
	proc = subprocess.run(['git', 'show', 'HEAD:%s/%d.xml' % (QUESTS_REL, quest_id)], cwd=REPO,
		capture_output=True, text=True)
	return proc.stdout if proc.returncode == 0 else ''


def retail_npcs():
	"""真端表 → {quest_id: {角色: [名字]}}（击杀目标 / 接取 NPC / 交付 NPC）。"""
	roles = {}
	for match in re.finditer(r'<id id="(\d+)">([\s\S]*?)</id>', RETAIL_TABLE.read_text(encoding='utf-8')):
		block = match.group(2)
		names = {'kill': [], 'acquired': [], 'reward': []}
		for slot in range(1, 9):
			found = re.search(r'<monster%d>(.*?)</monster%d>' % (slot, slot), block, re.S)
			if found:
				names['kill'].extend(part.strip() for part in found.group(1).split(',') if part.strip())
		for role, tag in (('acquired', 'acquired_npc_name'), ('reward', 'reward_npc_name')):
			found = re.search(r'<%s>(.*?)</%s>' % (tag, tag), block, re.S)
			if found and found.group(1).strip():
				names[role].append(found.group(1).strip())
		roles[int(match.group(1))] = names
	return roles


def universe():
	"""SimpleHunt 登记族：quest_id → (owner, reason)。 / Registered SimpleHunt rows with their owner/reason."""
	rows = {}
	for line in RETENTION.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		parts = line.split('\t')
		if len(parts) >= 4 and parts[2] == 'SimpleHunt':
			rows[int(parts[0])] = (parts[1], parts[3])
	return rows


def main():
	names, spawned, roles = npc_index(), static_spawns(), retail_npcs()
	census = collections.Counter()
	rows = []
	violations = []
	for quest_id, (owner, reason) in sorted(universe().items()):
		spawn_edges = re.findall(r'<spawn-npc[^>]*template-id="(\d+)"', head_xml(quest_id))
		for role, candidates in sorted(roles.get(quest_id, {}).items()):
			resolved = set()
			for name in candidates:
				resolved |= names.get(name.lower(), set())
			if not resolved:
				continue
			# 该 NPC 是否被本任务的刷怪边点名（只有刷怪边点名的角色才与"退役是否回归"相关）。
			# Only NPCs named by this quest's own spawn edges can regress on retirement.
			spawned_by_quest = resolved & {int(x) for x in spawn_edges}
			kind = ('XML_SPAWN_EDGE' if spawned_by_quest
				else 'XML_NO_SPAWN' if not spawn_edges else 'XML_OTHER_SPAWN')
			if resolved & spawned:
				census['STATIC_SPAWN/' + role] += 1
				continue
			census[owner + '/NO_STATIC_SPAWN/' + kind + '/' + role] += 1
			rows.append((quest_id, owner, role, kind, ','.join(str(npc) for npc in sorted(resolved)),
				','.join(spawn_edges) or '-', reason))
			# 不变量：本任务刷怪边是某角色进世界的唯一途径时，该行必须是 XML_RETENTION +
			# QUEST_SPAWN_UNEXPRESSED（删 XML 即回归）。
			if kind == 'XML_SPAWN_EDGE' and (owner != 'XML_RETENTION'
					or not reason.endswith('QUEST_SPAWN_UNEXPRESSED')):
				violations.append((quest_id, role, owner, reason))
	text = ('# P0c-6 SimpleHunt 任务 NPC 刷怪可达性普查（全登记族，不只已驱动行）\n'
		'# 角色：kill（击杀目标）| acquired（接取 NPC）| reward（交付 NPC）\n'
		'# kind: XML_NO_SPAWN（退役 XML 也没有刷怪边 → 退役不改变可达性） |\n'
		'#       XML_SPAWN_EDGE（本任务刷怪边是该 NPC 进世界的唯一途径 → 删 XML 即回归，必须保留 XML） |\n'
		'#       XML_OTHER_SPAWN（XML 有刷怪边但不含该角色）\n'
		'# quest_id\towner\trole\tkind\tnpc_ids\txml_spawn_templates\treason\n')
	for row in rows:
		text += '\t'.join(str(cell) for cell in row) + '\n'
	OUT.write_text(text, encoding='utf-8')
	print('普查 ->', OUT)
	for key in sorted(census):
		print('  %-52s %d' % (key, census[key]))
	print('  XML_SPAWN_EDGE 且未按 QUEST_SPAWN_UNEXPRESSED 保留：', violations)
	return 0 if not violations else 1


if __name__ == '__main__':
	raise SystemExit(main())
