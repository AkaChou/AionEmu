# Phase 5-3：真端表 → 任务定义 IR 编译器（表优先 / XML 降级）


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

- 日期：2026-09-22；性质：新增**定义来源切换点**（生产路径未接线、未提交）
- 目标：`去掉 XML` 不是重写运行时，而是换**定义来源**——真端表能整表复现的任务由表编译出同一个 IR；
  真端表复现不了的任务（阶段位、串行链、怪集合不一致……）保留 quest-definition XML 降级。

## 1. 交付物

| 产物 | 说明 |
|---|---|
| `questEngine.retail.RetailSimpleHuntIrCompiler` | 表行（`RetailSimpleHuntPlan`）→ `QuestDefinition`/`CompiledQuestDefinition`；编译期判定归属，拒绝时给出机器可读降级码 |
| `RetailSimpleHuntIrCompiler.tableFirst(catalog, questId, shell)` | "表优先"入口：命中真端行且可整表复现 → 表驱动定义；否则 `Optional.empty()`（调用方继续用 XML） |
| `RetailSimpleHuntIrCompilerTest` | 6 个测试：单槽位 1102、双槽位 1517、调度层对拍（1102/1517/1365）、链式同构（1135）、降级判据、家族扫描（COUNTER_GRID 527 + KILL_CHAIN 168 任务） |
| `.agents/summary/scriptdll-quest-driver/phase5-3-rejections.txt` | 家族扫描中被拒绝的 25 个任务及逐条原因（降级集合快照） |

## 2. 编译语义：真端表 → IR（与 XML `<counter-grid>` 同构）

| 真端表 | 编译出的 IR |
|---|---|
| 槽位 N（`countN`/`monsterN`） | 一个 6 位 bit-field：`var(N-1)`，offset `6*(N-1)`，width 6 —— 直接落在真端/客户端的 SECTION 位上 |
| `countN` | 该字段的取值范围 `0..countN`，完成值 `goal = Σ countN<<6(N-1)`（`RetailHuntCounterLayout`） |
| `monsterN`（经 `name_desc` 解析） | 该槽位的 `KillNpc(npcId)` 边集合 |
| 击杀推进 | START 节点取完整笛卡尔积 `∏(countN+1)`；未满槽位的每只怪一条"该槽位 +1"的边，`after-commit = SyncQuestState(PACKET_ONLY)`；槽位打满后无边（与 DLL 的 `< countN` 判定一致） |
| 迁移期借用 | metadata、奖励、接取/报告对话、非线性路由仍取自现有 XML（`shell`）；网格 START 标签用 `a0b1` 形式重新生成，借用路由按**投影值**重定向 |

标签只用于编译期，绝不持久化（`QuestNode` 契约），因此表驱动结果与 XML 结果在客户端的 SECTION/`quest_vars` 表现一致。

## 3. 判定与降级判据（拒绝即降级到 XML）

| 拒绝码 | 含义 |
|---|---|
| `RETAIL_PLAN_ID_MISMATCH` / `RETAIL_PLAN_EMPTY` | 表行与任务 ID 不符 / 表行没有槽位 |
| `RETAIL_COUNT_OUT_OF_RANGE` | `countN` 不在 `1..63`（6 位计数器装不下） |
| `RETAIL_SLOT_HAS_NO_NPC` | 槽位怪物名一个 npc_id 都没解析出来（名索引缺名） |
| `OVERLAPPING_NPC_ID` | 同一 npc_id 被两个槽位声明（真端表异常） |
| `SLOT_FIELD_MISSING` / `SLOT_FIELD_MISMATCH` | 布局里没有 offset `6*(N-1)` 的字段 / 该字段不是 6 位或装不下 `countN` |
| `NODE_FIELDS_MISMATCH` / `NODE_SET_MISMATCH` | START 节点不是该网格的完整笛卡尔积（计数器与阶段位混在一个投影里） |
| `KILL_ROUTE_MISMATCH` | 击杀边带有额外条件/动作、直接跳到非网格节点，或怪集合/边数与真端表不一致 |
| `COMPILATION_FAILED` | 拼装结果未通过共享结构校验 |

