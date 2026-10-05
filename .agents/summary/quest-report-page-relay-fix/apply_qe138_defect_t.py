#!/usr/bin/env python3
"""QE-138 增补缺陷 T（页条件=报告步已到，物品门只在确认动作）。"""
PATH = '.agents/memory-bank/patterns/quest-engine.md'
text = open(PATH, encoding='utf-8').read()
start = text.index('## [QE-138]')
end = text.index('\n## [QE-', start + 10)
card = text[start:end]

def replace_once(card, old, new):
    assert old in card, f"not found: {old[:60]!r}"
    return card.replace(old, new, 1)

# ① symptom 追加 ⑥
card = replace_once(card,
    '（根因=报告页分型被中继页 1352 占位）',
    '（根因=报告页分型被中继页 1352 占位）；⑥ 真机 1126（2026-10-05 09:12 用户提问「是否正确」）：已接取未集齐在交付 NPC 点任务行 → `questId=0 页=10`（体感「没反应」）——物品就绪门被误加到页下发上（缺陷 T）')

# ② fix 第 1 条补页条件
card = replace_once(card,
    'DataDriven 零步交付面恒 0（该面只服务零步 Talk 行）；-1=未声明 → 降级一步直达保旧行为',
    'DataDriven 零步交付面恒 0（该面只服务零步 Talk 行）；-1=未声明 → 降级一步直达保旧行为。**页只随报告步已到下发、物品门不计入页条件**（缺陷 T，2026-10-05）：Talk=`vars>=relayCount` / UseItem=`relayComplete` / CollectItem=`talkChainComplete`；退役 XML 1126/1137/80482：started 态 31→SELECT5/报告页 无 conditions——未集齐照发页，就绪分叉在 39/1009 确认动作上')

# ③ validation 追加
card = replace_once(card,
    'DialogServiceQuestDialog 8/8、TalkRowAlignment 5/5',
    'DialogServiceQuestDialog 8/8、TalkRowAlignment 5/5; 2026-10-05 缺陷 T：Talk 14/14、UseItem 11/11、CollectItem 16/16、ItemPlay 15/15 全绿（三族门各加「未持门点 31 → 报告页 2375 + 零推进零扣物」断言）')

# ④ boundaries 追加 ⑧
card = replace_once(card,
    '真有混用行按 fail-closed 处置（跳过候选后无声明取 -1 一步直达）',
    '真有混用行按 fail-closed 处置（跳过候选后无声明取 -1 一步直达）；⑧ 页下发的唯一门=报告步已到；物品/计数门只在确认动作（39/1009）分叉——26/-1 与 1009 的未就绪仍走页 10 兜底（1126 XML started 态 26/-1 无转换；unaccepted 态 FINISH_DIALOG→SELECT_QUEST 页 10 有 XML 背书）')

# ⑤ 正文判定规则补
card = replace_once(card,
    '（1352/2375/10002；**有中继步的任务跳过 1352——它被中继步 1 占用**）',
    '（1352/2375/10002；**有中继步的任务跳过 1352——它被中继步 1 占用**；**只问报告步已到、不问物品就绪**——未集齐照发页）')

# ⑥ keywords 补（限长内）
card = replace_once(card,
    '中继占用页、relaySteps、双页任务、REPORT_TWO_STEP_CONFIRM、QE-138',
    '中继占用页、relaySteps、双页任务、报告步已到、页条件、REPORT_TWO_STEP_CONFIRM、QE-138')

import re
kw = re.search(r'^keywords: (.*)$', card, re.M).group(1)
assert len(kw) <= 300, len(kw)

open(PATH, 'w', encoding='utf-8').write(text[:start] + card + text[end:])
print(f'QE-138 defect-T revision OK, keywords len={len(kw)}')
