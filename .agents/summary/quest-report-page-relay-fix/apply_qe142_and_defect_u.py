#!/usr/bin/env python3
"""QE-142 新建（真端 ScriptDLL 对话处理器取证法）+ DIAGNOSIS 缺陷 U 段。"""
Q_PATH = '.agents/memory-bank/patterns/quest-engine.md'
text = open(Q_PATH, encoding='utf-8').read()
assert text.rstrip().endswith('别把子页回发与 movie 判成互斥。'), 'file tail changed'

card = '''

## [QE-142] 一百四十二、真端对话处理器取证法：ScriptDLL 每任务 FUN_ 函数体（0x188 发页 / 0x4b8 关窗 / 0x5d8 刷新）——退役 XML 的 after-commit 可能被翻译夸大（1002 的 SETPRO6 实为无页） (SCRIPTDLL_DIALOG_HANDLER_FORENSICS)

<!-- pattern-metadata
status: CONFIRMED
scope: 对话「推进后/after-commit」行为的仲裁取证（发页/关窗/刷新）；XML 车道（retention）已实证 1002；表车道 helper（cab520/cabb10）同法可查但未取证
first_seen: 2026-10-05
last_verified: 2026-10-05
symptom: ① 用户实机 1002（2026-10-05 09:30）：点「结束对话。」（select5_2 按钮＝SETPRO6=10005）后弹多余「任务列表页（页 10）」，用户质疑"应直接关窗/不该再出现页面"；② 退役 XML 与旧引擎结论冲突的仲裁需求（用户裁定「xml 和旧引擎都可能不对，看真端」）
root_cause: 退役 XML 是翻译/演绎产物——真端的「发页/关窗/刷新」是每步**显式 vtable 调用**，不会凭空出现。1002 的 s14→reward 在真端（FUN_180f90280）为 `npc+0x100(完成/推进) + mgr+0x5d8(刷新)`，**无发页**；XML 却写 SHOW_SELECTION_PAGE(页 10)=翻译夸大。9/28 旧引擎「推进后零页」（QE-141 symptom③）与真端一致——此例旧引擎对、XML 错。
fix_or_guardrail: **取证法**（quest id→十六进制→真端反编译 ScriptDLL64.c）：① grep `0x<hexId>`——`FUN_180cb5920(&DAT, L"<注册名>", 0x<hexId>)`＝NPC 注册、`FUN_180cb3070(_,_,0x<hexId>,3,<step>,<arg>)`＝步注册；② 每任务函数模式：`if (state==0||10) mgr+0x188(npc, <pageHex>, questId)`＝打开发页；`action==0x2710+n`（＝SETPRO(n+1)，0x2714=SETPRO5=10004、0x2715=SETPRO6=10005…）分支；③ **vtable 词典（本批归纳）**：`mgr+0x188(_,page,quest)`＝发对话页；`mgr+0x4b8(_,_)`＝关窗；`mgr+0x5d8(_,_)`＝刷新（状态/可见性，对应 sync-quest-state）；`npc+0x100(quest,0,0)`＝完成/推进；`npc+0xf0/f8(quest,step)`＝SetProgress；④ 修改定义前先跑函数体对照——after-commit 里的「页/关窗」必须在函数体可见，否则删除（翻译夸大）。
evidence: 真端反编译源 ScriptDLL64.c（ScriptDLL64 反编译工作区）第 2545824-2545852 行（FUN_180f90280＝1002 的 s14 树：打开→发 2461；动作 2462 原样回发；SETPRO6(0x2715)→0x100+0x5d8，无页无关窗）；第 2545594-2545635 行（FUN_180f8fbf0＝1002 的 s13 树：打开→发 2375；2376/2377 回发；SETPRO5(0x2714)→0x5d8+0x220+0x330+0xf8(0x3ea,20)）；第 2545860-2545917 行（FUN_180f90430＝任务 0x7d2：SETPRO7(0x2716)→0x100+0x5d8+**0x4b8**（关窗显式存在——对照证明关窗不凭空出现））；第 2527748-2527764 行（FUN_180f72210＝0x36c9：SETPRO6→0x100+0x5d8 同型）；src/main/resources/aion/data/static_data/quest/definitions/quests/1002.xml（s14→reward 修正删页）；log/console.log 2026-10-05 09:30（用户实机）；QE-141（9/28 旧引擎「推进后零页」）
validation: 2026-10-05：生产目录全量编译绿（PRODUCTION_COMPILE_OK=707 / 失败 0——1002.xml 修改合法）；待用户实机复测 1002（点「结束对话。」后无多余页、客户端随状态包自行收尾）；待照此复核：XML 车道全体 SETPROn→SHOW_SELECTION_PAGE 行 + 表车道 cabb10 的推进后段（QE-141 的「回页 10」结论可能需回调为「仅状态包+页 10 仅在真端显式发时」）
superseded_by: none
boundaries: ① 本批只修 1002 的 s14→reward；s13→s20 的 teleport+close 保留（传送主导、玩家无感；真端 0x220/0x330 疑为树演出未读透）；② 表车道（SimpleTalk cab520/cabb10）推进后行为未取证——vtable 词典适用但 helper 另一套；③ 方法论基石：客户端收与真端相同的包 ⇒ 表现一致（不再靠客户端重发反推）；④ vtable 偏移语义从四处用法归纳，用前尽量多读一例确认
see_also: [QE-141], [QE-140], [QE-138], [QE-137]
first_check: 对话「推进后/after-commit」争议时先答：① 任务 id 十六进制是多少（grep 真端反编译 ScriptDLL64.c）？② 该任务函数的「打开/动作分支」读了吗（0x188 发页在哪几处、0x4b8 有无）？③ XML/旧引擎结论与函数体冲突时以函数体为准（XML 可能是翻译夸大）；④ 改完跑生产目录全量编译 + 用户实机
keywords: ScriptDLL、反编译、0x188、0x4b8、0x5d8、vtable词典、FUN_180cb3070、FUN_180cb5920、0x2714、0x2715、SETPRO、1002、结束对话、多余页、翻译夸大、真端取证、SCRIPTDLL_DIALOG_HANDLER_FORENSICS
-->

- **判定规则**：对话 after-commit 的「发页/关窗/刷新」以真端任务函数的显式 vtable 调用为准（0x188=发页 / 0x4b8=关窗 / 0x5d8=刷新）；XML 缺这些调用即为翻译夸大，删；旧引擎与真端一致时旧引擎为对。
- **安全网**：修改定义前必须先在真端反编译里读到对应函数体；改完生产目录全量编译 + 用户实机复测双过。
- **反漂移**：别拿退役 XML 直接当行为规范（它可能夸大）；别靠「客户端重发」反推正确包序列（客户端行为是结果不是依据——发与真端相同的包即得相同表现）。
'''
open(Q_PATH, 'w', encoding='utf-8').write(text.rstrip() + card + '\n')

