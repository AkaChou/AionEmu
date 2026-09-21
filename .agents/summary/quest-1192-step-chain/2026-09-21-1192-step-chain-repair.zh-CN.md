# 任务 1192 三步交付链修复 / Quest 1192 three-step hand-over chain repair

- 日期：2026-09-21
- 仓库：`/Users/mc/IdeaProjects/AionEmu-test`（生产任务定义 + 聚焦测试）
- 关联：QE-051（领奖行投影）、QE-032（交付路由）、wiki 仓库 `AionEmu-QuestWiki` 的 GM 命令生成
- 验收状态：**TESTS_PASSED**（聚焦测试 + 生产目录/白名单/客户端契约门禁通过）；**PENDING_CLIENT**（未实机验收）

## 1. 用户报障与结论

用户报「`/quest/1192` 每一步的 GM 都是 `//quest set 1192 START 0`」，追问后确认**服务端定义本身也是错的**：

- Aion 5.8 客户端 `data_unpacked/Dialogs/QUEST_Q1192.html` 的 `quest_summary` 声明三行：
  1. 到极乐世界把书信转交给拉比临托斯；2. 到贤者书库和科赛诺芬对话；3. 回贝尔特伦要塞和斯帕塔洛斯对话。
- 客户端为三步各定义了一条对话链，按钮动作互不相同：
  `select2/select2_1`（HACTION_SETPRO1）、`select3/select3_1`（HACTION_SELECT3_1 → HACTION_SETPRO2）、`select5`（HACTION_SELECT_QUEST_REWARD）。
- 修复前的 `1192.xml` 只有一个 `started(var0=0)` 进行中状态：把 `SETPRO1` 同时挂在 203701（拉比临托斯）与 203833（科赛诺芬）上并**直接进 reward**，
  `SETPRO2`、`SELECT3_1`、`SELECT_QUEST_REWARD`（斯帕塔洛斯）没有任何路由，`select3/select3_1/select5` 从未被引用。
  结果：跟拉比临托斯说完话就能跳过第 2、3 步直接领奖，wiki 侧表现为三步共用同一条命令。

## 2. 客户端证据（按钮 → 服务端 action，来自 quest-dialog-action-details.csv:32927-32936）

| 客户端页 | 按钮 | 动作 ID | 服务端 action | 修复前路由 |
| --- | --- | --- | --- | --- |
| select1 (1011) | 询问任务。 | 1007 | ASK_QUEST_ACCEPT | NPC_START 生成 ✅ |
| ask_quest_accept (4) | 接受。/ 拒绝。 | 1002/1003 | QUEST_ACCEPT_1 / QUEST_REFUSE_1 | NPC_START 生成 ✅ |
| quest_accept_1 (1003) | 结束对话。 | 1008 | FINISH_DIALOG | NPC_START 生成 ✅ |
| select2 (1352) | 拿出斯帕塔洛斯的书信。 | 1353 | SELECT2_1 | ✅（只挂在 started） |
| select2_1 (1353) | 结束对话。 | 10000 | SETPRO1 | ⚠️ 直进 reward（应进第 2 步） |
| select3 (1693) | 继续听下去。 | 1694 | SELECT3_1 | ❌ 无路由（页未被引用） |
| select3_1 (1694) | 结束对话。 | 10001 | SETPRO2 | ❌ 无路由 |
| select5 (2375) | 转告拉比临托斯的话。 | 1009 | SELECT_QUEST_REWARD | ⚠️ 错挂在 203701 上并直进 reward |
| select_quest_reward1 (5) | （无按钮） | — | 领奖窗口 | ✅ |

## 3. 修复内容（`src/main/resources/aion/data/static_data/quest_definition/quests/1192.xml`）

节点（QE-051：客户端任务书每一行都要有 `var0==行号` 的状态，领奖行 = 第 3 行；命名沿用 10527 的 `started`/`sN` 惯例，既有 `Quest1192ClientDialogAlignmentTest` 的 `started` 断言保持不变）：

