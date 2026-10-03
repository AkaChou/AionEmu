# P3 步骤 4 报告：同批删旧 + 旧金标按族重锚 + `con_quest` 接线

> **门禁产物清理提示（2026-10-03）**：本文件引用的门禁运行产物（`gates/*.log`、`*-red-classes.tsv`、`*-delta.tsv` 等）已按「只记录重要的过程内容、不记录门禁」口径清理，不再随仓保留；关键读数已内联于正文，复现请重跑对应聚焦套件，清理说明见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

> 主题：SimpleTalk（3152 行）原生车道的**残余收口**——删除旧 retail→IR 路径、把本族旧金标改锚真端表行、
> 接线真端 `con_quest`（0x1e 槽链式接取窗）、修正 native 系统发放判定的一处越权。
> 日期：2026-10-01。分支：`quest`。
> 前置：`p3/P3-STEP1..3-REPORT.zh-CN.md`、`p3-prereqs/simple-talk-codegen.md`（真端 DLL 侧槽位与 thunk 立即数）。

---

## 1. 交付 1：同批删旧（旧 retail→IR 路径整段退场）

| # | 对象 | 处置 | 证据/替代 |
|---|---|---|---|
| 1 | `retail/RetailSimpleTalkDefinitionCompiler.java` | **删除** | 行模型与语义全部由 `NativeQuestTableLoader.SimpleTalkRow` + `SimpleTalkHandler` 承担 |
| 2 | `retail/RetailSimpleTalkTable.java` | **删除** | 同上；行装载由 `NativeQuestTableLoader` 单一事实来源提供 |
| 3 | `RetailQuestDriver.compileSimpleTalk` + `simpleTalkTable` 字段/构造参数 + `compileByFamily` 的 talk 分支 + `retailOwnedSimpleTalk` | **删除** | 族已 100% 原生；残留分支是第二事实来源 |
| 4 | `retail/RetailSimpleTalkGateTest.java`（814 行） | **删除** | 断言面全部是旧编译器输出；等价不变量已迁入 `tablelane/SimpleTalkRowAlignmentGateTest`（逐行复算）与 `SimpleTalkNativeFamilyGateTest` |
| 5 | `RetailSystemGrantDispatchTest` 的 talk 侧 | **重锚** | 由 `RetailSimpleTalkTable` 改为 `SimpleTalkHandler`（原生车道）读类别哨兵行 |

**保留边界（不属本批）**：`RetailClientAcceptNpcSets` / `RetailClientHandinNpcSets` 与两张客户端集合 TSV
仍被 `SimpleUseItem` / `SimpleItemPlay` / `DataDriven` / `CombineTask` / `SimpleCollectItem` 五个未切换家族消费，
按 §0.1.5 属过渡台账，随各自家族切换批删除（不在 P3 内单独删除，避免制造双通道）。

## 2. 交付 2：native 系统发放判定的越权修复（本批新发现）

`SimpleTalkHandler.isSystemGranted` 原判据为 `routes ∧ grantKind.knownGrant()`，而
`RetailGrantKind.knownGrant()` **包含 `_challengetask_`**（挑战任务在本服只有完成回调、没有受理入口）：

- 后果：`NpcFactions.sendDailyQuest()` 的 `SimpleTalkHandler.grantSystemStart()` 首判会替挑战任务行建档，
  与「挑战任务行不得被系统发放」的既定合同相反（旧断言因落在 typed 目录、对已切换行恒为 false 而无法发现）；
- 修复：判据改为 `grantKind != NPC ∧ grantKind.grantable()`（`grantable()` 正是枚举里「本服已有发放入口」的定义：
  NPC / FACTION / AREA / WORLD）；
- 守门：`SimpleTalkRowAlignmentGateTest.familyCountsAreFrozenOverTheWholeTable` 按新判据逐行复算；
  `RetailSystemGrantDispatchTest.challengeTaskSentinelRowsAreNotSystemGranted` 增加 native 侧负例（0 命中）。

## 3. 交付 3：`con_quest` 链式接取窗接线（真端 0x1e 槽）

