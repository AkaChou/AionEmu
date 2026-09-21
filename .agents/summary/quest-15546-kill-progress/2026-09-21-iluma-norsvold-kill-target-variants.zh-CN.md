# 15546《[每日]雷欧娜的委托》击杀不计数根因与 Iluma/Norsvold 击杀目标族修复

- 日期：2026-09-21
- 状态：**实现完成 + 定向/全量门禁复核（无新增失败）/ 待客户端实机验收**
- 工作区：`/Users/mc/IdeaProjects/AionEmu-test`（未提交）

## 1. 现象

玩家接取 15546（ELYOS，66+，每日）后击杀任务说明中的怪物，客户端四个计数条（各 `/4`）不增长。

## 2. 根因（证据链）

1. **客户端说目标是谁**：`Aion 5.8 客户端 quest_q15546.html` 的接取对白与计数条目直接点名四只怪：
   `T_ElementalLightF_A_66_n`、`T_Daru_A_66_n`、`T_Popoku_As_A2_67_n`、`T_WoodTesinon_A2_67_n`，计数形态为 `[%5]/4 [%8]/4 [%11]/4 [%14]/4`。
   客户端契约表 `Quest_unpacked/quest_monster.csv` 的四行（SECTION_1..4）每行给出同族四个名字变体（base 66/67 + `T_` 66/67）。
2. **服务端登记了谁**：`quests/15546.xml` 的 12 条击杀路线只登记 `240475 / 240483 / 240495 / 240497`（LF6 base 66/67 模板）。
3. **世界里刷的是谁**：`spawns/Npcs/210100000_Iluma.xml`（ELYOS 可用地图，任务 NPC 835514 也在此）只刷新 `T_` 变体：
   `241656/241657`（星光精灵）、`241664/241665`（达鲁）、`241676/241677`（波波库）、`241678/241679`（木特西农）。
   四个 base 模板在 `spawns/**` 中**零刷新**；`600200000_Lakrum` 地图在 `world_maps.xml` 中已注释，属失效刷怪文件。
4. **运行时后果**：`QuestEngine.onKill` 先用 `getQuestNpc(npcId).getOnKillEvent()` 判断拥有者，该索引只会由编译后的击杀转换（`installProductionDefinitions`）填充；
   15546 没有登记任何世界真实刷新的 NPC，因此 typed dispatcher 永远收不到击杀事实（其他任务登记了同一批 `T_` 变体，但事件不会反向路由给 15546），`var1..var4` 恒为 0，客户端进度不动。

结论：**不是计数/打包/投影问题，而是击杀目标 ID 与客户端契约的变体集合不一致**——登记的是世界中不存在的基础模板，漏掉真正刷新的 `T_` 变体。

## 3. 影响范围（同类审计）

审计规则（`.agents/summary/quest-15546-kill-progress/audit_hard_broken_kill_routes.py`）：
击杀路线登记的目标全部无活跃地图刷新，但同族兄弟模板存在刷新 → 该任务击杀进度必然无法推进。

- `HEAD` 命中：**42 个任务**（2026-09-21 复核：对被并发修改的任务 XML 用 `git show HEAD:` 内容替换后重跑同一审计逻辑）
- 修复后当前工作区命中：**0 个**（先前记录为 1，命中项 `20529` 属用户并发文件，本次未触碰，复核时已不再命中）
- 本次修复覆盖 45 个任务：
  - Iluma/Norsvold 主区族：`15546 25546 25500 25501 25503 25504 42001 42002 80891 80892 80897 80898 80927 80928 80929`
  - 子区（B/B1/B2/C/D/E/G/I）族：`25506 25507 25509 25510 25512 25513 25515 25516 25518 25519 25521 25522 25524 25525 25527 25528 25530 25533 25534 25640 42003 42004 42005 42006 42103 42104 42105 42106 51077 51078`

## 4. 修复内容

1. **15546 / 25546（四计数器任务）**：每个计数器族的三条路线（priority 2 累加、priority 1 收口、priority 0 终击进 REWARD）从单 ID 改为客户端契约的四变体集合：
   - 15546 `SECTION_1..4`：`240475 240476 241656 241657` / `240483 240484 241664 241665` / `240495 240496 241676 241677` / `240497 240498 241678 241679`
   - 25546 `SECTION_1..4`：`240377 240378 241504 241505` / `240371 240372 241498 241499` / `240381 240382 241508 241509` / `240385 240386 241512 241513`
   - 同步 `<metadata><kills>` 声明到同一口径（QE-035：`<kills>` 必须与计数器同口径）。