| 节点 | 状态 | var0 | 含义 |
| --- | --- | --- | --- |
| unaccepted | NONE | 0 | 未接取 |
| started | START | 0 | 行 0：极乐世界找拉比临托斯（203701） |
| s1 | START | 1 | 行 1：贤者书库找科赛诺芬（203833） |
| s2 | START | 2 | 行 2：回贝尔特伦要塞找斯帕塔洛斯（203098） |
| reward | REWARD | 2 | 领奖行 = 第 3 行 |
| complete | COMPLETE | 2 | 完成 |

路由（`npc` → 事件 → 目标，after-commit 顺序照抄既有契约）：

- 接取：203098 / 203701 / 203833 三条 `NPC_START`（`unaccepted → started`，`selection-sources="unaccepted started s1 s2"`，`start-page="SELECT1"`）。
- 步骤 1：`started + 203701 + QUEST_SELECT → select2`；`+ SELECT2_1 → select2_1`；`+ SETPRO1 → s1`（清空工作物品 182200556，`count="ALL"`）+ `sync-quest-state(LEVEL_AND_VISIBILITY_REFRESH)` + `close-dialog`。
- 步骤 2：`s1 + 203833 + QUEST_SELECT → select3`；`+ SELECT3_1 → select3_1`；`+ SETPRO2 → s2` + sync + `close-dialog`。
- 步骤 3：`s2 + 203098 + QUEST_SELECT → select5`；`+ SELECT_QUEST_REWARD → reward` + sync + `SHOW_SELECT_QUEST_REWARD_WINDOW1`。
- 领奖：`npc-complete` 只保留 203098（旧定义的 203701/203833 领奖路由删除，客户端只在斯帕塔洛斯处结束）。
- 旧存档自愈（QE-051 规则 3）：无 source 的 `enter-world` 边，条件 `status-is REWARD` + `variable-is var0=0`，动作 `set-variable var0=2`，after-commit `sync-quest-state(LEVEL_AND_VISIBILITY_REFRESH)`。

## 4. 验收证据（全部已执行）

### 4.1 聚焦测试与门禁（`mvn -B -q test -Dtest=...`，用户 2026-09-21 授权）

| 测试 | 结果 |
| --- | --- |
| `Quest1192StepChainContractTest`（新增，4 个方法：三行↔三状态、三步路由与运行时 `QuestMutationPlanner`、SETPRO1/SETPRO2 不得直达 reward、领奖唯一化 + 旧存档自愈） | Tests run 4 / Failures 0 |
| `Quest1192ClientDialogAlignmentTest`（既有接取链断言） | Tests run 1 / Failures 0 |
| `QuestWorkItemMigrationCoverageTest#verteronReinforcementsDeclaresItsWorkItemAndTurnsItInAtLavirintos` + `#everyItemEnteringRewardIsHandedInOrDeclaredAsAWorkItem` | Tests run 2 / Failures 0 |
| `QuestDefinitionDirectoryLoaderTest`（状态图 BFS 门禁） | Tests run 2 / Failures 0 |
| `QuestDefinitionCatalogManifestTest` | Tests run 10 / Failures 0 |
| `ProductionCatalogWhitelistVerificationTest` | Tests run 1 / Failures 0 |
| `QuestClientContractGateTest`（客户端页面/按钮路由门禁；未引入新指纹） | Tests run 1 / Failures 0 |

门禁输出：`PRODUCTION_COMPILE_OK=6189 / PRODUCTION_COMPILE_FAILURES=0 / PRODUCTION_WHITELIST_VIOLATIONS=0`。

既有失败（与本次改动无关，供留档）：`QuestWorkItemMigrationCoverageTest#everyExecutableWorkItemQuestIsCleanedUpOnCompletion` 报告
`10526 legacy=[164002347] declared=[182216074] missing=[164002347]` 与 `20526 legacy=[164002348] declared=[182216086] missing=[164002348]`；
10526/20526 工作树未修改、断言列表不含 1192，属既有 work-item 基线，未在本轮处理。

