#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P1 wave-2：SimpleItemPlay 逐任务裁定表生成（数据源 = 族门禁逐行结果）。

输入：p1-itemplay-family-gate.tsv（RetailSimpleItemPlayGateTest -Dretail.itemPlay.equivOut 导出）
  EQUIVALENT      → ADOPT_RETAIL / ITEM_PLAY_CANONICAL（合成定义与退役前生产 XML IR 逐字等价）
  <稳定拒绝码>     → KEEP_XML / <稳定拒绝码>（真端表列形状无法在本波合成器表达）
  NO_PRODUCTION   → 跳过（真端独有开发/测试任务，本服无生产对应，不进裁定）

输出：p1-itemplay-decisions.tsv（quest_id / verdict / basis / axes / evidence；
与 p3b-use-item-decisions.tsv 同格式，供 build_retention_list.py 消费）。
断言：6 ADOPT + 9 KEEP，UNRESOLVED/未知码 0，否则不写盘。
"""
from pathlib import Path

HERE = Path(__file__).resolve().parent
SRC = HERE / 'p1-itemplay-family-gate.tsv'
OUT = HERE / 'p1-itemplay-decisions.tsv'

WAVE_ONE = {13704, 13708, 19048, 23704, 23708, 29048}
KEEP_EVIDENCE = {
    'RETAIL_ACQUIRE_NPC_UNRESOLVED': 'acquired_npc_name 未解（HousingManager_Li/Da、NPC_event_devasday_shugo 不在 npc_template 集）',
    'RETAIL_ACQUIRE_NPC_SENTINEL': 'acquired_npc_name=_faction_ 势力哨兵（每日任务按阵营接取，表列无运行时阵营语义）',
    'RETAIL_TALK_CHAIN': 'talk_npc1/talk_npc2 对话链 + give_item2/remove_item2 中途换道具（链式合成未排期）',
    'RETAIL_ADVANCE_UNEXPRESSED': '行无 use_item_name：推进事件不在族表（80255/80256 字符烟花，set_succeed 轴）',
}

rows = []
for line in SRC.read_text(encoding='utf-8').splitlines():
    if line.startswith('#') or not line.strip():
        continue
    parts = line.split('\t')
    quest_id, result = int(parts[0]), parts[1]
    if result == 'NO_PRODUCTION':
        continue
    if result == 'EQUIVALENT':
        rows.append((quest_id, 'ADOPT_RETAIL', 'ITEM_PLAY_CANONICAL', '-',
            'IR-equivalent to production XML (RetailSimpleItemPlayGateTest wave-1); '
            'fingerprint=%s (p1-itemplay-family-gate.tsv)' % parts[2]))
        continue
    assert result in KEEP_EVIDENCE, '未知结果码 %s（quest %d）— 先更新门禁白名单与本脚本' % (result, quest_id)
    rows.append((quest_id, 'KEEP_XML', result, '-', KEEP_EVIDENCE[result]))

adopt = sorted(q for q, v, *_ in rows if v == 'ADOPT_RETAIL')
keep = sorted(q for q, v, *_ in rows if v == 'KEEP_XML')
assert adopt == sorted(WAVE_ONE), 'ADOPT 面 != wave-1 6 行：%s' % adopt
assert len(rows) == 15, '生产族应 15 行，实得 %d' % len(rows)

text = ('# P1 SimpleItemPlay 逐任务裁定（真端优先；由 p1_build_itemplay_decisions.py 生成）\n'
    '# quest_id\tverdict\tbasis\taxes\tevidence\n')
for quest_id, verdict, basis, axes, evidence in sorted(rows):
    text += '%d\t%s\t%s\t%s\t%s\n' % (quest_id, verdict, basis, axes, evidence)
OUT.write_text(text, encoding='utf-8')
print('裁定表 -> %s（ADOPT %d + KEEP %d）' % (OUT, len(adopt), len(keep)))
