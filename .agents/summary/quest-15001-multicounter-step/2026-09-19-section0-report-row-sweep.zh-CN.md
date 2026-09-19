# SECTION_0 报告行同族 sweep（244 + 2 任务批量修复）

- 日期：2026-09-19
- 触发：用户回报「15041 也是这样的问题」（与 15001 同型：击杀计数打满后任务说明停在击杀行）。
- 前置审计：`.agents/summary/quest-15001-multicounter-step/2026-09-19-section0-report-row-closure-audit.zh-CN.md`（脚本 + 逐行 CSV）。

## 1. 判定与执行

| 步骤 | 产物 |
|---|---|
| 审计脚本（Aion 5.8 客户端 `quest_monster.csv` + `quest_dialogs quest_q*.html` + `origin/history` 旧 handler 三源交叉） | `audit_section0_report_row_closure.py` |
| 批量修复脚本（结构同型才处理，否则 SKIP） | `apply_section0_report_row_sweep.py` |
| 修复前候选清单 | `section0-report-row-closure-candidates.csv` |
| 修复后残余清单 | `section0-report-row-closure-residual.csv` |
| 合同快照（回归测试输入） | `src/test/resources/quest/quest-section0-report-row-contract.tsv`（246 行） |

审计脚本本轮修正了自定义节点漏解析（16986/17526 的 `k1` 节点本来就投影 `var0=1`，此前被误报），候选从 250 行修正为 **248 行**。

## 2. 修复合同（每个任务四项）

1. `reward` 节点投影 `var0 = S+1`（客户端报告行；S = 客户端计数阶段）；
2. `started -> started` 击杀自环显式 `set var0 = 所在行`（自愈脏行索引，钉住来源节点投影）；
3. `started -> reward` 的击杀终击路线显式 `set var0 = S+1`，并保证 `LEVEL_AND_VISIBILITY_REFRESH`；
4. 新增无 source 的 `enter-world` 迁移修复路线（`status-is REWARD` + `variable-below var0 S+1` -> `set var0=S+1`），修复已持久化的旧存档。

## 3. 结果

| 分类 | 数量 | 处置 |
|---|---|---|
| 结构同型（脚本批量修复） | 244 | 已修复 |
| 多阶段（`started`/`step2` 行推进 + 报告行） | 2（18994/28994） | 已修复：`step2 -> reward` 与 reward 投影改为报告行 3 |
| 需要额外路线重建 | 2（15101/24153） | **未修复**（见下） |
| `REVIEW_LEGACY_NO_VAR0` | 24 | 待复核（其中 15060/16974 等旧 handler 用 `setQuestVar(1)` 或 `setQuestVarById(0, var+1)` 写入 var0，属审计正则漏判，需要二次分类） |
| `REVIEW_NO_LEGACY` | 7 | 待复核（无旧 handler 可对照） |

### 未修复的两个任务

- **15101**：客户端 3 行（0 找 NPC、1 击杀 10 只、2 报告）。XML 从未写 `var0`；旧 handler 在 NPC 804715 的 `STEP_TO_1` 分支写 `setQuestVarById(0,1)`，最后一击写 `setQuestVar(2)`。需要补「0->1 对话推进行」+ 终击写 2 + reward 投影 2，属**新增对话路线**，不在本次「报告行闭环」机械 sweep 范围。
- **24153**：客户端 3 行（0 找 Delris、1 击杀 1 只、2 找 Akigatan）。XML **完全没有击杀路线**（只有对话），旧 handler `onKillEvent` 对 5 个 ID 分别写 var0..var4。需要重建击杀路线与计数器布局，属独立修复。

## 4. 回归测试

`src/test/java/com/aionemu/gameserver/questEngine/definition/QuestSection0ReportRowContractTest.java`：

1. `contractSnapshotKeepsItsCoverage`：合同快照 246 行且 `report_row == client_stage + 1`；
2. `counterCompletionAdvancesSectionZeroToTheReportRow`：逐任务断言 reward 投影、continue 自环钉行、终击写报告行 + `LEVEL_AND_VISIBILITY_REFRESH`、`ENTER_WORLD` 迁移路线（`variable-is 0` 与 `variable-below report` 两种等价写法都接受）；
3. `killSimulationLeavesTheJournalOnTheReportRow`：用真实 `QuestMutationPlanner` 从全新 START 连续击杀，断言进入 REWARD 时 `var0 == 报告行`（需要前置对话/物品的任务跳过，保留 ≥ 200 个模拟下限）。

