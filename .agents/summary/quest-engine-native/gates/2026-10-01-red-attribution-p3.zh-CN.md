# 聚焦套件红灯归因（P3 SimpleTalk 切换批，2026-10-01）

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 命令：`mvn test '-Dtest=*Quest*Test,*Retail*Test'`（日志：`gates/2026-10-01-focused-run-p3.log`）


## 1. 总量对比

| 时点 | 用例 | 红（F+E） | 红类 | 日志 |
|---|---|---|---|---|
| P2 收口（基线） | 1675 | 204F + 120E = 324 | 118 | `gates/2026-10-01-focused-run-p2.log` |
| **P3 切换批（本次）** | 1678 | 164F + 226E = 390 | 152 | `gates/2026-10-01-focused-run-p3.log` |

## 2. 归因结论

本次红灯增量 **100% 来自 SimpleTalk 家族被切到原生车道后的旧 IR 金标**：
这些测试按 `ProductionQuestDefinitions.definition(<talk id>)` 断言该任务在 typed 目录里的 IR 形状，
而 P3 切换批已按 §8.3「同批删旧」把 3152 行移出旧 IR（`RetailQuestDriver` 不再注册 `retailOwnedTalk`），
旧金标必须按 §8.9「真端表行 + 客户端页动作」重锚。**不是原生车道回归。**

依据：
1. 全部新增红类都在 `questEngine.definition.*` / `questEngine.runtime.*` / `questEngine.retail.RetailSimpleTalkGateTest`，
   报错形态为 `missing production quest definition <id>`（typed 目录已无该行）；
2. 原生车道自身门禁全绿：`SimpleTalkNativeFamilyGateTest` 7/7、`NativeQuestTableLoaderTest` 10/10、
   tablelane 套件 69/69、启动与派发门禁 27/27（见 `p3/P3-STEP2-REPORT.zh-CN.md` §4）；
3. 基线 118 类红全部保留在集合内（`REMOVED 0`），说明本次没有掩盖任何旧债。

## 3. 新增红类清单（34 类，待按 §8.9 重锚）

- `com.aionemu.gameserver.questEngine.definition.Quest1141ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest1152RetailAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest1163ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest1192ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest13305ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest13902ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest16922ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest18800ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest18802ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest1936ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest2110WorkItemRegressionTest`
- `com.aionemu.gameserver.questEngine.definition.Quest2150ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest2151ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest23902ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest26922ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest28625And28626ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest28800ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest28802ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest30312RetailAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest30315RetailAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest3103ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest4913ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest50009ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest51009ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.QuestDefinitionCatalogManifestTest`
- `com.aionemu.gameserver.questEngine.definition.QuestHaramelItemCollectingRegressionTest`
- `com.aionemu.gameserver.questEngine.definition.QuestKaligaCollectionClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.QuestMinionTutorialRetailAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.QuestWorkItemMigrationCoverageTest`
- `com.aionemu.gameserver.questEngine.definition.RetailSingleStepRewardRowContractTest`
- `com.aionemu.gameserver.questEngine.retail.RetailSimpleTalkGateTest`
- `com.aionemu.gameserver.questEngine.runtime.Quest80487ProductionFlowTest`
- `com.aionemu.gameserver.questEngine.runtime.QuestInteractionObjectContractGateTest`
- `com.aionemu.gameserver.questEngine.runtime.QuestRepeatLifecycleTest`

## 4. 已消失的红类（0 类）

无。

## 5. 与 QE-112 门禁债的关系

基线 118 类红（旧 IR 金标门禁债）属 QE-112 在飞切片的外部阻塞，按 §10.3 阻塞 1 处置；
本次新增的 34 类属 **P3 批次自身**的重锚任务，须在 P3 标记完成前清零或按族重锚到位。

## 6. 重锚计划（按 §8.9：真端表行 + 客户端页动作）

共同根因：这些金标的取数入口是 `ProductionQuestDefinitions.definition(id)` / `RetailQuestDriver.current()`，
而该任务已在 P3 切换批移出 typed 目录。重锚方向（**不得**为兼容而重建旧 IR 形状）：

### 单任务对齐金标（ClientDialogAlignment / RetailAlignment）（25 类）

- `com.aionemu.gameserver.questEngine.definition.Quest1141ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest1152RetailAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest1163ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest1192ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest13305ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest13902ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest16922ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest18800ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest18802ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest1936ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest2150ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest2151ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest23902ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest26922ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest28625And28626ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest28800ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest28802ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest30312RetailAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest30315RetailAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest3103ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest4913ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest50009ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.Quest51009ClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.QuestKaligaCollectionClientDialogAlignmentTest`
- `com.aionemu.gameserver.questEngine.definition.QuestMinionTutorialRetailAlignmentTest`

### 族级/契约金标（SimpleTalk 族与本批直接相关）（3 类）

- `com.aionemu.gameserver.questEngine.definition.QuestDefinitionCatalogManifestTest`
- `com.aionemu.gameserver.questEngine.definition.RetailSingleStepRewardRowContractTest`
- `com.aionemu.gameserver.questEngine.retail.RetailSimpleTalkGateTest`

### 物品/工作物品回归金标（3 类）

- `com.aionemu.gameserver.questEngine.definition.Quest2110WorkItemRegressionTest`
- `com.aionemu.gameserver.questEngine.definition.QuestHaramelItemCollectingRegressionTest`
- `com.aionemu.gameserver.questEngine.definition.QuestWorkItemMigrationCoverageTest`

### 运行期流程金标（3 类）

- `com.aionemu.gameserver.questEngine.runtime.Quest80487ProductionFlowTest`
- `com.aionemu.gameserver.questEngine.runtime.QuestInteractionObjectContractGateTest`
- `com.aionemu.gameserver.questEngine.runtime.QuestRepeatLifecycleTest`

### 执行口径

1. 单任务金标：改为断言真端表行事实（`NativeQuestTableLoader.requireTalk(id)` 的 NPC 对/中继链/物品列）
   + 该行在原生 handler 上的页阶梯（接取 4/1003/1004、中继 1352/1693/2034、报告 5/10、完成 1008）
   与交付门（`workItems(id)`），客户端页 id 仍以 `HtmlPages.xml` 出处为准；
2. 族级金标（`RetailSimpleTalkGateTest` 等）：随同批删旧（阻塞 8）删除或改写为原生族门；
3. 运行期金标：改为驱动 `SimpleTalkHandler.onDialog`（假背包端口可注入）断言状态/页/物品三面；
4. 所有重锚必须同时删除旧编译器依赖，避免出现「假绿」：重锚后的测试不得再引用 `RetailSimpleTalkDefinitionCompiler`。
