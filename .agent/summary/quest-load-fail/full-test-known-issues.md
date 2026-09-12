# 全量测试已知问题清单（2026-09-12 全量跑：3086 tests）

本会话已修复：CollectTurnInClientActionAlignmentBatchTest（18745 确认页顺序与断言）。

## 环境性失败（JDK 25 反射 / Objenesis mock 限制，与本轮改动无关）—— 113 个

- com.aionemu.gameserver.controllers.movement.NpcMoveControllerPathTest (2)
- com.aionemu.gameserver.network.aion.serverpackets.SMInstanceScoreFissureTest (1)
- com.aionemu.gameserver.network.aion.serverpackets.SMLunaShopTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest13704ClientDialogAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest13708ClientDialogAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest13951And23951RewardOwnerTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest1466ClientDialogAlignmentTest (2)
- com.aionemu.gameserver.questEngine.definition.Quest15334And25334And30800ClientDialogAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest19048And23704And23708And29048ClientDialogAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest2393And3722ItemPlayRewardOwnerTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest25670ClientDialogAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest28931ClientDialogAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest3100ClientDialogAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest3732InstanceObjectiveTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest4914ClientDialogAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest49715RetailFlowAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.QuestPacketOrderRegressionTest (2)
- com.aionemu.gameserver.questEngine.e2e.ClientTaskScopeAuditTest (2)
- com.aionemu.gameserver.questEngine.e2e.QuestE2eInfrastructureTest (37)
- com.aionemu.gameserver.questEngine.e2e.QuestEquippedStartProductionFlowTest (1)
- com.aionemu.gameserver.questEngine.e2e.QuestExclusiveSiblingAttributionTest (2)
- com.aionemu.gameserver.questEngine.e2e.QuestGoldenJourneyTest (3)
- com.aionemu.gameserver.questEngine.e2e.QuestProductionJourneyTest (9)
- com.aionemu.gameserver.questEngine.e2e.QuestWorldReachabilityOracleTest (1)
- com.aionemu.gameserver.questEngine.runtime.PlayerQuestDialogPortTest (9)
- com.aionemu.gameserver.questEngine.runtime.PlayerQuestMoviePortTest (1)
- com.aionemu.gameserver.questEngine.runtime.PlayerQuestRewardPortTest (1)
- com.aionemu.gameserver.questEngine.runtime.Quest1112ProductionFlowTest (3)
- com.aionemu.gameserver.questEngine.runtime.Quest80487ProductionFlowTest (2)
- com.aionemu.gameserver.questEngine.runtime.QuestCounterProjectionLockFollowUpTest (3)
- com.aionemu.gameserver.questEngine.runtime.QuestCounterSourceProjectionProductionFlowTest (2)
- com.aionemu.gameserver.questEngine.runtime.QuestDialogProjectionLockFollowUpTest (2)
- com.aionemu.gameserver.questEngine.runtime.QuestMinionTutorialProductionFlowTest (3)
- com.aionemu.gameserver.questEngine.runtime.QuestResidualCounterLocksTest (3)
- com.aionemu.gameserver.questEngine.runtime.QuestStepNpcSkipGuardTest (2)
- com.aionemu.gameserver.services.DialogServiceQuestDialogTest (4)
- com.aionemu.gameserver.services.PetitionServiceTest (1)
- com.aionemu.gameserver.services.player.PlayerLimitServiceTest (2)

## 断言漂移 / 待甄别 —— 72 个

甄别方法：git diff ee0363c10..HEAD 检查失败断言对应的任务 XML 是否被本会话改动；
未改动者为既有漂移（全量测试在起点从未运行过），已改动者按客户端证据更新断言或修 XML。

已确认的既有漂移样本（断言与本会话改动无关）:
- Quest2841RetailAlignmentTest：sync 模式断言（PACKET_ONLY）与起点 XML（target=reward → LEVEL_AND_VISIBILITY_REFRESH）不符
- Quest3057RetailFlowAlignmentTest：QUEST_SELECT target=started 断言与 NPC_START 生成语义（target=source）矛盾，断言起点已存在
- QuestStartItemDefinitionRegressionTest：1582 XML 未被本会话改动
- QuestAdditionalCapabilityDefinitionTest#lowersSelectableReward：inline XML 缺 COMPLETION sync（旧提交 68d5786a5 引入）

