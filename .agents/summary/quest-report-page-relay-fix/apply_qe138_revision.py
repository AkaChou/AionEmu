#!/usr/bin/env python3
"""修订 QE-138：加入缺陷 S（报告页分型跳过被中继步占用的 SELECT2，2026-10-05）。"""
PATH = '.agents/memory-bank/patterns/quest-engine.md'
text = open(PATH, encoding='utf-8').read()

start = text.index('## [QE-138]')
end = text.index('\n## [QE-', start + 10)
card = text[start:end]

def replace_once(card, old, new):
    assert old in card, f"not found: {old[:60]!r}"
    return card.replace(old, new, 1)

# ① last_verified
card = replace_once(card, 'last_verified: 2026-10-04', 'last_verified: 2026-10-05')

# ② symptom 追加 ⑤（行尾）
card = replace_once(card,
    '（16:27 quests.log 三连「动作=39」全无 S->C 回包）',
    '（16:27 quests.log 三连「动作=39」全无 S->C 回包）；⑤ 真机 1118 交付 NPC（Melpone=203079，双页任务：select2=Kustanon 中继树 / select5=报告页）31 → 1352（错发中继树页）→ 1353 → 10000 落空关窗——玩家「和 NPC 对话没有反应」（2026-10-05 08:53 quests.log；根因=报告页分型被中继页 1352 占位）')

# ③ root_cause 追加（行尾）
card = replace_once(card,
    '（1870/2870 归 XML 保留，其余 8 件为 10000/10001 结果页型 Shape B，非零步交付面所辖，未接线=悬案）',
    '（1870/2870 归 XML 保留，其余 8 件为 10000/10001 结果页型 Shape B，非零步交付面所辖，未接线=悬案）。**SELECT2(1352) 同 id 语义互斥（2026-10-05 缺陷 S）**：无中继任务的 select2 = 报告页（按钮 SELECT_QUEST_REWARD/39，1102 型）；有中继任务的 select2 = 中继步 1 页（按钮 SELECT2_1 翻页或 SETPRO1，1118 型）——报告页选择必须排除被中继步占用的页（中继步 1..3 页恒为 SELECT2/SELECT3/SELECT4，即 RELAY_STEP_PAGES 1352/1693/2034）。233 件「中继 + select2&select5 双页」客户端页普查零反例（select2 按钮全为翻页/步进、select5 按钮全为报告动作）；反向「有中继 + 仅 select2」0 件（跳过不会空落）。三方印证：1118 客户端 HTML（select2「询问飞行移动的方法」/ select5「拿出药膏」）+ 退役 XML 1118（203079 QUEST_SELECT→SELECT5）+ 退役 XML 1971（NPC_REPORT npc-id=203812 page=SELECT5 显式写法）')

# ④ fix_or_guardrail 第 1 条更新为新签名
card = replace_once(card,
    '1. 31 → `QuestDialogContract.reportConfirmPage(questId)`（按 SELECT2/SELECT5/DEFAULT_SUCCESS 声明序取页，-1=未声明 → 降级一步直达保旧行为）',
    '1. 31 → `QuestDialogContract.reportConfirmPage(questId, relaySteps)`——按 SELECT2/SELECT5/DEFAULT_SUCCESS 声明序取页，**relaySteps>=1 时跳过 SELECT2 候选**（被中继步 1 占用）；relaySteps 由各调用方传本族中继步数：SimpleTalk=`relayCount(questId)`、SimpleUseItem/SimpleCollectItem=`relayNpcs(questId).size()`、DataDriven 零步交付面恒 0（该面只服务零步 Talk 行）；-1=未声明 → 降级一步直达保旧行为')

# ⑤ evidence 追加
card = replace_once(card,
    '（缺陷 H/M）与 retired-xml-behavior-census.md',
    '（缺陷 H/M）与 retired-xml-behavior-census.md; 缺陷 S 对账（2026-10-05）：.agents/summary/quest-report-page-relay-fix/reconcile_report_page.py（六族数据 × 客户端页全量交叉：233 件影响面逐件按钮对照 + 0 件空落）；1118 客户端任务页 QUEST_Q1118.html 全文（select2=SELECT2_1 翻页 / select2_1=SETPRO1 / select5=SELECT_QUEST_REWARD「拿出药膏」）；退役 XML 1118 与 1971（git show 4ede058c0~1 逐件取回）')

