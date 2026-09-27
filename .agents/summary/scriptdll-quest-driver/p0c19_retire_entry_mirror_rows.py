#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-19：入口 NPC 镜像三行（11003/80356/80365）退役落地。

裁定依据（真端对、XML 错，见 p0c19-entry-npc-mirror-decisions.tsv 头注）：
  客户端模板索引 start/end NPC 列声明接取/交付分离（11003: 798933/798942；80356: 831815/831819；
  80365: 831827/831819），真端表 acquired/reward 同对；SimpleTalk 单步编译的 itemCheckReportFlow
  按真端表发在交付 NPC。遗留 XML 的对称双 NPC 全形状 = 手工漂移。
  P0c-18 的"单步 report 流缺失"结论系探针单 NPC 过滤伪影，第六类缺口不存在。

前置断言（任一不满足即中止，不写盘）：
  1. retention（两副本）中 3 行 owner=RETAIL_TABLE/SimpleTalk/OK（补丁已翻转）；
  2. 驱动已接线（RetailQuestDriver 源码含 compileSimpleTalk 分支）；
  3. 判官镜像已改指交付 NPC（LegacyTemplateMirrorRouteRegressionTest 含 798942/831819）；
  4. 3 个生产 XML 在盘。

动作：删除 3 个生产 XML + quest_definition_catalog.xml 移除对应 <definition> 行（同 p0c16 口径）。

用法：python3 -B p0c19_retire_entry_mirror_rows.py [--dry-run]
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
JUDGE_SRC = REPO / 'src/test/java/com/aionemu/gameserver/questEngine/definition/LegacyTemplateMirrorRouteRegressionTest.java'
DECISIONS = HERE / 'p0c19-entry-npc-mirror-decisions.tsv'


def adopt_ids():
    ids = []
    for line in DECISIONS.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        if parts[1] != 'ADOPT_RETAIL':
            continue
        ids.append(int(parts[0]))
    assert len(ids) == 3, '裁定行数 %d != 3' % len(ids)
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
                '%s retention 未翻转：%s/%s/%s' % (quest_id, owner, family, reason)
    judge = JUDGE_SRC.read_text(encoding='utf-8')
    assert 'new ItemMirror(11003, 798942, 39, 2716)' in judge, '判官镜像未更新（11003 -> 798942）'
    assert 'new ItemMirror(80356, 831819, 39, 2716)' in judge, '判官镜像未更新（80356 -> 831819）'
    assert 'new ItemMirror(80365, 831819, 39, 2716)' in judge, '判官镜像未更新（80365 -> 831819）'
    assert 'compileSimpleTalk' in DRIVER_SRC.read_text(encoding='utf-8'), '驱动未接线'
    already = [q for q in adopt if not Path(str(QUESTS) % q).exists()]
    to_retire = [q for q in adopt if q not in already]
    print('前置断言全过：ADOPT 3（retail 已翻转 + 判官镜像已改交付 NPC）+ 驱动已接线')

    if args.dry_run:
        print('dry-run：将删除 %d 个 XML；catalog 移除 %d 行' % (len(to_retire), len(to_retire)))
        return 0

    for quest_id in to_retire:
        Path(str(QUESTS) % quest_id).unlink()
    print('删除 XML %d 个（已退役 %d）' % (len(to_retire), len(already)))

    catalog_lines = CATALOG.read_text(encoding='utf-8').splitlines()
    kept = [line for line in catalog_lines
        if not (line.strip().startswith('<definition ') and
            any('id="%d"' % q in line.split('resource=')[0] for q in to_retire))]
    removed = len(catalog_lines) - len(kept)
    assert removed == len(to_retire), 'catalog 移除 %d 行 != 预期 %d' % (removed, len(to_retire))
    CATALOG.write_text('\n'.join(kept) + '\n', encoding='utf-8')
    print('catalog -> 移除 %d 条（余 %d）' % (removed, sum(1 for line in kept if '<definition' in line)))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
