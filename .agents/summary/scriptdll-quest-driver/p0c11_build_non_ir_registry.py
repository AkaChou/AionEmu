#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-11：非 IR 轴统一登记表生成器（采纳真端会连带改变的"节点/转换之外"的轴）。

三个轴源（门禁 `RetailNonIrAxisGateTest` 消费本表 + 各源）：
  ① CAP_LEVEL              src/test/resources/quest/quest-start-metadata-retail-cap-exceptions.tsv
                           （服务器侧有意封顶；已采纳行不可能带封顶 ⇒ 生产==真端 UNLIMITED）
  ② PREREQ_DUAL_EXPRESSION src/test/resources/quest/retail-metadata-divergences.tsv
                           （前置条件两种等价表达 start-conditions(finished) ↔ prerequisites，
                            映射器按固定规则归属、分歧按 M1 登记——轴级登记，M1 门禁守）
  ③ LEGACY_SAVE_HEAL       src/test/resources/quest/retail-legacy-save-normalization.tsv
                           （旧 XML 的 EnterWorld 存档自愈边，真端不表达；可选一次性 DB 归一化）

输出 src/test/resources/quest/retail-non-ir-axis-registry.tsv：
  quest_id / axis / disposition / detail / source
采纳切片落地后（删 XML/改封顶清单/追加自愈边登记）**必须重跑本脚本**刷新统一登记表，
否则 `RetailNonIrAxisGateTest.registryMatchesBothQuestLevelSources` 失败。

用法：python3 -B p0c11_build_non_ir_registry.py
"""
import os
import re
from collections import defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = Path(f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}")
CAP = REPO / 'src/test/resources/quest/quest-start-metadata-retail-cap-exceptions.tsv'
HEAL = REPO / 'src/test/resources/quest/retail-legacy-save-normalization.tsv'
DIV = REPO / 'src/test/resources/quest/retail-metadata-divergences.tsv'
OUT = REPO / 'src/test/resources/quest/retail-non-ir-axis-registry.tsv'


def main():
    rows = []
    # ① 封顶/等级（逐任务）
    for line in CAP.read_text(encoding='utf-8').splitlines():
        if not line.strip() or line.startswith('#'):
            continue
        parts = line.split('\t')
        if len(parts) < 2 or not parts[0].strip().isdigit():
            continue  # 表头
        rows.append((parts[0], 'CAP_LEVEL', 'SERVER_SIDE_CAP_INTENTIONAL',
            'cap=%s %s' % (parts[1], parts[2].strip() if len(parts) > 2 else ''),
            'quest-start-metadata-retail-cap-exceptions.tsv'))
    # ③ 旧存档自愈边（按任务聚合）
    heal_edges = defaultdict(list)
    for line in HEAL.read_text(encoding='utf-8').splitlines():
        if not line.strip() or line.startswith('#'):
            continue
        parts = line.split('\t')
        if len(parts) < 4 or not parts[0].strip().isdigit():
            continue
        heal_edges[parts[0]].append('%s->%s' % (parts[1], parts[2]))
    for quest_id, edges in sorted(heal_edges.items(), key=lambda kv: int(kv[0])):
        rows.append((quest_id, 'LEGACY_SAVE_HEAL', 'DB_NORMALIZATION_OPTIONAL',
            'edges=%d (%s)' % (len(edges), ';'.join(edges[:3]) + ('…' if len(edges) > 3 else '')),
            'retail-legacy-save-normalization.tsv'))
    # ② 前置两表达（轴级）
    axis_counts = defaultdict(int)
    for line in DIV.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        if len(parts) > 1 and parts[1] in ('startConditions', 'prerequisites'):
            axis_counts[parts[1]] += 1
    rows.append(('*', 'PREREQ_DUAL_EXPRESSION', 'METADATA_MAPPING_RULE',
        'startConditions=%d;prerequisites=%d（分歧行数，M1 门禁守）' % (axis_counts['startConditions'],
            axis_counts['prerequisites']),
        'retail-metadata-divergences.tsv'))

    text = [
        '# 非 IR 轴统一登记表（P0c-11）——采纳真端会连带改变的"节点/转换之外"的轴',
        '# 生成：python3 -B .agents/summary/scriptdll-quest-driver/p0c11_build_non_ir_registry.py（采纳切片落地后必须重跑）',
        '# 门禁：RetailNonIrAxisGateTest（登记新鲜度 + 封顶行不得为已采纳 + 自愈边任务必须真端驱动且无 EnterWorld 路由）',
        '# quest_id\taxis\tdisposition\tdetail\tsource',
    ]
    text += ['\t'.join(row) for row in rows]
    OUT.write_text('\n'.join(text) + '\n', encoding='utf-8')
    by_axis = defaultdict(int)
    for row in rows:
        by_axis[row[1]] += 1
    print('统一登记表 %d 行 -> %s' % (len(rows), OUT))
    print('轴分布:', dict(by_axis))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