# ⑥ validation 追加
card = replace_once(card,
    '真机 1103 报告链复测待用户执行',
    '真机 1103 报告链复测待用户执行; 2026-10-05 缺陷 S 批次（IDEA MCP runner）：SimpleTalk 族门 14/14（新增 reportPageSkipsTheRelayConsumedSelect2：1131 双页 31→2375）、SimpleUseItem 11/11、SimpleCollectItem 16/16、SimpleItemPlay 15/15、ItemPlayRowInventory 5/5（evidenceFacesStayFrozen 显式改表：5 组键随真端名组表解析面扩展已解）、DataDrivenNativeRuntime 26/26、DataDrivenNativeContract 7/7、DialogServiceQuestDialog 8/8、TalkRowAlignment 5/5')

# ⑦ first_check 追加 ⑥
card = replace_once(card,
    '「未持满时下发哪个声明失败页（checkFailPage：select6=2716）」？',
    '「未持满时下发哪个声明失败页（checkFailPage：select6=2716）」？⑥ 该任务有没有中继步（talk/relay 链）？有 ⇒ SELECT2(1352) 是中继步页（按钮 SETPRO1/SELECT2_1 翻页），报告页须从 SELECT5 起选（`reportConfirmPage(q, relaySteps>=1)`）；没有才允许 SELECT2 分型（此时其按钮须为报告动作）？')

# ⑧ keywords 追加
card = replace_once(card,
    'reportConfirmPage、REPORT_TWO_STEP_CONFIRM、QE-138',
    'reportConfirmPage、中继占用页、relaySteps、双页任务、select2 中继树、reportPageSkipsTheRelayConsumedSelect2、REPORT_TWO_STEP_CONFIRM、QE-138')

# ⑨ boundaries 追加 ⑦
card = replace_once(card,
    '⑥ 39 在其它状态的动作（如 canonical 30217 的「1693 页确认按钮 39 在 REWARD 态开奖励窗」型）不在本 Pattern 的表车道两族范围',
    '⑥ 39 在其它状态的动作（如 canonical 30217 的「1693 页确认按钮 39 在 REWARD 态开奖励窗」型）不在本 Pattern 的表车道两族范围；⑦ SELECT2(1352) 语义互斥判别看按钮动作与 talk 链（无中继=报告页 / 有中继=中继步 1 页），不存在第三种混用——真有混用行按 fail-closed 处置（跳过候选后无声明取 -1 一步直达）')

# ⑩ 正文三行
card = replace_once(card,
    '- **判定规则**：报告 NPC 上 31 = 只发客户端声明确认页（1352/2375/10002），确认动作推进 REWARD + 奖励窗——直翻型 1009 / 检查型 39（未持满 → checkFailPage 的 select6=2716）；-1/26 永不推进。',
    '- **判定规则**：报告 NPC 上 31 = 只发客户端声明确认页（1352/2375/10002；**有中继步的任务跳过 1352——它被中继步 1 占用**），确认动作推进 REWARD + 奖励窗——直翻型 1009 / 检查型 39（未持满 → checkFailPage 的 select6=2716）；-1/26 永不推进。')

card = replace_once(card,
    '- **反漂移**：别对任何动作无条件推进（跳步）；别在第一步扣物品；确认页以客户端契约声明为准、退役 XML 作交叉印证；别把 39 当未知动作吞掉（两族 675+85 件报告页的主按钮）。',
    '- **反漂移**：别对任何动作无条件推进（跳步）；别在第一步扣物品；确认页以客户端契约声明为准、退役 XML 作交叉印证；别把 39 当未知动作吞掉（两族 675+85 件报告页的主按钮）；别把「有中继任务的 select2」当报告页发——玩家会在中继树页上点出 10000 落空关窗（1118 没反应的原型）。')

open(PATH, 'w', encoding='utf-8').write(text[:start] + card + text[end:])
print('QE-138 revised OK')
