# 聚焦套件红线归因（P2 收口时点）

> 批次：2026-10-01（P2 SimpleSerialHunt 原生切换后，P3 开工前）。
> 命令：`mvn test '-Dtest=*Quest*Test,*Retail*Test'`（用户已就该计划全程授权测试）。
> 原始日志：`gates/2026-10-01-focused-run-p2.log`（含 clean 复核，见 §3）。
> 口径：**本文件只做归因，不改任何生产代码/门禁**；每条结论给出可复现判据。

## 1. 读数

| 项 | 值 |
|---|---:|
| 用例总数 | 1675 |
| Failures | 204 |
| Errors | 120 |
| 红用例合计 | **324** |
| 红灯测试类 | **118** |

## 2. 归因方法（可复现）

判据：**同一批切片改动中已被作者重锚的测试全部通过，未被重锚的全部红**。
工作区在飞切片（QE-112 / DD-Hunt 行阶梯）改了共享 IR 合成器
`RetailSimpleHuntDefinitionCompiler`（+279 行，被全家族编译器复用）与 `RetailDataDrivenDefinitionCompiler`。
该切片同时把下列门禁改成了新形状：

| 已被切片重锚的门禁 | 结果 |
|---|---|
| `QuestKillCounterRetailGateTest` | 绿 |
| `Quest13765RetailAlignmentTest` | 绿 |
| `CounterChainBriefingStageContractTest` | 绿 |
| `Quest15546KillCounterSaturationFlowTest` | 绿 |
| `RetailSimpleHuntEquivalenceGateTest` | 绿 |
| `QuestEngineNpcDialogDispatchTest` | 绿 |
| `SimpleHuntHandlerTest` / `SimpleHuntNativeFamilyGateTest` / `SimpleSerialHuntNativeFamilyGateTest` | 绿 |

⇒ 红灯集合 = **尚未按新形状重锚的旧 IR 金标**，不是原生车道回归。

## 3. clean 复核（排除 `target/` 陈旧产物）

`mvn clean test -Dtest='Quest19004RetailAlignmentTest,SimpleSerialHuntNativeFamilyGateTest,SimpleHuntNativeFamilyGateTest'`
→ `Quest19004RetailAlignmentTest` 仍红（`expected <Perikles's Insight> but was <Q19004>`），
原生两族仍绿。**红灯与构建缓存无关。**

## 4. 红类按根因分组（代表项）

### A. 网格/阶梯形状漂移（占比最大）

旧金标断言 `a0/a0b0` 网格节点与 `select2..select5` 页链；新形状是行阶梯 + `started/s1/reward`。

| 类 | 红/总数 |
|---|---:|
| `QuestLegacyMonsterHuntProductionFlowTest` | 33F/6E |
| `Quest2877To2887UrgentOrdersFlowTest` | 22F |
| `QuestDaevanionThreeStageFlowTest` | 8F |
| `QuestMultistepChainContractTest` | 5F |
| `Quest15400And25400KillCounterContractTest` | 5F |
| `QuestArchivesMissionCounterProductionFlowTest` / `QuestArchivesDualCounterProductionFlowTest` / `QuestCradleCounterProductionFlowTest` / `QuestDataDrivenHuntProductionFlowTest` | 各 4E（`nodeByKillProjection` 找不到旧投影节点）|

### B. 击杀上界重裁（`>63` 假宽计数的收口面）

真端事实（计划 §2.8）：10034/20506/25082 等实际需求 = 1。旧金标仍写 10/30/5。

- `Quest19636RetailAlignmentTest` / `Quest19640RetailAlignmentTest`：`kill counter ceiling expected 10 but was 1`
- `Quest25640ClientDialogAlignmentTest`（30→1）、`Quest25698ClientDialogAlignmentTest`（5→1）

### C. 缺 XML 定义（owner 已翻成 RETAIL_TABLE，定义文件已退役）

- `QuestNAndNKillCounterContractTest`（5）、`QuestNRetailAlignmentTest`（2）、`RetailSequentialQuestFamilyTest`、
  `QuestConquestOfferingSpawnDataTest`（25321.xml）、`RetailNonIrAxisGateTest`、`RetailRewardWindowRouteTest`
- 判据：`unable to load .../quests/N.xml` / `missing production quest definition N`

### D. 台账/清单门禁未随新表同步

- `RetailTsvManifestGateTest`：磁盘新增 `retail/table-source-provenance.tsv` 未登记进 manifest
- `RetailSystemGrantDispatchTest`：退役 `_area_`/哨兵行缺 SystemGrant 边（64 行 no-definition）
- `RetailSimpleTalkMigrationReviewContractTest`（3）、`RetailNonIrAxisGateTest.cappedQuestsAreNeverRetailDriven`

### E. 元数据/命名口径

- `Quest19004RetailAlignmentTest` 等：英文名人工资产（`Perikles's Insight`）vs 真端符号名（`Q19004`）。
  真端口径已在 `RetailQuestMetadataCompiler` 首部登记（"英文名为在库人工资产且运行时无消费方"）⇒ 金标需改判据，不是生产缺陷。

### F. 与本次迁移无关的既存红

- `RetailPatternAI2Test`（AI 侧 NPE）、`QuestNpcFactionRetailGateTest`（阵营日常池空）、
  `QuestProductionAcceptProtocolRegressionTest`（80028 页 4 vs 1011）、`QuestEventQuestBatchDefinitionTest`（Fayrefolk 名）

## 5. 对计划的含义

1. **不是原生车道回归**：tablelane 与启动门禁全绿（61 例）。
2. **是门禁债**：计划 §8 不变式 9/13 要求"门禁锚定真端表行与客户端页动作，不锚定 IR 指纹/当前形状"。
   324 条旧金标正是这条不变式的待清偿面，须**按家族在切换批内逐批重锚**，不得一次性批量改写。
3. **阻塞关系**：在飞切片未收口前冻结任何验收数字都会把这些红混入 P0a 基线（计划 §9 "QE-112 未提交"行）。
