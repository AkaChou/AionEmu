#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P8 §10.3-#25 收口：生产阵营轮换表镜像同步 + 阵营归属契约快照 v2（三列含掩码）。

背景（P8 取证）：生产 npc_factions_quest.xml 是真端 npcfactions_quest.xml 的旧快照——
缺 11 行（35027-35030/45027-45030/36514/36517/37006，真端全 0 掩码），且 39713/49713
（SimpleItemPlay 行，p0c3_fix_faction_masks.py 普查范围外）被本地写成全 1 而真端为全 0。
按 P0c-3 判例（真端全 0 = 该日常永不发放，生产写全 1 等于发放真端下线内容）补齐镜像。

契约快照 v2（quest-npc-faction-retail-contract.tsv）：
- 人口 = 旧 253 行 ∪ 34 条新可达行（真端轮换 ∧ 势力名可解析 ∧ 家族车道路由）= 287 行；
- 每行三列：quest_id、faction_id、掩码（mon..sun 七位，与真端逐位一致）；
- 无车道路由且无移植定义的行保留在快照（休眠行，until-ported：覆盖后自动转为强制，
  沿用 quest-prerequisite 契约的既有模式）。
用法：python3 -B sync_faction_rotation_and_contract.py [--dry-run]
"""
import argparse
import re
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
RETAIL = Path(f"{REPO.parent / '58Server'}/Map/XML")
ROTATION = REPO / 'src/main/resources/aion/data/static_data/npc_factions/npc_factions_quest.xml'
CONTRACT = REPO / 'src/test/resources/quest/quest-npc-faction-retail-contract.tsv'
DAYS = ('mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun')

# 与生产 NativeNpcFactionNames 一致的势力名 → id（内部名，唯一事实来源）。
# The canonical faction-name-to-id map, mirroring NativeNpcFactionNames.
NAMES = {"Army_Da": 6, "Army_Li": 3, "BountyHunter_Da": 7, "BountyHunter_Li": 4,
         "Greenhat_D": 18, "Greenhat_L": 17, "GuardianOfDivine": 2, "GuardianOfTower": 5,
         "Silverlin_D": 16, "Silverlin_L": 15}


def faction_ids():
    """真端 npcfactions.xml 名 → id（轮换表镜像用，覆盖全部真端势力）。
    / Retail faction-name-to-id from the retail definitions, covering every faction."""
    text = (RETAIL / 'npcfactions.xml').read_text(encoding='utf-16', errors='replace')
    out = {}
    for m in re.finditer(r'<npcfaction>\s*<id>(\d+)</id>\s*<name>([^<]+)</name>', text):
        out.setdefault(m.group(2).strip(), int(m.group(1)))
    return out


def retail_rows():
    """真端轮换表 → {quest_id: (faction_id, mask 七位字符串)}。 / Retail rotation rows."""
    ids = faction_ids()
    text = (RETAIL / 'npcfactions_quest.xml').read_text(encoding='utf-16', errors='replace')
    out = {}
    for m in re.finditer(r'<quest_id\s+quest_id="(\d+)"[^>]*>(.*?)</quest_id>', text, re.S):
        block = m.group(2)
        name = re.search(r'<npcfaction_name>([^<]*)</npcfaction_name>', block)
        mask = ''
        for day in DAYS:
            # 真端把 0 位省略不写：缺元素即 0。 / Retail omits zero bits: a missing element is 0.
            found = re.search(r'<%s>(\d)</%s>' % (day, day), block)
            mask += found.group(1) if found else '0'
        out[int(m.group(1))] = (ids.get(name.group(1).strip(), 0) if name else 0, mask)
    return out


def routed_sentinels():
    """三条发放车道（Talk/Collect/Hunt）的 `_faction_` 哨兵行。 / Lane-routed `_faction_` rows."""
    routed = set()
    for family in ('Quest_SimpleTalk.xml', 'Quest_SimpleCollectItem.xml', 'Quest_SimpleHunt.xml'):
        text = (RETAIL / family).read_text(encoding='utf-16', errors='replace')
        for qid, body in re.findall(r'<id\s+id="(\d+)"[^>]*>(.*?)</id>', text, re.S):
            acquired = re.search(r'<acquired_npc_name>(.*?)</acquired_npc_name>', body, re.S)
            if acquired and acquired.group(1).strip() == '_faction_':
                routed.add(int(qid))
    return routed


def sync_rotation(rows, dry_run):
    """生产轮换表对齐真端：补缺行 + 修掩码漂移。 / Align the production rotation table."""
    text = ROTATION.read_text(encoding='utf-8')
    present = {int(m.group(1)) for m in re.finditer(r'quest_id="(\d+)"', text)}
    added, fixed = [], []
    for quest_id, (faction_id, mask) in sorted(rows.items()):
        if faction_id == 0 or '?' in mask:
            continue
        if quest_id not in present:
            attrs = ' '.join('%s="%s"' % (d, mask[i]) for i, d in enumerate(DAYS))
            line = '\t<npc_faction_quest quest_id="%d" faction_id="%d" %s/>\n' % (
                quest_id, faction_id, attrs)
            anchor = text.rindex('</npc_faction_quests>')
            text = text[:anchor] + line + text[anchor:]
            added.append(quest_id)
            continue
        pattern = re.compile(r'(<npc_faction_quest quest_id="%d" faction_id="\d+"[^>]*)/>' % quest_id)
        match = pattern.search(text)
        tag = match.group(0)
        new_tag = tag
        for index, day in enumerate(DAYS):
            new_tag = re.sub(r'%s="\d"' % day, '%s="%s"' % (day, mask[index]), new_tag)
        if new_tag != tag:
            text = text.replace(tag, new_tag, 1)
            fixed.append(quest_id)
    if not dry_run:
        ROTATION.write_text(text, encoding='utf-8')
    print('轮换表：补行=%s 修掩码=%s' % (added, fixed))


def rebuild_contract(rows, routed, dry_run):
    """契约快照 v2：旧 253 行 ∪ 新可达行，三列（含真端掩码）。 / Contract snapshot v2."""
    old = {}
    for line in CONTRACT.read_text(encoding='utf-8').splitlines():
        if line and not line.startswith('#') and not line.startswith('quest_id'):
            parts = line.split('\t')
            old[int(parts[0])] = int(parts[1])
    population = {}
    for quest_id, faction_id in sorted(old.items()):
        mask = rows[quest_id][1]
        assert '?' not in mask, 'contract row %d has no retail rotation row' % quest_id
        population[quest_id] = (faction_id, mask)
    added = []
    for quest_id, (faction_id, mask) in sorted(rows.items()):
        if faction_id in NAMES.values() and quest_id in routed and quest_id not in population:
            population[quest_id] = (faction_id, mask)
            added.append(quest_id)
    header = (
        '# Aion 5.8 阵营日常归属契约 v2（native 车道时代）\n'
        '# 人口 = 真端轮换表 ∧ 势力名可解析 ∧（家族车道路由 ∪ 旧评审 253 行）= %d 行；\n'
        '# 无路由且无移植定义的行为休眠行（until-ported：覆盖后自动转为强制）；\n'
        '# 掩码列 = 真端 npcfactions_quest.xml 星期位逐位镜像（mon..sun），全 0 = 真端本征不轮换。\n'
        '# 来源：真端 npcfactions_quest.xml + 三发放车道哨兵行普查（P8 §10.3-#25）。\n'
        '# Faction daily ownership contract v2: retail rotation rows with a resolvable faction,\n'
        '# unioned with the reviewed 253-row snapshot; dormant rows stay until-ported; the mask\n'
        '# column mirrors the retail weekday bits exactly.\n'
        'quest_id\tfaction_id\tmask\n' % len(population)
    )
    body = ''.join('%d\t%d\t%s\n' % (quest_id, population[quest_id][0], population[quest_id][1])
                   for quest_id in sorted(population))
    if not dry_run:
        CONTRACT.write_text(header + body, encoding='utf-8')
    print('契约快照：%d 行（新增可达行 %s）' % (len(population), added))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()
    rows = retail_rows()
    routed = routed_sentinels()
    sync_rotation(rows, args.dry_run)
    rebuild_contract(rows, routed, args.dry_run)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
