# 任务 30600 客户端验收记录（SerialHunt 简报对话面）

quest: 30600「[Group] Fight Of The Navigators / 萨德哈德雷得奇安动线引导」（天族，56-60；简报 NPC = Linocus 800324，接取/交付 = Hejitor 800325）。

user acceptance confirmation: 用户 2026-10-08 原话「实机验证成功」；验收面 = 本次修复目标「与 800324 对话，点击任务没有下一步」的死循环——修复后点任务行开出简报页、结束对话关窗刷新（详见 steps）。

server launch mode: IDEA（常驻 Spring Boot 进程，classpath=target/classes）；服务端重启由用户管理（本次修复需重启后生效，用户已重启）。

repository commit: 随本批次提交（`fix(quest): 30600/SerialHunt 简报对话面修复`）；记录见 `.agents/summary/quest-30600-serialhunt-briefing/`。

working tree: dirty；并行会话的 npc-walker / quest-chain 批次及 memory-bank 改动保留未暂存，未纳入本任务提交。

Aion 5.8 client/data provenance: 客户端 pak 侧本次无变更；30600 客户对话契约 = `client_dialog_contract.tsv`（30600：1011/1352/2375——本次使用 select2=1352）；同批修复的客户端对话页（Linocus.html）见 `.agents/summary/client-dialog-html-repair/`。

npc template/object: 简报 NPC Linocus = 800324（运行时 objectId=76362，trace 捕获）；接取/交付 Hejitor = 800325；简报守卫位 = var5（bit30, 1<<30）。

map/instance: 未捕获（本次不涉地图变更）。

steps:

1. 接取 30600（本次以 GM 命令置简报复议态：`//quest set 30600 START 1073741824`，var5=1）。
2. 与 800324 对话：页 10 任务列表 → 点「30600」任务行（动作 31）→ **开出简报页 select2（1352）**（修复前：循环重发页 10、无下一步）。
3. 简报页点「结束对话」（SETPRO1=10000）→ 关窗 + `SM_QUEST_ACTION(30600, START, 0)` → 任务书刷新（简报行完成、击杀行亮起）。
4. 击杀 Named 指挥官/舰长 → 向 Hejitor 800325 报告领奖——本轮未逐步复测（用户确认修复面验收；整链为既有 native 计数逻辑，族门测试覆盖）。
5. 旧解码（页 10 循环）路径不再出现；重登/死亡等未复测。

source state/status/vars: 简报复议 `START / var5(bit30)=1` → 简报后 `START / 0`（briefed 投影）。

action/page/button: 31（QUEST_SELECT）→ 页 1352（select2，带 questId）；26/-1 → 页 10（通用列表）；10000（SETPRO1）→ 清位 + 关窗（真端行 0b close-dialog）。

expected response: 点任务行一次即开简报页；「结束对话」一次点击关窗并刷新任务书；无死循环、无未声明页下发。

actual response: 用户确认「实机验证成功」（2026-10-08）；逐步 trace 以修复前循环 trace 为对照（用户提供），修复后 trace/截图 not captured。

startup health: not captured（服务端由用户重启管理）；离线门禁 `SimpleSerialHuntNativeFamilyGateTest` 5/5（2026-10-08，IDEA MCP）。

runtime logs: 修复前循环 trace（用户提供，见 summary README）；修复后 not captured。

protocol trace: 修复前 SM_DIALOG_WINDOW(页10)/CM_DIALOG_SELECT(31) 循环 captured（用户粘贴）；修复后 not captured。

screenshots/recordings and SHA-256: not captured。

acceptance status: ACCEPTED_EXISTING_PATTERN

matched Pattern: `RELAY_DIALOG_OPEN_AND_SUBPAGE_ECHO`（QE-141）；matched fields = QUEST_SELECT(31) → 该任务页（带 questId）、SETPRO(10000) → 推进 + 关窗（零发页）、打开类动作维持通用页；differing fields = 简报面**清位对象**为守卫位 var5（bit30）而非步号 var0，页面为简报页 select2（1352）；representative commit = `d12e4236e`（14120）/ `f84393452`（3058）；本次测试 = `SimpleSerialHuntNativeFamilyGateTest#briefingGateEnforcesTalkBeforeKills`。

remaining risks: ① 9622（多简报 NPC talk_npc1-3）逐人多段流程未实机复测（同契约含 1352）；② 30610（魔族版）未实机复测；③ 击杀死/领奖段本轮未逐步复测（族门已锁）；④ QE-141 卡片边界扩展待并入 memory-bank（并行线占用，见 summary README 待办）。
