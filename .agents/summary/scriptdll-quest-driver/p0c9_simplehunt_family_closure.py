#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-9：SimpleHunt 族收口对账（942 行三分区 + 四桶零差集 + 目录/磁盘对账）。

对账不变量（任一不成立即非收口态，脚本以非零码退出并列出差集）：
  1. 保留清单 family=SimpleHunt 的行数 = 942（族目标），owner ∈ {RETAIL_TABLE, XML_RETENTION}。
  2. XML_RETENTION 行与四个登记源**精确互斥且并集相等**：
       缺口表   simplehunt-dialog-route-gaps.txt      （真端表缺口，P0c-8c 起 4 稳定码）
       拒绝表   src/test/resources/quest/retail-simplehunt-compiler-rejects.tsv（族编译器拒绝码）
       覆盖缺口 retail-simple-hunt-adjudicated-decisions.tsv 的 QUEST_SPAWN_UNEXPRESSED 行（驱动覆盖）
       等价残留 phase5-3-rejections.txt               （P0c-8a 后仅剩待裁定行，收口态应为空）
  3. RETAIL_TABLE 行：XML 已删（磁盘无文件）且 catalog 无条目。
     XML_RETENTION 行：XML 在磁盘且 catalog 有条目。
  4. 生产裁定表（ADOPT 行）与 XML_RETENTION 行**无交集**（已裁定采纳的行不得仍走 XML）。
  5. 拒绝表/缺口表的码与保留清单 reason 的 SEMANTIC_GAP:<码> 逐一相等。

输出：
  p0c9-family-reconciliation.tsv   每行一条：<quest_id>\t<owner>\t<bucket>\t<code>\t<status>
  p0c9-family-divergence-table.tsv 族级分歧表：<族>\t<码>\t<行数>\t<可否归零>\t<归零路径>

