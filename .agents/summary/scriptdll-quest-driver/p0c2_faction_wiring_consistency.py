#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-2 复核：已退役 SimpleTalk `_faction_` 行的阵营接线四层一致性。

①真端 quest.xml `<npcfaction_name>` → ②编译器 `NPC_FACTIONS` 映射（从源码正则抽取，不复制映射）
→ ③生产 `npc_factions_quest.xml` 的 `faction_id`（必须与②逐行相等）→ ④`npc_factions.xml` 含该 id
（`NpcFactions` 才会遍历到）。任一层不一致即打印差异并以退出码 1 结束。

用法：python3 -B p0c2_faction_wiring_consistency.py
"""
import io
import re
import sys
from pathlib import Path

REPO = Path('/Users/mc/IdeaProjects/AionEmu-test')
RETAIL = REPO / 'src/main/resources/aion/data/static_data/quest_retail'
RETENTION = RETAIL / 'retail-xml-retention.tsv'
TALK_TABLE = RETAIL / 'Quest_SimpleTalk.xml'
METADATA_COMPILER = (REPO / 'src/main/java/com/aionemu/gameserver/questEngine/retail'
                     / 'RetailQuestMetadataCompiler.java')
WEEKDAY = REPO / 'src/main/resources/aion/data/static_data/npc_factions/npc_factions_quest.xml'
FACTIONS = REPO / 'src/main/resources/aion/data/static_data/npc_factions/npc_factions.xml'


def retired_simple_talk_faction_ids():
    """已退役（owner=RETAIL_TABLE）且真端表接取名为 _faction_ 的 SimpleTalk 行。"""
    talk = TALK_TABLE.read_text(encoding='utf-8', errors='replace')
    sentinels = {int(m.group(1)) for m in re.finditer(r'<id\s+id="(\d+)"[^>]*>(.*?)</id>', talk, re.S)
                 if '_faction_' in m.group(2)}
    retired = set()
    for line in RETENTION.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        if len(parts) >= 3 and parts[1] == 'RETAIL_TABLE' and parts[2] == 'SimpleTalk':
            retired.add(int(parts[0]))
    return sorted(sentinels & retired)


def main():
    ids = retired_simple_talk_faction_ids()
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
        weekday[int(re.search(r'quest_id="(\d+)"', tag).group(1))] = int(
            re.search(r'faction_id="(\d+)"', tag).group(1))

    have = {int(x) for x in re.findall(r'<npc_faction[^>]*\bid="(\d+)"', FACTIONS.read_text(encoding='utf-8'))}

    problems = []
    for quest_id in ids:
        name = names.get(quest_id)
        meta = mapping.get(name or '', 0)
        week = weekday.get(quest_id)
        if meta == 0:
            problems.append(f'{quest_id}: 真端名 {name!r} 未落到 NPC_FACTIONS')
        elif week is None:
            problems.append(f'{quest_id}: 生产星期位表无条目')
        elif week != meta:
            problems.append(f'{quest_id}: 星期位表 faction_id={week} != 编译器 {meta}')
        elif meta not in have:
            problems.append(f'{quest_id}: npc_factions.xml 缺 id={meta}')
    print(f'已退役 _faction_ 行={len(ids)} 问题={len(problems)}')
    for problem in problems:
        print(' ', problem)
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main())