**真端事实**（`p3-prereqs/simple-talk-codegen.md` §1/§6）：`con_quest` 列由**交付 NPC 节点**上的 `0x1e` 槽消费，
thunk 立即数 = 下一环 quest id（例：14275 `con_quest=1469` → `FUN_180df8df0` 调用 `mgr+0x1a8(player, 0x5bd)`）。

**本车道等价物**：本车道接取路由按 NPC 建表，故「下一环的接取 NPC == 本行交付 NPC」即窗口成立，
不需要第二套路由。全表 492 行独立复算结论（`SimpleTalkRowAlignmentGateTest.chainAcquireWindowsCloseAtTheRewardNpc`）：

| 目标类型 | 行数 | 判定 |
|---|---|---|
| 目标在本表内 | 308 | 全部满足 `target.acquired == source.reward`（窗口已由目标自身那一行实现） |
| 目标在同批族表内（Hunt/CollectItem/CombineTask/UseItem/ItemPlay/SerialHunt） | 89 | 全部满足同一不变量 |
| 目标为本服用物品接取行（无接取 NPC 列） | 2 | 冻结例外：80196→80197、80200→80201（归 SimpleUseItem 家族批） |
| 目标无任何真端表行 | 95 | 其中 70 行的 owner 是 XML 车道（由 XML 行负责）、25 行不在本服宇宙（无物可发放） |

**实现**：`SimpleTalkHandler` 新增 `conQuest(questId)` 装载面与 `unresolvedChainQuestIds()` fail-closed 证据面；
构造期对「目标由本族路由且接取名已解析」的行逐行验证 `target.acquireNpc == source.rewardNpc`，不闭环即登记
（当前空集 = 全表闭环）。**不新增路由**：下一环的接取路由永远由下一环自己那一行提供（单 owner 不变量）。

## 4. 交付 4：旧金标按族重锚（§8.9）

本批把 P3 步骤 2/3 引入的 34 个红类全部处理到位（详见 §5 门态）：

| 类别 | 类数 | 处置 |
|---|---|---|
| 单任务对齐金标（ClientDialogAlignment / RetailAlignment） | 25 | 生成器重写为真端表行锚（步骤 3 已完成） |
| P3 新增族级/契约金标 | 3 | `RetailSimpleTalkGateTest` 删除；`QuestDefinitionCatalogManifestTest`(26930)、`RetailSingleStepRewardRowContractTest` 重锚原生车道 |
| 物品/工作物品回归金标 | 3 | `Quest2110` / `QuestWorkItemMigrationCoverageTest`(1192) / `QuestHaramelItemCollectingRegressionTest` 改锚真端表行 + `quest.xml` 交付通道 |
| 运行期流程金标 | 3 | `QuestRepeatLifecycleTest`（步骤 3）、`Quest80487ProductionFlowTest`、`QuestInteractionObjectContractGateTest` 重锚 |
| 本批追加 | +2 | `Quest30314RetailAlignmentTest`、`QuestDialog31RegressionTest`（talk 行部分） |

重锚统一形态：**真端表行（接取/交付 NPC、中继步、发放/回收、交付门、过场、类别哨兵）+ 族级页阶梯
（4 / 1352 / 1693 / 2034 / 5 / 1008）+ 原生进世界自愈**；禁止保留 IR 形状断言（节点名/条件/动作/页链）。

## 5. 门态（可复跑）

| 门 | 命令 | 结果 |
|---|---|---|
| tablelane 全包（含新增 `SimpleTalkRowAlignmentGateTest` 5 例、`NativeQuestStartPortTest` 8 例） | `mvn -o -q test -Dtest='com.aionemu.gameserver.questEngine.tablelane.*Test'` | **全绿** |
| 启动/派发门 | `QuestProductionStartupGateTest`(2) / `RetailQuestDriverOverlayTest`(5) / `QuestEngineNpcDialogDispatchTest`(6) / `QuestEngineRuntimeCompositionTest`(13) | **全绿** |
| 重锚类 | 本批 9 类 27 例（含 `SimpleTalkRowAlignmentGateTest` 5） | **全绿** |
| 系统发放门 | `RetailSystemGrantDispatchTest` 7 例 | 5 绿 + **2F**（`retiredSimpleHunt...` / `retiredArea...` 属 §10.3 阻塞 1 的 P1/P2 旧族金标，非本批范围） |
| 聚焦套件 | `mvn -o test '-Dtest=*Quest*Test,*Retail*Test'` | **1678 例 / 162F + 185E = 347 红 / 117 类** |

