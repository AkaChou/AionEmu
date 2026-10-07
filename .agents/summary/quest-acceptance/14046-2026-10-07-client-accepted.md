# 14046 客户端验收记录（2026-10-07）

quest: 14046「Piecing the Memory / 记忆的碎片2」（Elyos 45+ MISSION；SET_SUCCEED 交接 NPC Aithra 203754）
user acceptance confirmation: 用户先报「和 npc 203754 对话后，游戏客户端按 J 的任务书列表步骤为空了」（附任务书步骤空白截图与 `SM_QUEST_ACTION 任务=14046 状态=4 步数=7` 的 trace）→ 修复（reward 投影 7 -> 6 + 自愈边反转）后回复「修复成功」（2026-10-07）；未限定分支或步骤 ⇒ 整任务验收
server launch mode: IDEA（常驻进程；用户重启加载含修复构建后复测）
repository commit: `cec4ef885`（repair；验收时工作区含该修复的未提交版本，其后按本记录落库）
working tree: dirty during acceptance（56 任务 QE-054 全量收口 XML/测试 + 审计摘要待提交）；repair 落 `cec4ef885`
Aion 5.8 client/data provenance: Aion 5.8 客户端（用户实机）；退役前 XML `quests/14046.xml`（内容见 git 历史）；真端 `ScriptDLL64.c` 14046 SET_SUCCEED 处理器带 `step==6` 守卫（`FUN_180caf3c0(0x36de, …, 6)`）且全脚本 `SetProgress` 常量集合只有 {3,5}（另有数据驱动 4）；证据 `.agents/summary/quest-step-axis-fullscan/README.zh-CN.md` §1/§2
npc template/object: 203754（Aithra；SET_SUCCEED 交接 NPC）；runtime object ID not captured
map/instance: 普通世界；runtime world/instance ID not captured

steps:
1. 前置：Elyos 角色（45+）持有 14046（进行中；任务书 8 行）
2. 与 203754（Aithra）对话 → 客户端发 `CM_DIALOG_SELECT npcId=203754 动作=10255`（HACTION_SET_SUCCEED）
3. 预期：任务进入 REWARD，任务书按 `[%N]` 门槛正常显示各步骤行（reward 投影 = 6，8 行任务书）；`SM_QUEST_ACTION 状态=4 步数=6`
4. 修复前实测：`SM_QUEST_ACTION 状态=4 步数=7`（领奖行批次抬升值）→ 客户端 REWARD 态显示位再进一位到第 9 行 ⇒ 全部行门槛不成立 ⇒ 任务书步骤整块空白（只剩「指令」行与奖励区）
5. 修复后实测：用户确认「修复成功」——步骤区恢复正常显示

source state/status/vars: 14046 `REWARD`（packed step 7 -> 6）；自愈边 `REWARD/var0=7 -> 6` 承担旧档（已存 7 的存档进世界时回滚到权威值）
action/page/button: `CM_DIALOG_SELECT npcId=203754 动作=10255`（HACTION_SET_SUCCEED）
expected response: 领奖态任务书 8 行全部可显示（reward=6；客户端 REWARD 态按自身 `[%N]` 门槛显示报告行）；无行门槛越界
actual response: 用户实机确认「修复成功」（与预期一致：步骤区恢复、领奖流程正常）
startup health: 用户重启加载修复构建后正常；未见 typed quest engine 初始化/编译异常上报（未逐项采集）
runtime logs: 报障窗口（2026-10-07 22:04:47）两条 `[QUEST-TRACE]`——`CM_DIALOG_SELECT npcId=203754 动作=10255`、`SM_QUEST_ACTION 任务=14046 状态=4 步数=7`，见 `.agents/summary/quest-step-axis-fullscan/README.zh-CN.md` §1；验收时刻 not captured
protocol trace: 修复前 `动作=10255 -> SM_QUEST_ACTION 步数=7`（客户端步骤空白）；验收时刻 not captured
screenshots/recordings and SHA-256: 报障截图（任务书步骤空白）在 README.zh-CN.md §1 转述；验收时刻 not captured

acceptance status: ACCEPTED_EXISTING_PATTERN
matched Pattern: `STEP_AXIS_RETAIL_SETPROGRESS_AUTHORITY`（Playbook 8.54/8.55 建立口径、8.57 全库收口）；matched fields: 领奖态 packed step 被末行索引推断批次抬升、自愈边方向反转、真端 `0x100` 状态推进不写轴；differing fields: 无（同模式的新批次实例）；representative commit `cec4ef885`；`RewardRowProjectionRegressionTest#rewardProjectionKeepsTheRetailProgressValue`（56 行参数化）、`#batchCorruptedSavesRollBackToTheRetailValue`
remaining risks: 其余 55 任务实机 PENDING_CLIENT；表车道 5 行（15613/25023/25606/80020/80021，遗留 A）与 NO_RETAIL 存量（遗留 C）未复验；36500/46500 族 17 个（遗留 B）与 4 组待定案（10525/20525、10101/20101、14014、14043，遗留 D）不在本批；死亡/放弃/重登旁路未逐条验证
