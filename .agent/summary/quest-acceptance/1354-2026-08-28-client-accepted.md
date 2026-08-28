# Quest 1354 验收记录

quest: 1354「Practical Aerobatics / 上级飞行术考试」
user acceptance confirmation: 用户于 2026-08-28 回复“验收并提交”，未限定分支；按规则视为整任务客户端主路径验收完成。
server launch mode: not captured（服务端由用户管理）
repository commit: ad0d1385a
working tree: dirty；本次修复提交仅包含 `src/main/resources/aion/data/static_data/quest_definition/quests/1354.xml` 和 `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest1354ClientDialogAlignmentTest.java`；其他修改和未跟踪文件保留。
Aion 5.8 client/data provenance: Aion 5.8 客户端；对话证据取自 `Data/Dialogs/Dialogs.pak` 中的 `QUEST_Q1354.html`，客户端任务元数据来自 `Data/China/Quest/quest.pak` 中的 `quest.xml`；本次未新增独立客户端包 SHA-256。
npc template/object: start/reward NPC template 203983（Marana / 马拉纳）；flying ring templates `ERACUS_TEMPLE_AIR_BOOSTER_1`..`7`；runtime object ID not captured。
map/instance: world ID 210020000（Eltnen / 埃特南）；instance ID not captured；entry/reentry context not captured。

steps:
1. 前置和起点：完成前置任务 1423（상급 비행술 강의 / Practical Flight）后，在埃特南埃拉库斯地下神殿与 NPC 203983（马拉纳）对话。
2. 交互顺序：接取任务后与 203983 对话选择 `SETPRO1` 确认开始考试，触发 120 秒（2分钟）倒计时并关闭对话；依次飞跃 7 个飞行环加速点（`BOOSTER_1 -> 4 -> 3 -> 6 -> 5 -> 2 -> 7`），穿过最后一个环（`BOOSTER_7`）后取消倒计时，返回与 203983 对话选择奖励并完成任务。用户确认该主路径验收通过。
3. 登出/登录、重连、重启、死亡、重试或重复领取：倒计时超时后状态重置回 `started`，可重新与 203983 对话重新挑战。

source state/status/vars: 初始接取节点 `started`，状态 `START`，`var0=0`；考试开始后进入 `t`（`var0=1`），依次通过环进入 `r2`..`r8`（`var0=2..8`）；最终向 203983 报告进入 `reward`（`REWARD`，`var0=8`）并完成进入 `complete`（`COMPLETE`，`var0=8`）。
action/page/button: NPC 203983 对话 `SETPRO1` 启动 120 秒任务倒计时；依次飞环触发 `pass-flying-ring`；最终 `QUEST_SELECT` 打开 `SELECT5`，`SELECT_QUEST_REWARD` 打开奖励窗口，完成任务。
expected response: `SETPRO1` 触发 `start-quest-timer seconds="120"`，向客户端下发 120 秒倒计时封包（`SM_QUEST_ACTION` action=4, timer=120），状态同步并关闭对话；`BOOSTER_7` 触发 `cancel-quest-timer`；领奖发放称号 14、经验 340413、物品 186000003。
actual response: 用户确认“验收并提交”；任务 2 分钟倒计时与飞环流程实机验证通过。

startup health: not captured；本次未启动或重启服务，也未运行 Maven/服务端启动检查；无新增启动 WARN/ERROR 证据。
runtime logs: not captured；无稳定的时间窗口、角色、NPC、任务、世界或实例日志附件。
protocol trace: not captured；无稳定的 packet order、objectId、questId、page 或 action trace 附件。
screenshots/recordings and SHA-256: not captured；未形成可复用的稳定截图或录像附件。

acceptance status: ACCEPTED_NEW_PATTERN
matched Pattern: `TIMED_QUEST_DURATION_ALIGNMENT`；匹配字段为客户端对话明确规定任务限制时间（2分钟/120秒），而旧代码/迁移 XML 设置了不一致的倒计时（5分钟/300秒），导致客户端倒计时与任务描述错位；代表提交 `ad0d1385a`，代表测试 `Quest1354ClientDialogAlignmentTest#locksFlightTimerAndRingProgressionContract`。
remaining risks: 本次未采集运行日志、协议 trace 和稳定截图；未在本会话运行全局 Maven 编译；重连、中途掉线和死亡重试未单独验证。
