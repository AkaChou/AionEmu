#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-10h：SimpleTalk wave B B-6 微波两行退役落地（varless 节点回放支持后等价，判例 30711 翻案）。

前置断言（任一不满足即中止，不写盘）：
  1. retention（两副本）中 16 行 owner=RETAIL_TABLE/SimpleTalk（生成器已按 B-1 裁定表翻转；
     其中 19004 为重裁定升级——p0c10f 曾因登记表缺 E 记录判 KEEP，E 记录修复后等价成立）；
  2. 冻结指纹表含 16 行（99 = wave A 83 + B-1 16）；
  3. 驱动已接线（RetailQuestDriver 源码含 compileSimpleTalk 分支）；
  4. 16 个生产 XML 在盘。

动作：
  1. Path.unlink() 删除 16 个生产 XML（不用 shell 删除）；
  2. quest_definition_catalog.xml 移除对应 <definition> 行（同 p1/p0c9 落地脚本口径）；
  3. 打印退役计数（verify_retirement.py 随后单独跑）。

用法：python3 -B p0c10h_retire_b6_rows.py [--dry-run]
"""
import argparse
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent.parent
QUESTS = REPO / 'src/main/resources/aion/data/static_data/quest/definitions/quests/%d.xml'
CATALOG = REPO / 'src/main/resources/aion/data/static_data/quest/definitions/quest_definition_catalog.xml'
RETENTION_COPIES = (
    REPO / 'src/test/resources/quest/retail-xml-retention.tsv',
    REPO / 'src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv',
)
DRIVER_SRC = REPO / 'src/main/java/com/aionemu/gameserver/questEngine/retail/RetailQuestDriver.java'
FINGERPRINTS = REPO / 'src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv'

# B-6 ADOPT：varless 节点回放支持后新增等价的两行（30711 当年 KEEP 系登记缺其 varless 节点）。
ADOPT = (30711, 30761)


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
            assert (owner, family, reason) == ('RETAIL_TABLE', 'SimpleTalk', 'OK'), \
                '%s retention 未翻转：%s/%s/%s（先跑 build_retention_list.py）' % (quest_id, owner, family, reason)
    frozen = set()
    for line in FINGERPRINTS.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        frozen.add(int(line.split('\t')[0]))
    assert set(ADOPT) <= frozen, '冻结指纹缺行：%s' % sorted(set(ADOPT) - frozen)
    assert 'compileSimpleTalk' in DRIVER_SRC.read_text(encoding='utf-8'), '驱动未接线'
    missing = [q for q in ADOPT if not Path(str(QUESTS) % q).exists()]
    assert not missing, 'XML 缺失：%s' % missing
    print('前置断言全过：ADOPT 16（retail 已翻转 + 指纹已冻结）+ 驱动已接线')

    if args.dry_run:
        print('dry-run：将删除 %s；catalog 移除 2 行' % [str(QUESTS) % q for q in ADOPT])
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
