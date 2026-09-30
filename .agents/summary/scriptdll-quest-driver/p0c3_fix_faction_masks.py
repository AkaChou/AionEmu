#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-2/P0c-3：把类别哨兵行的生产星期位按真端 npcfactions_quest.xml 对齐。

判例（M5-b3x / P0c-3）：真端星期位全 0 = 该阵营日常"永不发放"；生产表若写成全 1，会让轮换发放
真端下线的内容。本脚本只修正"真端有明确 0/1 值、且生产与真端不一致"的行（真端未登记的行保持不动），
覆盖范围 = SimpleHunt 哨兵行（普查口径）+ 已退役 SimpleTalk 哨兵行（保留清单口径）。
用法：python3 -B p0c3_fix_faction_masks.py [--dry-run]
"""
import os
import argparse
import re
from pathlib import Path

REPO = Path(f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}")
RETAIL_DIR = REPO / 'src/main/resources/aion/data/static_data/quest_retail'
TALK_TABLE = RETAIL_DIR / 'Quest_SimpleTalk.xml'
RETENTION = RETAIL_DIR / 'retail-xml-retention.tsv'
RETAIL_SERVER = Path(f"{os.environ.get('AION_RETAIL_ROOT', os.path.expanduser('~/IdeaProjects/58Server'))}/Map/XML/npcfactions_quest.xml")
PROD = REPO / 'src/main/resources/aion/data/static_data/npc_factions/npc_factions_quest.xml'
CENSUS = Path(__file__).resolve().parent / 'p0c3-simple-hunt-sentinel-census.tsv'
DAYS = ('mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun')


def retail_masks():
    """真端 npcfactions_quest.xml → {quest_id: 星期位}。"""
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


def production_masks():
    """生产 npc_factions_quest.xml → {quest_id: 星期位}。"""
    text = PROD.read_text(encoding='utf-8')
    out = {}
    for m in re.finditer(r'<npc_faction_quest\b[^>]*/>', text):
        tag = m.group(0)
        quest_id = int(re.search(r'quest_id="(\d+)"', tag).group(1))
        out[quest_id] = ''.join(re.search(r'%s="(\d)"' % day, tag).group(1) for day in DAYS)
    return out


def targets():
    """真端有值、生产不一致的哨兵行 → {quest_id: retail_mask}（SimpleHunt 普查 ∪ SimpleTalk 已退役）。"""
    masks = retail_masks()
    prod = production_masks()
    out = {}
    for line in CENSUS.read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#') or line.startswith('quest_id'):
            continue
        parts = line.split('\t')
        quest_id, sentinel, in_universe = int(parts[0]), parts[1], parts[2]
        if sentinel != 'faction' or in_universe != '1':
            continue
        mask = masks.get(quest_id, '-')
        if '?' in mask or '-' in mask:
            continue
        if prod.get(quest_id) != mask:
            out[quest_id] = mask
    for family, path in (('SimpleTalk', TALK_TABLE),):
        retired = retired_ids(family)
        text = path.read_text(encoding='utf-8', errors='replace')
        sentinels = {int(m.group(1)) for m in re.finditer(r'<id\s+id="(\d+)"[^>]*>(.*?)</id>', text, re.S)
                     if '_faction_' in m.group(2)}
        for quest_id in sorted(retired & sentinels):
            mask = masks.get(quest_id, '-')
            if '?' in mask or '-' in mask:
                continue
            if prod.get(quest_id) != mask:
                out[quest_id] = mask
    return out


def retired_ids(family):
    ids = set()
    for line in RETENTION.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        if len(parts) >= 3 and parts[1] == 'RETAIL_TABLE' and parts[2] == family:
            ids.add(int(parts[0]))
    return ids


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()
    fixes = targets()
    text = PROD.read_text(encoding='utf-8')
    changed = []
    for quest_id, mask in sorted(fixes.items()):
        pattern = re.compile(r'(<npc_faction_quest quest_id="%d"[^>]*)/>' % quest_id)
        match = pattern.search(text)
        if not match:
            print('未找到生产行:', quest_id)
            continue
        tag = match.group(0)
        new_tag = tag
        for index, day in enumerate(DAYS):
            new_tag = re.sub(r'%s="\d"' % day, '%s="%s"' % (day, mask[index]), new_tag)
        if new_tag != tag:
            changed.append(quest_id)
            text = text.replace(tag, new_tag, 1)
    if not args.dry_run:
        PROD.write_text(text, encoding='utf-8')
    print('目标行=%d 实际修改=%d' % (len(fixes), len(changed)))
    print('修改:', changed)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