### 4.2 XML 与审计脚本

| 检查 | 命令 | 修复前 | 修复后 |
| --- | --- | --- | --- |
| 行 ↔ 状态投影 | `python3 .agents/summary/quest-1192-step-chain/audit_reward_row_vs_client_steps.py 1192` | `ROW_BEHIND` / `MISSING_TAIL_ROWS` / `ROW_WITHOUT_STATE`，visible_state_var0=`0`，recovery=False | `ROW_ALIGNED` / `ALIGNED` / `ROW_STATE_ALIGNED`，visible_state_var0=`0 1 2`，recovery=True |
| 客户端推进动作路由 | `python3 .agents/summary/quest-1192-step-chain/audit_unrouted_progress_actions.py 1192` | 缺 `SELECT3_1 SETPRO2` | 无缺失 |
| XML 语法 / schema | `xmllint --noout` / `xmllint --schema .../quest_definition.xsd` | — | 通过 / validates |

### 4.3 Wiki 侧（`AionEmu-QuestWiki`）

- `pnpm gen:data`：/quest/1192 三步命令变为 `//quest set 1192 START 0`（拉比临托斯）、`START 1`（科赛诺芬）、`REWARD 2`（斯帕塔洛斯，领奖行）；
  阶段列表变成 `started(var0=0) → s1(1) → s2(2) → reward(2) → complete(2)`。
- 浏览器（dev 5173）实测三步各显示各自命令；`pnpm lint && pnpm test（10 文件 86 用例）&& pnpm check:data && pnpm build` 全绿。

## 5. 全库同类候选（未修，待分批决策）

新增审计脚本 `audit_unrouted_progress_actions.py`：客户端 HTML 里出现推进类动作
（`HACTION_SETPRO<n>`、`HACTION_SELECT<n>_<m>`、`HACTION_SELECT_QUEST_REWARD`）但任务 XML 没有任何路由 → 83 个任务；
与「行 ↔ 状态缺行」（`ROW_WITHOUT_STATE` / `BOTH_MISALIGNED` / `MISSING_TAIL_ROWS` / `INTERIOR_GAP`）交叉后 **67 个候选**（`audit-cross.tsv`）。

- 与 1192 完全同型（3 行 + 缺 `SETPRO2`/`SELECT3_1`、visible_state_var0=`0`）14 个：
  1183、1479、1483、1514、1721、1724、1938、2279、2428、2480、2767、2913、2917、4501（抽样 1721 已确认同形态：和 Ascalon 军团长 / Sakmis 军团长 / Erinyes 百夫长三步链）。
- 4 行同形态（缺 `SETPRO3`）21 个：1484、1540、1574、1582、1937、2278、2289、2515、2523、2538、2646、2692、2912、3093、3966、3968、3970、3973、4052、4905 等。
- 噪声边界：候选含 `NO_STATE`（9554-9557、1908）与带未使用备用页的任务；批量修复前必须逐个比对客户端页面链与 NPC，
  **不得**按动作名机械补路由。候选清单仅作为分批修复的输入。

## 6. 未完成项

- 客户端实机验收（PENDING_CLIENT）：需确认 203701 → 203833 → 203098 三步顺序推进、任务书三行高亮、领奖窗口只在斯帕塔洛斯处出现。

## 7. 变更文件

- `src/main/resources/aion/data/static_data/quest_definition/quests/1192.xml`
- `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest1192StepChainContractTest.java`
- `.agents/summary/quest-1192-step-chain/`：本文件、`audit_reward_row_vs_client_steps.py`（副本）、`audit_unrouted_progress_actions.py`、
  `audit-output.tsv`、`audit-run.log`、`unrouted-progress-actions.tsv`、`audit-cross.tsv`