2. **其余 43 个任务**：`<kill-npc>` 目标集合扩为「原集合 ∪ 客户端契约声明的全部变体 ID」，不改变任何计数门控（`below N-1` / `at-least N-1` / REWARD 收口保持不变）。
3. **门禁**：新增 `QuestIlumaNorsvoldKillTargetCoverageTest` + 契约快照 `src/test/resources/quest/iluma-norsvold-kill-target-contract.tsv`：
   - 生产 IR 的击杀目标集合必须与评审快照逐任务一致（快照由客户端 `quest_monster.csv` 名单经 `npc_template` 名称解析生成）；
   - 每个任务必须至少有一个目标落在 `world_maps.xml` 活跃地图的刷怪数据里（可直接抓住 15546 这类"登记了不存在刷怪"的缺陷）；
   - 15546/25546：世界里真实刷新的变体必须都能被 planner 路由，且有 4 个独立计数器、16 杀完成（4 族 × 4）。
4. **既有零售锚定测试对齐**：`QuestA03ShardRetailAlignmentTest` 原先要求 25533/25640 的击杀目标**精确等于**零售 `data_driven_quest.xml` 名单；
   该名单只是部分等级变体（25533=23 IDs、25640=12 IDs），客户端 `quest_monster.csv` 声明的是全量族变体（25533=175、25640=117，与快照逐条一致，且零售名单是其子集）。
   测试改为：零售名单必须被覆盖（`containsAll`）＋ 目标集合必须等于评审后的客户端契约快照；其余三个任务（23920/25698/21065）仍按零售精确集合锚定。

## 5. 验证

已执行（无构建）：

| 检查 | 结果 |
| --- | --- |
| `xmllint --schema quest_definition.xsd` | 45/45 通过 |
| 同类审计 `audit_hard_broken_kill_routes.py` | `HEAD` 43 → 工作区 1（仅用户并发文件 20529） |
| 客户端契约 + 活跃地图可达性自检（生成快照时） | 45/45 无缺口 |
| `git diff --check` | 通过 |
| IDE 文件检查（IntelliJ inspections） | 新测试类 0 错误 |

已执行（用户授权后，2026-09-21）：

```bash
mvn -o test -Dtest='com.aionemu.gameserver.questEngine.definition.QuestIlumaNorsvoldKillTargetCoverageTest,com.aionemu.gameserver.questEngine.definition.QuestMonsterProgressContractAuditTest,com.aionemu.gameserver.questEngine.definition.ClientQuestSectionAlignmentTest'
```

| 测试类 | 结果 |
| --- | --- |
| `QuestIlumaNorsvoldKillTargetCoverageTest` | 3/3 通过 |
| `QuestMonsterProgressContractAuditTest` | 17/17 通过 |
| `ClientQuestSectionAlignmentTest` | 6/6 通过 |

```bash
mvn -o test -Dtest='com.aionemu.gameserver.questEngine.definition.QuestIlumaNorsvoldKillTargetCoverageTest,com.aionemu.gameserver.questEngine.definition.QuestKillCounterRetailGateTest,com.aionemu.gameserver.questEngine.definition.QuestDefinitionCatalogManifestTest,com.aionemu.gameserver.questEngine.ProductionCatalogWhitelistVerificationTest'
```

| 测试类 | 结果 |
| --- | --- |
| `QuestIlumaNorsvoldKillTargetCoverageTest` | 3/3 通过 |
| `QuestDefinitionCatalogManifestTest` | 10/10 通过 |
| `ProductionCatalogWhitelistVerificationTest` | 通过：`PRODUCTION_COMPILE_OK=6189`、`FAILURES=0`、`INTERACTION_OBJECT_FAILURES=0`、`WHITELIST_VIOLATIONS=0` |
| `QuestKillCounterRetailGateTest` | 3/4：**失败项与本改动无关且早于本改动** |

`QuestKillCounterRetailGateTest#singleCounterQuestsRequireExactlyTheClientGate` 报
`quest 15101 is in the single-counter contract but uses [var0, var1]`。15101 的击杀路线按已验收的
SECTION_0 报告行合同写 `var0`（`.agents/summary/quest-acceptance/15101-2026-09-19-section0-report-row-client-accepted.md`，
HEAD 提交 `c44c50bd0`），而该门禁仍按"单计数器只能有一个字段"计数；15101.xml、契约快照与
`QuestKillCounterSimulator` 本次均未改动，故属既有门禁口径滞后，需单独处理（本次不动）。

> 备注：首次运行时出现一次 `NoClassDefFoundError: QuestKillCounterSimulator`（`target/test-classes` 陈旧），
> 重新编译后同一命令稳定通过。

### 5.1 全量 questEngine 门禁（2026-09-21，用户授权后）

```bash
mvn -o test -Dtest='com.aionemu.gameserver.questEngine.**.*Test'
```