`QuestMonsterProgressContractAuditTest` 继续覆盖最初 14 个任务的合同。

## 5. 验证结果与边界

- 静态：246 个 XML 全部通过 XML 解析；IDE inspections 对新测试 0 error；审计脚本复跑后残余仅 `15101/24153`；Python 结构化复核（reward 投影、自环/终击 var0 写入、迁移路线）通过。
- 已执行（用户 2026-09-19 授权）：
  `mvn -q -Dtest='QuestSection0ReportRowContractTest,QuestMonsterProgressContractAuditTest,ClientQuestSectionAlignmentTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest' test`
  → PASS：`PRODUCTION_COMPILE_OK=6189`、`PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_INTERACTION_OBJECT_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`（5 个测试类 26 个用例）。
- 首轮失败与修正：新测试最初假设「终击路线来源节点一定是 `started`」，18994/28994 的计数阶段在 `step2` 节点，断言失败；已改为按 `SECTION_0==客户端阶段` 解析实际承载节点（`started`/`step2`/`k1`），修正后全绿（commit `265c63493`）。
- 未启动/重启服务端；未做客户端验收。建议代表任务：15041（单计数）、15471（周常）、15500（守护）、18994（副本多阶段）；`15101`、`24153` 修复后再验。

## 6. 第二轮：审计二次分类 + 残余处理（2026-09-19 追加）

- 旧 handler 证据提取扩展到 `setQuestVar(N)`（单参数即 var0）、`setQuestVarById(0, expr)` 与 `changeQuestStep(env, current, next, reward)`（next≠current 才算推进行）。
- 二次分类结果：原 24 行 `REVIEW_LEGACY_NO_VAR0` 中 20 行转为 `SAME_CLASS_CONFIRMED`，4 行确认为旧 handler 不写 var0（13945/23840/23945/25518）。
- 第二轮修复 **22 个任务**（合同同型，脚本 + 手工）：
  - 13 个 step-0 单计数：15060/15065/15074/15326/25090/25093/25325/25326/26837/26974/50076/50077/50078；
  - 5 个链式阶段（`s1..sN` 节点）：15601/15605/15608/25601/25605，同时把 `var0` 位段从 width=4 扩为 6-bit（客户端 SECTION_0 槽位，且报告行 6/7 原本超出 max）；
  - 4 个 legacy 不写 var0 但客户端合同要求报告行的任务：13945/23840/23945/25518（按客户端 `quest_summary` 报告步骤判定，属契约决策而非 legacy 复制）。
- 合同快照扩展到 **268 行**；`QuestSection0ReportRowContractTest` 三个用例全绿；生产门禁复跑 PASS（PRODUCTION_COMPILE_OK=6189、FAILURES=0、WHITELIST_VIOLATIONS=0）。
- 复跑审计后的**残余**（`section0-report-row-closure-residual.csv`）：

| 任务 | 阶段 | 缺口 | 状态 |
|---|---|---|---|
| 15101 | 1 | XML 从未写 var0；需要 0->1 的 NPC 对话推进行 + 终击写 2 + reward 投影 2（旧 handler 用 `STEP_TO_1` 分支，客户端页面动作需重映射到当前词汇） | EVIDENCE_REQUIRED |
| 24153 | 0 | XML 无任何击杀路线；需按旧 handler 的 5 个 NPC（213730/213788-213791 -> var0..var4）重建击杀计数链 | EVIDENCE_REQUIRED |
| 25304 | 2 | 只有 started(0) 与 reward(0)，缺 0->1->2 的中间行推进路线（旧 handler 无 var0 写入） | EVIDENCE_REQUIRED |
| 25604 | 2 | 有 s1..s4 节点但无击杀路线，reward 投影 var0=0；需要重建计数事件与 s4->reward 推进 | EVIDENCE_REQUIRED |
| 14252/18911/23918/24252/28911（7 行） | 0-2 | 组合节点（`a0b0c0` 类）任务，`SECTION_0` 在这些任务里不是单纯的说明行索引，客户端摘要的行选择语义需要 VarTable 证据 | EVIDENCE_REQUIRED |

- 至此：15001 家族 14 + 第一轮 246 + 第二轮 22 = **282 个任务** 已按报告行合同闭环；残余 4 + 7 行已明确标注所需证据。
