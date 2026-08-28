# Quest 1336 验收记录

quest: 1336「Scouting for Demokritos / 埃拉库斯沙漠调查」
user acceptance confirmation: 用户于 2026-08-28 回复“验收完成，提交 并且将这个案例加入任务书，详细描述”，未限定分支；按规则视为整任务客户端主路径验收完成。
server launch mode: not captured（服务端由用户管理）
repository commit: f7ae6a706
working tree: dirty；本次修复提交仅包含 `src/main/resources/aion/data/static_data/quest_definition/quests/1336.xml` 和 `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest1336ScoutingForDemokritosRegressionTest.java`；其他修改和未跟踪文件保留。
Aion 5.8 client/data provenance: Aion 5.8 客户端；客户端文件 `QUEST_Q1336.html`（`<steps>` 定义 step 0 为 3 处调查，step 1 为向德莫克里托斯报告）；旧 handler `quest.eltnen._1336ScoutingForDemokritos`；本次未新增独立客户端包 SHA-256。
npc template/object: start / reward NPC template 204006（Demokritos / 德莫克里托斯）；sensory area 区域对象 LF2_SENSORY_AREA_Q1336_1/2/3_210020000；movie ID 43, 44, 45；runtime object ID not captured。
map/instance: world ID 210020000（Eltnen / 埃特南）；instance ID not captured；entry/reentry context not captured。

steps:
1. 前置和起点：任务 1336 的 ELYOS 主路径，接取对象为埃特南沙漠侦察队队长 Demokritos (204006)。
2. 交互与调查顺序：接受任务后前往 3 处感知区域触发动画（movie 43, 44, 45）；全部 3 处调查完成后状态自动转入 `REWARD`；返回德莫克里托斯处对话显示 `SELECT2`，点击报告侦察结果打开奖励窗口并完成领奖。用户确认全流程实测验收通过。
3. 登出/登录、重连、重启、死亡、重试或重复领取：XML 包含 `enter-world` 与 `QUEST_SELECT` 兼容历史 `START, var0=112` / `START, var0=1` 状态的平滑恢复迁移。

source state/status/vars: 3 处调查组合节点 `ab` / `ac` / `bc`，状态 `START`，`var0` 为 48/80/96；第 3 段动画结束后进入 `reward`，状态 `REWARD`，`var0=1`；领奖完成后进入 `complete`，状态 `COMPLETE`，`var0=1`。
action/page/button: NPC 204006 的 `QUEST_SELECT(31)` / `USE_OBJECT(0)` 显示 `SELECT2(1352)`；`SELECT_QUEST_REWARD(1009)` 打开 `SHOW_SELECT_QUEST_REWARD_WINDOW1(5)`；完成使用 `SELECTED_QUEST_REWARD1..5`。
expected response: 第 3 段动画结束后转到 `reward`，提交等级/可见性刷新（`LEVEL_AND_VISIBILITY_REFRESH`），客户端任务追踪激活 `step 1`“向德莫克里托斯报告”，NPC 头顶显示领奖问号；与 NPC 204006 对话进入 `SELECT2`，选择奖励结算金币 18370、经验 593431 及所选装备，并完成任务。
actual response: 用户确认“验收完成，提交 并且将这个案例加入任务书，详细描述”；3 处调查完毕后正常显示下一步向德莫克里托斯报告，任务顺利完成。

startup health: not captured；本次未启动或重启服务，无新增启动 WARN/ERROR 证据。
runtime logs: not captured；无稳定的时间窗口、角色、NPC、任务、世界或实例日志附件。
protocol trace: not captured；无稳定的 packet order、objectId、questId、page 或 action trace 附件。
screenshots/recordings and SHA-256: not captured；未形成可复用的稳定截图或录像附件。

acceptance status: ACCEPTED_NEW_PATTERN
matched Pattern: `MULTI_LOCATION_SCOUTING_FINAL_REWARD_TRANSITION`；匹配字段为多目标区域/动画侦察任务通过位掩码记录进度，全部目标达成后直接转入 `REWARD` 并在 after-commit 提交 `LEVEL_AND_VISIBILITY_REFRESH`，禁止插入 `START` 汇总中间节点；代表提交 `f7ae6a706`，代表测试 `Quest1336ScoutingForDemokritosRegressionTest#completesTheReportStateForEveryInvestigationOrder`。
remaining risks: 本次未采集运行日志、协议 trace 和稳定截图；未在本会话运行聚焦 Maven 编译、生产目录编译或白名单检查；重连、重复交互、死亡重试和实例重入未单独验证。
