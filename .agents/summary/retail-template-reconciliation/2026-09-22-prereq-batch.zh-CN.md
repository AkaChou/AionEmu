# PREREQ 批次交付记录（真端 finished_quest_cond 对账）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-22；状态：**实现完成、验收 PENDING**（未授权跑 Maven，未做客户机/运行期验证）
- 变更：25 个生产任务 XML（24 补前置 + 1 修正迁移漂移），1 个新增门禁测试 + 1 个合同基线
- 权威证据：真端服务端 `58Server/Map/XML/quest.xml`、Aion 5.8 客户端 `Quest_unpacked/quest.xml`、仓库旧库 `quest_data.xml`（QE-001）
- 逐行台账：`prereq-batch-ledger.tsv`

## 1. 判定口径（三层过滤）

1. **语义**：每个 `finished_quest_condN` 是 OR 分支，分支内逗号是 AND（QE-021）；仓库口径 = `prerequisites` ∪ `start-conditions` ∪ `start-condition-groups` 的 finished 并集。
2. **可达分支**：分支内只要有一个任务未移植，该分支在本服不可能被满足 → 按仓库既有策略（2026-09-17 `043426b47` 清理死分支）不声明悬空前置；否则保持声明。
3. **真值表**：对 1648 个真端有前置的任务做命题等价枚举，区分「仓库更松（漏前置）」与「仓库更严（多余前置）」。

结果（修复前）：等价 1553、更松 27、更严 68。
结果（修复后）：等价 1578、更松 3、更严 67。

## 2. 已改（25 个文件）

- 补前置 24 个：2533←2532、3050←3049、15471←15402、15551←15550、15552←15551、15553←15552、15554←15553、15563←15550、15595←15550、15673←15550、16823←16822、16824←16821、16825←16822、18035←18036、18821←18830、18993←18992、21004←21001、21080←21065、21201←21200、26823←26822、28035←28036、30719←30708、49004←49003、80343←80341
  - 写法：`<metadata><prerequisites><quest id="X"/></prerequisites>`（XSD 顺序：races/classes/gender/repeat 之后，items/rewards 之前）
  - 16823/26823 的 LEVEL_UP 迁移条件本来就带 16822/26822，本批把同一合同补到元数据层，使 NPC 接取路由同样受门禁
- 修正 1 个：**2641** 原 `<start-conditions>finished 2640</start-conditions>` → `<prerequisites>2619</prerequisites>`
  - 三源一致：真端 `finished_quest_cond1=Q2619`、客户端 `Q2619`、`quest_data.xml` `finished 2619`；2640 无任何来源支持 → 迁移漂移
- 补丁脚本：`apply_prereq_batch.py`（幂等：已有前置/已引用目标时中止）

## 3. 刻意不改

### 3.1 未移植分支（25 个任务，语义已等价）
1365、1510、1517、1518、2217、2371、2433、2486、2585..2588、2697、3102、3975、4079、4975、18911..18913、19047、28911..28913、29047。
真端的另一分支指向未移植任务（1036/1062/1094/1099/2013/2035/2038/2039/2053/2055/2092/2099/15352/18910/25352/28910），
本服该分支恒假，保留可达到支即与 OR 语义等价；删改皆为无意义改动。

### 3.2 阻塞（3 个任务，fail-open 登记）
1870←1868、2869←2868、2870←2868：依赖任务未移植，补前置会把任务永久锁死。
保持当前可接取状态；门禁在依赖任务进入目录后自动要求补前置。

### 3.3 多余前置（3 个任务，留待 PREREQ-STRICTER 子批次）
10507（多余 10500..10504）、20507（多余 20500..20505）为链式冗余（被 10506/20506 传递蕴含），无害；
**21296 多余 21248** 会挡住只完成 21295 的玩家（真端/客户端只要求 21295）→ 属真缺陷，但属「删前置」类改动，单独批次处理。

## 4. 新增门禁

- 基线：`src/test/resources/quest/quest-prerequisite-retail-contract.tsv`（2611 行真端 DNF 快照；生成：`build_prereq_contract_tsv.py`）
- 测试：`QuestPrerequisiteRetailContractTest`
  1. `everyPortedRetailPrerequisiteBranchIsExpressedByTheCatalog`：可达分支必须被目录表达（当前覆盖 1734 分支，跳过 31 未移植分支）
  2. `batchPrerequisitesMatchTheRetailConditionExactly`：本批 25 行精确锁定 + 2641 不得回退到 2640
  3. `unportedSingleBranchChainsStayOpenUntilTheDependencyIsPorted`：未移植单分支链 fail-open，移植后必须补齐

## 5. 验证边界

| 层 | 状态 | 证据 |
|---|---|---|
| XML 语法 + XSD | 通过 | `xmllint --noout --schema quest_definition.xsd`，25/25 validates；负向对照可判失败 |
| 前置语义审计 | 通过 | `audit_prereq_semantics.py`：looser 27→3，2641 漂移消失 |
| 前置图不变量 | 通过 | `check_prereq_graph.py`：6224 任务、0 自环、0 悬空、0 环路 |
| 门禁离线模拟 | 通过 | `simulate_prereq_gate.py`：0 违规；用 HEAD 版本反算恰好 25 违规 |
| Maven 聚焦测试 | **PENDING** | 未授权 |
| 客户机/运行期验证 | **PENDING** | 未执行 |

## 6. 待授权命令

```bash
mvn -q -Dtest=QuestPrerequisiteRetailContractTest,CompletedQuestPrerequisiteRegressionTest,QuestRetailStartMetadataGateTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest test
mvn -q -Dtest=SensoryAreaRideRowContractTest,QuestCradleReunionProductionFlowTest,QuestEnterZoneStartOwnerRegressionTest,LegacyTemplateMirrorRouteRegressionTest,QuestReportedRewardCoverageTest,RetailSingleStepRewardRowContractTest,QuestDialogOrderAuditTest,RepresentativeQuestAbilityTest,RemainingCapabilityDefinitionTest,QuestPacketOrderRegressionTest,QuestDialog31RegressionTest test
```
