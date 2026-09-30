#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-3 普查：真端 Quest_SimpleHunt.xml 的类别哨兵行能否被系统发放形状承接。

判据（每行都要成立才可迁移）：
  1. 行在生产宇宙内（catalog ∪ 保留清单 owner=RETAIL_TABLE）；
  2. 报告 NPC 名唯一可解析，或是可由客户端 dic 链登记的复合势力引用；
  3. 击杀槽位的怪名全部可解析（合成器硬条件）；
  4. 计数不超过真端 6 位打包上限（63）；
  5. 阵营键：真端 npcfactions_quest.xml 星期位 + 生产 npc_factions_quest.xml 星期位（全 0 = 真端不发放）。

输出：p0c3-simple-hunt-sentinel-census.tsv
"""
import io
import os
import re
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
RETAIL = REPO / 'src/main/resources/aion/data/static_data/quest_retail'
NPC_DIR = REPO / 'src/main/resources/aion/data/static_data/npcs'
CLIENT_REWARD_NPCS = RETAIL / 'quest_client_reward_npcs.tsv'
RETAIL_SERVER = Path(f"{REPO.parent / '58Server'}/Map/XML")
OUT = Path(__file__).resolve().parent / 'p0c3-simple-hunt-sentinel-census.tsv'
MAX_COUNTERS = 5


def read(path, encodings=('utf-8', 'utf-16')):
    for encoding in encodings:
        try:
            return Path(path).read_text(encoding=encoding, errors='replace')
        except UnicodeError:
            continue
    raise RuntimeError(f'unreadable: {path}')


def text_of(block, tag):
    m = re.search(r'<%s>(.*?)</%s>' % (tag, tag), block, re.S)
    if not m:
        return None
    value = m.group(1).strip()
    return value or None


def parse_hunt():
    s = read(RETAIL / 'Quest_SimpleHunt.xml')
    rows = {}
    for m in re.finditer(r'<id\s+id="(\d+)"[^>]*>(.*?)</id>', s, re.S):
        rows[int(m.group(1))] = m.group(2)
    return rows


def npc_name_index():
    idx = {}
    for name in sorted(os.listdir(NPC_DIR)):
        if not name.startswith('npc_template_') or not name.endswith('.xml'):
            continue
        s = read(Path(NPC_DIR) / name)
        for m in re.finditer(r'<npc_template\b[^>]*>', s):
            tag = m.group(0)
            ids = re.search(r'npc_id="(\d+)"', tag)
            desc = re.search(r'name_desc="([^"]*)"', tag)
            if not ids or not desc:
                continue
            key = desc.group(1).strip().lower()
            idx.setdefault(key, set()).add(int(ids.group(1)))
            if key.startswith('npc_'):
                idx.setdefault(key[4:], set()).add(int(ids.group(1)))
    return idx


def resolve(index, name):
    if not name:
        return set()
    raw = name.strip()
    if raw.isdigit():
        return {int(raw)}
    return index.get(raw.lower(), set())


def universe():
    catalog = read(REPO / 'src/main/resources/aion/data/static_data/quest/definitions/quest_definition_catalog.xml')
    ids = {int(x) for x in re.findall(r'<definition id="(\d+)"', catalog)}
    for line in read(RETAIL / 'retail-xml-retention.tsv').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        if len(parts) >= 2 and parts[1] == 'RETAIL_TABLE':
            ids.add(int(parts[0]))
    return ids


def weekday_masks(path, tag='npc_faction_quest'):
    s = read(path)
    out = {}
    for m in re.finditer(r'<%s\b[^>]*/?>' % tag, s):
        t = m.group(0)
        qid = re.search(r'quest_id="(\d+)"', t)
        if not qid:
            continue
        mask = ''
        for day in ('mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'):
            found = re.search(r'%s="(\d)"' % day, t)
            mask += found.group(1) if found else '?'
        out[int(qid.group(1))] = mask
    return out


def retail_weekday_masks():
    """真端 npcfactions_quest.xml：<quest_id quest_id="N"> 块内的 mon..sun 与 npcfaction_name。"""
    s = (RETAIL_SERVER / 'npcfactions_quest.xml').read_text(encoding='utf-16', errors='replace')
    out = {}
    for m in re.finditer(r'<quest_id\s+quest_id="(\d+)"[^>]*>(.*?)</quest_id>', s, re.S):
        block = m.group(2)
        mask = ''
        for day in ('mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'):
            found = text_of(block, day)
            mask += found if found else '?'
        faction = text_of(block, 'npcfaction_name')
        out[int(m.group(1))] = (mask, faction)
    return out


def npc_factions_map():
    s = read(REPO / 'src/main/java/com/aionemu/gameserver/questEngine/retail/RetailQuestMetadataCompiler.java')
    block = re.search(r'NPC_FACTIONS = Map\.ofEntries\((.*?)\);', s, re.S).group(1)
    return {k: int(v) for k, v in re.findall(r'Map\.entry\("([^"]+)",\s*(\d+)\)', block)}


def client_reward_npcs():
    out = {}
    for line in read(CLIENT_REWARD_NPCS).splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        out[int(parts[0])] = parts[1]
    return out


def sentinel_of(name):
    if not name:
        return None
    raw = name.strip()
    if len(raw) > 2 and raw.startswith('_') and raw.endswith('_'):
        return raw[1:-1].lower()
    return None


def main():
    rows = parse_hunt()
    index = npc_name_index()
    world = universe()
    masks_prod = weekday_masks(REPO / 'src/main/resources/aion/data/static_data/npc_factions/npc_factions_quest.xml')
    masks_retail = retail_weekday_masks()
    registry = client_reward_npcs()

    out = []
    for quest_id, block in sorted(rows.items()):
        acquired = text_of(block, 'acquired_npc_name')
        sentinel = sentinel_of(acquired)
        if sentinel is None:
            continue
        reward = text_of(block, 'reward_npc_name')
        reward_ids = resolve(index, reward)
        counters = []
        counters_ok = True
        for slot in range(1, MAX_COUNTERS + 1):
            monsters = text_of(block, 'monster%d' % slot)
            count = text_of(block, 'count%d' % slot)
            if monsters is None and count is None:
                continue
            names = [x.strip() for x in re.split(r'[,\s]+', monsters or '') if x.strip()]
            resolved = set()
            for name in names:
                resolved |= resolve(index, name)
            if int((count or '0').strip()) > 63:
                counters_ok = False
            if not resolved:
                counters_ok = False
            counters.append('%s:%s' % (count, ','.join(names)))
        out.append({
            'quest_id': quest_id,
            'sentinel': sentinel,
            'in_universe': '1' if quest_id in world else '0',
            'reward_name': reward or '',
            'reward_ids': ','.join(str(x) for x in sorted(reward_ids)) or '-',
            'client_reward_ids': registry.get(quest_id, '-'),
            'counters': ' | '.join(counters) or '-',
            'counters_ok': '1' if counters_ok else '0',
            'mask_retail': (masks_retail.get(quest_id) or ('-', '-'))[0],
            'faction_retail': (masks_retail.get(quest_id) or ('-', '-'))[1],
            'mask_prod': masks_prod.get(quest_id, '-'),
        })

    cols = ['quest_id', 'sentinel', 'in_universe', 'reward_name', 'reward_ids', 'client_reward_ids',
            'counters', 'counters_ok', 'mask_retail', 'faction_retail', 'mask_prod']
    with OUT.open('w', encoding='utf-8') as fh:
        fh.write('# P0c-3 SimpleHunt 类别哨兵普查（生成脚本 p0c3_simple_hunt_sentinel_census.py）\n')
        fh.write('\t'.join(cols) + '\n')
        for row in out:
            fh.write('\t'.join(str(row[c]) for c in cols) + '\n')

    print('SimpleHunt 哨兵行总数:', len(out))
    for sentinel in sorted({r['sentinel'] for r in out}):
        subset = [r for r in out if r['sentinel'] == sentinel]
        inner = [r for r in subset if r['in_universe'] == '1']
        print('\n== %s: %d 行（宇宙内 %d）==' % (sentinel, len(subset), len(inner)))
        print('  报告名唯一可解:', sum(1 for r in subset if r['reward_ids'] != '-' and ',' not in r['reward_ids']))
        print('  报告名复合引用(未解):', sum(1 for r in subset if r['reward_ids'] == '-'))
        print('  客户端登记表可解:', sum(1 for r in subset if r['client_reward_ids'] != '-'))
        print('  击杀槽位全可解:', sum(1 for r in subset if r['counters_ok'] == '1'))
        print('  真端星期位: 全0=%d / 非全0=%d / 未登记=%d' % (
            sum(1 for r in subset if set(r['mask_retail']) == {'0'}),
            sum(1 for r in subset if r['mask_retail'] != '-' and set(r['mask_retail']) != {'0'}),
            sum(1 for r in subset if r['mask_retail'] == '-')))
        print('  生产星期位: 全0=%d / 非全0=%d / 未登记=%d' % (
            sum(1 for r in subset if set(r['mask_prod']) == {'0'}),
            sum(1 for r in subset if r['mask_prod'] != '-' and set(r['mask_prod']) != {'0'}),
            sum(1 for r in subset if r['mask_prod'] == '-')))
        print('  [宇宙内] 可承接(报告可解 + 槽位OK):', sum(
            1 for r in inner if r['counters_ok'] == '1'
            and (r['reward_ids'] != '-' and ',' not in r['reward_ids'] or r['client_reward_ids'] != '-')))
    print('\n输出:', OUT)
    return 0


if __name__ == '__main__':
    sys.exit(main())
