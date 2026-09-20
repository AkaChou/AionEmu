# 2026-09-20 击杀计数投影锁第二批：15321 / 25608 / 27510

## 现象（用户报障）

玩家 `Ww` 接取 15321「凯西内尔之翼能做的事」后进入 s1 击杀阶段（805332 交接 30 只），
**只有第一只怪计入，之后每次击杀都不算，任务不往下**。
客户端任务书计数停在 1，服务端无拒绝日志——事件根本没有 owner 认领。

## 根因：source 节点投影锁死计数自环

运行期路由匹配（`QuestMutationPlanner#matchesSourceNode`）用 **source 节点投影与当前 packed 变量做全等匹配**：

```java
Map<String, Integer> actual = layout.unpack(snapshot.packedVariables());
return source.projection().variables().entrySet().stream()
    .allMatch(entry -> entry.getValue().equals(actual.get(entry.getKey())));
```

15321 的 s1 节点当时投影 `var0=1, var1=0`，而计数自环 `s1 -> s1`（priority 1，`variable-below var1 29`）
自增 `var1`：

1. 第 1 只怪：state=(var0=1,var1=0) 与 s1 投影相等 → 计数 0→1；
2. 第 2 只怪：state=(var0=1,var1=1) ≠ s1 投影(var1=0) → 计数自环与收口边都 NO_MATCH；
3. 之后所有击杀都无人认领，任务永久停在 s1。

即：**计数字段被 source 投影钉死 = 计数自环只能执行一次**。这属于既有 `COUNTER_PROJECTION_LOCK`
缺陷族（cfddeead8 修了 135 个，后续批次 4711/30710/49702 见 `QuestCounterProjectionLockFollowUpTest`），
本次是第三批：修复 15321 时按族内规则做了全目录审计。

## 全目录审计（结构性判据）

判据：**自环转换（source == target）自增的字段不得出现在该 source 节点的投影里**。
审计脚本：`audit_counter_projection.py`（本目录）。

修复前命中 3 个任务（全目录 6200+）：

| 任务 | 阶段节点 | 计数自环 | 症状 |
|---|---|---|---|
| 15321 | s1/s3/s5/s7/s9/s11 | var1（30/30/10/30/30/30 只） | 每阶段只算第一只 |
| 25608 | step2 | var1（10 只 241235） | 只算第一只 |
| 27510 | s3 | var1（精英 10 只）+ var2（无名 boss 开关） | 只算第一只 |

同族已修参照：25321（15321 的魔族同型）阶段节点只投影 `var0`；15322/15324/15325/15326、
26802/30603/30613、4711/30710/49702 同为「START 源节点不投影实时计数字段」。

`audit_counter_projection.py` 同时给出修复前后行为对照（按 planner 规则模拟连续击杀）：

```
15321: 修复后处理 30/30，最终 var0=2,var1=0；修复前 1/30（第二只起停止计数）
25608: 修复后处理 10/10，最终 var0=3,var1=0；修复前 1/10
27510: 修复后处理 11/11，最终 var0=4,var1=0；修复前 1/11
```

## 修复

任务 XML（阶段节点只固定阶段位段，计数字段归转换所有）：

- `15321.xml`：s0–s11 去掉 `var1` 投影（对齐已修的 25321）；
- `25608.xml`：step2/step3 去掉 `var1` 投影；
- `27510.xml`：started/s1–s4 去掉 `var1` 投影。

共享编译期门禁（防止手写 transition 再犯；`<counter>` 积木原本已有
`COUNTER_SOURCE_PROJECTION_CONFLICT`，但迁移后的手写自环绕过）：

- `QuestDefinitionCompiler`：自环转换自增被 source 投影钉死的字段时抛
  `COUNTER_SELF_LOOP_PINS_INCREMENTED_FIELD`（新增 negative control
  `IncrementVariableDefinitionTest#selfLoopCounterRejectsIncrementingItsProjectedField`）。

测试：

- `QuestCounterProjectionLockFollowUpTest` 新增 15321/25608/27510 三条按真实 dispatcher 连续
  击杀的行为回归（每条都断言"每一只都被 handled"，修复前会停在第二只）；
- `RetailSequentialQuestFamilyTest` 的 `assertNode` 改为断言阶段节点只投影 `var0`；
- `QuestMutationPlannerTest.selfLoopIncrementSurvivesTargetProjection` 改为跨节点形态
  `incrementSurvivesTargetProjection`（自环 + 投影字段自增现在被编译期拒绝，原形态已不可能合法）。

## 验证状态

- 已完成：全目录 XML 解析 0 错误；结构性判据命中 0；IDE 静态检查四个改动文件无 error；
  审计脚本给出修复前/后行为对照。
- 已执行（2026-09-20 用户授权后）：`rtk mvn -q -Dtest=QuestCounterProjectionLockFollowUpTest,IncrementVariableDefinitionTest,
  QuestMutationPlannerTest,RetailSequentialQuestFamilyTest,QuestMonsterProgressContractAuditTest,
  QuestCounterSourceProjectionProductionFlowTest,QuestResidualCounterLocksTest,QuestDefinitionCatalogManifestTest,
  ProductionCatalogWhitelistVerificationTest -DfailIfNoSpecifiedTests=false test` → 11 个测试类 72 例 0 失败 0 错误（EXIT=0）；
  生产门禁 `PRODUCTION_COMPILE_OK=6189`、`PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_INTERACTION_OBJECT_FAILURES=0`、
  `PRODUCTION_WHITELIST_VIOLATIONS=0`。
- PENDING（需客户端）：`Ww` 实机复验 15321 s1 起 30 只怪逐只计数并推进。

## 遗留（本次未改，需单独证据批次）

1. 自环**写标记**（非计数）后落入无节点匹配状态的任务：2303、2411、2922、16800、18036、24112、28036。
   这些属于 `DIALOG_PROJECTION_LOCK` 形态（把 var0 写成 10/20 等客户端 section 标记），
   修复方向需先确认客户端 section 布局，不能照抄计数批次的改法。
2. E2E Journey 缺口：现有 Journey 对每个击杀 route 只派发一次事件，无法暴露"第二只起 NO_MATCH"，
   所以 15321/25608/27510 未出现在 `COUNTER_PROJECTION_LOCK` 分诊表里。
   建议 Journey 规划器对计数自环按阈值派发（或直接复用 `QuestKillCounterSimulator` 的封顶派发）。
