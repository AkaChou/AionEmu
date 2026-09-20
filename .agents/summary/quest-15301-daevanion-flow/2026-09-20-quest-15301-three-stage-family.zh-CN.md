# 15301 三阶段对话与同族材料扣除修复

日期：2026-09-20

## 玩家现象

- `玩家=Ww` 在 NPC `805327` 接取后进入 `15301 / START`，随后对 `805328` 发送 `CM_DIALOG_SELECT questId=15301 动作=31`。
- 服务端一直返回 `SM_DIALOG_WINDOW targetObj=65709 下发页=10`，客户端无法进入第一段任务页。
- 旧 `15301.xml` 只有 `started var0=0`，并把 `805328` 的 `SELECT1_1/SELECT1_1_1/SELECT1_1_1_1` 全部声明在 `unaccepted`，没有 `QUEST_SELECT(31)` 路由；接取后的 `START` 状态因此无法命中该 NPC 的任务入口。

## 权威证据

- Aion 5.8 客户端页面与动作：
  - `docs/quest/client-dialog-mapping/quest-dialog-pages.csv` 中 `15301`/`25301` 的 `SELECT_NONE(4762)`、`SELECT1(1011)`、`SELECT1_1(1012)`、`SELECT2(1352)`、`CHECK_USER_ITEM_OK(10000)`、`SELECT3(1693)`、`DEFAULT_SUCCESS(10002)`。
  - `docs/quest/client-dialog-mapping/quest-dialog-action-details.csv` 证明 `SELECT1_1_1_1 -> SETPRO1(10000)`、`SELECT2 -> CHECK_USER_HAS_QUEST_ITEM(39)`、`SELECT3_1_1 -> SET_SUCCEED(10255)`。
- 旧实现行为：
  - `origin/history:src/main/resources/aion/definitions/compact/quests/scripts/zz_retail_simple_quests.xml` 的 `15301`/`25301` 都是 `TALK -> COLLECT_ITEM -> TALK(give_item_id=182215859/182215871)` 三阶段。
  - `origin/history:.../DataDrivenQuest.java` 的 `COLLECT_ITEM` 分支调用 `checkQuestItems(...)`；`QuestHandler.checkQuestItems` 调用 `QuestService.collectItemCheck(env, true)`，因此收集物在交物阶段扣除，最终 `TALK` 只发放工作物品。
- 门禁证据：
  - `QuestClientContractGateTest` 使用 `QuestDialogOrderAudit` 检查可见按钮是否有服务端路由；修复前 `25304` 的 `started/805339/SELECT2` 暴露了无动作按钮 `CHECK_USER_HAS_QUEST_ITEM(39)`。

## 本轮修复

- 首次三阶段家族 8 条：`15301`、`15302`、`15303`、`15305`、`25301`、`25302`、`25303`、`25305`。
  - 重建 `s0 -> s1 -> s2 -> reward -> complete` 状态链，保留 `unaccepted` 接取边界。
  - 接取 NPC 只负责 `NPC_START` 和最终领奖；任务 NPC 显式提供 `QUEST_SELECT` 及对应客户端页链。
  - `CHECK_USER_HAS_QUEST_ITEM` 成功时按条件扣除材料、显示 `CHECK_USER_ITEM_OK`，然后 `SET_SUCCEED` 发放 `work-item` 并进入 `REWARD`。
- 重复三阶段家族 8 条：`15311`、`15312`、`15313`、`15315`、`25311`、`25312`、`25313`、`25315`。
  - 将材料扣除统一放在 `CHECK_USER_HAS_QUEST_ITEM` 成功分支。
  - 删除 `SET_SUCCEED` 阶段的重复扣除；`15311/15312/25311/25312` 原先在交物和收尾各扣一次，已修正为只扣一次。
- `25304`：
  - 删除 `started/805339` 上的 `QUEST_SELECT -> SELECT2`、`SELECT2_1 -> SELECT2_1` 两条越权展示路由。
  - 页面仍由 `805340` 的进度链提供，`805339` 只保留接取与领奖入口，消除错误 NPC 上的无动作按钮。

## 覆盖测试

- 新增 `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestDaevanionThreeStageFlowTest.java`：
  - 验证 8 条首次任务和 8 条重复任务的三阶段节点、客户端动作/页面、接取即时响应、材料单次扣除、错误 NPC/乱序动作拒绝、独占领奖选择。
- 已运行：
  - `rtk mvn -q -Dtest=QuestDaevanionThreeStageFlowTest,QuestDaevanionLeggingsProductionFlowTest,QuestItemSourceContractGateTest,QuestClientContractGateTest,QuestSection0ReportRowContractTest,QuestMonsterProgressContractAuditTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,QuestPageButtonAuditTest test`
  - 结果：全部通过；`PRODUCTION_COMPILE_OK=6189`、`PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_INTERACTION_OBJECT_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`。

## 状态

- 静态、聚焦测试与生产目录门禁已通过。
- 真实客户端尚未复验，状态为 `PENDING`；未提交本地 commit。