- com.aionemu.boot.AionBootApplicationTest (2)
- com.aionemu.gameserver.dataholders.loadingutils.RetailAiDefinitionLoaderTest (1)
- com.aionemu.gameserver.dataholders.loadingutils.TemplateShardWriterTest (2)
- com.aionemu.gameserver.instance.handlers.scripts.FissureOfOblivionInstanceTest (1)
- com.aionemu.gameserver.instance.handlers.scripts.KromedesTrialInstanceTest (1)
- com.aionemu.gameserver.lifecycle.GameCoreServicesRuntimeBridgeTest (1)
- com.aionemu.gameserver.model.templates.item.actions.DyeActionTest (1)
- com.aionemu.gameserver.questEngine.definition.CompletedQuestPrerequisiteRegressionTest (1)
- com.aionemu.gameserver.questEngine.definition.EarlyElyosQuestRegressionTest (1)
- com.aionemu.gameserver.questEngine.definition.ItemCollectingDialogProtocolAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.MigratedQuestRepairDefinitionTest (1)
- com.aionemu.gameserver.questEngine.definition.MissionItemConsumptionBatchRegressionTest (1)
- com.aionemu.gameserver.questEngine.definition.MonsterHunt1102DefinitionTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest14010EnterZoneProtocolTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest14015ClientDialogAlignmentTest (2)
- com.aionemu.gameserver.questEngine.definition.Quest1607MappingTheRevolutionariesRegressionTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest18602ClientDialogAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest1926And2938ClientDialogAlignmentTest (2)
- com.aionemu.gameserver.questEngine.definition.Quest21114PoisonedFungiRetailFlowTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest2150ClientDialogAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest24043RetailFlowAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest26803ClientDialogAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest26804ClientDialogAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest2841RetailAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest28510ClientDialogAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest2877To2887UrgentOrdersFlowTest (22)
- com.aionemu.gameserver.questEngine.definition.Quest28800ClientDialogAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest3057RetailFlowAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.Quest3103ClientDialogAlignmentTest (1)
- com.aionemu.gameserver.questEngine.definition.QuestAdditionalCapabilityDefinitionTest (1)
- com.aionemu.gameserver.questEngine.definition.QuestDialog31RegressionTest (1)
- com.aionemu.gameserver.questEngine.definition.QuestNoHandlerShard3DefinitionTest (1)
- com.aionemu.gameserver.questEngine.definition.QuestStartItemDefinitionRegressionTest (1)
- com.aionemu.gameserver.questEngine.definition.QuestStepDialogTerminationTest (1)
- com.aionemu.gameserver.questEngine.definition.ReportTo1101DefinitionTest (1)
- com.aionemu.gameserver.questEngine.definition.ReportToManySetSucceedAlignmentTest (2)
- com.aionemu.gameserver.questEngine.e2e.QuestPageButtonAuditTest (1)
- com.aionemu.gameserver.questEngine.runtime.QuestInteractionObjectCatalogTest (2)
- com.aionemu.gameserver.questEngine.runtime.QuestLegacyMonsterHuntProductionFlowTest (2)
- com.aionemu.gameserver.questEngine.runtime.QuestMutationPlannerTest (1)
- com.aionemu.gameserver.questEngine.runtime.QuestProductionAcceptProtocolRegressionTest (2)
- com.aionemu.loginserver.taskmanager.handler.implementations.LoginTaskShutdownHandlerTest (2)


## 2026-09-12 第二轮甄别结论（回滚简报对后）

select_none 批的 118 个"简报对"（QUEST_SELECT→SELECT_NONE + FINISH close，带 start-eligible）
违反了两项此前验收的合同，已全部回滚：

