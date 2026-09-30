#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-10：SimpleTalk 族收口对账（2223 行三分区 + 三桶零差集 + M3-d 降级复核 + 族级分歧表）。

对账不变量（任一不成立即非收口态，脚本以非零码退出并列出差集）：
  1. 保留清单 family=SimpleTalk 行数 = 2223（族目标），owner ∈ {RETAIL_TABLE, XML_RETENTION}。
  2. 漂移表 `retail-simple-talk-drift.tsv` 覆盖全部 2223 行（分类逐行登记，静默漂移由门禁守）。
  3. XML_RETENTION 685 行与三个登记源**精确互斥且并集相等**：
       编译器拒绝   drift `REJECTED:<码>`（585 行，6 个稳定码）
       M3-d 降级    `m3d-downgraded-quests.tsv`（83 行，逐任务客户端合同暂缓）
       报告变体     `m3b_simple_talk_drift.py` 的 CLIENT_REPORT_VARIANT 清单（17 行）
  4. RETAIL_TABLE 行：XML 已删且 catalog 无条目；XML_RETENTION 行：XML 在磁盘且 catalog 有条目。
  5. 拒绝码与保留清单 reason 的 SEMANTIC_GAP:<码> 逐一相等。

输出：
  p0c10-family-reconciliation.tsv   每行：<quest_id>\t<owner>\t<bucket>\t<code>\t<status>
  p0c10-family-divergence-table.tsv 族级分歧表：<族>\t<码>\t<行数>\t<可否归零>\t<归零路径>
  p0c10-m3d-recheck.tsv             83 行 M3-d 降级复核（磁盘/catalog/漂移分类/证据）