结果：**1555 run / 5 failures / 6 errors / 1 skipped**（修复前同一命令为 6 failures / 6 errors，差额即本次修好的
`QuestA03ShardRetailAlignmentTest#huntTargetsAndStepsMatchRetailProgressInfo`）。11 个未通过项均已逐条归因，**无一项由本次改动引入**：

| 未通过测试 | 归因证据 |
| --- | --- |
| `QuestA03ShardRetailAlignmentTest` | 本次已修：期望值改为"零售名单 ⊆ 目标 = 客户端契约快照"（见 4.4） |
| `Quest25512ClientDialogAlignmentTest`（2 errors） | **既有缺陷**：把工作区 XML 换回 `HEAD` 版本后 `mvn -o surefire:test` 复现同样 NPE（`sourceNode()` 为 null） |
| `QuestLegacyMonsterHuntProductionFlowTest`（3 errors，25060/25090/25093/25409 系列） | **既有缺陷**：这些任务 XML 均为未修改文件，且各自含无 `source` 的自愈边（如 `25060.xml` 1 条），测试用 `sourceNode().equals()` 未做空判 |
| `Quest28808ClientDialogAlignmentTest` | 与工作区并发改动相关（`28808.xml` 为他人未提交改动），本任务未触碰该文件 |
| `QuestArchDaevaPromotionDefinitionTest`（2 failures，`var0` 5≠6） | 相关任务 XML 未修改，属既有失败 |
| `QuestWorkItemMigrationCoverageTest`（10526/20526 `<work-items>`） | 相关任务 XML 未修改，属既有失败 |
| `QuestKillCounterRetailGateTest`（15101 单计数器口径） | 既有门禁口径滞后，见上（15101 已按 SECTION_0 合同验收） |
| `QuestMovieAndDialogLoopRegressionTest`（15301） | 15301.xml 未修改，属既有失败 |

遗留的 10 个既有未通过项建议单独立项处理（本任务不顺手改，避免与并发工作区串味）。

### 5.2 记忆库沉淀（2026-09-21）

- 新增 Pattern `QE-048`（`KILL_TARGET_COVERS_CLIENT_VARIANT_FAMILY`，击杀目标必须覆盖客户端变体族且至少一个目标真实刷新），写入 `.agents/memory-bank/patterns/quest-engine.md`，并在 `systemPatterns.md` 路由；
  派生索引（`index.jsonl`、`symptom-index.md`、`.agents/summary/index.jsonl`）由 `sync_memory_bank.py` 重新生成，`verify_memory_bank.py` 三步全绿（`MEMORY_BANK_VERIFY_OK`）。
- 追加 Pattern `QE-050`（`STATE_IDENTICAL_EXECUTION_MUST_NOT_SYNC_QUEST_STATE`，状态未变化的执行不得下发任务状态更新），覆盖第 8 节的超额击杀缺陷；同样已路由并在 `verify_memory_bank.py` 三步全绿。
- Playbook 代表案例：**暂不写入**（规则 11/40：待客户端验收后才补，且不与并发工作区串味）。

客户端/实机验收：**未进行**（待授权门禁通过后由用户实机确认 15546 击杀计数）。

## 6. 残留风险与后续

- 其余 43 个任务的击杀路线的**数量门控**未改动，只放宽了"哪些怪算数"；若某些任务在客户端另有分段（多次交付）合同，需要后续逐任务实机确认。
- 修复路线只在事件注册层面生效；已持久化但停在 0 计数的存档会随下一次真实击杀正常推进（无迁移需求）。
- 全量 questEngine 套件仍有 10 个既有未通过项（含 5 个 `sourceNode()` 空判类测试缺陷，见 5.1），与本次修复无关，需单独排期。

## 7. 变更文件

- 任务 XML：45 个（见第 3 节列表）
- 新增测试：`src/test/java/com/aionemu/gameserver/questEngine/definition/QuestIlumaNorsvoldKillTargetCoverageTest.java`
- 修改测试：`src/test/java/com/aionemu/gameserver/questEngine/definition/QuestA03ShardRetailAlignmentTest.java`（25533/25640 期望值改为客户端契约快照）
- 新增测试（超额击杀）：`src/test/java/com/aionemu/gameserver/questEngine/runtime/Quest15546KillCounterSaturationFlowTest.java`
- 修改测试（引擎护栏）：`src/test/java/com/aionemu/gameserver/questEngine/runtime/QuestExecutionCoordinatorTest.java`（新增 `stateIdenticalExecutionDropsTheRedundantStateSync`）
- 修改引擎：`src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestExecutionCoordinator.java`（状态未变化且无必需动作时丢弃多余的任务状态同步）
- 新增契约快照：`src/test/resources/quest/iluma-norsvold-kill-target-contract.tsv`
- 审计与生成脚本：`.agents/summary/quest-15546-kill-progress/`

## 8. 追加修复：超额击杀不再下发"任务更新"（2026-09-21 同日）