D_PATH = '.agents/summary/quest-accept-silent-refusal-20261003/DIAGNOSIS.zh-CN.md'
dtext = open(D_PATH, encoding='utf-8').read()
anchor = '## 修复落地（2026-10-04，缺陷 Q：领奖动作 8..23 一刀切残留——1107 奖励窗点确定循环）'
assert anchor in dtext
u = '''### 缺陷 U（2026-10-05）：任务 1002「结束对话」后弹多余页面——真端取证裁定「退役 XML 翻译夸大」

- 用户报告（08:53 后 09:30 时段）：1002 教程链，点 2461 页（select5_2）的「结束对话。」（SETPRO6=10005）
  后收到「状态4 + 页 10（任务列表）」——"任务结束后点击结束对话，还会出现一个页面，里面也有结束
  对话选项，应该点击结束对话就关闭对话窗口才对"；并质疑"退役 XML 和旧引擎可能不对，真端怎么做"。
- 判定：**用户正确，退役 XML 的 after-commit 是翻译夸大**。真端反编译取证（ScriptDLL64.c：
  FUN_180f90280＝1002 的 s14 树函数）：`SETPRO6(0x2715) → npc+0x100(完成/推进) + mgr+0x5d8(刷新)`，
  **无发页、无关窗**；XML 却写 `SHOW_SELECTION_PAGE SELECT_QUEST`（页 10）。旁证链：9/28 旧引擎
  「推进后零页」（QE-141 symptom③）与真端一致；对照函数 FUN_180f90430（任务 0x7d2 的 SETPRO7）
  显式带 `0x4b8`（关窗）——证明真端的"页/关窗"逐处显式、不会凭空出现。
- 修复：1002.xml 的 `s14→reward`（SETPRO6）after-commit 删 `SHOW_SELECTION_PAGE`，只留
  `sync-quest-state`（对应真端 0x5d8）；同型嫌疑（s13→s20 的 close、其他 SETPROn→SELECTION 行、
  表车道 cabb10 推进后段）登记待普查，不盲改。
- 验证（2026-10-05 IDEA MCP runner）：生产目录全量编译绿（707 OK/0 失败）；**待用户实机复测**。
- 方法沉淀：QE-142（真端对话处理器取证法 + vtable 词典 0x188/0x4b8/0x5d8/0x100/0xf8）。

'''
open(D_PATH, 'w', encoding='utf-8').write(dtext.replace(anchor, u + anchor, 1))
print('QE-142 + defect U OK')
