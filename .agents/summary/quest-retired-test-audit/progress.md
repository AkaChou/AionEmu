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

## 批次修复（2026-10-07，B/D 批，全部 IDEA MCP 复跑绿）

**B 批·半退役混锚（3 类）**：
- Quest10031And20031ZoneMissionBroadcastTest → 拆出 `Quest20031ZoneMissionBroadcastTest`（只保魔族半；退役目标 20032-20034 跳过路由核对，以 `checked>=1` 保证 live 目标（20035）仍受核对）。
- Quest10032ItemPlayClientCounterProductionFlowTest → 契约收缩到 live 半（10032，182215618-182215620）。
- QuestCorridorAndDestinyCounterProductionFlowTest → 只保 CORRIDOR_CONTRACTS(20035)；INGGISON_TARGETS 移除。

**D 批·门禁/登记表（4 类）**：
- QuestRewardValueGateTest → 生产视图并入退役行真端元数据（复用 `QuestRetailStartMetadataGateTest.addRetiredRetailMetadata`）。
- QuestTitleRewardCoverageTest → 退役行逐行取 `RetailQuestDriver.retailMetadataOf`；可执行性双车道（XML typed ∨ 已退役）。
- RetailQuestAiNameGroupGateTest → 全表不变式口径修正：title 判据改「组内 title 存在性一致」（16 组多 title_id 的 GAb1 族 + 2 组无 title 的 HousingManager 族）；精确通道一致性（精确命中只能为空或与声明集等集——别名表 5 条逻辑名与组表等集收敛）。
- RetailNonIrAxisGateTest → saveHeal「零 EnterWorld 路由」改三条现代表达（无 XML 定义 ∧ 未登记编译自愈边 ∧ SimpleHunt 车道 owns+routes）。

**E 批·NPE/文件缺失（4 类）**：
- RetailPatternAI2Test → Objenesis 夹具补 objectId（`setField(AionObject.class, ..., "objectId", n)`，选目标逻辑按 id 比对）。
- CompletedQuestPrerequisiteRegressionTest → 摘除已退役 subject（10035 方法删除、20032 行删除；DD 车道面由 DataDrivenNativeRuntimeGateTest 承担）。
- DisabledClientQuestPlaceholderCatalogTest → SUPPORT_ORDER 摘除退役的 10031 半；目录编译面随并行会话 4338.xml 收敛后 3/3 复验绿。
- QuestInstanceExitRecoveryTest → 摘除退役的 20034 场景（DataDriven 车道）。

**C 批·原生断言裁定（5 类）**：
- Quest1309ClientDialogAlignmentTest → 交付段按裁定 a 重锚：31 → 报告确认页 2375（select2 被中继占用）、1009 → REWARD + 页 5。
- QuestEventQuestBatchDefinitionTest → cake 行同为两步报告（31 → select5=2375、1009 → REWARD + 页 5）。
- QuestMultistepChainContractTest → 步面拆开：31 → 步页；推进（10000+step-1）after-commit 零发页 = 关窗（真端 FUN_180cabb10）；1514 交付同步裁定 a。
- QuestLunarEventDefinitionTest → COMPLETE 重开窗仍走 CanAcquireQuest 资格轴 ⇒ 先完成真端前置（Q80029/Q80032）。
- PlayerQuestStartEligibilityPortTest → 15321 退役（DD 车道）⇒ 用客户端槽位形状夹具锁定端口「组间或、组内与」语义。

**A 批·Playbook 引用（5 类，方法名保持、引用门禁复跑通过）**：
- Quest18600ClientDialogAlignmentTest → 全量重锚 SimpleTalk 行（Perento 接取/交付、Kurochin→Herthia 两步中继、give/remove 列、cutscene 189/10001）+ 客户端页链 + 两步报告。
- Quest1220ClientDialogAlignmentTest → 同上口径（Une 接取发箱、Shugo3 换箱、shugo_Lender 交付；1009 报告）。
- QuestStartItemDefinitionRegressionTest → 1323 退役半改锚 native 接取面（LF2_Lost_JewelBox=730032）；1582 typed 路由照旧。
- QuestPacketOrderRegressionTest → 24153 退役（SimpleHunt）⇒ typed 满段断言退场，新增 `NativeTalkFixture.assertQuestActionBeforeDialogWindow` 在 native 包队列上复核「已提交状态先于页面」；其余 4 行协议回环照旧。
- QuestMutationPlannerTest → 36539/36525 两条阵营生命周期行退役 ⇒ 方法删除（同一 planner 语义由 NpcFactionQuestMutationPlannerTest 合成定义常绿覆盖）。

## 当前红灯

本会话 21 个存量红灯类全部处理完毕（B 3 / D 4 / E 4 / C 5 / A 5），逐类经 IDEA MCP 复跑绿；
Playbook 引用门禁 `check_quest_repair_playbook.py` 复跑通过（81 条代表测试引用全部可解析）。
待办：全量套件复核（授权命令 `mvn -o test '-Dtest=*Quest*Test,*Retail*Test'`）宜待并行会话工作树收敛后执行
（当前工作树含其未提交的 tablelane 源码与 5 个 quest XML 在途改动，Maven/IDEA 共享 target/ 不宜并发）。
