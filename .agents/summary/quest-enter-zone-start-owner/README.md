# Enter-Zone 迁移接取 owner 回归修复

日期：2026-09-19
状态：IMPLEMENTATION_DONE / MAVEN_VERIFIED（客户端验收待执行）

## 症状

- 任务 18300 在 NPC 804699 处点击第 10 页任务行后持续出现：
  `SM_DIALOG_WINDOW page=10 -> CM_DIALOG_SELECT action=31 questId=18300 -> SM_DIALOG_WINDOW page=10`。
- 服务端没有 `unaccepted + 804699 + QUEST_SELECT` 路由，`DialogService` 对 action 31 的未处理回退再次下发第 10 页。
- 旧 handler `_18300Floating_Death.java` 的 `addOnQuestStart(804699)` 是权威接取 owner；`onEnterZoneEvent` 只在 `START + var0 == 1` 时推进到 REWARD。

## 根因

- 迁移提交 `51b4cb971` 将 `registerOnEnterZone` 错误概括为“进入区域自动接取”，把原本的 `START` 阶段推进写成了 `unaccepted -> started`。
- 旧 handler 同时存在 `addOnQuestStart` 时，进入区域不是任务开始条件。
- 之前的顺序审计已记录 18300 的未达页面，但分类为 `EVIDENCE_REQUIRED`，没有把“缺少真实接取 owner”判为致命回归。

## 审计范围

静态审计全部生产 quest XML，筛出“当前仍存在 `unaccepted -> started` enter-zone 且 legacy handler 同时有 `addOnQuestStart` 与状态门控 onEnterZone”的任务：

- 1393、14123、15322、16800、17500、18300、21080、25322、27500、28300。

## 修复内容

- `18300` / `28300`：恢复 `NPC_START` 接取链；由 804820/804821 以 `SELECT1 -> SELECT1_1 -> SETPRO1` 推进 `advanced` 节点；进入 300240000 后转入 REWARD；799530 负责报告和领奖。
- `1393`：恢复 204041 的 `NPC_START`，补 `SELECT1_1` 和 `QUEST_ACCEPT_1` 页面；`SETPRO1` 触发飞行传送；进入 210020000 后转入 REWARD。
- `15322` / `25322`：把错误的 `unaccepted` enter-zone 接取改为 legacy `onAtDistanceEvent` 对应的 `at-distance` 接取（805330 / 805342）。
- `14123`：移除错误的 unaccepted enter-zone 接取；保留 203933 对话接取和 START 阶段区域重入生成 Peddler。
- `16800`、`17500`、`27500`：移除错误的 unaccepted enter-zone 自动接取；保留客户端已有的对话接取链。
- `21080`：恢复 799231 的 `NPC_START`，接取时授予 182207939；移除错误的 unaccepted enter-zone 接取；保留现有 SELECT4 报告链。

## 测试变更

- 更新 `Quest14123ZoneSpawnTest`：不再把 unaccepted enter-zone 当作合法接取路径，新增反自动接取断言。
- 新增 `QuestEnterZoneStartOwnerRegressionTest`：锁定 10 个任务的 legacy 接取 owner，并禁止 `unaccepted -> START` 的 enter-zone 自动接取。

## 已执行验证

- 10 个修改 XML 均通过 `ElementTree` 解析。
- 全目录静态审计：`remaining_suspicious=0`。
- IDE error lint：修改的 XML 与测试文件无错误。
- `git diff --check` 通过。

## 未执行验证

以下命令尚未执行，工作保持 `PENDING`：

```bash
mvn -q -Dtest=QuestEnterZoneStartOwnerRegressionTest,Quest14123ZoneSpawnTest,MigratedQuestRepairDefinitionTest,QuestResidualCounterLocksTest,Quest26800ClientDialogAlignmentTest,LegacyTemplateMirrorRouteRegressionTest test
```

```bash
mvn -q -Dquest.client.contract.failOnStaleBaseline=true -Dtest=QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,QuestClientContractGateTest,QuestDialogOrderAuditTest,QuestPageButtonAuditTest test
```

客户端 5.8 实测和真实 packet trace 验收未执行。

## 剩余风险

