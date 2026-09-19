# 任务 15101 SECTION_0 报告行闭环修复：客户端验收记录（ACCEPTED）

```text
quest: 15101「Time For Your Close Up」（ELYOS，min-level 64，category IMPORTANT）
user acceptance confirmation: 用户 2026-09-19 回复「15101 验收完成」，未限定分支或步骤，按项目规则视为该任务的完整游玩验收
server launch mode: 用户管理的服务端实例（本轮未启动、停止或重启服务端）
repository commit: c44c50bd0（修复提交：15101 XML 重写 + 回归测试）；报告 NPC 自愈路线与本文档随后单独提交
working tree: 本任务路径已提交；工作区仍有其他并行任务脏文件（不属本任务范围）
Aion 5.8 client/data provenance: Aion 5.8 客户端解包数据。
  - Quest_unpacked/quest_monster.csv 第 3951 行：`15101,Progress(SECTION_0==1; SECTION_1<10)`，怪物 `lf5_j_tauricre_64_an` / `lf5_j_tauricre_65_an`
  - data_unpacked/Dialogs/10000_19999/quest_q15101.html：quest_summary 3 个 <step>（0 交谈 / 1 击杀 / 2 报告），报告行 = 3 - 1 = 2
  - 仓库内映射证据：docs/quest/client-dialog-mapping/quest-dialog-action-details.csv（15101 行：select1 1011 → HACTION_SETPRO1 10000「点头」；select_success 10002 → HACTION_SELECT_QUEST_REWARD 1009「报告任务」）
npc template/object: 接取 NPC 804711；推进行/报告 NPC 804715；击杀目标 235939、235940（运行时 objectId 本轮未采集）
map/instance: 天族野外任务；world ID 本轮未采集
```

## 修复前的缺口（对照三源）

- 客户端要求：第 0 行「与 804715 交谈」→ `SECTION_0==1` 后进入击杀行（`SECTION_1<10`）→ 第 2 行报告。
- 旧 handler（`origin/history` 的 cygnea `_15101Time_For_Your_Close_Up.java`）：`QuestDialog.STEP_TO_1` 分支写 `setQuestVarById(0, 1)`；`onKillEvent` 写 `setQuestVarById(1, +1)`，满 10 只时 `setQuestVar(2)` 并进 REWARD。
- 修复前生产 XML：只有 `started(0)` 与 `reward(0)`，**从未写 `var0` 也从未写 `var1`**，既没有 0→1 的对话推进行，也没有击杀计数路线 —— 玩家停在击杀行且计数恒为 0。

## 修复内容（commit c44c50bd0）

- 新增 `hunt` 节点（`var0=1`）作为击杀行；`started` 保留 `var0=0`。
- 804715 `QUEST_SELECT(1011)` 显示客户端页面 `SELECT1`；`SETPRO1(10000)` 在 `var0==0` 时推进到 `hunt`（`set var0=1` + `PACKET_ONLY` + `close-dialog`）。
- 击杀路线按客户端门控拆分 priority：`hunt->hunt` priority 1（`var1<10`，`set var0=1` + `increment var1`）与 `hunt->reward` priority 0（`var1>=9`，`set var0=2` + `set var1=10`，`LEVEL_AND_VISIBILITY_REFRESH`）。
- `reward` 投影 `var0=2, var1=10`；满计数后可在 804711/804715 走 `SELECT_QUEST_REWARD(1009)` 报告。
- 旧存档迁移：无 source 的 `ENTER_WORLD` 路线（`status-is REWARD` + `var0<2` → `set var0=2` + `LEVEL_AND_VISIBILITY_REFRESH`），以及报告 NPC 804715 上的 TALK 自愈路线（同条件 → `DEFAULT_SUCCESS`），覆盖「跨部署保持在线」不触发 ENTER_WORLD 的存档。

## 预期与实际的字段级合同

- source state/status/vars：接取后 `START`，`var0=0`，`var1=0`。
- action/page/button：804715 的 `select1(1011)` → `HACTION_SETPRO1(10000)` 点头。
- expected response：同一次交互内 `var0` 由 0 推到 1，客户端任务说明切到击杀行；随后每杀一只 `var1` +1 且 `var0` 保持 1；第 10 只写 `var0=2, var1=10` 并进入 `REWARD`，任务说明切到「报告 804715」行，可领奖完成。
- actual response（用户实机，2026-09-19）：用户确认「15101 验收完成」，即上述路线在客户端可完整游玩；未限定分支或步骤，按项目规则视为整任务验收（含推进行、击杀计数与报告领奖）。

## 验证边界与门禁

- 已执行（用户 2026-09-19 授权，修复当轮与本文档同轮复跑）：
  `mvn -q -Dtest='QuestSection0ReportRowContractTest,QuestMonsterProgressContractAuditTest,ClientQuestSectionAlignmentTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest' test`
  → PASS：5 个测试类 32 个用例全绿；`PRODUCTION_COMPILE_OK=6189`、`PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_INTERACTION_OBJECT_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`。
- 任务级回归：`QuestMonsterProgressContractAuditTest#quest15101DialogUnlocksTheKillRowAndTheLastKillReachesTheReportRow` 用真实 `QuestMutationPlanner` 断言 804715 `SETPRO1` 把 `SECTION_0` 推到 1、10 次击杀逐次推进 `SECTION_1` 且终击落到报告行 2；合同快照 `quest-section0-report-row-contract.tsv` 收录 `15101 1 2`（269 行）。
- startup health：本轮未采集服务端启动日志（用户管理进程）。若出现 `Can't initialize typed quest engine` / `QuestCompilationException` / `AMBIGUOUS_TRANSITION` / production catalog compile failure，本验收不成立。
- runtime logs / protocol trace / screenshots：`not captured`（用户仅回复验收完成）。
- 未做实机复验的同批任务：24153、25304、25604、14252、24252、23918 与 15101 同批修复，但属于各自独立的客户端语义家族，尚未逐个实机验收。

## 结论

```text
acceptance status: ACCEPTED
matched Pattern: `KILL_COUNTER_COMPLETION_ADVANCES_JOURNAL_ROW`（报告行合同）+ `SECTION_ZERO_COUNTER_VS_ROW_INDEX`（SECTION_0 双语义判定；代表提交 c44c50bd0）
matched fields: 行索引由 SECTION_0 承载、reward 投影写报告行、终击同事务写报告行并 LEVEL_AND_VISIBILITY_REFRESH、旧存档 ENTER_WORLD/TALK 自愈
differing fields: 15101 比 15001 家族多一段「0→1 对话推进行」（SETPRO1）与 10 杀单计数器；不是多组并行计数
remaining risks: 同批其余 6 个任务未实机复验；报告 NPC 的 TALK 自愈路线未单独构造脏存档实测（仅编译期与条件互斥证明）
```