1. 区域自动接取任务（1877-1887/2877-2887 等）：NONE 态不得有任何对话路由
   （Quest2877To2887UrgentOrdersFlowTest 68-70 行显式断言，22/22 恢复绿）；
2. auto-start 道具收集任务（80945/80946）：同上
   （ItemCollectingDialogProtocolAlignmentTest"remains auto-started"断言）。
   start-eligible 还触发 repeatable 别名展开（1 条迁移变 3 条），破坏精确计数断言。

回滚后全量复跑的 45 个非环境失败中，逐一甄别（git diff 对照失败断言的任务 XML）：

- 本会话引入且已修复：CollectTurnIn 18745（确认页顺序+断言）、ItemCollecting 80945/80946（回滚）、
  Quest2877To2887 22 个（回滚）。
- 确认既有漂移（失败断言对应的任务 XML 未被本会话改动，或断言与起点 XML 即不符）：
  ReportToMany 29683（无 diff）、ReportToMany 1876 step-1 页（diff 仅 reward 路由）、
  MissionItemConsumption 2333（起点 39 路由即无 remove-item）、2841、3057、1582、1926/2938
  （null-source enter-world 路由起点即有）、20047 inline XML 编译错（旧提交引入）等。
- 结论：剩余 34 类全部为仓库存量技术债（全量测试从未在起点运行），不属于本次治理范围；
  逐类修复需各自按客户端证据重新审定合同，建议独立任务推进。

## 2026-09-12 第三轮（环境修复后）终态清单

环境修复（NetworkConfig 代码级默认值 + 2 个 de-final + 1466 断言）后：185 → 61 个失败/错误。

本轮甄别补充（新增确认的既有漂移）：
- QuestProductionAcceptProtocolRegressionTest 16802/16803/16804：断言 31→1011，但该任务
  NPC_START 在早前会话已按客户端证据改为 SELECT_NONE（HTML 无 1011 页），断言未同步；
- MissionItemConsumption 2333：起点 39 路由即无 remove-item；
- ReportToMany 29683：任务 XML 无本会话 diff；
- QuestPacketOrder 1607：断言 z4 节点，起点 XML 即不存在。

余下失败均为 NO_MATCH e2e 家族（13704/13708/19048/28931/4914/2393/1466-step/ProductionJourney）、
计数漂移（MonsterHunt1102 36v35、ReportTo1101 30v29）、非 quest 基建
（TemplateShardWriter/RetailAiLoader/DyeAction/GameCoreServices/AionBoot 等）——
全部为起点存量（全量测试从未在起点运行），修复需各自独立甄别任务。