### 8.1 现象与判定

把某一族打到 4/4（其余族未满）后继续击杀同一族怪：进度不再增长（这一点是对的），但客户端仍会闪一次"任务更新"。
判定：**是缺陷** —— 本次击杀没有改变任何任务状态，却下发了一次任务状态更新。

### 8.2 根因

1. 第 4 次击杀由"收口"自环路线写入：`variable-at-least varN 3 -> set varN 4`（15546/25546 四族各一条）。
2. 该守卫**没有上界**：计数器已经是 4 时仍然命中，`QuestMutationPlanner` 因此生成一份与当前 packed 状态完全相同的计划。
3. `QuestExecutionCoordinator.requiresStatePersistence` 判定无需持久化 → `statePort` 不写库（正确），
   但 `after-commit` 里的 `sync-quest-state PACKET_ONLY` 仍会执行 → `PlayerQuestStateSyncPort.sync()` 下发
   `SM_QUEST_ACTION.updateQuest(...)` → 客户端渲染成一次"任务更新"提示。
4. 全库审计 `.agents/summary/quest-15546-kill-progress/audit_saturated_kill_selfloops.py`：同一形状的自环击杀路线共 **26 条**、
   分布在 12 个任务（15546/25546、25406/25407/25408、25580、26802、30600、30610、10112/20112、13705、17510/27510）。

### 8.3 修复（两层）

1. **引擎层（根因）**：`QuestExecutionCoordinator` 在"状态未变化**且**没有任何必需动作"时丢弃多余的 `SyncQuestState`
   （新助手 `withoutRedundantStateSync`）；状态或持久副作用真正变化时同步照旧下发。
2. **XML 契约层（15546/25546）**：计数器改为精确边界 —— 累加路线 `variable-below varN 4`（第 4 次击杀自己收口为 4），
   收口路线改为 `variable-at-least varN 5 -> set varN 4`（只用于越界旧存档自愈）；饱和状态下的额外击杀不再命中任何路线。

### 8.4 新增门禁

| 测试 | 断言 |
| --- | --- |
| `Quest15546KillCounterSaturationFlowTest`（3 例，生产 IR + 假端口） | 1..4 次击杀把 SECTION_1 推到 1/2/3/4 且每次恰好一次 `PACKET_ONLY` 同步；第 5 次击杀 `handled()==false`、packed 不变、**零**提交后动作；第 1 族饱和时第 2 族照常计数并同步 |
| `QuestExecutionCoordinatorTest#stateIdenticalExecutionDropsTheRedundantStateSync` | 命中但状态完全相同的执行不得执行状态同步（引擎护栏本体） |
| `QuestIlumaNorsvoldKillTargetCoverageTest#saturatedCountersRejectExtraKillsWithoutMatchingAnyRoute` | 15546/25546 四族饱和后，该族 4 个变体的击杀不得命中任何路线 |

隔离证据：临时禁用引擎护栏后，`QuestExecutionCoordinatorTest` 该用例报
`expected: <[]> but was: <[SyncQuestState[mode=PACKET_ONLY]]>`；修复前运行流程测试，第 5 次击杀同样是这一条失败。

### 8.5 验证（2026-09-21，用户授权 Maven）

| 检查 | 结果 |
| --- | --- |
| `xmllint --schema quest_definition.xsd`（15546/25546） | 通过 |
| 修复前流程测试（缺陷复现） | 第 5 次击杀 `expected: <[]> but was: <[SyncQuestState[mode=PACKET_ONLY]]>` |
| 聚焦门禁（含 A03/契约快照/协调器/流程/生产目录/白名单） | **88/88 通过**，`PRODUCTION_COMPILE_OK=6189`、`FAILURES=0`、`INTERACTION_OBJECT_FAILURES=0`、`WHITELIST_VIOLATIONS=0` |
| 全量 questEngine | **1561 run / 5 failures / 6 errors / 1 skipped**：与修复前同一批 11 项既有失败，无新增 |
| 饱和自环审计 | **26 → 18**（15546/25546 的 8 条已收口；余 18 条由引擎护栏在行为层覆盖） |
| IDE inspections | 新增/改动文件 0 error |

### 8.6 边界与后续

- 引擎护栏只在"状态未变化且无必需动作"时生效：奖励、物品、货币等持久副作用照常下发同步。
- 余下 18 条 `variable-at-least` 收口自环在饱和后仍会匹配（会提交一笔空事务，但**不再**下发任务更新）；可按各任务客户端上限用
  `audit_saturated_kill_selfloops.py` 逐条收紧，属后续批次（本次未动，避免在没有逐任务客户端证据时改语义）。
- 客户端复测点：四族依次打满应逐次 1/2/3/4 提示；打满后继续击杀同一族**不应**再出现"任务更新"。
