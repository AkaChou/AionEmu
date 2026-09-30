#!/usr/bin/env python3
"""M5-b3x：SimpleCollectItem 类别哨兵行（_faction_）= 真端阵营发放系统对账与生产星期位修正。

证据合成（逐行）：
- 真端家族表 Quest_SimpleCollectItem.xml 的 `_faction_` 行（acquired 哨兵 + 奖励引用）；
- 真端 Map/XML/npcfactions_quest.xml 的 (阵营名, 星期位)——发放语义的唯一权威；
- 生产 npc_factions/npc_factions_quest.xml 的现行星期位（早前生成器把 12 行禁用行写成全 1）；
- 客户端接取页探针（ask_quest_accept-only = 无 NPC 接取）与 DLL 事件普查（无生命周期三元组）；
- 本族生产宇宙 = 漂移登记 retail-simple-collect-item-drift.tsv 的行集合。

`--fix-production`：把生产文件中「真端全 0」行改成全 0（=真端永不发放，阵营随机池按 isActiveOn 排除），
不碰真端每天发放的行；修正前后星期位写进证据 TSV。

用法：python3 -B m5b3x_faction_sentinel_grants.py [--fix-production]
"""
import os
import argparse
import re
from pathlib import Path

REPO = Path(f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}")
TOPIC = REPO / '.agents/summary/scriptdll-quest-driver'
RETAIL_TABLE = REPO / 'src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleCollectItem.xml'
RETAIL_FACTION_QUESTS = Path(f"{os.environ.get('AION_RETAIL_ROOT', os.path.expanduser('~/IdeaProjects/58Server'))}/Map/XML/npcfactions_quest.xml")
PROD_FACTION_QUESTS = REPO / 'src/main/resources/aion/data/static_data/npc_factions/npc_factions_quest.xml'
DRIFT = REPO / 'src/test/resources/quest/retail-simple-collect-item-drift.tsv'
CLIENT_PAGE = TOPIC / 'm5b2b-client-accept-page-SimpleCollectItem.tsv'
CENSUS = TOPIC / 'm5b2b-quest-event-census.tsv'
EVIDENCE = TOPIC / 'm5b3x-collect-sentinel-grants.tsv'
DAYS = ('mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun')


def retail_rows():
	text = RETAIL_TABLE.read_text(encoding='utf-8')
	out = {}
	for rid, body in re.findall(r'<id id="(\d+)">(.*?)</id>', text, re.S):
		if '<acquired_npc_name>_faction_</acquired_npc_name>' not in body:
			continue
		reward = re.search(r'<reward_npc_name>([^<]*)</reward_npc_name>', body)
		out[int(rid)] = reward.group(1) if reward else ''
	return out


def weekday_bits(text, quest_id, element, attrs=False):
	match = re.search(rf'<{element}\s+quest_id="{quest_id}"([^>]*?)(?:/>|</{element}>)', text) if attrs \
		else re.search(rf'<{element}\s+quest_id="{quest_id}">(.*?)</{element}>', text, re.S)
	if not match:
		return None
	body = match.group(1)
	if attrs:
		return ''.join('1' if re.search(rf'{day}="1"', body) else '0' for day in DAYS)
	return ''.join('1' if f'<{day}>1</{day}>' in body else '0' for day in DAYS)


def faction_name(text, quest_id):
	match = re.search(rf'<quest_id quest_id="{quest_id}">.*?<npcfaction_name>([^<]+)</npcfaction_name>', text, re.S)
	return match.group(1) if match else '-'


def main():
	parser = argparse.ArgumentParser()
	parser.add_argument('--fix-production', action='store_true')
	args = parser.parse_args()

	retail = retail_rows()
	retail_text = RETAIL_FACTION_QUESTS.read_text(encoding='utf-16')
	prod_text = PROD_FACTION_QUESTS.read_text(encoding='utf-8')
	universe = {int(line.split('\t')[0]) for line in DRIFT.read_text(encoding='utf-8').splitlines()
		if line and not line.startswith('#')}
	client = {}
	for line in CLIENT_PAGE.read_text(encoding='utf-8').splitlines()[1:]:
		parts = line.split('\t')
		client[int(parts[0])] = (parts[2], parts[3])
	triplets = {}
	for line in CENSUS.read_text(encoding='utf-8').splitlines()[1:]:
		parts = line.split('\t')
		if parts[3] in ('0x1c', '0x1d', '0x26'):
			triplets[int(parts[0])] = True

	rows = ['# M5-b3x 哨兵行阵营发放对账（quest_id / 奖励引用 / 真端阵营 / 真端星期位 / 生产星期位 /'
		' 生产宇宙 / 客户端接取页 / DLL三元组 / 裁定 / 修正）']
	fixes = []
	for quest_id in sorted(retail):
		name = faction_name(retail_text, quest_id)
		retail_bits = weekday_bits(retail_text, quest_id, 'quest_id')
		prod_bits = weekday_bits(prod_text, quest_id, 'npc_faction_quest', attrs=True)
		in_universe = quest_id in universe
		style = client.get(quest_id, ('?', '?'))[1]
		triplet = 'yes' if triplets.get(quest_id) else 'no'
		if retail_bits == '1111111':
			verdict = 'GRANT_DAILY'
		elif retail_bits == '0000000':
			verdict = 'RETAIL_DISABLED'
		else:
			verdict = 'GRANT_PARTIAL'
		fixed = '-'
		if args.fix_production and verdict == 'RETAIL_DISABLED' and prod_bits != '0000000' and in_universe:
			pattern = re.compile(rf'(<npc_faction_quest quest_id="{quest_id}"[^>]*?/>)')
			match = pattern.search(prod_text)
			if match:
				faction = re.search(r'faction_id="(\d+)"', match.group(1)).group(1)
				replacement = (f'<npc_faction_quest quest_id="{quest_id}" faction_id="{faction}" '
					+ ' '.join(f'{day}="0"' for day in DAYS) + '/>')
				prod_text = prod_text[:match.start()] + replacement + prod_text[match.end():]
				fixed = f'{prod_bits}->0000000'
				fixes.append(quest_id)
		rows.append(f'{quest_id}\t{retail[quest_id]}\t{name}\t{retail_bits}\t{prod_bits or "-"}\t'
			f'{"yes" if in_universe else "no"}\t{style}\t{triplet}\t{verdict}\t{fixed}')

	EVIDENCE.write_text('\n'.join(rows) + '\n', encoding='utf-8')
	if args.fix_production and fixes:
		PROD_FACTION_QUESTS.write_text(prod_text, encoding='utf-8')
	counts = {}
	for line in rows[1:]:
		verdict = line.split('\t')[8]
		counts[verdict] = counts.get(verdict, 0) + 1
	print(f'rows={len(rows) - 1} verdicts={counts} production_fixed={len(fixes)}')
	if fixes:
		print('fixed ids:', fixes)
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
