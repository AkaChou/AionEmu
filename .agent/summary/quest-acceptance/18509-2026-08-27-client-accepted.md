# 任务 18509 客户端验收记录

quest: 18509；相关对照任务 28509 及 18505 修复已包含在本次提交

user acceptance confirmation: 用户于 2026-08-27 回复“验证完成 / 提交”，未限定分支或步骤；按规则视为本次 18509 完整任务流程验收完成。

server launch mode: not captured

repository commit: `c6166412dcbb5b375efc7ee3173cee4eda990c40` (`fix(quest): repair 18509 Haramel dialog routes`) 及后续 Haramel 对照任务对齐提交

working tree: dirty；验收对应的任务 XML 与回归测试已包含在提交中，其他未提交文件不属于本次修复。

Aion 5.8 client/data provenance: Aion 5.8 客户端；页面与动作证据取自仓库内 `docs/quest/client-dialog-mapping/` 清单及旧任务模板；本次未重新采集客户端包哈希。

npc template/object: 18509 接取 NPC `799523`、交付 NPC `799524`，交互物 `700853`；28509 接取 NPC `799523`、交付 NPC `799524`，交互物 `700853`；18505 接取 NPC `203166`、交付 NPC `203106`。运行时 object ID not captured。

map/instance: 哈拉梅尔副本 `300200000`；实际运行 world/instance ID not captured。

steps:

1. 18509/28509：在 Koran (`799523`) 处接取任务，进入哈拉梅尔副本开启箱子 (`700853`) 获得任务物品，在 Muorinerk (`799524`) 处交付并领取奖励。
2. 18505：在 Zephyr (`203166`) 处接取任务，击杀副本内怪物收集任务物品，在 Gaphyrk (`203106`) 处交付并领取奖励。

source state/status/vars: 任务开始为 `START var0=0`；交付后为 `REWARD var0=1`；完成后为 `COMPLETE var0=0`。

action/page/button: 交互物使用 `ACTION_ITEM_USE` 门控与 `USE_OBJECT` 动作；接取 NPC 使用 `QUEST_SELECT(31)`、`ASK_QUEST_ACCEPT(1002)`、`QUEST_ACCEPT_1(1003)` 及 `FINISH_DIALOG(1008)`；交付 NPC 使用 `QUEST_SELECT(31)`、`CHECK_USER_HAS_QUEST_ITEM(39)`、`CHECK_USER_HAS_QUEST_ITEM_SIMPLE(20002)`、`SET_SUCCEED(10255)` 及奖励选择动作。

expected response: 接取 NPC 与交付 NPC 严格解耦；接取后关闭对话或返回选择页正常；箱子交互物在 `START` 状态可用并掉落任务物品；交付 NPC 校验并移除物品后完成奖励流程。

actual response: 用户确认验证完成，未报告无法接取、交互物无响应或错误交付 NPC 异常。

startup health: 所有 XML 均通过 `xmllint --noout --schema quest_definition.xsd`，`git diff --check` 通过；`QuestHaramelItemCollectingRegressionTest` 与 `CollectTurnInClientActionAlignmentBatchTest` 全量通过。

runtime logs: not captured

protocol trace: not captured

screenshots/recordings and SHA-256: not captured

acceptance status: ACCEPTED_EXISTING_PATTERN

matched Pattern: `MULTI_NPC_HANDOFF_REWARD_OWNER`（接取 NPC 与交付/领奖 NPC 独立且交付 owner 唯一）；`STATE_GATED_ACTION_ITEM_USE_FLOW`（交互物动作与掉落门控）；`CLIENT_OWNED_ACCEPT_AND_FINISH_TRANSITIONS`（客户端显式关闭/完成路由）。代表提交 `c02c8722e`、`0786d4126`、`598deb98f`；代表测试 `QuestHaramelItemCollectingRegressionTest`。

remaining risks: Maven 专项测试已运行并全部通过；运行时 object/instance ID、启动日志、抓包和截图未捕获；28509 为同一 XML 修复批次中的阵营对照任务，未单独记录客户端路径。
