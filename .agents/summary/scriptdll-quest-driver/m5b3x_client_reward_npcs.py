#!/usr/bin/env python3
"""P1a / P0c-2：客户端任务书 dic 链 → 复合势力奖励引用（SimpleCollectItem + SimpleTalk）的交付 NPC 集登记表。

证据链（逐任务）：客户端 `QUEST_Q<id>.html` 引用 `[%dic:STR_DIC_E_<token>]`（按 id 或按名）→
串表 `client_strings_dic_*.xml` 的 `STR_DIC_E_<token>` 正文点名 `[%dic:STR_DIC_N_<显示名>]` →
`npc_template` 的 `name_desc` 唯一匹配 → npc id 集合。
（共享 dic 是常态：35022 复用 35021 的交付文本、36015 复用 36500——按"本族共享集合"归并。）

产出：
- `src/main/resources/aion/data/static_data/quest_retail/quest_client_reward_npcs.tsv`
  （quest_id / npc_ids / source_dic / npc_descs；只登记 NPC 集非空的行）；
- 证据副本 `.agents/summary/scriptdll-quest-driver/m5b3x-client-reward-npcs-evidence.tsv`。

族来源 = SimpleCollectItem 漂移登记全体 ∪ SimpleTalk 的 `_faction_` 哨兵行（P0c-2 起）。
用法：python3 -B m5b3x_client_reward_npcs.py
"""
import os
import re
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
UNPAK = Path(f"{REPO.parent / 'PycharmProjects' / 'unpak'}/data_unpacked")
TOPIC = REPO / '.agents/summary/scriptdll-quest-driver'
DRIFT = REPO / 'src/test/resources/quest/retail-simple-collect-item-drift.tsv'
SIMPLE_TALK = REPO / 'src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml'
SIMPLE_HUNT = REPO / 'src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleHunt.xml'
METADATA_COMPILER = REPO / 'src/main/java/com/aionemu/gameserver/questEngine/retail/RetailQuestMetadataCompiler.java'
OUT = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_reward_npcs.tsv'
EVIDENCE = TOPIC / 'm5b3x-client-reward-npcs-evidence.tsv'


def faction_names():
	"""从 RetailQuestMetadataCompiler 抽 NPC_FACTIONS 映射（真相只有一份，避免复制漂移）。"""
	text = METADATA_COMPILER.read_text(encoding='utf-8', errors='replace')
	block = re.search(r'NPC_FACTIONS = Map\.ofEntries\((.*?)\);', text, re.S).group(1)
	return set(re.findall(r'Map\.entry\("([^"]+)",\s*\d+\)', block))


def is_faction_composite(name, factions):
	"""<地图>_<势力名> 复合引用（_LD 双侧变体按 _L/_D 识别），与编译器同口径。"""
	if not name:
		return False
	split = name.find('_')
	if split <= 0 or split == len(name) - 1:
		return False
	suffix = name[split + 1:]
	if suffix in factions:
		return True
	if not suffix.endswith('_LD'):
		return False
	base = suffix[:-len('_LD')]
	return (base + '_L') in factions or (base + '_D') in factions


def family_ids():
	"""登记族：SimpleCollectItem 全族 ∪ SimpleTalk/SimpleHunt 的复合势力奖励引用行（真端表 reward_npc_name）。"""
	ids = {int(line.split('\t')[0]) for line in DRIFT.read_text(encoding='utf-8').splitlines()
		if line and not line.startswith('#')}
	factions = faction_names()
	for table in (SIMPLE_TALK, SIMPLE_HUNT):
		text = table.read_text(encoding='utf-8', errors='replace')
		for m in re.finditer(r'<id\s+id="(\d+)"[^>]*>(.*?)</id>', text, re.S):
			reward = re.search(r'<reward_npc_name>(.*?)</reward_npc_name>', m.group(2), re.S)
			if reward and is_faction_composite(reward.group(1).strip(), factions):
				ids.add(int(m.group(1)))
	return sorted(ids)


def main():
	family = family_ids()
	estr = {}
	for f in (UNPAK / 'Strings').glob('*.xml'):
		text = f.read_text(encoding='utf-8', errors='replace')
		for m in re.finditer(r'<name>STR_DIC_E_([^<]+)</name>\s*<body>([^<]*)</body>', text):
			estr[m.group(1)] = m.group(2)
	descs = {}
	for f in sorted((REPO / 'src/main/resources/aion/data/static_data/npcs').glob('npc_template_*.xml')):
		for m in re.finditer(r'<npc_template\s+([^>]*?)/?>', f.read_text(encoding='utf-8')):
			attrs = dict(re.findall(r'([\w]+)="([^"]*)"', m.group(1)))
			nid = attrs.get('npc_id')
			if nid and attrs.get('name_desc') and nid not in descs:
				descs[attrs['name_desc']] = nid

	rows = ['# 客户端交付 NPC 集登记（SimpleCollectItem / SimpleTalk / SimpleHunt 复合势力奖励引用；'
		+ '由 m5b3x_client_reward_npcs.py 生成）',
		'# quest_id\tnpc_ids\tsource_dic\tnpc_descs']
	misses = []
	for quest_id in family:
		hits = list((UNPAK / 'Dialogs').rglob(f'quest_q{quest_id}.html'))
		if not hits:
			misses.append(f'{quest_id}:no-html')
			continue
		text = hits[0].read_text(encoding='utf-8', errors='replace')
		tokens = re.findall(r'\[%dic:STR_DIC_E_([^\]]+)\]', text)
		id_set, dic_hits, desc_hits = {}, [], []
		for token in tokens:
			body = estr.get(token)
			if body is None:
				continue
			dic_hits.append(token)
			for name in re.findall(r'STR_DIC_N_([^\]]+)\]', body):
				nid = descs.get(name)
				if nid:
					id_set[nid] = name
					desc_hits.append(name)
		if id_set:
			ordered = sorted(id_set, key=int)
			rows.append(f'{quest_id}\t{",".join(ordered)}\t{",".join(sorted(set(dic_hits)))}\t'
				f'{",".join(id_set[n] for n in ordered)}')
		else:
			misses.append(f'{quest_id}:unresolved')
	OUT.write_text('\n'.join(rows) + '\n', encoding='utf-8')
	EVIDENCE.write_text('\n'.join(rows) + '\n', encoding='utf-8')
	print(f'family={len(family)} registered={len(rows) - 2} unresolved={len(misses)}')
	print('unresolved:', misses)
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