判定只看**语义**，不看 XML 写法：1135 的 XML 是 9 级串行击杀节点（快照判为 `KILL_CHAIN`），
但其状态机与真端"单槽位 9 次"逐边同构，因此被接受为表驱动任务。

## 4. 家族扫描实测（快照 `quest-simple-hunt-retail-contract.tsv`）

| 家族 | 表可整表复现 | 降级 |
|---|---|---|
| `COUNTER_GRID` | **520 / 527**（回归下限写死在测试里） | 7 |
| `KILL_CHAIN` | **150 / 168** | 18 |

25 个降级任务按归因（逐条见 `phase5-3-rejections.txt`）：

| 归因 | 任务 | 说明 |
|---|---|---|
| XML 比真端表**多**怪（客户端 `quest_monster.csv` 口径） | 4713、18915、30113、30218、30318、30320、80602、80603、80605、80607、80608、80610 | 表为服务端口径，XML 扩了一族怪；以哪边为准需单独判定 |
| XML 与真端表**怪集合不同** | 11038、28030、39001、49002、80387、80690 | 表/XML 互斥差异，需逐任务核对 |
| XML 边数**少于**真端表 | 2357（20→10）、2360（104→52）、80403（631→474） | XML 可能少建模了一半怪/次数，属待确认的 XML 侧疑点 |
| START 投影含阶段位 | 24155（var0+var5）、80445（var0+var1） | 计数器与阶段位混投影，需阶段语义 |
| START 取值超出网格 | 80412（var1=2）、80440（var1=4） | 可能出现"已推进到奖励态"的中间投影 |

## 5. 对拍证据

1. **单任务**：1102（`count1=3`，怪 210133/210134）与 1517（双槽位 4+6）由表单独复现；1517 槽位 2 首杀
   在 `quest_vars` 上表现为 `1<<6`（客户端 SECTION_1）。
2. **调度层逐帧**：同一击杀序列在「XML 编译结果」与「表编译结果」上产生相同的 `nextStatus` /
   `nextPackedVariables`，打满后两侧同样无边；报告路由签名（目标为 REWARD 的转换）两侧一致。
3. **逐状态语义**：家族扫描对每个已接受任务遍历全部网格状态 × 全部真端怪，断言
   "可计数处恰有一条 `+1` 边、指向 `RetailSimpleHuntPlan.count` 给出的值；槽位打满后无边"。
4. **降级不静默**：拒绝都带稳定码，未知码直接判失败。
5. **对拍逐边**：`KILL_ROUTE_MISMATCH` 会打印缺失/多余边的样本（见第 4 节归因）。

## 6. 验证（Maven 已授权）

```
RetailSimpleHuntIrCompilerTest        6/6 通过   ← 本次新增（含 695 任务家族扫描）
RetailQuestCatalogTest                2/2 通过
RetailSimpleHuntTableTest             3/3 通过
RetailSimpleHuntPlanEquivalenceTest   1/1 通过
QuestSimpleHuntRetailContractTest     1/1 通过
```

命令：`mvn -q -Dtest='com.aionemu.gameserver.questEngine.retail.*Test,QuestSimpleHuntRetailContractTest' test`

未执行：全量 `mvn test`（无授权需求时不做）；服务端启动/实机客户端验收（本轮范围外）。
旁注：`com.aionemu.gameserver.ai.RetailPatternAI2Test` 在本机失败（`RetailPatternAI2:893` NPE），
与本次改动无关（`src/main/java/.../ai/` 未改动，属既有/他人工作）。

## 7. 边界与下一步

- **生产路径未切换**：没有任何运行时接线（`RetailSimpleHuntIrCompiler` 目前只被测试引用），
  默认行为不变；切换点已备好（`tableFirst`），默认关闭。
- **仍借 XML 的部分**：metadata（等级/种族/前序/奖励）、接取与报告对话、非线性路由。因此本阶段的
  价值是"证明真端表能整表复现击杀语义 + 给出降级判据"，还不足以直接删 XML。
- **Phase 6（去 XML 的必要条件）**：解析真端 `quest.xml` 元数据（display-name/min-max level/category/
  races/前序/奖励/接取与报告 NPC），把 shell 从"XML 定义"换成"真端元数据 + 通用对话路由"；
  之后按家族删 XML，降级集合以 `phase5-3-rejections.txt` 为初始清单。