- com.aionemu.gameserver.questEngine.e2e.QuestProductionJourneyTest (4) touched=[] :: Failure[stepIndex=0, status=NO_MATCH, reason=request did not produce a conclusive handled route
- com.aionemu.gameserver.questEngine.definition.ReportToManySetSucceedAlignmentTest (2) touched=['1876'] :: quest 29683 started dialog 1009 ==> expected: <1> but was: <0>
- com.aionemu.boot.AionBootApplicationTest (2) touched=[] :: expected: <true> but was: <false>
- com.aionemu.gameserver.questEngine.definition.Quest14015ClientDialogAlignmentTest (2) touched=[] :: Cannot invoke "String.equals(Object)" because the return value of "com.aionemu.gameserver.quest
- com.aionemu.gameserver.dataholders.loadingutils.TemplateShardWriterTest (2) touched=[] :: expected: <npc_template_250001_250003.xml> but was: <npc_template_250001_250002.xml>
- com.aionemu.gameserver.questEngine.runtime.QuestProductionAcceptProtocolRegressionTest (2) touched=['16802'] :: quest 16802 npc 806148 is missing the 31 -> 1011 route ==> expected: <true> but was: <false>
- com.aionemu.gameserver.questEngine.definition.QuestPacketOrderRegressionTest (2) touched=[] :: DispatchOutcome[handled=false, failed=false, stateChanged=false, failure=null, packets=[]] ==> 
- com.aionemu.gameserver.questEngine.definition.Quest1926And2938ClientDialogAlignmentTest (2) touched=[] :: Cannot invoke "String.equals(Object)" because the return value of "com.aionemu.gameserver.quest
- com.aionemu.gameserver.questEngine.runtime.QuestInteractionObjectCatalogTest (2) touched=[] :: NO_TRANSITIONS: executable definition has no transitions
- com.aionemu.gameserver.questEngine.runtime.QuestLegacyMonsterHuntProductionFlowTest (2) touched=[] :: expected: <true> but was: <false>
- com.aionemu.gameserver.questEngine.definition.Quest13704ClientDialogAlignmentTest (1) touched=[] :: Failure[stepIndex=0, status=NO_MATCH, reason=request did not produce a conclusive handled route
- com.aionemu.gameserver.questEngine.definition.Quest28931ClientDialogAlignmentTest (1) touched=[] :: Failure[stepIndex=0, status=NO_MATCH, reason=request did not produce a conclusive handled route
- com.aionemu.gameserver.questEngine.definition.QuestAdditionalCapabilityDefinitionTest (1) touched=[] :: COMPLETE_QUEST_SYNC_REQUIRED: a COMPLETE projection requires one COMPLETION quest-state sync
- com.aionemu.gameserver.questEngine.e2e.QuestEquippedStartProductionFlowTest (1) touched=[] :: DispatchOutcome[handled=false, failed=false, stateChanged=false, failure=null, packets=[]] ==> 
- com.aionemu.gameserver.questEngine.definition.QuestDialog31RegressionTest (1) touched=[] :: Cannot invoke "String.equals(Object)" because the return value of "com.aionemu.gameserver.quest
- com.aionemu.gameserver.questEngine.e2e.QuestPageButtonAuditTest (1) touched=[] :: unrouted buttons shown on displayed pages must be flagged: [] ==> expected: <true> but was: <fa
- com.aionemu.gameserver.questEngine.definition.Quest4914ClientDialogAlignmentTest (1) touched=[] :: Failure[stepIndex=0, status=NO_MATCH, reason=request did not produce a conclusive handled route
- com.aionemu.gameserver.questEngine.definition.Quest28800ClientDialogAlignmentTest (1) touched=[] :: expected: <[ShowQuestDialog[dialogId=1352]]> but was: <[ShowQuestDialog[dialogId=2375]]>
- com.aionemu.gameserver.questEngine.definition.Quest28510ClientDialogAlignmentTest (1) touched=[] :: expected: <[ShowQuestDialog[dialogId=10002]]> but was: <[ShowQuestDialog[dialogId=5]]>
- com.aionemu.gameserver.questEngine.definition.CompletedQuestPrerequisiteRegressionTest (1) touched=[] :: Cannot invoke "String.equals(Object)" because the return value of "com.aionemu.gameserver.quest
- com.aionemu.gameserver.instance.handlers.scripts.FissureOfOblivionInstanceTest (1) touched=[] :: expected: <1> but was: <2>
- com.aionemu.gameserver.questEngine.definition.Quest18602ClientDialogAlignmentTest (1) touched=[] :: expected: <[[QuestStartCondition[type=finished, questId=18601, rewardMode=0]], [QuestStartCondi
- com.aionemu.gameserver.questEngine.definition.Quest14010EnterZoneProtocolTest (1) touched=[] :: Cannot invoke "String.equals(Object)" because the return value of "com.aionemu.gameserver.quest
- com.aionemu.gameserver.questEngine.definition.Quest24043RetailFlowAlignmentTest (1) touched=[] :: expected: <true> but was: <false>
- com.aionemu.gameserver.dataholders.loadingutils.RetailAiDefinitionLoaderTest (1) touched=[] :: expected: <132> but was: <0>
- com.aionemu.gameserver.questEngine.runtime.QuestMutationPlannerTest (1) touched=[] :: expected: <false> but was: <true>
- com.aionemu.gameserver.questEngine.definition.Quest21114PoisonedFungiRetailFlowTest (1) touched=[] :: No value present
- com.aionemu.gameserver.model.templates.item.actions.DyeActionTest (1) touched=[] :: expected: <true> but was: <false>
- com.aionemu.gameserver.questEngine.e2e.ClientTaskScopeAuditTest (1) touched=[] :: No value present
- com.aionemu.gameserver.questEngine.definition.QuestNoHandlerShard3DefinitionTest (1) touched=[] :: expected: <VariableBelow[field=var1, value=10]> but was: <VariableBelow[field=var0, value=9]>
- com.aionemu.gameserver.questEngine.definition.ReportTo1101DefinitionTest (1) touched=[] :: expected: <30> but was: <29>
- com.aionemu.gameserver.questEngine.definition.MonsterHunt1102DefinitionTest (1) touched=[] :: expected: <36> but was: <35>
- com.aionemu.gameserver.questEngine.definition.MigratedQuestRepairDefinitionTest (1) touched=[] :: expected: <true> but was: <false>
- com.aionemu.gameserver.questEngine.definition.Quest19048And23704And23708And29048ClientDialogAlignmentTest (1) touched=[] :: 19048 Failure[stepIndex=0, status=NO_MATCH, reason=request did not produce a conclusive handled
- com.aionemu.gameserver.questEngine.definition.Quest3103ClientDialogAlignmentTest (1) touched=[] :: expected: <[3102]> but was: <[]>
- com.aionemu.gameserver.questEngine.definition.QuestStepDialogTerminationTest (1) touched=[] :: NPC step routes must return a valid dialog response: [2008:s6 npc=203550 dialog=10015, 2008:s6 
- com.aionemu.gameserver.lifecycle.GameCoreServicesRuntimeBridgeTest (1) touched=[] :: Failed to load static data
- com.aionemu.gameserver.questEngine.definition.Quest1607MappingTheRevolutionariesRegressionTest (1) touched=[] :: 
- com.aionemu.gameserver.questEngine.definition.Quest2841RetailAlignmentTest (1) touched=[] :: expected: <[SyncQuestState[mode=PACKET_ONLY], ShowQuestDialog[dialogId=5]]> but was: <[SyncQues
- com.aionemu.gameserver.questEngine.definition.EarlyElyosQuestRegressionTest (1) touched=[] :: expected: <true> but was: <false>
- com.aionemu.gameserver.questEngine.definition.QuestStartItemDefinitionRegressionTest (1) touched=[] :: quest 1582 NPC 700196 ==> expected: <true> but was: <false>
- com.aionemu.gameserver.questEngine.definition.Quest2393And3722ItemPlayRewardOwnerTest (1) touched=[] :: Failure[stepIndex=0, status=NO_MATCH, reason=request did not produce a conclusive handled route
- com.aionemu.gameserver.questEngine.definition.MissionItemConsumptionBatchRegressionTest (1) touched=['2333'] :: quest 2333 transition started -> v1 must remove items [182204131], but removed [] ==> expected:
- com.aionemu.gameserver.questEngine.definition.Quest13708ClientDialogAlignmentTest (1) touched=[] :: Failure[stepIndex=0, status=NO_MATCH, reason=request did not produce a conclusive handled route
- com.aionemu.gameserver.questEngine.definition.Quest3057RetailFlowAlignmentTest (1) touched=[] :: expected: <started> but was: <unaccepted>
- com.aionemu.gameserver.questEngine.definition.Quest2150ClientDialogAlignmentTest (1) touched=[] :: expected at least 4 completion routes, got 2 ==> expected: <true> but was: <false>
- com.aionemu.gameserver.instance.handlers.scripts.KromedesTrialInstanceTest (1) touched=[] :: Lady Angerr death should still spawn Distraught Lady Angerr ==> expected: <true> but was: <fals
- com.aionemu.gameserver.questEngine.definition.Quest1466ClientDialogAlignmentTest (1) touched=[] :: Failure[stepIndex=0, status=NO_MATCH, reason=request did not produce a conclusive handled route
- com.aionemu.gameserver.questEngine.e2e.QuestE2eInfrastructureTest (1) touched=[] :: No value present
