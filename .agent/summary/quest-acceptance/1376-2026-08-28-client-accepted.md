# Quest 1376 验收记录

quest: 1376「A Mountain Of Trouble / 清扫库里乌鲁通道」
user acceptance confirmation: 用户于 2026-08-28 回复“验收通过 提交”，未限定分支；按规则视为整任务客户端主路径验收完成。
server launch mode: not captured（服务端由用户管理）
repository commit: 289307846
working tree: dirty；本次修复提交仅包含 `src/main/resources/aion/data/static_data/quest_definition/quests/1376.xml` 和 `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest1376ClientDialogAlignmentTest.java`；其他修改和未跟踪文件保留。
Aion 5.8 client/data provenance: Aion 5.8 客户端；仓库证据为 `docs/quest/client-dialog-mapping/legacy-quest-dialog-contracts.csv`、`docs/quest/client-dialog-mapping/quest-dialog-action-details.csv`、`docs/quest/client-dialog-mapping/quest-dialog-pages.csv` 及 `src/main/resources/aion/definitions/compact/quests/scripts/zz_retail_simple_quests.xml`；本次未新增独立客户端包 SHA-256。
npc template/object: start NPC template 203947；report/reward NPC template 203964；runtime object ID not captured；交互对象归属由客户端/旧任务合同确认 203947 接取、203964 报告和领奖。
map/instance: world ID / instance ID not captured；entry/reentry context not captured。

steps:
1. 前置和起点：任务 1376 的 ELYOS 主路径，接取对象为 NPC 203947；具体角色、坐标和服务端启动参数未采集。
2. 交互顺序：接受任务后完成 `k1` 至 `k7` 的击杀链；最终向 NPC 203964 报告，打开 SELECT2 报告页、奖励窗口并完成领奖。用户确认该主路径验收通过。
3. 登出/登录、重连、重启、死亡、重试或重复领取：not captured。

source state/status/vars: 最终击杀节点 `k7`，状态 `START`，`var0=7`；报告后进入 `reward`，状态 `REWARD`，保留 `var0=7`；完成后进入 `complete`，状态 `COMPLETE`，`var0=0`。
action/page/button: NPC 203964 的 `QUEST_SELECT(31)` 显示 `SELECT2(1352)`；`SELECT_QUEST_REWARD(1009)` 打开 `SHOW_SELECT_QUEST_REWARD_WINDOW1(5)`；完成使用 `SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD`。
expected response: `QUEST_SELECT` 保持 `k7`；`SELECT_QUEST_REWARD` 转到 `reward`，提交等级/可见性刷新后显示奖励窗口；选择奖励依次执行金币 33860（任务基础值）、经验 1244918（任务基础值）、物品 186000003 数量 2、完成任务 0，提交后依次刷新玩家属性、同步完成状态、显示 `SELECT_QUEST(10)`。
actual response: 用户确认“验收通过”；未提供独立的运行状态、日志、协议字段或稳定截图附件。

startup health: not captured；本次未启动或重启服务，也未运行 Maven/服务端启动检查；无新增启动 WARN/ERROR 证据。
runtime logs: not captured；无稳定的时间窗口、角色、NPC、任务、世界或实例日志附件。
protocol trace: not captured；无稳定的 packet order、objectId、questId、page 或 action trace 附件。
screenshots/recordings and SHA-256: not captured；未形成可复用的稳定截图或录像附件。

acceptance status: ACCEPTED_EXISTING_PATTERN
matched Pattern: `MULTI_NPC_HANDOFF_REWARD_OWNER`；匹配字段为接取 NPC 与最终报告/奖励 NPC 必须按客户端合同分离，且错误 NPC 不得保留报告或领奖路线；差异字段为任务 1376 直接从最终击杀节点 `k7` 报告，没有 1163 的工作物品和中间 SETPRO 交接节点。代表提交 `598deb98f9b3c90be0db7dd7e192d7b4a7ef6dac`，代表测试 `Quest1163ClientDialogAlignmentTest#followsTheRetailPotionHandoffAndRewardOwner`。
remaining risks: 本次未采集运行日志、协议 trace 和稳定截图；未在本会话运行聚焦 Maven 编译、生产目录编译或白名单检查；重连、重复交互、死亡重试和实例重入未单独验证。该修复沿用现有模式，因此不新增 Playbook/Pattern 条目。