**聚焦套件对比**

| 时点 | 用例 | 红（F+E） | 红类 | 日志 |
|---|---|---|---|---|
| P2 收口（基线） | 1675 | 204F + 120E = 324 | 118 | `gates/2026-10-01-focused-run-p2.log` |
| P3 切换批 | 1678 | 164F + 226E = 390 | 152 | `gates/2026-10-01-focused-run-p3.log` |
| **P3 步骤 4（本批）** | 1678 | 162F + 185E = 347 | **117** | `gates/2026-10-01-focused-run-p3step4.log` |

**对 P2 基线：REMOVED 1（`Quest30314RetailAlignmentTest`）/ ADDED 0** ⇒ 本批未新造任何红类。
`missing production quest definition` 错误数：P3 230 → 本批 **158**（P2 基线为 68，全属 Hunt/Serial/DataDriven 族）。

## 6. 残余（P3 步骤 5，不在本批）

P2 基线 117 红类里有 **10 个纯 talk 归属类 / 36 例**，其失败形态是 talk 行已移出 typed 目录后的
`missing production quest definition`（P2 已红，故不计入「新增类」，但断言面必须按 §8.9 重锚）：

- `QuestProductionAcceptProtocolRegressionTest`(1913)、`RetailClientAcceptEntryPageTest`(1101)
- `GrowthQuestDialogPageAlignmentTest`(80369)、`QuestRewardTitlePrerequisiteAuditTest`(2511)
- `QuestMovieEventDefinitionTest`(80016)、`QuestCharmedEventDefinitionTest`(80030/80033)
- `QuestLunarEventDefinitionTest`(80034)、`QuestMultistepChainContractTest`(1183)
- `QuestRewardItemGateTest`(49603)、`EarlyElyosQuestRegressionTest`(1117/1118/1131/1141/1156/1158/1414/1691)

另有 7 个**混合类**（talk 行与 Hunt/Serial/ItemPlay/DataDriven 行同批断言），须与 §10.3 阻塞 1 的
QE-112 原子落地一并重锚：`QuestRefactorRepairRegressionTest`、`PlayerQuestStartEligibilityPortTest`、
`QuestMovieAndDialogLoopRegressionTest`、`QuestItemPlayGrantGateTest`、`QuestDraupnirNpcVariantContractTest`、
`QuestPrematureRewardRouteExclusionTest`、`QuestBatchReportNpcAlignmentTest`。

## 7. 边界与未做

- **未启服**：无服务端启动、无 T3 全员回归；上线前 `PENDING_SERVER_RUN`。
- **客户端验收 `PENDING_CLIENT`**：建议代表任务 1131（真端行：接取 Hyacinte(203097) → 中继 Shugo_LF1a_01 →
  交付 Nadaelo(203101)，接取发 182200506、第 1 步换 182200507）。
- **未动 QE-112 在飞文件**：`RetailSimpleHuntDefinitionCompiler`、`RetailDataDrivenDefinitionCompiler`、
  `quest_definition_catalog.xml`、3122/3123/4122/4123、`retail-data-driven-*`、`RetailHuntLadderShape.java`、
  `ZzIrDumpProbeTest.java`、`.agents/summary/quest-dd-hunt6-ladder/`。
- **中间产物**：`ZzP3ProbeTest.java` 等临时探针已删除，未入仓。
- **实测口径**：§5 全部门态在**共享工作区**上实测（QE-112 在飞切片仍在工作区：`RetailSimpleHuntDefinitionCompiler`
  +279 行、`RetailDataDrivenDefinitionCompiler`、`quest_definition_catalog.xml` 等），因此数字是「工作区实测」而非「净树」；
  §8.15 的净树基线冻结仍待 QE-112 落地或裁决（§10.3 阻塞 1）。
