#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-12：单步过场行 13 行退役落地（10k 方法论：movie=cutsceneid1 + cs1_haction 触发限 canonical 路由）（真端表 give_item 为形状权威，编译器经
RetailQuestWorkItems 通道合成 GiveItem；差异四类逐一裁定 真端对、XML 错，见裁定表）。

前置断言（任一不满足即中止，不写盘）：
  1. retention（两副本）中 116 行 owner=RETAIL_TABLE/SimpleTalk/OK（生成器已按 p0c10m 裁定表翻转）；
  2. 驱动已接线（RetailQuestDriver 源码含 compileSimpleTalk 分支）；
  3. 116 个生产 XML 在盘；
  4. 漂移登记中 116 行已翻转（非 REJECTED:RETAIL_TALK_ITEM）。

动作：
  1. Path.unlink() 删除 116 个生产 XML（不用 shell 删除）；
  2. quest_definition_catalog.xml 移除对应 <definition> 行（同 p0c10j 落地脚本口径）；
  3. 打印退役计数（verify_retirement.py 随后单独跑）。

用法：python3 -B p0c10m_retire_item_rows.py [--dry-run]
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
DRIFT = REPO / 'src/test/resources/quest/retail-simple-talk-drift.tsv'
DECISIONS = HERE / 'p0c14-report-decisions.tsv'


def adopt_ids():
    ids = []
    for line in DECISIONS.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        if parts[1] != 'ADOPT_RETAIL':
            continue  # p0c10n 裁定表含 28 行 KEEP_XML（craft 缺口），退役只取 ADOPT
        ids.append(int(parts[0]))
    assert len(ids) == 16, '裁定行数 %d != 16' % len(ids)
    return ids


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()
    adopt = adopt_ids()

    for retention in RETENTION_COPIES:
        rows = {}
        for line in retention.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            parts = line.split('\t')
            rows[int(parts[0])] = (parts[1], parts[2], parts[3])
        for quest_id in adopt:
            owner, family, reason = rows[quest_id]
            assert (owner, family, reason) == ('RETAIL_TABLE', 'SimpleTalk', 'OK'), \
                '%s retention 未翻转：%s/%s/%s（先跑 build_retention_list.py）' % (quest_id, owner, family, reason)
    drift = {}
    for line in DRIFT.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        drift[int(parts[0])] = parts[1]
    stale = [q for q in adopt if drift[q].startswith('REJECTED:')]
    assert not stale, '漂移登记未翻转：%s' % stale[:5]
    assert 'compileSimpleTalk' in DRIVER_SRC.read_text(encoding='utf-8'), '驱动未接线'
    missing = [q for q in adopt if not Path(str(QUESTS) % q).exists()]
    assert not missing, 'XML 缺失：%s' % missing
    print('前置断言全过：ADOPT 16（retail 已翻转 + 漂移已冻结）+ 驱动已接线')

    if args.dry_run:
        print('dry-run：将删除 %d 个 XML；catalog 移除 %d 行' % (len(adopt), len(adopt)))
        return 0

    for quest_id in adopt:
        Path(str(QUESTS) % quest_id).unlink()
    print('删除 XML %d 个' % len(adopt))

    catalog_lines = CATALOG.read_text(encoding='utf-8').splitlines()
    kept = [line for line in catalog_lines
        if not (line.strip().startswith('<definition ') and
            any('id="%d"' % q in line.split('resource=')[0] for q in adopt))]
    removed = len(catalog_lines) - len(kept)
    assert removed == len(adopt), 'catalog 移除 %d 行 != 预期 %d' % (removed, len(adopt))
    CATALOG.write_text('\n'.join(kept) + '\n', encoding='utf-8')
    print('catalog -> 移除 %d 条（余 %d）' % (removed, sum(1 for line in kept if '<definition' in line)))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
