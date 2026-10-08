# 19640 客户端验收记录（2026-10-08）

quest: 19640「[상의] 지역 몬스터 처치 / [上衣] 区域怪物击杀」（Elyos 50+ IMPORTANT；前置 19639；Inggison 杀 `LF4_A3_Rottentree_Green_52_An` / `LF4_A3_Foam_Wisp_52_n` ×10 → 与洛塔斯 799022 对话领奖；**接取对象 799022 ≠ 交付对象 Barus 798991**）
user acceptance confirmation: 用户回复「实机验证完成，提交」（2026-10-08），对本次「native 奖励窗动作按 questId 恢复面（`NativeUnpinnedRewardWindow`）+ 传送门类 AI 未认领任务动作分类应答（`unclaimedReply`）」修复；未限定分支或步骤 ⇒ 整任务验收。
server launch mode: IDEA（常驻进程；用户 10:28:19 冷启动加载含修复构建后复测）
repository commit: `f09d74cc5`（repair）
working tree: repair 提交后工作区仅余并行任务（链式接取边主题）的改动，与本任务修复面无关
Aion 5.8 client/data provenance: Aion 5.8 客户端（用户实机）；真端表 `src/main/resources/aion/data/static_data/quest/retail/data_driven_quest.xml:13803`（19640：`value0_acquire_=Elim_DF4_01`、`con_quest=19641`、`reward_npc_name=Barus`、击杀 `LF4_A3_Rottentree_Green_52_An, LF4_A3_Foam_Wisp_52_n 10`）；客户端页契约 `src/main/resources/aion/definitions/quest_dialog/client_dialog_contract.tsv:24117-24118`（19640 = 4762/10002）
npc template/object: 799022（`name_desc=Elim_DF4_01`、name=lothas、`ai=portal_dialog`，`src/main/resources/aion/data/static_data/npcs/npc_template_286321_800030.xml:71535`）接取/报告对象；798991（`name_desc=Barus`、`ai=aggressive`，同文件 `:71162`）交付对象；runtime object ID not captured（报障会话 targetObj=79221/79065）
map/instance: Inggison（`LF4_A3_*` 目标怪刷新图；接取/报告 NPC 799022）

steps:
1. 前置：Elyos 玩家持有 19640 且杀满 10 只（`LF4_A3_Rottentree_Green_52_An` / `LF4_A3_Foam_Wisp_52_n`），`状态=4`（REWARD）
2. 与洛塔斯 799022 对话被传送门类 AI 的开门页轴开出奖励窗（页 5 + questId=19640）
3. 点「选择奖励 1」（动作 8 = `SELECTED_QUEST_REWARD1`）→ **修复前**：全链无人认领 → AI 把动作 id 回显为页 → 客户端 `load fail! Quest_Q19640.html (HtmlPageId 8)`（报障现象，5 轮可复现）
4. 修复后复测（10:28:19 冷启动后）：
   a. 击杀推进：`log/adminaudit.log` 10:32:46 `spawn 215525*10`（215525 = `LF4_A3_Foam_Wisp_52_n`，19640 击杀目标）
   b. 在 799022 处点奖励窗确认 → 实际：领奖成功、无 load fail（用户确认「实机验证完成」）
   c. 行为互证：10:34:15 `spawn 215658*10`（215658 = `LF4_B_Fethlot_Highland_53_n`，19641 目标）、10:34:32/46 `spawn 215653/215642*10`（19642 目标）——真端 `con_quest` 链 19640→19641→19642 要求 19640 已完成，任务链连续推进

source state/status/vars: 19640 `REWARD`（击杀满 10）
action/page/button: 动作 8（`SELECTED_QUEST_REWARD1`，奖励窗第 1 项）；修复前 `SM_DIALOG_WINDOW targetObj=79221 questId=19640 下发页=8`；预期 `下发页=10 questId=0`（回选择对话页）
expected response: 引擎按 questId 恢复结算（`NativeUnpinnedRewardWindow` → `NativeReportRewardFlow`，真端元数据/档位/按钮声明各门继续生效）→ 奖励发放 + 完成 → 收尾回选择对话页（页 10、questId=0）
actual response: 用户实机确认通过（领奖成功、无 load fail）；包序 not captured（验收会话 QUEST-TRACE 未开启）
startup health: 10:28:19 冷启动（含修复构建），typed 正式任务 owner 装载正常（`已加载 typed 正式任务 owner：707`）；此前 10:26 会话的 `IncompatibleClassChangeError`（JRebel 热更 `NativeQuestStartPort` lambda 残留，属并行工作热更环境噪声）冷启动后未复现
runtime logs: 报障窗口 2026-10-08 09:52:56–09:54:14（`log/quests.log`：`下发页=5` → `动作=8` → `下发页=8` ×5），转述于 `.agents/summary/quest-19640-reward-window-recovery/evidence-19640-live-trace.txt`；验收窗口 10:28:19–10:34:46（`log/console.log` 启动/登录、`log/adminaudit.log` 刷怪记录）
protocol trace: 修复前 `SM_DIALOG_WINDOW questId=19640 下发页=8`（同上 5 轮）；修复后 not captured
screenshots/recordings and SHA-256: not captured

acceptance status: ACCEPTED_NEW_PATTERN
matched Pattern: `UNPINNED_REWARD_WINDOW_QUEST_ID_RECOVERY`（Playbook 8.60 新建，指纹表同批登记）；matched fields: 传送门类 AI 在**接取对象**开奖励窗（页 5 + questId）、奖励窗确认动作落在非交付对象上全链无人认领、AI 把动作 id 回显为页导致客户端 load fail、引擎按 questId + action 恢复结算并回选择对话页；differing fields: 无（首个实例）；representative commit `f09d74cc5`；`NativeUnpinnedRewardWindowTest#engineSettlesTheRewardWindowActionOnTheAcquireNpc`
remaining risks: ① 同型 28 行（SimpleTalk 18711..18735/28711..28735 接取 Silion 799531、IDEvent 80279/80282）未逐行实机；② `checkDialog` 开启面未收敛（REWARD → 页 5 仍对所有注册对象生效，未按交付对象过滤；真端选择器证据不完整）；③ 同型 AI 回显面（`Mighty_HeroAI2` 等 20+ 处「未认领回显动作 id」）未改，按批次登记待查；④ 验收会话 QUEST-TRACE 未开启，包序证据缺口
