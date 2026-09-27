#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-2/P0c-3 复核（并集）：已退役类别哨兵行（SimpleTalk + SimpleHunt）的阵营接线四层一致性。

①真端 quest.xml `<npcfaction_name>` → ②编译器 `NPC_FACTIONS` 映射（源码正则抽取，不复制映射）
→ ③生产 `npc_factions_quest.xml` 的 `faction_id`（必须与②逐行相等，且星期位与真端一致）
→ ④`npc_factions.xml` 含该 id（`NpcFactions` 才会遍历到）。

用法：python3 -B p0c3_faction_wiring_consistency.py
"""
import io
import re
import sys
from pathlib import Path

REPO = Path('/Users/mc/IdeaProjects/AionEmu-test')
RETAIL = REPO / 'src/main/resources/aion/data/static_data/quest_retail'
RETENTION = RETAIL / 'retail-xml-retention.tsv'
TABLES = {'SimpleTalk': RETAIL / 'Quest_SimpleTalk.xml', 'SimpleHunt': RETAIL / 'Quest_SimpleHunt.xml'}
METADATA_COMPILER = (REPO / 'src/main/java/com/aionemu/gameserver/questEngine/retail'
                     / 'RetailQuestMetadataCompiler.java')
WEEKDAY = REPO / 'src/main/resources/aion/data/static_data/npc_factions/npc_factions_quest.xml'
FACTIONS = REPO / 'src/main/resources/aion/data/static_data/npc_factions/npc_factions.xml'
RETAIL_SERVER = Path('/Users/mc/IdeaProjects/58Server/Map/XML/npcfactions_quest.xml')
DAYS = ('mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun')


def retired_rows():
    """保留清单 owner=RETAIL_TABLE 的 (family, quest_id)。"""
    out = []
    for line in RETENTION.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        if len(parts) >= 3 and parts[1] == 'RETAIL_TABLE' and parts[2] in TABLES:
            out.append((parts[2], int(parts[0])))
    return out


def sentinel_ids(path, token):
    text = path.read_text(encoding='utf-8', errors='replace')
    return {int(m.group(1)) for m in re.finditer(r'<id\s+id="(\d+)"[^>]*>(.*?)</id>', text, re.S)
            if token in m.group(2)}


def retail_masks():
    text = RETAIL_SERVER.read_text(encoding='utf-16', errors='replace')
    out = {}
    for m in re.finditer(r'<quest_id\s+quest_id="(\d+)"[^>]*>(.*?)</quest_id>', text, re.S):
        block = m.group(2)
        mask = ''
        for day in DAYS:
            found = re.search(r'<%s>(.*?)</%s>' % (day, day), block, re.S)
            mask += found.group(1).strip() if found else '?'
        out[int(m.group(1))] = mask
    return out


def main():
    source = METADATA_COMPILER.read_text(encoding='utf-8')
    block = re.search(r'NPC_FACTIONS = Map\.ofEntries\((.*?)\);', source, re.S).group(1)
    mapping = {k: int(v) for k, v in re.findall(r'Map\.entry\("([^"]+)",\s*(\d+)\)', block)}

    retail = (RETAIL / 'quest.xml').read_text(encoding='utf-8', errors='replace')
    names = {}
    for m in re.finditer(r'<quest(?:\s[^>]*)?>\s*(.*?)</quest>', retail, re.S):
        body = m.group(1)
        qid = re.search(r'<id>(\d+)</id>', body)
        faction = re.search(r'<npcfaction_name>(.*?)</npcfaction_name>', body)
        if qid:
            names[int(qid.group(1))] = faction.group(1).strip() if faction else None

    weekday = {}
    for m in re.finditer(r'<npc_faction_quest\b[^>]*/>', WEEKDAY.read_text(encoding='utf-8')):
        tag = m.group(0)
        weekday[int(re.search(r'quest_id="(\d+)"', tag).group(1))] = (
            int(re.search(r'faction_id="(\d+)"', tag).group(1)),
            ''.join(re.search(r'%s="(\d)"' % day, tag).group(1) for day in DAYS))

    have = {int(x) for x in re.findall(r'<npc_faction[^>]*\bid="(\d+)"', FACTIONS.read_text(encoding='utf-8'))}
    masks = retail_masks()

    problems = []
    checked = 0
    for family, quest_id in retired_rows():
        path = TABLES[family]
        if quest_id not in sentinel_ids(path, '_faction_'):
            continue
        checked += 1
        name = names.get(quest_id)
        meta = mapping.get(name or '', 0)
        entry = weekday.get(quest_id)
        if meta == 0:
            problems.append(f'{family} {quest_id}: 真端名 {name!r} 未落到 NPC_FACTIONS')
        elif entry is None:
            problems.append(f'{family} {quest_id}: 生产星期位表无条目')
        elif entry[0] != meta:
            problems.append(f'{family} {quest_id}: 星期位表 faction_id={entry[0]} != 编译器 {meta}')
        elif meta not in have:
            problems.append(f'{family} {quest_id}: npc_factions.xml 缺 id={meta}')
        else:
            retail_mask = masks.get(quest_id)
            if retail_mask is not None and '?' not in retail_mask and entry[1] != retail_mask:
                problems.append(f'{family} {quest_id}: 生产星期位 {entry[1]} != 真端 {retail_mask}')
    print(f'已退役 _faction_ 行={checked} 问题={len(problems)}')
    for problem in problems:
        print(' ', problem)
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main())
