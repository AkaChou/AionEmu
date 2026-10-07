# 存量红灯重锚进度（盘古计划 §8.9 P3 口径）

范围：`mvn test '-Dtest=*Quest*Test,*Retail*Test'` 的 69 个存量红灯类（1485 例 / 27F + 152E）。
口径：每个类「旧 typed 断言 → 真端表行 + 客户端页契约 + native 对话面」重锚，逐类经 IDEA MCP 跑绿。
基线清单：同目录 `red-classes.tsv`（跑测日志 `focused-run.log`）。

## 已完成（27 类，全部 IDEA MCP 复跑绿）

| 类 | 真端锚点 | 备注 |
|---|---|---|
| QuestMetadataFieldMappingTest | catalog=733（b22e1e971） | 计数重锚 |
| QuestRetailStartMetadataGateTest | 退役行由 RetailQuestDriver 供元数据 + cap 台账缩至 4 行 | 6/6 |
| Quest1112ProductionFlowTest | SimpleHunt 行（Feira 双槽 5/5） | 全量重写 |
| Quest1346 / 1347 / 1376 / 1470 四个 ClientDialogAlignment | 各自 SimpleHunt 行 + 相机 + 奖励 | |
| Quest2841RetailAlignmentTest | 相机 44 + ASMODIANS | |
| Quest30318To30321RetailAlignmentTest | 前置链 + 行 + 面（循环内 clearPackets） | |
| Quest11110And1548PostKillReportDialogTest | 击杀打满停 START + 报告动作 | |
| Quest1842RepeatLifecycleTest | 可重复行 COMPLETE 重开 + 20000 清零 | |
| Quest19001 / 19003 / 29001 / 29003 ClientDialogAlignment | 单步 talk 行（接取+发放文档 / 报告 Fasimedes·Vidar / 2375 确认 / 1009→REWARD 页 5） | 19003/29001/29003 同形 |
| Quest19004RetailAlignmentTest | 两步中继 Perikles→Jucleas→Lavirintos→Hilarus | 步页 1352/1693 |
| Quest1553ClientDialogAlignmentTest | 镜子物换物（give 182201795 + remove 182201794） | RecordingInventory |
| Quest1913ClientDialogAlignmentTest | SimpleTalk 单步 Macus→Polyidus→Hyacinte | 旧 typed 传送 210030000 退役（talk 行无传送列） |
| Quest1913ProductionFlowTest | 同上（运行时全量重写为 native 流程） | 未满中继→页 10；10000→var0=1+关窗 |
| Quest1192StepChainContractTest | 三段链 Spatalos→Lavirintos→Xenophon→Spatalos | 未轮到零响应 + 跳步领取只回页 10 |
| Quest2953RetailFlowAlignmentTest | 单步 Veldina 物品链（可重复 100） | 物品通道 give+remove 断言 |
| Quest3961To3964RetailAlignmentTest | 灵符链 Flora→erdos→Flora + item_check 整组门 | 未持满→失败页 2716；持满→扣整组 + REWARD |
| Quest16802ClientDialogAlignmentTest | DD EnterArea + 30+2 网格（26802 天族孪生） | 镜像 26802 的 DD 公共面 |
| Quest19637ClientDialogAlignmentTest | DD Talk + 4 变体 ×10 单段网格 | 第 10 杀收口清槽；接取 4762 |
| Quest13944RetailAlignmentTest | DD Talk + 两变体 ×3 网格 | Dian 同时服务 13943 ⇒ 必须带 requestedOwner |
| Quest28932RewardRowContractTest | DD Talk + 单杀网格 + 交付 DF6_Olivia_E | 一杀收口 |
| Quest23830To23834TargetlessRewardTest | DD LevelUpLogIn + ItemPlay + 11 职业奖励梯 | classRewards 键 = 真端 tag（FIGHTER/KNIGHT/WIZARD/ELEMENTALIST/PRIEST…）；LevelUpLogIn 行不注册 Talk 交付面（面边界据实锁定） |

## 关键口径备忘

- 退役 quest 的 owner 断言三件套：`RetiredQuestIds.contains` + `SimpleTalkHandler/SimpleHuntHandler.routes` + `ProductionQuestDefinitions.catalog().findExecutable(...).isEmpty()`。
- 接取面资格走 `NativeQuestStartPort.evaluateNpcAcquire`（CanAcquireQuest）：前置未满足不进面（0 响应）⇒ 面测试先 `completePrerequisites`。
- 单步 talk 行的 START 报告面 = **在交付 NPC** 上发 `reportConfirmPage`（select2 被中继占用时跳过；本例全为 select5=2375）；未满中继 = 页 10（两参）。
- 中继推进 = var0=step + SM_QUEST_ACTION + 步物品 give/remove + 关窗（真端 after-commit 零发页）。
- 1913 的传送（210030000）是退役 XML 作者效果，真端 talk 行未声明传送列；车道语义以 var0 门控为准（如后续实机发现缺口另开修复）。

