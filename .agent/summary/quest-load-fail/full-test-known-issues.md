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
