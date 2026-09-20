# 11323 奖励窗口 “load fail” 修复与验证（2026-09-20）

## 结论

`SM_DIALOG_WINDOW page=23` 不是客户端解析错误，而是服务端把**按钮动作 ID**当成**页面 ID**下发了。

链路：

1. 玩家在奖励窗口点“放弃奖励”，客户端上送 `CM_DIALOG_SELECT action=23 (SELECTED_QUEST_NOREWARD)`，
   但 `targetObjectId` 仍是本次对话的交互对象（702745 / 对象 72989），不是任务登记的报告 NPC（798928）。
2. 11323 的交付路由（`dialog=8..23`，`reward -> complete`）编译后**只绑定 798928**；
   typed 引擎按 `TalkToNpc(npcId, dialogId)` 严格匹配失败 → `handled=false`。
3. `PortalDialogAI2.onDialogSelect` 的兜底分支执行
   `sendPacket(new SM_DIALOG_WINDOW(getObjectId(), dialogId, questId))`，把动作 ID 23 当页 ID 回显；
   客户端找不到 page 23（`HtmlPages 23 = HTML_PAGE_SHOW_GATHER_SKILL_LEVELUP_WINDOW`）→ load fail，
   任务停在 `Status: REWARD / Vars: 4 0 0 0 0 / Complete count: 0`。

## 修复

- `QuestDialogAction.isRewardWindowAction(int)`：标识 `SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD`(8..23)。
- `QuestEngine.onDialog`：严格绑定未命中且动作为奖励窗口确认时，按 `questId + action` 走恢复路径。
- `QuestProductionDispatcher.dispatchRewardWindowAction(...)` + `selectRewardCompletionRoute(...)`：
  只接受 owner 内**唯一**的 `REWARD -> COMPLETE` 且动作 ID 相同的转换；候选多于一个时 fail closed。
- `QuestExecutionCoordinator` / `QuestMutationPlanner`：新增“已按 questId + action 校验”的执行通道，
  跳过事件形状匹配但仍按玩家真实快照评估状态、条件与动作可行性；默认调用路径行为不变。
- `QuestEvent.matches` 补 `public`：运行时包调用它是既成事实（`target/classes` 里就是 public），
  源码缺失修饰符会导致干净构建失败。
- i18n：新增 `log.quest_engine.reward_window_unpinned_recovery`（中英 message bundle 同步）。

## 验证

- `mvn -q -o -DskipTests compile`：通过。
- `mvn -q -o -Dtest=QuestProductionDispatcherTest test`：21 用例通过（含新增
  `rewardWindowConfirmationRecoversTheTurnInRouteWithoutTheReportNpc`，先用严格绑定复现未命中，
  再验证恢复路径提交）。
- `mvn -q -o -Dtest='QuestProductionDispatcherTest,QuestExecutionCoordinatorTest,ReportToManyLegacyFlowRegressionTest,QuestXmlDomainBlocksTest,QuestRuntimeInfrastructureTest,QuestRuntimeCompositionCatalogSnapshotTest' test`：通过。
- `mvn -q -o -Dtest='QuestClientContractGateTest,QuestEngineRuntimeCompositionTest,QuestEngineEscortAndProximityRegistrationTest,QuestMovieContinuationGateTest,QuestDispatchToAltgardFamilyProductionFlowTest,Quest1913ProductionFlowTest' test`：通过。
- `mvn -q -o -Dtest='LocalizedLogCallsTest' test`：通过。
- 定义包全量（971 用例）存在 4 个**与本次改动无关的既有失败**，已用 stash 基线复现同样的
  `QuestKillCounterRetailGateTest` / `Quest25512ClientDialogAlignmentTest` / `QuestMovieAndDialogLoopRegressionTest` 失败。

## 待办

- 真实客户端复验：11323 奖励窗口点“放弃奖励”应完成任务且无 load fail。
- 该恢复通道对所有 owner 生效；若后续发现某任务在奖励窗口状态下存在多条 `reward -> complete`
  同动作候选，`selectRewardCompletionRoute` 会拒绝执行并回落到旧行为。
