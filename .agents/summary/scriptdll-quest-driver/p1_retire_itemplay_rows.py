#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P1 wave-2：SimpleItemPlay wave-1 六行退役落地。

前置断言（任一不满足即中止，不写盘）：
  1. retention（两副本）中 6 行 owner=RETAIL_TABLE/SimpleItemPlay（生成器已按裁定表翻转）；
  2. 9 行 KEEP 仍为 XML_RETENTION/SEMANTIC_GAP:RETAIL_*（不得连带翻转）；
  3. 驱动已接线（RetailQuestDriver 源码含 compileSimpleItemPlay 分支）；
  4. 6 个生产 XML 在盘、9 个 KEEP XML 在盘。

动作：
  1. Path.unlink() 删除 6 个生产 XML（13704/13708/19048/23704/23708/29048；不用 shell 删除）；
  2. quest_definition_catalog.xml 移除对应 <definition> 行（同 p0c9 落地脚本口径）；
  3. 打印退役计数（verify_retirement.py 随后单独跑）。

用法：python3 -B p1_retire_itemplay_rows.py [--dry-run]
"""
import argparse
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent.parent
QUESTS = REPO / 'src/main/resources/aion/data/static_data/quest_definition/quests/%d.xml'
CATALOG = REPO / 'src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml'
RETENTION_COPIES = (
    REPO / 'src/test/resources/quest/retail-xml-retention.tsv',
    REPO / 'src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv',
)
DRIVER_SRC = REPO / 'src/main/java/com/aionemu/gameserver/questEngine/retail/RetailQuestDriver.java'

ADOPT = (13704, 13708, 19048, 23704, 23708, 29048)
KEEP = {18213: 'RETAIL_TALK_CHAIN', 28213: 'RETAIL_TALK_CHAIN',
    18828: 'RETAIL_ACQUIRE_NPC_UNRESOLVED', 28828: 'RETAIL_ACQUIRE_NPC_UNRESOLVED',
    50048: 'RETAIL_ACQUIRE_NPC_UNRESOLVED',
    39713: 'RETAIL_ACQUIRE_NPC_SENTINEL', 49713: 'RETAIL_ACQUIRE_NPC_SENTINEL',
    80255: 'RETAIL_ADVANCE_UNEXPRESSED', 80256: 'RETAIL_ADVANCE_UNEXPRESSED'}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()

    for retention in RETENTION_COPIES:
        rows = {}
        for line in retention.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            parts = line.split('\t')
            rows[int(parts[0])] = (parts[1], parts[2], parts[3])
        for quest_id in ADOPT:
            owner, family, reason = rows[quest_id]
            assert (owner, family, reason) == ('RETAIL_TABLE', 'SimpleItemPlay', 'OK'), \
                '%s retention 未翻转：%s/%s/%s（先跑 build_retention_list.py）' % (quest_id, owner, family, reason)
        for quest_id, code in KEEP.items():
            owner, family, reason = rows[quest_id]
            assert (owner, family) == ('XML_RETENTION', 'SimpleItemPlay') \
                and reason == 'SEMANTIC_GAP:%s' % code, \
                '%s KEEP 行被搅动：%s/%s/%s' % (quest_id, owner, family, reason)
    assert 'compileSimpleItemPlay' in DRIVER_SRC.read_text(encoding='utf-8'), '驱动未接线'
    missing = [q for q in list(ADOPT) + list(KEEP) if not Path(str(QUESTS) % q).exists()]
    assert not missing, 'XML 缺失：%s' % missing
    print('前置断言全过：ADOPT 6（retail 已翻转）+ KEEP 9（SEMANTIC_GAP 原样）+ 驱动已接线')

    if args.dry_run:
        print('dry-run：将删除 %s；catalog 移除 6 行' % [str(QUESTS) % q for q in ADOPT])
        return 0

    deleted = 0
    for quest_id in ADOPT:
        Path(str(QUESTS) % quest_id).unlink()
        deleted += 1
    print('删除 XML %d 个：%s' % (deleted, list(ADOPT)))

    catalog_lines = CATALOG.read_text(encoding='utf-8').splitlines()
    kept = [line for line in catalog_lines
        if not (line.strip().startswith('<definition ') and
            any('id="%d"' % q in line.split('resource=')[0] for q in ADOPT))]
    removed = len(catalog_lines) - len(kept)
    assert removed == len(ADOPT), 'catalog 移除 %d 行 != 预期 %d' % (removed, len(ADOPT))
    CATALOG.write_text('\n'.join(kept) + '\n', encoding='utf-8')
    print('catalog -> 移除 %d 条（余 %d）' % (removed, sum(1 for line in kept if '<definition' in line)))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
