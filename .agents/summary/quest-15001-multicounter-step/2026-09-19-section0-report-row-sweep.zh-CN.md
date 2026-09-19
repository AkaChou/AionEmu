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

## 7. 旧存档路径加固：领奖对话框自愈（2026-09-19 追加）

- 为什么必须迁移：packed `var0` 已经持久化在存档里，XML 修复只影响之后的转换。旧存档停在 `REWARD/SECTION_0=计数行`，
  不会重跑击杀路线；而 `reward` 源节点的路由（`reward->reward` 对话、`npc-complete` 领奖）会被
  `QuestMutationPlanner.matchesSourceNode` 的投影匹配挡住 → 玩家点领奖 NPC 无响应。
- ENTER_WORLD 路线为什么够用（大多数情况）：迁移路线在登录/换图/实例切换时触发，服务端部署本就伴随重启，
  玩家重连即自愈。
- 剩余暴露：**跨部署保持在线**（热更 XML、只 reload 定义而不重连）或在同一会话内已经进入 `REWARD/SECTION_0=计数行`
  的玩家，不会触发 ENTER_WORLD，表现为「任务显示 REWARD、点 NPC 没反应」。
- 加固（本次）：`apply_section0_dialog_repair.py` 为 **244** 个任务复制其自身的 `reward->reward` 对话框响应，
  追加一条 source-less 修复路线（`status-is REWARD` + `variable-below var0 报告行` → `set var0=报告行` +
  原响应页）：
  1. 旧存档第一次点领奖 NPC 即自愈并直接看到原本的奖励页；
  2. 正常存档条件不成立（var0 已是报告行），行为不变；
  3. 迁移路线保留，幂等。
- 剩余 24 个任务没有可复制的 `reward->reward` 对话框路线，仍只依赖 ENTER_WORLD 迁移（清单：23759-23770、
  23840/23841/23844/23845、23945、24153 等；见 `apply_section0_dialog_repair.py` 的 SKIP 输出）。
- 门禁：244 个 XML 通过解析；`QuestSection0ReportRowContractTest`、`QuestMonsterProgressContractAuditTest`、
  `ClientQuestSectionAlignmentTest`、`ProductionCatalogWhitelistVerificationTest`、`QuestDefinitionCatalogManifestTest`
  复跑 PASS（PRODUCTION_COMPILE_OK=6189、FAILURES=0、WHITELIST_VIOLATIONS=0）。

## 8. 第三轮：残余 4 任务 + 7 行组合节点（2026-09-19 追加）

第二轮末把 4 个残余 + 7 行组合节点标成 `EVIDENCE_REQUIRED`。第三轮用同一三源（Aion 5.8 客户端 `quest_monster.csv` / `quest_dialogs quest_q*.html`、静态 NPC 表、`origin/history` 旧 handler）把它们全部判定完毕，结论如下。

### 8.1 `SECTION_0` 的双语义判定规则（关键结论）

`SECTION_0` 在 5.8 客户端里有两种互斥语义，必须用 `quest_monster.csv` 的**计数条件写法**区分，不能只看任务名或奖励格：

| 判别式 | 语义 | 家族 |
|---|---|---|
| `SECTION_0` 出现在 `<N` 计数条件里（如 `SECTION_0<1`） | 该槽位**本身就是计数器**（链式或单行狩猎），`SECTION_0==S` 是链式门控 | 1102、18911/28911、23918、24153、24155、17106 等 |
| `SECTION_0` 从不出现在 `<N` 里，且出现 `SECTION_0==S` 门控 | 该槽位是**任务说明行索引**，报告行 = HTML `<step>` 数 - 1 | 15001、15101、25304、25604、14252/24252 等 |

因此原报告里「7 行组合节点任务需要客户端 VarTable」是**审计器缺陷**造成的误判：14252/24252 的行索引语义已由客户端 `<N` 写法给出，18911/28911 的 `counter-grid` 形状本来就正确，23918 只是怪物 ID 配错且少一维。三者都不需要 VarTable。

### 8.2 本轮修复

| 任务 | 缺口 | 本轮改动 |
|---|---|---|
| 15101 | 缺 `0->1` 对话推进行、无任何 `var0` 写入 | 新增 `hunt`（`var0=1`）节点、804715 `SETPRO1` 推进行、`hunt->hunt` / `hunt->reward` 击杀路线（终击 `var0=2`）、reward 投影 `var0=2,var1=10`、ENTER_WORLD 迁移 + 804715 报告行自愈 |
| 24153 | XML 完全没有击杀路线 | 重建为 `var0..var4` 五位 0/1 计数器（offset 0/6/12/18/24）+ `var5`（offset 30，SECTION_5 门控）；`started`(`var5=1`) --204784 SETPRO2--> `hunted`(`var5=0`)；5 只怪 213730/213788/213789/213790/213791 各写自己的 `var0..4`；204787 `SELECT_QUEST_REWARD` 进 reward（五段=1、`var5=0`） |
| 25304 | 缺 `0->1->2` 中间行推进与计数事件 | 重建 `started->s1->s2->s3->reward`；805340 `SETPRO1`/`SELECT2_1`/`SETPRO2` 三页对话、`CHECK_USER_HAS_QUEST_ITEM` 交出 182215850 进 `s2`、`counter var1 required=60`（7 只 233902-233908）+ 已满补给路线、`s3` 三段对话 + `SET_SUCCEED` 给 182215874 收 182215850 进 `reward`、805339 领奖 + 报告行自愈（`var0=4`）+ ENTER_WORLD 迁移 |
| 25604 | 缺计数事件与行推进 | reward 投影改 `var0=5,var1=3`、complete 加 `var1=0`、新增 806115 报告行自愈 + ENTER_WORLD 迁移 |
| 14252 / 24252 | 旧 grid 把行号与每行计数混在同一组 var 里 | 重建为 `r0/r1/r2/r3` 行节点（`var0=0/1/2/3`）+ `var1` 每行计数；三行击杀路线（213775/236924 → 213780/236929/237263 → 237275）；`r3 -> reward`（`var0=3,var1=1`）；报告 NPC 832824/832820 后在 `r3` 领奖；保留旧 grid 存档迁移（ENTER_WORLD） |
| 23918 | 怪物 ID 全错（用了 EvGuard 234756/234759/234762/234765）且只有 4 维 | 改为 5 维 `counter-grid`（235559/235560/235561/235326/235327）+ 32 个 `a?b?c?d?e?` 组合 START 节点 + NPC_REPORT 802347 + ENTER_WORLD 迁移 + 报告行自愈 |