- 本批修复范围是“legacy 接取 owner / enter-zone 起始语义”这一类缺陷。
- 16800、17500、21080、27500 仍存在与本批无关的后续阶段建模差异，需按各自 legacy handler、客户端页面和真实 trace 单独审计。
- 18300/28300 已按 5.8 客户端页面链重建完整接取、推进、区域奖励路径。

## 扩展排查（同类问题覆盖面）

为确认“18300 类缺陷”不是单点问题，补充两次静态审计；脚本保留在本目录，均为只读分析：

```bash
python3 .agents/summary/quest-enter-zone-start-owner/audit_enter_zone_start_owner.py --legacy-rev 51b4cb971
python3 .agents/summary/quest-enter-zone-start-owner/audit_unreachable_start.py
```

### 1. legacy 接取 owner 保留情况（audit_enter_zone_start_owner.py）

以 `51b4cb971^` 中仍存在的 472 个 handler 为样本（205 个声明了 `addOnQuestStart`，201 个有对应 XML）：

| 分类 | 数量 | 说明 |
|---|---|---|
| A 无 QUEST_SELECT 应答（18300 症状） | 0 | 修复后已清零 |
| B 有应答页但无接取动作 | 0 | 修复后已清零 |
| C 仅自动接取、NPC 在 NONE 状态不可达 | 12 | 逐个复核后确认全部为设计如此 |

C 类逐个复核结论：

- `15322` / `25322`：legacy `onAtDistanceEvent` 即自动接取，现用 `at-distance npc-id` 表达，正确。
- `80030/80033/80034..80039`：EVENT 任务，legacy `onDialogEvent` 在 `NONE` 直接 `return false`，由 level-up / `event-quest-refresh` 接取，正确。
- `80945` / `80946`：legacy `onAtDistanceEvent` 自动接取（`at-distance npc-id="835303"`），正确。

审计过程中出现的其它“疑似”项均已排除：

- `26820`：legacy `addOnQuestStart(806075)` 是残留注册，真实 `NONE` 分支属于 806135（Corto），XML 与之一致。
- `2841`：legacy handler 用 271068，retail 合同（`legacy-quest-dialog-contracts.csv`）与 XML 均为 `805433 + SELECT1`，XML 正确。
- `25672`：legacy 契约就是进入 Iluma 自动接取、806116 只负责领奖，XML 一致。
- `17525/17540`：接取动作用 `actions="QUEST_ACCEPT_1 QUEST_ACCEPT_SIMPLE"` 表达，非缺失。

### 2. “任何已建模路径都无法接取”全目录扫描（audit_unreachable_start.py）

全部 6222 个 quest XML 中，33 个在 `NONE` 状态没有对话接取/自动接取事件：`11279-11286、1561、18849/18850、1993/1994、21281-21288、28849/28850、2993/2994、39707、49713/49715、80292/80293/80296/80297、89999`。

- 类别分布：`NON_COUNT` 11、`EVENT` 5、`FACTION` 3、其余为个别 QUEST/MISSION。
- 这些任务的接取由 XML 之外的系统完成（`RetailAreaEngine` 区域授予、`NpcFactions` 派系任务、EVENT 事件系统、遗物/物品授予），不属于本批缺陷。
- 仍未做真实客户端验证，保留为观察清单，不在本批修改范围内。

### 结论

修复后，上述两类静态扫描都不再命中“legacy 接取 owner 丢失 / 客户端点任务行无应答”的 18300 类缺陷。

## 验证结果（2026-09-19）

### 聚焦测试（PASS）

```bash
mvn -q -Dtest=QuestEnterZoneStartOwnerRegressionTest,Quest14123ZoneSpawnTest,MigratedQuestRepairDefinitionTest,QuestResidualCounterLocksTest,Quest26800ClientDialogAlignmentTest,LegacyTemplateMirrorRouteRegressionTest test
```

结果：6 个测试类 / 38 用例，0 失败（2 + 5 + 13 + 4 + 4 + 10）。

### 生产目录与客户端契约门禁（PASS）

```bash
mvn -q -Dquest.client.contract.failOnStaleBaseline=true -Dtest=QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,QuestClientContractGateTest,QuestDialogOrderAuditTest,QuestPageButtonAuditTest test
```

