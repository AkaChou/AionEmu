# B 类「页面已下发但按钮无路由」批量修复（5 个任务 + 2 个阻塞回归）— 2026-09-22

B 类是本轮优先项：客户端任务书的每一步都有一张页和一组按钮，服务端把整条链压成单个
`started(var0=0)`，页面照样下发、按钮却没有任何路由——玩家点下去没反应，任务卡死；
wiki 侧表现为每一步的 GM 命令都退化成 `//quest set <id> START 0`（1192 的同型缺陷）。

## 1. 修复合同

- 客户端 `quest_summary` 第 i 行 ⇒ 状态投影 `var0=i`（`//quest set <id> START <i>` 可精确跳步，领奖行是 REWARD 投影）；
- 行 i 的入口页与该行 NPC 取自客户端对话链，行内按钮（`HACTION_SETPRO<n>` / `HACTION_SELECT<n>_<m>` /
  `HACTION_CHECK_USER_HAS_QUEST_ITEM`）逐个在 XML 里落路由；
- 只有领奖行 NPC 拥有 `npc-complete`（`complete-reward-index` 指向本档奖励）；
- 旧存档自愈边：`enter-world + REWARD + 旧行号 → 领奖行号`。

## 2. 本批 5 个任务

| 任务 | 行数 | 状态 | 关键点 |
| --- | --- | --- | --- |
| 1430 | 2 | 已由 `cd7ce6447`（批次 12）提交 | 领奖行 Sonirim(203337) 的 `SETPRO1` 直接进 REWARD(var0=1)；接取 NPC 204630/204631/204632 |
| 2513 | 2 | 已由 `cd7ce6447` 提交 | 三个分支 NPC 各自 `SETPRO1/2/3` → REWARD(1)；领奖 NPC 204732 |
| 2962 | 3 | 已由 `cd7ce6447` 提交 + 本轮追加入口页修复 | 报告分支 `SETPRO3→reward1` / `SETPRO4→reward2`（`var1@offset 6` 两档领奖窗口 5/6）；`npc-complete` 两档 0/1 |
| 1643 | 5 | 本轮新增（未提交） | 行 1 是无按钮演出行；行 3 拆成独立 START `s3`（`QUEST_SELECT` 高亮行 3、`SET_SUCCEED` 进 REWARD）；领奖行 204545；工作物品 182201764 用 `remove-item ALL` 容错 |
| 4542 | 6 | 本轮新增（未提交） | 行 3 收集交付（`CHECK_USER_HAS_QUEST_ITEM` 成功/失败两档 + 扣 182215329）；行 4 换钥匙 182215330；行 5 说谎分支 `SETPRO6` 留在 `s5`、`SELECT_QUEST_REWARD` 领奖 |

1643 的领奖窗口由 `npc-complete` 的 `<preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>` 派生，
XML 里再手写一条同源同动作的 `SELECT_QUEST_REWARD` 自环会触发 `AMBIGUOUS_TRANSITION`（已实测）。

## 3. 阻塞项一：19064/29064 全目录编译回归（并行提交引入，本轮修复）

- `cd7ce6447` 重建 19064/29064 的领奖行与领奖 NPC 时，同时保留了 `<npc-complete ... >` 的
  `preview`（会派生 `USE_OBJECT` 与 `SELECT_QUEST_REWARD` 两条 `reward→reward` 路由）**和**一条手写的
  `SELECT_QUEST_REWARD` `reward→reward` 路由 → `AMBIGUOUS_TRANSITION: TALK_TO_NPC`，
  整个生产目录编译失败（`PRODUCTION_COMPILE_OK=6187`、`FAILURES=2`）。
- 修复：删除手写自环，交回 `preview` 派生（与 1643 同一口径，注释已写明原因）。
- 证据：`Probe` 在 HEAD 版本上 `FAIL ... AMBIGUOUS_TRANSITION`，修复后 `OK 19064 nodes=5 transitions=36`、
  `OK 29064 nodes=5 transitions=36`；行对齐审计的 `handovers` 列由 3 条降为 2 条（重复交付路由消失）。
- 归属：这是共享编译门禁的阻塞项（任何任务都无法验证），故在本批内一并修复；19064/29064 的领奖行/NPC 归属本身沿用批次 12 的结论。

## 4. 阻塞项二：2962 领奖态下发了步骤 3 的分支页（本轮修复）

- 症状：`QuestClientContractGateTest` 报 4 条 `BUTTON_WITHOUT_ROUTE` ——
  `reward1/reward2` 上的 `QUEST_SELECT(31)` 下发 `select3_1(1694)` / `select3_2(1779)`，
  而这两张页的可见按钮 `SETPRO3(10002)` / `SETPRO4(10003)` 与两页互跳（`1694`/`1779`）只在 `s2` 有路由；
  领奖态点下去没有落点。
- 修复：领奖态入口改下发本档领奖窗口 `select_quest_reward1(5)` / `select_quest_reward2(6)`
  （客户端页索引里 2962 确有这两页，页 13/14）；点任务行直接进本档奖励窗。
