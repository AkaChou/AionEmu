# 14047 客户端验收记录（2026-10-08）

quest: 14047「Chaining Memories / 束缚的记忆」（Elyos 45+ MISSION；副本 Azoturan Fortress 310100000；第 5 步击杀城堡 Boss 233877；领奖 NPC 278500）
user acceptance confirmation: 用户回复「实机验收成功」（2026-10-08），对本次「击杀步目标回归真端注册面」修复；未限定分支或步骤 ⇒ 整任务验收。
server launch mode: IDEA（常驻进程；用户重启加载含修复构建后复测）
repository commit: `463bf2891`（repair）
working tree: dirty during acceptance（3036 线的未提交改动与派生索引留在工作区，未随 repair 提交）；repair 落 `463bf2891`
Aion 5.8 client/data provenance: Aion 5.8 客户端（用户实机）；真端反编译源 `ScriptDLL64.c`（`FUN_180cb5920(..., "IDLF3_Castle_Lehpar_LehparIcaronixQ_45_Q_Ae", 0x36df)`，0x36df = 14047）、真端 AI pattern 表（`D2_FnA` 空 / `ND2_AhC_1` / `NLehpar_BhB`）、真端副本世界数据（233877 与 214598 两个领地）；证据 `.agents/summary/quest-14047-icaronix-kill-target/DIAGNOSIS.zh-CN.md`
npc template/object: 233877（`IDLF3_Castle_Lehpar_LehparIcaronixQ_45_Q_Ae`，城堡 Boss，模板 `ai` 修为 `aggressive`）；214598（`IDLF3CL_LehparIcaronixQ_45_Ah`，保持 `betrayer_icaronix`）；runtime object ID not captured
map/instance: 310100000（Azoturan Fortress）；instance ID not captured（复测时刻）

steps:
1. 前置：Elyos 角色持有 14047 且位于第 5 步（`START`，packed step = 5，任务书指向「消灭背叛者伊卡罗尼斯」）
2. 与 802052（Peitho）对话 `SETPRO11(10010)` → `SM_QUEST_ACTION 状态=3 步数=5` + `flight-teleport 72001` 飞抵城堡 Boss
3. 击杀 233877（复测路径：不再需要击杀任何「第二形态」）
4. 预期：`SM_QUEST_ACTION 任务=14047 状态=3 步数=6` + 电影 422 播放；任务书转入第 6 步（回 802051 报告 → 278500 领奖）
5. 实际：用户确认「实机验收成功」

source state/status/vars: 14047 `START` `var0: 5 -> 6`（`s5 → s6`）
action/page/button: 击杀事件（`QuestEvent.KillNpc(233877)`）触发 `s5→s6`；after-commit `sync-quest-state PACKET_ONLY` + `play-movie 422`
expected response: 状态=3/步数=6；事务内 `set-variable var0=6`；after-commit 顺序 = 状态同步 → 电影 422
actual response: 用户实机确认通过（具体包序/截图 not captured）
startup health: 用户重启加载修复构建后正常；未采集 typed quest engine 初始化/编译日志（not captured）
runtime logs: 报障窗口 2026-10-07 23:32:41–23:35:02（`log/quests.log`：进本 → `SETPRO10` 步数=4 → `SETPRO11` 步数=5 → GM `reset instance` → 回落步数=3，中间无击杀推进包），转述于 `.agents/summary/quest-14047-icaronix-kill-target/DIAGNOSIS.zh-CN.md`；验收时刻 not captured
protocol trace: 修复前无击杀推进包（同上）；验收时刻 not captured
screenshots/recordings and SHA-256: not captured

acceptance status: ACCEPTED_NEW_PATTERN
matched Pattern: `KILL_TARGET_BOUND_TO_RETAIL_REGISTRATION`（Playbook 8.58 新建，指纹表同批登记）；matched fields: 定义 kill-npc ∉ 真端注册面、空 pattern 的 Boss 被挂变身 AI、任务只因人造第二形态才推进；differing fields: 无（首个实例）；representative commit `463bf2891`；`Quest14047ClientDialogAlignmentTest#keepsOnlyTheQuestPeithoAndSpawnsTheIcarinoxKillTarget`、`#synchronizesCommittedProgressBeforeFlightsAndTheFinalMovie`
remaining risks: ① 3530/4526 的 SimpleHunt 计数链（214598→214599 变身）未实机核对（仍由 `Betrayer_IcaronixAI2` 承载，`ThresholdTransformDeathFallbackGateTest` 锁定）；② 客户端 Quest.pak 内的 step/怪物名单未直接取证；③ 死亡/放弃/重登旁路与电影 422 之后的领奖链（802051 → 278500）本次未逐步取证，依赖既有 278500 验收面。