无需改动（判定为审计器误报）：**18911 / 28911**（`counter-grid` 形状本就正确，`SECTION_0<1;SECTION_5==0` 是链式计数）。

### 8.3 两个编译器口径修正（本轮踩坑）

1. **`reward -> reward` 的 `SELECT_QUEST_REWARD(1009)` 不能手写**：`QuestDefinitionCompiler.restoreRewardPreviewContract` 会为 `reward` 源追加一条 dialogId 通配（-1）的奖励预览路线；手写的 1009 路线与它同源同事件，编译期直接 `AMBIGUOUS_TRANSITION`。14252/24153/24252 已删除手写路线，交回编译器派生，`PRODUCTION_COMPILE_OK` 恢复 6189。
2. **报告 NPC 上的无 source TALK 自愈与既有 `reward -> reward` 路线同事件**：同样触发 `AMBIGUOUS_TRANSITION`。因此 14252/24153/24252 只保留 ENTER_WORLD 迁移（重登/换图即自愈）；15101/25304/25604/23918 的报告 NPC 没有既有同事件路线，TALK 自愈保留。

### 8.4 审计器收敛（脚本重写）

`audit_section0_report_row_closure.py` 本轮修掉三处口径缺陷，然后重跑全量 6189 个 EXECUTABLE：

1. **自闭合节点漏解析**：`<node .../>` 之前被跨节点正则把下一个节点的 `<var>` 张冠李戴，导致 13758/19631 家族的 `reward` 投影读空、误报 `SAME_CLASS_CONFIRMED`；已改为显式识别自闭合节点。
2. **行索引闭环判定过严**：原判定要求 `reward` 投影写 `var0`；现在接受两种等价写法——`reward` 投影写报告行，或 `reward` 投影不写而进入 reward 的转换事务动作显式写报告行（`QuestMutationPlanner` 不覆盖动作已触及的字段）。
3. **怪物归属判定过严**：XML 一条路线可覆盖客户端多条计数记录（13758/15546 家族），改为按 npc 集合相交做覆盖判定。

重跑结果（`section0-report-row-closure-residual.csv`）：

| 判定 | 数量 | 说明 |
|---|---|---|
| `ROW_INDEX_CLOSED` | 635 | 报告行合同已闭环（含本轮 15101/25304/25604/14252/24252） |
| `COUNTER_CHAIN_OK` | 797 | 链式/单行计数器家族（含 24153、23918、18911/28911） |
| `COUNTER_CHAIN_GAP` | 32 | 长尾：非本轮范围的链式计数缺口（1842-1844、2842-2845、13910、16962/17016、23703、23905-23917、24112、24201、28030-28033、28313、28915、30600/30610、39001/39002、49002） |
| `SAME_CLASS_CONFIRMED` | 46 | 旧 handler 写了 `var0` 但当前 XML 未闭环：其中 26 行是「reward 投影 0/偏一」、11 行是怪物无击杀路线、其余为多阶段偏移，需下一轮单独 sweep |
| `REVIEW_LEGACY_NO_VAR0` | 5 | 16974、23934、23935、23938、28952 |
| `REVIEW_NO_LEGACY` | 2 | 25082、25608 |

### 8.5 门禁

用户 2026-09-19 授权的命令复跑 **PASS**：

```
mvn -q -Dtest='QuestSection0ReportRowContractTest,QuestMonsterProgressContractAuditTest,ClientQuestSectionAlignmentTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest' test
```

→ 5 个测试类 32 个用例全绿；`PRODUCTION_COMPILE_OK=6189`、`PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_INTERACTION_OBJECT_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`。

新增回归：
- `QuestMonsterProgressContractAuditTest` 增加 6 个用例（15101、24153、25304、25604、14252/24252、23918）+ `unpack` 辅助；
- `QuestPacketOrderRegressionTest` 的 24153 断言从旧 `started -> reward` 改为 `started -> hunted` 门控 + `hunted -> reward` 领奖；
- `quest-section0-report-row-contract.tsv` 增加 `15101 1 2`（269 行），`QuestSection0ReportRowContractTest` 期望行数 268 -> 269。

未执行：客户端实机验收、服务端启动。`PRODUCTION_COMPILE_OK=6189` 只证明 XML 可编译，不等于运行时行为已验证。