用法：python3 -B p0c9_simplehunt_family_closure.py
"""
import os
from collections import Counter
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
RETENTION = REPO / 'src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv'
CATALOG = REPO / 'src/main/resources/aion/data/static_data/quest/definitions/quest_definition_catalog.xml'
QUESTS = REPO / 'src/main/resources/aion/data/static_data/quest/definitions/quests'
GAPS = HERE / 'simplehunt-dialog-route-gaps.txt'
REJECTS = REPO / 'src/test/resources/quest/retail-simplehunt-compiler-rejects.tsv'
PHASE53 = HERE / 'phase5-3-rejections.txt'
ADJUDICATED = REPO / 'src/test/resources/quest/retail-simple-hunt-adjudicated-decisions.tsv'
M3D = HERE / 'm3d-downgraded-quests.tsv'
FAMILY_TARGET = 942

# 族级分歧表：码 → (可否归零, 归零路径)。/ Code → (zeroable, path) for the divergence table.
DIVERGENCE_NOTES = {
    'RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH':
        ('可归零（未排期）', '需按 M5-b2c 三类哨兵发放系统接线 CHALLENGE_TASK 挑战任务发放（P0c-2/3 先例），需 ScriptDLL 证据'),
    'RETAIL_COUNTER_EXCEEDS_6BIT':
        ('不可归零（当前）', '真端计数 > 63 超出 6 位 quest_vars 打包上限；扩位会破坏客户端 SECTION 门控合同'),
    'RETAIL_MONSTER_UNRESOLVED':
        ('可归零（需证据）', 'npc_templates 无精确名匹配；需补客户端/ScriptDLL 名→id 解析证据后重算'),
    'QUEST_SPAWN_UNEXPRESSED':
        ('不可归零（当前）', '族表无刷怪列；任务自身 <spawn> 是该 NPC 进世界的唯一来源（P0c-6 裁定）'),
    'KILL_COVERAGE_LOSS':
        ('不可归零（当前）', '真端编译集缺客户端点名且生产刷怪可达的怪；P0c-9 已复核 monster* 列外无可用真端源'),
    'CLIENT_BUTTON_UNWIRED':
        ('不可归零（当前）', '客户端按钮（select5 20002 / 报告页 1009）在真端路由集无对应动作；P0c-9 已逐行复核'),
    'CLASS_SELECTABLE_REWARD':
        ('已归零（P0c-9）', '<class>_selectable_reward 区块已落地 RetailSimpleHuntDefinitionCompiler（11102/28313 转 ADOPT）'),
    'XML_EXTRA_REWARD':
        ('不可归零（当前）', '旧 XML 多出客户端任务书点名的奖励物品，真端表没有；按客户端契约保留 XML'),
    'KILL_ROUTE_MISMATCH': ('待裁定（P0c-9）', 'phase5-3 旧码残留；经 p0c8c 判据机逐行裁定后归入 ADOPT/稳定码'),
    'NODE_SET_MISMATCH': ('待裁定（P0c-9）', 'phase5-3 旧码残留；经 p0c8c 判据机逐行裁定后归入 ADOPT/稳定码'),
    'NODE_FIELDS_MISMATCH': ('待裁定（P0c-9）', 'phase5-3 旧码残留；经 p0c8c 判据机逐行裁定后归入 ADOPT/稳定码'),
}


def read_ids(path, code_column=1, colon=False):
    """读 id→码 登记表（# 注释跳过）。colon=True 时行首是 `id:码 详情`（phase5-3 旧格式）。

    Reads an id→code registry, skipping comments; colon=True parses the legacy `id:code detail` lines.
    """
    rows = {}
    if not path.exists():
        return rows
    for line in path.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        if colon:
            head, _, rest = line.partition(':')
            rows[int(head.strip())] = rest.split()[0].strip() if rest.split() else '-'
            continue
        parts = line.split('\t')
        rows[int(parts[0])] = parts[code_column].strip() if len(parts) > code_column else '-'
    return rows


def main():
    problems = []
    # 1) 保留清单族行
    family = {}
    for line in RETENTION.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        if len(parts) > 3 and parts[2] == 'SimpleHunt':
            family[int(parts[0])] = (parts[1], parts[3])
    print('保留清单 SimpleHunt 行数 = %d（目标 %d）' % (len(family), FAMILY_TARGET))
    if len(family) != FAMILY_TARGET:
        problems.append('族行数 %d != %d' % (len(family), FAMILY_TARGET))

    retail_owned = {q for q, (owner, _) in family.items() if owner == 'RETAIL_TABLE'}
    xml_kept = {q for q, (owner, _) in family.items() if owner == 'XML_RETENTION'}
    if retail_owned | xml_kept != set(family):
        problems.append('owner 出现未知值：%s' % (set(family) - retail_owned - xml_kept))

    # 2) 四桶
    gaps = read_ids(GAPS)
    rejects = read_ids(REJECTS)
    phase53 = read_ids(PHASE53, colon=True)
    spawn_gap = {q for q, (owner, reason) in family.items()
        if owner == 'XML_RETENTION' and reason == 'SEMANTIC_GAP:QUEST_SPAWN_UNEXPRESSED'}
    buckets = {}
    overlap = []
    for name, ids in (('GAP', gaps), ('REJECT', rejects), ('PHASE53', phase53), ('SPAWN_GAP', spawn_gap)):
        for q in ids:
            if q in buckets:
                overlap.append('%d in %s+%s' % (q, buckets[q], name))
            buckets[q] = name
    if overlap:
        problems.append('桶重叠：%s' % overlap[:8])
    unclassified = xml_kept - set(buckets)
    dangling = set(buckets) - xml_kept
    if unclassified:
        problems.append('XML_RETENTION 未归桶 %d 行：%s' % (len(unclassified), sorted(unclassified)[:12]))
    if dangling:
        problems.append('归桶行不在 XML_RETENTION：%s' % sorted(dangling)[:12])
    print('四桶：GAP %d + REJECT %d + PHASE53 %d + SPAWN_GAP %d = %d（XML_RETENTION %d）'
        % (len(gaps), len(rejects), len(phase53), len(spawn_gap), len(buckets), len(xml_kept)))

    # 3) 磁盘 / catalog
    import re
    catalog_ids = {int(m) for m in re.findall(r'<definition id="(\d+)"', CATALOG.read_text(encoding='utf-8'))}
    for q in retail_owned:
        if (QUESTS / ('%d.xml' % q)).exists():
            problems.append('%d owner=RETAIL_TABLE 但 XML 仍在磁盘' % q)
        if q in catalog_ids:
            problems.append('%d owner=RETAIL_TABLE 但 catalog 仍有条目' % q)
    for q in xml_kept:
        if not (QUESTS / ('%d.xml' % q)).exists():
            problems.append('%d owner=XML_RETENTION 但 XML 不在磁盘' % q)
        if q not in catalog_ids:
            problems.append('%d owner=XML_RETENTION 但 catalog 无条目' % q)

    # 4) 裁定表 ADOPT_RETAIL 行与保留行无交集（KEEP_XML 行允许同在，如 QUEST_SPAWN_UNEXPRESSED）
    adjudicated_rows = read_ids(ADJUDICATED)
    adjudicated_adopt = {q for q, verdict in adjudicated_rows.items() if verdict == 'ADOPT_RETAIL'}
    clash = sorted(adjudicated_adopt & xml_kept)
    if clash:
        problems.append('裁定表 ADOPT 行仍走 XML：%s' % clash[:12])
    print('裁定表 %d 行（ADOPT %d）；ADOPT 与 XML_RETENTION 交集 %d'
        % (len(adjudicated_rows), len(adjudicated_adopt), len(clash)))

    # 5) 码一致性（保留清单 vs 缺口表/拒绝表/phase5-3）
    for q, code in gaps.items():
        if family.get(q, ('', ''))[1] != 'SEMANTIC_GAP:%s' % code:
            problems.append('%d 缺口表码 %s 与保留清单 %s 不一致' % (q, code, family.get(q, ('', ''))[1]))
    for q, code in rejects.items():
        if family.get(q, ('', ''))[1] != 'SEMANTIC_GAP:%s' % code:
            problems.append('%d 拒绝表码 %s 与保留清单 %s 不一致' % (q, code, family.get(q, ('', ''))[1]))
    for q, code in phase53.items():
        if family.get(q, ('', ''))[1] != 'SEMANTIC_GAP:%s' % code:
            problems.append('%d phase5-3 码 %s 与保留清单 %s 不一致' % (q, code, family.get(q, ('', ''))[1]))

    # 行级 reconciliation 输出
    lines = ['# P0c-9 SimpleHunt 族收口对账（python3 -B p0c9_simplehunt_family_closure.py 重跑）',
        '# quest_id\towner\tbucket\tcode\tstatus']
    for q in sorted(family):
        owner, reason = family[q]
        bucket = 'RETAIL_TABLE' if owner == 'RETAIL_TABLE' else buckets.get(q, 'UNCLASSIFIED')
        code = '-' if owner == 'RETAIL_TABLE' else reason.removeprefix('SEMANTIC_GAP:')
        status = 'OK'
        if owner == 'RETAIL_TABLE' and ((QUESTS / ('%d.xml' % q)).exists() or q in catalog_ids):
            status = 'BROKEN'
        if owner == 'XML_RETENTION' and bucket == 'UNCLASSIFIED':
            status = 'UNCLASSIFIED'
        lines.append('%d\t%s\t%s\t%s\t%s' % (q, owner, bucket, code, status))
    (HERE / 'p0c9-family-reconciliation.tsv').write_text('\n'.join(lines) + '\n', encoding='utf-8')

    # 族级分歧表（SimpleHunt 四桶 + SimpleTalk M3-d 降级）
    div = ['# P0c-9 族级分歧表：<族>\t<码>\t<行数>\t<可否归零>\t<归零路径>']
    code_rows = Counter()
    for q, code in gaps.items():
        code_rows[('SimpleHunt', code)] += 1
    for q, code in rejects.items():
        code_rows[('SimpleHunt', code)] += 1
    for q in spawn_gap:
        code_rows[('SimpleHunt', 'QUEST_SPAWN_UNEXPRESSED')] += 1
    for q, code in phase53.items():
        code_rows[('SimpleHunt', code)] += 1
    if M3D.exists():
        for line in M3D.read_text(encoding='utf-8').splitlines():
            if line.startswith('#') or not line.strip():
                continue
            code_rows[('SimpleTalk', 'M3D_' + line.split('\t')[1].strip())] += 1
    for (fam, code), count in sorted(code_rows.items()):
        zeroable, path = DIVERGENCE_NOTES.get(code, ('待评估', '-'))
        div.append('%s\t%s\t%d\t%s\t%s' % (fam, code, count, zeroable, path))
    (HERE / 'p0c9-family-divergence-table.tsv').write_text('\n'.join(div) + '\n', encoding='utf-8')
    print('分歧表 %d 行 -> p0c9-family-divergence-table.tsv' % len(code_rows))

    if problems:
        print('\n== 未收口（%d 个问题）==' % len(problems))
        for p in problems[:30]:
            print('  ' + p)
        return 1
    print('\n== 收口态：全部不变量成立 ==')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
