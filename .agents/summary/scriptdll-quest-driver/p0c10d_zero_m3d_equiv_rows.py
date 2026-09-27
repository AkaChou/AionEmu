#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-10d：SimpleTalk M3-d 复核 10 行 EQUIVALENT 归零落地。

裁定来源：p0c10-m3d-recheck.tsv（P0c-10 逐行复核）中漂移分类 = EQUIVALENT 的 10 行
（80029/80344/80351/80360/80575/80577/80578/80580/80683/80686）——真端合成定义与
退役前生产 XML 在归一化 IR 层逐字等价（P5-2 词汇规则口径），M3-d `CLIENT_ROUTE` 降级
不再必要。前置：已从 `m3d-downgraded-quests.tsv` 移除 + `build_retention_list.py` 重跑。

前置断言（任一不满足即中止，不写盘）：
  1. retention（两副本）中 10 行 owner=RETAIL_TABLE/SimpleTalk/OK；
  2. `m3d-downgraded-quests.tsv` 不再含这 10 行；
  3. `retail-simple-talk-drift.tsv` 中 10 行分类 = EQUIVALENT；
  4. 10 个生产 XML 在盘。

动作：Path.unlink() 删除 10 个生产 XML + quest_definition_catalog.xml 移除对应行。

用法：python3 -B p0c10d_zero_m3d_equiv_rows.py [--dry-run]
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
DRIFT = REPO / 'src/test/resources/quest/retail-simple-talk-drift.tsv'
REGISTRY = HERE / 'm3d-downgraded-quests.tsv'

ZERO = (80029, 80344, 80351, 80360, 80575, 80577, 80578, 80580, 80683, 80686)


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
        for quest_id in ZERO:
            owner, family, reason = rows[quest_id]
            assert (owner, family, reason) == ('RETAIL_TABLE', 'SimpleTalk', 'OK'), \
                '%s retention 未翻转：%s/%s/%s' % (quest_id, owner, family, reason)
    reg_ids = {int(line.split('\t')[0]) for line in REGISTRY.read_text(encoding='utf-8').splitlines()
        if line.strip() and not line.startswith('#')}
    assert not (set(ZERO) & reg_ids), 'M3-d 登记表仍含目标行'
    drift = {}
    for line in DRIFT.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        drift[int(parts[0])] = parts[1]
    for quest_id in ZERO:
        assert drift.get(quest_id) == 'EQUIVALENT', '%s 漂移分类 != EQUIVALENT' % quest_id
    missing = [q for q in ZERO if not Path(str(QUESTS) % q).exists()]
    assert not missing, 'XML 缺失：%s' % missing
    print('前置断言全过：10 行 RETAIL_TABLE/SimpleTalk/OK + 已出登记 + EQUIVALENT + XML 在盘')

    if args.dry_run:
        print('dry-run：将删除 %d 个 XML + catalog 移除 %d 行' % (len(ZERO), len(ZERO)))
        return 0

    deleted = 0
    for quest_id in ZERO:
        Path(str(QUESTS) % quest_id).unlink()
        deleted += 1
    print('删除 XML %d 个：%s' % (deleted, list(ZERO)))

    catalog_lines = CATALOG.read_text(encoding='utf-8').splitlines()
    kept = [line for line in catalog_lines
        if not (line.strip().startswith('<definition ') and
            any('id="%d"' % q in line.split('resource=')[0] for q in ZERO))]
    removed = len(catalog_lines) - len(kept)
    assert removed == len(ZERO), 'catalog 移除 %d 行 != 预期 %d' % (removed, len(ZERO))
    CATALOG.write_text('\n'.join(kept) + '\n', encoding='utf-8')
    print('catalog -> 移除 %d 条（余 %d）' % (removed, sum(1 for line in kept if '<definition' in line)))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