用法：python3 -B p0c10_simpletalk_family_closure.py
"""
import os
import csv
import re
from collections import Counter
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = Path(f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}")
RETENTION = REPO / 'src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv'
CATALOG = REPO / 'src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml'
QUESTS = REPO / 'src/main/resources/aion/data/static_data/quest_definition/quests'
DRIFT = REPO / 'src/test/resources/quest/retail-simple-talk-drift.tsv'
M3D = HERE / 'm3d-downgraded-quests.tsv'
FAMILY_TARGET = 2223

# 族级分歧表：码 → (可否归零, 归零路径)。/ Code → (zeroable, path).
DIVERGENCE_NOTES = {
    'RETAIL_TALK_CHAIN':
        ('可归零（未排期）', '客户端任务书页链登记面已具备（quest_client_talk_pages/handin_pages）；'
            '需 SimpleTalk 链式合成扩展（P5-3 wave B 同语义先例 RETAIL_TALK_CHAIN_DEFERRED）'),
    'RETAIL_TALK_ITEM':
        ('需评估', '物品轴在真端 SimpleTalk 表无列；需客户端物品词汇/ScriptDLL 证据定归零路径（P0c-11 面候选）'),
    'RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH':
        ('可归零（未排期）', '需 CHALLENGE_TASK 挑战任务发放接线（M5-b2c 三类哨兵系统；同 SimpleHunt 84 行）'),
    'RETAIL_ACQUIRE_NPC_UNRESOLVED':
        ('可归零（需证据）', 'npc_templates 无精确名匹配；需补客户端/ScriptDLL 名→id 解析证据后重算'),
    'RETAIL_REWARD_NPC_UNRESOLVED':
        ('可归零（需证据）', '同上；交付 NPC 名解析证据'),
    'RETAIL_TALK_CUTSCENE':
        ('需评估', '过场/影片轴在真端族表无列（movie-page-turn 为 XML 编写块）；需客户端影片登记证据'),
    'RETAIL_REWARD_NPC_FACTION_COMPOSITE':
        ('不可归零（当前）', '复合势力名客户端无登记（客户端字符串表只韩文、在库无 id）；按客户端契约保留 XML'),
    'CLIENT_ROUTE':
        ('可归零（未排期）', '逐任务客户端合同暂缓（M3-d）；P5-2 交付五页词汇（handin_pages + ok 本地关闭规则）'
            '为同语义先例，可按词汇覆盖逐任务归零'),
    'CLIENT_REPORT_VARIANT':
        ('不可归零（当前）', '客户端报告页变体按钮（HACTION_SELECT4/5）在真端路由集无对应动作；'
            '类同 SimpleHunt CLIENT_BUTTON_UNWIRED，按客户端契约保留 XML'),
}


def read_tsv_pairs(path, id_col=0, code_col=1):
    rows = {}
    for line in path.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        rows[int(parts[id_col])] = parts[code_col].strip()
    return rows


def main():
    problems = []
    # 1) 保留清单族行
    family = {}
    for line in RETENTION.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        if len(parts) > 3 and parts[2] == 'SimpleTalk':
            family[int(parts[0])] = (parts[1], parts[3])
    print('保留清单 SimpleTalk 行数 = %d（目标 %d）' % (len(family), FAMILY_TARGET))
    if len(family) != FAMILY_TARGET:
        problems.append('族行数 %d != %d' % (len(family), FAMILY_TARGET))
    retail_owned = {q for q, (owner, _) in family.items() if owner == 'RETAIL_TABLE'}
    xml_kept = {q for q, (owner, _) in family.items() if owner == 'XML_RETENTION'}
    if retail_owned | xml_kept != set(family):
        problems.append('owner 出现未知值')

    # 2) 漂移表全覆盖
    drift = read_tsv_pairs(DRIFT)
    missing_drift = set(family) - set(drift)
    extra_drift = set(drift) - set(family)
    if missing_drift:
        problems.append('漂移表缺行 %d：%s' % (len(missing_drift), sorted(missing_drift)[:8]))
    if extra_drift:
        problems.append('漂移表多行 %d：%s' % (len(extra_drift), sorted(extra_drift)[:8]))

    # 3) 三桶
    rejected = {q: code[len('REJECTED:'):] for q, code in drift.items() if code.startswith('REJECTED:')}
    m3d = read_tsv_pairs(M3D, id_col=0, code_col=1) if M3D.exists() else {}
    variants = set()
    evidence_col = {}
    for line in RETENTION.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        if len(parts) > 4 and parts[3] == 'SEMANTIC_GAP:CLIENT_REPORT_VARIANT':
            variants.add(int(parts[0]))
            evidence_col[int(parts[0])] = parts[4]
    buckets, overlap = {}, []
    for name, ids in (('REJECT', rejected), ('M3D', m3d), ('REPORT_VARIANT', variants)):
        for q in ids:
            if q in buckets:
                overlap.append('%d in %s+%s' % (q, buckets[q], name))
            buckets[q] = name
    if overlap:
        problems.append('桶重叠：%s' % overlap[:8])
    unclassified = xml_kept - set(buckets)
    dangling = set(buckets) - xml_kept
    if unclassified:
        problems.append('XML_RETENTION 未归桶 %d：%s' % (len(unclassified), sorted(unclassified)[:12]))
    if dangling:
        problems.append('归桶行不在 XML_RETENTION：%s' % sorted(dangling)[:12])
    print('三桶：REJECT %d + M3D %d + REPORT_VARIANT %d = %d（XML_RETENTION %d）'
        % (len(rejected), len(m3d), len(variants), len(buckets), len(xml_kept)))

    # 4) 磁盘 / catalog
    import re as _re
    catalog_ids = {int(m) for m in _re.findall(r'<definition id="(\d+)"',
        CATALOG.read_text(encoding='utf-8'))}
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

    # 5) 码一致性
    for q, code in rejected.items():
        if family.get(q, ('', ''))[1] != 'SEMANTIC_GAP:%s' % code:
            problems.append('%d 拒绝码 %s 与保留清单 %s 不一致' % (q, code, family.get(q, ('', ''))[1]))
    for q in m3d:
        if family.get(q, ('', ''))[1] != 'SEMANTIC_GAP:CLIENT_ROUTE':
            problems.append('%d M3-d 行与保留清单 %s 不一致' % (q, family.get(q, ('', ''))[1]))

    # 行级 reconciliation 输出
    lines = ['# P0c-10 SimpleTalk 族收口对账（python3 -B p0c10_simpletalk_family_closure.py 重跑）',
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
    (HERE / 'p0c10-family-reconciliation.tsv').write_text('\n'.join(lines) + '\n', encoding='utf-8')

    # 族级分歧表
    code_rows = Counter()
    for q, code in rejected.items():
        code_rows[code] += 1
    code_rows['CLIENT_ROUTE'] += len(m3d)
    code_rows['CLIENT_REPORT_VARIANT'] += len(variants)
    div = ['# P0c-10 族级分歧表：<族>\t<码>\t<行数>\t<可否归零>\t<归零路径>']
    for code, count in sorted(code_rows.items()):
        zeroable, path = DIVERGENCE_NOTES.get(code, ('待评估', '-'))
        div.append('SimpleTalk\t%s\t%d\t%s\t%s' % (code, count, zeroable, path))
    (HERE / 'p0c10-family-divergence-table.tsv').write_text('\n'.join(div) + '\n', encoding='utf-8')
    print('分歧表 %d 行 -> p0c10-family-divergence-table.tsv' % len(code_rows))

    # M3-d 83 行复核输出
    m3d_lines = ['# P0c-10 M3-d 降级复核（83 行逐行）', '# quest_id\t磁盘\tdatalog\t漂移分类\t证据']
    for q in sorted(m3d):
        on_disk = (QUESTS / ('%d.xml' % q)).exists()
        m3d_lines.append('%d\t%s\t%s\t%s\tper-quest-client-contract' % (
            q, 'Y' if on_disk else 'N', 'Y' if q in catalog_ids else 'N', drift.get(q, '-')))
    (HERE / 'p0c10-m3d-recheck.tsv').write_text('\n'.join(m3d_lines) + '\n', encoding='utf-8')
    print('M3-d 复核 %d 行 -> p0c10-m3d-recheck.tsv' % len(m3d))

    if problems:
        print('\n== 未收口（%d 个问题）==' % len(problems))
        for p in problems[:30]:
            print('  ' + p)
        return 1
    print('\n== 收口态：全部不变量成立 ==')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