## 口径变更：退役即删（2026-10-07 用户裁定）

「很多测试类，如果没有 xml 就直接删好了」——被测任务已退役（owner=RETAIL_TABLE、XML 只在 git 历史）且
无 Playbook `#method` 引用的专属测试类，直接删除，不再逐个重锚。

已删 24 类（定义包 14 / 运行时 9 / dataholders 1）：
- definition（14）：Quest10506、Quest25608RetailSevenStep、Quest30721And30771RetailFlow、Quest80312、Quest80318、
  Quest15400And25400KillCounterContract、QuestStepDialogResponseRegression、RetailSimpleTalkMigrationReviewContract、
  QuestBatchReportNpcAlignment、QuestDialog31Regression、QuestPrematureRewardRouteExclusion、
  XmlQuestFamilyDefinition、QuestInventoryStartItemGate、QuestSoloriusEventDefinition。
- runtime（9）：Quest1118ProductionFlow、QuestArchivesDualCounterProductionFlow、QuestCradleCounterProductionFlow、
  QuestCounterSourceProjectionProductionFlow、QuestDispatchToAltgardFamilyProductionFlow、
  QuestDispatchToVerteronFamilyProductionFlow、QuestHaramelSubsequentQuestsProductionFlow、
  QuestLegacyMonsterHuntProductionFlow、QuestResidualCounterLocks。
- dataholders（1）：QuestConquestOfferingSpawnData。

配套修复：`retail-non-ir-axis-registry.tsv` 按生成器重跑（`.agents/summary/scriptdll-quest-driver/p0c11_build_non_ir_registry.py`，
30 行 → 20 行）——台账裁剪（cap-exceptions 30 → 4 行）后登记表镜像 `RetailNonIrAxisGateTest` 曾失配。

套件复核（授权命令 `mvn -o test '-Dtest=*Quest*Test,*Retail*Test'`）：**1384 例 / 11F + 19E = 30 红**
（原 1485 例 / 179 红；Playbook 引用门禁复跑通过）。

## 当前红灯（21 类，2026-10-07 复核）

- 原生断言待裁定（原生测试，失败=行为差异）：Quest1309（[5]vs[10]）、QuestEventQuestBatchDefinition（REWARD vs START）、
  QuestLunarEventDefinition（80034 重开窗）、QuestMultistepChainContract（[1352]vs[0]、[5]vs[10]）、
  PlayerQuestStartEligibilityPort（2 vs 1）。
- 门禁/登记表：QuestRewardValueGate（契约 16800/16801 缺目录）、QuestTitleRewardCoverage（173 vs 76）、
  RetailQuestAiNameGroupGate（title_id 非唯一样本）、RetailNonIrAxisGate（saveHeal 查 16900 走 typed 面）。
- 待查（NPE/文件缺失，锚点是否已退役）：RetailPatternAI2、CompletedQuestPrerequisiteRegression、
  DisabledClientQuestPlaceholderCatalog、QuestInstanceExitRecovery、Quest10031And20031（10031/20032 半退役）、
  Quest10032ItemPlayClientCounter（20032 半退役）、QuestCorridorAndDestinyCounter。
- Playbook `#method` 引用（暂缓，改写需同步 Playbook）：Quest1220、Quest18600、QuestPacketOrderRegression、
  QuestStartItemDefinitionRegression、QuestMutationPlannerTest。

## 待办（原登记）

- SimpleTalk 断言族：10506 / 1309 / 15400+25400 / 30721+30771 / 80312 / 80318（含断言裁定，需逐条判）。
- SimpleTalk 结构族：28932 / 13944 / 3961-3964 / 23830-23834（先核族属与行）。
- CollectItem 族：25608（collect_progress 6 + check 页族）。
- DD 族：16802 / 19637（DataDrivenNativeRuntime）。
- 跨切/引擎：QuestBatchReportNpcAlignmentTest（10E）、QuestPrematureRewardRouteExclusionTest（20E）、QuestDialog31RegressionTest、QuestStepDialogResponseRegressionTest、RetailSimpleTalkMigrationReviewContractTest、XmlQuestFamilyDefinitionTest 等。
- 被 Playbook/CASES 引用的类（Quest18600 / Quest1220 / QuestMutationPlannerTest / QuestPacketOrderRegressionTest / QuestStartItemDefinitionRegressionTest / DisabledClientQuestPlaceholderCatalogTest）暂缓：改写需同步改 Playbook 引用，避免门禁断裂。