结果：5 个测试类 / 31 用例，0 失败；`PRODUCTION_COMPILE_OK=6189`、`PRODUCTION_COMPILE_FAILURES=0`、
`PRODUCTION_INTERACTION_OBJECT_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`，
`PAGE_NOT_IN_TASK_HTML=0`、`BUTTON_WITHOUT_ROUTE=0`（基线无需增删）。

首次运行该门禁时命中了 1 条新增致命指纹：

```text
BUTTON_WITHOUT_ROUTE|1393|started|204041|1013|1013|1002|started + NPC 204041 + 1013 -> started + page 1013
```

即 1393 客户端页 1013（`select1_1_1`）只有按钮 1002（`QUEST_ACCEPT_1`），而 `started` 状态没有对应路由。
按 NPC_START 的接取合同补上 `started + 204041 + QUEST_ACCEPT_1 -> shown page QUEST_ACCEPT_1` 后门禁转绿。

### questEngine 全包回归

`mvn -q -Dtest='com.aionemu.gameserver.questEngine.**.*Test' test`：1449 用例，仅剩两类非本任务问题：

- `QuestMonsterProgressContractAuditTest` 的 15001 断言失败 —— 属于并行进行中的 15001 修复任务（工作区另有 14 个 quest XML 与
  `QuestMonsterProgressContractAuditTest.java` 未提交改动，见 `.agents/summary/quest-15001-multicounter-step/`）。
- `ClientTaskScopeAuditTest` / `QuestExclusiveSiblingAttributionTest` / `QuestProductionJourneyTest` 的 6 个
  `NoClassDefFoundError` —— 与并行任务的 Maven 构建共用 `target/` 造成的瞬时类缺失；单独重跑这 3 个类全部通过（4 + 3 + 10）。

### 仍未执行

- Aion 5.8 客户端实测与真实 packet trace 验收（需服务端重启与真人操作）。

## 客户端验收（2026-09-19）

- 用户确认 18300 已可在客户端正常接取（“可以接了”）。

## 第二轮同类排查（新增两个审计脚本）

### 1. NONE 状态缺少任务列表行入口（audit_none_state_entry_actions.py）

`python3 .agents/summary/quest-enter-zone-start-owner/audit_none_state_entry_actions.py`

- 全目录命中 54 处“NPC 在 NONE 态有对话路由、但没有任务列表行入口动作”；
- 其中 7 处同时命中 retail TALK 合同（start NPC + open action 31）：
  `80000/798415`、`80001/798417`、`80034/799765`、`80035/799765`、`80036/799765`、`80037/799780`、`80230/831148`；
- 这 7 个都是 **自动接取**事件任务（`level-up` / `event-quest-refresh` / `use-item`），NONE 态只留下一条
  `QUEST_REFUSE_1` 遗留路由（ab65b3d45 的批量 refuse 对齐所加），客户端页面链 1011→4→1003→1004 完整；
- 按 Playbook 既有模式 `AUTO_START_KEEPS_NONE_DIALOG_FREE`（区域/事件/道具自动接取任务的 NONE 态不得新增对话路由），
  **本批不擅自补接取链**：需要先确认客户端是否真的在该 NPC 的任务列表里列出这些任务行；
- 同批 quest-order-audit 还记录这 7 个任务的 `started + NPC + 31 -> page 1011` 之后 `1007` 按钮无路由，
  属同一页面链缺口，一并留给下一批（需要 5.8 客户端逐页确认）。

### 2. retail TALK 合同 vs 当前 XML 的列表行入口（audit_retail_talk_entry.py）

`python3 .agents/summary/quest-enter-zone-start-owner/audit_retail_talk_entry.py`

- retail 合同 4902 条 TALK 起点，其中 257 个任务当前没有 quest XML；
- 449 个 start NPC 在当前 XML 里没有列表行入口；绝大多数属于“NONE 态完全没有该 NPC 路由”的存量
  `EVIDENCE_BLOCKED` 类（与 18300 的“有遗留路由却没有入口”指纹不同），保留为观察清单，不在本批修复。