- 证据：门禁指纹 4 → 0；`QuestBClassRouteContractTest#twoBranchReportQuestKeepsBothRewardTiers`
  追加断言：领奖态任何路由都不得下发 `select3_1/select3_2`。

## 5. 4542 的中间交付基线登记（不重复校验已扣除的收集物）

- `QuestItemSourceContractGateTest#everyRewardEntryBranchVerifiesTheQuestsOwnCollectedItems` 报
  `4542 branch s5@204768 reaches the reward node without verifying [182215329]`。
- 证据：客户端第 4 行「消灭 <mob>，把钥匙交给 Esnu」= 行 3 的交付行（`CHECK_USER_HAS_QUEST_ITEM` 在 204808，
  `has-item` + `remove-item` 182215329）；第 6 行「向 Sleipnirr 报告」只是最终汇报。收集物已在行 3 校验并扣除，
  REWARD 边重复校验会与客户端语义冲突（QE-032 已评审同类：10010/4338/10530/20530 等）。
- 处理：登记到既有机制 `src/test/resources/quest/quest-item-handin-baseline.tsv`（13 → 14 条），**未改 XML**。

## 6. 验证证据（分层）

| 层 | 命令 / 工具 | 结果 |
| --- | --- | --- |
| 结构 | `xmllint --noout --schema quest_definition.xsd`（1643/2962/4542/19064/29064） | 5/5 validates |
| 编译 | 逐任务 `Probe`（QuestDefinitionXmlCompiler） | 1643 nodes=7/transitions=38、4542 9/53、2962 7/31、19064 5/36、29064 5/36 全 OK |
| 全目录编译 | `ProductionCatalogWhitelistVerificationTest` | `PRODUCTION_COMPILE_OK=6189`、`FAILURES=0`、`INTERACTION_OBJECT_FAILURES=0`、`WHITELIST_VIOLATIONS=0`（修复前 6187/2） |
| 客户端契约门禁 | `QuestClientContractGateTest` | 通过，新增指纹 0（修复前 4 条全在 2962） |
| 家族合同 | `QuestBClassRouteContractTest`（本批新增，7 例） | Tests run 7 / Failures 0 |
| 聚焦 Maven（用户授权） | `mvn -B test -Dtest=QuestMultistepChainContractTest,Quest1192StepChainContractTest,QuestBClassRouteContractTest,QuestClientContractGateTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,QuestWorkItemMigrationCoverageTest,QuestItemSourceContractGateTest` | Tests run 34 / Failures 1 / Errors 0；唯一失败为**既有**的 `QuestWorkItemMigrationCoverageTest#everyExecutableWorkItemQuestIsCleanedUpOnCompletion`（10526/20526 幻象 work item 164002347/164002348，HEAD 原状，另有两份历史报告已留档 defer，与本批 5 个任务无关） |
| 无路由推进动作审计 | `audit_unrouted_progress_actions.py`（全库） | 候选 67 → 62（行数 68 → 63），本批 1430/1643/2513/2962/4542 全部出列 |
| 行对齐审计 | `audit_reward_row_vs_client_steps.py`（全库；基线取 HEAD 的临时 worktree 重跑，已 `worktree remove --force` + `prune`） | ROW_ALIGNED 2631 → 2633、ROW_BEHIND 213 → 211、ROW_STATE_ALIGNED 2409 → 2411、ROW_WITHOUT_STATE 538 → 536；逐任务 diff 只有 1643（`ROW_BEHIND/ROW_WITHOUT_STATE`, visible `0` → `ROW_ALIGNED/ROW_STATE_ALIGNED`, visible `0 1 2 3 4`）与 4542（同形，visible `0 1 2 3 4 5`）；19064/29064 仅 `handovers` 列变化；30614 的差异来自并行未提交改动 |
| 客户端实机验收 | — | **PENDING**（静态/编译/门禁已过，尚未进客户端逐行走链） |

### 6.1 顺带发现：提交里的行对齐审计产物是陈旧的

`.agents/summary/quest-10527-reward-row/audit-output.tsv`（HEAD 版本）里 1643/2962/4542/19064/29064
仍记录修复前的 `ROW_BEHIND / ROW_WITHOUT_STATE / visible=0`，说明批次 12 改完 XML 后没有重跑全库审计产物。
本批已用当前工作树重跑覆盖该文件；HEAD 版本已备份到 `/tmp/audit-head.tsv`（临时，不入库）。

## 7. 状态与提交边界

- 本轮未提交改动（精确路径）：`1643.xml`、`4542.xml`、`2962.xml`、`19064.xml`、`29064.xml`、
  `src/test/java/.../QuestBClassRouteContractTest.java`、`src/test/resources/quest/quest-item-handin-baseline.tsv`、
  本报告；
- `1430.xml` / `2513.xml` 已由 `cd7ce6447` 入库（工作树无 dirty）；
- 工作树另有并行 agent 的 200+ 处改动（含 `21455.xml`/`30614.xml` 与其它 Java/XML），提交时必须按精确路径 stage，禁止 `git add -A`。
