# Quest Engine Patterns & Pitfalls (任务系统模式与避坑)

本文档记录 AionEmu 声明式 XML 任务系统、状态机与 NPC 交互的实战避坑经验。

> Pattern IDs: `QE-001`–`QE-085`
> card_status: ACTIVE; existing entries retain their historical evidence boundary
> scope: production quest XML/compiler, Quest runtime, and Aion 5.8 client/legacy evidence
> last_reviewed: 2026-09-22

---

## [QE-001] 一、权威基准与对比原则
<!-- pattern-metadata
status: CONFIRMED
scope: Quest legacy Handler comparison, XML metadata and NPC registration
first_seen: unknown
last_verified: 2026-09-14
symptom: 前置缺失、level-up 过早接取、NPC 注册或路由不一致
root_cause: XML migration loses prerequisite or NPC registration semantics from legacy Handler
fix_or_guardrail: Compare commit 911440146 first and restore missing prerequisites or NPC mappings
evidence: commit 911440146; src/main/resources/aion/data/static_data/quest_data/quest_data.xml; docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md
validation: static; production-catalog; case-specific runtime/client validation required
boundaries: If origin/history is unavailable record substitute evidence; static proof is not client acceptance
superseded_by: none
first_check: old Handler, quest_data.xml, production catalog
-->
- **历史权威代码基准**：
  排查任务流程 Bug、条件分支不推进或逻辑缺失时，必须以历史旧 Java Handler（Git commit `911440146`）作为权威业务逻辑对照物。
- **旧 Handler 提取与对照命令**：
  - 提取旧 Handler：`git show 911440146:src/main/java/quest/<file>`
  - 提取旧 NPC 注册列表：`git ls-tree -r 911440146` 提取后 grep `registerQuestNpc`，与新 XML 的 `npc-id` 比对。
  - 元数据对照：`src/main/resources/aion/data/static_data/quest_data/quest_data.xml`
- **Level-up 与前置条件缺失案例 (如 14013)**：
  - 迁移时 level-up 自动获取转换若漏掉 `quests-finished` 前置条件（旧 handler `defaultOnLvlUpEvent(env, 14010, false)`），会导致过早接取。
  - 修复方案：给 level-up 转换补充 `<quests-finished quest-ids="..."/>`。

---

## 二、核心状态机与变异规划器 (QuestMutationPlanner)

### [QE-002] Target 投影覆盖 Action 变量陷阱 (2026-08-12 修复)
<!-- pattern-metadata
status: CONFIRMED
scope: QuestMutationPlanner variable actions and target projections
first_seen: 2026-08-12
last_verified: 2026-09-14
symptom: var0 不增长、自环计数卡 0、variable-at-least 不触发
root_cause: Target projection overwrote fields already changed by transition actions
fix_or_guardrail: Track actionTouchedFields and let actions win over target projection
evidence: .agents/summary/quest-e2e/triage.md:83; src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestMutationPlanner.java:81; quest 18972
validation: static; focused-test; runtime/client validation not implied
boundaries: Applies to action and target field collisions; unrelated state projection rules still need separate proof
superseded_by: none
see_also: docs/quest/repair-playbook/PATTERNS.zh-CN.md (COUNTER_SOURCE_PROJECTION_NO_LOCK)
first_check: QuestMutationPlanner.build, action variable writes, target projection
-->
   - **现象**：同节点自环计数任务（如 18972.xml 的 `started→started` 计数、击杀怪、收集道具）在玩家交互后，计数卡在 0，`variable-at-least` 完成转换永不触发。
   - **根因**：`QuestMutationPlanner.build()` 原用 `variables.putAll(projection.variables())`，Target 节点声明的 var 会无条件覆盖 Transition Actions 里的 `SetVariable` / `IncrementVariable`。
   - **引擎机制**：现已改为记录 `actionTouchedFields`，Target 投影仅覆盖 Action 未触及的字段；同节点自环计数 Action 结果被完整保留。

### [QE-003] 完成路径残留 Work-Item 自动清理 (2026-08-14 修复)
<!-- pattern-metadata
status: CONFIRMED
scope: Quest completion and work-item cleanup in QuestMutationPlanner
first_seen: 2026-08-14
last_verified: 2026-09-14
symptom: CompleteQuest 后任务道具残留，Abandon 与完成路径行为不对称
root_cause: Completion path omitted the legacy questWorkItems cleanup that existed on abandon
fix_or_guardrail: Append completion cleanup unless the XML explicitly removes all required work items
evidence: commit 8baeb9232; src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestMutationPlanner.java:291; src/test/java/com/aionemu/gameserver/questEngine/runtime/QuestMutationPlannerTest.java:207; .agents/summary/quest-acceptance/14023-2026-08-28-client-accepted.md:32
validation: static; focused-test; runtime/client validation not implied
boundaries: Do not add duplicate removal actions when explicit count=ALL already expresses the contract
superseded_by: none
first_check: CompleteQuest mutation plan and work-items declarations
-->
   - **现象**：任务完成后玩家背包仍残留任务道具（如 1122 任务完成箱子仍残留 182200216「Pernos's Robe」）。
   - **根因**：旧引擎 `QuestService.setFinishingState` 在完成时无条件清理所有 `questWorkItems`；新引擎此前只在放弃（Abandon）时清理，完成（`CompleteQuest`）路径漏了清理。
   - **引擎机制**：`QuestMutationPlanner.build()` 现已对称增加 `appendCompletionWorkItemCleanup`，凡声明了 `<work-items>` 且未显式配 `remove-item count="ALL"` 的任务，完成时由引擎自动补齐清理。

## 三、NPC 报告链与 Var 节点守则

### [QE-004] 多 NPC 交付链系统性丢失修复
<!-- pattern-metadata
status: CONFIRMED
scope: XML conversion of multi-NPC quest reports and variable nodes
first_seen: unknown
last_verified: 2026-09-21
symptom: dialog 31 无路由、中间 NPC 丢失、NPC ID 错配、choice/fallback 冲突；中间报告步点击任务行后服务端只回 questId=0 第 10 页
root_cause: XML migration omitted intermediate NPC report nodes or copied NPC identities incorrectly
fix_or_guardrail: Restore START variable nodes, NPC_REPORT transitions and explicit choice/fallback handling; every legacy START_DIALOG state must have its explicit QUEST_SELECT(31) self-loop entry page (1352/1693/2034/2375/2716 etc.), and follow-up page buttons such as SELECT6(2716) must have their own route
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/1900.xml; src/main/resources/aion/data/static_data/quest_definition/quests/10525.xml; src/main/resources/aion/data/static_data/quest_definition/quests/20525.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestDialog31RegressionTest.java; docs/quest/client-dialog-mapping/quest-dialog-action-details.csv; .agents/summary/quest-10525-report-dialog31/2026-09-21-10525-s4-s5-report-dialog31.zh-CN.md; NPC_REPORT repair cases in this card
validation: static（1900 历史案例；2026-09-21 10525/20525 s4 QUEST_SELECT->2375 与 s5 SELECT6->2716 路由枚举、XSD 2/2、IDE lint、git diff --check 通过）；focused-test（2026-09-21 授权 Maven：QuestDialog31RegressionTest + QuestClientContractGateTest + QuestDialogOrderAuditTest + Quest10525TestimonyCounterContractTest + Quest10529BossKillCounterContractTest + QuestIncrementRangeContractTest 六类退出码 0）；production-gate/client-contract evidence required per quest
boundaries: METADATA_ONLY tasks cannot receive executable nodes until their catalog mode is changed
superseded_by: none
see_also: docs/quest/repair-playbook/PATTERNS.zh-CN.md (MULTI_NPC_HANDOFF_REWARD_OWNER, ORDERED_MULTI_NPC_REPORT_FLOW)
first_check: quest XML nodes, NPC_REPORT edges, catalog mode and client dialog mapping；动作 31 落在中间报告步骤时逐档对比 legacy START_DIALOG 的 sendQuestDialog 页面，交付成功页还需检查 SELECT6 等后续按钮路由
-->
   - **现象**：从 Java Handler 迁移到 XML 时，多 NPC + 多 var 档流程在 XML 中只剩起始 NPC，中间交付 NPC 丢失或 ID 错配（如 804871↔804870 孪生 ID 抄错）。玩家点击 NPC 发送 dialog 31 无匹配转换。
   - **标准修复模板 (参考 `quests/1900.xml`)**：
     - 中间 NPC 各为一个 START 状态 var 档节点（`s1`/`s2`...）；
     - dialog 31 自环显示历史页（1352/1693/2034/2375 等）；
     - 推进 dialog（10000/10001...）跨节点推进 var（目标节点声明 var 自动写入）；
     - 物品交换使用 `give-item` / `remove-item`；
     - `npc-complete` 的 choice/fallback 处理 `SELECTABLE_ITEM`（choice 占 8/9 时其余用 fallback dialog-ids="10..23"，避免 `DUPLICATE_DIALOG_ID`）。
   - **2026-09-21 案例**：10525 与镜像 20525 的 s4 收集报告页本应由 legacy `START_DIALOG(2375)` 进入，s5 交付成功页还要处理 `SELECT6(2716)`；20525 已有两条路由，10525 两条都漏，表现为 `动作=31 -> questId=0/页=10`。已补齐 10525 并由 `QuestDialog31RegressionTest#migratedQuestHandlersKeepLegacyTurnInDialogRoutes` 同时锁定两阵营。
     - 注意：目录清单 `mode="METADATA_ONLY"` 的任务不能有 nodes/transitions，恢复执行定义后必须在清单中改为 `EXECUTABLE`。

### [QE-005] 连续两步汇报误删中间 Var 节点 (7 个典型案例)
<!-- pattern-metadata
status: CONFIRMED
scope: Multi-step NPC_REPORT routes and intermediate quest variables
first_seen: unknown
last_verified: 2026-09-14
symptom: 多 NPC 连续汇报时 var0 卡 0、任务追踪 UI 不推进、CLIENT_PAGE_UNREACHED
root_cause: Intermediate var node was deleted during Handler to XML migration
fix_or_guardrail: Restore the intermediate var node and route through NPC_REPORT instead of USE_OBJECT
evidence: docs/quest/client-dialog-mapping/quest-dialog-action-details.csv; docs/quest/client-dialog-mapping/quest-order-audit.csv; seven listed cases
validation: static; client-contract; real-client validation required for acceptance
boundaries: The intermediate NPC and terminal NPC must be distinguished by client action mapping
superseded_by: none
see_also: docs/quest/repair-playbook/PATTERNS.zh-CN.md (ORDERED_MULTI_NPC_REPORT_FLOW)
first_check: client dialog action details, quest-order-audit and var node sequence
-->
   - **现象**：`6d8019d8f` 误删中间 var 节点，导致 `var0` 永远卡在 0、客户端任务追踪 UI 不推进。
   - **受影响任务**：
     - `1115` The Elim's Message：删 `v1` (var0=1)，中间 NPC 203072 SETPRO1，终端 203058
     - `3201` Snow Blessing：删 `k1`，中间 804601，终端 204534
     - `4201` Sandblossom Wine：删 `k1`，中间 205233，终端 204791
     - `39000` Jump In Brusthonin：删 `k1`，中间 800501，终端 800500
     - `49000` A New Battlefront Opens：删 `k1`，中间 800503，终端 800502
     - `11109` The Negotiators：删 `k1`，中间 798979，终端 799075
     - `1158` Village Seal Found：三 NPC 角色全错。798003 仅接任务；700003 物件需 can-act `ACTION_ITEM_USE` 门禁；203128 为 reward 汇报 NPC。
   - **全量排查过滤公式**：
     - 使用 `SETPRO1(started→reward) + reward var0=0 + 无 set-variable action + SETPRO1 NPC≠终端 NPC` 过滤。
   - **判定依据**：
     - 客户端 `quest-dialog-action-details.csv`：若 select2→SETPRO1 与 select5→SELECT_QUEST_REWARD 分属两个不同 NPC，中间必须有 var 节点。
     - 客户端 `quest-order-audit.csv`：检查 NPC 是否存在路由孤岛或 `CLIENT_PAGE_UNREACHED`。
   - **修复模式**：恢复中间节点（`status=START, var0=1`）+ `NPC_REPORT`（中间→reward）+ reward 节点 `var0=1`。报告页必须走 `NPC_REPORT`，严禁误用 `USE_OBJECT`。

---

## [QE-006] 四、NPC 对话门控与任务路由冲突 (NPC_DIALOG_ROUTE_GATE_COLLISION)
<!-- pattern-metadata
status: CONFIRMED
scope: Quest event dispatch for multiple quests attached to one NPC
first_seen: unknown
last_verified: 2026-09-14
symptom: REWARD 状态无 Page 5、奖励窗口关闭或 NPC 对话无响应
root_cause: Broad NPC routing matched an unauthorized task before the REWARD-specific dispatcher
fix_or_guardrail: Use exact QuestEvent.matches filtering and preserve REWARD dispatcher priority
evidence: commit 59bba1a; .agents/summary/quest-acceptance/1006-2026-09-10-client-accepted.md:15
validation: focused-test; client-acceptance record; runtime boundary remains explicit
boundaries: Applies to shared NPC event routing; ordinary task authorization gates must remain intact
superseded_by: none
see_also: docs/quest/repair-playbook/PATTERNS.zh-CN.md (NPC_DIALOG_ROUTE_GATE_COLLISION)
first_check: QuestEvent.matches and QuestProductionDispatcher priority
-->

- **现象**：玩家达到领奖状态（`REWARD`）后，与 NPC 对话无法打开奖励选择窗口（Page 5），直接关闭对话或无响应（如 q1006「Ascension / 重生为守护者」）。
- **根因**：
  同一 NPC（如 Pernos 790001）身上挂有多个任务。当 NPC 使用较宽的 `TalkToNpc(npcId)` 索引分发时，其他处于未授权状态的普通任务（如 q1123）与当前处于 `REWARD` 状态的主线使命任务（MISSION）发生门控判断冲突，导致错误命中未授权分支。
- **修复与排查模式**：
  在 `QuestEngine` / `QuestProductionDispatcher` 中，事件路由必须按实际 `QuestEvent.matches` 精确过滤，确保各状态（尤其是 REWARD）的专属 Dispatcher 具有正确的优先级，同时保留普通任务列表的授权门禁保护（代表提交 `59bba1a`）。

---

## [QE-007] 五、可选/分支收集物提前扣除致使领奖卡死 (OPTIONAL_WORK_ITEM_CLEANUP_BLOCK)
<!-- pattern-metadata
status: CONFIRMED
scope: Optional branch work-item cleanup and reward transitions
first_seen: unknown
last_verified: 2026-09-14
symptom: 多选一交付后领奖或奖励预览卡死，removalFeasible 为 BLOCKED
keywords: 可选工作物品; 可选收集物清理; 交任务阻断; 领奖卡死; 奖励预览卡死; removalFeasible BLOCKED; quest 2392; quest 1922; quest 2947
root_cause: Branch selection removed one item early while later routes required all alternatives with count=1
fix_or_guardrail: Use count=ALL for reward-stage cleanup and preview routes after branch selection
evidence: commit fb26a0d49; .agents/summary/quest-2392/2026-09-14-optional-work-item-audit.md:19
validation: static; focused-test; production-gate; client validation required
boundaries: count=ALL is safe only for cleanup semantics and must not replace a required exact-count consumption
superseded_by: none
see_also: docs/quest/repair-playbook/PATTERNS.zh-CN.md (PATTERNS index)
first_check: removalFeasible, branch transitions and reward-stage remove-item actions
-->

- **现象**：任务可多选一交付物品（如 q2392「美丽的羽毛」三选一、q1922、q2947），玩家选定分支进入奖励状态（`REWARD`）后，点击 NPC 领奖或预览奖励窗口时完全卡死无响应。
- **根因**：
  1. 任务在 `started -> r1/r2/r3`（分支推进）时，已执行 `<remove-item count="1"/>` 扣除了玩家选定的该分支物品（背包内该物品余额归 0，且另两种物品从未收集）。
  2. 随后的 `rN -> complete` 以及 `rN -> rN`（自环预览）路由，错误地声明了对这全部三件物品的**严格正数扣除**（`count="1"`）。
  3. `QuestMutationPlanner.removalFeasible` 判定 `remaining >= remove.count()` 为 `false`，导致完成路由与预览路由全部被判定为 `BLOCKED`！
- **修复规范**：
  - 进入奖励阶段或自环预览的清理动作，针对这种多选一/分支已提前扣除的物品，**必须使用 `count="ALL"`**。
  - `removalFeasible` 对 `removeAll()` 会短路返回 `true`，背包未持有也不会阻断领奖。

---

## [QE-008] 六、不可达/999级任务前置阻断接取 (UNREACHABLE_METADATA_PREREQUISITE)
<!-- pattern-metadata
status: CONFIRMED
scope: Production quest catalog prerequisites and NPC start eligibility
first_seen: unknown
last_verified: 2026-09-14
symptom: 等级满足但无任务标记、接受动作无响应、前置任务为 999 级或不存在
keywords: 不可达接取前置; finished quest-id; 999 级前置; 接取动作无响应; QUEST_ACCEPT_1 无页面; quest 19055; quest 19054; NPC 798450
root_cause: Production metadata referenced an unavailable, obsolete or unreachable prerequisite
fix_or_guardrail: Prove the prerequisite against the production catalog before removing the invalid reference
evidence: commit adc5cbc0b; .agents/summary/quest-19055/2026-09-09-unreachable-start-prerequisite-audit.md:7; quest_data.xml
validation: static; focused-test; client acceptance not implied
boundaries: Do not remove a prerequisite merely because it is inconvenient; require catalog and version evidence
superseded_by: none
first_check: production catalog, quest_data.xml and start-condition definitions
-->

- **现象**：玩家达到等级要求，但在 NPC 处完全看不到可接任务标记或点击无接取对话（如 q19055）。
- **根因**：
  任务 metadata 中的 `<prerequisites>` 或 `<condition type="finished">` 引用了已废弃、未引入当前版本或 999 级的虚拟任务（如 `19054` 在当前生产 catalog 和 `quest_data.xml` 中均不存在）。
- **排查与修复规范**：
  - 检查 metadata 中的前置任务 ID 是否存在于生产 catalog。
  - 经核实目标为缺失定义或 999 级不可达任务时，安全清理该前置引用，恢复 NPC 的正常接取（`unaccepted -> started`）路由。

## [QE-009] 七、无任务上下文的 NPC 选择保持普通对话 (CONTEXTLESS_NPC_DIALOG_STAYS_PLAIN)
<!-- pattern-metadata
status: CONFIRMED
scope: CM_DIALOG_SELECT, DialogService and QuestEngine routing for Aion 5.8 NPC selections without client quest context
first_seen: 2026-09-13
last_verified: 2026-09-14
symptom: 关闭普通任务标记后 NPC 选择出现 load fail、隐藏任务 owner 截获或 action 被错误回显为 dialog page
root_cause: A contextless NPC selection with questId=0 was allowed to borrow an unrelated quest route instead of remaining in the plain-dialog path
fix_or_guardrail: Resolve remembered nonzero quest context before task routing; otherwise call the simple NPC dialog path, and preserve USE_OBJECT(-1)/START_DIALOG(31) object-task ingress
evidence: commit e518518ce; docs/quest/NPC_DIALOG_CONTEXT.zh-CN.md; .agents/summary/quest-acceptance/1370-2026-09-13-client-accepted.md; CMDialogSelectContextTest
validation: focused-test 31/31; client-acceptance recorded; existing 23 contract fingerprints remain baseline; full Maven/runtime gate not implied
boundaries: Applies to ordinary NPC selection without task context; do not change object-task routing or use an action ID as an SM_DIALOG_WINDOW page
superseded_by: none
first_check: CM_DIALOG_SELECT.hasQuestDialogContext, resolveRoutedQuestId, DialogService.onSimpleDialogSelect and client packet sequence
-->

- **快速判定**：普通 NPC 选择携带 `questId=0` 且客户端没有记忆中的非零任务上下文时，必须直接走普通页面；不能尝试绑定该 NPC 的所有任务，也不能把动作 ID 当作返回页面。
- **边界保留**：交互物任务仍从 `AI2Actions.selectDialog` 进入 `USE_OBJECT(-1)` / `START_DIALOG(31)` 路由；详细协议对照和客户端验收见 [NPC_DIALOG_CONTEXT.zh-CN.md](../../../docs/quest/NPC_DIALOG_CONTEXT.zh-CN.md)。

## [QE-010] 八、欧比斯外部准入必须满足阵营任务完成态 (ABYSS_ENTRY_REQUIRES_QUEST_COMPLETION)
<!-- pattern-metadata
status: CONFIRMED
scope: PortalService, TeleportService2 and fixed/multi-return item paths targeting Aion 5.8 Abyss world 400010000
first_seen: 2026-09-13
last_verified: 2026-09-14
symptom: 未完成进入任务仍可从主城门户、固定回城或多目标回城路径进入欧比斯
root_cause: XML quest_req can be bypassed by membership permission, while alternate teleport owners do not share the same admission check
fix_or_guardrail: Require COMPLETE quest 1920 for Elyos or 2945 for Asmodians at the narrow external-entry boundary, while preserving Abyss internal movement and instance-exit returns
evidence: portal_template2.xml:614-662; PortalService.java:72-86,311-316; TeleportService2.java:87-225,891-918; AbyssTeleporterQuestRequirementTest; rollout audit
validation: focused-test 18/18; diff/static checks; client acceptance and full catalog/whitelist gates not run; no commit
boundaries: Do not blanket-block all TeleportService2.teleportTo calls to world 400010000; membership permission must not bypass the external-entry gate, but internal and exit paths remain allowed
superseded_by: none
first_check: PortalService.port, permission branch, portal_use quest_req, fixed return-item handlers and MultiReturnAction target index
-->

- **准入规则**：Elyos 只认任务 `1920`，Asmodian 只认任务 `2945`，状态必须为 `COMPLETE`；目标世界为 `400010000`。
- **实现边界**：门禁放在门户及固定/多目标回城的实际 owner 上，不能在通用传送入口一刀切，否则会破坏欧比斯内部移动、任务传送和副本出口回城。该条仍是暂定模式，待客户端验收后再升级状态。

## [QE-011] 九、工作物品声明整批丢失导致完成后道具残留 (WORK_ITEM_DECLARATION_LOST_ON_MIGRATION)
<!-- pattern-metadata
status: CONFIRMED
scope: QuestMetadata.questWorkItems, QuestMutationPlanner completion/abandon cleanup, and the quest_data.xml to typed XML migration
first_seen: 2026-09-14
last_verified: 2026-09-14
symptom: 任务完成后任务道具仍留在背包、工作物品不回收、任务书显示 COMPLETE 但道具未消失
root_cause: Migrated metadata dropped <work-items>, so appendCompletionWorkItemCleanup cleaned nothing while legacy setFinishingState cleared questWorkItems unconditionally
fix_or_guardrail: Treat quest_data.xml <quest_work_items> as the authority for recoverability; declare <work-items> on every owning definition, and require each REWARD-entering route to hand the item in when the declaration is absent
evidence: commit de7c36d9d; .agents/summary/quest-1192/2026-09-14-work-item-migration-audit.zh-CN.md; QuestWorkItemMigrationCoverageTest
validation: static; focused-test; production catalog 6193 compile OK with 0 failures and 0 whitelist violations; client validation pending
boundaries: Applies to legacy-declared quest work items and to routes entering REWARD without a declaration. Do not auto-converge conflicting evidence (4942); ordinary collect items belong to COLLECT_ITEM_TURNIN_REMOVAL_MISSING
superseded_by: none
first_check: quest_data.xml quest_work_items, compiled metadata.questWorkItems(), and every transition whose source is not REWARD and target is REWARD
-->

- **权威与清理契约**：`quest_data.xml` 的 `<quest_work_items>` 是可回收性的权威来源。旧引擎 `QuestService.setFinishingState`（commit `911440146`）在完成时**无条件**清理全部 `questWorkItems`，同一份声明还参与掉落抑制；typed 引擎只在 metadata 声明了 `<work-items>` 时执行同等清理（`QuestMutationPlanner.appendCompletionWorkItemCleanup`，放弃路径 `QuestService.java:1450`）。**声明为空即等于完成时什么都不清理。**
- **判定盲区**：不要用「定义里某处存在该物品的 `remove-item`」作为通过依据。1192 的 182200556 确实有一处移除，但它挂在 `SELECT_QUEST_REWARD` 上，而玩家实际走的 `SETPRO1` 路径没有覆盖；必须逐条枚举「源非 REWARD、目标 REWARD」的 transition 并确认各自交出物品。
- **道具模板不承担消耗**：`QuestStartAction`（道具模板的 `queststart` 动作）只调用 `onDialog(ASK_ACCEPTION)`，`ReadAction` 只播动画并发对话，都不会移除道具。因此 `use-item` 进入 REWARD 的路径同样必须由任务侧显式交出物品。
- **全量审计基线（2026-09-14）**：1337 个 EXECUTABLE 任务声明了 `quest_work_items`。修复前有 111 个既无声明也无兜底、57 个仅有部分显式移除、8 个部分覆盖、5 个声明与 legacy 不一致；修复后 1336 个已对齐。审计脚本见 `.agents/summary/quest-1192/`。
- **证据冲突不自动收敛**：4942 的三方证据互不相同（旧 handler 移除 `186000085`、`quest_data.xml` 声明 `182207122`、当前 XML 声明 6 个 `152206*` 分支图纸），已登记在 `QuestWorkItemMigrationCoverageTest.EVIDENCE_CONFLICT_QUESTS` 中待人工裁定，不得自动改写。

---

## [QE-012] 十、客户端多 SECTION 计数必须使用固定 6-bit 位段 (CLIENT_SECTION_PACKING_MISMATCH)
<!-- pattern-metadata
status: CONFIRMED
scope: Quest progress bit-field layout for item/skill counters and Aion 5.8 client quest-summary SECTION placeholders
first_seen: 2026-09-14
last_verified: 2026-09-20
symptom: 使用任务物品或技能后服务端进入 START，但客户端任务说明为空、只剩奖励或计数步骤不显示；任务推进后客户端任务说明仍停留在上一行、不跟随服务端阶段；或单变量阶段行走任务被写入高位 var 导致步数打包为 4099/8196 引起任务追踪 HTML 完全空白
root_cause: 多段计数被紧凑放在 offset 0/4/8，而客户端脚本和旧 QuestVars 按 SECTION_n = 6*n 读取；或把任务说明行索引（阶段）从 SECTION_0 交换到 SECTION_1，使客户端行索引读到计数槽而停在固定行；或对客户端 Progress(min~!max) 纯阶段行走任务错误声明高位 varN（如 var2），打包后高位非零破坏客户端步骤校验
fix_or_guardrail: 当 Quest.pak 的 quest_script/HTML summary 引用 SECTION_N 时，varN 必须放在 offset=6*N 的 6-bit 位段，并用客户端脚本或旧 setQuestVarById(N) 对齐；阶段/任务说明行索引必须留在 SECTION_0，计数只能放 SECTION_1+，不得为了隔离计数而交换两者；客户端声明为 Progress(min~!max) 的单变量阶段行走任务，严禁在高位声明或写入多余的 varN，必须保持纯 var0 推进
evidence: .agents/summary/quest-11468-taloc-item-sections/2026-09-14-client-section-mismatch.zh-CN.md; .agents/summary/quest-10032/2026-09-15-section0-stage-correction.zh-CN.md; .agents/summary/quest-10101-door-and-counter/2026-09-20-10101-kill-counter-section2-and-door-evidence.zh-CN.md; src/test/java/com/aionemu/gameserver/questEngine/definition/ClientQuestSectionAlignmentTest.java; src/test/java/com/aionemu/gameserver/questEngine/runtime/Quest10032ItemPlayClientCounterProductionFlowTest.java; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMonsterProgressContractAuditTest.java; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestPacketOrderRegressionTest.java
validation: focused regression ClientQuestSectionAlignmentTest, QuestMonsterProgressContractAuditTest, QuestDefinitionCatalogManifestTest and ProductionCatalogWhitelistVerificationTest passed (PRODUCTION_COMPILE_OK=6189, 0 failures, 0 violations); 10101 var2 high-bit pollution removed and verified clean step 3/4
boundaries: 有客户端证据证明的单字段紧凑布局可以保留；只有客户端脚本或旧 handler 明确寻址独立 SECTION 时才应用该规则；对于客户端声明为 Progress(min~!max) 的单变量阶段行走任务，严禁在高位声明或写入多余的 varN，否则高位非零整型步数会导致客户端 HTML 渲染崩溃
superseded_by: none
first_check: Quest.pak quest_script_monster.csv 的 SECTION_N、旧 handler setQuestVarById(N)、XML offset/width、任务说明行索引是否仍读取 SECTION_0
-->

- **判定规则**：5.8 客户端的 `SECTION_0..3` 分别对应 `quest_vars` 的 `0..5`、`6..11`、`12..17`、`18..23` 位段。任务 XML 中的 `varN` 若参与客户端摘要或脚本条件，必须与 `SECTION_N` 对齐，不能仅因为当前最大值较小就紧凑改到 `var(N-1)` 的位段。客户端 `quest.xml` 的 `<collect_progress>N</collect_progress>` 给出收物/领奖行的 step 值，可用来判别该任务是 step 走行还是行号走行（N 超过任务书行数上限即 step 走行，2289 = 4 行 / step 7）。
- **代表案例**：
  1. 11468/21468 需要 `SECTION_1<10`、`SECTION_2<5`、`SECTION_3<3`，且进行中要求 `SECTION_0==0`。旧 XML 把三个字段放在 `0/4/8`，第一次使用物品就把 `SECTION_0` 置 1，客户端摘要整体隐藏；修复为 `6/12/18` 后恢复计数段。
  2. 10032/20032：真机在服务端已到 s1（交换布局 wire=64，var1=1）时，客户端任务说明仍显示第 0 行；`//quest set 10032 START 65`（SECTION_0=1）后说明行立即前进，证明行索引读 SECTION_0。修正为 `var0=阶段(0..8, offset 0)`、`var1=眼泪次数(0..20, offset 6)`，掉落门禁恢复阶段 6；`Quest10032ItemPlayClientCounterProductionFlowTest` 锁定新合同，既有 `QuestPacketOrderRegressionTest` 的 `var0=7` 断言同时恢复通过。
  3. 10101/20101：客户端在 `quest_script_monster.csv` 声明为 `Progress(2~!4)`（单变量阶段行走），客户端无该任务的 `SECTION_2` 计数器。若在 `<progress>` 声明并写入 `var2`（offset 12），击杀 2 只后整型步数被打包为 `8196`（高位非零），破坏客户端步骤校验使任务追踪 HTML 完全空白；只有保持纯 `var0` 阶段行走（2→3→4）下发纯净整型步数，HTML 才能正常渲染并无缝前进。
  4. 2289（Altgard Rampaging Mosbears）：客户端 `Progress(0~4)` 让行 0 的击杀计数占 SECTION_0 的 0..4（五次击杀），第 5 次击杀才把任务书推进到行 1（step 5），后续情报/收物行在 step 6/7；客户端 `collect_progress=7` 与 legacy `checkQuestItems(7, 7, true, 5, 2120)` 同时证明 reward 投影 = step 7，而按“末行索引 3”改投影会让行 0 的计数位顶掉整条阶梯。任何“var0 只当行号、把计数拆到高位 varN”的改法都会同时破坏客户端 step 校验与旧存档；门禁 `AltgardMosbearsCounterLadderContractTest`，报告 §五十四。

---

## [QE-013] 十一、影片 self-loop 必须下发后续页 (MOVIE_CONTINUATION_RESPONSE)
<!-- pattern-metadata
status: CONFIRMED
scope: Quest movie page-turn self-loops and Aion 5.8 client dialog page continuation
first_seen: unknown
last_verified: 2026-09-15
symptom: 继续听、电影重复播放、动画结束仍是原按钮、点击后无下一页
root_cause: 同状态 movie self-loop 的 after-commit 只有 play-movie，没有按客户端 action 到 page 合同下发后续页、关闭窗口或推进状态，客户端停在原页并重发
fix_or_guardrail: 在 play-movie 后补权威合同要求的 SHOW_QUEST_PAGE、close-dialog 或状态推进；不能只保留 movie 副作用。写作侧统一改用 movie-page-turn 积木（缺同名页面即编译失败），编译侧遇未登记账的 movie-only 翻页抛 MOVIE_WITHOUT_CONTINUATION，翻页动作 after-commit 完全为空抛 PAGE_TURN_WITHOUT_ANY_RESPONSE，运行侧由 CM_DIALOG_SELECT 重发断路器关闭窗口
evidence: commit ae1015818bf2b530f4ba0ea7ec4d26d6e0cdbf43; commit 8b058d4b4; .agents/summary/quest-14045-14046-movie-page-turn/README.md; .agents/summary/quest-acceptance/14045-14046-2026-09-14-client-accepted.md; src/main/java/com/aionemu/gameserver/questEngine/definition/QuestMovieContinuation.java; src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDialogContract.java; src/main/java/com/aionemu/gameserver/questEngine/definition/QuestXmlBlockExpander.java; src/main/java/com/aionemu/gameserver/questEngine/QuestEngine.java; src/main/resources/aion/data/static_data/quest_definition/quest_definition.xsd; src/main/resources/aion/definitions/quest_dialog/client_dialog_contract.tsv; src/main/resources/aion/definitions/quest_dialog/movie_continuation_exceptions.tsv; src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_DIALOG_SELECT.java; src/main/java/com/aionemu/gameserver/model/gameobjects/player/DialogSelectRepeat.java; .agents/summary/quest/generate_quest_dialog_contract.py; src/test/java/com/aionemu/gameserver/questEngine/definition/MovieContinuationResponseFamilyTest.java; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMovieContinuationGateTest.java; src/test/java/com/aionemu/gameserver/questEngine/e2e/QuestDialogLoopBreakerProductionFlowTest.java; src/main/java/com/aionemu/gameserver/questEngine/definition/QuestPageTurnResponseGate.java; src/test/java/com/aionemu/gameserver/network/aion/clientpackets/CM_DIALOG_SELECTRepeatGuardTest.java; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestPageTurnResponseGateTest.java; docs/quest/WRITING_GUIDE.zh-CN.md; docs/quest/repair-playbook/PATTERNS.zh-CN.md
validation: focused-test: MovieContinuationResponseFamilyTest, Quest14045And14046MoviePageTurnContractTest and the 6-method QuestMovieContinuationGateTest passed on clean HEAD 0a121ca50 plus this batch; engine-level QuestDefinitionCatalogManifestTest, QuestEngineNpcDialogDispatchTest, QuestEngineRuntimeCompositionTest and ProductionCatalogWhitelistVerificationTest passed (30 tests); Phase 3/4: 118 tests passed on clean HEAD 569ba10d8 plus this batch, including QuestXmlDomainBlocksTest, CM_DIALOG_SELECTRepeatGuardTest, QuestDialogLoopBreakerProductionFlowTest, LocalizedLogCallsTest and the e2e protocol suites; production-gate: PRODUCTION_COMPILE_OK=6193, FAILURES=0, WHITELIST_VIOLATIONS=0; generator --check OK; negative checks confirmed MOVIE_WITHOUT_CONTINUATION on a stripped 14045 page and a freshness failure on a mutated CSV; client: Aion 5.8 accepted 14045/14046 on 2026-09-14. 2026-09-15 review fixes (resend cadence floor, tolerant pre-scan, package-private thresholds, MOVIE_PAGE_TURN_DUPLICATE_ACTION) passed 55 focused tests in the main worktree with user build authorization (QuestXmlDomainBlocksTest 44, CM_DIALOG_SELECTRepeatGuardTest 5, QuestDialogLoopBreakerProductionFlowTest 1, QuestMovieContinuationGateTest 5 catalog-free methods); the production-directory ledger method and ProductionCatalogWhitelistVerificationTest remain unexecuted because parallel WIP blocks the main worktree; a later 2026-09-15 review round added QuestPageTurnResponseGate/PAGE_TURN_WITHOUT_ANY_RESPONSE (verified with 9 tests plus PRODUCTION_COMPILE_OK=6193 / 0 failures / 0 whitelist violations) and the interaction-boundary reset (CM_SHOW_DIALOG/CM_CLOSE_DIALOG clear DialogSelectRepeat) verified by 13 tests including QuestDialogLoopBreakerProductionFlowTest#reopeningTheDialogRestartsTheResendCounter
boundaries: 仅适用于客户端当前页按钮存在后续页或明确关闭/状态推进合同；纯过场无 NPC 对话入口的任务按系统页或自动完成处理，不强制补对话页。契约基于 tracked 客户端 CSV，未有 CSV 证据的页面不在护栏覆盖内；编译期加载依赖运行目录 definitions/quest_dialog/*.tsv（scripts/package.sh/start-silent.sh 会同步），缺失即 fail-closed 而不会静默放行。movie-page-turn 积木只覆盖「同名页面或显式 page」形态，额外条件/动作或不同副作用仍需显式 transition；运行侧断路器是启发式（同一选择连续 4 次、相邻间隔 0.8–5 秒，人类连点被排除），只关闭窗口并跳过本次路由，不改变任务状态，真实客户端未复现该路径
superseded_by: none
see_also: docs/quest/repair-playbook/PATTERNS.zh-CN.md
first_check: 客户端当前页 action、电影 transition 的完整 after-commit、后续页是否存在
-->

- **判定规则**：`TALK_TO_NPC` 的翻页动作（action X 对应客户端 page X）如果在同一状态以 `play-movie` 自环，必须在 movie 之后继续下发目标页、关闭窗口或推进状态。只播影片会让客户端停在原页并反复发送同一个 `CM_DIALOG_SELECT`。
- **代表案例**：14047 movie 421 后回到 2376 并继续 SETPRO5，代表提交 `8b058d4b4`，代表测试 `Quest14047ClientDialogAlignmentTest#returnsFromMovie421ToTheStep11PageAndThenAdvancesToStep5`；14045 movie 272 后补发 SELECT1_1_1(1013)；14046 movie 102 后补发 SELECT2_1(1353)。后两者修复提交 `ae1015818bf2b530f4ba0ea7ec4d26d6e0cdbf43`，2026-09-14 客户端验收通过。
- **历史修复盲区**：`cfc2fa048` 的 page-turn 批量修复只补「未注册」的客户端动作；已经挂在 movie transition 上的动作被跳过，因此需要单独检查 movie self-loop 的 after-commit，不能只依赖 BUTTON_WITHOUT_ROUTE 审计。
- **家族扩展（2026-09-14，提交 `d7e0f4cb0`）**：2002、2007、2008、24045、24052、24053 的可达电影翻页链已补齐目标页与中继页，由 `MovieContinuationResponseFamilyTest` 锁定；24053 step1-step4 保留旧 handler switch fallthrough 的 movie-only 路由（不在客户端 1011->1012->10000 可达链上），作为有意例外。
- **Phase 1 硬门禁（2026-09-15）**：`QuestMovieContinuationGateTest` 对全生产目录检查同状态 movie-only page-turn；合法例外只允许 24053 step1-step4 四条显式 ledger，集合相等断言保证修复后必须移除旧例外。
- **Phase 2 编译期强制（2026-09-15）**：规则下沉为共享实现 `QuestMovieContinuation.violations(...)` + `QuestDialogContract`，由 `QuestDefinitionXmlCompiler.compile(InputStream[, Schema])` 在生产目录编译期校验，违规抛 `MOVIE_WITHOUT_CONTINUATION`；例外只能来自 `movie_continuation_exceptions.tsv` 显式 ledger。契约 `client_dialog_contract.tsv` 由 `.agents/summary/quest/generate_quest_dialog_contract.py` 生成，优先从外部 definitions 目录加载、缺失才回退 classpath（打包 jar 始终排除 `aion/**`，生产只可能走文件路径）；`QuestEngine.loadProductionCatalog()` 每次编译前失效缓存，`//reload quest` 可重新读取契约与 ledger；文件头源哈希由门禁测试校验，CSV 变更未重新生成即失败。
- **Phase 3/4 写作积木与运行断路器（2026-09-15）**：写作侧用 `<movie-page-turn>`（`QuestXmlBlockExpander`）把「播影片 + 显示同名目标页」固定成一条积木，缺同名页面且未显式声明 `page` 时抛 `MOVIE_PAGE_TURN_PAGE_MISSING`，因此积木无法表达 movie-only 形态；`next-action` 追加中继页路径（与 `action` 相同则抛 `MOVIE_PAGE_TURN_DUPLICATE_ACTION`），路由同时登记进 `explicitDialogRoutes`。运行侧 `CM_DIALOG_SELECT` 对「同一目标/上一页/动作/任务」计数，只有相邻间隔落在 0.8–5 秒的连续 4 次才判定循环，随后写 `log.quest_dialog_select_loop`、清除任务列表记忆并发送关闭窗口，人类连点与「重新打开/关闭对话」（`CM_SHOW_DIALOG`/`CM_CLOSE_DIALOG` 清零计数）都不会触发。同一族还有编译期门禁 `PAGE_TURN_WITHOUT_ANY_RESPONSE`（`QuestPageTurnResponseGate`）：翻页动作的 `after-commit` 完全为空即拒绝编译，堵住「静默死按钮」整类（validatePageTurnResponseContract，现网 0 例，生产目录 6193 零违规）。

---

## [QE-014] 十二、同 NPC 同阶段严禁无优先级动作重叠与计数节点投影重合 (AMBIGUOUS_TRANSITION_AND_COUNTER_PROJECTION_COLLISION)
<!-- pattern-metadata
status: CONFIRMED
scope: QuestDefinitionCompiler transition conflict validation, QuestXmlBlockExpander counter expansion, and node projection uniqueness
first_seen: 2026-09-15
last_verified: 2026-09-20
symptom: 任务引擎启动崩溃、Can't initialize typed quest engine、AMBIGUOUS_TRANSITION: same event has overlapping transitions without unique priorities: TALK_TO_NPC、DUPLICATE_NODE_PROJECTION
root_cause: 同阶段同 NPC 动作被多次注册（例如修复直达奖励时对齐 SETPRO 却未清理历史自循环），或多阶段任务引入 counter 积木时因 source 节点不得固定计数字段导致省略声明退化为 START:0 碰撞
fix_or_guardrail: 对齐客户端动作时彻底清理原同动作自循环边；多阶段且含 counter 的任务，progress 必须分离阶段位段（如 var0）与计数字段（如 var1，参考 4944 潘利尔规范），source 节点固定阶段 var0，counter 绑定 var1
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/1722.xml; src/main/resources/aion/data/static_data/quest_definition/quests/3940.xml; src/main/resources/aion/data/static_data/quest_definition/quests/4944.xml; src/main/resources/aion/data/static_data/quest_definition/quests/15321.xml; retail-xml-retention.tsv 的 quest 25608 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/main/resources/aion/data/static_data/quest_definition/quests/27510.xml; src/test/java/com/aionemu/gameserver/questEngine/runtime/QuestCounterProjectionLockFollowUpTest.java; src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDefinitionCompiler.java; src/main/java/com/aionemu/gameserver/questEngine/definition/QuestXmlBlockExpander.java; .agents/summary/quest-counter-projection-family/2026-09-20-counter-projection-lock-batch.zh-CN.md; .agents/summary/quest-engine-startup-debug/README.md
validation: static（全目录 COUNTER_PROJECTION_LOCK 审计：修复前 3 任务命中，修复后归零）；focused-test（2026-09-20，11 个测试类 72 例 0 失败 0 错误）；production-gate（PRODUCTION_COMPILE_OK=6189、白名单违规 0）；client 复验 PENDING
boundaries: 同一事件在不同源节点、或有互斥条件（如 class/has-item）、或声明了唯一优先级的属于合法分支；仅适用于同一可达源节点下动作、条件、优先级完全相同的重叠，以及 counter source 节点的字段分配
superseded_by: none
first_check: 冲突任务 XML 的 transitions 中同 NPC/同 action 的边、nodes 列表中的投影 (status + var)、counter 的 field 与 source/target 节点定义
-->

- **判定规则**：
  1. `QuestDefinitionCompiler` 在编译期对所有转换建立冲突索引：对于同一 NPC 的相同 `TALK_TO_NPC` 动作，如果两者可能从同一节点触发、条件非互斥且均未声明唯一 `priority`，即判定为无歧义解析保证（`AMBIGUOUS_TRANSITION`）并拒绝启动。
  2. `<counter>` 领域积木强制要求其 `source` 节点不得固定计数字段（`COUNTER_SOURCE_PROJECTION_CONFLICT`），因为击杀计数递增过程中该字段值动态变化。若任务拥有多个 `START` 阶段，不可直接省略变量声明（缺省会退化为 `START:0` 与初始 `started` 节点重叠触发 `DUPLICATE_NODE_PROJECTION`）。
  3. 运行期路由用 source 节点投影与 packed 变量做全等匹配（`QuestMutationPlanner#matchesSourceNode`）：计数自环（`source == target` 且自增字段）的字段一旦被 source 投影钉死，第一次递增后同源事件永远 `NO_MATCH`，表现为「只有第一只怪计入、任务不往下」。手写 transition 由 `QuestDefinitionCompiler` 抛 `COUNTER_SELF_LOOP_PINS_INCREMENTED_FIELD` 兜底（第二阶段 2026-09-20 新增），`<counter>` 积木仍走上一条 `COUNTER_SOURCE_PROJECTION_CONFLICT`。
- **代表案例**：
  1. `1722.xml`（拉斯汀的秘密指令）：提交 `94636797a` 将 `s2` 推进到 `s3` 的动作从 `SELECT_QUEST_REWARD` 纠正为 `SETPRO3` 时，漏删了文件下方历史遗留的 `s2 -> s2 SETPRO3` 自循环边，导致两边重叠报错。删除冗余自循环边后闭环。
  2. `3940.xml`（米拉詹特武器忠诚任务）：提交 `0823653a7` 尝试单字段承载阶段与 300 击杀（6..306），因 `<counter>` 约束移除了 `hunt` 节点的变量声明，导致其退化为 `START:0` 与 `started` 发生投影重合。对齐魔族同型任务 `4944.xml`（潘利尔武器任务）标准设计：分离阶段字段 `var0`（6-bit）与计数字段 `var1`（9-bit，0..300），`hunt` 固定 `var0=6`，`hunt-done` 固定 `var0=6, var1=300`，彻底消除节点投影碰撞。
  3. 2026-09-20 第二批 `COUNTER_PROJECTION_LOCK`：`15321`（s0–s11 全阶段投影 `var1=0`，s1/s3/s5/s7/s9/s11 各自 30 只击杀）、`25608`（step2/step3）、`27510`（started/s1–s4，s3 并行计数精英与无名 boss）把实时击杀计数钉进 source 投影，玩家实测第二只怪起全部 `NO_MATCH`。三者的 START 阶段节点改为只固定阶段位段（对齐已修的 25321），计数由自环转换拥有；行为回归与编译期反例见 `QuestCounterProjectionLockFollowUpTest` 与 `IncrementVariableDefinitionTest#selfLoopCounterRejectsIncrementingItsProjectedField`。

---

## [QE-015] 十三、区域任务结束广播目标必须真实拥有该事件路由 (ZONE_MISSION_BROADCAST_TARGETS_MUST_BE_ROUTABLE)
<!-- pattern-metadata
status: CONFIRMED
scope: BroadcastZoneMissionEnd after-commit action, QuestProductionDispatcher.dispatchOwners target contract and production quest XML broadcast target lists
first_seen: 2026-09-15
last_verified: 2026-09-15
symptom: 任务领奖/完成后刷 typed 任务已提交但有提交后动作失败、QUEST_AUDIT AFTER_COMMIT 失败、QuestAfterCommitException: after-commit action BroadcastZoneMissionEnd failed
root_cause: 广播目标列表包含完成方自身等没有 zone-mission-end 路由的 owner；dispatchOwners 把目标路由条件不匹配视为成功投递，把目标缺路由视为失败，因此自含目标必然让整个提交后广播失败
fix_or_guardrail: 广播目标只保留真正声明了 zone-mission-end 路由的后续任务，且绝不包含完成方自身；保持 dispatchOwners 的缺路由硬失败语义，禁止用容错掩盖目标清单错误
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/10031.xml; src/main/resources/aion/data/static_data/quest_definition/quests/20031.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/Quest10031And20031ZoneMissionBroadcastTest.java; .agents/summary/quest-10031-zone-mission-broadcast/2026-09-15-zone-mission-end-broadcast-self-target.zh-CN.md; commit 911440146
validation: focused-test Quest10031And20031ZoneMissionBroadcastTest; production catalog 6193 executable definitions compile with 0 failures and 0 whitelist violations; production-directory broadcast audit 6231 definitions and 24 broadcasts with 0 self-includes and 0 unroutable targets; client/runtime re-verification still pending
boundaries: 条件不匹配仍属于成功投递；只有缺路由或执行失败才算失败。旧 Handler 对未注册 onEnterZoneMissionEnd 的 quest 是静默 no-op，因此旧代码把整族含自身都发一遍的写法不能直接照搬到 typed XML
superseded_by: none
first_check: broadcast-zone-mission-end 的 quest-ids 是否包含自身或其他未声明 zone-mission-end 路由的 owner；目标 quest 的 transitions 是否存在 ZoneMissionEnd 事件
-->

- **判定规则**：
  1. `PlayerQuestBroadcastPort.broadcastZoneMissionEnd()` 通过 `QuestProductionDispatcher.dispatchOwners(new QuestEvent.ZoneMissionEnd(), ...)` 逐 owner 投递；`dispatchOwners` 的成功条件是每个目标都有路由且没有执行失败，条件不匹配（`UNKNOWN`/`NOT_HANDLED`）不算失败。
  2. 因此广播目标必须是声明了 `<zone-mission-end/>` 转换的 owner；完成方自身若没有该路由就不能出现在 `quest-ids` 里。
  3. 该约束同样适用于 `schedule-event-quest-refresh` 的显式目标列表。
- **代表案例**：
  1. `10031.xml`/`20031.xml`：4 处领奖广播把完成方自身列入目标（`10031 10032 10033 10034 10035` / `20031 20032 20033 20034 20035`），但完成方没有 `<zone-mission-end/>` 路由，导致每次奖励预览与领奖都记录 `AFTER_COMMIT BroadcastZoneMissionEnd failed`。修复为只广播 `10032~10035` / `20032~20035`，并新增 `Quest10031And20031ZoneMissionBroadcastTest` 锁定目标集合与 after-commit 顺序。
  2. 对照 `10520/20520`：同族广播只列后续任务（`10521 ...` / `20521 ...`），因此从未出现该审计失败。

---

## [QE-016] 十四、实例回退边只覆盖旧 handler 声明的阶段区间 (ROLLBACK_STAGE_SCOPE_MUST_MATCH_LEGACY)
<!-- pattern-metadata
status: CONFIRMED
scope: Quest instance rollback transitions (enter-world/die/log-out) and their legacy var range boundaries
first_seen: 2026-09-15
last_verified: 2026-09-15
symptom: 完成副本阶段后离副本/死亡/下线，已完成的计数被清空并回退到前置节点
root_cause: XML 迁移把旧 handler 的 var >= X && var < Y 回退区间扩大到了已完成的后续阶段
fix_or_guardrail: 回退/清空边必须逐条对照旧 handler 的变量区间；例如 10032 只有 var0 4..5 回退，s6 完成 20 次后离副本/死亡/下线都必须保留进度，s7 离副本才转 reward
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/10032.xml; src/main/resources/aion/data/static_data/quest_definition/quests/20032.xml; src/test/java/com/aionemu/gameserver/questEngine/runtime/Quest10032ItemPlayClientCounterProductionFlowTest.java; .agents/summary/quest-10032/2026-09-15-section0-stage-correction.zh-CN.md; commit 911440146
validation: focused-test Quest10032ItemPlayClientCounterProductionFlowTest (s6 survives leave/die/logout, s7 turns in on leave); production catalog 6193 definitions compiled with 0 failures and 0 whitelist violations; real-client re-verification pending
boundaries: 死亡、下线、离副本三个事件在旧 handler 中可能各自不同区间，必须分别对照，不能用统一区间覆盖
superseded_by: none
first_check: 旧 handler onEnterWorldEvent/onDieEvent/onLogOutEvent 的 var 区间与 XML source/target 回退边逐条比对
-->

- **判定规则**：从 Java handler 迁移实例任务的回退/清空边时，先提取每个事件里的 `var` 区间（如 `var >= 4 && var < 6`），再决定 XML 需要哪些 `source` 节点；区间之后的完成阶段不得挂回退边，否则玩家已完成目标的进度会被错误清空。
- **代表案例**：10032 旧 handler 的 `onEnterWorldEvent`/`onDieEvent`/`onLogOutEvent` 都只在 `var0 4..5` 时回退到 2，`var0=6`（20 次眼泪已完成）不回退，`var0=7` 离副本转 reward。XML 曾为 s4/s5/s6 都挂回退边，导致完成 20 次后出副本被清空；修正为只保留 s4/s5 回退，并新增回归断言 s6 离副本/死亡/下线无匹配计划、s7 离副本转 reward。

---

## [QE-017] 十五、未处理的任务动作不得回显成对话页 (UNHANDLED_QUEST_ACTION_ECHOED_AS_DIALOG_PAGE)
<!-- pattern-metadata
status: CONFIRMED
scope: DialogService fallback after QuestEngine declines a quest action; quest action id vs dialog page id namespaces
first_seen: 2026-09-13
last_verified: 2026-09-16
symptom: 点任务按钮后弹 HtmlPageId 10000 / HtmlPageId 1002 load fail、窗口不关闭、任务卡在原阶段
root_cause: DialogService 在 QuestEngine.onDialog 返回 false 后把客户端按钮的 dialogId 当对话页面 ID 回显；动作 ID 与页面 ID 是两个独立命名空间
fix_or_guardrail: questId != 0 且 dialogId != QUEST_SELECT(31) 的未处理动作必须清空 NPC 任务对话选择并关窗（SM_DIALOG_WINDOW(0,0)）；通用任务列表动作 31 保留第 10 页合同，questId == 0 仍走普通对话
evidence: commit 2169b6332; src/main/java/com/aionemu/gameserver/services/DialogService.java; src/test/java/com/aionemu/gameserver/services/DialogServiceQuestDialogTest.java; src/test/java/com/aionemu/gameserver/questEngine/definition/Quest1220ClientDialogAlignmentTest.java; .agents/summary/quest-acceptance/1220-2026-09-16-client-accepted.md; docs/quest/repair-playbook/CASES.zh-CN.md
validation: focused-test DialogServiceQuestDialogTest 三分支（未处理动作关窗、31 保留第 10 页、questId==0 普通对话）+ Quest1220ClientDialogAlignmentTest + QuestProductionJourneyTest；production catalog 6200 条 0 编译失败 0 白名单违规；2026-09-16 用户回复「1220 也过」完成客户端验收
boundaries: 只覆盖 questId != 0 的未处理任务动作；questId == 0 的普通 NPC 对话复用 QE-009 / CONTEXTLESS_NPC_DIALOG_STAYS_PLAIN；通用任务列表动作 31 必须保留第 10 页回显；任务本该显示真实页面时应回任务 XML 与客户端页面图补路由，不得放宽本条关窗规则
superseded_by: none
first_check: CM_DIALOG_SELECT 的 targetObjectId/dialogId/lastPage/questId 与 QuestEngine.onDialog 返回值；DialogService 回退分支发出的页面 ID 是否等于按钮动作 ID
-->

- **判定规则**：任务动作 ID 与对话页面 ID 是两个命名空间。`QuestEngine` 拒绝某个 `questId != 0` 的动作时，客户端点的是按钮而不是页面，服务端只能关窗（或按任务 XML 明确路由到真实页面），绝不能把动作 ID 当作页面 ID 回显，否则客户端会加载不存在的 html 页并报 load fail。
- **边界保留**：`questId == 0` 的普通 NPC 对话保持 plain dialog 回显（见 [QE-009]）；通用任务列表动作 `QUEST_SELECT(31)` 继续使用第 10 页合同。代表案例：Playbook 案例 8.36（`2169b6332`，1220 与 9550 同根因；见 [CASES.zh-CN.md](../../../docs/quest/repair-playbook/CASES.zh-CN.md)）。

---

## [QE-018] 十六、多段计数的最终事件必须进入 REWARD (MULTI_COUNTER_FINAL_EVENT_ENTERS_REWARD)
<!-- pattern-metadata
status: PROVISIONAL
scope: Multi-dimensional skill/item counters sharing a START node, final-event timing, and persisted full-counter recovery
first_seen: 2026-09-14
last_verified: 2026-09-16
symptom: 多项实时计数已经全部达到上限但状态仍是 START；最后一件任务物品使用后下一步不出现，客户端无法进入领奖阶段
root_cause: XML 迁移只保留 started -> started 的计数自环，漏掉旧 handler 在全部计数满足后立即 setStatus(REWARD) 的 priority 0 最终路线
fix_or_guardrail: 每个计数字段保留 priority 1 的继续自环；追加 priority 0 的最终事件路线，当前字段等于 required-1 且其他字段达到阈值时执行最后一次 increment，目标 REWARD，after-commit 使用 LEVEL_AND_VISIBILITY_REFRESH；已持久化满计数存档由带阈值的 SELECT_QUEST_REWARD 恢复路线处理
evidence: commit 7f824dc78; src/main/resources/aion/data/static_data/quest_definition/quests/11468.xml; src/main/resources/aion/data/static_data/quest_definition/quests/21468.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/Quest11468And21468SkillCompletionTest.java; .agents/summary/quest-11468-taloc-item-sections/2026-09-16-final-counter-reward.zh-CN.md; .agents/summary/quest-acceptance/11468-2026-09-16-final-counter-client-accepted.md; docs/quest/repair-playbook/CASES.zh-CN.md
validation: XML XSD 与 IDE/diff 静态检查通过；2026-09-16 用户确认 11468/21468 均正常完成，客户端验收成功，最终计数 -> REWARD -> 报告领奖 -> COMPLETE 已闭环；专项 Maven 与生产 catalog/白名单门禁通过（PRODUCTION_COMPILE_OK=6193、FAILURES=0、WHITELIST_VIOLATIONS=0）
boundaries: 适用于多个实时计数字段共享 START 状态、最终事件同时完成计数与状态迁移的任务；若根因是 START 节点固定投影了实时字段，复用 QE-002 与 COUNTER_SOURCE_PROJECTION_NO_LOCK；位掩码多地点侦察复用 MULTI_LOCATION_SCOUTING_FINAL_REWARD_TRANSITION
superseded_by: none
first_check: 每个计数字段的 continuing/completing priority、最终字段条件、target status、事务动作与完整 after-commit；旧 handler 是否在全部计数满足后立即进入 REWARD
-->

- **判定规则**：多段计数的每个字段都需要成对的 continuing 与 completing 路线。只在未完成时自环、到上限后返回空计划，会让所有计数满但状态仍停在 START；最后一个事件必须以 priority 0 直接进入 `REWARD`，不能等下一次交互或只依赖无门禁的报告路线。
- **代表案例**：Playbook 案例 8.37（`7f824dc78`），11468/21468 的三个 UseSkill 计数分别为 `var1=10`、`var2=5`、`var3=3`，最终技能使用后目标 `reward`；`Quest11468And21468SkillCompletionTest#finalItemSkillEntersRewardAndPersistedFullCountersCanReport` 锁定三条 completing 路线和满计数恢复路线。

---

## [QE-019] 十七、接取转换必须继承元数据前置条件事实需求 (QUEST_FACT_ACQUISITION_METADATA_PREREQUISITES)
<!-- pattern-metadata
status: CONFIRMED
scope: 任务最小快照事实采集（QuestFactRequirements）、任务接取转换与元数据前置条件校验（metadataPrerequisitesSatisfied）
first_seen: 2026-09-16
last_verified: 2026-09-16
symptom: 任务在客户端对话列表中可见、点击后能打开详情页，但点击“接受任务”（动作 20000 / QUEST_ACCEPT_SIMPLE 等）后无反应直接关闭对话框，服务端未下发 SM_QUEST_ACTION
root_cause: QuestFactRequirements 只静态扫描了 transition 显式声明的 conditions/actions，未感知任务元数据中的 prerequisites 与 startConditionGroups；接取转换（NONE -> 非 NONE）在 planner 隐式执行 metadataPrerequisitesSatisfied 时读取 snapshot 未捕获的完成任务集合（questIdSets=false），fail-closed 直接判定不匹配导致拒接
fix_or_guardrail: QuestFactRequirements 重载支持 CompiledQuestDefinition；对接取转换自动从元数据继承事实需求：声明 prerequisites 或 finished/unfinished/acquired/noacquired 时开启 questIdSets=true，声明 equipped 时开启 equipment=true；QuestExecutionCoordinator 统一传入 definition；QuestFactRequirementsTest 锁定该断言
evidence: commit af2304f67; commit 3b4e7fc4c; src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestFactRequirements.java; src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestExecutionCoordinator.java; src/test/java/com/aionemu/gameserver/questEngine/runtime/QuestFactRequirementsTest.java; .agents/summary/quest-kill-contracts/2026-09-16-quest-19638-start-condition-prerequisite-fix.zh-CN.md
validation: 专项测试 QuestFactRequirementsTest（9 tests）全绿；QuestMonsterProgressContractAuditTest（2 tests）全绿；生产目录编译门禁 ProductionCatalogWhitelistVerificationTest（6193 OK）通过；2026-09-16 游戏实机验证通过
boundaries: 仅在从 QuestStatus.NONE 迁移到非 NONE 状态的接取转换中生效；中途杀怪/对话或无前置元数据的任务不额外采集事实，保持零内存性能损耗
superseded_by: none
see_also: .agents/summary/quest-kill-contracts/2026-09-16-quest-19638-start-condition-prerequisite-fix.zh-CN.md
first_check: 任务元数据是否声明 prerequisites 或 start-conditions；接取转换推导出的 questIdSets/equipment 是否为 true；snapshot 中 completedQuestsCaptured 是否为 true
-->

- **判定规则**：任务执行协调器在采集快照前，推导事实需求不能仅看 transition 本身声明的条件。若该转换是从 `NONE` 状态进入非 `NONE` 状态（接取任务），由于 `QuestMutationPlanner` 会隐式执行 `metadataPrerequisitesSatisfied`，推导器必须同步解析任务元数据，将前置任务 ID 集合及装备事实纳入需求，防止读取方按 fail-closed 策略误判为前置未满足。
- **代表案例**：天族特别任务 2（19638），配置 `<condition type="finished" quest-id="19637"/>`；修复提交 `af2304f67`；由 `QuestFactRequirementsTest#acquiringTransitionInheritsMetadataPrerequisites` 锁定契约，2026-09-16 客户端实机验收通过。

---

## [QE-020] 十八、领奖阶段严禁重复扣除前序步骤已消耗的道具 (NO_DUPLICATE_ITEM_REMOVAL_AT_REWARD)
<!-- pattern-metadata
status: CONFIRMED
scope: 任务领奖转换（reward -> complete/reward）道具条件与扣除动作
first_seen: 2026-09-17
last_verified: 2026-09-17
symptom: 任务已顺利推进到 REWARD 阶段，与终点 NPC 对话无法打开奖励页面，或点击领奖后提示失败/无反应，任务无法完结
root_cause: 中途步骤已通过 remove-item 扣除了任务道具，但在领奖阶段机械复用了初始/中间状态的模板，再次声明了 has-item 条件或 remove-item count="1"；由于玩家背包已无此道具，领奖事务校验失败导致永久卡死
fix_or_guardrail: 凡是在进入 REWARD 前已从背包扣除的道具，领奖与完成阶段严禁重复声明 has-item 或严格 remove-item；只有在领奖时才初次交付的物品才保留扣除；可选分支道具若必须清理统一使用 count="ALL"
evidence: retail-xml-retention.tsv 的 quest 25400 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/main/resources/aion/data/static_data/quest_definition/quests/24046.xml; .agents/summary/quest-systemic-audit/2026-09-17-systemic-quest-family-audit.zh-CN.md
validation: 静态 XML 校验与全量断言通过；阿斯特拉核心使命 25400 与 24046 领奖重复扣除完全清除
boundaries: 适用于中途已消耗道具或可选分支交付物；正常由最终 NPC 初次回收的收集物不受影响
superseded_by: none
see_also: [QE-006]
first_check: 检查背包道具是否在前面的 transition 中已经被 remove-item；reward -> complete 中是否有针对同一 item-id 的 has-item 或严格 remove-item
-->

- **判定规则**：凡是在中途步骤（例如调查石碑、制作钥匙、提交半成品）已被扣除的任务物品，在最终与奖励 NPC 交互（`reward -> complete`）时严禁再次声明 `has-item` 校验或 `remove-item count="1"` 扣除动作。否则玩家在终点 NPC 面前必然因缺少物品而无法交付任务。
- **代表案例**：阿斯特拉 66 级核心主线使命 `25400`（庞特卡内的悲剧），调查物品在 step 4 推进到 step 5 时已被扣除，但完成转换又声明了 3 件道具的 `has-item` 与 `remove-item`，导致全服魔族玩家在最终领奖时 100% 卡死；修复后彻底清除多余校验。

---

## [QE-021] 十九、真端多前置分支必须使用 start-condition-groups 保持析取语义 (DISJUNCTIVE_PREREQUISITE_GROUPS)
<!-- pattern-metadata
status: CONFIRMED
scope: 任务元数据前置条件（start-conditions 与 start-condition-groups）
first_seen: 2026-09-17
last_verified: 2026-09-17
symptom: 玩家已完成对应前置剧情，但后续任务在 NPC 处不可见或无法接取；只有把所有互斥分支/新老使命全部做完的非正常账号才能接取
root_cause: Aion 5.8 客户端 quest.xml 中的 finished_quest_cond1 与 finished_quest_cond2 属于可选完成其一（OR 关系）；服务端 XML 机械平铺在单个 start-conditions 中被解释为全量必须满足（AND），导致单分支玩家前置被死锁
fix_or_guardrail: 具有分支可选前置的任务必须升级为 start-condition-groups，每个 group 声明一个分支 finished 条件并携带公共互斥条件，引擎使用 DNF 析取逻辑（anyMatch）判定
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/2303.xml; src/main/resources/aion/data/static_data/quest_definition/quests/19008.xml; .agents/summary/quest-systemic-audit/2026-09-17-systemic-quest-family-audit.zh-CN.md
validation: 43 个真端分支前置任务全量升级为 start-condition-groups，静态解析与断言全部通过
boundaries: 适用于真端明确声明多个 finished_quest_condN 的分支任务；属于纯 AND 线性前置的任务保持单一 start-conditions
superseded_by: none
first_check: 比对 Quest_unpacked/quest.xml 中的 finished_quest_cond1/cond2，确认是否为分支剧情、互斥制作专精或新老版本使命替换
-->

- **判定规则**：真端客户端数据中凡声明了 `finished_quest_cond1`、`finished_quest_cond2` 等多个前置字段的任务，其业务语义是“完成路线 A **或** 路线 B 均可接取”。服务端严禁写在同一个 `<start-conditions>`（AND）中，必须使用 `<start-condition-groups>`，以 `<group>` 包裹各分支条件，保证走任一剧情分支的玩家都能顺利接取后续任务。
- **代表案例**：任务 `2303`（真端完成 2304 或 2305 或 2499 任一即可）、守护者系列 `19008` 等 43 个任务全量重构为 `<start-condition-groups>`。
---

## [QE-022] 二十、状态机拓扑可达性与击杀汇报源状态错位 (NO_REACHABLE_DEAD_END_NODES)
<!-- pattern-metadata
status: CONFIRMED
scope: 任务状态机节点流转、杀怪与击杀链目标节点、报告 NPC 源状态配置
first_seen: 2026-09-17
last_verified: 2026-09-17
symptom: 玩家杀怪达成数量后任务显示已完成，但去找对应 NPC 时 NPC 无响应或无汇报选项，任务无法推进至领奖，陷入“没有下一步”
root_cause: 击杀 transition 或 kill-chain 节点链将任务状态推进到了新状态（如 k1 或 k5），但 NPC_REPORT 或后续对话转换却机械配置了初始状态 source="started"；导致达成击杀后由于源节点不匹配，目标 NPC 无法触发领奖流转，形成可达死胡同
fix_or_guardrail: NPC_REPORT 的 source 必须精确匹配前序步骤/击杀链最终到达的节点 label；在 QuestDefinitionDirectoryLoaderTest 中增加全量状态图 BFS 遍历门禁 executableQuestsHaveNoReachableDeadEndNodes()，禁止除 COMPLETE 以外的任何无出边可达死胡同
evidence: retail-xml-retention.tsv 的 quest 16900 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; retail-xml-retention.tsv 的 quest 24151 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/main/resources/aion/data/static_data/quest_definition/quests/1640.xml; retail-xml-retention.tsv 的 quest 2569 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）
validation: 全库 6,186 个生产执行任务全量 BFS 连通性通过，0 死胡同节点，单测全部通过
boundaries: 适用于所有带中间推进节点（k1..kN, s1..sN）的状态机任务；纯 unaccepted 自环交互的活动派发任务（如 89999）除外
superseded_by: none
see_also: [QE-002]
first_check: 检查前序 kill/kill-chain 的 target node 是否与后续 NPC_REPORT 的 source node 一致
-->

- **判定规则**：任务状态机中任何可达节点（除 `COMPLETE` 终态外）必须具有通向下一阶段或完成状态的出边转移。凡通过 `kill-npc` 或 `kill-chain` 改变状态的任务，后续 `NPC_REPORT` 的 `source` 必须严格指向击杀结束后的目标节点，严禁误配为 `started`。
- **代表案例**：天族任务 `16900~16903`（杀怪后进入 k1 但报告 NPC 只认 started）、魔族任务 `24151`（5 连杀进入 k5 但报告 NPC 只认 started）、`1640`（reward 状态缺失安装部件完成路由）、`2569`（虚设 s2 死胡同）；修复后由全量 BFS 拓扑门禁永久拦截。

---

## [QE-023] 二十一、任务前置自依赖与循环死锁拦截 (PREREQUISITE_DEPENDENCY_CYCLE_FREE)
<!-- pattern-metadata
status: CONFIRMED
scope: 任务元数据前置条件（prerequisites、start-conditions、start-condition-groups）
first_seen: 2026-09-17
last_verified: 2026-09-17
symptom: 玩家无论达到何种等级或进度均无法在 NPC 处看到或接取任务，任务永久断链
root_cause: 任务配置了指向自身 ID 的 finished 前置条件（自指依赖），或多个任务相互引用形成闭环前置死锁，导致不完成自身就无法接取自身的拓扑死循环
fix_or_guardrail: 彻底清除自身依赖，在 CompletedQuestPrerequisiteRegressionTest 中增加全库前置拓扑有向图 DFS 环路门禁 noQuestRequiresItselfOrCreatesDependencyCycle()，禁止任何自环或相互依赖环路
evidence: retail-xml-retention.tsv 的 quest 18992 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）
validation: 全库 6,222 个任务全量前置 DFS 拓扑环路扫描 0 环路，单测通过
boundaries: 适用于所有任务前置依赖声明
superseded_by: none
see_also: [QE-001], [QE-021]
first_check: 检查 start-conditions 中 finished 条件的 quest-id 是否等于自身任务 ID，或是否存在 A->B->A 环路
-->

- **判定规则**：任务的前置条件（`prerequisites`、`start-conditions`、`start-condition-groups`）严禁引用自身任务 ID，且整个前置依赖有向图中严禁存在环路（Cycle-Free Directed Acyclic Graph）。
- **代表案例**：天族重大副本任务 `18992`，其 `<start-conditions>` 误配了 `finished quest-id="18992"` 导致自身死锁；修复后由 DFS 环路门禁守护。

---

## [QE-024] 二十二、任务影片循环与推进假死重复对话防线 (MOVIE_LOOP_AND_PROGRESS_STALL_FREE)
<!-- pattern-metadata
status: CONFIRMED
scope: 任务影片播放 (play-movie)、剧情推进对话 (SETPRO*) 与状态机流转
first_seen: 2026-09-17
last_verified: 2026-09-17
symptom: 看完电影没有下一步，再次点击 NPC 无限重复看电影；或者玩家点击推进选项（SETPRO*）后对话直接关闭或停在原步数，再次点击 NPC 完全重复对话，任务目标无法推进
root_cause: 1. 包含 play-movie 的 transition 被错误配置为 source == target 且无变量变更、无 movie-end 事件、无后续对话页下发，导致动画播放后任务依然停在原状态，再次交互形成无限观影循环；或在非击杀阶段错配击杀怪物播放电影（如 14047）；2. 核心推进动作（SETPRO*、QUEST_ACCEPT_1）被错误配置为 source == target 且 actions 为空，导致玩家点击推进后原地打转，无法推进到打怪/收集/交付阶段（如 14112, 24155, 28301, 50010, 15301, 25301, 1423）
fix_or_guardrail: 1. 为电影推进对话补充独立 step 节点（如 s1）并配置 set-variable，转移报告源至新节点，新节点配置防重播保护；2. 移除错配在非目标阶段的击杀电影垃圾路由；3. 补齐推进动作的目标节点、任务道具发放与变量变更；4. 新增 QuestMovieAndDialogLoopRegressionTest，对全服可执行任务建立无纯电影死循环门禁
evidence: retail-xml-retention.tsv 的 quest 16942 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; retail-xml-retention.tsv 的 quest 26942 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/main/resources/aion/data/static_data/quest_definition/quests/14047.xml; src/main/resources/aion/data/static_data/quest_definition/quests/14112.xml; retail-xml-retention.tsv 的 quest 24155 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/main/resources/aion/data/static_data/quest_definition/quests/28301.xml; src/main/resources/aion/data/static_data/quest_definition/quests/50010.xml; retail-xml-retention.tsv 的 quest 15301 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; retail-xml-retention.tsv 的 quest 25301 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/main/resources/aion/data/static_data/quest_definition/quests/1423.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMovieAndDialogLoopRegressionTest.java
validation: 全库 6,222 个任务全量扫描 0 纯电影死循环，测试用例通过
boundaries: 适用于所有含影片交互与 SETPRO 推进的任务；显式记录的 24053 fallback 除外
superseded_by: none
see_also: [QE-013], [QE-022]
first_check: 检查 play-movie 或 SETPRO 所在 transition 的 source 与 target 是否相同且无 actions，检查非目标阶段是否存在冗余 kill-npc 触发电影
-->

- **判定规则**：任务影片播放或 SETPRO 推进动作必须实现明确的状态迁移或变量改变。严禁配置 `source == target` 且 `actions` 为空的纯消费自环（除非有后续 `movie-end` 推进或明确翻页），严禁在非目标阶段错配全节点击杀怪物播放电影。
- **代表案例**：巴鲁纳研究所 `16942/26942`（SETPRO1 播放电影 899/900 留原地死循环）、阿祖图兰要塞 `14047`（错配 6 处击杀伊卡罗尼斯 214599 乱播电影 422）、消除污染 `14112`（SETPRO1 停在 started 无法推进到击杀）、结界塔修复 `24155`（SETPRO2 停在 started 无法破坏装置）、空中要塞 `28301`（SETPRO2 停在 started 无法拾取动力装置）、活动任务 `50010`（单身线 SETPRO2 停在 unaccepted 永远接不上任务）、圣灵装备 `15301/25301`（QUEST_ACCEPT_1 强制停留在 unaccepted）、飞行术 `1423`（SETPRO1 留原地无法交付）。修复后由 `QuestMovieAndDialogLoopRegressionTest` 全局防护。

---

## [QE-025] 二十三、任务掉落收集步数有效性与死锁防护 (QUEST_DROP_COLLECTING_STEP_VALIDITY)
<!-- pattern-metadata
status: CONFIRMED
scope: 任务元数据掉落 (drops collecting-step) 与运行时掉落服务判定
first_seen: 2026-09-17
last_verified: 2026-09-17
symptom: 玩家击杀任务指定怪物或采集目标物体成百上千次，永远无法掉落任何任务道具，任务彻底卡死无法进行
root_cause: 任务 XML 的 <drops> 中将 collecting-step 错误配置为非 0 且不存在于当前任务节点 var0 取值集合中的步数（例如任务只有 var0=0，drops 却配置了 collecting-step="2"）。QuestService 在判定掉落时若 collectingStep != 0 则严格校验 player.getQuestVarById(0) == drop.collectingStep()，导致条件永远不成立，掉落率直接变成 0%
fix_or_guardrail: 将不可达的 collecting-step 修正为正确的当前阶段步数或 0（允许整个 START 进行期间掉落）；在 QuestMovieAndDialogLoopRegressionTest 中新增 fatalImpossibleDropStepsAreEliminated() 全库门禁，遍历所有可执行任务的所有 drops，强制断言非 0 的 collectingStep 必须存在于任务声明的 var0 取值集合中
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/2372.xml; src/main/resources/aion/data/static_data/quest_definition/quests/4907.xml; retail-xml-retention.tsv 的 quest 24202 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/main/resources/aion/data/static_data/quest_definition/quests/24203.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMovieAndDialogLoopRegressionTest.java
validation: 全库 6,222 个任务全量扫描 0 不可达掉落步数，测试用例通过
boundaries: 适用于所有带 drops 的任务定义
superseded_by: none
see_also: [QE-024]
first_check: 检查 drops 中 collecting-step 是否非 0 且该值在 nodes 的 var0 中未定义
-->

- **判定规则**：任务 `<drops>` 中的 `collecting-step` 如果非 0，必须存在于任务 `nodes` 所声明的有效 `var0` 取值集合中。严禁配置任务生命周期中永远无法达到的步数，避免运行时掉落判定恒为 false。
- **代表案例**：魔族任务 `2372`（drops 误配 collecting-step="2" 但任务只有 var0=0）、间谍任务 `4907`（drops 误配 collecting-step="1" 但任务只有 var0=0）、布鲁斯特豪宁任务 `24202` 与 `24203`（drops 误配 collecting-step="2" 但任务只有 var0=0）。修复后由 `fatalImpossibleDropStepsAreEliminated` 门禁全局防护。

---

## [QE-026] 二十四、多档奖励组声明与档位奖励窗口一致性 (REWARD_GROUP_TIER_FIDELITY)
<!-- pattern-metadata
status: CONFIRMED
scope: 任务多档阶梯奖励 (reward-groups)、npc-complete complete-reward-index 与档位奖励窗口页
first_seen: 2026-09-17
last_verified: 2026-09-17
symptom: 玩家交出更高档位的兑换材料后，客户端奖励窗口显示的仍是第 1 档文案（例：交出 3 个线索却显示“收到了 1 个线索”），或任务声明了多档奖励但某一档位永远无法被发放（死档）
root_cause: 多档结算任务把各档奖励平铺在单个 <rewards> 容器里（物理上只有 1 个奖励组），引擎在 groups.size()==1 时按兼容契约不做组索引校验，于是档位只靠固定奖励索引区分，奖励窗口页也始终复用第 1 档页面；客户端文案与实际档位因此不一致。另一类风险是声明了多个 <reward-groups> 却没有任何完成路径发放其中某一档，形成永远拿不到的死档
fix_or_guardrail: 1. 多档任务按档位声明 <reward-groups>，每档一个 <group>，并让第 N 档入口下发 SHOW_SELECT_QUEST_REWARD_WINDOWn（档位文案来自客户端 select_quest_rewardN 页）；2. npc-complete 的 complete-reward-index 与固定奖励索引按本档自身组解析；3. QuestMovieAndDialogLoopRegressionTest 新增 multiTierQuestsNeverDeclareDeadRewardGroups() 全库门禁，声明多组的任务必须每档都能被发放（组索引被 CompleteQuest 引用，或该完成路径的显式 grant-reward 与该组内容完全一致）
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/50023.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMovieAndDialogLoopRegressionTest.java
validation: 全库 21 个多奖励组任务扫描 0 死档；QuestMovieAndDialogLoopRegressionTest 13 项用例全绿
boundaries: 适用于所有多档奖励/兑换任务；单组任务若确为真端单一奖励仍保持 <rewards> 平铺
superseded_by: none
see_also: [QE-025]
first_check: 检查多档任务是否声明了 <reward-groups>，第 N 档是否下发 SHOW_SELECT_QUEST_REWARD_WINDOWn，且每档都有可发放的完成路径
-->

- **判定规则**：多档结算任务必须按档位声明 `<reward-groups>`；第 N 档入口必须下发与该档客户端文案一致的 `SHOW_SELECT_QUEST_REWARD_WINDOWn`；每个声明的档位都必须存在可被发放的完成路径，严禁声明后无人发放的死档。
- **代表案例**：天族事件兑换任务 `50023`（凭 1 个线索换小盒、凭 3 个线索换大盒）原先两档共用 `SHOW_SELECT_QUEST_REWARD_WINDOW1`，交 3 个线索时显示的是 1 个线索的文案；修复为两组 `<reward-groups>` + 档位 2 下发 `SHOW_SELECT_QUEST_REWARD_WINDOW2`（客户端 `select_quest_reward2` 文案“您有 3 个线索啊”），并由 `multiTierQuestsNeverDeclareDeadRewardGroups` 门禁守护死档。

---

## [QE-027] 二十五、任务计时器生命周期闭环 (QUEST_TIMER_LIFECYCLE_CLOSURE)
<!-- pattern-metadata
status: CONFIRMED
scope: 任务计时器 (start-quest-timer / start-invisible-timer / cancel-quest-timer / quest-timer-end / invisible-timer-end)
first_seen: 2026-09-17
last_verified: 2026-09-17
symptom: 玩家已完成计时任务的交付，客户端却继续跑倒计时并在归零时按“超时”渲染；或计时器到期后事件被静默丢弃，任务的时限语义完全失效
root_cause: 任务通过 start-quest-timer / start-invisible-timer 启动了计时器，但既没有同 timer-id 的 cancel-quest-timer，也没有对应类型的 quest-timer-end / invisible-timer-end 事件路由，形成无人收尾的孤儿计时器
fix_or_guardrail: 1. 计时器使命结束（交付成功、进入下一阶段）时必须 cancel-quest-timer；2. 依赖超时推进/失败的任务必须提供 quest-timer-end（可见）或 invisible-timer-end（不可见）路由；3. QuestMovieAndDialogLoopRegressionTest 新增 startedQuestTimersHaveCancelOrExpiryRoute() 全库门禁，按可见/不可见类型分别匹配到期事件，逐个 timer-id 校验闭环
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/2230.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMovieAndDialogLoopRegressionTest.java
validation: 全库 28 个计时器任务扫描 0 孤儿计时器；QuestMovieAndDialogLoopRegressionTest 15 项用例全绿
boundaries: 43 秒 invisible “返回”计时器按真端语义只靠 invisible-timer-end 路由闭环，无需 cancel；2230 的 1800 秒赌注倒计时按真端 handler 在交付成功时 questTimerEnd 收尾
superseded_by: none
see_also: [QE-024]
first_check: 检查每个 start-quest-timer / start-invisible-timer 是否有同 timer-id 的 cancel-quest-timer，或有同类型的 timer-end 事件路由
-->

- **判定规则**：任务启动的每个计时器都必须闭环——要么有同 `timer-id` 的 `cancel-quest-timer`，要么有与计时器类型匹配的到期事件路由（可见 → `quest-timer-end`，不可见 → `invisible-timer-end`）。严禁出现既无取消又无到期处理的孤儿计时器。
- **代表案例**：魔族任务 `2230`（30 分钟赌注倒计时）迁移后漏掉真端 handler 成功交付时的 `questTimerEnd`，客户端在交完 10 颗棕熊尖牙后仍继续倒计时；补 `cancel-quest-timer timer-id="visible"` 后由 `startedQuestTimersHaveCancelOrExpiryRoute` 门禁守护。

---

## [QE-028] 二十六、多档奖励窗口档位页面与交付分支档位一致性 (REWARD_WINDOW_TIER_PAGE_FIDELITY)
<!-- pattern-metadata
status: CONFIRMED
scope: 任务奖励窗口预览 (npc-complete preview / 编译器合成预览) 与多档交付分支 (reward-groups + COMPLETE)
first_seen: 2026-09-17
last_verified: 2026-09-27
symptom: 多档任务在 REWARD 阶段重新与交付 NPC 对话（USE_OBJECT / SELECT_QUEST_REWARD）时，客户端渲染的仍是第 1 档奖励文案与物品；或两条交付分支各自进入的窗口与最终发放的档位相反（在“正直奖赏”窗口里拿到“满足愿望”档位的奖励）；或把**零奖励组**任务（事件类无 reward_item*）按 `rewardGroups().size() - 1` 送进档位查表，规范形编译当场失败（`reward tiers exceed the six client reward windows`）
root_cause: 1. QuestXmlBlockExpander.expandNpcComplete 的预览路由写死 ShowQuestDialog(5)，忽略 complete-reward-index，使第 2/3 档重开窗口时回到第 1 档；2. QuestDefinitionCompiler.restoreRewardPreviewContract 用 5 + rewardIndex 线性推算页面，第 5/6 档在客户端是页面 45/46，会下发不存在的页面 9/10；3. 1114 两条交付分支的 complete-reward-index 与进入窗口相反，导致窗口文案与实发奖励错配
fix_or_guardrail: 1. 新增唯一档位查表 QuestDialogPage.rewardWindowForTier(tier)（0..5 → 5/6/7/8/45/46，越界返回空），禁止任何线性偏移推算；2. expandNpcComplete 预览改用查表页面，声明预览却落在客户端未声明档位时以 NPC_COMPLETE_REWARD_WINDOW_UNSUPPORTED 编译失败；3. restoreRewardPreviewContract 改用同一查表，无窗口可用时不合成；4. 1114 的 Asteros 分支改 complete-reward-index="0"、Namus 分支改发第 2 档并修正 reward-index；5. QuestMovieAndDialogLoopRegressionTest 新增全库门禁：档位查表锁定、合成与显式预览页面、5,351 个 npc-complete 预览块逐块断言、多档任务“进入窗口档位 = 结算档位”；6. 编译器侧新增共享助手 RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage(metadata, questId)：rewardGroups 非空走 rewardWindowForTier(size-1)，为空兜底 SHOW_SELECT_QUEST_REWARD_WINDOW1（与引擎 completeFlow 预览同口径），CollectItem/Collect/Talk 各规范形交付段共用（判例 80829，零奖励组事件任务）
evidence: src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDialogPage.java; src/main/java/com/aionemu/gameserver/questEngine/definition/QuestXmlBlockExpander.java; src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDefinitionCompiler.java; src/main/resources/aion/data/static_data/quest_definition/quests/1114.xml; src/main/resources/aion/data/static_data/quest_definition/quests/1367.xml; src/main/resources/aion/data/static_data/quest_definition/quests/2430.xml; src/main/resources/aion/data/static_data/quest_definition/quests/50023.xml; src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleCollectItemDefinitionCompiler.java; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMovieAndDialogLoopRegressionTest.java
validation: mvn test -Dtest=QuestMovieAndDialogLoopRegressionTest,QuestDefinitionDirectoryLoaderTest,CompletedQuestPrerequisiteRegressionTest,QuestClientContractGateTest,ProductionCatalogWhitelistVerificationTest 共 30 项用例全绿；PRODUCTION_COMPILE_OK=6186、FAILURES=0、WHITELIST_VIOLATIONS=0；21 个多档任务静态审计 0 发现；2026-09-27 DD 尾片复验：deliveryWindowPage 兜底后 80829 受理恢复，DD 指纹面 1217 行漂移集恰为 228（D-a 116 ∪ D-b 112）、非漂移行零漂移
boundaries: 适用于所有声明 <reward-groups> 的多档任务与所有 npc-complete 预览；单档任务（rewardGroups == 1）继续固定第 1 档窗口；**零奖励组任务**（rewardGroups 为空）不得进入档位查表（`size()-1 = -1` 必抛越界），一律走 deliveryWindowPage 兜底固定窗 1；1122/2513 这类“按档位手写 grant-reward + 持久化索引 0”的体例以显式 grant 命中奖励组来判定档位
superseded_by: none
see_also: [QE-026], [QE-017]
first_check: 检查每个 npc-complete 预览下发的页面是否等于 rewardWindowForTier(complete-reward-index)，以及 REWARD 节点的进入窗口档位与实际发放档位是否一致
-->

- **判定规则**：奖励窗口页面只能由档位查表得到（第 1~4 档 = 页面 5/6/7/8，第 5/6 档 = 页面 45/46）；`npc-complete` 预览必须下发本档自己的窗口；多档任务中 `REWARD` 节点的进入窗口档位必须等于它实际结算的档位。严禁写死第 1 档页面或按 `5 + index` 线性推算，也严禁窗口与 `complete-reward-index`/`grant-reward` 档位相互矛盾。**零奖励组族（rewardGroups 为空的事件类任务）不得进入档位查表**——查表入参只能在非空集合上取 `size()-1`，为空时一律用 `deliveryWindowPage` 的兜底固定窗 1（与引擎 `completeFlow` 预览同口径）。
- **代表案例**：魔族任务 `1367`（三档收集窗口分别对应脖子肉/里脊/火鸟大腿肉，预览写死第 1 档导致第 2/3 档重开错档）、`2430`（reward_b/reward_c 预览同样回到第 1 档）、天族事件任务 `50023`（r2 重开路径仍显示第 1 档文案）、天族任务 `1114`（Asteros 与 Namus 两条交付分支的档位与窗口相反）；修复后由 `rewardWindowTierMappingCoversEveryClientWindow`、`synthesisedAndDeclaredPreviewUseTheTableDrivenTierPage`、`npcCompletePreviewsOpenTheirOwnTierWindowAcrossTheCatalog`、`multiTierHandInWindowsMatchTheirGrantedTier` 四项门禁守护。

---

## [QE-029] 二十七、任务生成 NPC 的权威 handle 存活语义 (QUEST_SPAWN_HANDLE_LIVENESS)
<!-- pattern-metadata
status: CONFIRMED
scope: QuestSpawnRegistry / PlayerQuestSpawnPort / 任务 slot 生成与重建
first_seen: 2026-09-17
last_verified: 2026-09-17
symptom: 任务 NPC 被世界级清理、其他玩家击杀或实例场景清理销毁后，玩家再回到同一阶段（含重登、重进副本、重新触发 self-loop 生成）时任务 NPC 不再出现，任务永久无法推进且没有任何报错
root_cause: slot 幂等原先只判断“该 slot 是否登记过 handle”，不判断 handle 是否仍在世界中；被注册表之外路径销毁的登记仍被视为“期望状态已满足”，后续 spawn-npc 被静默跳过，玩家再也拿不到该任务 NPC
fix_or_guardrail: 1. PlayerQuestSpawnPort.isUsableAuthoritativeHandle：真实 NPC 一旦已死亡或已离开世界即判定陈旧，无生命属性的替身保持幂等；2. QuestSpawnRegistry.replaceStale：按调用方判定的具体陈旧 handle 做 CAS 替换（slot 被清空时接管，并发下保留他人已换入的 handle 并取消旧跟随任务）；3. spawnNpc 与 spawnNpcRandom 的非替换路径统一改用该语义登记；4. 回归覆盖 PlayerQuestSpawnPortTest#questNpcDestroyedOutsideTheRegistryIsRebuiltForItsOwner、QuestSpawnRegistryTest#replaceStaleSwapsOnlyTheJudgedHandle、PlayerQuestSpawnPortRaceTest#aRegistrationRaceDoesNotLeaveAnUntrackedNpc 与既有幂等/清理用例
evidence: src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestSpawnRegistry.java; src/main/java/com/aionemu/gameserver/questEngine/runtime/PlayerQuestSpawnPort.java; src/test/java/com/aionemu/gameserver/questEngine/runtime/QuestSpawnRegistryTest.java; src/test/java/com/aionemu/gameserver/questEngine/runtime/PlayerQuestSpawnPortTest.java; src/test/java/com/aionemu/gameserver/questEngine/runtime/PlayerQuestSpawnPortRaceTest.java
validation: mvn test -Dtest=PlayerQuestSpawnPortTest,PlayerQuestSpawnPortRaceTest,QuestSpawnRegistryTest,QuestExecutionCoordinatorTest 共 34 项全绿；任务门禁套件 30 项全绿（PRODUCTION_COMPILE_OK=6186、FAILURES=0、WHITELIST_VIOLATIONS=0）
boundaries: 只把“真实 NPC 已死亡或已离开世界”视为陈旧；替换只作用于调用方判定的那个 handle，绝不按 templateId 删同类；终态清理（完成/放弃/登出/副本销毁）仍是 slot 回收的主路径
superseded_by: none
see_also: [QE-024]
first_check: 检查任务 slot 的幂等判定是否只看 contains，以及被击杀/离开世界后的 NPC 是否还能在同一 slot 重建
-->

- **判定规则**：任务 slot 的幂等语义是“权威 handle 仍可用”，不是“曾经登记过”。真实 NPC 一旦死亡或离开世界，该 slot 必须允许重建；重建只能替换调用方判定的陈旧 handle，并在并发下保留他人已换入的 handle，禁止制造无人登记的孤儿 NPC。
- **代表案例**：`1922` 的 `<delete-world-npcs/>` 会清空世界地图实例内的全部 NPC，任何在该实例留有 slot 登记的任务此后都无法再生成自己的 NPC；`QuestSpawnRegistry.replaceStale` + `isUsableAuthoritativeHandle` 让被杀/被外部销毁的任务 NPC 能重建，并由 `replaceStaleSwapsOnlyTheJudgedHandle`、`questNpcDestroyedOutsideTheRegistryIsRebuiltForItsOwner` 守护。

---

## [QE-030] 二十八、任务掉落契约真端基线门禁 (QUEST_DROP_CONTRACT_BASELINE)
<!-- pattern-metadata
status: CONFIRMED
scope: 任务元数据 <drops> / 真端 quest.xml 掉落契约 / 收集类任务交付
first_seen: 2026-09-17
last_verified: 2026-09-17
symptom: 击杀任务指定怪物或使用任务对象成百上千次永远拿不到任务道具，收集类交付目标（check-item / npc-item-report / has-item）无法满足，任务卡在收集阶段
root_cause: 1. 迁移时把任务对象的掉落行整体丢失（15400 三个雷山塔野外箱、51022 活动货箱、50019 情人节活动怪），道具只在别处需要、没有任何获得路径；2. 掉落来源被裁剪（14016 少一个来源、21107 少一个来源）或概率统一写成 100%（75 个任务与真端 drop_prob 不符，含 18834/4078 等 60/75 档）
fix_or_guardrail: 1. 按真端 drop_monster/drop_item/drop_prob/each_member 与旧 quest_data 数值 ID 恢复缺失掉落（15400/51022/50019），补齐被裁剪的来源（14016→210753、21107→216535）；2. 75 个任务的概率按真端收敛（67 个统一档 + 8 个修正档 + 6 个混合档先按 npc 模板名核对再逐行对齐）；3. 新增 test resource `/quest/quest-drop-retail-contract.tsv`（1,158 任务：掉落道具种数 + 按怪物加权的概率直方图，由真端 quest.xml 生成，生成脚本入库）+ `/quest/quest-drop-contract-exceptions.tsv`（仅 1127 直接 give-item 与 25604 刻意重构两条，含证据理由）+ `QuestDropContractGateTest` 全库门禁：生产可按同一批怪重新分行、可比真端多来源，但任一概率档的怪物数或掉落道具种数低于基线即失败
evidence: retail-xml-retention.tsv 的 quest 15400 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/main/resources/aion/data/static_data/quest_definition/quests/51022.xml; src/main/resources/aion/data/static_data/quest_definition/quests/50019.xml; src/main/resources/aion/data/static_data/quest_definition/quests/14016.xml; retail-xml-retention.tsv 的 quest 21107 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/test/resources/quest/quest-drop-retail-contract.tsv; src/test/resources/quest/quest-drop-contract-exceptions.tsv; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestDropContractGateTest.java; .agents/summary/quest/twin-pair-scan/audit_drop_shape_vs_retail.py
validation: 真端形态审计由 84 条不一致降为 6 条（4 条为生产多来源的超集、2 条为有证据豁免）；mvn test 任务门禁套件 31 项全绿，PRODUCTION_COMPILE_OK=6186、FAILURES=0、WHITELIST_VIOLATIONS=0
boundaries: 真端只提供名称，离线无法把名称映射成 ID，故基线为“按怪物加权的结构契约”而非逐道具逐怪精确比对；each-member 组合语义与任务目录之外的来源（商店/合成/其他系统）不在本门禁范围
superseded_by: none
see_also: [QE-025], [QE-029]
first_check: 比对任务 <drops> 与真端 quest.xml 的 drop_monster/drop_prob（按怪物加权直方图），以及收集类目标是否至少存在掉落或 give-item 获得路径
-->

- **判定规则**：任务的掉落契约不得低于真端 quest.xml——允许生产把同一批怪重新分行、允许比重端多来源，但**不得少于真端任一概率档的怪物数、不得丢失掉落道具**；收集类交付目标必须至少存在一条获得路径（掉落或 `give-item`）。豁免必须逐条写入有证据的例外清单，禁止任务级通配豁免。
- **代表案例**：雷山塔 `15400`（三个野外箱的掉落行整体丢失，s3 交付三个道具永远无法满足）、活动任务 `51022`（货箱掉落丢失）、`50019`（活动怪掉落丢失）、`14016`/`21107`（真端双来源被裁成单来源）、以及 75 个概率被写成 100% 的任务；由 `QuestDropContractGateTest` + 真端基线 TSV 守护。

---

## [QE-031] 二十九、任务道具角色归属与收集事件监听对象 (QUEST_ITEM_ROLE_OWNERSHIP)
<!-- pattern-metadata
status: CONFIRMED
scope: 任务 metadata <items>/<inventory-items>/<has-item>/<remove-item>/<collect-item> 与真端 collect_item/check_item 的角色归属
first_seen: 2026-09-17
last_verified: 2026-09-17
symptom: 收集任务的怪物掉落正常，交付对话却始终提示物品不足；或收集进度条完全不刷新、任务停在收集阶段无法进入下一步
root_cause: 1. 批量迁移把"道具块"按邻居任务复制：15010 写成 15011 的 quest_15011a(7)、15012 写成 15013a、15043 写成 15044a、15070 写成 15071a、51021 写成 51018a——掉落发的是自家道具，交付却校验邻居任务的道具，玩家永远凑不齐；2. collect-item 事件监听对象错写成邻居任务道具、count 误用掉落行数：28836 监听 quest_28835a(5)、28838 监听 quest_41257b(8)，而这两个任务各有 5/8 条掉落行，事件永不触发，客户端收集进度不刷新
fix_or_guardrail: 1. 5 个任务的交付条件改为自家真端道具（15010 = quest_15010a x5 + quest_15010b x3，15012/15043/15070 分别为 quest_15012a x5、quest_15043a x7、quest_15070a x10，51021 = quest_51017a x3）；2. 2 个任务的 collect-item 事件改为自家道具 + 收集数量（28836 → 182213207 count 50，28838 → 182213208 count 50，真端 collect_item/check_item 均为 50）；3. 新增 QuestItemSourceContractGateTest：全库不变量 I1「collect-item 事件只能监听本任务声明/发放/上报的道具」+ 7 个修复任务的交付集合回归；4. 追加批次：7 个 COLLECT_ITEM 任务（1932/3547/14121/14201/24121/24152/24242）自行掉落并声明任务道具、却没有任何交付校验 → 玩家可零进度领奖且道具永不消耗；在唯一进入 reward 的交付边补回 has-item + remove-item（数量取真端 collect_item，其中 3 个真端 COLLECT_ITEM 注释 NPC 与交付边 NPC 吻合）；5. 第三批：6 个多 NPC 变体任务（2232/2239/2289/3013/3088/4542）的 17 条进入 reward 的交付边全部无条件 → 任一入口可零进度领奖；每条交付边按自家道具与真端数量补 has-item + remove-item；6. 真端 collect_item/check_item 名称经 item_template `name_desc` 映射为 ID，生成 /quest/quest-item-role-baseline.tsv（3,660 任务）供后续逐条评审
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/15010.xml; retail-xml-retention.tsv 的 quest 15012 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; retail-xml-retention.tsv 的 quest 15043 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/main/resources/aion/data/static_data/quest_definition/quests/15070.xml; src/main/resources/aion/data/static_data/quest_definition/quests/51021.xml; retail-xml-retention.tsv 的 quest 28836 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; retail-xml-retention.tsv 的 quest 28838 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestItemSourceContractGateTest.java; src/test/resources/quest/quest-item-role-baseline.tsv; .agents/summary/quest/item-producer-scan/audit_cross_quest_item_roles.py; .agents/summary/quest/item-producer-scan/generate_item_role_baseline.py
validation: 道具开发名核对（182215668 = quest_15013a 而 15012 真端 collect/check 为 quest_15012a）与真端字段逐条比对确认 7 处均为迁移错配；全库不变量 I1 复核 0 违规
boundaries: 真端 collect/check 名称在我方交付集合中缺失的 76 行已逐条定性（item-role-gaps.tsv 的 verdict 列：48 外部来源、29 同类缺交付校验（无 reward 入边或已存在其它道具校验者待逐条确认）、22 本任务无掉落、11 名称待映射、4 他任务道具），未纳入门禁；item_template 缺少 name_desc 的道具无法映射，不参与该判定；跨任务道具交接链（如 13904 交付 13903 的道具、50048 消耗 50047 的奖励）属真端设计，不得按本模式一律判错
superseded_by: none
see_also: [QE-030]
first_check: 任务交付条件里的道具是否等于本任务 items/drops 里的道具（开发名见 item_template name_desc）；collect-item 事件是否监听本任务道具
-->

- **判定规则**：任务的交付/消耗条件只能校验**本任务自己声明或发放**的道具；`collect-item` 事件是客户端进度刷新的触发点，监听对象必须是本任务自己的道具。道具归属以 item_template 的 `name_desc`（`quest_<questId>...`）为权威，跨任务引用必须能给出真端 collect/check 或交接链证据。
- **代表案例**：`15012` 掉落 quest_15012a 却校验 quest_15013a（15013 的道具，同批 15010/15043/15070/51021 都是 +1 偏移复制）；`28836`/`28838` 的 collect-item 事件监听邻居任务道具并把 count 写成掉落行数（5/8），收集进度永不刷新；由 `QuestItemSourceContractGateTest` 守护。

---

## [QE-032] 三十、交付简写展开的无条件奖励路由 (NPC_REPORT_SHORTHAND_REWARD_ROUTE)
<!-- pattern-metadata
status: CONFIRMED
scope: <dialog type="NPC_REPORT"> / <npc-report> 简写展开、SELECT_QUEST_REWARD 交付路由、<items> 收集道具
first_seen: 2026-09-17
last_verified: 2026-09-17
symptom: 收集任务只要点交付按钮就直接进奖励窗，背包里一个任务道具都没有也能领奖；掉落出来的收集道具永远不被消耗、堆在背包里
root_cause: QuestXmlBlockExpander.expandNpcReport 把简写展开成两条边——QUEST_SELECT（展示页，source→source）与 SELECT_QUEST_REWARD（source→target/reward，conditions 与 actions 全空）。只有显式声明同 (source, npc, SELECT_QUEST_REWARD) 路由时，简写那条才会被 explicitDialogRoutes 过滤掉。51 个任务（多为多 NPC/多职业变体，合计 79 条路由）只写了简写，于是每条交付路由都是无条件的
fix_or_guardrail: 1. 51 个任务逐条补三条路由——priority=0 gated 交付路由（has-item + remove-item，数量取本任务 <items> 且与真端 collect_item 逐条相等）、priority=1 未集齐回落路由（CHECK_USER_ITEM_FAIL＝页 10001）、以及 FINISH_DIALOG(1008) 关闭路由（失败页上客户端渲染的关闭按钮必须有落点，缺则客户端契约门禁报 BUTTON_WITHOUT_ROUTE）；显式路由自动取代简写无条件边；2. 新增全库门禁 QuestItemSourceContractGateTest#everyRewardEntryBranchVerifiesTheQuestsOwnCollectedItems——只要任务同时声明并掉落某收集道具，其每个 (源节点, NPC) 交付分支都必须有一条覆盖该任务全部收集道具的 gated 路由
evidence: retail-xml-retention.tsv 的 quest 11003 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; retail-xml-retention.tsv 的 quest 15000 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; retail-xml-retention.tsv 的 quest 15072 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/main/resources/aion/data/static_data/quest_definition/quests/2307.xml; retail-xml-retention.tsv 的 quest 3096 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/main/resources/aion/data/static_data/quest_definition/quests/4940.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestItemSourceContractGateTest.java; .agents/summary/quest/item-producer-scan/item-handin-route-gaps.tsv
validation: 修复前 66 个任务命中该不变量；修复后简写类归零，剩余 30 个任务 / 42 条为既有无条件分支（含 3217/4217，真端 collect 名称未映射），全部留档为基线待评审；mvn test 任务门禁 34 项全绿
boundaries: 42 条既有无条件交付分支已逐条攻坚：修复 32 条无条件/旁路漏洞（包含 3217/4217 大写模板简写 4 条、1870/2870/10530/15478/15479/25478/25479/80795/80796/80797/80798/80888 共 20 条无条件交付旁路、1482/2430/20530/26930/26977/28739/28740/50021 共 8 条显式漏校验与魔族镜像对齐修复）；基线严格收敛至 10 条（经逐条确认均为合法的中间步骤已提前扣除，或多结局分支/副本宝箱例外）；reward→reward 的简写（13 个）是"重新打开奖励窗"，道具已在首次交付时扣除，**不得**加校验
superseded_by: none
see_also: [QE-031]
first_check: 检查任务是否只用 <dialog type="NPC_REPORT"> 简写交付；是则确认它是否声明并掉落 <items> 收集道具，并检查是否有同 (source, npc, SELECT_QUEST_REWARD) 的显式 has-item 路由
-->

- **判定规则**：简写交付边**天生无条件**——凡任务声明并在本地掉落收集道具，其每条非奖励阶段交付分支都必须显式声明带 `has-item` 的 `SELECT_QUEST_REWARD` 路由（引擎会用显式路由取代简写同路由）；未集齐的回落路由只允许显示失败页、不得进入奖励阶段。奖励阶段的 `reward→reward` 简写是重开奖励窗，必须保持无条件。
- **代表案例**：`15000` / `15072` / `2307` / `3096` / `4940` 等 51 个任务共 79 条无条件交付路由（含 2~5 个 NPC 变体），修复前均可零进度领奖；由 `QuestItemSourceContractGateTest` 的全库分支不变量守护。

---

## [QE-033] 三十一、quest_use_item 掉落必须有 ACTION_ITEM_USE 资格路由 (QUEST_USE_ITEM_DROP_REQUIRES_ACTION_ELIGIBILITY)
<!-- pattern-metadata
status: CONFIRMED
scope: 任务 metadata <drops> 中 AI 为 quest_use_item 的交互物；START 节点的 can-act ACTION_ITEM_USE 资格路由；启动校验与生产目录门禁
first_seen: 2026-09-18
last_verified: 2026-09-18
symptom: 服务端启动报 Can't initialize typed quest engine，原因是 quest <id> quest_use_item catalog drop npc <npc> item <item> collecting step <step> has no matching START ACTION_ITEM_USE eligibility route；或交互物在任务中无法使用、掉落永不触发
root_cause: 任务声明了 chance>0 且 NPC AI 为 quest_use_item 的目录掉落，但没有提供同 template-id、同源 START 节点的 ACTION_ITEM_USE 资格路由。缺掉落时校验不会遍历该 NPC；恢复或新增掉落行会把潜伏的不完整合同激活。51022 恢复 Event_Cargobox 701470 掉落后暴露该问题
fix_or_guardrail: 1. 每个 quest_use_item 掉落必须声明无副作用的 <can-act template-id="..." action-type="ACTION_ITEM_USE"/>，source 为 START；collecting-step=0 可用任意 START 节点，非 0 时 source 节点的 var0 必须等于 collecting-step；2. QuestInteractionObjectValidator.validateDefinition 作为启动与测试共享的校验入口；3. ProductionCatalogWhitelistVerificationTest 逐个 EXECUTABLE 定义调用该入口，使生产目录编译/白名单门禁在启动前捕获缺失资格；4. QuestInteractionObjectCatalogTest 继续覆盖交互物 Talk/drop 合同
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/51022.xml; src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestInteractionObjectValidator.java; src/test/java/com/aionemu/gameserver/questEngine/runtime/QuestInteractionObjectTestData.java; src/test/java/com/aionemu/gameserver/questEngine/runtime/QuestInteractionObjectCatalogTest.java; src/test/java/com/aionemu/gameserver/questEngine/ProductionCatalogWhitelistVerificationTest.java; .agents/summary/quest/2026-09-18-quest-51022-cargobox-action-eligibility.md
validation: mvn test -Dtest=QuestInteractionObjectCatalogTest,ProductionCatalogWhitelistVerificationTest 共 8 项全绿；PRODUCTION_COMPILE_OK=6189、PRODUCTION_INTERACTION_OBJECT_FAILURES=0、PRODUCTION_WHITELIST_VIOLATIONS=0；全库同口径静态扫描由 1 条缺口降为 0
boundaries: 只适用于实际 NPC AI 为 quest_use_item 的掉落；普通怪物掉落不需要 can-act。资格路由必须保持无 state/actions/after-commit 副作用；若交互物同时需要对话推进，必须另加显式 TALK 路由。测试 AI 查询必须保持与启动时 DataManager.NPC_DATA 的 quest_use_item 判定一致
superseded_by: none
see_also: [QE-030]
first_check: 遇到启动校验失败时，先查 drop npc 的 AI；若为 quest_use_item，检查是否存在同 template-id 的 START ACTION_ITEM_USE，且 source var0 与 collecting-step 匹配
-->

- **判定规则**：`quest_use_item` 的掉落是“交互物可使用”与“掉落目录”两半合同的组合；只补 `<drop>` 不补资格路由会在启动时 fail-closed。资格自环只表达可使用性，不能承担状态推进。
- **代表案例**：`51022` 商团货物箱子 `701470` 恢复 `182215183` 掉落后缺少 `started` 资格自环，导致 `QuestEngine` 启动失败；修复后由 `QuestInteractionObjectValidator.validateDefinition` 与 `ProductionCatalogWhitelistVerificationTest` 双重守护。

---

## [QE-034] 三十二、接取元数据与奖励数值的真端合同对齐 (RETAIL_START_METADATA_CONTRACT_ALIGNMENT)
<!-- pattern-metadata
status: CONFIRMED
scope: 任务 metadata 接取元数据（min-level/max-level/races/classes/gender/repeat）与档位 1 数值奖励（EXP/GOLD/AP/GP）；真端解包数据 sentinel 语义；生产目录级门禁
first_seen: 2026-09-18
last_verified: 2026-09-18
symptom: 等级/职业/阵营资格与真端不一致（过宽被低等级或非目标职业接取、过窄漏接）；多档任务误把档位 N 值当档位 1；npc-complete 索引越界启动失败
root_cause: 迁移与批量编辑漂移：min/max 等级抄写错误、base 职业死条目与进阶职业混淆、阵营拆分/武器适配/版本倍率等有意差异无例外台账、奖励插入位移 npc-complete 的 fixed/choice reward-index
fix_or_guardrail: 1. 真端 sentinel 语义：minlevel_permitted=999=占位任务（不可接，跳过比对）；maxlevel 0/998/999 与生产 2147483647/999/998 均为无上限；pc_light pc_dark=PC_ALL 等价；2. class token 直接映射，min>=10（转职）后 6 个 base 职业（WARRIOR/SCOUT/MAGE/PRIEST/TECHNIST/MUSE）为死条目，比对"实际可接受职业集合"；3. 整族一致的差异（如 329 条 max=82 封顶、AP 精确 ×4 倍率族 20 条）按 intentional 记例外清单，零散值按真端修复；4. 有意武器适配（奖励武器类型限定职业）与阵营拆分配对（15205/25205）逐条留证；5. 奖励行插入只允许容器尾部追加、删除前核对 npc-complete 索引合同；6. 三个目录级门禁 QuestRetailStartMetadataGateTest/QuestRetailClassGateTest/QuestRewardValueGateTest + 基线 TSV（quest-start-metadata-retail-contract.tsv、quest-class-retail-contract.tsv、quest-reward-value-retail-contract.tsv）从真端数据可复算
evidence: commit fbfbaba1c (P1/P2 min/max 9+28 条), 1a9a80f72 (P3 class 58 条), b93db336b (P4-1 数值 78 处); src/test/java/com/aionemu/gameserver/questEngine/definition/QuestRetailStartMetadataGateTest.java; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestRetailClassGateTest.java; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestRewardValueGateTest.java; .agents/summary/quest-systemic-goal/GOAL_PROGRESS.zh-CN.md
validation: 三个门禁测试全绿（6+3+1 例）；PRODUCTION_COMPILE_OK=6189、0 失败、0 白名单违规；基线 TSV 6222/135/6215 行
boundaries: 真端字段缺失=未配置（生产自建奖励属服务端设计）不等于字段为 0（真端明确无奖励）；整族一致差异必须先排除系统性设定再判缺陷；METADATA_ONLY 任务对玩家同样生效接取元数据，门禁必须覆盖
superseded_by: none
see_also: [QE-008], [QE-021]
first_check: 先用 audit 脚本做全库只读扫描归类（real defect / intentional variant / EVIDENCE_BLOCKED），确认 sentinel 与死条目归一后再逐条修复；有 npc-complete 索引合同的任务禁止头部插入与盲目删除奖励行
-->

- **判定规则**：接取元数据与档位 1 数值奖励的对齐必须以真端解包数据为唯一权威，但机械逐值替换是错的——先归一 sentinel（0/998/999/2147483647 均为无上限；999 同时用作占位任务）、等价表达（PC_ALL=双阵营；base 死条目）与系统性设定（整族一致封顶 82、AP ×4 倍率族），剩余零散差异才是缺陷。
- **代表案例**：min-level 9 条（1648=42、2641=41、19000~19003=50、25407/25408=68，镜像互证但 2641 与镜像 1641 真端本就不同）；max-level 28 条（19 条补真端上限、80621 族 82→65、27525/50074/80945/80946/80878 族内不对称残留 cap）；class 58 条（导师任务族漏声明、Kaliga 武器收集族/Dark Poeta 分组按真端写入、19074 镜像互证删 AETHERTECH、14031/24031 机甲星使命收窄）；奖励数值 78 处（2641/2724 多档任务档位 1 对齐、80993/80996 补 AP 50000）。道具旧名（956 条）与 title 名称映射（173 条）因缺真端模板表记 EVIDENCE_BLOCKED。

---

## [QE-035] 三十三、多杀任务的 `<kills>` 声明与 var1 计数器双口径 (KILL_COUNTER_VS_KILLS_METADATA)
<!-- pattern-metadata
status: CONFIRMED
scope: 任务 `<metadata><kills>` 击杀声明与 `<progress>` var1 击杀计数器；批量改写多杀任务（counter-grid / 自环累加 + 收口）时
first_seen: 2026-09-18
last_verified: 2026-09-18
symptom: 玩家报告“要杀的比任务说明多”（13758 族实为 5 杀却要杀 15/12）；结构门禁报 expected <N> but was <0>（断言 KillNpc 条数），或家族内 var1 目标与客户端条目互不一致
root_cause: `<kills>` 序列数被当作运行时击杀合同，但生产代码从不消费 QuestMetadata.kills()，真正的计数合同是转换里的 var1；b771eef59 按错误读数把 13758-13769 延长到 15/8/20/12/6/20，3b4e7fc4c 改成计数器时沿用这些错值（并把 transitions 开标签属性重写丢失，见 QE-037）
fix_or_guardrail: 1. 击杀数只认三份互相独立的真端证据：客户端 quest_monster.csv 的 SECTION_1<N 门控、data_driven_quest.xml 的 value0_progress_ 数量、911440146 旧 handler 的 var1 < N 阈值；2. `<kills>` 必须与计数器同口径（单条狩猎步骤列出怪物集合），禁止仅凭序列数反推击杀数；3. 计数器收口值 = 客户端门控值（below N-1 累加，at-least N-1 收口为 set N）；4. 批量改写只允许改结构，禁止顺带改计数目标
evidence: commits b771eef59, commit 3b4e7fc4c, commit f00d6e538; retail-xml-retention.tsv 的 quest 13758 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）, retail-xml-retention.tsv 的 quest 13761 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）, retail-xml-retention.tsv 的 quest 13764 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）, retail-xml-retention.tsv 的 quest 13765 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）, retail-xml-retention.tsv 的 quest 13767 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）, retail-xml-retention.tsv 的 quest 13769 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; .agents/summary/quest-counter-audit/audit_counters.py; .agents/summary/quest-counter-audit/census_counters.py; .agents/summary/quest-counter-audit/2026-09-18-counter-kill-and-repeat-gate-alignment.zh-CN.md
validation: production-gate | focused-test：QuestKillCounterRetailGateTest 4/4（414 个单计数器任务与客户端门控逐条比对，含 34 个“多杀一只”任务的批量修正）；mvn -o test -Dtest='com.aionemu.gameserver.questEngine.**.*Test' 1440 run / 0 failures / 0 errors
boundaries: 只覆盖单段击杀计数；SECTION_1 出现多行/多段的复合任务必须逐段确认；set 值 = 门控 + 1 与 = 门控 都是合法记账形态，不能按差值判缺陷；真端 UI 计数显示仍需实机确认
superseded_by: none
see_also: [QE-006], [QE-007], [QE-037]
first_check: 先跑 mvn -o test -Dtest='QuestKillCounterRetailGateTest'（planner 模拟出引擎要求的击杀数并与客户端门控比对），仍存疑时用 .agents/summary/quest-counter-audit/audit_counters.py 对比客户端门控与 var1 目标，再用 git show 911440146:<quest/*/_<id>*.java> 核旧 handler 阈值；断言 KillNpc 条数的测试是旧链式形状的残留，不是运行时合同
-->

- **判定规则**：`<kills>` 是声明、var1 才是合同。两者不一致时以客户端表与旧 handler 的三方一致结果为准，并把 `<kills>` 修正到同一口径，而不是让门禁去数序列。
- **代表案例**：13758/13761/13764/13767 因误读被延长到 15/12 杀（客户端与旧 handler 均为 5），玩家需多杀 7-10 只；13765/13769 的 `<kills>` 声明为 8/20 条而同族计数器是 5，属同一漂移的两面。

---

## [QE-036] 三十四、可重复任务的 COMPLETE 重开局对话必须显式 start-eligible (REPEAT_COMPLETE_START_DIALOG_GATE)
<!-- pattern-metadata
status: CONFIRMED
scope: 任务开局对话路由；max-repeat-count > 1 的可重复任务；NPC_START 生成块与手写 complete→complete 路由
first_seen: 2026-09-18
last_verified: 2026-09-18
symptom: 生产目录门禁报 missing repeat dialog route: quest=<id> source=complete npc=<npc> dialog=<page>；或重复任务完成后无法重新打开开始页、或在不合格状态下仍显示开始页
root_cause: `<dialog type="NPC_START">` 只为 source（unaccepted）生成开局路由，selection-sources 只影响 FINISH_DIALOG；COMPLETE 状态重开局必须手写 complete → complete 镜像。f00d6e538 用生成块替换手写块时补了镜像却漏掉 <start-eligible/>
fix_or_guardrail: 1. 可重复任务的每个 NONE→NONE 开局路由（含页面自环）都必须在 COMPLETE 节点上有同 event + 同 after-commit 的镜像，且镜像必须带 <start-eligible/>；2. unaccepted 侧镜像保持无条件下发页面；3. 运行期依据：QuestMutationPlanner.matchesSourceStatus 只允许带 StartEligible 的转换把 COMPLETE/LOCKED 快照跨越到 NONE 起点
evidence: src/test/java/com/aionemu/gameserver/questEngine/definition/QuestDefinitionCatalogManifestTest.java; src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestMutationPlanner.java; commit f00d6e538; src/main/resources/aion/data/static_data/quest_definition/quests/2677.xml, retail-xml-retention.tsv 的 quest 1742 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）, retail-xml-retention.tsv 的 quest 2317 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）, retail-xml-retention.tsv 的 quest 11202 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）
validation: production-gate：全库 2735 条重复开局检查本次仅 2677 缺失 1 条，补齐后 QuestDefinitionCatalogManifestTest 与 questEngine 全包全绿
boundaries: 仅适用于 max-repeat-count > 1 的任务；单次任务不得为通过门禁硬加 StartEligible 镜像；页面自环只负责下发页面，不得携带状态推进动作
superseded_by: none
see_also: [QE-006], [QE-011]
first_check: 门禁失败时先确认失败路由是否属于 NPC_START 生成集合，再对比同族已对齐任务（如 1742/2317）的 complete 镜像写法，不要改门禁放宽
-->

- **判定规则**：开局页由 NONE 起点声明，重开局页由 COMPLETE 起点声明；两条镜像的差别就是后者必须带 `start-eligible`。
- **代表案例**：2677 的 `complete → complete SELECT1_1(1012)` 缺条件，是 f00d6e538 结构化重写时的手工遗漏（同批 44 个任务都带条件）。

---

## [QE-037] 三十五、重写 transitions 开标签必须保留块级属性 (REPORTED_REWARD_MODE_ATTR_LOSS)
<!-- pattern-metadata
status: CONFIRMED
scope: 生产 quest XML 的块级属性（当前为 transitions reported-reward-mode="FIXED|CHOICE|CLASS"）；批量脚本或手工重写 transitions 容器时
first_seen: 2026-09-18
last_verified: 2026-09-18
symptom: QuestReportedRewardCoverageTest 报 quest=<id> action=108 expected 1 but was 0（客户端“无目标自动领奖”路线消失）；游戏内只能走选定目标领奖
root_cause: 3b4e7fc4c 批量重写多杀任务的 transitions 块时把三个任务的 transitions reported-reward-mode="FIXED" 写成裸 transitions，expandReportedRewards 不再派生 SELECTED_QUEST_AUTO_REWARD(108) 路由；同批 13947 属性保留，构成可见对照
fix_or_guardrail: 1. 任何重写 transitions 开标签的批处理都必须整行读取并保留属性（禁止按字面 <transitions> 匹配替换）；2. 该属性是客户端实时报告合同的开关，删掉等于静默关闭一条领奖路径；3. 门禁 QuestReportedRewardCoverageTest 覆盖 182 个客户端可实时报告任务（FIXED/CHOICE/CLASS 三类）
evidence: commit 3b4e7fc4c, commit f00d6e538; retail-xml-retention.tsv 的 quest 13841 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）, src/main/resources/aion/data/static_data/quest_definition/quests/13845.xml, src/main/resources/aion/data/static_data/quest_definition/quests/13849.xml, retail-xml-retention.tsv 的 quest 13947 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/main/java/com/aionemu/gameserver/questEngine/definition/QuestXmlBlockExpander.java
validation: production-gate：QuestReportedRewardCoverageTest 182 任务全部通过；questEngine 全包 1440 run / 0 failures
boundaries: 只适用于声明该属性的任务族；属性值必须与奖励结构匹配（FIXED 要求无 selectable/class 奖励，否则编译期 fail-closed 报 REPORTED_REWARD_METADATA_MISMATCH）
superseded_by: none
see_also: [QE-021], [QE-035]
first_check: 自动领奖路线缺失时先看 transitions 是否还有 reported-reward-mode，再看奖励结构是否满足对应模式的编译前置
-->

- **判定规则**：块级属性是行为开关，重写容器行等于改行为；批量编辑按「结构不动属性、属性不动结构」分离。
- **代表案例**：13841/13845/13849 因属性丢失少了 FIXED 派生的无目标领奖路线，13947 保留属性可作对照。

---

## [QE-038] 三十六、阵营日常必须声明 npc-faction-id 且轮换星期位不得全 0 (NPC_FACTION_DAILY_OWNERSHIP)
<!-- pattern-metadata
status: CONFIRMED
scope: category=FACTION 的每日/每周势力任务；quest_data.xml 的 npcfaction_id、npc_factions_quest.xml 的星期位与生产 XML metadata.npc-faction-id
first_seen: 2026-09-19
last_verified: 2026-09-19
symptom: 势力日常“怎么接都接不到”；GM `//quest start` 只给通用失败提示；或某任务永远不会出现在每日轮换里
root_cause: NpcFactions.sendDailyQuest 的候选池按 metadata.npcFactionId() 过滤，缺声明等于永不入池（同时 PlayerQuestStartEligibilityPort 会跳过阵营校验，门禁反而更松）；轮换表星期位全 0 时 isActiveOn 恒假，任务同样永不轮换。旧路径 QuestService.startQuest 反而用 legacy npcfaction_id，导致新旧要求不一致形成死锁
fix_or_guardrail: 1. 归属取值以 quest_data.xml 的 npcfaction_id 与 npc_factions_quest.xml 的 faction_id 两源一致为准，逐任务写入 metadata npc-faction-id；2. 轮换行要么缺省（isActiveOn 视为每天可发）要么至少一个星期位为 1，禁止全 0；Elyos/Asmodian 镜像与 legacy repeat_cycle=ALL 可作补掩码证据；3. 门禁 QuestNpcFactionRetailGateTest 同时校验归属基线、日常池组成与星期位；4. GM 调试用 `//quest set <id> START 0`（绕过 start 检查），`//quest start` 受 legacy maxlevel_permitted 限制且 dialogId=0 时不打印真实原因
evidence: src/main/resources/aion/data/static_data/quest_data/quest_data.xml; src/main/resources/aion/data/static_data/npc_factions/npc_factions_quest.xml; src/main/resources/aion/data/static_data/quest_definition/quests/35059.xml; src/main/java/com/aionemu/gameserver/model/gameobjects/player/npcFaction/NpcFactions.java; src/main/java/com/aionemu/gameserver/questEngine/runtime/PlayerQuestStartEligibilityPort.java; src/test/java/com/aionemu/gameserver/model/gameobjects/player/npcFaction/QuestNpcFactionRetailGateTest.java; src/test/resources/quest/quest-npc-faction-retail-contract.tsv; .agents/summary/quest-counter-audit/2026-09-18-counter-kill-and-repeat-gate-alignment.zh-CN.md
validation: focused-test：QuestNpcFactionRetailGateTest + NpcFactionsCanonicalCatalogTest 4/4；全量 mvn -o test 见提交信息
boundaries: 只覆盖能被玩家接取的阵营日常；缺轮换行按“每天可发”处理（isActiveOn 语义），不得据此删行；等级/阵营成员等水平门禁属正常拒绝，不算缺陷
superseded_by: none
see_also: [QE-021], [QE-035]
first_check: 先用 quest_data.xml + npc_factions_quest.xml 两源比对生产 XML 的 npc-faction-id 与星期位；再看角色等级是否超过 max-level（属正常拒绝）
-->

- **判定规则**：势力日常要“接得到”，必须同时满足归属声明、星期位、势力成员与等级门禁；缺任何一项都不是玩家能自己解决的操作问题。
- **代表案例**：35059（Alabaster Order 日常）缺 `npc-faction-id="2"` 导致永不入池；同批 218 个任务缺归属、44 个任务星期位全 0。

---

## [QE-039] 三十七、迁移不得用 enter-zone 自动接取替代 legacy NPC 接取 owner (LEGACY_START_OWNER_LOSS)
<!-- pattern-metadata
status: CONFIRMED
scope: 从 origin/history Java handler 迁移到 quest-definition XML 的接取语义；NONE 状态接取路由与客户端任务列表入口
first_seen: 2026-09-19
last_verified: 2026-09-19
symptom: 在 NPC 任务列表点第 10 页任务行后服务端反复下发同一页（SM_DIALOG_WINDOW page=10 -> CM_DIALOG_SELECT action=31 -> page=10 循环），表现为“任务怎么点都接不到”；查 XML 却发现存在 unaccepted->started 路由
root_cause: 51b4cb971 迁移把 registerOnEnterZone 一律概括成“进区域自动接取”，于是同时注册了 addOnQuestStart(npc) 的任务丢掉了 NPC 接取 owner：客户端在第 10 页点任务行发 QUEST_SELECT(31)，服务端没有 unaccepted+NPC+31 路由，DialogService 回退再次下发第 10 页形成循环。共有 18300/28300/1393/14123/15322/16800/17500/21080/25322/27500 十个任务命中；15322/25322 的正确语义是 legacy onAtDistanceEvent（at-distance 接取），14123/16800/17500/27500/21080 的接取 owner 一直是对话 NPC，enter-zone 只是 START 阶段的推进或生成
fix_or_guardrail: 1. legacy handler 的 addOnQuestStart(npc) 是权威接取 owner，迁移必须落成 NPC_START（或在 NONE 状态显式路由该 NPC 的 QUEST_SELECT/QUEST_ACCEPT_*），enter-zone 只能表达 START 阶段的推进，二者不得互相替代；2. 只有当 legacy 该 NPC 在 NONE 状态没有对话分支（或只有 onEnterZone/onEnterWorld/onAtDistanceEvent 建档）时，才允许 unaccepted 自动接取，与 AUTO_START_KEEPS_NONE_DIALOG_FREE 一致；3. 被下发的客户端任务页上每个可见按钮都必须在该状态有路由：1393 补完 1003->1013->1002 链后才满足 QuestClientContractGateTest
evidence: commit 51b4cb971（错误迁移，同时退休 18300/28300/1393/21080 等 handler）；origin/history 下对应 quest/handlers 旧 handler（`git show 51b4cb971^:<path>`）；src/main/resources/aion/data/static_data/quest_definition/quests/18300.xml、28300.xml、1393.xml、14123.xml、15322.xml、25322.xml、21080.xml；src/main/java/com/aionemu/gameserver/questEngine/definition/QuestXmlBlockExpander.java；src/test/java/com/aionemu/gameserver/questEngine/definition/QuestEnterZoneStartOwnerRegressionTest.java；.agents/summary/quest-enter-zone-start-owner/README.md
validation: focused-test 6 类/38 用例 0 失败（QuestEnterZoneStartOwnerRegressionTest、Quest14123ZoneSpawnTest、MigratedQuestRepairDefinitionTest、QuestResidualCounterLocksTest、Quest26800ClientDialogAlignmentTest、LegacyTemplateMirrorRouteRegressionTest）；production-gate 5 类/31 用例 0 失败（QuestClientContractGateTest 于 -Dquest.client.contract.failOnStaleBaseline=true 下 PAGE_NOT_IN_TASK_HTML=0 / BUTTON_WITHOUT_ROUTE=0，PRODUCTION_COMPILE_OK=6189、WHITELIST_VIOLATIONS=0）；Aion 5.8 客户端实测未执行
boundaries: 静态与 headless 门禁通过不等于客户端验收；audit_enter_zone_start_owner.py 只覆盖 51b4cb971^ 的 handler 集合，audit_unreachable_start.py 命中的 33 个任务由 RetailAreaEngine/NpcFactions/EVENT/物品等 XML 外机制接取，属观察清单
superseded_by: none
see_also: [QE-006], [QE-035]
first_check: 任务点不动时先确认该 NPC 在 NONE 状态是否有 QUEST_SELECT(31) 应答；再把 origin/history handler 的 addOnQuestStart 与 XML 的 NPC_START/at-distance 逐任务对照
-->

- **判定规则**：接取 owner 只能有一个权威来源（legacy handler 的注册 + 客户端任务列表入口），迁移时用“进区域自动接取”顶替 NPC 接取会让客户端点击变成死循环。
- **代表案例**：18300 在 804699 处点击任务行后 page=10 循环；同批 10 个任务按 legacy 语义分别恢复 NPC_START、at-distance 或删除多余自动接取。

---

## [QE-040] 三十八、击杀计数结束时必须把任务说明行索引推进到报告行 (KILL_COUNTER_COMPLETION_ADVANCES_JOURNAL_ROW)
<!-- pattern-metadata
status: CONFIRMED
scope: 同一个 SECTION_0 说明行上并行门控多组击杀计数（SECTION_1..N<N）的击杀任务，最后一次击杀进入 REWARD 时的 packed step 合同与旧存档迁移
first_seen: 2026-09-19
last_verified: 2026-09-19
symptom: 一组或多组击杀计数已经打满、服务端 SM_QUEST_ACTION 已下发 状态=REWARD，但客户端任务说明仍停在击杀行；计数行出现 (/N) 空分子，下一步「报告某 NPC」不出现
root_cause: 任务说明行索引由 SECTION_0 承载、击杀计数在 SECTION_1+；reward 节点投影把 var0 固定成计数行，started->reward 的终击路线只写计数字段。QuestMutationPlanner 先应用 actions，再用目标节点投影补足未被动作触及的字段，于是终态 SECTION_0 停在计数行，客户端继续渲染击杀步骤；旧 handler 在同一刻写的是 setQuestVarById(0, 报告行)
fix_or_guardrail: 终击必须在同一事务内写 SECTION_0=报告行（S+1）并保留 LEVEL_AND_VISIBILITY_REFRESH；reward 节点投影的 var0 必须与报告行一致（reward 源节点的既有路由按该投影匹配）；started->started 击杀自环显式 set var0=所在阶段以自愈脏数据；已持久化的 REWARD/SECTION_0=计数行 存档用无 source 的 ENTER_WORLD 迁移路线（status-is REWARD + var0==所在阶段 -> set var0=报告行）修复
evidence: commit c34458083; retail-xml-retention.tsv 的 quest 15001 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; retail-xml-retention.tsv 的 quest 15203 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMonsterProgressContractAuditTest.java; .agents/summary/quest-15001-multicounter-step/2026-09-19-15001-double-counter-step-closure.zh-CN.md; .agents/summary/quest-15001-multicounter-step/2026-09-19-section0-report-row-closure-audit.zh-CN.md; .agents/summary/quest-acceptance/15001-2026-09-19-section0-report-row-client-accepted.md; .agents/summary/quest-acceptance/15101-2026-09-19-section0-report-row-client-accepted.md
validation: QuestMonsterProgressContractAuditTest（含 runtime planner 断言 20801）、ClientQuestSectionAlignmentTest、ProductionCatalogWhitelistVerificationTest、QuestDefinitionCatalogManifestTest 通过（PRODUCTION_COMPILE_OK=6189、FAILURES=0、WHITELIST_VIOLATIONS=0）；2026-09-19 用户确认 15001 客户端验证完成；同型 sweep 已修复 246 个任务（244 批量 + 18994/28994）并新增 QuestSection0ReportRowContractTest + quest-section0-report-row-contract.tsv（246 行合同快照），该测试的 Maven 门禁已于 2026-09-19 授权执行并通过（PRODUCTION_COMPILE_OK=6189、FAILURES=0、WHITELIST_VIOLATIONS=0，5 个测试类 26 用例）；第三轮残余 4 任务与 3 个组合节点任务已修复并复跑门禁（32 用例全绿、PRODUCTION_COMPILE_OK=6189、FAILURES=0、WHITELIST_VIOLATIONS=0）；2026-09-19 用户确认 15101 客户端验收完成（记录 .agents/summary/quest-acceptance/15101-2026-09-19-section0-report-row-client-accepted.md）
boundaries: 行索引与计数字段是两个独立合同字段，只断言 status=REWARD 或计数饱和不算闭环；同型批量修复只对结构同型任务机械套用（reward 投影 + 自环钉行 + 终击写报告行 + ENTER_WORLD 迁移），多阶段/自定义节点/无击杀路线的任务必须逐个判定；若缺的是计数自环/字段错位复用 QE-012 与 COUNTER_SOURCE_PROJECTION_NO_LOCK，若缺的是进入 REWARD 的路线本身复用 QE-018；迁移修复路线只在 ENTER_WORLD 触发，在线且不重登/不切图的旧存档不自动纠正
superseded_by: none
see_also: [QE-012], [QE-018], .agents/summary/quest-15001-multicounter-step/2026-09-19-section0-report-row-closure-audit.zh-CN.md
first_check: 用 quest_monster.csv 找同一 SECTION_0==S 上并行门控多个 SECTION_n<N 的任务，再核对 reward 投影 var0、终击 actions 与 enter-world 迁移路线；旧 handler 是否在完成分支写 setQuestVarById(0, 报告行)
-->

- **判定规则**：击杀任务的“杀满即报告”由两个字段共同完成——计数（`SECTION_1+`）与任务说明行索引（`SECTION_0`）。最后一条击杀路线必须把行索引写成报告行，并且 `reward` 节点投影要与之一致；否则服务端进入 `REWARD` 而客户端任务说明停在击杀行，出现空分子与“下一步不出现”的玩家可见症状。
- **代表案例**：15001（绿雾湿地双计数）修前追踪 `状态=4 步数=20800`（`SECTION_0=0, SECTION_1=5, SECTION_2=5`），修后 `20801`；同批 15020/15073/15100/15104/15203/15406/15407/15408/15580/15671/25671/25060/18952 同型；代表测试 `QuestMonsterProgressContractAuditTest#stepZeroMultiCounterHuntsAdvanceSectionZeroToTheReportStep`。
- **同型存量**：2026-09-19 审计命中 248 个可执行任务（旧 handler 在完成分支写 var0/报告行索引、当前 XML 未推进），两轮共修复 **268 个合同行**（246 + 22；第二轮把旧 handler 证据扩展到 `setQuestVar(N)`/`changeQuestStep(env, cur, next, bool)`，并把 5 个链式阶段任务的 var0 扩为 6-bit）。残余 4 个（15101 缺 0->1 对话推进行、24153 缺击杀路线、25304 缺中间行推进、25604 缺计数事件与推进）与 7 行组合节点任务（`SECTION_0` 非单纯行索引，需要客户端 VarTable 证据）均标为 EVIDENCE_REQUIRED。；该持久化迁移不能只依赖 ENTER_WORLD：跨部署在线或在同一会话进入 REWARD/计数行的存档会因 reward 源节点投影匹配失败而点 NPC 无响应 → 已为 244 个任务补 source-less 的领奖对话框自愈路线（复制自身 reward 响应页），24 个无对话响应的任务仍只依赖 ENTER_WORLD。
- **`SECTION_0` 双语义判定（2026-09-19 第三轮，必须先判语义再套合同）**：`SECTION_0` 有两种互斥语义，只由客户端 `quest_monster.csv` 的条件写法决定，不能用任务名/阵营/等级/奖励格数/有没有 `counter-grid` 块来猜。
  1. `SECTION_0` 出现在 `<N` 计数条件里（如 `SECTION_0<1;SECTION_5==0`）→ 该槽位**就是计数器本身**（链式或单行狩猎，`SECTION_0==S` 是链式门控），落盘按 `counter`/`counter-grid`/`kill-chain` 合同核对每维计数与 START 节点笛卡尔积；代表 1102、18911/28911、23918、24153、24155、17106。
  2. `SECTION_0` 从不出现在 `<N` 里、只出现 `SECTION_0==S` 门控 → 该槽位是**任务说明行索引**，报告行 = HTML `<step>` 数 - 1，落盘按本 Pattern 的报告行合同核对；代表 15001、15101、25304、25604、14252/24252。
  判定顺序错了会把整个链式家族误判成「缺报告行」或反过来把行索引任务当成计数器，2026-09-19 的 7 行「组合节点任务需要 VarTable」结论即由此误判产生（已撤销：18911/28911 无需改动，14252/24252 是行索引，23918 只是怪物 ID 错且少一维）。
- **第三轮残余清零（2026-09-19）**：15101（补 `0->1` 对话推进行与击杀路线）、24153（重建 5 个 0/1 计数器与 5 条击杀路线）、25304（补 `0->1->2` 中间行与 60 计数事件）、25604（补计数事件与行推进）、14252/24252（重建 `r0..r3` 行索引）、23918（改 5 维 `counter-grid` 与正确怪物 ID）全部修复；合同快照 269 行；门禁 32 用例全绿、`PRODUCTION_COMPILE_OK=6189`、`FAILURES=0`、`WHITELIST_VIOLATIONS=0`。
- **编译器口径（同批踩坑；含 2026-09-19 自我更正）**：`reward -> reward` 的 `SELECT_QUEST_REWARD(1009)` 不要手写——当任务同时用 `npc-complete` 声明了可查表的完成档位时，`restoreRewardPreviewContract` 会再派生一条 dialogId 通配（-1）的奖励预览路线，两条同源同事件直接撞 `AMBIGUOUS_TRANSITION`，交回编译器派生即可。**与之相对，报告 NPC 上的无 source TALK 自愈与既有 `reward -> reward` 路线并存是安全的**：`QuestDefinitionCompiler.compatibleSourceNodes` 用节点投影求值条件，`variable-below var0 <报告行` 在 `reward` 投影上求值为假 ⇒ 自愈路线的可匹配来源节点集合为空，两条路线互斥。本条目初稿曾把首次编译失败同时归因给 TALK 自愈，随后用编译探针证伪（把手写 1009 删除、自愈插回的三份 XML 全部编译通过：14252/24252 transitions=39、24153 transitions=38），三条自愈路线已恢复并由 `QuestMonsterProgressContractAuditTest` 锁定；用条件在节点投影上的真假判定路线冲突，不要靠「同 NPC 同事件」的直觉。
- **残余长尾（下一轮候选，非本轮范围）**：审计重跑后 `SAME_CLASS_CONFIRMED` 46 行（26 行 reward 投影 0/偏一、11 行怪物无击杀路线、其余多阶段偏移）、`COUNTER_CHAIN_GAP` 32 行、`REVIEW_LEGACY_NO_VAR0` 5 行（16974/23934/23935/23938/28952）、`REVIEW_NO_LEGACY` 2 行（25082/25608）；清单见 `.agents/summary/quest-15001-multicounter-step/section0-report-row-closure-residual.csv`。


---

## [QE-041] 三十九、本地关闭按钮不能作为服务端续接点 (HANDOVER_CHECK_PAGE_LOCAL_CLOSE_CONTINUATION)
<!-- pattern-metadata
status: CONFIRMED
scope: Aion 5.8 客户端 HACTION_FINISH_DIALOG(1008) 页面的下发语义；上交检查页（check_user_item_ok/check_user_item_fail）与成功分支续接页（奖励窗 5 / DEFAULT_SUCCESS 10002）
first_seen: 2026-09-19
last_verified: 2026-09-19
symptom: 玩家上交证物/收集物后服务端已推进到 REWARD 并下发 check_user_item_ok，但对话停在原地、点按钮没有下一步；必须重新与同一 NPC 对话才收到 DEFAULT_SUCCESS(10002) 并打开奖励窗
root_cause: Aion 5.8 客户端把 HACTION_FINISH_DIALOG(1008) 当成本地关闭动作，点击只发 CM_CLOSE_DIALOG、不回传任何任务 action；服务端若把“下发 check_user_item_ok(10000) 后等客户端回传 1008 续接”当作合同，该分支永远没有后继。10501 的 s6->reward 成功分支即为此形态，失败页 10001 的 1008 与成功页同名但带 SELECT_QUEST 落点，是唯一的服务端可见落点
fix_or_guardrail: 1. 上交成功分支不得停在下发 check_user_item_ok(10000) 的对话上；应直接下发目标状态下同 NPC 的续接页（USE_OBJECT(-1) 入口页：奖励窗 SHOW_SELECT_QUEST_REWARD_WINDOW1(5) 或 DEFAULT_SUCCESS(10002)）；2. 只有客户端 HTML 中该 ok 页存在会回传任务 action 的可见按钮（1009/故事翻页等）时才保留确认页形态（Playbook 8.25 CHECK_CONFIRMATION_PAGE_CONTRACT）；3. 判定必须读任务自身 HTML 的按钮动作，不得按任务名或任务族猜测
evidence: commit 75312dcdc; retail-xml-retention.tsv 的 quest 10501 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; docs/quest/client-dialog-mapping/quest-dialog-action-details.csv:645; src/test/java/com/aionemu/gameserver/questEngine/definition/Quest10501HandoverContinuationTest.java; src/test/java/com/aionemu/gameserver/questEngine/e2e/QuestHandoverContinuationAuditTest.java; src/test/java/com/aionemu/gameserver/questEngine/e2e/HandoverContinuationContract.java; .agents/summary/quest-10501-handover-continuation/README.md; .agents/summary/quest-acceptance/10501-2026-09-19-client-accepted.md
validation: 聚焦与客户端契约门禁 47/47（含 Quest10501HandoverContinuationTest 2/2、QuestHandoverContinuationAuditTest 2/2）；production-catalog PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0；Aion 5.8 客户端实测已验收（2026-09-19 用户确认“客户端验证成功”）
boundaries: “1008 为客户端本地关闭”来自实机 packet trace（下发 10000 后无 CM_DIALOG_SELECT 1008）与 10501 页面证据；同批 41 个任务/42 条分支按合同静态锁定，未逐个实机复验；无同 NPC 续接页的任务（122 候选中的 55 个）保持确认页终端形态，不得套用本卡
superseded_by: none
see_also: [QE-032], [QE-031]
first_check: 先看该任务客户端 HTML 里 check_user_item_ok 页的按钮动作；若是 HACTION_FINISH_DIALOG(1008)，再看目标节点是否存在同 NPC 的 USE_OBJECT(-1) 续接页
-->

- **判定规则**：`HACTION_FINISH_DIALOG(1008)` 不产生服务端任务动作；凡客户端把上交确认页渲染成纯 1008 按钮，服务端必须在状态提交的同一次 after-commit 里直接下发续接页，而不是等待 1008 回包。
- **代表案例**：10501（被毁的遗迹）上交龙族证物后停在页 10000、重新对话才进 10002；同批 41 个任务/42 条分支按同一合同把成功分支改到奖励窗 5 或 10002（10504 与 10501 同形）。

---

## [QE-042] 四十、inventory-items 是接取携带门禁，只能来自真端 inventory_item_name (INVENTORY_ITEMS_ACCEPT_GATE_SOURCE)
<!-- pattern-metadata
status: CONFIRMED
scope: 任务 metadata <inventory-items> 与真端 Quest_unpacked/quest.xml 的 inventory_item_name*/check_item*/collect_item* 字段角色
first_seen: 2026-09-19
last_verified: 2026-09-19
symptom: 在 NPC 任务列表点任务行后对话框立刻关闭（SM_DIALOG_WINDOW page=0）或任务行点不动、永远接不到；客户端动作是 QUEST_ACCEPT_SIMPLE(20000)，服务端无异常堆栈，容易误判成接取路由缺失
root_cause: metadata/inventory-items 在 typed engine 中被 PlayerQuestStartEligibilityPort 当作接取前置；私有 quest_data.xml 迁移把真端 check_item（任务中段交付/使用道具）写进 inventory_items（该字段的真端来源是 inventory_item_name），玩家接取时尚未持有该道具 → REQUIRED_INVENTORY_ITEM_MISSING，unaccepted→s0 转换不提交，DialogService 按“未处理的任务动作不得回显成对话页”关窗
fix_or_guardrail: 1. metadata/inventory-items 只允许来自真端 quest.xml 的 inventory_item_name*；check_item/collect_item 分别属于交付校验与收集合同（QE-031/QE-032），不得充当接取门禁；2. 任务中段的 give-item/item-play/remove-item 合同不得跟着删；3. 改 XML 时必须同步删 quest_data.xml 的对应 inventory_items 行，保持迁移来源一致
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/30721.xml; src/main/resources/aion/data/static_data/quest_data/quest_data.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestInventoryStartItemGateTest.java; src/test/resources/quest/quest-inventory-start-item-retail-contract.tsv; .agents/summary/quest-30721/2026-09-19-quest-30721-inventory-start-gate.zh-CN.md; .agents/summary/quest-inventory-start-item/audit_inventory_start_items.py; .agents/summary/quest-inventory-start-item/inventory-start-item-gaps.tsv
validation: focused-test（QuestInventoryStartItemGateTest 2/2；相邻回归 32/32：QuestItemSourceContractGateTest、QuestRetailStartMetadataGateTest、QuestStartEligibilityContractTest、PlayerQuestStartEligibilityPortTest、QuestEnterZoneStartOwnerRegressionTest）；production-gate（QuestDefinitionCatalogManifestTest、ProductionCatalogWhitelistVerificationTest、QuestClientContractGateTest、QuestDialogOrderAuditTest、QuestPageButtonAuditTest 31/31，PRODUCTION_COMPILE_OK=6189 / FAILURES=0）；runtime 与 client 未复验，需用户重建资源并重启服务端后实测
boundaries: 全库审计反向违规（生产声明、真端无 inventory_item_name）为 0；仅剩“真端有、生产 XML 未声明”的 78 条缺口（inventory-start-item-gaps.tsv 标 XML_MISSING_GATE）与 223 条无生产 XML 的任务（NO_QUEST_XML），均未批量补——未证明这些任务要求接取前携带；30721 与 18300 不是同一根因（18300 属 QE-039 的 legacy 接取 owner 丢失），两者症状相似但判定路径不同
superseded_by: none
see_also: [QE-039], [QE-031], [QE-032]
first_check: 见到“点任务后直接关窗/接不到、动作 20000 后 page=0”时，先查该任务 metadata/inventory-items 是否来自真端 inventory_item_name，再看 PlayerQuestStartEligibilityPort 的 REQUIRED_INVENTORY_ITEM_MISSING 分支
-->

- **判定规则**：`inventory-items` 声明的是“玩家接取前必须携带”的道具，语义来源只有真端 `inventory_item_name*`；把 `check_item`/`collect_item` 当接取前置会把任务变成不可接取，症状与“接取路由缺失”高度相似，必须用真端字段名区分。
- **代表案例**：30721（真端只有 `check_item1_1 = quest_30721a 1`；玩家在 s1→s2 才从 804868 拿到 182215698，s2→s3 由 ITEM_PLAY 消耗）；按真端 inventory_item_name 反向核对生产 199 条声明后异常声明归零。

---

## [QE-043] 四十一、任务道具使用区域定义与剧本怪物触发收口 (QUEST_ITEM_USE_ZONE_AND_AMBUSH_CONTRACT)
<!-- pattern-metadata
status: CONFIRMED
scope: 任务道具使用区域判定（ItemTemplate usearea 与 zones_quest.xml）、data_driven_quest.xml 的 ItemPlay 袭击怪物机制与唯一领奖人路由
first_seen: 2026-09-19
last_verified: 2026-09-19
symptom: 到达任务指定地点使用道具时提示「无法在此处使用该物品」(1300143)；使用道具后本应出现的偷袭怪物缺失；领奖对话跳过故事页直接弹领奖框或接取 NPC 提前截胡完成
root_cause: 1. 道具模板声明了 usearea（如 LF5_ITEMUSEAREA_Q30721），但 zones_quest.xml 中缺失对应 zone 定义，PlayerRestrictions#canUseItem 校验失败拦截；2. data_driven_quest.xml 中声明了 Relative 怪物生成，XML 漏配 spawn-npc-at-player 与对话后的 despawn-npc；3. npc-complete 预览包含 USE_OBJECT(-1) 跳过了 QUEST_SELECT 触发的 DEFAULT_SUCCESS；接取 NPC 被误写进 npc-complete 导致提前截胡
fix_or_guardrail: 1. 道具 usearea 必须在 zones_quest.xml 中补入，坐标与半径采信 5.8 客户端解包 source_sphere.csv；2. data_driven_quest.xml 的 ItemPlay 袭击怪使用 spawn-npc-at-player 挂载对应 slot，并在后续 talk 或 SET_SUCCEED 中通过 despawn-npc 清理；3. npc-complete 预览仅保留 SELECT_QUEST_REWARD，移除 USE_OBJECT；非交付 NPC 严禁配置 npc-complete
evidence: src/main/resources/aion/data/static_data/zones/zones_quest.xml; src/main/resources/aion/data/static_data/quest_definition/quests/30721.xml; src/main/resources/aion/data/static_data/quest_definition/quests/30771.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/Quest30721And30771RetailFlowTest.java; .agents/summary/quest-30721/2026-09-19-quest-30721-and-30771-retail-flow-and-ambush-repair.zh-CN.md
validation: focused-test (Quest30721And30771RetailFlowTest 3/3); production-gate 36/36 (PRODUCTION_COMPILE_OK=6189 / FAILURES=0); XML schema valid
boundaries: 仅适用于道具自身限制了使用区域（hasAreaRestriction）的任务；无袭击怪物的纯使用道具任务只配 ItemPlay 不需要配怪物槽位
superseded_by: none
see_also: [QE-042], [QE-031]
first_check: 道具无法使用时先查 item_template 的 usearea 是否在 zones_*.xml 中注册；领奖直接弹窗时查 npc-complete 的 preview actions 是否包含 USE_OBJECT
-->

- **判定规则**：`item_template` 的 `usearea` 必须在 `zones_quest.xml` 中以 `zone_type="ITEM_USE"` 形式存在，否则 `PlayerRestrictions` 会返回 1300143 拦截；`data_driven_quest.xml` 中带 `Relative` 的剧情袭击怪需在道具使用时刷出并在后续交互中销毁；汇报故事页依赖 `QUEST_SELECT`，`npc-complete` 预览不得包含 `USE_OBJECT` 以免故事页被跳过。
- **代表案例**：30721/30771（Cygnea/Enshar 提亚马特城堡残骸任务；真端 `source_sphere.csv` 分别定义 `LF5_ITEMUSEAREA_Q30721` 与 `DF5_ITEMUSEAREA_Q30771`；使用镇静剂/恢复剂后偷袭怪为 `236654` 德拉坎）。

## [QE-044] 四十二、收集步变量提前推进导致客户端交付对白脱节 (COLLECT_PROGRESS_PREMATURE_ADVANCE_DIALOG_DROPPED)
<!-- pattern-metadata
status: CONFIRMED
scope: 客户端 quest.xml 的 <collect_progress>N 与 data_driven_quest.xml 的 CollectItem 步骤契约；物品收集交付 NPC 对白（QUEST_SELECT、CHECK_USER_HAS_QUEST_ITEM）
first_seen: 2026-09-19
last_verified: 2026-09-19
symptom: 玩家完成收集且背包持有足量任务道具，但找到交付 NPC 时，NPC 头顶无任务对白标记，点击交互时下发 questId=0 通用第 10 页 (SM_DIALOG_WINDOW 玩家=xx targetObj=xx questId=0 下发页=10)，任务卡在收集步无法推进交付；计数残留臂还表现为感应区/影片/步骤门控不触发（进入非计数阶段时整型步数被 var1/var2 高位污染，如 10507 s7 影片 993 不触发）
root_cause: Aion 5.8 客户端严格根据 collect_progress=N（或 DDQ 步骤 N）在 progress==N (var0==N) 且持有任务道具时才触发该任务对白请求。若在步骤 N 时因采集物/祭坛交互、拾取道具（get-item）提前推进 var0=N+1，或前置击杀阶段推进至步骤 N 时未清零局部计数器（如 var1 残留导致打包步数变为 (2<<6)|2=130），导致客户端看到的整型步数与步骤 N 脱节，NPC 对话无法触发（//quest set <id> START N 强制覆写为纯净整型步数后才恢复）；或因 collecting-step 写死导致后置步骤无法采集道具造成死锁
fix_or_guardrail: 1. 采集物/祭坛交互给予道具时严禁提前推进 var0，必须保持 var0==N 停留同阶段，且交互完成后下发 PACKET_ONLY 同步包；2. 前置击杀阶段转移至步骤 N 时必须显式清零局部计数（set-variable field="var1" value="0"），保证打包步数纯净为 N；3. CHECK_USER_HAS_QUEST_ITEM 路由必须配置在 var0==N，成功后在事务内扣除道具并推进 var0=N+1（进入后续对白阶段）；4. 收集物 drop 规则有前置多目标时 collecting-step 必须放宽（如 collecting-step=0），且后续步骤必须保留 can-act 与 USE_OBJECT，避免击杀提步后采集死锁；5. 增加 enter-world 自愈回退路由（处于 N+1 且持旧道具时回退至 N，或已在 N 但有残留 var1 时清零）；6. 增加 QuestCollectProgressAlignmentGateTest 门禁测试锁定全服同类任务
evidence: retail-xml-retention.tsv 的 quest 10503 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/main/resources/aion/data/static_data/quest_definition/quests/10530.xml; src/main/resources/aion/data/static_data/quest_definition/quests/20530.xml; retail-xml-retention.tsv 的 quest 10504 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/main/resources/aion/data/static_data/quest_definition/quests/1373.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestCollectProgressAlignmentGateTest.java; .agents/summary/quest-collect-progress-alignment/README.md; .agents/summary/quest-acceptance/10503-2026-09-19-client-accepted.md; .agents/summary/quest-acceptance/10504-2026-09-19-client-accepted.md; .agents/summary/quest-acceptance/10507-2026-09-19-client-accepted.md
validation: focused-test (Quest10503ClientDialogAlignmentTest, Quest10504ClientDialogAlignmentTest, Quest10530ClientDialogAlignmentTest, Quest20530ClientDialogAlignmentTest, Quest1373ClientDialogAlignmentTest, QuestCollectProgressAlignmentGateTest 10/10 PASS); catalog-gate (QuestDefinitionCatalogManifestTest 10/10 PASS, QuestItemSourceContractGateTest 3/3 PASS); client-acceptance (10503、10504、10507 用户 2026-09-19 实机回复验证完成，未限定分支；10506 已随其进攻回廊修复单独验收；同批 20504/10527/20527/10528/20528/10530/20530/1373 仍待逐任务实机验收)
boundaries: 适用于所有客户端声明 collect_progress 或 DDQ 声明 CollectItem 的任务，以及跨阶段计数残留会污染打包整型步数（(var1<<6)|var0 等）的多阶段任务（如 10507 的 s7 感应区/影片门控）；无收集步骤且无跨阶段计数残留的纯杀怪/纯对话任务不适用
superseded_by: none
see_also: [QE-025], [QE-041]
first_check: 交付 NPC 下发 page 10 时，优先对比客户端 quest.xml 的 collect_progress 与玩家当前 quest_vars 的 var0 阶段值
-->

## [QE-045] 四十三、旧 handler 领奖投影必须等于进入 REWARD 前的 packed step (LEGACY_REWARD_ENTRY_KEEPS_PACKED_STEP)
<!-- pattern-metadata
status: CONFIRMED
scope: 由 Java handler 迁移而来的 typed 任务定义：旧 handler 以 changeQuestStep(env, from, to, true)、SET_REWARD/setStatus(REWARD)、useQuestItem(..., true) 进入 REWARD 的领奖投影与旧存档恢复
first_seen: 2026-09-09
last_verified: 2026-09-20
symptom: 领奖阶段任务书空白、任务信息消失、背包已有任务道具但下一 NPC 不显示、无法领奖；SM_QUEST_ACTION 状态=REWARD 的步数比报告行大 1（15300 为 状态=4 步数=14）
root_cause: 旧 QuestHandler.changeQuestStep(..., reward=true) 只写 QuestStatus.REWARD、不写 nextStep，旧存档在 REWARD 的 packed var0 仍是进入前的 from；迁移却把 reward 节点投影与 START -> REWARD 交接动作写成 to(=from+1)。QuestMutationPlanner 先应用 actions 再用目标投影补足未触及字段，最终 packed step 与客户端任务书行索引错位
fix_or_guardrail: 1. reward 节点投影必须等于旧 handler 进入 REWARD 前的 packed step；2. START -> REWARD 交接 transition 不得再 set-variable 该字段，只保留物品扣除/发放；3. 必须补无 source 的 ENTER_WORLD 恢复边（status-is REWARD + 变量 == to -> reward，仅 LEVEL_AND_VISIBILITY_REFRESH）纠正已落盘错位存档；4. 迁移或新增任务后必须运行 .agents/summary/quest-reward-projection-audit/audit_legacy_reward_projection.py，偏差要么修要么在 summary 显式记录"有意重定基线"的客户端证据
evidence: commit f6aff952a; commit 075464ebd; src/main/resources/aion/data/static_data/quest_definition/quests/15300.xml; src/main/resources/aion/data/static_data/quest_definition/quests/25300.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/Quest15300And25300RewardProjectionTest.java; src/test/java/com/aionemu/gameserver/questEngine/definition/LegacyRewardStepProjectionRegressionTest.java; .agents/summary/quest-15300-reward/2026-09-19-reward-projection.zh-CN.md; .agents/summary/quest-reward-projection-audit/2026-09-20-legacy-reward-projection-audit.zh-CN.md; docs/quest/repair-playbook/CASES.zh-CN.md 案例 8.18
validation: focused-test (Quest15300And25300RewardProjectionTest 2/2、QuestInstanceExitRecoveryTest 2/2，2026-09-20 聚焦批 22 例全绿); production-gate (ProductionCatalogWhitelistVerificationTest + QuestDefinitionDirectoryLoaderTest + QuestDefinitionCatalogManifestTest 13/13 PASS); client (用户 2026-09-19/20 确认 15300 全程顺利完成、奖励可领取); 全库审计 2026-09-20：490 个旧 handler 领奖入口，106 MISMATCH / 32 MISSING_RECOVERY_EDGE 待处置
boundaries: 只适用于旧 handler 确实没有在进入 REWARD 时改写 packed var 的任务；若旧 handler 有 setQuestVar/changeQuestStep(..., false)/checkQuestItems，或迁移有意重定基线，必须先按客户端 quest_summary 行清单（Dialogs/*/quest_q<id>.html 的 <steps>/<step>）核对再动，不得机械批量套用；同型 106 个 MISMATCH 尚未批量修复，15301/25302 等折叠步骤链的家族需要单独设计；恢复边只在 ENTER_WORLD 触发，在线且不重登/不切图的旧存档不会自动纠正；`useQuestItem(..., from, to, true)` 里被旧 helper 丢掉的 `to` 是否就是客户端领奖行，必须按 QE-051 用客户端 quest_summary 与镜像任务（q/q+10000）逐任务判定
superseded_by: none
see_also: [QE-002], [QE-040], [QE-041], [QE-051]
first_check: 先比旧 handler 进入 REWARD 的调用参数（from/to）与当前 reward 节点投影；再看 START -> REWARD transition 是否 set-variable 该字段；最后确认是否存在 status-is REWARD + 变量==to 的无 source enter-world 恢复边
-->

- **判定规则**：`reward` 节点投影 = 旧 handler 进入 `REWARD` 前的 packed step（`from`），交接 transition 不改写该字段，并存在 `REWARD && 变量 == to` 的无 source `enter-world` 恢复边；三者缺一都会让已落盘的错位存档继续空白。
- **为什么反复出现**：`f6aff952a` 的修复对象是人工上报的 22 个任务，落地形式是硬编码 ID 的回归锁而不是"扫描全部旧 handler"的规则门禁；迁移工具链没有这一项检查，于是 2026-08-04 的 1500 任务迁移、2026-08-05 的 evergale/high-daevanion 迁移（15300/25300 就在其中，`cfc2fa048` 时已是 `var0=14`）与 redemption_landing 批次都可以重复引入。
- **代表案例**：15300/25300（高等大天使线，旧 handler `changeQuestStep(env, 13, 14, true)`；修复后 `reward var0=13` + `REWARD var0=14 -> reward` 恢复边；2026-09-19/20 客户端验收通过）。

---

## [QE-046] 四十四、引擎外推进 REWARD 必须对齐领奖投影并注册领奖态入口页 (EXTERNAL_REWARD_WRITER_REENTRY_CONTRACT)
<!-- pattern-metadata
status: CONFIRMED
scope: 引擎外（questEngine/commands/gmhandler 之外）直接 setStatus(QuestStatus.REWARD) 的客户端包、服务与 AI：reward 节点投影、领奖态入口页、无 source enter-world 自愈边；typed 引擎按 (status, packed var) 匹配的语义
first_seen: 2026-09-20
last_verified: 2026-09-21
symptom: 领奖阶段（REWARD）与 NPC 对话只有通用「结束对话」，点击任务行没有本任务的完成对话（select_success 10002）、无法打开奖励窗口；任务书 Vars 与 reward 投影不一致（例：Vars 0 0 0 0 0 + Status REWARD）
root_cause: 1. 迁移只保留 started -> reward 的 SELECT_QUEST_REWARD 路由，漏掉 reward + QUEST_SELECT(31) -> DEFAULT_SUCCESS 领奖态入口页，而客户端点任务行发的正是 31；2. 引擎外写入方 setStatus(REWARD) 留下的 packed step 与 reward 节点投影不一致（10522/20522 写入 0 却投影 1，15545/25545 写入 1 却投影 0），QuestMutationPlanner#matchesSourceNode 要求 source 节点投影的每个变量都等于实际 packed 变量，于是该存档匹配不到任何 reward 路由；3. 批次 8 把这类不一致统一到客户端领奖行（QE-051）：写入方在 setStatus(REWARD) 前显式写领奖行 var0，reward 投影同步改成领奖行，旧写入方留下的旧值改由 enter-world 自愈边覆盖
fix_or_guardrail: 1. 先用只读审计脚本枚举引擎外的 setStatus(QuestStatus.REWARD) 写入方（排除 questEngine/commands/gmhandler），记录每个任务写入后的 packed step；2. reward 节点投影必须等于该步数；3. 进入 REWARD 的 transition 不得再 set-variable 该字段（打包步数由目标投影决定）；4. 每个完成 NPC 必须注册 reward -> reward + TALK_TO_NPC(QUEST_SELECT 31) -> SHOW_QUEST_PAGE DEFAULT_SUCCESS(10002)，conditions/actions 全空，1009 继续由 npc-complete 预览打开奖励窗口；5. 定义曾经投影过别的值时补无 source ENTER_WORLD 自愈边（status-is REWARD + 变量 == 旧值 -> reward，仅 LEVEL_AND_VISIBILITY_REFRESH）；6. 基线 TSV + 回归测试锁定任务清单，新增引擎外写入方直接失败；6. 当写入方步数与客户端领奖行（QE-051）冲突时，以客户端领奖行为准**同时**改写入方与投影：写入方在 setStatus(QuestStatus.REWARD) 前写领奖行 var0（qs.setQuestVarById(0, 1)，批次 8 覆盖 CM_CREATIVITY_POINTS#checkQuestCompletion、CoalescenceService#updateQuestsOnCoalescenceComplete、RiftOrbAI2#forQuest），reward 投影改领奖行，并把“自愈边”条件改为旧写入方留下的旧值，最后重刷 external-reward-advance-baseline.tsv；14. 领奖态入口页的 owner 必须同时具备 `SELECT_QUEST_REWARD(1009)` 的完成路线（`npc-complete` 的 `<preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>`），否则客户端契约门禁报 BUTTON_WITHOUT_ROUTE（28208/28209：客户端 quest_summary 末行是 Anja = npc 205321，但只有 205320 有完成块）
evidence: src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestMutationPlanner.java:340 (matchesSourceNode 按 packed 变量匹配 source 节点); src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_CREATIVITY_POINTS.java:107 (checkQuestCompletion 只置 REWARD); src/main/java/com/aionemu/gameserver/services/toypet/MinionService.java:320 (checkQuest 先 setQuestVar(1)); src/main/java/com/aionemu/gameserver/services/item/CoalescenceService.java:151 (updateQuestsOnCoalescenceComplete); src/main/java/com/aionemu/gameserver/ai/instance/beshmundirTemple/RiftOrbAI2.java:52 (forQuest); commit 93be8ddca (origin/history ref 的旧 handler _10522Using_Essence：31 -> 10002 / 1009 -> 5); Aion 5.8 客户端 quest_q10522.html (select_success 10002 唯一按钮 HACTION_SELECT_QUEST_REWARD 1009); src/main/resources/aion/data/static_data/quest_definition/quests/10522.xml 等 10 个任务; src/test/java/com/aionemu/gameserver/questEngine/definition/ExternalRewardAdvanceReentryContractTest.java; src/test/resources/quest/external-reward-advance-baseline.tsv; .agents/summary/quest-10522-reward-reentry/2026-09-20-external-reward-advance-reentry.zh-CN.md; .agents/summary/quest-10522-reward-reentry/audit_external_reward_advance.py; .agents/summary/quest-10522-reward-reentry/verify_external_reward_reentry_contract.py; src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_CREATIVITY_POINTS.java:107 (批次 8：置 REWARD 前先 qs.setQuestVarById(0, 1)); src/main/java/com/aionemu/gameserver/services/item/CoalescenceService.java:149 (批次 8 同上); src/main/java/com/aionemu/gameserver/ai/instance/beshmundirTemple/RiftOrbAI2.java:65 (批次 8 同上，覆盖 30211/30213/30311/30313); src/test/resources/quest/external-reward-advance-baseline.tsv (10 任务 writer step=1 / projection=1 / recovery=[0]）; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestClientContractGateTest.java（批次 8 回归：28208/28209 的 BUTTON_WITHOUT_ROUTE|reward|205321|31|10002|1009 指纹）；src/test/resources/quest/quest-client-contract-baseline.tsv（基线 2026-09-11 刷新时 0 缺陷，新指纹必须直接失败）；docs/quest/client-dialog-mapping/quest-dialog-pages.csv + quest-dialog-action-details.csv（28208 的 select_success(10002) 页只有按钮 1009 HACTION_SELECT_QUEST_REWARD）
validation: static (10 任务静态合同校验器 + XML 解析 + git diff --check 通过; 客户端页面索引 10/10 存在 select_success(10002)+1009); focused-test (2026-09-21 授权 Maven: 回归批 40 例全绿，含 Quest10522AutoStartDialogTest/Quest20522AutoStartDialogTest/ExternalRewardAdvanceReentryContractTest/Quest30311RetailAlignmentTest/Quest30313RetailAlignmentTest/MinionServiceTest/LegacyRewardStepProjectionRegressionTest); client-contract (QuestClientContractGateTest + QuestDialogOrderAuditTest + QuestStepDialogTerminationTest 19 例全绿，且以 -Dquest.client.contract.failOnStaleBaseline=true 运行); production-gate (ProductionCatalogWhitelistVerificationTest + QuestDefinitionDirectoryLoaderTest + QuestDefinitionCatalogManifestTest 13 例全绿，PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / WHITELIST_VIOLATIONS=0); 宽口径 definition 包 980 例中 4 failures+3 errors 全部落在未改动任务（10520/20520、15101、10526/20526、25512、15301/25301）的既存欠账上; 客户端验收未做; 批次 8 focused-test（2026-09-21 用户授权 Maven：ExternalRewardAdvanceReentryContractTest、Quest10522AutoStartDialogTest、ReportRowRewardProjectionContractTest 等 17 个测试类 => Tests run 84 / Failures 0 / Errors 0；PRODUCTION_COMPILE_OK=6189 / PRODUCTION_COMPILE_FAILURES=0 / PRODUCTION_INTERACTION_OBJECT_FAILURES=0 / PRODUCTION_WHITELIST_VIOLATIONS=0）；批次 8 static（xmllint + quest_definition.xsd 8 文件 validates、IDE lint 0 警告、git diff --check 干净；audit_external_reward_advance.py + verify_external_reward_reentry_contract.py 复跑 10 任务 writer step=1 / projection=1 / 入口页 OK / recovery=[0]）
boundaries: 只适用于 REWARD 由 typed 引擎外代码写入的任务；若写入方改走 SELECT_QUEST_REWARD 事务或投影本身有客户端证据支持，必须重跑审计脚本重定基线，禁止机械套用。与 QE-045 同源但触发面不同：QE-045 面向旧 handler 迁移的 packed 投影，本条面向引擎外直写 + 领奖态入口页缺失，审计口径与代表测试互不替代。enter-world 自愈边只在登录/切图触发，在线旧存档不会立即纠正；10 个任务的真实客户端领奖复测仍未完成，不得据此条宣称客户端验收；批次 8 已把这 10 个任务的写入方与投影统一到领奖行 1（10522/20522 的旧自愈边由 var0==1 改为 var0==0，15545/25545 的 setQuestVar(1) 语义不变，var0 同为 1）；仍不得据此条宣称客户端验收（PENDING_CLIENT）
superseded_by: none
see_also: [QE-045], [QE-032], [QE-040]
first_check: 玩家反馈「领奖阶段只有结束对话 / 点任务行没有完成对话」时，先看状态包里的 Status 与 Vars，再到该任务 reward 节点投影核对；然后用 audit_external_reward_advance.py 确认是否有引擎外写入方、是否缺 reward + QUEST_SELECT(31) -> DEFAULT_SUCCESS 入口页
keywords: 领奖只有结束对话、REWARD 没有完成对话、点任务行没反应、无法打开奖励窗口、Vars 0 0 0 0 0、Status REWARD、sendQuestEndDialog、HACTION_SELECT_QUEST_REWARD、10002、1009、CM_CREATIVITY_POINTS、MinionService、CoalescenceService、RiftOrbAI2
-->

- **判定规则**：`REWARD` 由引擎外代码写入的任务，`reward` 节点投影必须等于写入方留下的 packed step；进入 `REWARD` 的事务不得改写该字段；每个完成 NPC 必须注册 `reward + QUEST_SELECT(31) -> DEFAULT_SUCCESS(10002)` 的无条件入口页，`1009` 继续由 `npc-complete` 预览打开奖励窗口；定义曾经投影过别的值时补 `REWARD && 变量 == 旧值` 的无 source `enter-world` 自愈边。
- **为什么容易漏**：`matchesSourceNode` 用 packed 变量匹配 source 节点，步数错位时**整个状态没有任何可用路由**，玩家侧只表现为「只有结束对话」；入口页缺失即使步数正确也照样无法打开 `10002`。两种缺陷症状相同、修法不同，必须分别取证。
- **代表案例**：10522/20522（`CM_CREATIVITY_POINTS` 置 REWARD 不写步数，reward 投影 `1 -> 0`、补 `806075`/`806079` 的 31 入口页与 `var0=1` 自愈边）；同批 15542/25542、15545/25545、30211/30213/30311/30313 补入口页并按写入方对齐步数，由 `ExternalRewardAdvanceReentryContractTest` + 基线 TSV 守护；批次 8（2026-09-21，用户授权后执行）：与 QE-051 求交后统一到客户端领奖行——10522/20522/15542/25542/30211/30213/30311/30313 的 reward 投影 `0 -> 1`、三个写入方各补 `qs.setQuestVarById(0, 1)`（RiftOrbAI2 一处覆盖 4 个任务）、旧自愈边由 `var0==1` 改为 `var0==0`（无 actions，仅 `LEVEL_AND_VISIBILITY_REFRESH`），基线 TSV 重刷为 `writer step=1 / projection=1`。

## [QE-047] 四十五、被精确匹配消费的计数增量必须有上限守卫且不得越界 (EXACT_MATCHED_COUNTER_NEEDS_UPPER_BOUND)

<!-- pattern-metadata
status: CONFIRMED
scope: typed 任务定义的 progress 位段与 increment-variable/set-variable 事务动作；被 variable-is 精确匹配消费的自环计数（10525/20525 四位证言集合、10529/20529 s8 boss 击杀计数）
first_seen: 2026-09-21
last_verified: 2026-09-21
symptom: 玩家反复点击同一对话或重复触发同一事件后，QUEST_AUDIT 在阶段 PLAN 失败、已提交=false、根因=java.lang.IllegalArgumentException：value out of range for progress field: varN（栈 ProgressLayout.pack -> QuestMutationPlanner.build），客户端只收到通用窗口（questId=0 目标=0 下发页=0）且按钮再无反应；或任务永久卡在需要精确步数的阶段而任务说明停止跟随
root_cause: 自环计数使用无上限守卫的 increment-variable 累加，而 ProgressLayout.pack 对解包后的存档值做严格范围校验：值一旦超过声明 max（10525 var1 5-bit max31 的第 4 次 +8 得到 32、10529 var1 max7 的第 2 次 +1 得到 8）即抛 IllegalArgumentException 使整笔事务失败；即使未越界，完成路线用 variable-is 精确匹配单一取值，累加值单调增长后永远回不到阈值（10525 从 0 出发的 32 个可达值中有 17 个成为死锁），客户端可见行索引还会因紧凑位段（var1 offset=4）被低位计数改写 SECTION_0 而脱节
fix_or_guardrail: 1. 凡以 variable-is 精确匹配作为完成条件的自环计数（以及所有可能被重复触发的计数增量）必须在同一 transition 声明上限守卫（variable-below 目标阈值或 variable-is），并满足 bound + delta <= 字段 max；2. 跨步骤转移（如 s7->s8）必须显式 set-variable 清零局部计数，禁止依赖旧引擎的 6-bit 掩码回绕；3. 已饱和/越界的旧存档补无 source 的 enter-world 自愈边（status + 阶段变量 + variable-at-least 上限 -> set-variable 0）或把无法安全表达的累加自环改为“一次事件直接推进并在同一 mutation 清零”（10529/20529）；4. 客户端读取的计数必须按 QE-012 放在 SECTION_n = offset 6n，不得为紧凑而把 var1 放到 offset 4；5. 新增 QuestIncrementRangeContractTest 全服门禁（精确匹配字段的每条增量必须带上界且 bound+delta<=max）
evidence: src/main/java/com/aionemu/gameserver/questEngine/definition/ProgressLayout.java:59 (pack 越界抛 value out of range); src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestMutationPlanner.java:141 (IncrementVariable merge) 与 :196 (layout.pack); src/main/java/com/aionemu/gameserver/questEngine/model/QuestVars.java (六槽 6-bit，getQuestVarById(1)=SECTION_1); src/main/resources/aion/data/static_data/quest_definition/quests/10525.xml 与 20525.xml（var1 offset=6 width=4 max=15 + variable-below 14/13/11/7 + enter-world 自愈边）; quests/10529.xml 与 20529.xml（s7->s8 清零 var1；s8 击杀不再累加，直接 set var0=9 并 set var1=0）; origin/history 旧 handler _10525Agent_Viola_Call（getQuestVarById(1) 累加后判 ==15）与 _10529Protection_Artifact_2（var==8 时 var1!=0 只累加）; Aion 5.8 客户端解包数据 quest_script_monster 第 206-208 行（Progress(SECTION_0==6; SECTION_1<7) 等）与客户端任务说明页面 quest_q10529.html（step8 文案 /1）; src/test/java/com/aionemu/gameserver/questEngine/definition/Quest10525TestimonyCounterContractTest.java; Quest10529BossKillCounterContractTest.java; QuestIncrementRangeContractTest.java; .agents/summary/quest-10525-testimony-counter/2026-09-21-10525-testimony-counter-overflow.zh-CN.md; .agents/summary/quest-10525-testimony-counter/audit_increment_range.py
validation: static（全库 6222 定义 / 1012 条增量审计：EXACT_COUNTER_UNBOUNDED 6->0、OUT_OF_RANGE 0；4 个改动 XML xsd-valid；IDE 检查新增测试与 XML 无 error/warning；可达性模型 0..14 无越界无死锁）；focused-test（Maven -Dtest=Quest10525TestimonyCounterContractTest,Quest10529BossKillCounterContractTest,QuestIncrementRangeContractTest test：5/5 全绿）；production-gate（Maven QuestDefinitionCatalogManifestTest,QuestCollectProgressAlignmentGateTest,ClientQuestSectionAlignmentTest,QuestSection0ReportRowContractTest,QuestMonsterProgressContractAuditTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionDirectoryLoaderTest test：全绿，PRODUCTION_COMPILE_OK=6189，FAILURES=0，INTERACTION_OBJECT_FAILURES=0，WHITELIST_VIOLATIONS=0）；client-acceptance 未做
boundaries: 证言的“累加 + 精确阈值”语义沿用旧 handler 与镜像任务 20525，只有每名证人各一次的流程下 sum 才等价于位掩码；严格 |= 语义需要引擎新增 set-bit 动作（本轮未引入）。旧存档按新布局解读（packed 386 -> var0=2,var1=6）仍可完成，但 var1>=15 的饱和值需要一次重新进入世界触发自愈边。10529/20529 的 s7->s8 清零与 s8 一次击杀推进属行为变化（旧版依赖 6-bit 回绕并可能多次击杀），需客户端复测。全库另有 71 条非精确匹配的无上界增量（多为 variable-at-least 溢出臂或跨步骤写入）本轮未收紧
superseded_by: none
see_also: [QE-012], [QE-044]
first_check: 出现 QUEST_AUDIT “value out of range for progress field: varN” 时，先定位该任务的 bit-field 声明与全部 increment-variable，检查每条增量的上界守卫与 bound+delta<=max；再确认完成路线是否为 variable-is 精确匹配、跨步骤是否显式清零计数、字段是否落在 SECTION_n=6n
keywords: value out of range for progress field、ProgressLayout.pack、QuestMutationPlanner、IllegalArgumentException、越界、计数累加、证言、SELECT3_4_1、1950、806227、10525、20525、10529、20529、var1、SECTION_1、精确匹配、PLAN 阶段失败
-->

- **判定规则**：`IncrementVariable` 与解包存档合并后由 `ProgressLayout#pack` 校验范围；被 `variable-is` 精确匹配消费的字段一旦越界即整笔事务失败，即使未越界也会因单调增长而永久偏离阈值。每条此类增量都必须自带 `variable-below`/`variable-is` 上界，满足 `bound + delta <= max`；跨步骤计数必须显式清零；饱和旧存档必须有归零出口（进入世界自愈边，或把无法安全表达的累加自环改为一次事件直接推进并清零）。
- **为什么容易漏**：旧 handler 在 6-bit 槽位里靠掩码回绕，越界不会报错，迁移到 typed 位段后同一写法变成硬失败；`variable-at-least` 分支（“非 0 就累加”）看起来像守卫，实际只给下界；紧凑位段（`offset=4`）同时还会污染客户端 `SECTION_0` 行索引，症状与计数越界混在一起。
- **代表案例**：10525/20525（四位证人证言 `+1/+2/+4/+8` 无守卫，实测第 4 次点击越界；改为 `variable-below 14/13/11/7` + `var1` 移到 SECTION_1 + 饱和自愈边）；10529/20529（s7→s8 未清零 s6 击杀计数，s8 boss 击杀第二次即越界；补 s7→s8 清零，并把 s8 击杀改为一次推进且同事务清零 var1），由 `Quest10525TestimonyCounterContractTest`、`Quest10529BossKillCounterContractTest` 与全服门禁 `QuestIncrementRangeContractTest` 守护。

## [QE-048] 四十六、击杀目标必须覆盖客户端变体族且至少一个目标真实刷新 (KILL_TARGET_COVERS_CLIENT_VARIANT_FAMILY)

<!-- pattern-metadata
status: CONFIRMED
scope: typed 任务定义的击杀路线（<kill-npc npc-id|npc-ids>）、<metadata><kills> 与 QuestEngine 击杀 owner 索引；覆盖 Iluma(210100000)/Norsvold(220110000) 主区与子区任务族（15546/25546 及 43 个同族任务）
first_seen: 2026-09-21
last_verified: 2026-09-21
symptom: 玩家接取任务后击杀任务说明点名的怪，客户端计数条 [%n]/N 一动不动、任务停在原状态（报障：15546《[每日]雷欧娜的委托》四个 [%n]/4）；同族任务在别的等级或子区同样卡住，玩家只看到进度不涨、无任何报错
root_cause: QuestEngine.onKill 先用 getQuestNpc(npc.getNpcId()).getOnKillEvent() 判断 owner，而该索引只由编译后的击杀转换填充（installProductionDefinitions 的 KillNpc/KillNpcSet 分支）。任务登记的 NPC ID 与世界里真实刷新的模板不是同一批：15546 只登记 base 模板 240475/240483/240495/240497，这 4 个模板在 spawns/** 中 0 刷新；Iluma 实际刷的是同族 T_ 变体 241656/241657/241664/241665/241676/241677/241678/241679（12 个刷新点）。别的任务虽然登记了这些 T_ 变体，但击杀事实只会路由给登记了该 npcId 的任务，因此 15546 的 var1..var4 恒为 0。同型硬缺陷在 HEAD 命中 42 个任务（登记目标全无刷新、但同族兄弟模板有刷新）
fix_or_guardrail: 1. <kill-npc> 目标集合必须覆盖客户端 quest_monster/SECTION 名单解析出的全部同名变体（base + T_ 等级变体），并把 <metadata><kills> 同步到同一口径；2. 每条击杀路线至少要有 1 个目标出现在 world_maps.xml 活跃地图的 spawns/** 数据里，禁止只登记零刷新的基础模板；3. 新增 QuestIlumaNorsvoldKillTargetCoverageTest + iluma-norsvold-kill-target-contract.tsv（45 任务快照）门禁：IR 目标集合必须等于评审快照、每任务至少 1 个可刷目标、15546/25546 必须保留 4 个独立计数器与 16 杀闭环；4. 零售 data_driven_quest.xml 的 progress_info 名单可能只是变体族的子集，测试只能把它当 containsAll 下界，精确期望以客户端契约快照为准；5. 同类排查用 .agents/summary/quest-15546-kill-progress/audit_hard_broken_kill_routes.py 扫全库
evidence: src/main/java/com/aionemu/gameserver/questEngine/QuestEngine.java:425 (onKill 用 getQuestNpc(npc.getNpcId()).getOnKillEvent() 判 owner) 与 src/main/java/com/aionemu/gameserver/questEngine/QuestEngine.java:2128 (installProductionDefinitions 用 KillNpc/KillNpcSet 填索引); retail-xml-retention.tsv 的 quest 15546 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）（四族 × base + T_ 变体集合与 metadata kills 同口径）与 retail-xml-retention.tsv 的 quest 25546 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）、retail-xml-retention.tsv 的 quest 25533 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）、retail-xml-retention.tsv 的 quest 25640 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/main/resources/aion/data/static_data/spawns/Npcs/210100000_Iluma.xml（只刷 T_ 变体，base 模板 0 刷新）; Aion 5.8 客户端解包 quest_q15546.html（点名 T_ElementalLightF_A_66_n / T_Daru_A_66_n / T_Popoku_As_A2_67_n / T_WoodTesinon_A2_67_n，计数 [%5]/4 [%8]/4 [%11]/4 [%14]/4）与客户端 quest_monster 名单（每个 SECTION 四个变体名）及 docs/quest/client-dialog-mapping/client-monster-progress-contracts.csv; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestIlumaNorsvoldKillTargetCoverageTest.java; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestA03ShardRetailAlignmentTest.java（25533/25640 期望改为 零售 ⊆ 客户端契约快照）; src/test/resources/quest/iluma-norsvold-kill-target-contract.tsv; .agents/summary/quest-15546-kill-progress/2026-09-21-iluma-norsvold-kill-target-variants.zh-CN.md、同目录 audit_hard_broken_kill_routes.py 与 generate_kill_target_contract_tsv.py
validation: static（45 个改动 XML 全部 xsd-valid；审计脚本 HEAD 42 命中 -> 当前工作区 0 命中；契约快照生成自检 45/45 无缺口，25533=175/25640=117 与客户端名单逐条一致；git diff --check 通过；IDE 检查新增与改动测试 0 error/warning）；focused-test（Maven -Dtest=QuestA03ShardRetailAlignmentTest,QuestIlumaNorsvoldKillTargetCoverageTest,QuestMonsterProgressContractAuditTest,ClientQuestSectionAlignmentTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest test：40/40 全绿，PRODUCTION_COMPILE_OK=6189、FAILURES=0、INTERACTION_OBJECT_FAILURES=0、WHITELIST_VIOLATIONS=0）；full-suite（Maven -Dtest=com.aionemu.gameserver.questEngine.**.*Test test：1555 run / 5 failures / 6 errors / 1 skipped，11 个未通过项逐条归因且均非本改动引入；其中 25512 与 LegacyMonsterHunt 的 5 个 sourceNode() 空判错误已用 HEAD 版本 XML 直接复现，属既有测试缺陷）；client-acceptance 未做（待用户实机确认 15546 四个 [%n]/4 推进并可报告）
boundaries: 只放宽哪些怪算数，未改动任何数量门控、打包或投影；名称解析是 name_desc 大小写不敏感精确匹配（客户端怪物名 -> 服务端 npc_template），客户端存在别名或重名时需要人工确认；600200000_Lakrum 在 world_maps.xml 中被注释，其刷怪文件不参与可达性判定；已持久化但停在 0 计数的存档无需迁移，下一次真实击杀即可推进；快照只守 45 个 Iluma/Norsvold 任务，其他区域同型缺陷需另跑审计脚本确认
superseded_by: none
see_also: [QE-040], [QE-043], [QE-050]
first_check: 击杀任务进度不动时，先取该任务 XML 的全部击杀目标 ID 与 spawns/** 的 npc_id 取交集；交集为空（或只剩客户端没声明的模板）即命中本模式，再按客户端 quest_monster/SECTION 名单补齐同族变体、同步 metadata kills，并用新增门禁锁死集合
keywords: 击杀不计数、进度不更新、计数条不涨、onKill、getOnKillEvent、owner 索引、kill-npc、npc-ids、T_ 变体、base 模板、零刷新、15546、25546、25533、25640、quest_monster、SECTION_1、Iluma、Norsvold
-->

- **判定规则**：击杀路线的目标集合是「世界可交互事实」与「任务 owner 索引」的唯一桥梁——登记了不刷新的模板等于该任务没有击杀入口。集合必须覆盖客户端 quest_monster/SECTION 声明的全部变体名（base + `T_` 等级变体），且至少有一个目标能被活跃地图的刷怪数据刷出；`<metadata><kills>` 必须与计数器同口径。
- **为什么容易漏**：任务 XML 看起来完全正确（有击杀路线、有计数器、有 REWARD 收口），症状只是「进度不涨」，既不报错也不进 QUEST_AUDIT；而 retail `data_driven_quest.xml` 的 progress_info 名单本身只是变体子集（25533 登记 23 个、客户端声明 175 个；25640 登记 12 个、客户端声明 117 个），照抄零售名单就会漏掉 `T_` 层级，只有把「客户端合同」与「生产刷怪可达性」交叉验证才能发现。
- **代表案例**：15546/25546（四族 × SECTION_1..4 四计数器，目标由单 base 模板扩为 base + `T_` 变体集合，Iluma 只刷 `T_` 变体）；同批 43 个 Iluma/Norsvold 任务（25500/25501/25503/25504/25506..25534/25640/42001..42106/51077/51078/80891..80929）按同一契约扩集合，由 `QuestIlumaNorsvoldKillTargetCoverageTest` + `iluma-norsvold-kill-target-contract.tsv` 守护；`QuestA03ShardRetailAlignmentTest` 相应改为「零售名单 ⊆ 目标 = 客户端契约快照」。

## [QE-050] 四十八、状态未变化的执行不得下发任务状态更新 (STATE_IDENTICAL_EXECUTION_MUST_NOT_SYNC_QUEST_STATE)

<!-- pattern-metadata
status: CONFIRMED
scope: typed 引擎的事务提交后协议动作（AfterCommitAction.SyncQuestState）与"命中但状态不变"的计划；击杀计数器饱和后的超额击杀是最常见触发面，全库同形自环 26 条（12 个任务）
first_seen: 2026-09-21
last_verified: 2026-09-21
symptom: 任务进度已经打满（例如 15546 某族 4/4、其余族未满）后继续击杀同一批怪，进度不再变化，但客户端仍闪一次"任务更新"；玩家侧描述为"没涨却提示更新"
root_cause: 自环"收口"路线的守卫只有下界（variable-at-least varN 3 -> set varN 4），计数器到达上限后仍然命中，QuestMutationPlanner 因此生成一份与当前 packed 状态完全相同的计划；QuestExecutionCoordinator.requiresStatePersistence 判定无需写库，但 after-commit 里的 sync-quest-state PACKET_ONLY 仍会执行，PlayerQuestStateSyncPort.sync() 于是下发 SM_QUEST_ACTION.updateQuest(...)，客户端把它渲染成一次任务更新通知。全库审计（audit_saturated_kill_selfloops.py）确认 26 条同形自环、分布在 12 个任务
fix_or_guardrail: 1. 引擎护栏：QuestExecutionCoordinator 在"状态未变化且没有任何必需动作"时丢弃多余的 SyncQuestState（withoutRedundantStateSync），状态或持久副作用真正变化时同步照常下发；2. 计数收口路线必须给精确边界：累加路线 variable-below ceiling（第 N 次击杀自己收口为 ceiling），越界自愈路线 variable-at-least ceiling+1 -> set ceiling，禁止只带下界的收口自环；3. 门禁三件套：Quest15546KillCounterSaturationFlowTest（生产 IR 流程：1..4 逐次同步、第 5 次零动作）、QuestExecutionCoordinatorTest#stateIdenticalExecutionDropsTheRedundantStateSync（引擎本体）、QuestIlumaNorsvoldKillTargetCoverageTest#saturatedCountersRejectExtraKillsWithoutMatchingAnyRoute（XML 契约）；4. 全库排查用 .agents/summary/quest-15546-kill-progress/audit_saturated_kill_selfloops.py
evidence: src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestExecutionCoordinator.java（withoutRedundantStateSync + requiresStatePersistence）; src/main/java/com/aionemu/gameserver/questEngine/runtime/PlayerQuestStateSyncPort.java（sync 无条件下发 SM_QUEST_ACTION）; retail-xml-retention.tsv 的 quest 15546 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单） 与 retail-xml-retention.tsv 的 quest 25546 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）（variable-below 4 累加 + variable-at-least 5 越界自愈）; src/test/java/com/aionemu/gameserver/questEngine/runtime/Quest15546KillCounterSaturationFlowTest.java; src/test/java/com/aionemu/gameserver/questEngine/runtime/QuestExecutionCoordinatorTest.java; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestIlumaNorsvoldKillTargetCoverageTest.java; .agents/summary/quest-15546-kill-progress/2026-09-21-iluma-norsvold-kill-target-variants.zh-CN.md 与同目录 audit_saturated_kill_selfloops.py
validation: static（xmllint --schema 15546/25546 通过；饱和自环审计 26 -> 18；IDE inspections 0 error）；focused-test（Maven -Dtest=QuestIlumaNorsvoldKillTargetCoverageTest,QuestA03ShardRetailAlignmentTest,QuestExecutionCoordinatorTest,Quest15546KillCounterSaturationFlowTest,QuestMonsterProgressContractAuditTest,ClientQuestSectionAlignmentTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,QuestMutationPlannerTest,QuestDataDrivenHuntProductionFlowTest test：88/88 全绿，PRODUCTION_COMPILE_OK=6189、FAILURES=0、INTERACTION_OBJECT_FAILURES=0、WHITELIST_VIOLATIONS=0）；隔离证据（临时禁用护栏后 QuestExecutionCoordinatorTest 报 expected <[]> but was <[SyncQuestState[mode=PACKET_ONLY]]>）；full-suite（Maven -Dtest=com.aionemu.gameserver.questEngine.**.*Test test：1561 run / 5 failures / 6 errors / 1 skipped，与修复前同一批既有失败，无新增）；client-acceptance 未做（待用户实机确认超额击杀不再出现任务更新）
boundaries: 护栏只在"状态未变化且无必需动作"时生效：奖励、物品、货币等持久副作用照常下发同步；余下 18 条只带下界的收口自环在饱和后仍会匹配（提交一笔空事务，但不再下发任务更新），需要逐任务客户端上限证据后再收紧；本次未改变任何计数语义（1..4 仍逐次推进，只是第 4 次由累加路线收口）
superseded_by: none
see_also: [QE-047], [QE-048]
first_check: 玩家报告"进度没变却提示任务更新/任务闪一下"时，先看该击杀路线的条件是否只有下界（variable-at-least）而动作是 set 到上限；确认 planner 产出的 packed 与当前完全相同，再查 after-commit 是否带 sync-quest-state，最后按 audit_saturated_kill_selfloops.py 全库定位同形路线
keywords: 任务更新、进度不涨、超额击杀、多杀、饱和、收口路线、variable-at-least、variable-below、sync-quest-state、PACKET_ONLY、SM_QUEST_ACTION、updateQuest、15546、25546、25406、26802、QuestExecutionCoordinator
-->

- **判定规则**：任务状态包只有在"状态或持久副作用真正变化"时才允许下发。命中转换但产出的 packed 状态与当前快照完全相同时，执行路径必须丢弃 `sync-quest-state`（引擎护栏），并且这类路线本身应当收紧守卫而不是依赖护栏。
- **为什么容易漏**：客户端只在收到状态包时刷新任务书，重复包与真实进度包在下行链路上没有区别；服务端侧"命中但状态不变"既不报错也不写库，只在 `QuestExecutionCoordinator` 的 `requiresStatePersistence` 与 after-commit 列表之间留下一条静默通道；而收口自环的 `variable-at-least` 守卫在饱和后仍成立，正是它的主要来源。
- **代表案例**：15546/25546 四计数器任务（第 4 次击杀由 `variable-below 4` 累加路线收口，越界值由 `variable-at-least 5 -> set 4` 自愈；超额击杀不再命中任何路线）；引擎护栏同时覆盖 25406/25407/25408、25580、26802、30600/30610、10112/20112、13705、17510/27510 等 18 条同形自环，由 `Quest15546KillCounterSaturationFlowTest`、`QuestExecutionCoordinatorTest#stateIdenticalExecutionDropsTheRedundantStateSync` 与 `QuestIlumaNorsvoldKillTargetCoverageTest` 守护。；批次 19（2026-09-22）：单步塌陷对 15000/15670——镜像 25000/25670 已是完整 4 行阶梯（reward=3），本侧被迁移塌陷成“单步交接直接置 REWARD”；按客户端页按钮链补回行 1/行 2（15000：交背囊 182215661 -> SELECT2(1352) -> SELECT2_1(1353) -> SETPRO2 接过工作物品 182215662 -> use-item -> 领奖行 3；15670：赫梅洛斯 806093 行 0/行 1 -> 第五处痕迹 731793 行 2 -> 伊利西亚 806114 行 3），4 处痕迹 703434-703437 的 can-act 自环与 drops collecting-step 随行号 0 -> 1 移动，由 CollapsedSingleStepLadderContractTest（7 例）锁定
- **编号说明**：`QE-049` 已被并行会话在提交 `1d8777c53` 中预留给「接取发放丢失」（`ACCEPT_TIME_WORK_ITEM_GRANT_LOST_ON_MIGRATION`），因此本卡片顺延为 `QE-050`。

---

## [QE-051] 四十九、领奖投影必须等于客户端任务书领奖行 (REWARD_ROW_MUST_MATCH_CLIENT_JOURNAL_ROW)

<!-- pattern-metadata
status: CONFIRMED
scope: typed 任务定义的 `reward` 节点 var0 投影 vs Aion 5.8 客户端 `Dialogs/*/quest_q<id>.html` 的 quest_summary 行索引；重点是 `use-item`/对话交接进入 REWARD 且客户端有独立“领奖行”的任务族（10520-10530/20520-20530、15001 家族）
first_seen: 2026-09-21
last_verified: 2026-09-22
symptom: 使用任务道具或完成最后一步交接后任务停在原步骤，任务书仍显示“调查/使用道具”那一行，不切到“和代理人 X 对话”；用户报“应该是领奖（REWARD），var0 应该是 15，现在是 14”
root_cause: 迁移把 legacy `useQuestItem(env, item, from, to, true)` / `changeQuestStep(env, from, to, true)` 里被旧 helper 丢掉的 `to` 一并丢了（旧 changeQuestStep 在 reward=true 时只置 REWARD、不写 nextStep），reward 节点投影就地抄成 `from`；QuestMutationPlanner 对动作未触及的字段采用目标投影，REWARD 存档于是落到 `var0=from`，客户端按 SECTION_0=from 渲染上一行
fix_or_guardrail: 1. reward 节点 var0 = 客户端 quest_summary 的领奖行索引（默认最后一行“和 X 对话/报告”；最后一行是第 0 行重复行或步骤链折叠时按客户端验收值记例外）；2. 交接 transition 不必写该字段（目标投影权威），但值必须等于客户端领奖行；3. 已落盘的错位存档必须补无 source 的 enter-world 恢复边（status=REWARD && var0==from -> set var0=to），否则 QuestMutationPlanner#matchesSourceNode 会把旧存档挡在 reward 节点所有领奖路由之外；4. 客户端 quest_summary 的每一行都必须能在服务端找到 var0==行号的 START/REWARD 状态，缺口行在游戏里永远不会高亮；5. 迁移或修复前先跑 .agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py（判定行/状态错位）与 .agents/summary/quest-10527-reward-row/audit_legacy_reward_entry_steps.py（取 legacy packed step，区分“迁移丢 nextStep”与 legacy 本身保持 step） 并用镜像任务（q 与 q+10000）交叉验证；7. 末行是否真是领奖行用客户端 NPC 名独立复核：quest_summary 末行的 STR_DIC_N_XXX 必须在 npcs_unpacked/client_npcs_npc.xml 的 <id>/<name> 里对上**reward 路线（target=reward/source=reward）的 NPC**（批次 3 的 202 个任务即以此为收口证据；批次 2 用的是任务内 NPC 命中 + 镜像同形）；批次 2 的 120 个任务就是用这条证据收口的（双侧都缺末行、末行 NPC 命中、镜像同行数同形状）；6. progress 里 var0 的 max 必须 >= 领奖行且 max 不得超出 width（否则 ProgressLayout.pack 抛 value out of range for progress field: var0，或编译期 bit-field value range exceeds its declared width；10525/20525、26820、1921、2634/2669/24201/24202、2946/11076/14051 都踩过）；批次 3 有个别任务原投影是 0（迁移从未设置），同样按“推到客户端末行 + 自愈边”处理；8. 折叠步骤链（10529/20529 的第 10 行“带上陷入沉睡的德扎波波向代理人报告”原本直接压在 REWARD 上）必须把报告行拆成独立 START 节点（s10, var0=10），reward 投影推到真正的领奖行（11），并为旧存档补 REWARD/var0=10 -> 11 自愈边；拆出的节点要接回客户端对话链（select11 -> select11_1 -> select11_1_1 -> SET_SUCCEED），页面枚举缺项（6502 SELECT11_1_1）先补 QuestDialogPage/QuestDialogAction；9. 客户端任务书行索引读 questVars 的低 6 位（SECTION_0），因此任何 varN（N>=1）都必须落在 offset 6N：3090 的 var1@2/var2@3 让 s1-zone/s1-object 下发 SECTION_0=5/9，行号被计数位顶走；全库门禁 ClientQuestSectionAlignmentTest#sectionNamedCountersStayAtTheirFixedSixBitOffset 只放行 14 个显式挂账的历史任务；10. 行推进本身也可能是迁移丢失面：15551-15554/25551-25554 这类“进入感应区/坐骑”任务的 legacy `onEnterZoneEvent` 用两条感应区路线推进行号（A_TO_X 上 var0 0 -> 1、X_TO_A 上 var0 1 -> 2 并置 REWARD），迁移提交 79bc5d3a6 只保留了 `unaccepted -> started` 的自动接取，把推进整条丢掉，于是中间行没有状态、reward 停在倒数第二行；修法是把自动接取原样保留、按旧 handler 恢复推进路线（中间行用 `PACKET_ONLY`、走进 REWARD 用 `LEVEL_AND_VISIBILITY_REFRESH`，与 26800 的 DF_TOWER_SENSORY_AREA 同型）并同步抬高 `var0` 的 width/max；已知粒度：未接任务时进入 A_TO_X 会以 var0=0 接取，需要再次经过该感应区才推进（迁移“进入即接取”模型的固有取舍，不属本轮缺陷）；11. 多计数器任务的行推进必须按客户端 quest_script 的 SECTION 声明建模：18208/18209/28208/28209 的 `SECTION_0==0; SECTION_1<5` 与 `SECTION_0==1; SECTION_2<1` 对应“var0=行号 + var1=5 次击杀计数 + var2=精英标志”，迁移把每个击杀写成 var0 阶段（k1..k7）会把第 1 行的计数位顶走、领奖行（行 3）也永远不亮；重定阶段时 reward 仍按第 1 条投影到领奖行（本任务=2，不因旧 handler 停在 1 而保留 pre-REWARD step），并为旧值域补无 source 的 enter-world 收敛边：START 侧 `var0>=1 && var0<5 && var1<4 -> 0`（回第 1 行）与 `var0>=5 -> 1 且 var1=4`（进第 2 行），REWARD 侧 `var0<2`、`var0>=3` 统一收敛到 2；8. 报告/交付型 2 行任务（成长任务族）reward 投影必须=1，其余行仍由 START 状态承载；9. 若某个 START 事务把 var0 写成 k（行推进），必须同时存在投影 var0=k 的 START 节点，否则 QuestMutationPlanner#matchesSourceNode 会让所有以旧投影节点为 source 的 `var0>=k` 事务永远匹配不到——18036/28036 的 `started(SETPRO1)` 写 var0=1 却仍以投影 var0=0 的 started 为 source，交出物品后任务卡死，批次 7 新增 s1(START,var0=1) 并把三条事务改指 s1 才修好；12. 与 QE-046 求交（引擎外写入方直写 REWARD）时，领奖行不能只改 XML：必须同时把写入方改成在 setStatus(REWARD) 前写领奖行 var0，并把自愈边条件改成旧写入方留下的旧值，再重刷 external-reward-advance-baseline.tsv （批次 8：10522/20522/15542/25542/30211/30213/30311/30313，reward `0 -> 1`、自愈边改 `var0==0`、CM_CREATIVITY_POINTS / CoalescenceService / RiftOrbAI2 各补 qs.setQuestVarById(0, 1)）；13. 补了无 source 的 `enter-world` 自愈边之后，任何按 `sourceNode()` 过滤的测试/审计都必须用 null-safe 比较（`Objects.equals(x.sourceNode(), source)`）：批次 8 用「引用了提交里改过的任务 id + 按 sourceNode 过滤」筛出 43 个定向测试类，其中 21 个类的 helper 因 `sourceNode().equals(...)` 直接 NPE；15. 自闭合的 `started`（`<node label="started" status="START"/>`）在审计里表现为“行 0 无状态”（INTERIOR_GAP/ROW_WITHOUT_STATE）；修法是按客户端 quest_summary 行数与镜像任务补**只含行号**的投影（28932 补 var0=0，与 18932 同形），绝不能把计数器（var1/var2）一起钉进投影——否则 `started` + `var1>=1` 的“满计数恢复路线”会因 `matchesSourceNode` 要求声明变量全等而整条失配；16. `var0` 是标志位/计数槽而非任务书行号的任务不得按行号改投影：30203/30303 的四只守护者击杀标志位（legacy `_30203GroupHalttheCeremony` 逐个 setQuestVarById(n,1)、集齐后 setStatus(REWARD)，领奖态就是 var0..3=1）与客户端 `Progress(SECTION_0<1)…Progress(SECTION_3<1)` 声明一致，审计判 ROW_WITHOUT_STATE 属误报，已在审计脚本用 `VAR0_FLAG_EXCEPTIONS = {30203, 30303}` 记录；9. retail `data_driven_quest` 单步族（start + 1 step，`origin/history:src/main/resources/aion/definitions/compact/quests/scripts/zz_retail_simple_quests.xml` 共 249 个）里 184 个早已是 `reward var0=1`，落后的 18 个（1527/1528/1725/1963/1964/2135/2247/2266/3087/4020/16838/16977/18035/21455/26838/29002/80735/80736）按族内模板 reward 投影 0->1 + 无 source enter-world 自愈边收口；10. 客户端 HTML 里与相邻行共用 visible 槽位的空 `<p>`（10530 第 8/9 行同为 `[%24]`，镜像 20530 没有这一行）会让 client_rows 比真实状态多 1，禁止按“末行行号”加一（审计脚本已登记 `DUPLICATE_VISIBLE_SLOT_BLANK_ROWS`）；11. 领奖 NPC 归属的验收口径是客户端 quest_summary 行内命名 NPC（末行或该行对话对象写明的 NPC = 实现侧领奖/completion NPC，起始 NPC 不得兼任领奖；批次 12：19064/29064 的 Jucleas/Balder 203752/204075、21455 的 Unset 799244）；当 legacy/retail 单文件（如 `terath_dredgion.xml` 的 `start_npc_ids`）与客户端命名冲突时以客户端命名 + 族内一致性为准（同族 30610/30611/30612/30613 的「行内 NPC ↔ 定义 NPC」4/4），禁止把同一派生物的两个副本当双重证据（30614 教训：retail contract 与 legacy template 同源于 `terath_dredgion.xml`）；领奖态入口页必须位于 `npc-complete` 之后且唯一，插进 completion 块内部会直接 XSD 报错；批次 13（两行末行对话族）：客户端恰好 2 行且行 1 点名领奖/完成 NPC 的任务（1926/2938/39003/49003/80989/80990）用 reward 投影 0->1 + 无 source 的 `REWARD && var0==0 -> set var0=1` 自愈边收口，迁移期 `REWARD && var0==1` 入口边按任务保留或不新增；批次 14（事件族两行）：80255/80256（活动烟花族漏网项，行 1 是字面中文名所以批次 1-7 的“末行 NPC 键”筛选漏掉）与 80601/80606（legacy `_80601Fight_Of_The_Navigators` / `_80606The_Good_News_And_Bad` 击杀分支 `setQuestVarById(0, 1)` 后才置 REWARD，typed 击杀事务也保留 `set-variable var0=1`，但 reward 投影仍是 0，经无 actions 的 `NPC_REPORT` 进入领奖态的存档匹配不到 reward 路由）同用 reward 投影 0->1 + 无 source `REWARD/var0=0 -> set var0=1` 自愈边收口；批次 15（残余两行）：1123（行 1 用客户端 actor 名键 `STR_DIC_LA12`，靠同族 1006/1122/1124/30507 的 completion owner = 790001 = Pernos 解键）与 2484（legacy 在烽火对象 700267 处 `setQuestVarById(0, 1)` 后在 Hippolyta 203331 处置 REWARD）同用投影 0->1 + 无 source `REWARD/var0=0 -> set 1` 自愈边；由此“两行 + 末行对话”族（批次 13/14/15 共 12 个）收口完毕；副本/传送段的行（批次 22，24046）：被 legacy `changeQuestStep(from, from+2)` 跳过的行必须由真实事件补源——进入副本世界用 `enter-world` + `world-is <副本世界>`（副本内条件），并配 `world-is ... expected="false"` 的副本外回退，不得照抄 legacy 的 var 值，也不要把任务改挂到与任务无关的 portal 物件上
evidence: src/test/java/com/aionemu/gameserver/questEngine/definition/AlignedMirrorRewardRowContractTest.java（批次 1：9 组镜像领奖行 + 恢复边 + planner 计划）; src/test/java/com/aionemu/gameserver/questEngine/definition/MirrorPairRewardRowContractTest.java（批次 2：120 条合同）; src/test/java/com/aionemu/gameserver/questEngine/definition/JournalRewardRowRepairContractTest.java（批次 3：202 条合同）; retail-xml-retention.tsv 的 quest 11294 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/main/resources/aion/data/static_data/quest_definition/quests/15002.xml; src/main/resources/aion/data/static_data/quest_definition/quests/15010.xml; src/main/resources/aion/data/static_data/quest_definition/quests/15070.xml; src/main/resources/aion/data/static_data/quest_definition/quests/15514.xml; retail-xml-retention.tsv 的 quest 19004 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）; src/main/resources/aion/data/static_data/quest_definition/quests/25062.xml; src/main/resources/aion/data/static_data/quest_definition/quests/25073.xml; retail-xml-retention.tsv 的 quest 26820 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）（reward var0 改为客户端领奖行、max 同步抬高、补 enter-world 自愈边）; .agents/summary/quest-10527-reward-row/audit-missing-last-row.tsv（492 行 “只缺最后一行”清单）; .agents/summary/quest-10527-reward-row/audit-qe051-candidates.tsv（404 个末行是领奖行的候选，其中 9 组镜像一侧完全对齐：11294/21294、15002/25002、15010/25010、15070/25070、15514/25514、19004/29004、25062/15062、25073/15073、26820/16820）; src/main/resources/aion/data/static_data/quest_definition/quests/10527.xml; src/main/resources/aion/data/static_data/quest_definition/quests/20527.xml; src/main/resources/aion/data/static_data/quest_definition/quests/10528.xml; src/main/resources/aion/data/static_data/quest_definition/quests/20528.xml; src/main/resources/aion/data/static_data/quest_definition/quests/10525.xml; src/main/resources/aion/data/static_data/quest_definition/quests/20525.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/ArchdaevaRewardRowContractTest.java; src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestMutationPlanner.java; origin/history 分支旧 handler _10527Finding_The_Traces_Of_The_Sage（useQuestItem(env, item, 14, 15, true)，仓库内已删除）; Aion 5.8 客户端解包数据 quest_q10527/quest_q10528 的 quest_summary（16 行与 13 行，末行均为“和代理人维达对话”）; .agents/summary/quest-10527-reward-row/2026-09-21-10527-reward-row-and-family-audit.zh-CN.md; .agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py; src/test/java/com/aionemu/gameserver/questEngine/definition/JournalReportRowSplitContractTest.java（批次 4：10529/20529 报告行拆分 + 3090 位段/领奖行合同）; src/test/java/com/aionemu/gameserver/questEngine/definition/ClientQuestSectionAlignmentTest.java#sectionNamedCountersStayAtTheirFixedSixBitOffset（全库 varN 必须落在 6N 的门禁 + 14 个挂账任务清单）; src/main/resources/aion/data/static_data/quest_definition/quests/3090.xml; src/main/resources/aion/data/static_data/quest_definition/quests/10529.xml; src/main/resources/aion/data/static_data/quest_definition/quests/20529.xml; src/main/resources/aion/data/static_data/quest_definition/quests/10530.xml; src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDialogPage.java（SELECT11_1_1=6502）; origin/history 旧 handler _10529Protection_Artifact_2 / _10530Trouble_Back_Home / _3090In_Search_Of_Pippi_The_Porgus; Aion 5.8 客户端 quest_q10529/quest_q20529（12 行）与 QUEST_Q3090（5 行）+ 客户端 quest_script 的 Progress(SECTION_1==0; SECTION_0==1); src/test/java/com/aionemu/gameserver/questEngine/definition/SensoryAreaRideRowContractTest.java（批次 5：8 条感应区坐骑行推进合同，含 planner 两段推进、自愈边、镜像同形）; src/main/resources/aion/data/static_data/quest_definition/quests/{15550,25550,15551,15552,15553,15554,25551,25552,25553,25554}.xml（批次 5：领奖行 + 感应区行推进）; origin/history 旧 handler _15550Iluma_Field_Guide/_25550A_Norsvold_Story（STEP_TO_1/STEP_TO_2/SELECT_REWARD）与 _15551Giddyup_Starturtle/_15552Surfing_The_Ancient_Well/_15553Ride_An_Iluman_Butterfly/_15554A_Rickety_Ride/_25551Springleaf_Shortcut/_25552Pull_The_Lever/_25553Butterfly_March/_25554Reed_Patch_Rush 的 onEnterZoneEvent 行推进（仓库内已随 79bc5d3a6 删除）; Aion 5.8 客户端 quest_q15550-15554 与 quest_q25550-25554 的 quest_summary（各 3 行）; src/main/resources/aion/data/static_data/quest_definition/quests/18208.xml、18209.xml、28208.xml、28209.xml（批次 6：2 阶段 + 2 计数器、reward=2、4 条旧存档收敛边）; origin/history 旧 handler _18208IllusionOrInfiltration/_18209ARiftInTheSpaceTwineContinuum/_28208ARiftAdrift/_28209CatchingTheRift（var0 0->1、var1 0..4、var2 0->1、setStatus(REWARD) 且不改写 var0，仓库内已随 7e9f0316c 删除）; Aion 5.8 客户端 quest_q18208/18209/28208/28209 的 quest_summary（各 3 行）与 客户端 quest_script_monster 的 SECTION_0==0; SECTION_1<5 / SECTION_0==1; SECTION_2<1; .agents/summary/quest-10527-reward-row/check_section0_requirements.py 与 dump_section0_evidence.py（SECTION_0 约束审计：UNREACHABLE 5 -> 3）; src/test/java/com/aionemu/gameserver/questEngine/definition/ReportRowRewardProjectionContractTest.java（批次 7：21 条合同）; .agents/summary/quest-10527-reward-row/apply_batch7_report_row_contract.py 与 batch7-evidence.tsv; retail-xml-retention.tsv 的 quest 19672 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）（成长任务族代表）; src/main/resources/aion/data/static_data/quest_definition/quests/18036.xml（s1 节点修复代表）; src/main/resources/aion/data/static_data/quest_definition/quests/3210.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/ExternalRewardAdvanceReentryContractTest.java（批次 8 基线：10 任务 writer step=1 / projection=1 / 入口页 / recovery=[0]）; .agents/summary/quest-10522-reward-reentry/apply_batch8_external_writer_reward_row.py（批次 8 重放脚本，--check 幂等报 BATCH8_VERIFY_OK quests=8）; src/test/java/com/aionemu/gameserver/questEngine/definition/Quest28932RewardRowContractTest.java（批次 9：started 只声明 var0=0 且与镜像 18932 同形、单次击杀计划到领奖行、var0=0/var1=1 满计数存档的四条恢复对话仍可规划）；.agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py（`VAR0_FLAG_EXCEPTIONS` 记录 30203/30303 的 var0 标志位例外）; src/test/java/com/aionemu/gameserver/questEngine/definition/RetailSingleStepRewardRowContractTest.java（批次 10：18 条单步/单侧合同的 reward 行、镜像同值、自愈边条件/动作/after-commit 与 planner 计划）; src/test/java/com/aionemu/gameserver/questEngine/definition/Quest25608RetailSevenStepAlignmentTest.java（批次 11：25608 七步行、两个 ENTER_AREA zone、HUNT 自环、reward 交付行、旧存档自愈与 drop collecting-step 合同）; .agents/summary/quest-10527-reward-row/batch11-quest25608-evidence.tsv; .agents/summary/quest-10527-reward-row/apply_batch10_retail_single_step_rows.py 与 batch10-evidence.tsv（批次 10 可重放脚本与逐任务证据：client_rows=2、领奖行 1、旧投影 0、领奖/交付 NPC）; src/test/java/com/aionemu/gameserver/questEngine/definition/MirrorRewardProjectionLagContractTest.java（批次 18：6 条镜像单侧投影落后合同 + 自愈边 + 无事务写非领奖行 var0 + 16837/16986/16988 锁定护栏 + 两侧 var0 一致）; .agents/summary/quest-10527-reward-row/apply_batch18_mirror_projection_lag.py 与 batch18-evidence.tsv（批次 18 重放脚本 + 逐任务证据：镜像对照、client_rows、领奖行、completion owner、audit 前后状态）; src/test/java/com/aionemu/gameserver/questEngine/definition/CollapsedSingleStepLadderContractTest.java（批次 19：单步塌陷对 15000/15670 的行阶梯、客户端页按钮链、证据消耗、collecting-step 行号、旧存档自愈与无塌陷跳转 7 例）; .agents/summary/quest-10527-reward-row/apply_batch19_collapsed_single_step_ladder.py 与 batch19-evidence.tsv（批次 19 重放脚本 + 逐任务证据）；批次 22：src/main/resources/aion/data/static_data/quest_definition/quests/24046.xml（副本段 3->4->5->6->REWARD(7) 串行阶梯 + `REWARD/var0=6 -> 7` 自愈边）与 quests/14046.xml（对照基线）；src/test/java/com/aionemu/gameserver/questEngine/definition/ShadowCourtRowLadderContractTest.java（6 例：行状态齐备 / 副本段逐格推进 / 乱序无计划 / 旧存档自愈 / 出口物件只在副本内 / 两侧变体不互污）；.agents/summary/quest-10527-reward-row/apply_batch22_shadow_court_row_ladder.py、batch22-evidence.tsv、2026-09-21-10527-reward-row-and-family-audit.zh-CN.md（§二十六）；legacy 证据：迁移前 handler `_24046The_Shadow_Calls`（已删除，仅存在于 git 历史，用 `git show '7e9f0316c^'` 取回：`changeQuestStep(env, 3, 5, false)` 跳格 + 700369 出口 + die/enter-world 回退）
validation: static（xmllint 语法 + quest_definition.xsd：10525/20525/10527/20527/10528/20528 + 批次 1 的 11294/15002/15010/15070/15514/19004/25062/25073/26820 validates；审计脚本 6222 定义全库运行成功并产出 audit-output.tsv（2026-09-21 全库复核，客户端 quest_summary 覆盖 5572：形状 ALIGNED 2348 / STATES_BEYOND_ROWS 2627 / MISSING_LAST_ROW 149 / INTERIOR_GAP 267 / MISSING_TAIL_ROWS 92 / NO_STATE 89 / NO_CLIENT_HTML 650（批次 5 后）；“每行都要有 START/REWARD 状态”判定 ROW_STATE_ALIGNED 2348 / ROW_WITHOUT_STATE 597 / STATE_OUT_OF_RANGE 2450 / BOTH_MISALIGNED 177；领奖行指标 ROW_ALIGNED 2566 / ROW_BEHIND 277 / ROW_AHEAD 2593 / NO_REWARD_ROW 178；“reward 投影 > progress var0 max” 0 个）；focused-test 已通过（用户授权后 mvn -B test -Dtest=JournalRewardRowRepairContractTest,MirrorPairRewardRowContractTest,AlignedMirrorRewardRowContractTest,ArchdaevaRewardRowContractTest,QuestCollectProgressAlignmentGateTest,Quest10520ClientDialogAlignmentTest,QuestDialog31RegressionTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest => Tests run 45 / Failures 0 / Errors 0，含批次 2/3 共 322 条合同 + ExternalRewardAdvanceReentryContractTest）；production-gate 通过（PRODUCTION_COMPILE_OK=6189、PRODUCTION_COMPILE_FAILURES=0、PRODUCTION_WHITELIST_VIOLATIONS=0；首轮暴露 10525/20525 var0 max=6、次轮暴露 26820 var0 max=1 并已修）；批次 5 追加验证（用户授权后）：mvn -B test -Dtest=SensoryAreaRideRowContractTest,JournalReportRowSplitContractTest,JournalRewardRowRepairContractTest,ClientQuestSectionAlignmentTest,ArchdaevaRewardRowContractTest,AlignedMirrorRewardRowContractTest,MirrorPairRewardRowContractTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest,ExternalRewardAdvanceReentryContractTest,QuestCollectProgressAlignmentGateTest => Tests run 57 / Failures 0 / Errors 0，PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0，xmllint 10 个文件全部 validates、IDE lint 0 警告；同工作树前后对照审计（先 git checkout HEAD 这 10 个文件取基线再回放）ROW_ALIGNED 2556 -> 2566、ROW_BEHIND 287 -> 277、ROW_STATE_ALIGNED 2338 -> 2348、ROW_WITHOUT_STATE 607 -> 597、ALIGNED 2338 -> 2348、MISSING_LAST_ROW 159 -> 149；批次 6 追加验证（用户授权后）：mvn -B test -Dtest=ArenaPhaseRowContractTest,SensoryAreaRideRowContractTest,JournalReportRowSplitContractTest,JournalRewardRowRepairContractTest,ClientQuestSectionAlignmentTest,ArchdaevaRewardRowContractTest,AlignedMirrorRewardRowContractTest,MirrorPairRewardRowContractTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest,ExternalRewardAdvanceReentryContractTest,QuestCollectProgressAlignmentGateTest => Tests run 63 / Failures 0 / Errors 0，PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0，xmllint 4 个文件全部 validates、IDE lint 0 警告、git diff --check 干净；check_section0_requirements.py 复跑 UNREACHABLE 5 -> 3；同工作树前后对照审计（先 git checkout HEAD 这 4 个文件取基线再回放）ROW_ALIGNED 2566 -> 2570、ROW_BEHIND 277 -> 275、ROW_AHEAD 2593 -> 2591、ROW_STATE_ALIGNED 2348 -> 2352、ROW_WITHOUT_STATE 597 -> 595、STATE_OUT_OF_RANGE 2450 -> 2448、MISSING_TAIL_ROWS 92 -> 90、STATES_BEYOND_ROWS 2627 -> 2625、ALIGNED 2348 -> 2352（MISSING_LAST_ROW 149 不变），两轮审计输出逐行 diff 只有这 4 个任务变化；client-acceptance 未做（PENDING_CLIENT）；批次 7 focused-test（2026-09-21 用户授权 Maven：ReportRowRewardProjectionContractTest + 批次 1-6 的 12 个测试类 = 68 例全绿；PRODUCTION_COMPILE_OK=6189 / PRODUCTION_COMPILE_FAILURES=0 / PRODUCTION_INTERACTION_OBJECT_FAILURES=0 / PRODUCTION_WHITELIST_VIOLATIONS=0）；批次 7 static：xmllint quest_definition.xsd 21 文件全 validates、IDE lint 0 警告、git diff --check 干净；批次 7 同工作树前后对照（只回放这 21 个文件）：ROW_ALIGNED 2570 -> 2591、ROW_BEHIND 275 -> 254、ROW_STATE_ALIGNED 2352 -> 2372、ROW_WITHOUT_STATE 595 -> 575、ALIGNED 2352 -> 2372、MISSING_LAST_ROW 149 -> 129（ROW_AHEAD 2591、STATE_OUT_OF_RANGE 2448、INTERIOR_GAP 267、STATES_BEYOND_ROWS 2625 不变），两轮审计逐行 diff 只有这 21 个任务变化；批次 7 client-acceptance 未做（PENDING_CLIENT）；批次 8 focused-test（2026-09-21 用户授权 Maven：17 个测试类 => Tests run 84 / Failures 0 / Errors 0，含 Quest10522AutoStartDialogTest 的 reward 投影 1 与 `var0==0` 自愈边断言；PRODUCTION_COMPILE_OK=6189 / PRODUCTION_COMPILE_FAILURES=0 / PRODUCTION_INTERACTION_OBJECT_FAILURES=0 / PRODUCTION_WHITELIST_VIOLATIONS=0）；批次 8 static（xmllint 8 文件 validates、IDE lint 0 警告、git diff --check 干净）；批次 8 同工作树前后对照（只回放这 8 个文件）：ROW_ALIGNED 2591 -> 2599、ROW_BEHIND 254 -> 246、ROW_STATE_ALIGNED 2372 -> 2380、ROW_WITHOUT_STATE 575 -> 567、ALIGNED 2372 -> 2380、MISSING_LAST_ROW 129 -> 121（ROW_AHEAD 2591、STATE_OUT_OF_RANGE 2448、INTERIOR_GAP 267、MISSING_TAIL_ROWS 90、STATES_BEYOND_ROWS 2625 不变），两轮审计逐行 diff 只有这 8 个任务变化；批次 8 client-acceptance 未做（PENDING_CLIENT）；批次 8 回归收口（2026-09-21 用户授权 Maven）：43 个定向回归类 + 客户端契约门禁 + 批次 1-8 各门禁 => Tests run 302 / Failures 0 / Errors 0；PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0；改动的 21 个测试类改为 Objects.equals(sourceNode(), …)、7 个任务的 reward 行期望按客户端行数更新（1553=2、1988/2988=3、3082=3、14026/24026=5、14051=4、15550/25550=2、24030=9）；xmllint 10 文件 validates；IDE lint 无 error；批次 9（2026-09-21 用户授权 Maven）：Quest28932RewardRowContractTest（3 例）+ ArenaPhaseRowContractTest + QuestClientContractGateTest + QuestDefinitionCatalogManifestTest + ProductionCatalogWhitelistVerificationTest => Tests run 21 / Failures 0 / Errors 0；PRODUCTION_COMPILE_OK=6189 / PRODUCTION_COMPILE_FAILURES=0；xmllint quest_definition.xsd 28932 validates；全库审计同工作树前后对照：ROW_STATE_ALIGNED 2380 -> 2381、ROW_WITHOUT_STATE 567 -> 566、ALIGNED 2380 -> 2381、INTERIOR_GAP 267 -> 266（ROW_ALIGNED 2599、ROW_BEHIND 246、MISSING_LAST_ROW 121 不变），逐行 diff 仅 28932（last_start_var0 None -> 0）；批次 9 client-acceptance 未做（PENDING_CLIENT）；批次 10（2026-09-22，用户授权）：xmllint quest_definition.xsd 18/18 validates；聚焦 Maven 11 个测试类 38 例全绿（RetailSingleStepRewardRowContractTest 4 + MirrorPairRewardRowContractTest 4 + JournalRewardRowRepairContractTest 3 + QuestClientContractGateTest 1 + QuestDefinitionCatalogManifestTest 10 + ProductionCatalogWhitelistVerificationTest 1 + QuestDialog31RegressionTest 2 + QuestMinionTutorialRetailAlignmentTest 3 + ItemCollectingDialogProtocolAlignmentTest 6 + QuestHandoverContinuationAuditTest 2 + QuestRepeatLifecycleTest 2；因并行任务把 PlayerCommonData.java / CM_HOTSPOT_TELEPORT.java 留在编辑中的语法错误态，主工作树无法编译，按项目规则改用临时 worktree 验证并已 `git worktree remove --force` + `git worktree prune`）；PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0；全库审计同工作树前后对照 ROW_ALIGNED 2599->2617、ROW_BEHIND 246->228、ROW_STATE_ALIGNED 2381->2394、ROW_WITHOUT_STATE 566->553、ALIGNED 2381->2394、MISSING_LAST_ROW 121->108（ROW_AHEAD 2591、STATE_OUT_OF_RANGE 2448、INTERIOR_GAP 266、MISSING_TAIL_ROWS 90、STATES_BEYOND_ROWS 2625、BOTH_MISALIGNED 177、NO_REWARD_ROW 178、NO_CLIENT_HTML 608/650 不变），逐行 diff 仅这 18 个任务；客户端实机复测未做（PENDING_CLIENT）；批次 11（2026-09-22）：25608 单任务审计 ROW_BEHIND/MISSING_LAST_ROW -> ROW_ALIGNED（reward_var0 5->6、visible 0..6、rows_without_state 空）；临时 worktree（主工作树被并行任务 13 个 AMBIGUOUS_TRANSITION 阻塞）28 例全绿，PRODUCTION_COMPILE_OK=6189 / FAILURES=0；quest_definition.xsd 与 zones.xsd 均 validates；IDE lint 0 warning；批次 13 static（apply_batch13 脚本 --check 6/6 OK、xmllint --schema 6/6 validates、IDEA lint 0 problem、单任务审计 6/6 ROW_BEHIND/MISSING_LAST_ROW/ROW_WITHOUT_STATE -> ROW_ALIGNED/ALIGNED/ROW_STATE_ALIGNED，全库 ROW_ALIGNED 2633->2639、ROW_BEHIND 211->205、MISSING_LAST_ROW 99->93；RewardRowTwoRowTalkFamilyContractTest 6 例未跑 Maven，PENDING_MAVEN/PENDING_CLIENT）；批次 14 static（apply_batch14 脚本 --check 4/4 OK、xmllint --schema 4/4 validates、IDEA lint 0 problem、单任务审计 4/4 ROW_BEHIND/MISSING_LAST_ROW/ROW_WITHOUT_STATE -> ROW_ALIGNED/ALIGNED/ROW_STATE_ALIGNED，全库 ROW_ALIGNED 2639->2643、ROW_BEHIND 205->201、MISSING_LAST_ROW 93->89）；Maven 原授权命令重跑 29 例全绿 （PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / WHITELIST_VIOLATIONS=0，覆盖本批 4 个 XML 的生产目录编译）；追加授权后 8 个测试类 35 例全绿（含新增 RewardRowEventTwoRowContractTest 6/6）；客户端 PENDING_CLIENT；批次 15 static（apply_batch15 脚本 --check 2/2 OK、xmllint 2/2 validates、IDEA lint 0 problem、单任务审计 2/2 ROW_BEHIND/MISSING_LAST_ROW -> ROW_ALIGNED/ALIGNED/ROW_STATE_ALIGNED，全库 ROW_ALIGNED 2643->2645、ROW_BEHIND 201->199、MISSING_LAST_ROW 89->87）；Maven 已授权 8 类重跑 35 例全绿（PRODUCTION_COMPILE_OK=6189/FAILURES=0）；追加授权后 9 个测试类 41 例全绿（含新增 RewardRowResidualTwoRowContractTest 6/6）；客户端 PENDING_CLIENT；批次 17 追加授权后 mvn 12 个测试类 58 例全绿（2026-09-22 12:31，含 DurableDaevanionWeaponRewardRowContractTest 7/7，PRODUCTION_COMPILE_OK=6189 / FAILURES=0）；批次 18（2026-09-22）：MirrorRewardProjectionLagContractTest（5 例：镜像领奖行参照 + planner 收敛 + 无事务写非领奖行 var0 + 16837/16986/16988 锁定护栏 + 两侧 var0 一致）；15 个测试类 92 例全绿（PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0）；xmllint quest_definition.xsd 6/6 validates、IDEA lint 0 problem；单任务审计 6/6 ROW_BEHIND -> ROW_ALIGNED（recovery=True、rows_without_state 空）、全库 ROW_ALIGNED 2650 -> 2656 / ROW_BEHIND 196 -> 190（row_state 计数不变）；客户端 PENDING_CLIENT；批次 19（2026-09-22）：CollapsedSingleStepLadderContractTest（7 例：reward 投影 == 镜像领奖行 + 每行一个状态 + 客户端页按钮链 + 证据消耗 + can-act/collecting-step 行号 + 旧存档自愈 + 无塌陷跳转）；16 个测试类 99 例全绿（PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0）；首轮 QuestClientContractGateTest 报 BUTTON_WITHOUT_ROUTE（15000：select2 页 1353 无路由）后按客户端页按钮链拆成 1352/1353 两条路由；xmllint 2/2 validates；单任务审计 2/2 ROW_BEHIND -> ROW_ALIGNED、全库 ROW_ALIGNED 2656 -> 2658 / ROW_BEHIND 190 -> 188 / ROW_STATE_ALIGNED 2428 -> 2430 / ROW_WITHOUT_STATE 521 -> 519；客户端（2026-09-22 用户实机确认 10527 验收通过：使用道具 182216075 后任务书切到「和代理人维达ID对话」的领奖行、不再停在第 14 行，`REWARD/var0=15` 投影与旧存档自愈边在真实客户端链路生效；本卡其余任务（20527/10528/20528、批次 1-5 各族与批次 51/52 等）仍 PENDING_CLIENT，不得据此条宣称它们已验收）
boundaries: 与 QE-045 是同一迁移的两条收口方式，不得互相批量套用（批次 2 已按该边界排除 QE-045 锁 10 个与 QE-046 引擎外直写 8 个）：15300/25300 的客户端验收值是进入前的 packed step（13），15001 与 10527 的验收值是客户端领奖行（1、15）；判定必须以客户端 quest_summary 行清单 + 镜像任务为准；10529/10530 等折叠步骤链需要拆分状态而不是只改 reward 行；全库筛查（MISSING_LAST_ROW 492，其中末行是“对话/报告/见面”417、QE-045 锁 10、镜像同样缺末行 192；STATES_BEYOND_ROWS 2627 里 2545 个客户端只有 1-2 行、var0 实际是计数/阶段槽）中多数任务 var0 并非任务书行索引（反例：1221 的 var0 是击杀计数、1001 客户端 5 行而服务端 var0 0..8、10504 的 reward=4 而客户端只声明 4 行），禁止机械批量改；行号判据的另一处已知偏差：任务书里可能有与相邻状态共用 visible 槽位的空行（10530 第 8 行与第 9 行同为 [%24]），此时 client_rows 比真实状态数多 1，必须用“可见槽位集合 + 镜像 20530 + 旧 handler 步骤序列”收口（10530 reward=9 而不是行号推出来的 10）；SECTION 位段隔离的 14 个挂账任务（1842/1843/1844/2843/2844/2845、4928、16800、18738、19078、20034、28738、29074、29078）需要逐个客户端脚本证据，禁止机械搬 offset；24201/24202 仍未收口（quest_script 是 Progress(SECTION_0<12; SECTION_5==0) 与 Progress(2)，SECTION_0 可能同时充当击杀计数槽，先观测再决定补 stage1 还是改计数模型）；多计数器任务（18208/18209/28208/28209 族）的领奖行投影按第 4 条行状态判据（reward=2），与 QE-045 的 pre-REWARD step 例外族（15300/25300 等有客户端验收记录）分开；这一族的客户端 script 只声明到行 0/1 的 SECTION_0，不代表领奖行不需要独立状态；SECTION_0 约束审计的 `UNREACHABLE` 分两类：18208/18209/28208/28209 是迁移丢阶段（可按第 11 条修），1922/2947/14054 是 legacy 从未实现客户端声明的中间阶段（1922/2947 用 `defaultCloseDialog(env, 0, 4)` 直接跳过 var0=1/2/3 的对话+收集+野外击杀；14054 只实现第一轮，缺第二轮 12/13、Barantina 两次对话与最终报告），后者属于任务链实现缺口（需要补对话/传送/掉落/击杀阶段并逐页核对客户端 HTML），不得按行投影模板机械修；批次 7 边界：只收口“末行 NPC 命中 reward 路线”的报告/交付型 21 个；10522/20522/15542/25542/30211/30213/30311/30313 被 QE-046 基线（src/test/resources/quest/external-reward-advance-baseline.tsv）锁住——reward 投影必须等于引擎外写入方留下的 packed step（批次 8 已把这 8 个任务的写入方与投影统一到领奖行 1：写入方先写 var0=1 再置 REWARD、投影 0->1、自愈边改 `var0==0`，并重刷 src/test/resources/quest/external-reward-advance-baseline.tsv），不能只改 XML；30614（末行 Astella=800327，定义 NPC 却是 Aluna=800326）、26838（末行 Jarik01=806574，定义 806575）、19064/29064（末行 Jucleas/Balder，定义 Lavirintos/Kvasir）需先核实领奖 NPC 身份；28932/30203/30303 的 started 节点没有 var 投影，要补 var0 投影才能让行 0 有状态；1607/1990/2990/3502/14012/14013/17511/27511 属多阶段/内部缺口（INTERIOR_GAP、MISSING_TAIL_ROWS），必须单独设计阶段推进；批次 8 补充边界：无 source 自愈边会让所有 `sourceNode().equals(...)` 形式的测试 helper 抛 NPE，新增该族边时必须同步做 null-safe 化（本次 21 个类）；领奖态入口页与 `npc-complete` 必须成对出现在同一个 owner 上，只加半边的写法会被 QuestClientContractGateTest 直接判失败；引擎外写入者任务（10522/20522/15542/25542/30211/30213/30311/30313）的 reward 投影、写入方与基线 TSV 三处必须同改；28208/28209 的 Inggness(205320) 与 Anja(205321) 目前并存为领奖 NPC，实机复测若确认 retail 只允许 Anja，再按 QE-046 收口 Inggness 的 NPC_REPORT/npc-complete；批次 9 边界：自闭合 `started` 的补投影只允许声明行号字段，声明计数器会把“满计数恢复路线”挡掉（28932 的 var1>=1 对话路线有回归门禁）；`var0` 作为标志位/计数槽的任务（30203/30303 等）属审计误报族，必须用 legacy handler 与客户端 SECTION 声明核对后再决定是否收口，禁止按“末行行号”机械推进；批次 10 边界：retail 单步族的收口模板是族内 184 个已对齐任务，不要再按“末行 NPC 命中”批量推进；21455 的 retail end_npc_ids=799244（server `name_desc=Unset`，与客户端 `STR_DIC_N_Unset` 一致）与 typed 定义当前把领奖放在起始 NPC 799404 上不一致，NPC 归属需单独核实（本批只改行投影）；25608 已在批次 11 收口（retail 7 step ↔ 客户端 7 行；实际缺 ENTER_AREA 206534 与 ENTER_AREA 206542 两行，旧链把 HUNT/Mumu/交付行错放在 var0=2/3/4、reward 停在 5；修复为 step2/step5 两个 enter-zone、HUNT→step3、Mumu SELECT5→step4、交付行归 step6、reward 5→6，并从客户端 Levels/DF6/Level.pak 的 mission_mission0.xml 取触发点注册两个 sensory zone）；多阶段 ENTER_AREA 行对齐必须同时补 zones_quest.xml（zone 名沿用同族 A/B_DYNAMIC_ENV 规则），只改 quest XML 会因 zone 未注册而永远不触发；29002 的 legacy handler 语义是“204099 的 STEP_TO_1 写 var0=1、204257 领奖不改 var0”，与镜像 19002 同值；19008/19014/19020/19026/19032 等“名人考试”族的 reward=1 属 legacy 语义（19057/29057 的 handler 另有 var0=2 的失败分支），不得按“末行行号 2”修改；剩余 MISSING_LAST_ROW 95 个（108 − 13）里镜像同缺 32、QE-045 锁 10，其余需逐族 legacy/retail 证据；批次 12（2026-09-22）已核实 19064/29064/21455/30614 的领奖 NPC 归属（30614 由 Aluna 800326 回滚为 Astella 800327：客户端行 1「向 Astella 报告」+ quest_complete「阿斯泰拉说必须…」+ 同族命名规则 4/4 + 2026-08-06 f737cfef1 客户端/真端交叉审计；legacy `terath_dredgion.xml` 单源不足以覆盖），26838（末行 Jarik01=806574 / 定义 806575）仍待单独取证；批次 13 边界：同形的 QE-045 锁任务（13965/23965 由 enter-zone、15674/25674 由 CHECK_COLLECTED_ITEMS 置 REWARD）不得随族批量翻动——它们的 `reward var0=0` + `REWARD && var0==1` 恢复边是 f6aff952a 的基线并被 LegacyRewardStepProjectionRegressionTest 20 例硬锁，重定基线需一次客户端观测（REWARD 态任务书高亮行 0 还是行 1）；`last_row_npc_matches_quest=False` 在客户端表名带前缀（LF2a_dromik_G_DHM、DF2a_Nevma_G_LHM、IDRUN_Entrance_guide）时只是 STR_DIC key 不同形，不等于归属错误；批次 14 边界：8060x 族里只有头本 80601/80606 的 var0 是行索引，80602..80605/80607..80610 的 var0=3/4/5/9 是阶段计数（审计 STATES_BEYOND_ROWS），禁止按行号机械压成 1；行内用字面中文名（80255/80256）“和帕尔图对话”或非 NPC 键（1123 的 STR_DIC_LA12）时，归属必须用客户端 NPC 表 + npc-complete owner 交叉核对；1466 的 reward 节点没有 var0 投影（NO_REWARD_ROW）、2484/4712 的 completion owner 不唯一、2842 由击杀自环推进，均须单独设计；批次 15 边界：1466 被 `Quest1466ClientDialogAlignmentTest` 硬锁（reward 必须无投影、报告路由必须写 `var0=2`、headless journey 通过），按行号收口需先重定该基线（要有客户端观测）；4712 的行 1“向 Henir 报告”与 completion owner（279042 / 798327 / 798330 = Dreadgion_prisoner_dark1/4）不一致，属归属核实题；2842 的 var0 是 0..39 击杀计数且行 1 槽位是 `[%15]`（值 5）；行内键若属 actor 命名空间（`STR_DIC_LA*`）必须用同族 completion owner 交叉解键，不能用 `last_row_npc_matches_quest=False` 直接判缺陷；批次 17 边界：LEGACY_NEXTSTEP_DROPPED 族已清空，修领奖行前先跑 .agents/summary/quest-10527-reward-row/audit_legacy_reward_entry_steps.py 取 legacy packed step（区分“迁移丢 nextStep”与“legacy 本身保持 step”）；STEP_EQUALS_NEXTSTEP 的 392 个任务（如 2600/1920/2945 的 defaultCloseDialog(env, s, s, true, ...)）与 QE-045 基线（15300/25300 的 reward=13、3722/4722 等 QE045_LOCKED）不得按“客户端末行”机械推进；同族变体若客户端只有 1 行，禁止保留越界投影（80290/80294 即 STATE_OUT_OF_RANGE）；批次 16 边界：4712/2484 的 completion owner 已按 QE-052 从行 0 囚犯（798327/798330）与接取 NPC/烽火对象（204407/700267）收敛到领奖行 NPC（279042/203331），这两个任务不再属于“owner 不唯一”的挂账；其余 owner 归属只允许在保留入口路由的前提下收敛；批次 18 边界：镜像单侧投影落后族一律以“已对齐的那一侧”为参照基线（两侧 quest_summary 行数相同、末行 NPC 都能在任务内对上），只改 reward 投影 + 补自愈边，不动状态阶梯；16837/16986/16988 被 QuestPrematureRewardRouteExclusionTest 的 RewardCase.reward={var0:0} 锁定（完成报告后 packed var0 保持 0），属既有契约，未取到客户端观测前禁止按镜像推进；镜像两侧投影比较只比行号 var0，镜像侧可另有计数槽（27160/27161 带 var1=10）；批次 19 边界：单步塌陷对（Elyos 存根：镜像已有完整阶梯、本侧只剩行 0）**不能只改 reward 投影**——那只指向一个服务端没有状态的行，必须按客户端 quest_summary 行数与对话页 HACTION 按钮链补回 START 阶梯；`npc-complete` 的 `<preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>` 已展开 reward 自环，不得再显式声明 SELECT_QUEST_REWARD 的 reward 自环（AMBIGUOUS_TRANSITION）；客户端页上的按钮必须在服务端有落地页路由（1352 -> SELECT2、1353 -> SELECT2_1），否则 QuestClientContractGateTest 报 BUTTON_WITHOUT_ROUTE；剩余塌陷对 23809/13809（还需 owner 收敛）、23918/13918（串行阶梯 vs 组合计数口径未定）、24046/14046（INTERIOR_GAP，缺行 4/7）、1000/11000、39713/49713（页动作不同形）需逐族处理
superseded_by: none
see_also: [QE-012], [QE-045], [QE-046]
first_check: 领奖阶段任务书停在“使用道具/调查”一行或空白时（先确认末行确实是领奖行：末行 STR_DIC_N_XXX 要能在 npcs_unpacked/client_npcs_npc.xml 的 <name> 里对上任务内 NPC，对不上就先别改）（审计脚本的客户端索引必须大小写不敏感：Dialogs 下同时存在 quest_q<id>.html 与 QUEST_Q<id>.html，否则 2016 个任务会被误判成 NO_CLIENT_HTML），先跑 .agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py 比对 reward 投影与客户端 quest_summary 行索引、并核对“每一行都有 START/REWARD 状态”（缺口行 = 永远不亮的行），再看 (q, q+10000) 镜像是否一致，最后按客户端领奖行同时交付投影与无 source enter-world 恢复边；动手改 reward 投影前先确认任务书的可见槽位与行数是否一一对应（client_rows != client_states 时空行只与相邻状态共槽，不要按行号加一），再确认所有 varN（N>=1）落在 6N，否则客户端读到的 SECTION_0 根本不是 var0
keywords: 领奖任务书不切换、10527、20527、reward 投影、SECTION_0、quest_summary、领奖行、useQuestItem、changeQuestStep reward、REWARD var0、任务书停在上一行、报告/交付型末行、成长任务族、19672、18036、3210、引擎外写入方领奖行、writer step=1、retail 单步任务、zz_retail_simple_quests、1527、21455、26838、29002、client_rows 空行、镜像单侧投影落后族、MirrorRewardProjectionLag、11110、17526、单步塌陷
-->

- **判定规则**：`reward` 节点的 `var0` 投影就是客户端任务书 `quest_summary` 的行索引（SECTION_0），必须等于领奖行——默认是最后一行“和 X 对话/报告”；交接 transition 不改写该字段时由目标投影补足；已落盘的错位存档必须有 `status=REWARD && var0==from` 的无 source `enter-world` 恢复边。交互物族（批次 20 补充）：由世界物件/交互物（`ai="quest_use_item"` 的 FOBJ、`USE_OBJECT` 路由）承载中间行的任务，每一行必须是**该行交互物**的两条边——`USE_OBJECT -> SHOW_QUEST_PAGE 该行自己的页`（不能三行共用同一页，页 id 见客户端 `select2/select3/select4`）+ `SETPROx -> 下一行` 并**发放本行采集物**（`give-item`）；发放依据是 `quest_data.xml` 只声明 `quest_work_items`、交互物没有 `quest_drop`（否则玩家永远拿不到交付物，`has-item` 交付分支永不可达）。同步模式对标 legacy `sendUpdatePacket`：START 行只发 `SM_QUEST_ACTION`（`PACKET_ONLY`），进入 REWARD 的那一行额外 `updateZone/updateNearbyQuests`（`LEVEL_AND_VISIBILITY_REFRESH`）。副本段补充（批次 22）：与副本 NPC 对话后**不得**一步跳到副本内行；进副本的行要由 `enter-world + world-is <副本世界>` 驱动，并保留副本外的回退（`die` / `expected=false` 的 `enter-world`）回到“可重新进入”的那一行，否则被跳过的行永远不亮、副本内掉线或死亡也会卡在没有对话的行号上。
- **为什么容易漏**：旧 `QuestHandler.changeQuestStep(..., reward=true)` 只置状态、不写 `nextStep`，迁移时 `to` 看起来“没有产物”，reward 投影就地抄了 `from`；静态门禁、目录编译、白名单全绿，只有玩家跑到领奖那一步任务书才停在上一行；同批镜像任务分别迁移，两侧差 1 也没人比对。
- **代表案例**：10527（`useQuestItem(env, item, 14, 15, true)`，reward 投影应为 15，镜像 20527 即 15；2026-09-21 修复并补 `REWARD/var0=14` 恢复边；2026-09-22 用户实机确认通过（使用 182216075 后任务书切到领奖行））、10528/20528（批次 53 修正：领奖投影 = legacy 落盘 step 11，交接不写该字段、自愈边 REWARD/var0=12 -> 11）与 10525/20525（两侧都是 6、客户端 8 行只到第 6 行，第 7 行领奖行无状态；同日改为 7 并补 `REWARD/var0=6` 恢复边）；批次 1（2026-09-21，用户授权后执行）：镜像一侧已对齐的单侧缺陷 9 组——11294->1（镜像 21294）、15002/15010/15070/15514->1（镜像 25002/25010/25070/25514）、19004->2（镜像 29004）、25062->1（镜像 15062）、25073->1（镜像 15073）、26820->2（镜像 16820），全部按“reward 投影 + 无 source enter-world 自愈边”收口并由 AlignedMirrorRewardRowContractTest 锁定；批次 2（2026-09-21）：天/魔镜像两侧都缺末行的 120 个任务（10110/20110、13700/23700、13961/23961、14031/24031、15323/25323、15502/25502、18806/28806、28940/28940 一类的对话/报告末行），统一 reward 投影 N-2->N-1 + enter-world 自愈边；未纳入：QE-046 引擎外直写 8 个、末行与首行重复 12 个（15300/25300 型）、末行 NPC 对不上 68 个、reward 路线已写 var0 的 12 个（25052/28253 已单独补齐）、以及条件不自洽的 18036/28036；批次 3（202 个任务）：末行 STR_DIC_N_XXX 命中 reward 路线 NPC 的单侧任务（11001/11009 交信链、1218/1319/1322/1324 交付链、1361/1430/1452/1464 对话交付链等），统一 reward 投影->末行 + enter-world 自愈边；未纳入：2600/21075（末行 NPC 不在 reward 路线）、3090（紧凑布局、加宽会与 var1/var2 重叠）、24201/24202（reward 行已修，但仍缺 var0=1 的击杀阶段，形状为 INTERIOR_GAP）、10529/20529/10530/20530 折叠链；三批累计修复 331 个任务；同族待确认：10529/20529（缺第 11 行、第 10 行报告目标压在 REWARD 上，需拆折叠链）、10530/20530（缺第 9/10 行且两侧客户端行数不同）、10522/20522（外部写入者，见 QE-046）；批次 4（2026-09-21，用户授权后执行）：10529/20529 把压在 REWARD 上的报告行拆成独立 START 节点 s10（var0=10）并把 reward 推到最后一行 11，接回 select11/select11_1/select11_1_1 报告链并补 QuestDialogPage/QuestDialogAction 的 SELECT11_1_1(6502)，10530 与镜像 20530 对齐到 reward=9（第 8 行是与第 9 行共槽的空行，行号判据会多算一行），3090 把 var1/var2 从 SECTION_0（bit 2/3）移到 bit 6/12 并把领奖行从 3 推到 4，新增全库 varN=6N 门禁（14 个历史任务显式挂账），三者由 JournalReportRowSplitContractTest 与 ClientQuestSectionAlignmentTest 锁定；仍未收口：24201/24202（INTERIOR_GAP，SECTION_0 疑似计数槽）以及位段清单里其余 14 个任务；批次 5（2026-09-21，用户授权后执行）：15550/25550（三段对话链）与 15551-15554/25551-25554（感应区坐骑）共 10 个任务——按 origin/history 旧 handler 恢复行推进（坐骑族 A_TO_X var0 0->1 + X_TO_A var0 1->2 并置 REWARD）、新增中间行 START 节点 s1、reward 推到第 3 行并同步 `var0` width/max 1->2、保留迁移的两条 unaccepted->started 自动接取、补 REWARD/var0=1 -> 2 自愈边，由 SensoryAreaRideRowContractTest（8 条合同）与 MirrorPairRewardRowContractTest（追加 15550/25550）锁定；批次 1-5 累计修复 345 个任务（331 + 批次 4 的 4 个 + 批次 5 的 10 个）；批次 6（2026-09-21，用户授权后执行）：18208/18209/28208/28209（苍穹试炼场单人竞技场）——按客户端 quest_script 的 SECTION_0 声明恢复“2 阶段 + 2 计数器”模型（5 次 217819 击杀在 var1、精英 218185/218200 写 var2 并进入 REWARD，并把客户端未声明的 218192 移出目标）、reward 投影按领奖行合同设为 2、补 4 条旧存档收敛边（旧 k1..k4 回第 1 行、k5..k7 进第 2 行、REWARD 低/高值收敛到领奖行），由 ArenaPhaseRowContractTest（6 条合同）锁定；批次 1-6 累计修复 349 个任务（331 + 4 + 10 + 4），并新增 SECTION_0 约束审计（check_section0_requirements.py，UNREACHABLE 5 -> 3）；批次 7（2026-09-21，用户授权后执行）：报告/交付型末行 21 个——成长任务族 18 个（19672..19694 / 29672..29694，客户端 2 行“收集成长货币 → 向成长支援教官报告”，定义里唯一 NPC 806698/806700 既接取又领奖）reward 0->1 + enter-world 自愈边；3210（客户端 3 行，末行“和 Shugo_Shulack_02 对话”，镜像 4210 已是 2）reward 0->2 + var0=0/1 两条自愈边；18036/28036（2 行“和德拉坎战士对话 → 向 Demades/Latkel 报告”）新增 s1(START,var0=1) 节点、SETPRO1 事务改指 s1、两条 `var0>=1` 报告事务改以 s1 为 source、reward 0->1 并补自愈边（修掉“交出物品后无任何事务可匹配”的卡死），由 ReportRowRewardProjectionContractTest（5 条合同）锁定；批次 1-7 累计修复 370 个任务（349 + 21）。批次 8（2026-09-21，用户授权后执行）：QE-046 引擎外写入方与领奖行求交的 8 个任务（10522/20522 CM_CREATIVITY_POINTS、15542/25542 CoalescenceService、30211/30213/30311/30313 RiftOrbAI2）——写入方在置 REWARD 前写领奖行 `setQuestVarById(0, 1)`、reward 投影 `0 -> 1`、旧自愈边改 `var0==0`，重刷 external-reward-advance-baseline.tsv，由 ExternalRewardAdvanceReentryContractTest 与 Quest10522AutoStartDialogTest 锁定；批次 1-8 累计修复 378 个任务（370 + 8）。；批次 10（2026-09-22，用户授权后执行）：retail 单步族（start + 1 step，249 个里 184 个早已 reward var0=1）落后的 18 个 — A 组 12 个末行完全没有状态（1527/1528/1725/2135/2247/2266/3087/4020/21455/26838/80735/80736）、B 组 5 个末行有中间态但 reward 停在行 0（1963/1964/16838/16977/18035）、C 组 29002（镜像 19002 已对齐 + legacy handler 证据）——统一 reward 投影 0->1 + 无 source enter-world 自愈边，由 RetailSingleStepRewardRowContractTest 锁定；审计 ROW_ALIGNED 2599->2617、MISSING_LAST_ROW 121->108；批次 11（2026-09-22，用户授权后执行）：25608 补齐两个 ENTER_AREA 行（step2 206534 / step5 206542），HUNT→step3、Mumu SELECT5→step4、交付行归 step6，reward 投影 5→6 并补 REWARD/var0<6 的 enter-world + QUEST_SELECT 自愈边；drop collecting-step 0→6；从客户端 Levels/DF6/Level.pak 的 mission_mission0.xml 触发点注册两个 zones_quest zone，由 Quest25608RetailSevenStepAlignmentTest（6 条合同）锁定；单任务审计 ROW_BEHIND→ROW_ALIGNED；批次 12（2026-09-22）：领奖 NPC 归属核实族 4 个任务——19064/29064 新增 s1(START,var0=1)、行 0 路由交 Undin/Darfen(798450/798452)、领奖/completion 从起始 NPC 203701/204053 改到 Jucleas/Balder(203752/204075)、reward 0→1 + `var0=0/2` 双自愈边；21455 领奖/completion 799404→Unset(799244)、删除客户端无按钮的 SETPRO2、换物品并入 SETPRO1；30614 reward 0→1 + `var0=0` 自愈边 + 领奖态入口页 `reward + QUEST_SELECT(31) -> DEFAULT_SUCCESS(10002)`（位于 npc-complete 之后且唯一），并把归属从 Aluna(800326) 回滚为 Astella(800327)（客户端行 1 + quest_complete + 同族 4/4 + f737cfef1；10a2e7e57 仅据 legacy 单源改错并登记在 report-npc-mismatch.csv 第 64 行）；由 RewardNpcOwnershipContractTest（6 条合同：owner 归属、talk 路线集合、Dredgion 族命名规则、自愈边与 planner、入口页唯一且在 npc-complete 之后、SETPRO2 删除）锁定；单任务审计 4/4 ROW_ALIGNED/ALIGNED/ROW_STATE_ALIGNED，`last_row_npc_matches_quest` 523→519；批次 13（2026-09-22）：5.8“两行、末行是与领奖 NPC 的对话”族 6 个任务（1926/2938 承 `_1926/_2938Secret_Library_Access`，39003/49003 客户端行 1 = 800504/800505，80989/80990 事件链 836196）reward 投影 0->1 + 无 source 自愈边，由 RewardRowTwoRowTalkFamilyContractTest（6 条合同，含“QE-045 锁的 4 个同形姊妹任务必须保持 reward var0=0”的显式边界）锁定，单任务审计 6/6 ROW_ALIGNED/ALIGNED/ROW_STATE_ALIGNED；批次 14（2026-09-22）：事件族 4 个任务（80255/80256 是 80255..80260 活动烟花族的漏网项，同族 80257-80260 已由批次 1-7 `7a7d27809` 对齐；80601/80606 是德雷得奇安事件链头本，legacy `setQuestVarById(0, 1)` 是硬证据）reward 投影 0->1 + 无 source 自愈边，由 RewardRowEventTwoRowContractTest（6 条合同：投影/owner/自愈边 + planner/无事务写非领奖行 var0/同族参照 80257-80260 保持 1/击杀事务保留 legacy var0=1）锁定，单任务审计 4/4 ROW_ALIGNED/ALIGNED/ROW_STATE_ALIGNED；批次 15（2026-09-22）：残余两行族 1123/2484（STR_DIC_LA12 跨族解键 = Pernos 790001；legacy `_2484OurManInElysea` 的烽火 `setQuestVarById(0, 1)`）投影 0->1 + 自愈边，由 RewardRowResidualTwoRowContractTest（6 条合同：投影/owner/入口路由保真/自愈边 + planner/无事务写非领奖行 var0/STR_DIC_LA12 解键护栏）锁定；至此两行 + 末行对话族 12 个全部收口；批次 16（2026-09-22）：领奖 owner 收敛 4712/2484（见 QE-052）；批次 17（2026-09-22）：圣灵守护者武器事件族 80290/80291/80294/80295——先用 .agents/summary/quest-10527-reward-row/audit_legacy_reward_entry_steps.py 扫描迁移前 handler 的进入 REWARD 调用（651 个任务），把“nextStep 被丢弃”精确到 15300/25300（QE-045 基线）+ 80291/80295（收口后 LEGACY_NEXTSTEP_DROPPED 为空）；80291/80295（客户端 2 行）投影 0->1、80290/80294（客户端仅 1 行、原投影 1 属 STATE_OUT_OF_RANGE）投影 1->0，各补对应自愈边，由 DurableDaevanionWeaponRewardRowContractTest（7 例）锁定；批次 18（2026-09-22）：镜像单侧投影落后族 11110/14201/16974/17160/17161/17526——同形镜像对（q/q±10000）客户端行数相同、末行 NPC 都能在任务内对上，但只有一侧 reward 投影等于领奖行，已对齐侧作基线（11110/21110=1、14201/24201=2、16974/26974=1、17160/27160=1、17161/27161=1、17526/27526=1），6 个任务本就有完整 0..N-1 行状态，修复只需投影 + REWARD/var0=0 自愈边，由 MirrorRewardProjectionLagContractTest（5 例）锁定；批次 20（2026-09-22）：焦树族 23809/13809（三棵共用世界物件 DeadTree 730969/730970/730971，客户端 4 行：行 0/1/2 依次调查三棵树并采集 quest_<id>a/b/c，行 3 向 LDF5_Fortress_Village_Guard01_D(802429)/L(802427) 报告）——23809 被塌陷成“三棵树各一条 started -> reward 直跳 + 整包交付”（缺行 1 2）、13809 阶梯已对但三棵树各挂 NPC_START/npc-complete（QE-052）；本批两侧同形重建阶梯、每棵树只开自己的页（SELECT2/3/4）并按 SETPRO1/2/3 推进行 + 发放本行采集物、行 3 用 `reward --QUEST_SELECT --> SELECT5` 承接、owner 收敛到守卫，由 TreeLadderOwnerTrimContractTest（7 例，含 planner 顺序门禁）锁定；单任务审计 ROW_BEHIND -> ROW_ALIGNED（23809）、全库 ROW_ALIGNED 2658 -> 2659 / ROW_BEHIND 188 -> 187 / ROW_STATE_ALIGNED 2430 -> 2431 / ROW_WITHOUT_STATE 519 -> 518；批次 22（2026-09-22）：24046（The Shadow Calls）——legacy `changeQuestStep(env, 3, 5, false)` 把行 4「进入 DC1_door_Q2076 寻找沉默审判官」整格跳过、reward 投影停在 6（客户端末行是行 7“向 Muninn 报告”）；修复补 `s4` 让 3→4 只推一格并保留副本传送、4→5 由进入副本 320120000 驱动、4→3 做副本外回退、reward 投影 6→7 + 无 source `REWARD/var0=6 -> 7` 自愈边，由 ShadowCourtRowLadderContractTest（6 例）与 JournalRewardRowRepairContractTest 的 `Contract(24046, 7, 6)` 锁定，报告 §二十六；批次 23（2026-09-22）：39713（[Daily] Fresh Powder，绿帽团阵营日任天族侧）被迁移把 3 行任务书塌陷成 9 条无守卫 `started -> reward` 直跳（npc-item-report ×3 + SET_SUCCEED ×3 + SELECT_QUEST_REWARD ×3），行 1/行 2 没有任何 START/REWARD 状态、`started --QUEST_SELECT` 错开成报告页 select5、reward 投影停在 0（客户端领奖行是 2）、报告路由还重复要求已经用掉的净化粉末 182215285；按客户端行号重建阶梯（`started --SETPRO1--> powder-received(1)` 发 182215285、`powder-received --use-item--> powder-used(2)` 消耗道具、`powder-used --SELECT_QUEST_REWARD--> reward(2)`、`started`/`powder-used` 的 QUEST_SELECT 分别给 select2/select5）+ 无 source `REWARD/var0=0 -> 2` 自愈边，`var0` width=1/max=1 -> width=2/max=3，与已对齐的魔族镜像 49713 同形，由 FactionDailyRowLadderContractTest（7 例）与 JournalRewardRowRepairContractTest 的 `Contract(39713, 2, 0)` 锁定，报告 §二十七；批次 24（2026-09-22）：空槽位族登记 —— 只读扫描 9116 个客户端任务书，13 个任务的 quest_summary `<step>` 可见文本全空或尾随为空（序幕 1000/2000 的 4 个空槽只挂 `[%collectitem]` 占位符，quest.xml 既无 collect_item 也无 NPC，服务端 enter-zone → movie → complete、无 REWARD 节点；1400 的行 0 是击杀计数、var0/var1 是 8x4 组合），行号口径会误判成 NO_REWARD_ROW / MISSING_TAIL_ROWS / ROW_AHEAD；登记为审计脚本的 `BLANK_JOURNAL_SLOT_EXCEPTIONS`（判定不变），由 BlankJournalSlotBoundaryContractTest（4 例）锁定“不得按空槽补行阶梯”，扫描脚本 audit_blank_journal_slots.py 与明细 blank-journal-slots.tsv，报告 §二十八
- **批次 51 补充判据（2026-09-22）**：任务书行号与客户端 `visible` 槽位的固定换算。全库 471 个
  `ROW_STATE_ALIGNED`（客户端 ≥3 行、无脚本计数）任务里，任务书行 0..N-1 的 `visible` 槽位恰好是
  `3×行号`（0/3/6/…），而服务端投影就是连续行号 0..N-1——即客户端把「行号」映射到「槽位 3×行号」的
  visible/color 双位，**服务端只需保证每一行都有 var0 = 行号的 START/REWARD 状态**。据此收口批次 51
  （3938/4942 神圣圣殿骑士晋级双子，报告 §五十五）：11 行任务书原本阶梯只到 s8、`reward` 投影停在 8/0，
  末两行永远不亮；修复补 s9（仪式行，START）与 s10（领奖行，REWARD，var0=10），仪式道具路由改到 s9、
  起始 NPC 处补 `s9 --SELECT_QUEST_REWARD--> s10` 领取路由、`npc-complete` 归属迁到 s10，
  并补 `REWARD/var0<10 -> 10` 自愈边；审计由 `ROW_BEHIND | MISSING_TAIL_ROWS` 变为
  `ROW_ALIGNED | ALIGNED`，门禁 `HolyTemplarFinalRowPairContractTest`。判据边界：这条换算只适用于
  「阶梯齐全、仅缺尾行」的行号族；2289 那类是客户端 step（击杀计数占多个 slot），按 `Progress(a~b)` +
  `collect_progress` 判定，禁止互相套用；此外 `npc-complete` 的 source 节点必须投影 REWARD、
  `complete` 必须可达，把「领奖行」定在最后一行时只能有一个 reward 角色节点。
- **批次 52 补充判据（2026-09-22）**：同形「计数行走行」族的副本对象 id 必须成对登记。18301/28301
  （阿图拉姆空中要塞监视水晶球）的客户端脚本声明 `Progress(0~6)`——七个水晶球占 step 0..6、第七个把
  SECTION_0 推到 7，H-Core 由副本脚本在 Weapon Hugen 死亡时生成，领奖态与满计数同为 step 7（与批次 50 的
  2289 同类合同）；同一水晶球在普通副本 300240000 与活动副本 300241000 分别以 **702656 / 730373** 生成，
  `kill-npc` 必须同时登记两个 id（同族 18314/28314 的既有口径），否则只覆盖一种副本。天族 18301 原本被塌陷成
  `started -> reward`（reward 投影 0），本批与魔族镜像 28301 收敛成同形：唯一 Hariken(799530) owner、
  装置只在 H-Core(730374) 的 `SETPRO2` 发放、`REWARD/var0<7 -> 7` 自愈边；门禁
  `AturamSkyFortressCrystalLadderContractTest`，报告 §五十六。边界：严格线性口径（reward=8）与 legacy、
  镜像 28301、同副本 18302（5 座塔 -> reward=5）都不一致，未采纳。
- **批次 47 勘误（2026-09-22）**：`1123` 属客户端脚本驱动行（`ProgressAll` + `sensoryArea`），其 reward 投影权威值是 `REWARD/var0=0`；本节元数据里批次 15 记录的“1123 投影 0 -> 1”与 `REWARD/0 -> 1` 自愈边已被推翻，详见 `QE-056`。
- **批次 53 修正（2026-09-22，用户实机口径）**：10528/20528（构筑保护之实体 1 双子）属**末行 = 第 0 行重复行**的例外族，领奖投影是 **legacy 落盘 step 11**，不是任务书末行 12——用户口径“发动 2 次；start 最后是 11，reward 是 11”，与 legacy `changeQuestStep(env, 11, 12, true)`（reward 分支只置状态、不写 nextStep，落盘停在 11）逐字一致；客户端末行 12「和代理人 X 对话」只是第 0 行复述，`last_row_npc_matches_quest=True` 这种“末行点名领奖 NPC”的旁证在本族**不足以推翻 legacy 落盘值**。处置：两侧 reward 投影 12 -> 11、交接删掉 `set-variable var0=12`、无 source 自愈边反转为 `REWARD/var0=12 -> 11`，`var0` 的 `max` 保留 12（旧存档要 pack 得动，QE-047），门禁 `ArchdaevaRewardRowContractTest`。10528/20528 已登记进 `audit_reward_row_vs_client_steps.py` 的 `LEGACY_STEP_EXCEPTION`（QE-054）；同族歧义带另有 34 个任务（10525/20525、10526/20526、10529/20529、10031、11072、11143、11233、13945、14220、15550、15590、17505、18805/28805、18809、19004、20503/20505/20506、21106、21217、21323、23945、24150、24220、25550、25590、27505、29004、30217、30302、30317 等，扫描脚本 `.agents/summary/quest-20528-reward-row/scan_duplicate_row0_family.py`，family size 109）**禁止按任一方向批量改**：10527/20527 的 15 同样是客户端验收值，两值并存；修复后实机复测 PENDING_CLIENT。


---

## [QE-052] 五十、领奖 owner 必须等于客户端任务书领奖行 NPC (REWARD_OWNER_MUST_BE_JOURNAL_REWARD_ROW_NPC)

<!-- pattern-metadata
status: CONFIRMED
scope: typed 任务定义的 completion owner（`npc-complete` 的 NPC 集合）vs 客户端 `Dialogs/*/quest_q<id>.html` 的 quest_summary 领奖行 NPC；重点是迁移把“行 0 交互对象 / 接取 NPC / 任务目标物件”一起生成成领奖 owner 的任务族（4712 的 Dredgion 囚犯、2484 的烽火对象）
first_seen: 2026-09-22
last_verified: 2026-09-22
symptom: REWARD 态从多个实体都能领取（行 0 的交互 NPC、接取 NPC、任务目标物件都会弹奖励窗），领奖行 NPC 反而只是其中一条备用路径；任务书行“向 X 报告”与服务端 completion owner 集合不再一一对应
root_cause: 迁移按“旧 handler 里哪个 NPC 能开对话”生成 `npc-complete`，把只用于写 `var0`/推进状态的实体也登记成完成 owner（4712 囚犯 `defaultCloseDialog(env, 0, 1, true, false)` 后 `onDelete`、2484 烽火对象 `setQuestVarById(0, 1)`），而 legacy 里只有领奖行 NPC 才 `setStatus(REWARD)`/`sendQuestEndDialog`
fix_or_guardrail: 1. 每个任务只保留领奖行 NPC 的 `npc-complete`；行 0 交互对象/接取 NPC/目标物件若在 legacy 里只写 var0 或只推进状态，只能保留 `NPC_REPORT -> reward` 入口路由、不得保留 `npc-complete`；2. 删除与保留必须成对（删 `npc-complete`、留 `dialog NPC_REPORT`），删多留少会让行 0 无法推进；3. 客户端 NPC 表无名的 owner（279042）用同族“行内命名 + completion owner”交叉解键（4713/4714/4715/4716 行内 STR_DIC_N_Henir 且 owner 全是 279042 → Henir = 279042），与 QE-051 的 STR_DIC_LA12 方法同源；4. 领奖态入口页与 `npc-complete` 必须落在同一个 owner 上（只加半边会被 QuestClientContractGateTest 判失败）；5. 门禁 RewardOwnerTrimContractTest（owner 唯一、入口路由保真、npc-complete 计数=1、投影/自愈边/planner、无事务写非领奖行 var0、同族解键）
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/4712.xml（owner 收敛 279042，删 798327/798330 的 npc-complete，保留三条 NPC_REPORT 入口）与 quests/2484.xml（owner 收敛 203331，删 204407/700267）；src/test/java/com/aionemu/gameserver/questEngine/definition/RewardOwnerTrimContractTest.java（7 例，Maven 48 例批次内全绿）；.agents/summary/quest-10527-reward-row/apply_batch16_reward_owner_trim.py（--check 幂等 2/2）、batch16-evidence.tsv、报告 §二十；legacy 证据：`git show '7e9f0316c^'` 中的迁移前 handler `_4712Escape_From_The_Dredgion` 与 `_2484OurManInElysea`（迁移已删除，仅存在于 git 历史）；批次 26 追加：retail-xml-retention.tsv 的 quest 30600 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单） 与 retail-xml-retention.tsv 的 quest 30610 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单） 的 owner 收敛到客户端任务书行点名的 NPC（30600 接取/报告 800325 Hejitor + 简报 800324 Linocus；30610 接取/报告 800327 Astella + 简报 800326 Aluna，静态 spawn 见 src/main/resources/aion/data/static_data/spawns/Npcs/210070000_Cygnea.xml 与 src/main/resources/aion/data/static_data/spawns/Npcs/220080000_Enshar.xml），删除只在 legacy 出现的 205842(Ancanus)/205864(Udvi)；CounterChainBriefingStageContractTest（6 例）、apply_batch26_briefing_and_named_ladders.py、batch26-evidence.tsv、报告 §三十
validation: static（xmllint + quest_definition.xsd 2/2 validates；IDEA lint 0；git diff --check 干净；单任务审计 4712 ROW_BEHIND -> ROW_ALIGNED，全库 ROW_ALIGNED 2645 -> 2646 / ROW_BEHIND 199 -> 198 / MISSING_LAST_ROW 87 -> 86）+ focused-test（10 个测试类 48 例全绿，含本卡门禁 RewardOwnerTrimContractTest 7/7；PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / WHITELIST_VIOLATIONS=0，2026-09-22 12:13）；客户端实机 PENDING_CLIENT
boundaries: 只处理“legacy 只在领奖行 NPC 置 REWARD”的任务；若客户端/OA 证据表明多 NPC 都能领奖（28208/28209 的 Inggness(205320)/Anja(205321) 并存），必须先取客户端 quest_complete/按钮证据再收敛；不适用于 QE-045 锁的 4 个同形任务与 QE-046 的引擎外写入方（其 owner 由基线 TSV 守卫）；删除 owner 必须以保留入口路由为前提，并同步更新证据表与门禁；26838/16838 的 01/02 变体分歧仍未收敛；批次 26 追加：owner 可由客户端任务书行 NPC 与静态 spawn 直接解键时，必须删除只在 legacy 出现、静态 spawn 与实例/AI 都无出场点的 owner（205842/205864 → 800325/800327）
superseded_by: none
see_also: [QE-051], [QE-012], [QE-046]
first_check: REWARD 态被“不是领奖行的 NPC/物件”开了奖励窗，或行 0 交互对象的对话直接结束任务时，先 `grep -c '<npc-complete' quests/<id>.xml` 数 owner、再跑 .agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py，然后用 `git show <迁移前 commit>` 取迁移前 handler 原文确认 legacy 只在哪个 NPC `setStatus(REWARD)`；owner 未唯一时不要先改 reward 投影
keywords: 领奖 owner、completion owner、npc-complete、行 0 交互对象、接取 NPC 兼任领奖、目标物件兼任领奖、4712、279042、Henir、Dredgion 囚犯、2484、203331、Hippolyta、700267、204407、REWARD_OWNER_MUST_BE_JOURNAL_REWARD_ROW_NPC
-->

- **判定规则**：任务在 REWARD 态只允许一个 completion owner，且必须等于客户端 quest_summary 领奖行点名的 NPC；行 0 的交互对象、接取 NPC、任务目标物件只能保留 `NPC_REPORT -> reward` 入口路由，不得保留 `npc-complete`（否则客户端在任务书仍渲染行 0 时就能领奖，且 completion owner 与领奖行不再一一对应）。
- **为什么容易漏**：批次 12 的归属核实只处理了“末行 NPC 与 owner 不同名”，漏掉“多个 owner 并存且行 0 交互对象也是 owner”这一形状；`npc-complete` 重复登记不会冲突报错，静态门禁、目录编译、白名单全绿，只有玩家在行 0 的 NPC/物件上开奖励窗时才会暴露；审计脚本按 reward 投影判定，owner 集合不进判定，所以 ROW_ALIGNED 与 owner 不唯一可以同时成立。
- **代表案例**：4712（Escape From The Dredgion，批次 16 收口：行 1 向 Henir(279042) 报告，囚犯 798327/798330 的 npc-complete 删除、保留 NPC_REPORT；Henir 用 4713/4714/4715/4716 的 owner=279042 交叉解键）与 2484（Our Man In Elysea：legacy 只在 203331 Hippolyta 置 REWARD，204407 接取 NPC 与 700267 烽火对象只写 var0/推进状态 → 删除其 npc-complete、保留三条报告路由）；由 RewardOwnerTrimContractTest（7 例）与 .agents/summary/quest-10527-reward-row/{apply_batch16_reward_owner_trim.py,batch16-evidence.tsv} 锁定，报告 §二十。；批次 20（2026-09-22）：焦树族 23809/13809——三棵树（730969/730970/730971）被登记成 NPC_START + npc-complete，行 0 交互物同时能接取与领奖（客户端行 0 只写“调查并采集”，行 3 才写“向守卫报告”）；两侧 owner 收敛为守卫 802429/802427 各一条 npc-complete，树只保留 USE_OBJECT 页 + SETPROx 推进（并发放本行采集物），由 TreeLadderOwnerTrimContractTest（7 例）锁定；批次 23（2026-09-22）：39713/49713 绿帽团日任的三名可互换支部成员（800936/800937/800938，客户端字符串 STR_DIC_E_LDF5a_Greenhat_BA 的正文点名 LDF5b_Ubarung/Dieroonroon/Argarung_Greenhat）本就是正确 owner，本批只重建 39713 的行阶梯、未收敛 owner；FactionDailyRowLadderContractTest 断言两侧领奖/完成/预览 owner 均保持这三名成员；批次 25（2026-09-22）：18033/28033 同盟任务——旧定义把报告与领奖挂在行 0 前的接取 NPC 801037（LDF5_Village_Guard11_L，Stifas）/801047（LDF5_Village_Guard11_D，Tobald）上，而客户端行 1 点名的是 LDF5b_Demades_E(801281)/LDF5b_Latkel_E(801280)（28033 还并存 801047 的旧 NPC_REPORT 与重复的 k1->reward 边）；两侧报告与完成 owner 收敛到行 1 NPC 各一条 npc-complete。28313 的 Nineveh(804821) 同时是客户端行 0 接取与行 1 报告 NPC，属同形不收敛（门禁对该情形显式跳过）。由 CounterChainTripletContractTest 的 owner 断言与 apply_batch25_counter_chain_triplets.py 锁定，报告 §二十九；30600/30610（批次 26，2026-09-22）：接取/报告 owner 收敛到任务书行点名的 Hejitor 800325 / Astella 800327，legacy 的 205842(Ancanus)/205864(Udvi) 在静态 spawn 与实例/AI 代码里都没有出场点，由 CounterChainBriefingStageContractTest 锁定

---

## [QE-053] 五十一、链式 SECTION 计数族必须逐段 0/1 串行阶梯 (CHAINED_SECTION_COUNTERS_MUST_BE_SERIAL)

<!-- pattern-metadata
status: CONFIRMED
scope: 客户端 quest_monster 用 `SECTION_n<1` + 前置 `SECTION_(n-1)==1` 链式门控的任务族（多维 0/1 计数槽）vs 服务端 `<progress>` 位域、节点与转换建模；含 SECTION_5 简报标志位门控（接取置 1、简报清 0）的两段式对话族（24112/30600/30610）
first_seen: 2026-09-22
last_verified: 2026-09-22
symptom: 击杀进度看起来在涨但任务书行不动、中间行整段消失或直接跳到报告行；自由顺序击杀的怪只计入一部分；按行号推进的任务第 3 只之后再无行能亮；接取后简报标志位不清、或领奖态存档匹配不到 reward 节点而卡在简报/领奖阶段
root_cause: 把客户端的“链式串行 0/1 计数槽”实现成自由顺序的多维组合网格（counter-grid 各维独立 required=1，2^n 个组合节点）或单槽行号/步骤号（var0 写 0..N），计数槽之间因此失去前置语义：跨序号击杀时前置槽仍是 0，行 n 与行 n-1 同时不满足可见条件，任务书中间段没有任何一行成立
fix_or_guardrail: 1. 每只怪只把自己那一槽推到 1（0/1 计数，禁止写行号/步骤号），同步用 PACKET_ONLY；2. `<progress>` 按 SECTION_n = 6n 逐槽声明全部 n 个位域（缺一槽即 COUNTER_CHAIN_GAP），客户端未置位的槽不声明；3. 节点按“已推 N 槽”建串行阶梯（started/K1..Kn/reward/complete），禁止 2^n 组合网格；4. 位宽按旧存档可能写过的最大值保留（本族 6 bit），1-bit 位域会让 VariableIs/VariableAtLeast 被 ProgressLayout.pack 的上界校验拒绝、旧存档永久卡在中间行；5. 迁移旧“步骤号”或旧组合存档要补 enter-world（+ TALK）自愈边，把计数补齐到全 1；6. 领奖行 NPC 与 completion owner 必须一致（QE-052），`fixed-reward-indices` 要覆盖客户端领奖页的全部奖励格（漏 ITEM 格会静默吞道具）；7. 门禁必须同时锁：五槽 6n 对齐、逐槽投影、只推自己那一槽、乱序/回看无计划、镜像同形、自愈边唯一；8. SECTION_5 作为简报标志位的族必须用两段式对话：QUEST_SELECT 只展示 select2 页（保持标志位），页面唯一可见按钮 SETPRO1(10000) 才清标志位并推进（直接挂在 QUEST_SELECT 上会被 QuestClientContractGateTest 判 BUTTON_WITHOUT_ROUTE）
evidence: retail-xml-retention.tsv 的 quest 13918 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）（五槽 6n 位域 + 9 节点串行阶梯 + 旧步骤号自愈边）与 retail-xml-retention.tsv 的 quest 23918 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）（删除 32 节点 counter-grid，改同形阶梯并补 kills 五条）; src/test/java/com/aionemu/gameserver/questEngine/definition/ChainEliteLadderContractTest.java（7 例：逐槽推进、乱序/回看无计划、owner 唯一、奖励格三格、旧存档自愈、23918 不得有步骤号迁移边）; .agents/summary/quest-10527-reward-row/apply_batch21_chain_elite_ladder.py（可重放，--check 幂等）; .agents/summary/quest-10527-reward-row/batch21-evidence.tsv; retail-xml-retention.tsv 的 quest 18033 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单） 与 retail-xml-retention.tsv 的 quest 28033 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）（三槽阶梯 + owner 收敛 + 旧网格 0/1 自愈边）、retail-xml-retention.tsv 的 quest 28313 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）（三槽阶梯 + 步骤号迁移边，保留 27 条按职业展开的 reward 分支）; src/test/java/com/aionemu/gameserver/questEngine/definition/CounterChainTripletContractTest.java（8 例：三槽 6n 对齐、逐槽推进、乱序/回看无计划、owner 收敛、旧存档自愈、职业奖励分支保真）; .agents/summary/quest-10527-reward-row/apply_batch25_counter_chain_triplets.py（可重放，--check 幂等）; .agents/summary/quest-10527-reward-row/batch25-evidence.tsv; .agents/summary/quest-10527-reward-row/2026-09-21-10527-reward-row-and-family-audit.zh-CN.md（§二十五、§二十九）; .agents/summary/quest-15001-multicounter-step/audit_section0_report_row_closure.py（COUNTER_CHAIN 口径，本族两侧 COUNTER_CHAIN_OK；行号口径 audit_reward_row_vs_client_steps.py 已登记 VAR0_FLAG_EXCEPTIONS）；客户端侧：Aion 5.8 解包 Dialogs 的 quest_q13918 / quest_q23918 的 quest_summary 6 行与 quest_monster 的 SECTION_0..4 链式门控（客户端文件外置，不入库）；批次 26 追加：retail-xml-retention.tsv 的 quest 24112 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）（var0 + var5@30 单槽，reward 投影 var0=1;var5=0）与 retail-xml-retention.tsv 的 quest 30600 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）、retail-xml-retention.tsv 的 quest 30610 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）（var0/var1 + var5@30 双层，reward 投影 var0=1;var1=1;var5=0），三任务由 COUNTER_CHAIN_GAP → COUNTER_CHAIN_OK，COUNTER_CHAIN_GAP 归零；CounterChainBriefingStageContractTest（6 例）、apply_batch26_briefing_and_named_ladders.py、batch26-evidence.tsv、报告 §三十
validation: static（xmllint + quest_definition.xsd 2/2 validates；IDEA lint 0；git diff --check 干净）+ audit（两侧 COUNTER_CHAIN_OK、行号口径登记例外、全库快照变化全部归因于口径登记、QuestDialogOrderAudit 无 CLIENT_PAGE_UNREACHED）+ focused-test（21 个测试类 149 例全绿，含本卡门禁 7/7；批次 25 追加 30 个测试类 193 例全绿、本卡门禁 8/8；PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / WHITELIST_VIOLATIONS=0，2026-09-22）；客户端实机 PENDING_CLIENT；批次 26 追加 31 个测试类 199 例全绿（含 CounterChainBriefingStageContractTest 6 例与修复后的 QuestClientContractGateTest）
boundaries: 只适用于客户端 quest_monster 明确写出 `SECTION_n` 链式门控（含前置 `SECTION_(n-1)==1`）的族；同一 SECTION_0 并行门控多行（QE-035 的 SECTION_0==S 形态）与单计数器累加族不适用，必须先用 quest_monster 判语义再套本卡；本族 var0 不是行号，QE-051 的行号口径会把奖励行判成 ROW_BEHIND，禁止据此反向“修正” reward 投影；位宽、SECTION_5 门控与领奖页按钮数逐族不同，禁止跨族套用数值；批次 26 追加：SECTION_5 简报门控族（接取置 1、简报清 0）的 select2 页只有 SETPRO1(10000) 一个可见按钮，QUEST_SELECT 只能“同状态 + SHOW_QUEST_PAGE SELECT2”，推进必须挂 SETPRO1；owner 必须同时有任务书行 NPC 与静态 spawn 证据
superseded_by: none
see_also: [QE-051], [QE-052], [QE-035], [QE-045]
first_check: 玩家报“杀怪进度在涨但任务书行不动/跳到报告行”时，先读该任务客户端 quest_monster 的门控表达式：出现 `SECTION_n<1; SECTION_(n-1)==1` 即命中本族，再数 `<progress>` 是否逐槽声明（缺槽 = COUNTER_CHAIN_GAP）、节点是串行阶梯还是 2^n 组合网格；权威判定用 .agents/summary/quest-15001-multicounter-step/audit_section0_report_row_closure.py，行号口径只看通知口径失败
keywords: 链式计数、SECTION 链、行不前进、任务书跳行、中间行消失、串行阶梯、counter-grid、组合网格、0/1 计数槽、精锐兵、13918、23918、18033、28033、28313、三槽阶梯、单槽步骤号、6n 位域、旧步骤存档、CHAINED_SECTION_COUNTERS_MUST_BE_SERIAL、24112、30600、30610、SECTION_5 简报标志位、两段式对话、SETPRO1、BUTTON_WITHOUT_ROUTE
-->

- **判定规则**：客户端的 `SECTION_n<1` + 前置 `SECTION_(n-1)==1` 是**链式串行**语义，服务端必须用同样串行的 0/1 计数槽表达：每只怪只推自己那一槽、节点按“已推 N 槽”建阶梯；任何“每维独立完成”的集合（组合网格）或“单槽写行号/步骤号”的写法都会让中间段的行同时不满足可见条件。
- **为什么容易漏**：组合网格在服务端自洽（5 维全 1 就进 reward，planner 探针全绿），自由顺序击杀时也会“正好”在最后一只之后收口；任务号口径审计（QE-051）又把 `var0` 当行号，组合网格与步骤号在它眼里常常显示 ROW_ALIGNED/ROW_BEHIND 的混合结果，只有把 quest_monster 的门控表达式和客户端行可见性一起读出来，才会发现“第 3 只之后整段行消失”这一层。缺一槽（如只声明 var0..var3）不会触发任何结构错误，只在客户端表现为那一行永远不亮。
- **代表案例**：23918 / 13918 精锐兵族（批次 21，2026-09-22）：两侧客户端 quest_summary 6 行（行 0..4 各一只特殊精锐兵、行 5 向 Elger/Helgund 报告），quest_monster 用 `SECTION_0<1; SECTION_5==0` .. `SECTION_4<1; SECTION_3==1` 链式门控；23918 原为 32 节点 `<counter-grid>` 自由组合（+ owner 错挂接取 NPC + `fixed-reward-indices="0 1"` 吞掉 ITEM 169405255×6 + 无 kills），13918 原为“单槽 var0=0..5 步骤号 + 缺 var4”（+ owner 同样错挂接取 NPC）；本批两侧同形重建为 var0..var4 各占 6n 的 0/1 串行阶梯、owner 收敛到领奖行 NPC、补回 ITEM 奖励格与 kills，迁移自愈 6 条，由 ChainEliteLadderContractTest（7 例）锁定。三槽族 18033/28033/28313（批次 25，2026-09-22）：客户端 quest_summary 2 行、quest_monster 是 `SECTION_0<1; SECTION_5==0` / `SECTION_1<1` / `SECTION_2<1` 三条链式 0/1 记录（行 0 = 使用 IDF5_TD_Drum 后出现的 230744/230745/230749；28313 是 IDStation 三组强敌、各 3 个难度变体）；18033/28033 原为单维 `<counter-grid>`（required=1，任意一只怪即满足、SECTION_1/SECTION_2 永远为 0），28313 原为“单槽 var0=步骤号 0..3”；本批按 6n 重建三槽阶梯（每只/组怪只推自己那一槽）、28313 补 START var0=2/3 的步骤号迁移边、三侧补 REWARD 旧投影自愈边（18033/28033 用 `variable-sum-below (var0,var1,var2) < 3` 避免重放正规领奖态，28313 用 `var0==3`），由 CounterChainTripletContractTest（8 例）锁定；本批把 section0 审计的 COUNTER_CHAIN_GAP 由 13 收到 3（余 24112/30600/30610），并把 1842/1843/1844/2843/2844/2845 六个大值域计数器登记为 `COUNTER_CHAIN_EXTENDED_EXCEPTION`。；批次 26（2026-09-22）：24112 单槽 + SECTION_5 简报标志位（旧 reward 投影 var0=0 而击杀已写 1，领奖态存档无路由、玩家卡死）、30600/30610 双层 Named/Boss（旧单步骤号 var0=0/1/2、var1 缺失、两条无守卫 SETPRO1 直跳、owner 205842/205864 无出场点），三任务重建为串行阶梯 + 两段式简报对话（QUEST_SELECT 开页、SETPRO1 清标志位）后 COUNTER_CHAIN_GAP 3 → 0，由 CounterChainBriefingStageContractTest（6 例）锁定
- **编号说明**：`QE-049`（接取发放丢失）与 `QE-050`（状态未变化的执行不得下发任务状态更新）已被并行会话占用，故本卡片编号为 `QE-053`。

---

## [QE-054] 五十二、领奖态 packed step 的权威值是 legacy 落盘值 (LEGACY_REWARD_STEP_IS_AUTHORITATIVE)

<!-- pattern-metadata
status: CONFIRMED
scope: typed 任务定义 reward 节点投影（打包成 SM_QUEST_ACTION step 的 var0）vs legacy handler 进入 REWARD 时真正落盘的 step；重点是行号口径审计（QE-051）把 `reward = 末行索引 - 1` 的族误判成 MISSING_LAST_ROW 的场景；批次 28 起还覆盖同族的行阶梯（每一行一个 START/REWARD 状态，推进只由 legacy 事件触发）与旧存档自愈边的旧值选取；批次 29 把该口径推广到全库剩余 81 个 MISSING_LAST_ROW 并给出四族分类（真缺陷 / legacy 落盘=投影 / var0 不承载行号 / 无 legacy 依据）
first_seen: 2026-09-22
last_verified: 2026-09-22
symptom: 审计报 MISSING_LAST_ROW（reward 投影比客户端末行索引小 1），但该任务其实已通过真机验收或已有门禁锁定；机械按末行索引修会把已验收行为改坏（任务书消失或停在旧行）
root_cause: 旧引擎 API 层只有一条写入通道：`changeQuestStep(env, step, nextStep, reward, varNum)` 在 reward=true 时只 `setStatus(REWARD)`、**不写 nextStep**，因此 `changeQuestStep(..., old, new, true)` 落盘 old，而 `useQuestItem(env, item, old, new, true)`（批次 29 逐行核对迁移前 QuestHandler：它最终也委托给同一个 `changeQuestStep`）在旧引擎里落盘的同样是 **old**；只有 `setQuestVar(N)` / `setQuestVarById(0, N)` / `changeQuestStep(..., false)` 之后的 `setStatus(REWARD)` 才把 var0 写成 N。客户端任务书行由 SM_QUEST_ACTION 下发的 step（= reward 节点投影的打包值）驱动，因此权威值是 legacy 的实际落盘值；10527（`useQuestItem(env, item, 14, 15, true)`，reward 投影 15）是该任务自己的客户端 quest_summary 契约 + 用户报障确定的已验收个例，不能当成通用规则去取 newStep
fix_or_guardrail: 1. 判定 MISSING_LAST_ROW 是否为真缺陷前，先从迁移前 handler 提取 reward 写入：`changeQuestStep(env, old, new, true)` 与 `useQuestItem(..., old, new, true)` 都取 **oldStep**（reward 分支只 setStatus、不写 nextStep），`defaultFollowEndEvent` / `defaultCloseDialog` / `checkQuestItems` / `checkItemExistence` / `useQuestObject` 同理取各自的 step 参数、`setQuestVar(N)` / `setQuestVarById(0, N)` / `changeQuestStep(..., false)` 之后的 setStatus 取 N、`defaultOnKillEvent(env, npcId, var, true)` 取匹配值 var；2. XML reward 投影 == legacy 落盘 step 时只能登记例外，禁止按末行索引改；3. XML 投影 != legacy 落盘 step 时才是真缺陷，按 QE-051 模板修投影并补 `REWARD && 旧值 -> reward` 的 enter-world 自愈边；4. 同一系列内两种口径可能并存（10100 的 step=4 与 10101 的末行索引 8），禁止按任务号区间批量套用；5. 门禁必须锁 reward 投影、交接路线不得回写旧行、旧存档自愈、正规态不重放；6. 同族两侧的旧投影值可能不同（16800 旧投影 1、26800 旧投影 3），进入世界的自愈边必须逐侧写各自的旧值，不能共用一条；7. 修投影时同步重建 legacy 的行阶梯：删掉 started 态直跳 reward 的捷径与多余 owner，领奖 owner 收敛到客户端领奖行点名的 NPC，zone/dialog 推进按 legacy 的 step 链逐段落地；8. 批次 29 的全库分类法：把 81 个 MISSING_LAST_ROW 逐个对 legacy 落盘值，分成族 A（投影≠落盘，真缺陷，本批 6 个）、族 B（投影=落盘但<末行索引，登记 LEGACY_STEP_EXCEPTION）、族 C（var0 是计数器/分支标记，登记 COUNTER_SLOT_EXCEPTIONS）、族 D（legacy 无 handler 也无脚本，登记 NO_LEGACY_HANDLER_OBSERVED 待取证）
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/18805.xml 与 src/main/resources/aion/data/static_data/quest_definition/quests/28805.xml（批次 27 修复：reward 投影 1 -> 2、删除 s1 -> reward 的 set-variable var0=1、补 REWARD/var0=1 的 enter-world 自愈边）；src/test/java/com/aionemu/gameserver/questEngine/definition/HousingRecycleRewardRowContractTest.java（5 例）；.agents/summary/quest-10527-reward-row/apply_batch27_housing_reward_row.py（--check 幂等）；.agents/summary/quest-10527-reward-row/batch27-evidence.tsv；.agents/summary/quest-10527-reward-row/2026-09-21-10527-reward-row-and-family-audit.zh-CN.md（§三十一）；口径例外（投影 = legacy 落盘 step，禁止按末行索引改）：src/main/resources/aion/data/static_data/quest_definition/quests/15300.xml 与 src/main/resources/aion/data/static_data/quest_definition/quests/25300.xml（changeQuestStep(env, 13, 14, true) -> step 停 13，2026-09-19/20 用户真机全程验收含领奖，提交 075464ebd 与 src/test/java/com/aionemu/gameserver/questEngine/definition/Quest15300And25300RewardProjectionTest.java 锁定）以及 src/main/resources/aion/data/static_data/quest_definition/quests/10100.xml 与 src/main/resources/aion/data/static_data/quest_definition/quests/20100.xml（useQuestItem(env, item, 4, 4, true) -> step=4，src/test/java/com/aionemu/gameserver/questEngine/definition/Quest10100And20100ItemUseRemovalTest.java 锁定道具消耗）；批次 28（2026-09-22）修复同族真缺陷 retail-xml-retention.tsv 的 quest 16800 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单） 与 retail-xml-retention.tsv 的 quest 26800 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）（legacy `changeQuestStep(env, 2, 3, true)` 落盘 step=2，旧投影分别写成 1 / 3）：两侧重建 unaccepted(0)/started(0)/s1(1)/s2(2)/reward(2)/complete(0)，16800 补 LF_TOWER_SENSORY_AREA_Q16800_210110000 与 IDETERNITY_01_Q16800_301540000 两条 zone 推进并删掉 var1 影片旗标与 6 条直跳领奖捷径，26800 的 s2 -> reward 不再回写 var0=3，两侧各补 `REWARD/旧投影值 -> reward` 的 enter-world 自愈边；门禁 src/test/java/com/aionemu/gameserver/questEngine/definition/ArchivesRewardStepLadderContractTest.java（7 例），同步更新 src/test/java/com/aionemu/gameserver/questEngine/definition/Quest26800ClientDialogAlignmentTest.java（reward 由 3 改为 2）与 src/test/java/com/aionemu/gameserver/questEngine/definition/ClientQuestSectionAlignmentTest.java（16800 退役 var1 后移出 SECTION_LAYOUT_DEBT）；应用脚本 .agents/summary/quest-10527-reward-row/apply_batch28_archives_reward_row.py（--check 幂等）、证据 .agents/summary/quest-10527-reward-row/batch28-evidence.tsv 与报告 §三十二；行号审计脚本补登记 LEGACY_STEP_EXCEPTION={15300,25300,10100,20100}；批次 29（2026-09-22）修复 src/main/resources/aion/data/static_data/quest_definition/quests/1876.xml、src/main/resources/aion/data/static_data/quest_definition/quests/2876.xml、src/main/resources/aion/data/static_data/quest_definition/quests/14123.xml、src/main/resources/aion/data/static_data/quest_definition/quests/2600.xml、retail-xml-retention.tsv 的 quest 11010 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）、src/main/resources/aion/data/static_data/quest_definition/quests/1466.xml（reward 投影分别 1->2、1->2、0->1、0->1、0->3、无->1，逐任务补 REWARD/旧值 -> 新值的 enter-world 自愈边），门禁 src/test/java/com/aionemu/gameserver/questEngine/definition/Batch29RewardRowClosureContractTest.java（4 例），同步更新 src/test/java/com/aionemu/gameserver/questEngine/definition/Quest1466ClientDialogAlignmentTest.java；全库分类明细 .agents/summary/quest-10527-reward-row/batch29-triage.tsv（81 行）、证据 .agents/summary/quest-10527-reward-row/batch29-evidence.tsv、报告 §三十三；应用脚本 .agents/summary/quest-10527-reward-row/apply_batch29_reward_row_closure.py（--check 幂等）；审计脚本补登记 LEGACY_STEP_EXCEPTION（64 项）、COUNTER_SLOT_EXCEPTIONS（2303/50008/51008/11467/1114/80690）、NO_LEGACY_HANDLER_OBSERVED（1005/1479/24120/24123/51010/51020/51022）
validation: static（xmllint + quest_definition.xsd 2/2 validates；apply 脚本 --check 幂等）+ audit（18805/28805 MISSING_LAST_ROW -> ALIGNED；全库 MISSING_LAST_ROW 84 -> 82、ROW_ALIGNED 2661 -> 2663、ROW_STATE_ALIGNED 2433 -> 2435、ROW_WITHOUT_STATE 516 -> 514）+ focused-test（批次 27：32 个测试类 204 例全绿，含 HousingRecycleRewardRowContractTest 5 例；批次 28：27 个测试类 146 例全绿，含新增 ArchivesRewardStepLadderContractTest 7 例；PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0，2026-09-22）+ audit（批次 28：16800 由 MISSING_LAST_ROW、26800 由 STATES_BEYOND_ROWS 双双转为 ALIGNED / ROW_ALIGNED / ROW_STATE_ALIGNED / recovery=True、visible_state_var0=0 1 2；全库 MISSING_LAST_ROW 82 -> 81、ROW_ALIGNED 2663 -> 2665、ROW_STATE_ALIGNED 2435 -> 2437、ROW_WITHOUT_STATE 514 -> 513、STATES_BEYOND_ROWS 2623 -> 2622、ROW_AHEAD 2589 -> 2588、ROW_BEHIND 183 -> 182；section0 residual 837 不变）；批次 29：static（xmllint 6/6 validates；apply 脚本 --check 幂等 6/6）+ audit（MISSING_LAST_ROW 81 -> 77，1466/1876/2876/11010 转 ALIGNED，14123/2600 按 QE-054 保留 legacy 落盘 1 并转入登记；剩余 77 个 100% 落在三组登记集合内；ROW_ALIGNED 2665 -> 2669、ROW_BEHIND 182 -> 179、NO_REWARD_ROW 179 -> 178）+ focused-test（65 个 reward/row/ladder/journal/counter 测试类 299 例，与本次改动相关的全绿，含新增 Batch29RewardRowClosureContractTest 4 例；PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0，2026-09-22）；客户端实机 PENDING_CLIENT
boundaries: 本卡只约束 reward 投影的取值来源，不改变 QE-051 的“行号口径用于发现缺行”作用；无 legacy handler（9 个）或 handler 走 defaultCloseDialog/setStatus 的组合形态（61 个）必须先逐族解出落盘 step 再分类，禁止用启发式批量改数据；15300/25300 的 REWARD/var0=14 恢复边与 10100/20100 的道具消耗属于已验收行为，不得随投影“归一化”一起重构；批次 28 补充：同族两侧旧投影值可能不同（16800=1、26800=3），自愈边只能按侧写各自的旧值；行阶梯必须由 legacy 事件（zone/dialog）驱动，禁止保留 started -> reward 的捷径路线；批次 29 补充：`useQuestItem` 在旧引擎里落盘的也是 old（不是 new），取 newStep 的唯一依据是那个任务自己的客户端 quest_summary 契约与用户报障，不能推广；族 B 的“末行不亮”是 legacy 行为，未复测前不要重开；族 C 的 var0 是计数器/分支标记（2303 的 11..15/21..25、50008/51008 的 sensoryArea 计数、11467 的四投影、1114 的分支领奖行、80690 的槽位 15）；族 D（1005/1479/24120/24123/51010/51020/51022）缺 legacy 依据，需要客户端/数据侧取证后才能判定
superseded_by: none
see_also: [QE-051], [QE-052], [QE-046]
first_check: 审计报 MISSING_LAST_ROW 时，先查该任务是否已有真机验收记录或门禁锁定（Quest15300And25300RewardProjectionTest、Quest10100And20100ItemUseRemovalTest），再从迁移前 handler 提取 reward 落盘 step（changeQuestStep(..., old, new, true) 与 useQuestItem(..., old, new, true) 都取 oldStep；setQuestVar(N)/changeQuestStep(..., false) 之后的 setStatus 取 N；defaultOnKillEvent(..., var, true) 取匹配值 var）；落盘值与 XML 投影一致时登记 LEGACY_STEP_EXCEPTION，不要按末行索引改
keywords: 领奖行、MISSING_LAST_ROW、legacy step、changeQuestStep、useQuestItem、SM_QUEST_ACTION、reward 投影、15300、25300、10100、20100、18805、28805、16800、26800、1876、2876、14123、2600、11010、1466、2303、50008、51008、11467、1114、80690、1005、1479、24120、24123、51010、51020、51022、领奖阶阶梯、Archives、LEGACY_REWARD_STEP_IS_AUTHORITATIVE
-->

- **判定规则**：客户端任务书的当前行由 `SM_QUEST_ACTION` 的 step 决定，而 step 就是 reward/STEP 节点投影打包后的 var0。因此 `reward` 投影的正确值是 **legacy 进入 REWARD 时真正落盘的 step**。旧引擎只有一条写入通道 `changeQuestStep(env, step, nextStep, reward, varNum)`：reward=true 时它只 `setStatus(REWARD)`、**不写 nextStep**，所以 `changeQuestStep(env, old, new, true)` 落盘 old；`useQuestItem(env, item, old, new, true)` / `defaultCloseDialog` / `checkQuestItems` / `checkItemExistence` / `useQuestObject` / `defaultFollowEndEvent` 最终都委托给同一个方法，落盘的也是 **old**（批次 29 逐行核对迁移前 `QuestHandler` 确认）。只有 `setQuestVar(N)` / `setQuestVarById(0, N)` / `changeQuestStep(..., false)` 之后的 `setStatus(REWARD)` 才写 N。行号口径（QE-051）算出的“末行索引”只是待验证的候选，不是权威。
- **为什么容易漏**：`MISSING_LAST_ROW` 的形状（可见状态 0..N-1、客户端 N+1 行）在两种成因下完全一样：真缺陷（legacy step 是 N，XML 写成 N-1）与口径例外（legacy step 本来就是 N-1）。只看审计输出会想当然地按末行索引批量加一；而 15300/25300 这种族已经真机验收过（reward=13 正确），10100/20100 又由道具消耗门禁锁住，批量替换会同时破坏两条已验证路径。
- **代表案例（批次 29，全库 81 个 MISSING_LAST_ROW 的四族分类）**：族 A 只有 6 个真缺陷——1876/2876（`changeQuestStep(env, 1, 2, false)` 写 2 后 `setStatus(REWARD)`，旧投影写成 1）、14123（`defaultOnKillEvent(env, 206360, 0, 1)` 写 1，旧 XML 的 4 条交接又回写 `var0=0`）、2600（legacy 只在 `var0 == 1` 时响应 `SELECT_REWARD`，旧投影 0）、11010（`defaultCloseDialog(env, 2, 3)` 写 3，旧投影 0）、1466（两条进入路径分别落盘 0 与越界的 2，客户端只有 2 行，统一为 1），本批按 legacy 落盘值修投影并各补 `REWARD/旧值 -> 新值` 自愈边；族 B 62 个（`defaultFollowEndEvent(1,1,true,12)`、`defaultCloseDialog(1,1,true,false)`、`checkQuestItems(1,1,true,...)`、`changeQuestStep(13,14,true)`、`checkItemExistence(11,11,true,...)` 等形态）投影已经等于 legacy 落盘值，只是小于末行索引，全部登记为 `LEGACY_STEP_EXCEPTION`；族 C 6 个（2303 的 11..15/21..25 击杀计数、50008/51008 的 sensoryArea 计数、11467 的 reward0..3 四投影、1114 的分支领奖行、80690 的槽位 15）var0 不承载行号；族 D 7 个（1005/1479/24120/24123/51010/51020/51022）在 legacy 侧既无 handler 也无脚本，登记待取证。反例提醒：`useQuestItem` 并不写 newStep——10527 的 reward=15 来自它自己的客户端 16 行契约与用户报障，不是通用规则。
- **代表案例（批次 27/28）**：18805/28805（批次 27，2026-09-22）：客户端 3 行、领奖行索引 2，legacy `_18805Going_Thrifting` / `_28805SomethingOld_SomethingNew` 用回收箱的 `STEP_TO_2 -> defaultCloseDialog(env, 1, 2)` 把 step 推到 2，回到旧货商主人的 `SELECT_REWARD` 才 `changeQuestStep(env, 2, 2, true)`（step 停 2）；旧 XML 投影写成 1 且交接回写 1，本批改为投影 2、删回写、补 `REWARD/var0=1` 自愈边，由 HousingRecycleRewardRowContractTest（5 例）锁定。反例（禁止按末行索引改）：15300/25300 的 `changeQuestStep(env, 13, 14, true)` -> step 停 13（真机已验收，reward=13）、10100/20100 的 `useQuestItem(env, item, 4, 4, true)` -> step=4（reward=4）。批次 28 补充同族第二个案例 16800/26800（客户端都 3 行，legacy `changeQuestStep(env, 2, 3, true)` 落盘 step 2，旧投影 16800=1 / 26800=3）：两侧重建 0/1/2 阶梯（塔感应区 zone 把 0 推到 1、Etezar/Enfitenta 的 SET_SUCCEED 把 1 推到 2、知识书库 zone 只置 REWARD 并播影片 931/932）、删掉 started 态直跳领奖的捷径与多余 owner，并各补 `REWARD/旧值 -> 2` 的自愈边，由 ArchivesRewardStepLadderContractTest（7 例）锁定。
- **编号说明**：`QE-049`/`QE-050` 已被并行会话占用，`QE-051`（领奖行投影）/`QE-052`（owner 收敛）/`QE-053`（链式计数器）分别覆盖相邻主题，本卡片编号为 `QE-054`。

---

## [QE-055] 五十三、过场/影片播放隐藏任务族 (CUTSCENE_HIDDEN_QUESTS)

<!-- pattern-metadata
status: CONFIRMED
scope: 真端标记为“过场/影片播放用”的隐藏任务：空任务书槽位的服务端行为（enter-world 自动接取 + 播放过场 + 过场结束完成）与 SM_PLAY_MOVIE 包类型选择
first_seen: 2026-09-22
last_verified: 2026-09-22
symptom: 客户端任务书对应不上服务端的“缺口定义”——审计报 NO_NODES 或 MISSING_DEFINITION，任务书里却只有空槽；或按任务书行号口径去补定义/行节点，造出永远不显示的节点
root_cause: 真端把这类任务当作“过场/影片播放用隐藏任务”（dev_name 直接写明），任务书没有可见目标行，行为只有“进入指定世界 → 播放过场/影片 → 结束即完成”；迁移时若只看服务端缺口清单，容易误判成“定义缺失需要按行号补齐”，也容易把过场 id 的包类型写错（CutScenes.xml 与 CutSceneMovies.xml 是两张不同的资源表）
fix_or_guardrail: 1. 先读真端 quest.xml 的 dev_name 与客户端 quest_summary 的 <step> 槽：槽内可见文本全空 → 这一族没有可点亮的行，禁止按 QE-051 行号口径补节点；2. 行为按迁移前 handler 的 enter-world/replay/movie-end 三段落成 typed 定义：unaccepted -> started 用 enter-world + world-is + start-eligible（等级/阵营/已完成由引擎元数据门控），started -> started 重播，started -> complete 用 movie-end + complete-quest；3. 过场/影片 id 必须在客户端资源表里查证，包类型由表决定：CutScenes.xml -> CUTSCENE(0)，CutSceneMovies.xml -> CUTSCENE_MOVIE(1)，迁移前 handler 的 SM_PLAY_MOVIE(1, id) 不是类型依据；4. 没有过场 id 或触发世界证据的成员保持隔离（METADATA_ONLY 或不注册），不得凭空补行为
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/18744.xml 与 src/main/resources/aion/data/static_data/quest_definition/quests/28744.xml（批次 30：进入 300610000 自动接取 + 过场 912 -> 完成，三节点无行节点）；src/main/resources/aion/data/static_data/quest_definition/quests/16984.xml 与 src/main/resources/aion/data/static_data/quest_definition/quests/26984.xml（同族仍为 METADATA_ONLY，过场 id/触发世界未取证）；src/test/java/com/aionemu/gameserver/questEngine/definition/CutsceneHiddenQuestFamilyContractTest.java（4 例：自动接取/重播/过场结束完成、不造行不挂对话、METADATA_ONLY 保持、隔离成员未注册未打包）；src/test/java/com/aionemu/gameserver/questEngine/definition/BlankJournalSlotBoundaryContractTest.java（空槽位族边界，批次 24）；src/test/java/com/aionemu/gameserver/questEngine/definition/DisabledClientQuestPlaceholderCatalogTest.java（3959/4963 禁用占位锁定）；.agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py（BLANK_JOURNAL_SLOT_EXCEPTIONS / CLIENT_ONLY_ISOLATED_QUESTS 登记与 [10] 节输出）；.agents/summary/quest-10527-reward-row/2026-09-21-10527-reward-row-and-family-audit.zh-CN.md（§三十四）；迁移前 handler `AbstractRaksangIntro` 与 _18744Avisos_Intelligence/_28744Procuras_Intelligence 仅存在于 origin/history commit 77d99efd6；真端 quest 模板 dev_name（18744/28744 = 타메스 컷신 재생용(천)、16984/26984 = 룬의 안식처 컷신 재생용 히든 퀘스트 (천)、20015 = 5.5 인트로 영상 재생용 히든 퀘스트）；Aion 5.8 客户端 CutScene 资源表的过场 912 = CS_ID_132（其过场文本为拉科兰遗迹开场）
validation: static（xmllint + quest_definition.xsd 2/2 validates，catalog XSD validates，docs/QUEST_CATALOG.zh-CN.md 行刷新幂等）+ audit（全库行号审计 NO_REWARD_ROW 178 -> 180、客户端任务书覆盖 5572 -> 5574，MISSING_LAST_ROW 77 / ROW_ALIGNED 2669 / ROW_BEHIND 179 不变）+ focused-test（批次 30：CutsceneHiddenQuestFamilyContractTest 4 例、引擎组合与目录门禁 90 例全绿；PRODUCTION_COMPILE_OK=6191 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0）；客户端实机 PENDING_CLIENT
boundaries: 只覆盖“任务书无可见行、任务本身只是播放过场/影片”的隐藏任务；正常有行目标的任务仍按 QE-051/QE-054 判；16984/26984 在拿到过场 id 与触发世界之前不得转成 EXECUTABLE；video 型（CutSceneMovies.xml）与 cutscene 型（CutScenes.xml）不得混用包类型；20015 还带客户端 check_user_item 检查页，属于另一个未取证形态，不得按本卡批量实现
superseded_by: none
first_check: 审计报 NO_NODES / MISSING_DEFINITION 且客户端 quest_summary 的 <step> 槽全空时，先查真端 quest.xml 的 dev_name 是否为“컷신/영상 재생용”，再查迁移前 handler 是否有 enter-world + movie 三段行为；有证据才落 typed 定义，并到 CutScenes.xml / CutSceneMovies.xml 里确认过场 id 属于哪张表
keywords: 隐藏任务、过场播放、CutScenes.xml、CutSceneMovies.xml、CS_ID_132、Raksang Ruins、300610000、SM_PLAY_MOVIE、enter-world、movie-end、start-eligible、METADATA_ONLY、18744、28744、16984、26984、20015
-->

- **判定规则**：真端 `quest.xml` 的 `dev_name` 直接标成“컷신 재생용 / 영상 재생용 히든 퀘스트”的任务，客户端任务书只有空 `<step>` 槽（可见文本全空，最多挂 `[%collectitem]` 占位），服务端行为就是“进入指定世界 → 播放过场/影片 → 结束即完成”。这类任务**没有可点亮的行**，QE-051 的行号口径与 QE-054 的落盘 step 口径都不适用；它们出现在 `NO_NODES` / `MISSING_DEFINITION` 桶里是“清单口径问题”，不是“按行号补定义”的工单。
- **为什么容易漏**：`quest_summary` 的行既可能写成 `<p>` 也可能写成 `<step>`，只看 `<p>` 会把这一族误判成“1 行”；包类型也常被迁移前 handler 的 `SM_PLAY_MOVIE(1, id)` 带偏——`CutScenes.xml`（990 条，`.seq`）与 `CutSceneMovies.xml`（38 条，`.bik`）是两张互斥资源表，id 落在哪张表决定包类型是 `CUTSCENE`(0) 还是 `CUTSCENE_MOVIE`(1)。
- **代表案例（批次 30，2026-09-22）**：`18744/28744`（真端 dev_name「타메스 컷신 재생용(천)」）——客户端 4 个空槽、等级 60、`reward_exp1/gold1=0`；迁移前 `AbstractRaksangIntro` 在 world `300610000` 按等级+阵营自动接取并播放过场、已接取存档重播、过场结束置 REWARD 并完成；过场 `912` = `CutScenes.xml` 的 `CS_ID_132`（`cs_id_132.xml` 文本为拉科兰遗迹开场），故 typed 定义用 `CUTSCENE`(0) 而不是 handler 里的类型 1。同族隔离成员：`16984/26984`（METADATA_ONLY，过场 id/触发世界未取证）、`20015`（5.5 开场影片 + `check_user_item` 检查页）、`18706/28706`（客户端 999 级占位）、`3959/4963`（`DisabledClientQuestPlaceholderCatalogTest` 锁定的禁用占位）、`29706`（客户端与真端 `quest.xml` 都不存在）。
- **全库交叉验证方法**：把 `quest_definition/quests/*.xml` 的 `play-movie movie-id` 与两张客户端资源表求交——当前 232 个 `CUTSCENE` 动作 100% 命中 `CutScenes.xml`，8 个 `CUTSCENE_MOVIE` 100% 命中 `CutSceneMovies.xml`（1..37）；任何新过场动作都应先过这道交叉检查再写。
- **编号说明**：`QE-054`（legacy 落盘 step 权威值）与 `QE-051`（领奖行投影）覆盖有行任务，本卡片覆盖无行隐藏任务，三者按“任务书是否有可见目标行”分流。

---

## [QE-056] 五十四、客户端脚本驱动的任务书行不得按行号抬升 (CLIENT_SCRIPTED_JOURNAL_ROW)

<!-- pattern-metadata
status: CONFIRMED
scope: Aion 5.8 客户端 Quest.pak 的 quest_script_monster 表声明 ProgressAll（尤其 sourceType=sensoryArea）的任务：任务书行由客户端脚本自身累计的进度叠加服务端 SECTION_0，服务端 reward 投影不是行号本身
first_seen: 2026-09-22
last_verified: 2026-09-22
symptom: 服务端已下发 SM_QUEST_ACTION 的 REWARD（trace 可见），任务说明却整块空白或停在上一行；按“末行索引 = 领奖行”把 reward 投影抬 1 后任务说明反而整块不亮（1123；同型越界先例 1466）
root_cause: 客户端脚本 ProgressAll + sensoryArea 会自行累计感应区进度并叠加到服务端 SECTION_0，任务说明行 = 客户端自身进度 + SECTION_0；服务端若再按“末行索引”把 var0 抬 1，行号越出 quest_summary 声明的 3×行号槽位（两行任务的 <p visible="[%0]"/[%3]>），两条 <p> 都不亮
fix_or_guardrail: 1. 先查客户端 Quest.pak 的 quest_script_monster 表：声明 ProgressAll（尤其 sensoryArea）的任务，reward 投影必须保持 legacy/用户真机值，禁止按 QE-051 的“末行索引”机械抬升；2. 1123（Where's Tutty?）的权威值是 REWARD/var0=0，抬到 1 会让两行任务书整块空白（批次 15~46 的弯路，2026-09-22 用户真机推翻）；3. 已落盘的错值用反向自愈边修（REWARD/1 -> 0，enter-world 触发）；4. 进入 REWARD 保持 legacy 同序（play-movie + 落状态 + LEVEL_AND_VISIBILITY_REFRESH），movie-end 再补一次同步；5. 新增任务先做三问：quest_script 是否有 ProgressAll、quest_summary 槽位是否 3×行号、迁移前 handler 是否只 setStatus(REWARD) 不写 var0；6. 门禁 src/test/java/com/aionemu/gameserver/questEngine/definition/Batch47ClientScriptedRewardRowContractTest.java，审计登记 .agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py 的 CLIENT_SCRIPTED_ROW_EXCEPTIONS（当前 {1123}）
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/1123.xml（批次 47：reward var0 回到 0、enter-zone 播片 11 + 落 REWARD、movie-end 11 重同步、REWARD/1 -> 0 自愈边）；src/test/java/com/aionemu/gameserver/questEngine/definition/Batch47ClientScriptedRewardRowContractTest.java（5 例）；.agents/summary/quest-10527-reward-row/2026-09-21-10527-reward-row-and-family-audit.zh-CN.md（§五十一）；.agents/summary/quest-10527-reward-row/apply_batch47_client_scripted_reward_row.py（--check 幂等）；.agents/summary/quest-10527-reward-row/batch47-evidence.tsv；用户 2026-09-22 真机判定“看完影片，状态应该是 reward 0”，reward=1 时任务说明整块空白
validation: static（xmllint + quest_definition.xsd + 应用脚本 --check 幂等）；focused-test（2026-09-22 批次 47 共 99 例 Maven 全绿，含 Batch47ClientScriptedRewardRowContractTest 5/5；PRODUCTION_COMPILE_OK=6191 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0）；client（2026-09-22 用户真机确认 1123「可以了」）
boundaries: 只适用于客户端脚本自算进度的任务（ProgressAll，尤其 sensoryArea）；Progress(N) 形态（客户端脚本按 SECTION_n 门控）仍按 QE-051/QE-054 的行号/legacy 口径逐任务取证，不得套用本卡；50008/51008 早已登记在 COUNTER_SLOT_EXCEPTIONS（var0 不承载行号）；1336/1661/1670/16920 是多感应区组合族，禁止按行号补阶梯；ProgressAll 全量 17 个任务（sensoryArea：1123、1336、1661、1670、16920、26920、30708、30758、3959、4077、4963、50008、51008；killedByUser：11302、21303、11467、21467）必须逐族取证，本卡只锁定 1123
superseded_by: none
first_check: 查客户端 Quest.pak 的 quest_script_monster 表里该任务是否有 ProgressAll 行；查 quest_summary 的 <p visible> 槽位是否为 3×行号；查迁移前 handler 进入 REWARD 是否只 setStatus 不写 var0；必要时用 //quest set <id> REWARD 0/1 在实机对比显示后果
keywords: 客户端脚本驱动行、ProgressAll、sensoryArea、任务书整块空白、任务说明不显示、REWARD 越界、1123、Where's Tutty、50008、51008、1336、1661、1670、16920、QE-051 勘误
-->

- **判定规则**：`quest_script_monster` 表里声明 `ProgressAll`（尤其 `sourceType=sensoryArea`）的任务，任务书行由**客户端脚本自己累计**的进度叠加服务端 `SECTION_0`（var0 低 6 位）决定。因此服务端 `reward` 投影不是“客户端末行索引”，而是让“客户端进度 + SECTION_0”落在可见行范围内的基值：1123 必须停在 0（客户端进度 1 + 0 = 行 1），抬到 1 会变成 1 + 1 = 行 2 越界，两条 `<p visible="[%0]"/[%3]">` 全不亮。
- **为什么容易漏**：同一症状（REWARD 已下发但任务书停在上一行或整块空白）在两类任务里形状相似，审计的 `MISSING_LAST_ROW` 只按“服务端可见行 vs 客户端行数”统计，看不出 var0 是行索引还是别的语义。1123 的 quest_summary 只有两行、领奖行恰好是行 1，“末行索引 = 1”的常识与 QE-051 的默认口径都指向 1，很容易把已经正确的 0 又改回 1（批次 15~46 的弯路）；结论必须由客户端脚本声明 + 真机显示共同锁定。
- **代表案例（批次 47，2026-09-22）**：`1123`（Where's Tutty?，天族）客户端脚本是 `1123,ProgressAll,,sensoryArea,,1,LF1_SensoryArea_Q88` —— 客户端自己累计感应区进度；迁移前 `_1123Wheres_Tutty` 在进入感应区时 `playQuestMovie(11)` + `setStatus(REWARD)`，**不写 var0**，legacy 落盘即 0。批次 15 按行号口径把 reward 投影抬到 1，用户真机看到“影片结束后任务说明整块空白”；批次 47 回到 0 后用户复测「1123 可以了」。落点：`1123.xml` 的 reward 投影回 0、enter-zone 保持播片 + 落状态 + 刷新、新增 `movie-end 11` 重同步、自愈边由 `REWARD/0 -> 1` 反向为 `REWARD/1 -> 0`；`RewardRowResidualTwoRowContractTest` 里 1123 退出“末行 = 领奖行”合同（只留 2484），由 `Batch47ClientScriptedRewardRowContractTest`（5 例）接管。
- **同族分类**：ProgressAll 全量 17 个任务按 sourceType 分为 sensoryArea 13 个（1123、1336、1661、1670、16920、26920、30708、30758、3959、4077、4963、50008、51008）与 killedByUser 4 个（11302、21303、11467、21467）。其中 50008/51008 早已按“var0 不承载行号”登记，1336/1661/1670/16920 是多感应区组合（行由客户端进度驱动），3959/4963 是客户端占位/禁用形态，其余成员在本轮尚未逐族取证——本卡只锁定 1123，其余任务不得据本卡批量改投影。
- **编号说明**：本卡是对 `QE-051`（领奖行投影）批次 15 记录中“1123 投影 0 -> 1”的勘误与例外族补充。`QE-051` 仍是通用行号口径，`QE-054` 仍是 legacy 落盘 step 口径，本卡只覆盖“客户端脚本自算进度（ProgressAll）”的分流；`QE-055` 之后编号为 `QE-056`。

---

## [QE-057] 五十五、目录 EXECUTABLE 模式翻转必须伴随节点定义 (CATALOG_EXECUTABLE_MODE_NO_NODES)

<!-- pattern-metadata
status: CONFIRMED
scope: quest_definition_catalog.xml 模式轴与退役/采纳波次（catalog mode flips during adoption/retirement waves）
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 服务端启动即 GameServerError "Can't initialize typed quest engine"，根因 QuestCompilationException NO_NODES: executable definition has no nodes；异常信息不含任务 id（可诊断性缺口）
root_cause: 并行退役波把 15548/25548 的 catalog 条目从 METADATA_ONLY 翻成 EXECUTABLE，但 XML 只有 <metadata> 没有 <nodes>；QuestDefinitionCompiler 对节点为空的可执行定义 fail(NO_NODES)。RetailQuestDriver.overlayProduction 在 manifest 编译之后才运行，救不了这一步；target/classes 陈旧副本会让实机在源码修复前持续崩
fix_or_guardrail: 1. 目录模式翻转必须与节点定义同波落地，零节点 EXECUTABLE 挂进 catalog = 启动必崩；2. 挂 EXECUTABLE 前先对 src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv 复核，REJECTED:*/XML_RETENTION 行（如 15548/25548 的 SEMANTIC_GAP:RETAIL_ACQUIRE_GRANT_UNSUPPORTED）禁止 EXECUTABLE；3. 复现/门禁：python3 .agents/summary/quest-no-nodes-startup-failure/scan_no_nodes.py（遍历全部 EXECUTABLE 条目断言 nodes/node ≥ 1，退出码即红绿）；4. 修复 = 条目回退 METADATA_ONLY 并同步 target/classes 对应文件；5. 被拒行要走"恢复→采纳→退役"闭环（p52b 先例），不得只翻模式
evidence: quest_definition_catalog.xml:553/917（工作区翻转）；quests/15548.xml、25548.xml（14 行仅 metadata）；retail-xml-retention.tsv 两行 XML_RETENTION + SEMANTIC_GAP:RETAIL_ACQUIRE_GRANT_UNSUPPORTED；.agents/summary/scriptdll-quest-driver/reports/2026-09-25-P0c10g-talk-chain-gate-permanent.zh-CN.md 与 INDEX.zh-CN.md 2026-09-26 行（"lane-owned 15548/25548 零节点 XML 挂 catalog EXECUTABLE，阻塞全部 catalog 级测试"）；用户 2026-09-26 01:11 实机启动日志
validation: scan_no_nodes.py 修复前 RED（15548、25548 两 offender）/ 修复后 GREEN；catalog 级 Maven 门禁（ProductionCatalogWhitelistVerificationTest、QuestDefinitionCatalogManifestTest）待授权复跑
boundaries: 只针对 catalog 模式轴；METADATA_ONLY 条目零节点合法（parse 不走结构编译器）；15546 删除（条目 + XML 同波移除）是正确形状，与本坑区别在"只翻模式不删不补"；DataDriven 通道补齐 acquire-grant 后这两行应走退役闭环（删 XML + 删条目）而非长期挂 EXECUTABLE
superseded_by: none
first_check: python3 .agents/summary/quest-no-nodes-startup-failure/scan_no_nodes.py；再取 catalog EXECUTABLE 条目 id 集与 retail-xml-retention.tsv 非 RETAIL_TABLE/ACCEPTED 行求交
keywords: NO_NODES、quest_definition_catalog、EXECUTABLE、METADATA_ONLY、retail-xml-retention、15548、25548、启动崩溃、typed quest engine、retention 分类、模式翻转
-->
- **判定规则**：catalog 条目的 `mode` 声明的是"该 XML 承载可执行定义"，`QuestDefinitionCompiler.compile` 第一步就要求 `nodes` 非空（`NO_NODES`）。零节点的 metadata-only XML 只允许 `METADATA_ONLY`；把模式翻成 `EXECUTABLE` 而不同波补节点，等于把启动崩溃写进目录。
- **为什么容易漏**：① 异常信息不带任务 id，实机日志看不出是谁崩的——直接跑 `scan_no_nodes.py` 重放编译判定即可拿到 offender 清单；② 翻转常来自并行波次的批量脚本，单看 diff 一行像"无害改动"；③ `target/classes` 会保留翻转后的 catalog，源码回退后不清理/不同步，实机下一次启动仍崩（同 25002 假绿家族）。
- **代表案例（2026-09-26）**：15548/25548（"Congratulations!" 75 级 IMPORTANT 双族镜像）被并行 DataDriven 车道翻成 `EXECUTABLE`，但其采纳裁定本就是 `REJECTED:RETAIL_ACQUIRE_GRANT_UNSUPPORTED`（真端表表达不了接取发放路径），retention 清单锁定 `XML_RETENTION`。该崩溃阻塞了全部 catalog 级测试约 20h（P0c-10g/P0c-12/INDEX 多处登记"待 lane 修复"）。修复 = 两行回退 `METADATA_ONLY` + 同步 target/classes；同波 15546 是"条目 + XML 一起删"的正确退役形状，未受影响。

---

## [QE-058] 五十六、进世界接取任务严禁将地图 ID 解析为接取 NPC 模板 (ENTER_WORLD_ACQUIRE_MAP_ID_AS_NPC_FORBIDDEN)

<!-- pattern-metadata
status: CONFIRMED
scope: DataDriven / RetailDriver 进世界（category_acquire=EnterWorld）接取任务与 NPC 模板注册边界
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 服务端启动期 WARN "c.a.g.model.templates.quest.QuestNpc - 任务 Q15596 不存在 NPC 模板 210100000" 及 Q25596 不存在 NPC 模板 220110000；玩家在对应地图无法通过进世界自动接取任务
root_cause: data_driven_quest.xml 中 category_acquire=EnterWorld 时，value0_acquire_ 为地图 ID（如 210100000 阿斯特拉 / 220110000 诺斯佩拉）；RetailDataDrivenDefinitionCompiler 将非 enterarea/none 均视作 npcAcquire，并将纯数字地图 ID 传给 npcIndex.resolveAll 解析为接取 NPC，转入 RetailSimpleHuntDefinitionCompiler 生成了假的 TalkToNpc(worldId) 接取流，导致启动期注册 NPC 模板失败
fix_or_guardrail: 1. 严格以真端和客户端为主，不以旧 XML 或旧 handler 为主；2. category_acquire=EnterWorld 时 acquireParam 为地图 ID，严禁调用 npcIndex 解析 NPC，严禁生成 TalkToNpc 接取流；3. RetailGrantKind 扩展 WORLD 类型，RetailSimpleHuntPlan 支持 worldAcquireId；4. 生成标准的 QuestEvent.EnterWorld 接取边：条件为 StartEligible + WorldIs(worldAcquireId)，目标为初始节点，副作用为 SyncQuestState(VISIBILITY_REFRESH)；5. 修复 isHuntEaMix 逻辑判断（必须同时含有 hunt 和 enterarea，避免将纯 Hunt 任务错误拦截）；6. 门禁回归覆盖：QuestEnterWorldPvpRetailContractTest 与 RetailDataDrivenGateTest
evidence: data_driven_quest.xml (15596 value0_acquire_=210100000, 25596 value0_acquire_=220110000); 用户 2026-09-26 启动日志报障；QuestEnterWorldPvpRetailContractTest 断言 0 个假 NPC 交互且恰好 1 条 EnterWorld 边
validation: focused gates QuestEnterWorldPvpRetailContractTest, RetailDataDrivenGateTest, QuestProductionStartupGateTest 全部 GREEN (10 tests, 0 failures)
boundaries: 仅适用于真端 category_acquire=EnterWorld 的任务（如 15596、25596 及 10010 等混合链）；普通 Talk 接取任务仍由 npcIndex 解析唯一 NPC
superseded_by: none
first_check: 检查任务 category_acquire 是否为 EnterWorld，核对 value0 是否为地图 ID（210100000、220110000、302340000 等），检查生成的 transitions 是否包含 TalkToNpc(worldId)
keywords: EnterWorld、value0_acquire_、地图ID、210100000、220110000、Esterra、Nosra、QuestNpc、不存在NPC模板、RetailGrantKind、isHuntEaMix
-->
- **判定规则**：真端 `data_driven_quest.xml` 中 `category_acquire=EnterWorld` 时，`value0_acquire_` 是玩家进入的目标地图/世界 ID（如 210100000 Esterra，220110000 Nosra，302340000 副本等），绝对不是 NPC 名或 NPC 模板 ID。严禁将其传给 NPC 索引解析，严禁为其生成 `TalkToNpc` 接取流。
- **正确实现**：接取应声明 `QuestEvent.EnterWorld` 事件，前置条件包含 `StartEligible` 和 `WorldIs(worldAcquireId)`，目标节点为未计数初始状态（如 `a0` 或 `started`），副作用包含 `SyncQuestState(VISIBILITY_REFRESH)`。领奖与完成阶段仍按 `reward_npc_name` 对话真实 NPC 推进。
- **关联修复**：在排查整族时发现 `isHuntEaMix` 结尾误写为 `return hunt;` 导致纯 Hunt 任务被误拦截进混合链编译器；修复为 `return hunt && ea;`，使得整族 1036 个退役任务裁定与 IR 指纹完全闭合。

---

## [QE-059] 五十七、编译器级糖元素必须逐类转写进登记表，否则编译静默丢边 (COMPILER_SUGAR_MUST_BE_TRANSCRIBED)

<!-- pattern-metadata
status: CONFIRMED
scope: 真端登记表（quest_client_talk_chain_steps.tsv 等）与族编译器的转写完整性；SimpleTalk 链式行
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 客户端任务书写信页的可见按钮在服务端无路由（零基线客户端契约门新增 BUTTON_WITHOUT_ROUTE 指纹：`24202|started|205150|31|2375|20002`、`80320|started|831427|31|2375|39`），玩家点击无反应
root_cause: 遗留 XML 用 `<npc-item-report source target item-id required failure-page>` 表达 item_check 门（XML 编译器展开为 39/20002 成功/失败对），而链登记表生成器只转写 TALK_TO_NPC 路由与 NPC_START/NPC_COMPLETE 块，没有该元素的记录类型；翻转后链编译只回放登记表记录 → started 态没有门路由
fix_or_guardrail: 1. 转写边界按「XML 编译器会展开的元素」逐类建立映射，新增元素先加记录类型再翻转，未知元素 fail-closed（不是静默跳过）；2. 翻转前对目标行做「真端编译 vs 遗留 XML 编译」逐边差集核对，尤其 XML 糖元素展开出的边；3. 缺边的行回退 XML_RETENTION 并在 retention 证据列写稳定码（`basis=ITEM_CHECK_GATE_NOT_TRANSCRIBED`），通道落地后再入台；4. 通道已落地（P0c-34）：登记表新增 `I` 记录逐字转写 `npc-item-report`（npc/source/target/item_id/required/remove_count/failure_page），链编译器 `itemReportGate` 展开 39/20002 各成功/失败对（成功扣物→target + LEVEL_AND_VISIBILITY_REFRESH + 领奖窗 5，失败留 source，缺省 SELECT6/CLOSE 关窗），生成器对每行做真端双背书 fail-closed（`<item_check>` + `quest.xml collect_item` 符号经 item_name_index 解析到同一 (item_id,count)）；5. 通道验收判据 = 该行真端编译与已删 XML 编译的 IR 差集为空（24202 实测 onlyRetail=0/onlyXml=0），差的只剩通用修复边才可入台
evidence: src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv（24202 = 13 条 R 记录、零 39/20002）；HEAD 版 24202 与 80320 的任务 XML（`<npc-item-report>` 行，git 历史可查）；QuestClientContractGateTest 指纹 52→44；.agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c33-window-open-item-report-rollback.zh-CN.md
validation: focused gates RetailSimpleTalkGateTest 3/3、RetailSimpleTalkChainGateTest 2/2、RetailOwnershipGateTest 4/4、QuestClientContractGateTest（两行指纹 0）、QuestMovieAndDialogLoopRegressionTest 19/19、verify_retirement.py 1392/1392/4832/6224 OK；通道落地后（P0c-34）：1152 入台（verify_retirement.py 1381+4843=6224 OK；Quest1152RetailAlignmentTest 改生产视图 1/1 绿）、漂移门 24202 DIFF:TRANSITION_SET→EQUIVALENT、三行生产视图致命契约指纹 0；24202/80320 仍 KEEP，阻塞于进度行投影轴（见 QE-061）
boundaries: 适用于任何「XML 编译器有糖元素、真端登记表按逐字转写消费」的族（SimpleTalk 链、SimpleHunt 串行链、DataDriven 链）；纯单步行由 build() 直接消费真端表，不经登记表。本卡只覆盖"哪些边要转写"；把 `ITEM_*` 符号解成具体 item id 的**通道优先级**（真端名索引优先，序数通道会置换）见 QE-065
superseded_by: none
first_check: 对目标行比对真端编译与 HEAD 版 XML 编译的边集合差集；再 grep 登记表该任务的记录类型分布（缺 39/20002 即命中本模式）
keywords: npc-item-report、item_check、CHECK_USER_HAS_QUEST_ITEM、20002、39、BUTTON_WITHOUT_ROUTE、登记表转写、糖元素、SimpleTalk 链、24202、80320
-->
- **判定规则**：真端行声明 `item_check` 且有 `collect_item`（门物品）/客户端写信页有检查按钮时，编译定义必须同时有 `39/20002` 两条（成功进领奖窗 + 失败留页）或 `SELECT_QUEST_REWARD(1009)+HasItem` 门；客户端页面可达而边缺失 = 死按钮，属**客户端契约缺口**，不是"可接受降级"。
- **波及口径**：真端 `item_check` 行审计（已驱动行中）16 行缺 CHECK 门——14 行走 1009 门（正常族形）、2 行本模式命中（24202/80320）、2 行潜伏（13809/23809：客户端无检查按钮且 XML 无 npc-item-report，审计不命中，维持观察）。
- **同族观测**：链路径通用「`var0=0` → 领奖行」修复边应在**已有 REWARD 修复边**时不发射（19004 同形双修复边潜伏；判官 `JournalRewardRowRepairContractTest` 钉"恰一条"）。

---

## [QE-060] 五十八、窗口期正式门抓到红先判归属：对 HEAD 版遗留 XML 跑同一断言 (ATTRIBUTE_RED_BY_XML_ERA_REPLAY)

<!-- pattern-metadata
status: CONFIRMED
scope: 多车道共享工作区下的门禁红区归因（客户端契约门、判官断言、指纹/债池 diff）
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 生产视图恢复后正式门跑出"新"失败或新指纹，无法判断是本车道翻转引入、并行车道在飞，还是既有债（判官在更早任务处中止掩蔽）
root_cause: 冻结基线是零基线/截断输出（契约门只印前 30 条指纹；判官循环在首个红处中止），单看"现在红、基线没列"会把掩蔽暴露与真回归混为一谈
fix_or_guardrail: 1. 用 `git show HEAD:<xml>` 导出该任务的遗留 XML，写 `/tmp`（不落仓），编进 `ImmutableQuestCatalog` 跑同一审计/断言：XML 期有 = 既有特性，XML 期无 + 真端期有 = 本车道引入；2. 截断基线用「集合算术」核对（本片：基线 50 = 现 44 + 并行修 6 DD 行；指纹 +2/−2 自洽）；3. 判官中止掩蔽要按循环序逐个复算（80020 修好后暴露 24202）
evidence: .agents/summary/scriptdll-quest-driver/RetailXmlEraContractProbeTest.java.txt（存档探针）；gates/T2-095648.log（184/6F/4E）与 gates/T2-101946.log（回退后 87/3F）；reports/2026-09-26-P0c33-window-open-item-report-rollback.zh-CN.md §2、§4
validation: XML 期审计 0 条 UNROUTED vs 真端期 1 条（两行），据此回退后门禁复绿且集合算术闭合
boundaries: 遗留 XML 仍在 git 历史（未提交删除或已提交）时可用；纯新建任务无 XML 期可比
superseded_by: none
first_check: 先看该任务 retention owner（RETAIL_TABLE=RETAIL 期、XML_RETENTION=XML 期），再对 HEAD 版 XML 重放断言
keywords: 归因、XML 期对拍、零基线门、截断输出、判官中止掩蔽、judge-abort、指纹集合算术、多车道工作区
-->
- **判定规则**：窗口期"新红"三步定性——① 取基线做归一化方法名 diff（去行号/后缀）；② 对可疑任务跑 XML 期对拍（HEAD 版 XML）；③ 用集合算术核对计数变化能否由"并行车道修复 + 本车道增删"闭合。
- **为什么必要**：零基线门（契约门 baseline 为空）使"指纹总数"每个窗口都在动；判官在首个红处中止会让后续任务的问题长期不可见（本片 24202 的两类问题就是被 80020 掩蔽）。

---

## [QE-061] 五十九、真端 collect_progress 是任务书掉落生效行：进度行投影不到位会让门物品绝版 (DROP_STEP_NEEDS_PROGRESS_ROW)

<!-- pattern-metadata
status: CONFIRMED
scope: 真端驱动的采集/交付型任务（SimpleTalk 链行、SimpleCollectItem、DataDriven collect）与运行时掉落门
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 任务在 XML 期可完成，翻转到真端驱动后门物品不再掉落（必须收集的物品只能靠该掉落获得 → 任务不可完成）；或入台前评审发现该风险
root_cause: 真端 quest.xml 的 `collect_progress` = 任务书上掉落生效行（与客户端 quest_summary 里带 `[%collectitem]` 的 step 行号一致），元数据编译器原样映射为 `QuestDrop.collectingStep`；运行时 `QuestService.isQuestDrop` 要求 `status=START && 该任务 var0 == collectingStep`（`collectingStep==0` 表示任意 START 行）。若定义把多行任务书压成单个 `started(var0=0)`（XML 期简化形状），而真端 collect_progress > 0，则没有 `START 且 var0==collectingStep` 的节点 → 掉落永远不触发。retail-metadata-divergences.tsv 早已按 RETAIL_PRIORITY 登记该轴（该项只决定"哪个值生效"，不含可达性）
fix_or_guardrail: 1. 入台前逐行做**掉落可达性检查**：对每个 `QuestDrop`，断言存在 `status=START` 且 `var0==collectingStep` 的节点，或 `collectingStep==0`；不满足即先做进度行投影（按客户端任务书行序补 `v{step}` 行 + 中间对话推进；先例 `RetailSimpleCollectItemDefinitionCompiler` 的 `talksFirst → v{step}` 约定）。2. 客户端证据取 quest_summary 的 step 行（`[%collectitem]` 标记行 = 掉落行），与 `collect_progress` 互证。3. 判官若断言某任务的 `collectingStep==0`（XML 期冻结值），入台前必须按真端值重新裁定并带证据更新，而非保持两套口径
evidence: src/main/resources/aion/data/static_data/quest_retail/quest.xml（24202 collect_progress=2 / 80320=1 / 1152=1）；.agents/summary/scriptdll-quest-driver/p0c34-item-check-gate-channel-decisions.tsv（两行 KEEP_XML 的 PROGRESS_ROW_PROJECTION_PENDING 判据）；.agents/summary/scriptdll-quest-driver/p0c34-item-report-crosscheck.tsv；src/main/java/com/aionemu/gameserver/services/QuestService.java（isQuestDrop 的 START + var0 门）；src/test/resources/quest/retail-metadata-divergences.tsv（24202 drops / 80320 drops·items = RETAIL_PRIORITY）；P0c-35 证据面：.agents/summary/scriptdll-quest-driver/p0c35-progress-row-overrides.tsv + p0c35-progress-row-projection-decisions.tsv（逐行 6 判据）、p0c35_flip_progress_row_rows.py（入台脚本）、reports/2026-09-26-P0c35-progress-row-projection.zh-CN.md §3/§5、src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv（`N started START 2` / `START 1`）、src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMovieAndDialogLoopRegressionTest.java（真端口径断言）
validation: P0c-34 前向验证：24202/80320 门路由已就位、IR 与已删 XML 等价（24202 onlyRetail=0/onlyXml=0），但因现形状无 `START & var0==collect_progress` 节点而维持 XML_RETENTION；1152（无 drop 轴）同批入台后运行时掉落仍正常（真端表无 drop 轴）。P0c-35 落地并验收：生成器四轴 fail-closed（真端 `collect_progress>0` / 裁定值==该值 / ==客户端任务书**末行**行号（非末行报「需中间行合成，另行裁定」）/ 有 drop 或 `<item_check>` 背书）后裁定 `24202 started 0→2`、`80320 started 0→1` 并入台；生产视图致命指纹扫描本片三行（1152/24202/80320）贡献 0（总数 45 归并发轴）；判官 `QuestMovieAndDialogLoopRegressionTest` 由"断言 24202 collectingStep==0"改为"step==2 且存在 `START & var0=2` 节点"（19/19 绿）；契约审计残留非致命 `CLIENT_PAGE_UNREACHED`（24202 页 1352/1353/1693/1694 = Neligor/Cairon 中间对话行、80320 页 1352）即本模式的"中间行未合成"未竟面
boundaries: 只影响声明了 `QuestDrop` 的行；`collectingStep==0` 或客户端任务书只有单行的行不受影响；进度行投影的形状权威是客户端任务书行序 + 真端 collect_progress，XML 只是对照。**投影只能落在任务书末行**——中间行（多段对话行）需要「中间对话行合成」通道（客户端页文本 + 真端 `talk_npc1`/`talk_npc2` + `Dialogs/QUEST_Q<id>.html` 的 dic 名），未建模前把中间行的行号写进裁定表会被生成器 fail-closed 拒绝。**P0c-36 已建该通道**（见 QE-063）：中间行建为阶梯节点 `s1(1)..sK(K)`，`K` 仍等于 `collect_progress`（24202 K=2 SETPRO / 80320 K=1 TALK 已落地，中间页全部转为 `PAGE_ACTION_MATCHED`）
superseded_by: none
first_check: 取该任务的 drop 与 collect_progress → 在定义里找 `status=START` 且 `var0==collectingStep` 的节点；找不到即命中本模式（再看客户端 quest_summary 该行是否带 `[%collectitem]` 复核）
keywords: collect_progress、collectingStep、isQuestDrop、掉落生效行、collectitem、进度行投影、v{step}、门物品绝版、24202、80320、RETAIL_PRIORITY
-->
- **判定规则**：真端 collect_progress > 0 时，任务书上带 `[%collectitem]` 的那一行才是掉落生效行；定义必须有该行对应的 START 态投影（`var0 == collect_progress`），否则掉落永不触发。
- **同族观测量**：`retail-metadata-divergences.tsv` 的 `drops RETAIL_PRIORITY` 只表示"取真端值"，**不等于**该值可达；两者要分开判定（P0c-34 的 24202/80320 就是"值已裁定、形状未跟上"）。
- **落地与残留（P0c-35）**：投影通道已在 SimpleTalk 链登记表落地（`N started START <collect_progress>` + 生成器四轴 fail-closed），24202/80320 入台后本片三行致命契约指纹为 0。残留是**中间对话行未合成**：24202 的 Neligor/Cairon 中间页（客户端页 1352/1353、1693/1694）与 80320 的页 1352 仍报非致命 `CLIENT_PAGE_UNREACHED`；这些行一旦要建模，需要「中间对话行合成」通道（页文本 + 真端 `talk_npc1`/`talk_npc2` + HTML dic 名），在此之前生成器只接受末行投影。


---

## [QE-062] 六十、退役（删除）会让记忆库证据悬空：结构门禁的修复配方 (RETIRED_ARTIFACT_EVIDENCE_MUST_BE_REPOINTED)

<!-- pattern-metadata
status: CONFIRMED
scope: `.agents/memory-bank/patterns/*.md` 的证据字段（root_cause/evidence/validation 等）；凡引用了已退役（已删除）产物的卡片（quest XML、旧表、旧 handler）
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: `python3 -B .agents/memory-bank/verify_memory_bank.py` 报 `MEMORY_BANK_VERIFY_FAILED STEPS=structure`，逐行 `- Pattern QE-0NN evidence references missing path .../quests/NNNNN.xml`（derived-index 与 freshness 都 OK）；卡片内容本身是对的，只是证据指到了已删除的 XML
root_cause: `check_memory_bank.py` 用 `EVIDENCE_REFERENCE` 正则从证据字段抓"路径形状"的 token（`目录/文件.ext` 或 `文件.ext`，扩展名域 = java/xml/csv/md/json/properties/xsd/yml/yaml），再用 `EvidenceResolver` 在 `src/ docs/ .agents/ scripts/` 下做精确+后缀匹配；"退役 = 删除"把 quest XML 删掉后，历史卡片里的那些路径就永久悬空。`target/`、`aion/`、`log/` 前缀被显式豁免；`.tsv` 不在扩展名域内，所以写 `retail-xml-retention.tsv` 不会被抓
fix_or_guardrail: 1. 批量退役收口后跑一次 `verify_memory_bank.py`，把悬空清单当退役尾巴处理（P0c-35 实测 52 处 / 18 卡）。2. 把每处已删 XML 路径替换为**持久指针**：`retail-xml-retention.tsv 的 quest <id> 行（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）`。3. 替换必须**整串精确匹配**：若先替换短形式 `quests/NNNN.xml`，会把同一文件里长路径 `.../quest_definition/quests/NNNN.xml` 的尾段一起吃掉，留下 `.../quest_definition/retail-xml-retention.tsv` 这种半截假路径（本片踩中 2 处，需二次修）。4. 修复脚本按 card 维度 read-modify-write 并带竞态重试（记忆库是并发 lane 共享文件）。5. 收口跑 `sync_memory_bank.py` → `verify_memory_bank.py`，必须 `MEMORY_BANK_VERIFY_OK STEPS=3`
evidence: .agents/summary/scriptdll-quest-driver/mb_dangling_evidence_fix.py（可重放，--dry-run/--apply；含 retention owner 校验：非 RETAIL_TABLE 的悬空引用列 UNEXPECTED 而不改）；.agents/memory-bank/check_memory_bank.py（EVIDENCE_REFERENCE / EVIDENCE_ENV_ALLOWLIST / EvidenceResolver.SEARCH_ROOTS）；.agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c35-progress-row-projection.zh-CN.md
validation: P0c-35 实测：52 处悬空（18 卡，全部命中 RETAIL_TABLE 退役行，UNEXPECTED 0）修复后结构门禁由 FAILED 变 `MEMORY_BANK_VERIFY_OK STEPS=3`；PATTERNS=107 不变（无卡片丢失，证明修复未覆盖并发 lane 的追加写）
boundaries: 只处理"引用了已删除产物"的悬空；若引用对象仍在树内却解析不到、或该 quest 并未退役，那是真缺口而非本模式（脚本按 retention owner 分流上报，不静默改）
superseded_by: none
first_check: `verify_memory_bank.py` 取悬空清单 → 逐条核对 quest 是否为 RETAIL_TABLE 退役 → `mb_dangling_evidence_fix.py --dry-run` 预览 → `--apply` → sync + verify
keywords: 记忆库、悬空证据、references missing path、退役、retail-xml-retention、结构门禁、MEMORY_BANK_VERIFY_FAILED、EvidenceResolver
-->
- **判定规则**：结构门禁的"references missing path"几乎总是退役留下的尾巴，不是卡片内容错；先按 owner 清单确认该产物确实已退役，再改指向而不改结论。
- **为什么用 `retail-xml-retention.tsv` 形态**：它既指出持久证据（清单 + git 历史），又因 `.tsv` 不在扩展名域而不会被门禁抓成路径——用"半截路径 + 说明文字"是最容易再次踩坑的写法。


---

## [QE-063] 六十一、多段对话行必须建成 var0 推进阶梯：中间对话页不是死页而是 sK (TALK_LADDER_MIDDLE_ROWS)

<!-- pattern-metadata
status: CONFIRMED
scope: 真端驱动的多段对话行（真端 `Quest_SimpleTalk.xml` 的 `talk_npc1`/`talk_npc2`/`talk_npc3`、DataDriven talk 链）与"客户端任务书行数 > 定义 START 节点数"的形状缺口
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 客户端任务书有 N 段对话（`quest_summary` 的 `<steps>` 多行），生产视图里却只有单个 `started(var0=0)`；客户端契约审计对中间对话页报非致命 `CLIENT_PAGE_UNREACHED`（玩家看不到也点不到中间 NPC 的页），或者为掩盖它把行号直接投影到末行，导致玩家跳过中间对话 NPC 且掉落行错位
root_cause: XML 期简化形状把多段对话压成单个 `started(var0=0)`，中间页因此没有承接节点；而真端行其实逐段声明了对话 NPC（`talk_npc1..3`），客户端 `quest_summary` 第 k 行文本点名同一个 NPC（`STR_DIC_N_<talk_npcK>` 的 dic 名）、末行带 `[%collectitem]`，真端 `collect_progress` 给出掉落生效行号 K。三者对齐时，中间行必须是 `var0 = 1..K` 的阶梯节点，否则中间页不可达（也拿不到"该段对话推进"的语义）
fix_or_guardrail: 1. 阶梯形状 = `started(START,var0=0) → s1(var0=1) → … → sK(var0=K)`，`K = 真端 collect_progress`（QE-061 的掉落行约束落在 `sK`，两者是同一条轴的两种表现）；2. 收集/交付段（item_check 门 I、`NPC_REPORT` 报告流 B、`QUEST_SELECT`/`SET_SUCCEED`/`SELECT_QUEST_REWARD`/`USE_OBJECT` 的 R）的 source 与 **target 一并**迁到 `sK`（只改 source 会造出 `sK → started` 回退边）；3. 推进方式**由客户端页链按钮证据决定**，不许猜：阶段末页有 `HACTION_SETPRO{k}`（`QuestDialogAction.SETPRO1`/`SETPRO2`）→ `SETPRO` 模式，该按钮是唯一推进点；阶段页链**没有**推进按钮（例如只有 `HACTION_FINISH_DIALOG`）→ `TALK` 模式，该次对话的 `QUEST_SELECT` 事件本身推进 var0；4. 阶段页链遍历只跟随"目标页名以 `SELECT` 开头"的续页动作——页 id 与按钮动作共号空间（`1008` 既是 `HACTION_FINISH_DIALOG` 也是 `quest_complete` 页 id），按 id 跟会走错；5. 生成器必须 fail-closed：talk 数 == K、每个 talk 名唯一解析为 NPC id、`summary_rows == K+1`、`collect_progress == K`、mode ∈ {SETPRO,TALK}、reward NPC 唯一解析、`started` 存在且 `var0 ∈ {0,K}`；任一不满足即报错不产出（禁止半截阶梯）
evidence: .agents/summary/scriptdll-quest-driver/p0c36-talk-ladder-decisions.tsv（24202 K=2 SETPRO / 80320 K=1 TALK 逐行三轴依据）; .agents/summary/scriptdll-quest-driver/p0c36-talk-ladder-census.tsv; .agents/summary/scriptdll-quest-driver/p0c36_ladder_gap_census.py; .agents/summary/scriptdll-quest-driver/build_quest_client_talk_chain_steps.py（阶梯通道 + fail-closed 校验 + 段迁移 + 阶段入口路由）; src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv（24202 = 20 条 R + P/N×6/I/B/E；80320 = 18 条 R + P/N×5/I）; src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv（24202 bac4158d… / 80320 d825451f…）; .agents/summary/scriptdll-quest-driver/P0c36TalkLadderProbeTest.java.txt + .agents/summary/scriptdll-quest-driver/p0c36-probe-out.txt; .agents/summary/scriptdll-quest-driver/P0c36ContractScanProbeTest.java.txt + .agents/summary/scriptdll-quest-driver/p0c36-scan-out.txt; .agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c36-talk-ladder.zh-CN.md; src/test/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkChainGateTest.java; src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDialogAction.java（SETPRO1(10000)/SETPRO2(10001)）与 src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDialogPage.java（SELECT2(1352)/SELECT3(1693)/SELECT5(2375)）; docs/quest/client-dialog-mapping/quest-dialog-pages.csv 与 docs/quest/client-dialog-mapping/quest-dialog-action-details.csv（页 id ↔ 按钮 href 解包证据）
validation: P0c-36 落地并验收：生成器回显 `TALK-LADDER 24202：K=2 mode=SETPRO talk=[205159, 205198]`、`TALK-LADDER 80320：K=1 mode=TALK talk=[831427]`，登记表 1366 路由重建（src↔target md5 07a3adf9…）；探针实测 24202 `transitions=43`、节点 `started(0)/s1(1)/s2(2)/reward(2)`、掉落 `collectingStep=2`，80320 `transitions=38`、`started(0)/s1(1)/reward(1)`、`collectingStep=1`；契约扫描 `PRODUCTION-FATAL total=44` 且所列 36 行与改动前逐字相同（本片 0 命中），中间页全部由 `CLIENT_PAGE_UNREACHED` 转 `PAGE_ACTION_MATCHED`（24202 页 1352→1353→10000、1693→1694→10001、2375→20002；80320 页 1352→1008、2375→39、2716→1008）；链指纹门 2/2（274 行恰 2 行演进）、族门 3/3（漂移登记无需改：该分类对的是历史 XML，阶梯是新增节点仍 `DIFF:NODE_PROJECTION`）、审计 17/17、`QuestMovieAndDialogLoopRegressionTest` 19/19；`verify_retirement.py` = `catalog=1351 directory=1351 retired=4873 sum=6224 — OK`。本片 0 退役 0 采纳（纯形状重建），运行时/客户端目检 PENDING（需授权启动服务器）
boundaries: 只适用于"三轴对齐"的行——`quest_summary` 步数 == talk 数 + 1、第 k 行文本点名 `STR_DIC_N_<talk_npcK>`、`collect_progress == K`；族内普查 324 行里步名对齐 273（已建模 232 / 缺阶梯 41），**不对齐的 51 行必须先补步名证据再判**，不得按行号硬套。`talk_npc3` 存在（族内 68 行）时 K 的取值同样以 `collect_progress` 为准。TALK 模式只在页链确无推进按钮时成立；有 `SETPRO{k}` 按钮却挂 `QUEST_SELECT` 推进会被客户端契约门判 `BUTTON_WITHOUT_ROUTE`。阶梯增删会改变定义 IR → 链指纹必须按"恰好哪几行演进"核对后外科重冻（禁止整表重写覆盖并发 lane 的行）。**P0c-37 补三条**：①候选行必须先用 `QuestDialogOrderAudit` 做可达性过滤（节点投影轴会把"var0 旗标形"误报成缺阶梯，见 QE-064），只有客户端任务页确实 `CLIENT_PAGE_UNREACHED` 才动手；②生成器泛化后必须证明**对既有已阶梯行无损**——把改动前后的登记行逐行字节比较（P0c-37 实测 24202/80320 `IDENTICAL=True`）；③交付段迁移判据是"source=`started` 且 npc=交付 NPC"，不是动作白名单（24123 的门路由是 `CHECK_USER_HAS_QUEST_ITEM`，白名单会漏迁）。另：`collect_progress==0` 的行同样可以套阶梯——运行时 `isQuestDrop` 只在 `collectingStep != 0` 时比较 var0，`0` 表示掉落门不限行
superseded_by: none
first_check: 客户端 `quest_summary` 的 `<steps>` 行数 vs 生产视图 START 态节点数（不相等即命中）；再看该行真端 `talk_npc1..3` 与 `collect_progress`，并读 `Dialogs/QUEST_Q<id>.html` 的阶段页按钮是否为 `HACTION_SETPRO{k}`
keywords: 中间对话行、多段对话、talk_npc1、talk_npc2、talk_npc3、CLIENT_PAGE_UNREACHED、对话阶梯、s1..sK、SETPRO1、SETPRO2、HACTION_SETPRO、QUEST_SELECT 推进、collect_progress、steps 步名、STR_DIC_N_、24202、80320、Quest_SimpleTalk
-->
- **判定规则**：客户端任务书段数 > 定义 START 节点数 = 缺阶梯；`K` 由真端 `collect_progress` 定，推进方式（SETPRO / TALK）由**阶段页按钮**定，两者都要证据。
- **为什么不能压成一行**：压行会让中间对话页成为死页（`CLIENT_PAGE_UNREACHED`），而把行号硬投影到末行会让玩家跳过中间 NPC、并把掉落行与对话段错位（与 QE-061 是同一条轴）。


---

## [QE-064] 六十二、形状普查必须用客户端页可达性对拍：节点投影轴看不见 transition 级阶段编码 (SHAPE_CENSUS_NEEDS_CLIENT_PAGE_REACHABILITY)

<!-- pattern-metadata
status: CONFIRMED
scope: 真端驱动任务的"形状缺口"普查（缺阶梯/缺节点/缺投影）与批次候选生成；凡以"任务书行 ↔ 节点投影"为判据的普查
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 普查报出一批"缺节点/缺阶梯"的行，按它改形状却发现这些行的客户端页本来就可达、阶段语义也已在生效——改动等于重写一个工作正常的形状，还会与既有的 var0 条件互相冲突
root_cause: 节点投影轴（"任务书每行都要有对应的 START/REWARD 节点投影"）只看**节点**，看不见把阶段编码在 **transition 级**的行：这类行用单个 `started` 节点 + var0 旗标 + 条件化路由表达阶段（推进边的 `cond VAR_IS:var0=k-1` + `act SET_VAR:var0=k`，交付边要求 `VAR_IS:var0=k`）。两者在客户端页可达性上等价，却会被"节点轴"判成缺口，于是普查把"形状不同"误报成"形状缺失"
fix_or_guardrail: 1. 任何形状普查都必须跑**第二道过滤器**：用 `QuestDialogOrderAudit` 对该行做客户端页可达性对拍——只有"客户端点名且是任务页（`SELECT*` 前缀）的页在编译后 IR 里 `CLIENT_PAGE_UNREACHED`"才算真缺口；中间页已 `PAGE_ACTION_MATCHED` 的行属误报，**不得重写**。2. 误报判据要落表留痕（本例 `p0c37-gap-partition.tsv`：41 行 = 真缺口 19 + 误报 22），并把判据写进候选生成脚本（读分区表过滤），不要改历史普查脚本（口径要可追溯）。3. 判定"真缺口"后仍要逐轴检查是否可以安全重写：目标行的推进/交付条件若已引用 var0，套节点阶梯必须与这些条件对拍，否则先做条件轴裁定。4. 交付段迁移规则不能只列动作白名单——门路由的动作名逐族不同（`CHECK_USER_HAS_QUEST_ITEM`/`_SIMPLE`/`ITEM_USE`…），白名单会静默漏迁；判据应是"source=阶段起点 且 npc=交付 NPC"
evidence: .agents/summary/scriptdll-quest-driver/p0c37-gap-partition.tsv（41 行分区，探针 `QuestDialogOrderAudit` 实测）; .agents/summary/scriptdll-quest-driver/p0c37_ladder_batch_derive.py（读分区表过滤 + 自校验）; .agents/summary/scriptdll-quest-driver/p0c37-blocked-rows.tsv（误报行逐条带判据）; .agents/summary/scriptdll-quest-driver/p0c37-gap-partition.txt（探针原始输出）; .agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c37-ladder-batch-and-census-correction.zh-CN.md（§1/§3③）; src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv（误报样例行 35010 R12/R16 的 cond/act 逐字）; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestDialogOrderAudit.java
validation: P0c-37 实测：P0c-36 普查的 39 行余量经可达性对拍后**只有 19 行是真缺口**、22 行是误报（含 5 行 35010 系旗标形与 4 行 reward 轴行）；误报行不做任何改动（登记表逐字节未变）。真缺口里仅 1479 具备落地条件（已驱动 + 三轴齐备），阶梯后其 4 个中间页由 `CLIENT_PAGE_UNREACHED` 全部转 `PAGE_ACTION_MATCHED`，链门 2/2、族门 3/3、审计 17/17，致命契约集与改动前逐字相同
boundaries: 反过来的情形同样成立——客户端页可达 ≠ 形状正确（掉落行投影 QE-061、奖励行投影 QE-051 都是"页可达但语义错"的轴），所以可达性对拍是**过滤器（否证）**不是**判据（确认）**：它只能剔除误报，不能替代逐轴证据。误报判定只对"阶段由 transition 级条件承载且条件与节点投影自洽"的行成立；一旦要改该行的任何一轴（奖励行/掉落行/NPC），必须把 var0 条件与节点投影一并重裁
superseded_by: none
first_check: 普查报缺口时，先跑 `QuestDialogOrderAudit` 取该任务的 `CLIENT_PAGE_UNREACHED` 行；若没有（只见非任务页或全 matched）→ 误报，去看该任务的 SETPRO/推进边是否已带 `VAR_IS:var0=k` + `SET_VAR:var0=k+1`
keywords: 形状普查、误报、节点投影轴、transition 级阶段编码、var0 旗标、CLIENT_PAGE_UNREACHED、PAGE_ACTION_MATCHED、可达性对拍、QuestDialogOrderAudit、缺阶梯、动作白名单漏迁、交付段迁移、1479、35010
-->
- **判定规则**：普查给候选，可达性对拍做过滤，逐轴证据做确认——三步缺一不可；只有"客户端任务页确实不可达"才值得改形状。
- **为什么容易误报**：`var0` 既能当"阶梯节点号"也能当"阶段旗标"，两种编码在客户端视角等价；只看节点投影的脚本必然把后者当成缺失。

## [QE-065] 六十三、`ITEM_` 符号解析必须走真端名索引：`quest_data` 序数通道会静默置换物品 (ITEM_SYMBOL_CHANNEL_BY_NAME_INDEX)

<!-- pattern-metadata
status: CONFIRMED
scope: 把 `ITEM_*` 符号解析成具体 item id 的任何路径（canonical 合成的接取发放/阶段 give·remove 动作串、登记表 `I` 记录的 item_check 门、链编译器消费的动作串）
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 两种表象，后一种更隐蔽——①一批行 canonical 合成直接失败（登记表无记录 ⇒ 阶梯/路由通道不可用、这行被登记成 `ADOPTION_BLOCKED`）；②**合成成功的行物品被错位置换**：接取时给出的是末期物品，阶段物品倒着走（生产里实链为 `074→071→072→073` 而不是 `071→072→073→074`）
root_cause: 符号 → id 存在**两个通道**：真端 `item_name_index.tsv`（按名索引：符号去 `ITEM_` 前缀 + 小写 → id）与历史 `quest_data.xml` 的 `<quest_work_items>` **序数配位**（第 i 个符号配第 i 个 work_item）。任务书里 doc/quest 混合集合的两侧顺序并不一致，序数通道就把 `A/B/C/D` 配成 `D/A/B/C`；而该行在 quest_data 里**没有 work_items** 时序数通道直接无解 ⇒ 合成失败。两条症状同一根因
fix_or_guardrail: 1. 解析顺序固定为**真端 item 名索引优先**（符号去 `ITEM_` 前缀 + 小写 → id）→ 序数通道仅兜底 → 两通道皆无解 **fail-closed**（禁止猜）；2. 两通道都有值时**真端索引胜**，且逐条留痕（`quest_id/symbol/retail_index_id/work_items_id/verdict`，verdict ∈ {MATCH, INDEX_ONLY, DIVERGED_RETAIL_WINS, POSITIONAL_FALLBACK}），分歧不得静默；3. 落地后必须用**生产视图**复核物品链方向（应呈 `A→B→C→D` 严格递进：接取给首件、每阶段 `GIVE` 下一件并 `REMOVE` 上一件），登记表 `R` 行的 `GIVE_ITEM/REMOVE_ITEM` 串与编译后 `GiveItem/RemoveItem` 两侧互证；4. 通道变化会改 `RETAIL_TABLE` 行的 IR ⇒ 链指纹必须 dump 对拍（`changed/added/removed`）后**外科重冻**；5. 禁止用"该符号在 quest_data 里能查到"作为验收依据——那正是置换的来源
evidence: .agents/summary/scriptdll-quest-driver/build_quest_client_talk_chain_steps.py（`synthesize_canonical` 内 `item_id(sym)` 双通道 + `item_channel` 留痕 + 守卫放宽）; .agents/summary/scriptdll-quest-driver/p0c38-canonical-item-channel.tsv（132 条留痕：MATCH 111 / INDEX_ONLY 10 / DIVERGED_RETAIL_WINS 10，分歧逐条给出两侧取值）; .agents/summary/scriptdll-quest-driver/p0c38-generate.log（`CANONICAL-INDEX` ×10 + `canonical 合成：111 行；缺口（KEEP）：0 行 []` + 重跑幂等）; .agents/summary/scriptdll-quest-driver/P0c38ChannelProbeTest.java.txt + p0c38-probe.txt（生产视图 give/remove 逐字）; src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv（2458 R seq5、4905 R seq5/8/11、4906 R seq5/8/11 的动作串）; src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv（changed=[2458,4905,4906]）; .agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c38-canonical-item-channel.zh-CN.md
validation: P0c-38 实测：canonical 合成 **101→111 行、KEEP 缺口 0**；10 行 `INDEX_ONLY` 首次合成（18035/18807/18809/21070/21460/24120/28035/28807/29070/29071，owner 仍 `XML_RETENTION` ⇒ 生产惰性）；3 行 `DIVERGED_RETAIL_WINS`（2458/4905/4906，`RETAIL_TABLE`）物品链由倒走改正为严格递进（2458 `194→195`、4905 `071→072→073→074`、4906 `075→076→077→078`）；指纹 dump 对拍 changed=[2458,4905,4906] / added=[] / removed=[]（275 行）；链门 2/2、族门 3/3、审计 17/17、T2（14 id）67 例 5 红全归因他车道、verify_retirement 1351/1351/4873/6224 OK；生成器重跑幂等
boundaries: 本卡只约束「符号 → id」这一步；符号的**存在与语义**（哪些符号属接取发放、哪些属阶段发放、交付是否豁免）仍由真端行结构 + 客户端页链决定（见 QE-063）。序数通道并非"坏数据"——只有当符号集合与 work_items 集合**同集且同序**时才等价，两者不一致或 work_items 缺失时必须走名索引。对 owner=`XML_RETENTION` 的行，登记表里的合成记录是**惰性**的（生产仍由 XML 拥有，链门只进 KEEP 桶、指纹表里没有这些行），采纳需另做 flip（owner + 目录 + XML 删除）后才生效
superseded_by: none
first_check: 合成失败报"符号两通道均无解"、或物品链方向可疑（接取即给末期物品）时，把该符号同时 `grep` 两侧取值——`item_name_index.tsv`（去 `ITEM_` 前缀 + 小写）与 `quest_data.xml` 的 `<quest_work_items>` 序号位——不一致即命中本模式，取真端索引值并留痕
keywords: item 符号、ITEM_、item_name_index、quest_work_items、序数通道、置换、DIVERGED_RETAIL_WINS、INDEX_ONLY、POSITIONAL_FALLBACK、GIVE_ITEM、REMOVE_ITEM、canonical 合成、2458、4905、4906、18035、29070
-->
- **两个通道**：真端名索引按**名字**匹配（去 `ITEM_` 前缀 + 小写），历史 `quest_data.xml` 按**序号**配位；只有两侧同集同序才等价。
- **为什么序数通道会静默出错**：4 个符号配 4 个 work_items 时它是"合法"的映射，不报错、不告警，只是把 `A/B/C/D` 配成 `D/A/B/C`——表现的物品链倒走只能靠生产视图复核发现。
- **落地判据**：留痕表逐条给两侧取值，分歧一律取真端；`RETAIL_TABLE` 行的变化必须经指纹 dump 对拍后外科重冻，`XML_RETENTION` 行的记录则是惰性素材。

## [QE-066] 六十四、采纳（flip）前必须做"真端侧 vs 现生产侧"客户端契约对拍：非致命的未达页会在采纳后升级成致命 (ADOPTION_FLIP_NEEDS_RETAIL_SIDE_CONTRACT_AUDIT)

<!-- pattern-metadata
status: CONFIRMED
scope: 任何 `XML_RETENTION → RETAIL_TABLE` 的采纳 flip（含 owner/清单/目录/XML 删除四件套），以及"真端已能编译但还没入台"的行
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 采纳前审计显示某行只有**非致命**的 `CLIENT_PAGE_UNREACHED`（"页没被下发"），flip 之后同一条客户端页变成**致命** `BUTTON_WITHOUT_ROUTE`（"可见按钮没有路由"）——因为真端 IR 把那一页**下发**了，而页上的按钮没有对应路由；`QuestClientContractGateTest` 的 `introduced` 集因此增长（该门基线为空 ⇒ 任何新增都算本车道引入）
root_cause: 两类审计状态的分母不同：XML 侧压根不下发该页 ⇒ 只报"页未达"（非致命）；真端侧按真端表下发页 ⇒ 审计能走到"页上的按钮"这一层，发现按钮无路由（致命）。同一行的同一个客户端页，**两侧的致命性不同**；只看真端侧或只看 XML 侧都会误判准入
fix_or_guardrail: 1. flip 前把该行按**真端编译器**编译（SimpleTalk 走 `RetailSimpleTalkDefinitionCompiler.compile` + 元数据编译器 + 登记表），组 `ImmutableQuestCatalog`，对同一份客户端页跑 `QuestDialogOrderAudit`；2. 与 `RetailQuestDriver.overlay(QuestDefinitionDirectoryLoader.compile(...))`（现生产侧）**逐行对拍**未达/致命两侧计数；3. 准入判准 = 真端侧致命类计数**不得比现生产侧多**（未达页只能减少或持平）——多出来的行按轴 HOLD（本例 3 行 `ACCEPT_DIALOG_ENTRANCE`：接取对话页 `1003/1004/1011/1012/1013/4` 已下发但按钮 `1012/1013/1007/1002` 无路由）；4. flip 后必须复跑生产视图审计，确认**行仍 executable**（XML 删了、真端 overlay 必须接上）且未达/致命按预测下降；5. 采纳会让该行转退役 ⇒ 家族门对退役行取冻结登记值、链门要求该行在冻结指纹里（dump 对拍 added=N/changed=0/removed=0 后外科插入，保留文件既有布局，勿整表重写）；6. 陈期望处理：跨族回归测试若把 XML 期形状写成期望（本例 `ReportToManyDialogRouteRegressionTest` 的 `(24120, 802441, 2375)`），先按客户端 `quest_summary` 行文本 + 真端表 `talk_npcK`/`reward_npc_name` 裁定"真端对/XML 错"，再外科改那一条并写明证据，不改无关条目
evidence: .agents/summary/scriptdll-quest-driver/P0c39PreflipProbeTest.java.txt（探针源码：真端侧编译 + 双侧审计 + 58 条期望复算）; .agents/summary/scriptdll-quest-driver/p0c39-probe-before.txt（XML 20 未达/0 致命 vs 真端 14 未达/3 致命）与 p0c39-probe-after.txt（生产视图 13 未达/0 致命、10 行仍 executable）; .agents/summary/scriptdll-quest-driver/p0c39-canonical-adoption-decisions.tsv（7 ADOPT + 3 HOLD 逐行两侧计数）; .agents/summary/scriptdll-quest-driver/p0c39_adopt_flip_rows.py（四副本 whipsaw 守卫 + 前置存在性校验）; src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv（+7 行）; .agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c39-canonical-adoption-flip.zh-CN.md
validation: P0c-39 实测：10 行分两批 = 7 ADOPT + 3 HOLD（21460/29070/29071 各新增 1 条致命 `1011→1012`）；采纳后 7 个客户端死页（18807 `2375`、18809 `1693`+`2375`、21070 `2375`、24120 `1693`+`1694`、28807 `2375`）全部转 `PAGE_ACTION_MATCHED`，生产视图未达 20→13（余数全在 HOLD 行）、致命 0；契约致命集 44→44（本片 0 增）；链门 2/2（added=7/changed=0/removed=0）、族门 3/3、审计 17/17、归属 4/4、目录 10/10；`verify_retirement` catalog=1344 directory=1344 retired=4880 sum=6224 OK
boundaries: 判准只针对"客户端契约"这一轴——采纳仍可能因其它轴被挡（掉落行投影 QE-061、奖励行投影 QE-051、接取入口、con_quest/cutscene）；`CLIENT_PAGE_UNREACHED` 本身是登记桶（非致命），只有当 flip **把页下发**之后按钮无路由才致命。探针必须与门禁同源：审计实现（`QuestDialogOrderAudit`）与客户端页数据（`docs/quest/client-dialog-mapping/*.csv`）都要与生产门一致，否则判准失真。期望更新只动被本片改变的那一条，且必须带客户端证据
superseded_by: none
first_check: 准备 flip 某行时，先跑双侧审计：把该行按真端编译器编译后审计，与生产视图审计对拍——出现"真端侧致命 > 现生产侧致命"即命中本模式，该行按轴 HOLD 而不是硬 flip
keywords: 采纳、flip、XML_RETENTION、RETAIL_TABLE、客户端契约、BUTTON_WITHOUT_ROUTE、CLIENT_PAGE_UNREACHED、PAGE_ACTION_MATCHED、致命升级、接取对话页、ACCEPT_DIALOG_ENTRANCE、ImmutableQuestCatalog、QuestDialogOrderAudit、退产 XML、指纹重冻、陈期望
-->
- **为什么必须双侧对拍**：两侧审计的"分母"不同——不下发页的行只会报"页没到"（非致命），下发页但按钮没路由才报"可见按钮无路由"（致命）。只跑一侧会得出相反的准入结论。
- **落地顺序**：先对拍定批次 → flip（清单四副本 + 目录 + XML 删除）→ `verify_retirement` → 指纹 dump 对拍外科插入 → 生产视图复跑（行仍 executable + 未达/致命按预测下降）→ 门禁。
- **期望漂移**：XML 期形状被写成测试期望时，按客户端 `quest_summary` 行文本与真端表字段裁定归属，再外科更新那一条。
## [QE-067] 六十五、客户端页梯必须在单步与链式两条编译器路径上同源补齐：链式 NPC_START 块漏 acceptContinuation 会让 select1 页按钮成死端 (ACCEPT_PAGE_LADDER_MUST_BE_SHARED_BY_BOTH_COMPILER_PATHS)

<!-- pattern-metadata
status: CONFIRMED
scope: SimpleTalk 链式/单步两条编译路径（`RetailSimpleTalkDefinitionCompiler` 的 `buildDefinition` vs `buildChain`），以及任何"真端模板表没有该列、形状只能来自客户端登记表"的对话页梯（select1/select2/select5 续页）
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 客户端任务书里 `select1(1011)` 页只有一个可见按钮（`HACTION_SELECT1_1`，动作 id 1012），但真端 IR 只下发 1011 页、没有任何 `dialogId=1012` 的路由 ⇒ 审计对**链式行**报致命 `BUTTON_WITHOUT_ROUTE`（"visible client action has no route"），玩家点"继续听"没有任何反应；同一形状的单步行完全正常。全目录同轴 42 行（其中 4 行在门禁的"已确认接取对话"集里），`QuestClientContractGateTest` 的 fatal 集因此长期停在 44 条
root_cause: 接取页梯（`SELECT1` → `SELECT1_1` → [`SELECT1_1_1`] → 接取窗）在**单步路径**由 `acceptContinuation(npc, continues)` 按客户端出口登记（`RetailClientDialogExits` 的 `SELECT1_1`/`SELECT1_1_1`）发射；**链式路径**的 NPC_START 块只调 `acceptFlowChain`（内部走 `acceptFlow`，覆盖 SELECT1/ASK_QUEST_ACCEPT/ACCEPT/REFUSE 四页与关闭路由），漏了 `acceptContinuation` 这一支，而它此前只消费了出口登记的 `SELECT2_CONTINUE`/`SELECT5_CHECK[_SIMPLE]` 两支。真端模板表（`Quest_SimpleTalk.xml`）**没有页链列**，页梯的唯一权威是客户端出口登记（派生自 `docs/quest/client-dialog-mapping/quest-dialog-action-details.csv` 的 `href` 列）
fix_or_guardrail: 1. 在 `buildChain` 的 NPC_START 块循环内、`acceptFlowChain` 之后，按与单步路径**逐字同形**的闸门补 `acceptContinuation`：`if (exits.requires(questId, SELECT1_1)) blockTransitions.addAll(acceptContinuation(start.npcId(), exits.requires(questId, SELECT1_1_1)))`；2. **修在编译器侧、不要改登记表**——登记表是逐字转写事实源，且"显式路由覆盖块路由"与"块间同键去重"两条既有语义会让补进去的块路由自动让位给已存在的显式 `unaccepted:npc:1012` 路由（这正是 42 行而非 42+35 行变化的原因）；改表则会把这 35 行也拖进变更面并可能改写别处语义；3. 复跑判据 = 全目录 `selectorGap(shownPage=1011 ∧ clientVisibleAction=1012)` 归零 **且** 链式指纹 set 对拍的 `changed` 集合与缺口集合**逐元素相同**（变化面 = 缺陷面，无溢出；`added`/`removed` 只应等于本轮 flip 行数）；4. 指纹重冻走外科改值 + 前缀插入，尾部历史块逐字节保留（该文件前缀升序、尾块历史追加，整表排序重写会破坏布局）
evidence: .agents/summary/scriptdll-quest-driver/P0c40AcceptChainProbeTest.java.txt（探针源码：双侧 RET/XML 转换 dump、全目录 blast、修复前仿真诊断）; .agents/summary/scriptdll-quest-driver/p0c40-accept-continuation-blast.tsv（42 行缺口清单 + 修复前后全局计数 + 指纹变化集合一致性断言）; .agents/summary/scriptdll-quest-driver/p0c40-accept-chain-decisions.tsv（3 行 HOLD 裁定：XML 侧 13 未达 vs 真端侧 0 未达 0 致命）; src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkDefinitionCompiler.java（补丁点：NPC_START 块循环）; src/main/resources/aion/data/static_data/quest_retail/quest_client_dialog_exits.tsv（页梯权威登记）; docs/quest/client-dialog-mapping/quest-dialog-action-details.csv（客户端按钮 `href` = 动作 id 1012/1013/1007/1002/1003）; src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv（重冻后 285 行）; src/test/resources/quest/quest-client-contract-baseline.tsv（空基线 ⇒ 任何 fatal 都会现形）; .agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c40-accept-continuation-and-hold-flip.zh-CN.md; .agents/summary/scriptdll-quest-driver/p0c40-blocked-rows.tsv
validation: P0c-40 实测：修复前 `selectorGap(1011→1012)=42`、`EVIDENCE_REQUIRED=44`、`CLIENT_PAGE_UNREACHED=1555`、契约门 fatal=44；修复后 `selectorGap=0`、`EVIDENCE_REQUIRED=2`、`CLIENT_PAGE_UNREACHED=1389`、契约门 fatal=2，链式指纹 `changed=42`（与缺口集合逐元素相同）`/added=0/removed=0`；21460/29070/29071 采纳后真端侧 0 未达 0 致命（XML 侧 13 未达被闭合），`verify_retirement` 1341/1341/4883/6224 OK；链门 2/2、审计 17/17、T2（45 id）204 例与改动前全量 T3 逐条对拍零新增失败（CHRONIC 20 / TRANSIENT 2 / 本片改善 1）
boundaries: 本模式只管"页已下发但按钮无路由"（致命）这一轴；**页根本没被下发**的 `CLIENT_PAGE_UNREACHED`（本例 1011 页仍有 48 行）是另一轴 `ACCEPT_PAGE_NOT_EMITTED`，补续页无效。契约门残留 2 条（1932/3092：显式 `R` 把 CHECK 失败页 `SELECT6(2716)` 锚在 reward 态而 `FINISH_DIALOG` 关闭路由只登记在阶段态）属 `REPORT_ALIAS_SELECT6_FROM_REWARD`，与本轴无关。门禁的"已确认接取对话"集过滤条件只覆盖 `clientVisibleAction=1007` 与页 `4/1003/1004`，**不覆盖 1011 页**——所以这一类能长期存在而不被该断言拦住，只能靠空基线的 `QuestClientContractGateTest` 现形
superseded_by: none
first_check: 审计出现"某页可见按钮无路由"且该行是多 NPC 链式行（有 NPC_START 块）时，先比对同一形状的单步行：若单步行有该动作的路由而链式行没有，即命中本模式——查 `exits.requires(questId, SELECT1_1/SELECT1_1_1)` 与 `acceptContinuation` 是否只在单步路径被调用
keywords: acceptContinuation、SELECT1_1、SELECT1_1_1、1011、1012、1013、HACTION_SELECT1_1、接取页梯、链式路径、buildChain、NPC_START、BUTTON_WITHOUT_ROUTE、visible client action has no route、quest_client_dialog_exits、客户端出口登记、指纹重冻、变化面=缺陷面
-->
- **一句话判据**：页梯的续页路由**只认客户端出口登记**，任何"两条编译路径只补了一条"的缺口都会以"页已下发但按钮无路由"（致命）现形——修的时候先看**单步路径有没有现成实现**，再按同一闸门补另一条。
- **为什么不能改登记表**：登记表是逐字转写事实源 + 显式路由覆盖块路由 + 同键去重三条语义叠加后，"补表"会把非缺口行也拖进变更面；编译器补丁则会被这三条语义自动收敛到真正缺路由的那 42 行。
- **收口判据**：全目录同轴缺口计数归零 **且** 冻结指纹的 `changed` 集合与缺口集合逐元素相同。

## [QE-068] 六十六、下发 select6 失败页的节点必须同节点同 NPC 登记 1008 关闭出口：同一交付段的"显式路由锚"与"块关闭出口锚"分家会让 reward 态失败页按钮死掉 (SELECT6_CLOSE_EXIT_MUST_BE_ANCHORED_WHERE_THE_PAGE_IS_PUSHED)

<!-- pattern-metadata
status: CONFIRMED
scope: SimpleTalk 链式编译路径（`RetailSimpleTalkDefinitionCompiler.buildChain`）的交付段（`B NPC_REPORT` 块 + 显式 `R` 记录 + `I` 门三通道）；同形状在其他族（DataDriven / SimpleCollectItem / CombineTask / 旧 Java 形态）仍存在但审计不可达（62 行）
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 审计报致命 `BUTTON_WITHOUT_ROUTE`：`EVIDENCE_REQUIRED | 2716 | action=1008 | path=reward + NPC 203893 + 20002 -> reward + page 2716 | reason=visible client action has no route`——同一页在另一个锚点上是好的（`PAGE_ACTION_MATCHED | 2716 | path=s1 + NPC 203893 + 39 -> s1 + page 2716`）。判例 1932/3092，是 `QuestClientContractGateTest` 空基线里最后 2 条致命
root_cause: 同一交付段由两个机制合成而锚点不同：①`B NPC_REPORT` 块（`reportFlowChain`）把 select5 页 + CHECK 按钮对 + **关闭出口** 一起锚在块 `source`（阶梯行 `s{K}`）；②生成器 `synthesize_canonical` 的显式 `R` 记录把同一个 CHECK 对锚在块 `target`（字面 `reward`，因为阶梯迁移规则只搬 `source=='started'` 的行）。"显式路由覆盖块路由"只在 `(source, npc, dialogId)` 同键时生效，键不同 ⇒ `reward` 态多出一对 priority=1 的失败页下发路由（39/20002 → `ShowQuestDialog(2716)`），却没有任何 `FINISH_DIALOG(1008)` 路由。客户端 `select6(2716)` 页的唯一按钮就是 `1008`「结束对话」，于是该按钮死
fix_or_guardrail: 1. 在 `buildChain` 加**闭包后处理**（与既有的 `SELECT2_CONTINUE`、`SELECT5_CHECK` 后处理同址同形）：只认 `exits.requires(questId, SELECT6)` 的行，扫描全部 transition，凡"下发 `ShowQuestDialog(SELECT6(2716))` 的节点 + 该节点缺同 NPC 的 `FINISH_DIALOG(1008)`"就补一条路由，形状（target、after-commit `ShowQuestSelectionDialog(SELECT_QUEST)`）**逐字取自同任务既有关闭出口**，不新造页与动作；2. 注意预扫描"已服务"集合时只能收集 **`dialogId == 1008`** 的路由——把任意 TalkToNpc 路由都写进该集合会让闭包永远判为已满足（首次实现即踩此坑，探针计数一字未变）；3. 不要去改登记表的锚点（改事实源 + priority=1 失败行 `target=reward` 的"失败不推进"语义需逐行重写），也不要做任务级打点补丁（掩盖客户端合同）
evidence: .agents/summary/scriptdll-quest-driver/P0c41Select6CloseExitProbeTest.java.txt（探针：全目录 select6 pusher 扫描 + 契约致命列表 + 1932/3092 的 IR/客户端页/审计逐行 dump）; .agents/summary/scriptdll-quest-driver/p0c41-select6-close-exit-blast.tsv（修复前/后对拍：69→65 键、闭包 4 键、溢出 0）; .agents/summary/scriptdll-quest-driver/p0c41-select6-closure-decisions.tsv（逐键裁定 + 两条未采取路线的理由）; .agents/summary/scriptdll-quest-driver/p0c41_refreeze_fingerprints.py（set 对拍 + 原值漂移守卫 + whipsaw 守卫）; src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkDefinitionCompiler.java（闭包点 + `closeKey`/`pushesSelect6`）; src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv（交付段 R 记录锚在 reward 的逐字证据）; docs/quest/client-dialog-mapping/quest-dialog-action-details.csv（2716 页唯一按钮 1008「结束对话」）; .agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c41-select6-close-exit-closure.zh-CN.md
validation: P0c-41 实测：修复前 `FATAL count=2`（1932/3092）、`statuses` 含 `EVIDENCE_REQUIRED=2`、`SELECT6 pushers=1519 / missing close exit=89 / missing WITH select6 exit=79`（去重 69 键 / 64 任务）；修复后 `FATAL count=0`、`EVIDENCE_REQUIRED` 桶消失、`PAGE_ACTION_MATCHED` 106756→106758（+2 = 两条新关闭路由被匹配）、`missing WITH select6 exit=75`；闭包集合断言 `closed=4 introduced=0 chain_closed=[1932,3092] other_closed=[]`；链式指纹 set 对拍 `changed=2 (1932/3092) added=0 removed=0`，重冻后双副本 md5 `6a4b09d6f24074d523fffb737b518cc4`（仅第 48/122 行改值）；T1 净树 63 例 **4F→3F**（消失的那条正是契约门；余 3 条 DataDriven/CollectItem 为并发车道在途）
boundaries: 本模式只管"页已下发但按钮无路由"且该页在客户端出口登记在册的行；**页根本没下发**是 `ACCEPT_PAGE_NOT_EMITTED`（另一轴），`PAGE_NOT_IN_TASK_HTML` 是第三轴。审计的可达性模型走 IR 路由（不是客户端页图），所以"reward 态 39/20002 路由在客户端图上其实不可达"不构成豁免——空基线的契约门按 IR 可达性判定，闭包是让 IR 自洽的最小改动。其余 62 行同形状（DataDriven/SimpleCollectItem/CombineTask/旧 Java 形态，节点名 `s5`/`equipped96`/`reward30`/`instance95` 等）审计不可达 ⇒ 非致命，**不得套用本片规则批量改**，需按族单独取证（`SELECT6_CLOSE_EXIT_OTHER_FAMILIES`）
superseded_by: none
first_check: 审计出现"某页可见按钮无路由"且 path 的两端节点名不同（`X + NPC n + action -> Y + page p`）时，先在 IR 里 `grep` 该 `(Y, n, 1008)` 是否存在——缺则命中本模式；同时 `grep` 该 `(X, n, 1008)` 是否存在以确认"关闭出口锚在另一个节点"这一根因
keywords: select6、2716、1008、FINISH_DIALOG、HACTION_FINISH_DIALOG、结束对话、关闭出口闭包、锚点错位、reportFlowChain、显式路由覆盖块路由、synthesize_canonical、阶梯迁移、BUTTON_WITHOUT_ROUTE、EVIDENCE_REQUIRED、契约门归零、闭包后处理、变化面=缺陷面
-->
- **一句话判据**：客户端页上的**每一个可见按钮**都要在"该页被下发的那个节点"上有路由——关闭按钮也不例外；把"下发页的锚点"和"关闭出口的锚点"当成一件事，是这个 bug 家族的全部内容。
- **为什么是闭包而不是改锚点**：改锚点要动登记表事实源与失败分支的 target 语义；闭包只在"真缺"处补一条与已有关闭出口同形的路由，天然把变更面收敛到缺陷面（4 键 / 2 行）。
- **实现坑**：预扫描"关闭已被服务"的集合时**只能收 `FINISH_DIALOG`**（收全部 TalkToNpc 会把 1008 键误标为已存在）。
- **收口判据**：契约门 fatal 归零 **且** 冻结指纹 `changed` 集合 == 闭包键所属任务集合（本例 `{1932, 3092}`），`added`/`removed` 为空。

## [QE-070] 六十八、物件哨兵接取者的接取阶梯必须改绑物件 owner，且修复必须落进生成器：无主 Q 记录 + 丢页的物件路由 = 接取窗永远打不开 (OBJECT_SENTINEL_ACCEPT_LADDER_REBOUND_IN_GENERATOR)

<!-- pattern-metadata
status: CONFIRMED
scope: SimpleTalk 链式行里 `acquired_npc_name` 为**物件哨兵**（箱子/物件对象，判例 `LF2_Lost_JewelBox` = 730032）的行；登记表 `quest_client_talk_chain_steps.tsv` 的接取侧（无主 `Q` 记录 + 物件 `R USE_OBJECT` 路由）；生成器 `build_quest_client_talk_chain_steps.py` 的裁定表通道
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 审计报 `CLIENT_PAGE_UNREACHED`——客户端接取段三页从未被下发（入口页 `select1(1011)`、接取窗页 `ask_quest_accept(4)`、拒绝结果页 `quest_refuse_1(1004)`），接受结果页 `quest_accept_1(1003)` 靠一条**无主** `QUEST_ACTION + 1002` 侥幸 `PAGE_ACTION_MATCHED`（path 里没有 NPC，写的是 `unaccepted + QUEST_ACTION + 1002`）。玩家点箱子后用不出任何页 ⇒ **接取窗打不开 ⇒ 任务不可接取**。因判据是"未达"（非致命），空基线的 `QuestClientContractGateTest` 不会拦，只能在轴普查里现形
root_cause: 接取者是**物件**（真端 `acquired_npc_name=LF2_Lost_JewelBox` = 箱子 730032），而历史 XML 把接取阶梯写成 4 条**无主** `Q` 记录（`QUEST_ACTION` 无目标事件、npcId 记 0），唯一的物件路由 `R USE_OBJECT` 又**丢了下发页**（`after='-'`、`page_check='-'`）——于是"这些页由谁下发"在登记表里根本没写；`B NPC_START` 块缺失（`p0c10h-chain-keep-closure.tsv` 记 `NO_START_OBJECT`："接取经物件交互，链表无物件起始词汇"）是同一件事的另一面
fix_or_guardrail: 1. 按**原位**把无主 `Q` 阶梯转成绑定物件的 `R` 记录（页名动作 → `DIALOG:SHOW_QUEST_PAGE:<页>` + `<页>=CLIENT`；`FINISH_DIALOG` → `CLOSE`），给物件交互路由补入口页下发，并**紧邻**插入 `ASK_QUEST_ACCEPT → DIALOG:SHOW_QUEST_PAGE:SHOW_ASK_QUEST_ACCEPT_WINDOW`（形状逐字取自同族已采纳兄弟行 2611/3001/24123/35010/35017）；2. 接取侧四条全部绑物件——审计 `sameDialogOwner` 要求"下发页的路由"与"页按钮的路由"同 owner，绑错 owner 依旧是未达；3. **修复必须落进生成器**：登记表是生成物，手改会被下次重生成静默回退 ⇒ 裁定写成 `p0c42-accept-entrance-decisions.tsv` + 生成器函数 `rebind_accept_entrance()`（六轴 fail-closed），**落地判据 = 把 `OUT` 重定向到临时文件跑全量生成器，输出与人工编辑结果逐字节相同**（本轮 md5 `badc6ee20acfd17cb66ca04005a7e206`，且 8 张既有裁定表零漂移）；4. 客户端**两套 id 空间不同号**（按钮 1002 `HACTION_QUEST_ACCEPT_1` 的目标页是 1003）⇒ 按钮校验用**常量名**（`HACTION_ASK_QUEST_ACCEPT` / `HACTION_FINISH_DIALOG`），页校验用页册 `page_constant` 去 `HTML_PAGE_` 前缀的引擎符号，并与 `QuestDialogPage` 枚举号互证（实测 4 == `SHOW_ASK_QUEST_ACCEPT_WINDOW(4)`）
evidence: .agents/summary/scriptdll-quest-driver/p0c42-accept-entrance-decisions.tsv（裁定表：六轴判据逐条写在表头）; .agents/summary/scriptdll-quest-driver/build_quest_client_talk_chain_steps.py（`rebind_accept_entrance()` + `client_action_constants` / `client_page_constants` / `page_enum_ids` 三个取证通道）; .agents/summary/scriptdll-quest-driver/p0c42_accept_entrance_repair.py（首轮外科手术脚本）; .agents/summary/scriptdll-quest-driver/p0c42_refreeze_fingerprints.py（`EXPECTED_CHANGED=("1323",)` + PRE 值漂移守卫）; .agents/summary/scriptdll-quest-driver/p0c42-accept-page-gap-{pre,post}.tsv（轴全量 46/45 行身份）; .agents/summary/scriptdll-quest-driver/p0c42-accept-page-blast.tsv（`removed=1 introduced=0`）; .agents/summary/scriptdll-quest-driver/p0c42-1323-audit-{pre,post}.tsv（3 条未达 → 0）; .agents/summary/scriptdll-quest-driver/p0c42-blocked-rows.tsv; .agents/summary/scriptdll-quest-driver/P0c42AcceptPageNotEmittedProbeTest.java.txt; src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv（1323 接取侧 14 条 R 记录）; docs/quest/client-dialog-mapping/quest-dialog-action-details.csv（1007「打开盖子看看。」/ 1002「接受。」/ 1003「拒绝。」/ 1008「结束对话。」）; docs/quest/client-dialog-mapping/quest-dialog-pages.csv（页序 1..4 与 `page_constant`）; .agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c42-object-sentinel-accept-entrance.zh-CN.md
validation: P0c-42 实测：1011 缺口 46→**45**（`removed={1323}`、`introduced=[]`，carriers 4067）；1323 审计 3 条 `CLIENT_PAGE_UNREACHED`（1011/4/1004）→**0**（11 行全 `PAGE_ACTION_MATCHED`/`TERMINAL_PAGE_REACHED`，接取侧 `unaccepted + NPC 730032 + …`）；IR 转换 31→32（+1 = 新 `ASK_QUEST_ACCEPT` 边）；链式冻结指纹 `changed={1323}/added=0/removed=0`，重冻后双副本 md5 `55302172e2f95cce2b9b324b8711113c`（288 行布局逐字节保留）；契约门 fatal 保持 0；净树复跑 20/20（链门 2 + 审计 17 + 契约门 1）；T1 63 例 3F 与 P0c-41 基线同名同条；T2（1323）75 例 5F 全 chronic（1131/1197 历史在册）；T3 全树 2003 例 124F+23E 与并发 lane `T3-153216` 身份差集 −1/+5（−1 = 本片登记表↔指纹跨窗口瞬态，重冻后三处转绿；+5 = 并发 lane 的 25052 半途翻窗）
boundaries: 只管**物件哨兵接取者**的 SimpleTalk 链式行（登记表里该物件的 `R` 路由存在、`B NPC_START` 块缺失）；DataDriven 行的 `select1(1011)` 是**FOBJ 交互推进页**不是接取入口（该族接取入口是 `select_none(4762)` 的 20000/20001 按钮；25070 实测真端用 `PACKET_ONLY` 不发页），属另一轴 `DATADRIVEN_SELECT1_IS_FOBJ_STEP_PAGE_NOT_EMITTED`；44 行 `XML_RETENTION` 的 1011 未达不属本轴（IR 来自旧 XML，随采纳消失，采纳前按 QE-066 双侧对拍）
superseded_by: none
first_check: 审计出现 `CLIENT_PAGE_UNREACHED | page=1011` 且该任务 owner=`RETAIL_TABLE` 时，先 grep 登记表该任务的记录类型分布——出现**无主 `Q` 记录**（`Q` = `QUEST_ACTION` 无目标）+ 物件 `R` 路由 `after='-'` 即命中本模式；再查真端 `acquired_npc_name` 是否物件哨兵（非 NPC 名）以确认轴
keywords: 1011、select1、接取入口、物件哨兵、LF2_Lost_JewelBox、730032、无主 Q 记录、QUEST_ACTION、USE_OBJECT、ASK_QUEST_ACCEPT、SHOW_ASK_QUEST_ACCEPT_WINDOW、sameDialogOwner、CLIENT_PAGE_UNREACHED、ACCEPT_PAGE_NOT_EMITTED、生成物修复必须落进生成器、逐字节复现、1323
-->
- **一句话判据**：接取者是**物件**时，"谁下发接取页"必须在登记表里显式绑到那个物件——无主（npcId=0）的阶梯记录等于没写，玩家用箱子时服务端不知道要下发任何页。
- **为什么绑物件而不是绑 NPC**：审计按 owner 匹配（`sameDialogOwner`），入口页与接取窗页只能由**箱子**触发；改绑到任何 NPC 都会让审计继续判未达（页可达但按钮的 owner 对不上）。
- **实现坑**：客户端按钮 id 与页 id 是**两套不同号的 id 空间**（按钮 1002 → 页 1003），别用 id 对齐页；用 `action_constant`（`HACTION_*`）与 `page_constant`（`HTML_PAGE_*` 去前缀）对齐，并把引擎页符号与 `QuestDialogPage` 枚举号互证。
- **生成物耐久性（本卡第二判例）**：登记表/指纹/清单这类**生成物**的缺陷，修复必须同时落进生成器（裁定表 + fail-closed 形状断言）；落地判据不是"文件现在是对的"，而是"**生成器重跑输出与人工编辑逐字节相同**"——否则下一次重生成会静默回退，缺陷在下游以别的形状复现。
- **收口判据**：轴清单 `removed` == 裁定行集合且 `introduced` 为空 **且** 该任务审计未达归零 **且** 链式指纹 `changed` == 该任务（`added`/`removed` 为空）**且** 生成器重跑逐字节复现。

## [QE-071] 六十九、"阶梯改写"与"接取入口重绑"都改不了的行走**改道**（整块弃转写按真端重合成）：判据是两条既有通道的改写面都覆盖不到它 (CANONICAL_REROUTE_WHEN_BOTH_LOCAL_CHANNELS_MISS)

<!-- pattern-metadata
status: CONFIRMED
scope: SimpleTalk 链式登记表 `quest_client_talk_chain_steps.tsv` 里**结构轴**与真端分歧的行（不是页名/动作错，而是缺整段结构与错绑 NPC）；生成器的三条形状裁定通道（阶梯改写 P0c-36/37、接取入口重绑 P0c-42、改道 P0c-43）
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 审计报**中间对话页** `CLIENT_PAGE_UNREACHED`（判例 24123：`select2(1352)` / `select2_1(1353)` / `select3(1693)` 三页）且该任务 owner=`RETAIL_TABLE`——玩家在阶段 NPC 处看不到对话页、阶段推不动；同时交付/报告路由全部挂在**接取 NPC** 上（`SELECT_QUEST_REWARD` / `CHECK_USER_HAS_QUEST_ITEM[_SIMPLE]` / `SET_SUCCEED` / `npc-complete` 的 npc-id 全 = 接取人），而真端 `reward_npc_name` 是另一个人
root_cause: 遗留 XML 是**手工转写惯性产物**的两轴叠加——①把多行任务书压成单个 `started(var0=0)`、`N reward REWARD 1`、布局 `P 0 1 0 1`（只容 0/1，装不下 K 段阶段号）⇒ K=2 的中间页**根本没有转写**；②把全形状铺满一个 NPC（P0c-19 判例：按真端对、XML 错裁）。两条既有局部通道都覆盖不到：`apply_talk_ladder` 只搬 `int(r[3]) == reward_id` 的行且不重绑报告 NPC、不动 `P` 布局；`rebind_accept_entrance` 只改接取段（本行接取段本来正确）
fix_or_guardrail: 1. **改道 = 整块弃转写**，按真端走 `synthesize_canonical`（K 段 `SETPRO` 阶梯 + `P 0 6 0 63` + 报告流绑 `reward_npc_name`）；2. 裁定写成 `p0c43-canonical-resynthesis.tsv`（列 `quest_id / code / legacy_report_npc / retail_reward_npc / basis`，code 域 `LEGACY_REPORT_FLOW_ON_ACQUIRED`）+ 生成器加载器**六轴 fail-closed**：code 域 / `legacy_report_npc` 解析 id **== 真端 acquired**（确认是"全绑接取 NPC"这一形）/ 真端 `reward_npc_name` 唯一解析且 **≠ acquired** / 每个 `talk_npcK` **≠ acquired**（防阶梯自环）/ 客户端任务书行数 **== K+1**（QE-051）/ 客户端页册含 `SELECT{i+2}`（i=0..K-1），并加**与阶梯表·接取入口表的互斥闸**；3. **转写循环必须对该行 skip**（`CANONICAL-OVERRIDE`）——否则同一任务出双份块；4. 落地判据 = 生成器重跑输出逐字节相同（本轮 `322f4d1a99192de960ec52d6c18c16f1` / 430735 字节）+ 逐任务块爆炸半径 `CHANGED==裁定行集` 且 `ADDED/REMOVED` 为空（295 块 → 295 块）
evidence: .agents/summary/scriptdll-quest-driver/p0c43-canonical-resynthesis.tsv（裁定表，表头逐轴判据 + 真端/客户端留证）; .agents/summary/scriptdll-quest-driver/build_quest_client_talk_chain_steps.py（`canonical_resynthesis` 加载器 + 转写 skip）; .agents/summary/scriptdll-quest-driver/p0c43_dryrun_diff.py（安装前干跑逐任务块对拍）; .agents/summary/scriptdll-quest-driver/p0c43_blast_radius.py + p0c43-registry-pre.tsv（安装后独立复现爆炸半径）; .agents/summary/scriptdll-quest-driver/p0c43-24123-audit-{pre,post}.tsv（10 行含 3 未达 → 9 行全达）; .agents/summary/scriptdll-quest-driver/p0c43-24123-ir-post.tsv（38 条转换逐条）; .agents/summary/scriptdll-quest-driver/p0c43-24123-registry-{pre,post}.tsv（22 行 / 19 行）; .agents/summary/scriptdll-quest-driver/p0c43_refreeze_fingerprints.py; .agents/summary/scriptdll-quest-driver/p0c43-generator-fidelity.txt; src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv（24123 块）; src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml（24123 行）; docs/quest/client-dialog-mapping/quest-dialog-pages.csv（页序 1352/1353/1693/2375）; .agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c43-canonical-resynthesis-24123.zh-CN.md
validation: P0c-43 实测：24123 块 22→19 行（`P 0 1 0 1`→`P 0 6 0 63`、`N reward REWARD 1`→`s1(1)/s2(2)/reward(2)`、`B NPC_START 204345` + `B NPC_REPORT/NPC_COMPLETE 204387`、16 R→9 R）；审计 10 行（3 × `CLIENT_PAGE_UNREACHED` = P0c-37 `GENUINE_GAP`）→**9 行全达**（转换 35→38）；爆炸半径 `CHANGED [24123] / ADDED [] / REMOVED []`（295→295 块）；指纹 `changed={24123}/added=0/removed=0`（288 行布局逐字节保留，双副本 md5 `3686124be2b45dda60c0fdb07f437258`）；契约门 fatal 0；净树 20/20；T1/T2 与基线 `T1-143553` 逐字相同；派生留痕 `p0c34-chain-reward-row-divergence.tsv` 中该行的"REWARD 投影 1 vs 客户端末行 2"消失（P0c-34 轴闭合）
boundaries: 1. **接取人形态分家**：NPC 接取者的入口页下发来自 `B NPC_START` 块的 `startPages=SELECT1`，客户端 `select1(1011)` 的 20000/20001（`ACCEPT_SIMPLE`/`REFUSE_SIMPLE`）由编译器 `acceptFlow` 自动覆盖（24123 实测两按钮都 `PAGE_ACTION_MATCHED`）⇒ **不需要** QE-070 的物件重绑；物件接取者才需要。2. 只对"真端多阶段 + 报告人在别处"的行有效；真端 `reward_npc_name` == acquired 的行不属于本形（六轴 ③ 会 fail）。3. 改道只改**形状**，不代裁客户端页是否存在（页证在轴 ⑥ fail-closed）。4. 残留面（2026-09-26 普查）：登记表内 117 行真端多阶段行里阶梯缺失轴归零，报告 NPC 未接线仅 **2482**（Moreinen 204211 完全不接线，阶段当主 3 人 vs K=2）——单行同签名候选，**须逐行取证裁定**，不得批量套用本卡
superseded_by: none
first_check: 中间对话页 `CLIENT_PAGE_UNREACHED` 且 owner=`RETAIL_TABLE` 时，先比真端 `talk_npc1..K` 与登记表 `N` 记录的 var0 投影（见 QE-072 的投影式口径），再比"指向 reward 的 R 行所绑 NPC"与真端 `reward_npc_name`；两项都分歧 ⇒ 走本卡改道
keywords: 改道、reroute、canonical 合成、synthesize_canonical、canonical_pending、CANONICAL-OVERRIDE、双份块、XML_NPC_AXIS、GENUINE_GAP、CLIENT_PAGE_UNREACHED、report_npc_name、reward_npc_name、阶线自环、K+1 任务书行数、24123、DF2_NPC_Ananta、DF2_LycanPrisoner_Q24123A
-->
- **一句话判据**：**局部改写够不着整段结构分歧**——"阶梯改写"只搬 `reward_id` 绑定的行、"接取入口重绑"只动接取段；当缺的是整段阶段梯且报告人绑错时，唯一正确动作是**整块弃转写、按真端重合成**，并把"允许改道"做成裁定表 + fail-closed 断言。
- **两端都要改**：生成器加改道通道**同时**要让转写循环 skip 这些行——否则同一任务在输出里出现双份块（旧转写块 + 新 canonical 块），加载器顺序无关反而更隐蔽。
- **六轴里最容易漏的两轴**：ⓐ 阶段归属自环（`talk_npcK` 解析 id == acquired）——真端写了新 NPC 但名册把它解析回接取人时，阶梯等于原地踏步；ⓑ 与其它裁定表互斥——同一任务不能同时出现在阶梯表/接取入口表（两套裁量）。
- **接取流覆盖不要重做**：NPC 接取者的 `1011` 入口页 + `20000/20001` SIMPLE 按钮由 `B NPC_START` 的 `startPages` 与编译器 `acceptFlow` 覆盖（QE-070 的物件形才需要手绑）。
- **收口判据**：生成器重跑逐字节相同 **且** 逐任务块爆炸半径 `CHANGED == 裁定行集`（`ADDED/REMOVED` 为空）**且** 该任务审计未达归零 **且** 指纹 `changed` == 该任务 **且** 派生留痕表（如 REWARD 投影分歧）里该行的旧分歧消失。
- **裁定表是 code 域而不是单例**：同一条通道可承载多个 code（P0c-44 加 `LEGACY_STAGE_OWNER_DRIFT`），但**每个 code 必须有自己的形态判据**（本 code：转写报告 NPC 既不在真端声明集内、又必须出现在 census `DEVIATION` 列——两条合起来才排除"真端表就写了他"与"census 根本没测到他"两种伪漂移），共用的真端轴照旧 fail-closed；新 code 的落地判据与首个 code 完全相同。

## [QE-072] 七十、跨族残留普查必须**投影式**且先在已知判例上校准：标签/通用事件名/末段 var0 三处都会造假阳性 (CROSS_FAMILY_CENSUS_MUST_BE_PROJECTION_BASED)

<!-- pattern-metadata
status: CONFIRMED
scope: 登记表/清单类产物的跨族残留普查（"这个轴还剩几行"）；任何按节点标签、动作名或单点数值做匹配的统计口径
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 普查报出的残留行数比真实缺陷面大一个量级，导致下一步切片的范围被误估（本片首版"缺阶段梯 64 行"、二版"报告侧 215 行"，真值分别是 0 行与 1 行）
root_cause: 三类假阳性——①**节点标签不统一**：登记表阶段节点用 `s1/s2`（24123）、`v1/v2`（1394）、`k1..k3`（1537），其他族还有 `steps1`/`equipped96`/`reward30`/`instance95`，按 `s\d+` 匹配会把**完整阶梯**判成缺失（117 行里 88 行）；②**把通用事件名当专用语义**：`QUEST_SELECT` 是**每个链上 NPC 的通用对话事件**（含接取 NPC），把它算进"报告侧动作"会把全部多 NPC 链都标记成"报告人绑错"；③**未计 QE-051 的等价形**："reward 与末段 sK 同 var0" ⇒ 末段 var0 可以落在 REWARD 节点上（4052 = `stage1(1)/stage2(2)/reward(3)`），只数 START 节点会少算一段
fix_or_guardrail: 1. **判据用投影（var0 / 节点接线 / 页可达性），不用标签、不用动作名字面**；2. 长度类判据要显式写出等价形（START 节点 var0≥1 **并集** REWARD 节点 var0）；3. **先在已知判例上双向校准**：判据必须在"修复前的判例快照"上触发、在"修复后的现状"上不触发（本片用 `p0c43-registry-pre.tsv` 的 24123 块与现状块各跑一次）；4. 残余的不确定性（如"报告流用 `USE_OBJECT` 推页而不是 `QUEST_SELECT`"）用**更强的不变量**替代（本片改为"真端 `reward_npc_name` 是否在块内接线"，动作名无关）；5. 信息性标记与缺陷标记分开列（`REPORT_SIDE_MIXED` 仅留证，"共享交付 NPC"是同族正常形态）
evidence: .agents/summary/scriptdll-quest-driver/p0c43_residue_census.py（判据 v1→v4 的注释里逐条留档三次过度标记）; .agents/summary/scriptdll-quest-driver/p0c43-residue-census.tsv（最终 3 行：1 缺陷候选 2482 + 2 信息性 35010/35011）; .agents/summary/scriptdll-quest-driver/p0c43-registry-pre.tsv（校准用的修复前快照）; src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv（1394 的 `v1/v2`、1537 的 `k1..k3`、4052 的 `stage1/stage2/reward` 三种标签实况）; src/main/resources/aion/data/static_data/quest_retail/quest_client_summary_rows.tsv（K+1 行数背书）; .agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c43-canonical-resynthesis-24123.zh-CN.md（§6 三次过度标记留档）
validation: P0c-43 实测：按 `s\d+` 标签判据 ⇒ `LADDER_MISSING` 64 行；计入 QE-051 末段 var0 后 ⇒ **0 行**（117 行真端多阶段行全覆盖，K=1 的 224 行不适用）；把 `QUEST_SELECT` 移出报告侧动作集 ⇒ 噪声从 215 行降到 40 行，再换成"reward NPC 块内接线" ⇒ **1 行**（2482）；判例校准：同一判据在 24123 修复前块上触发、修复后不触发
boundaries: 本卡只管**普查口径**（"还剩几行"），不代替逐行裁定——候选行仍须按所在族的取证口径（真端轴 + 客户端页证 + 审计）逐行判定，不得按族批量套用；已登记为"信息性"的形态（同族共享交付 NPC、`USE_OBJECT` 推进报告页）不要当缺陷改
superseded_by: none
first_check: 普查结果若与既有登记面差一个量级（或首次运行就报出几十上百行），先假定是判据假阳性：抽 2-3 行打开登记表**逐记录**看真实形状（标签/节点投影/接线 NPC），再用"修复前快照 + 现状"双向校准
keywords: 残留普查、census、假阳性、过度标记、节点标签不统一、投影式判据、var0 投影、QUEST_SELECT 通用事件、QE-051、reward 与末段同 var0、判例校准、REWARD_NPC_UNWIRED、LADDER_MISSING、s1/v1/k1、2482
-->
- **一句话判据**：普查口径的每一处"字面匹配"（标签名、动作名、单一数值）都会在跨族时变成假阳性源——先问"这条记录**投影**出什么（var0 / 接线 / 页可达）"，再问"名字对不对"。
- **校准是硬要求**：新判据必须同时在"已知缺陷的修复前快照"和"修复后现状"上跑一遍（触发/不触发），否则量出来的数字不能进台账。
- **量级异常先当假阳性**：首次运行就报出几十上百行，几乎总是判据问题（本片 64 → 0、215 → 1）。
- **缺陷与信息性分开**：同族共享交付 NPC、`USE_OBJECT` 推报告页这类"看着不对但对得上客户端"的形态只留证，不进缺陷面。
- **配套**：普查脚本要把判据版本与每次修正的理由写在文件头注释里（本片 v1→v4 全部留档），否则下一轮会重复踩同一个坑。


## [QE-073] 七十一、审计的 owner 判据是**自洽性**不是**身份**：NPC 绑错人对审计 / 契约门 / 链指纹是三重盲区 (AUDIT_OWNER_IS_SELF_CONSISTENCY_NOT_IDENTITY)

<!-- pattern-metadata
status: CONFIRMED
scope: SimpleTalk 链登记表（及一切"页路由—按钮—owner"型派生登记）的 **owner 身份正确性**；审计门、客户端契约门、链指纹三者的可见面
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 阶段/报告流绑到了真端未声明的 NPC（判例 2482：二段与报告完成流绑 Andraste 278018 / Finne 278020，而真端表与客户端任务书正文只出现 Sereniti 204210 → Neusa 204224 → Moreinen 204211），但**改道前后审计都全绿**（12 行 → 13 行全达）、契约门 fatal 0、链指纹只报"该任务值变了"不报"错在哪"
root_cause: 审计的 owner 判据是**自洽性**——下发页的路由 owner 与页内按钮的路由 owner 相同、且每个按钮都有路由（`sameDialogOwner`）；它从不问"owner 是否等于真端声明的那个人"。契约门比的是页可达性与词汇、指纹是内容哈希：三者对"换了个人但结构不变"完全不敏感
fix_or_guardrail: 1. 身份轴只用**三源**判定：真端 `<acquired_npc_name>/<talk_npcK>/<reward_npc_name>` 声明 + 客户端任务书 HTML 正文人名计数 + `p0c10e` census 的 `DEVIATION` 列（`npc_check=XML_ONLY:*` 是同一轴的未普查面）；2. 修复必须落在**生成器**（新裁定表 code `LEGACY_STAGE_OWNER_DRIFT`，与首个 code 共用六轴 + 各自形态判据），落地判据 = 重跑逐字节相同 + 块爆炸半径 `CHANGED == 裁定行集` + 指纹 `changed == 该任务`；3. 守卫要**负例可证**：对每条 fail-closed 轴做变异测试（把非法值写进裁定表，断言生成器拦下 + 逐字节还原），因为"重跑逐字节相同"只证明**可复现**、不证明守卫会触发
evidence: .agents/summary/scriptdll-quest-driver/p0c44-2482-audit-pre.tsv; .agents/summary/scriptdll-quest-driver/p0c44-2482-audit-post.tsv; .agents/summary/scriptdll-quest-driver/p0c44-guard-probe.txt; .agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c44-stage-owner-drift-2482.zh-CN.md
validation: P0c-44 实测：审计 12 → 13 行全达且 owner 全部换回真端声明人、**未达页数不变**（即该轴对审计透明）；守卫负例 4/4 按预期拦下（code 域 / 非本 code 的漂移形 / 真端声明集内 / census DEVIATION 未记）；残留轴复算 `REWARD_NPC_UNWIRED 0 / LADDER_MISSING 0`（117 行真端多阶段行）；T1 与基线同名同条；T3 失败身份集 only-now=2 全归并发 lane、零条提及 2482
boundaries: 本卡只管 **NPC 身份轴**——owner 自洽性本身不是缺陷（QE-070 的物件哨兵、QE-051 的末段 var0 都合法）；不得据"名字看着不对"就改，必须凑齐三源证据；同名不同 id 与同 id 异名都要按 id 判；并发 lane 下切片编号会撞车（同轮出现两个"P0c-44"），唯一键是 **lane + 续片号**，产物文件名必须带任务号
superseded_by: none
first_check: 审计全绿却"看起来不对"时（客户端正文提到的人名在登记表里找不到，或登记表的 owner 在真端声明/正文里完全不出现），先跑 `npc_check=XML_ONLY:*` 全登记表普查定位身份漂移行；不要因为"审计绿 + 指纹只报 churn"就判定这个轴已完成
keywords: 审计盲区、owner 自洽性、sameDialogOwner、NPC 身份漂移、XML_ONLY、npc_check、契约门盲区、链指纹、LEGACY_STAGE_OWNER_DRIFT、2482、三源证据、守卫负例、变异测试
-->
- **一句话判据**：审计问的是"页与按钮的 owner 对不对得上**自己**"，不是"owner 是不是真端的那个人"——**身份正确性不落在任何自动门里**。
- **三重盲区的边界**：审计 / 契约门 / 指纹都能证明"没坏"，都不能证明"是对的"；身份轴只能靠真端声明 + 客户端正文 + census 偏差三源。
- **守卫要负例可证**：生成器"重跑逐字节相同"是**可复现性**证明；守卫的**可拦性**必须用变异测试单独证明（本片 4/4），否则 fail-closed 只是一句注释。
- **落地判据仍走 QE-071 那套**：新 code 同样要过"重跑相同 + 爆炸半径 == 裁定行集 + 指纹 changed == 该任务"。
- **并发约定**：切片号会撞车（两个 lane 同轮各发一个"P0c-44"）——用 lane + 续片号做唯一键，产物文件名带任务号，报告中显式声明本片编号。

## [QE-074] 七十二、生成物的"成员校验"列必须先**分通道展开**再比较：复合条目（`CLIENT:a,b`）与哨兵字面量（`SENTINEL:x`）会把 93/104 行判成缺陷 (MARKER_COLUMN_MUST_EXPAND_COMPOSITE_FORMS)

<!-- pattern-metadata
status: CONFIRMED
scope: 一切"逐行标记真端/客户端是否为权威"的生成物列（`npc_check`、`page_check`、owner/归属类标记）；判据来自把声明集写成一个字符串列的登记表
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 标记列报出的缺陷面比真实面大一个数量级（本片 `npc_check=XML_ONLY:*` 104 行里 **93 行**是假阳性，单任务最多误报 33 行），据此排期会把 6 个任务当成缺陷任务
root_cause: 声明集列有三种**异质形态**——纯 id、`SENTINEL:x`（类别哨兵，**不是** NPC）、`CLIENT:a,b`（**复合条目**：客户端通道解析出的多 id）。标记生成时用 `npc in set(column.split('|'))` 整串比较 ⇒ `'799800' not in {…, 'CLIENT:799800,799801'}` **恒真**，所有由客户端通道声明的 id 全部误标
fix_or_guardrail: 1. 装载时**分通道**建集合：真端通道（纯 id）与客户端通道（`CLIENT:` 展开、逗号切分）各存一份，哨兵字面量**剔除**；2. 标记域**三值化**并写明来源：`RETAIL_MATCH` / `CLIENT_MATCH` / 未声明（`XML_ONLY:<id>`），别再把两个通道混成一个 `MATCH`；3. 守卫判据（如"是否在声明集内"）用**两通道并集**——声明证据 = 真端 ∪ 客户端；4. 标记改动必须**不动 IR**：本片 93 行标记改动的指纹零漂移是"标记列不参与编译"的机器证明，改标记时把它当验收项
evidence: .agents/summary/scriptdll-quest-driver/p0c45_xml_only_npc_census.py; .agents/summary/scriptdll-quest-driver/p0c45-xml-only-npc-census-pre.tsv; .agents/summary/scriptdll-quest-driver/p0c45-xml-only-npc-census.tsv; .agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c45-extra-owner-prune.zh-CN.md
validation: P0c-45 实测：修复前 104 行（假阳性 93 / 生产面 6 / 证据面 5）；修复后 post 快照只剩 2 行证据面（生产面 0）；同批 93 行标记改动经链指纹 `set-diff: added=[] removed=[] changed=['35010','35011']` 证明零 IR 影响
boundaries: 本卡只管**标记列的口径**，不代替逐行裁定（假阳性清掉后剩下的行仍要按所在族的取证口径判）；`CLIENT_MATCH` 不等于真端声明——它只是客户端通道背书，涉及"谁该服务"时仍须真端声明 + 客户端正文/登记三源合证
superseded_by: none
first_check: 标记列报出的缺陷面若与直觉差一个数量级、或同一族的多个任务整批上榜，先打开生成器看**比较两侧的字符串形态**（是否复合条目/哨兵/前缀）——本片与 QE-072 是同一坑的两种表现（普查口径 vs 标记口径）
keywords: npc_check、标记列、假阳性、复合条目、CLIENT:、SENTINEL:、声明集展开、三值标记、RETAIL_MATCH、CLIENT_MATCH、XML_ONLY、QE-072、45024、35024
-->
- **一句话判据**：凡是"逐行打标"的生成物列，写之前先问**声明集有几条通道、每条的形态是什么**——整串比较必然把复合条目误判成"不在集合内"。
- **通道要显式**：真端通道 / 客户端通道 / 哨兵字面量三名分开存，别混进一个 `MATCH`；守卫取并集，标记保通道名。
- **验收项**：改标记列的任务，指纹必须**零漂移**（标记不参与编译）——本片 93 行改动 `changed` 只有 2 个 IR 真改的任务。
- **与 QE-072 的关系**：普查口径的假阳性来自"标签/通用事件名"，标记口径的假阳性来自"复合条目整串比较"——同属"先校准再排期"。
- **配套**：普查脚本把判据版本与每次修正理由写进文件头（本片 v1→v4），下一轮不重复踩坑。

## [QE-075] 七十三、形状裁定的**第三条通道 = 剪除**：删去真端与客户端都不声明的**多余 owner**，判据是"投影子集 + 元素越界"双守卫（SHAPE_CHANNEL_THREE_PRUNE_WITH_PROJECTION_GUARDS）

<!-- pattern-metadata
status: CONFIRMED
scope: 登记表/生成物里"某 NPC 服务了某动作"的记录与真端＋客户端声明集冲突时的处置；改写/改道/剪除三条通道的选型
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 遗留转写把交付流铺给了两人（Palas 799805 + Priamos 799806），真端与客户端四路只认一人；删错方向（剪掉声明 owner、或剪掉唯一服务某页的 NPC）会让客户端页变未达
root_cause: 三条通道的改写面不同——**改写**只搬局部行（`apply_talk_ladder`）、**改道**整块弃转写重合成（`canonical_resynthesis`）、而"结构正确、只是多了一个无背书的 owner"两者都不适用（改写没有该形状的搬行规则，改道会把对的 shape 也重造）
fix_or_guardrail: 1. 剪除只授权**多余形**：多余 owner 的（动作集, 下发页集）**投影 ⊆** 声明 owner 的（**覆盖守卫**）——这是"删了不丢页"的可判定判据；不满足即**替身形**（真干活的人在声明集外），必须走改道/重绑；2. **越界守卫**：多余 owner 在 XML 里只许出现在 `<dialog>` 与 `<npc-complete>`，出现在其它元素（item-report/can-act…）即越界，fail-closed 交人裁；3. 装载期另有 7 轴（code 域 / 与其它裁定表互斥 / owner 必须 RETAIL_TABLE / 多余 NPC ∉ 声明集（真端 ∪ 客户端）/ census DEVIATION 记过 / 客户端登记未背书 / 声明 owner 名 == 真端 `reward_npc_name` 且唯一）；4. 剪除后重编 R 序号保持 1..N，不留空洞；5. 裁定表必须写清**对立证据**（本片 = 遗留 XML 注释"任一报告"）——它是判"XML 错"的原始理由
evidence: .agents/summary/scriptdll-quest-driver/p0c45-extra-owner-decisions.tsv; .agents/summary/scriptdll-quest-driver/p0c45-blast-radius.txt; .agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c45-extra-owner-prune.zh-CN.md; .agents/summary/scriptdll-quest-driver/p0c45-35010-35011-audit-post.txt
validation: P0c-45 实测：35010 剪 4 R + 1 B（`B NPC_COMPLETE`）、35011 剪 2 R；爆炸半径 `CHANGED [8] / ADDED [] / REMOVED []`；审计前后同值（页集不变、IR 引用 799806 = 0）；指纹 `changed={35010,35011}`；报表规则在 2653/28805 上被判为**替身形**（`SETPRO1`/`SETPRO2` 只有声明集外的 NPC 在做）⇒ 未剪，登记为保留面
boundaries: 本卡不适用于**替身形**（走 QE-071 的改道 / 既有重绑通道）；也不适用于 XML 保留行（IR 属 XML，标记只是转写证据，无生产改动）；"多余"的判定必须来自**真端 ∪ 客户端**声明集，不得凭直觉或"看着重复"
superseded_by: none
first_check: 遇到"多了一个来源不明的 owner"时先在登记表里做投影差集：多余 NPC 的（动作, 页）是否被声明 owner 覆盖？覆盖 → 剪除；不覆盖 → 替身/结构问题，改道或重绑；越界元素 → 停下问人
keywords: 剪除、prune、多余 owner、替身形、覆盖守卫、越界守卫、LEGACY_EXTRA_DELIVERY_OWNER、改写/改道/剪除三分工、35010、35011、799806、Priamos、Palas、audit 前后同值
-->
- **一句话判据**：结构对、只多了一个没人声明的 owner —— 剪；**删掉会丢页**（投影不被声明 owner 覆盖）就是替身形，改道或重绑，不许剪。
- **两条守卫是硬门**：覆盖守卫（投影子集）与越界守卫（元素类型）必须都在生成器里 fail-closed，人工"看着像多余"不算判据。
- **三通道分工**：改写 = 局部搬行；改道 = 整块重合成；剪除 = 删多余 owner——选通道前先问"分歧在行/块/多余记录哪一层"。
- **对立证据要留档**：XML 侧若写了理由（如"任一报告"），它就是把 XML 判错的原始语境，必须进裁定表 basis。
- **收尾动作**：剪除后重编序号 + 爆炸半径 + 指纹 + 审计四件套（本片 `1a3386b2…` / `CHANGED [8]` / `changed={35010,35011}` / 13·6 行全达）。

## [QE-076] 七十四、跨通道守卫必须**类型/符号空间对齐**：str/int 混用会让"投影子集"守卫退化成「空集 ⊆ 空集」恒真（VACUOUS_GUARD_FROM_TYPE_MISMATCH）

<!-- pattern-metadata
status: CONFIRMED
scope: 生成器里任何"两侧投影比较"式守卫（覆盖守卫 / 集合包含 / 身份比对）；跨正则捕获组（字符串）与解析键（整数）的比较
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 守卫从不报错、看起来"总是通过"——即使被剪对象的投影明显不是子集；换一批数据、甚至把被剪对象换成任意 NPC，依然通过
root_cause: 一侧来自正则捕获组（`TR.group(4)` 是 str），另一侧来自解析函数（`resolve_npc()` 返回经 `int()` 的键，是 int）⇒ `tm.group(4) != npc_id` 恒真、两侧集合恒空；`empty <= empty` 恒真——守卫执行了，但从未比较过任何元素
fix_or_guardrail: 1. 守卫函数入口统一规范化类型（`npc_id = str(npc_id)`），并在注释里写明为什么；2. 必须证明守卫**非空执行**：准备一个"已知会触发"的校准快照跑出命中（P0c-46 用 `p0c43-registry-pre.tsv` 触发 `24123 DELIVER_UNCOVERED@204345`）；3. 修复后把守卫重跑到**旧裁定数据**上，确认结论不变（P0c-46 撤表重生成 = P0c-45 收口值逐字节相同）
evidence: .agents/summary/scriptdll-quest-driver/build_quest_client_talk_chain_steps.py（`_served` / `_deliver_proj` 的类型对齐注释）; .agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c46-role-narrowing.zh-CN.md §6.1
validation: P0c-46 实修：修前 35010/35011 的覆盖守卫两侧都是空集（恒真通过）；修后仍通过，且生成输出逐字节不变（撤表重生成 `1a3386b2b26a84c34f975e1da7725dbc51` = P0c-45 收口值）⇒ 旧裁定结论不受影响，但原轮"守卫通过"不构成证据
boundaries: 不限于 str/int——同号不同空间（NPC id / 页 id / 动作 id）、大小写差异、`npc_` 前缀别名、复合条目（见 QE-074）都是同一类坑
superseded_by: none
first_check: 新写"投影/集合包含"守卫时先打印两侧集合大小与元素；只要出现"两侧都空还判通过"，就按本节处理
keywords: 恒真守卫、vacuous guard、类型对齐、str/int、投影子集、覆盖守卫、校准正例、空集比较、守卫非空
-->
- **一句话判据**：守卫"总是通过"要先怀疑它在比较**空集**——打印两侧大小，别信返回值。
- **两个动作**：入口统一类型 + 用校准正例证明守卫真的命中过；缺一个都不算验证。
- **修复要回头复核**：守卫改动后重跑旧裁定数据，输出逐字节相同才算"结论不受影响"。


## [QE-077] 七十五、**角色轴**与**角色域收窄剪除**：标记只判"集合成员"不判角色，同一 NPC 服务了真端没给它的角色要用**角色域投影**判定（ROLE_AXIS_AND_ROLE_SCOPED_PRUNE）

<!-- pattern-metadata
status: CONFIRMED
scope: 标记列 / 审计 / 契约门 / 指纹都看不出的"角色扩散"——把交付（或接取、阶段）角色铺到链上每个 NPC；与 P0c-45 的**身份轴**（多余 owner）正交
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 链上每个 NPC（接取 / 阶段 / 报告）都带一份**逐字相同**的 `npc-complete` 块 ⇒ 玩家可在任意链上 NPC 交任务，而真端只认一个交付 NPC；审计与契约门全绿（审计判 owner 自洽、标记判集合成员，都不看角色）
root_cause: 遗留 XML 转写把交付角色复制给了每个链上 NPC；`npc_check` 只判 NPC 是否在名字集合内（QE-074/QE-075 口径），集合成员服务错角色不会被任何既有门禁标出（与 QE-073 的审计盲区同源）
fix_or_guardrail: 1. 判据必须**角色域**：先按动作/页归角色（DELIVER = `SELECT_QUEST_REWARD` / `CHECK_USER_HAS_QUEST_ITEM*` / `SET_SUCCEED` / `SHOW_SELECT_QUEST_REWARD_WINDOW*` / `SELECT5` / `SELECT6` / `B NPC_REPORT` / `B NPC_COMPLETE`；ACCEPT、STAGE 同理分域），再与**该角色的声明源**比（DELIVER = 真端 `reward_npc_name` ∪ 客户端 `end_npc_ids`）；2. 前置 = 声明**单值**（真端唯一解析 ∧ 客户端唯一 ∧ 相等），否则记 `DELIVER_DECL_UNRESOLVED` 不判；3. 剪除只剪**角色域记录**（`role_pruned` 剪块、`role_pruned_route` 只剪交付动作/交付页的 R 行）——接取/阶段记录必须原样保留，这是与整组剪除的本质区别；4. 覆盖守卫**角色域内**：被剪 NPC 的交付块参数逐字相同 + 交付动作/页集 ⊆ owner；5. 覆盖守卫要防空集恒真（QE-076）；6. 页语义先对齐真端：`SELECT5/SELECT6` 是**报告页族**不是阶段页
evidence: .agents/summary/scriptdll-quest-driver/p0c46_role_axis_census.py; .agents/summary/scriptdll-quest-driver/p0c46-role-narrowing-decisions.tsv; .agents/summary/scriptdll-quest-driver/p0c46-blast-radius.txt; .agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c46-role-narrowing.zh-CN.md
validation: P0c-46 实测：DELIVER 候选 38 项 / 22 任务 ⇒ **33 可剪**（块逐字相同 + 投影 ⊆ owner + 被剪 NPC 另有链上角色 7 接取/26 阶段）+ **5 保留**（`DELIVER_UNCOVERED` 自带 owner 没有的交付页）；登记表 5105→5071 行；爆炸半径删 33 块 + 2 条重复领奖窗 R 行 / 增 1（序号重编）；指纹 `changed={17 任务}`；净树 20/20（审计 17/17 绿 ⇒ 未孤立任何客户端声明页）；撤销裁定表重生成 = P0c-45 收口值逐字节相同
boundaries: v1「同名块在多个 NPC 上重复」（40 项假阳性：多 NPC 逐 NPC 对话绑定）与 v2（把 `SELECT5` 当阶段页，248 项假阳性）都是**已否决**的判据，不要重推；ACCEPT / STAGE 两个角色域**另立轴**（分别与接取入口轴、阶梯轴同域），不得与交付域混判；`DELIVER_UNCOVERED` 是结构不同（自带独有交付页）⇒ 不剪，逐项取证
superseded_by: none
first_check: 看到"每个 NPC 都带同一份块"先问三句：这份块属于哪个**角色**？该角色的**声明源**是谁（真端 ∪ 客户端）？声明是否**单值**？——三者齐了才谈剪
keywords: 角色轴、role axis、角色域投影、角色域剪除、交付角色、npc-complete 重复、reward_npc_name、end_npc_ids、DELIVER_UNCOVERED、LEGACY_ROLE_SPREAD_DELIVER、SELECT5 报告页族
-->
- **一句话判据**：NPC 在声明集内 ≠ 它该服务这个角色；判定必须**先按角色分域**，再与该角色的声明源比。
- **单值是前置**：真端 reward 与客户端 end 都唯一且相等才谈剪；多值/不可解一律 `UNRESOLVED`。
- **角色域剪除不是整组剪除**：只剪交付块与交付 R 行，接取/阶段记录原样保留——搞错会把阶段交互打死。
- **页语义先对齐真端**：`SELECT5/SELECT6` = 报告页族；把它们当阶段页会得到 248 项假阳性（v2 实测）。
- **收尾四件套**：爆炸半径（只删目标记录）+ 保真（撤表重生成 = 上一收口值）+ 指纹 changed 面 + 审计绿。

## [QE-078] 七十六、形状裁定的**第四条形态 = 行内令牌收窄**：分歧落在**记录内部的 after-commit 令牌**上时，改令牌不改行（IN_ROW_TOKEN_NARROWING_WITH_PAGE_COLUMN_SYNC）

<!-- pattern-metadata
status: CONFIRMED
scope: 登记表里某条记录的**角色投影**越界（名下有别人的页/动作），但这行本身属于该角色（阶段推进行）——剪除/改道都不适用时的处置与守卫
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: 阶段推进行（落到 REWARD 的 `SETPRO<k>`）自带 `SHOW_SELECT_QUEST_REWARD_WINDOW1` 下发 ⇒ 玩家在**阶段 NPC** 处弹出领奖窗（该 NPC 没有领奖选择路由 ⇒ 死窗）；角色域普查判 `DELIVER_UNCOVERED`（自带 owner 没有的交付页），但该行**不是**多余 owner 的块/行
root_cause: 遗留 XML 把"跨到 REWARD 的那一步"当作交付步写：阶段推进与交付（领奖窗）两个角色被压进**同一条记录**——所以分歧既不在块层（改道会把对的阶段形状重造）、也不在记录层（删行会丢掉阶段推进本身），而在**行内令牌**层
fix_or_guardrail: 1. **先分层再选通道**：块层 = 改道（弃转写重合成）、记录层 = 剪除（多余 owner）、**行内层 = 令牌收窄**（本卡）；2. 令牌域与形状**逐字**断言：`after-commit` 必须是 `SYNC:<mode>;<窗外溢令牌>`（窗外溢唯一且在末位）+ `target` 节点投影 = REWARD，改写 = 末位令牌换 `CLOSE`（SYNC 模式逐字保留）；3. **页列同步**：改写后必须同时清掉该行 `page_check` 里的同页条目——否则普查/审计仍把该页记在越界 NPC 名下 = 收窄未生效（自证伪点）；4. **无页丢失守卫**：改写后该任务仍有 **owner 名下**开领奖窗的记录（`B NPC_COMPLETE` 的 `preview` 非 `-`，或 owner 名下记录含该页令牌）；5. 声明**单值**前置：真端 `reward_npc_name` 唯一解析 ∧ 客户端 `end_npc_ids`（若登记）== 它 ∧ ≠ 阶段 NPC；6. 阶段归属轴：`action = SETPRO<k>` 必须对上真端 `talk_npc<k>`（全局普查：`talk_npcK` 顺序 == 客户端 `progress_npc_ids` 顺序 **55/55**）；7. 裁定表条目必须**全部命中**转写行（陈旧表 fail-closed）；8. 同片新增的第三种改道 code `LEGACY_DELIVER_FLOW_ON_TALKNPC`（真端 `reward == acquired` ∧ 转写交付 owner ∈ `talk_npcK`）是块层对照形——同一批残留按"分歧在哪层"分派两条通道
evidence: .agents/summary/scriptdll-quest-driver/p0c47-stage-window-spread.tsv; .agents/summary/scriptdll-quest-driver/p0c43-canonical-resynthesis.tsv; .agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c47-deliver-uncovered-closure.zh-CN.md; .agents/summary/scriptdll-quest-driver/p0c47-blast-radius.txt; .agents/summary/scriptdll-quest-driver/p0c47-audit-post.txt
validation: P0c-47 实测：登记表 5071→5076 行（`ef2cc1a5…`→`395f4016…`）、爆炸半径 5 块 `ADDED/REMOVED []`、指纹 `changed={2964,11106,21036,39003,49003}`；**独立判据复算**：`DELIVER_UNCOVERED` 5→0 行（INFO 两轴 28/34 逐字不变）；审计前后：2964 的未达页 1352/1353 **闭合**、领奖窗路径从 `NPC 278137` 改到 `NPC 204253`，11106/21036 的领奖窗**只**从阶段 NPC 上消失（`s1 + 798979/798713 + 10001` 路径消失、owner 路径仍在），39003/49003 审计行逐字不变；净树 20/20（审计 17/17）、保真 422519 字节 + 21 表零漂移；T1/T2 失败全归因并发 lane（本片 5 id 零命中）
boundaries: 不适用于**缺页**（那要补行，不是收窄）；不适用于 owner 侧**没有**开窗记录的任务（会丢页 ⇒ 先补 owner 侧再收窄）；不适用于阶段页**归属本身错位**（如阶段入口页缺失、页挂在接取 NPC 上——那是阶梯轴，另裁，见 `STAGE_PAGE_OWNER_SPREAD_PENDING`）；`HAS_ITEM` 门是否撤除属真端 `<item_check>` 轴（`GATE_VS_ITEM_CHECK_PENDING`），与令牌收窄分开判
superseded_by: none
first_check: 看到"某 NPC 名下有别人的页"先问：分歧在**块/记录/行内**哪一层？行内层 ⇒ 只改令牌 + 页列同步 + 无页丢失守卫；再核对声明是否单值、阶段归属是否对上 `talk_npc<k>`
keywords: 行内令牌收窄、in-row narrowing、STAGE_REWARD_WINDOW_SPREAD、阶段推进行、领奖窗外溢、页列同步、无页丢失守卫、LEGACY_DELIVER_FLOW_ON_TALKNPC、2964、11106、21036、39003、49003、结束对话、通道分层
-->
- **一句话判据**：分歧只在**行内令牌**（页下发）里、而该行本身属于该角色 ⇒ 改令牌不改行；剪了会丢页、改道会把对的形状重造。
- **页列必须同步**：after-commit 改完但 `page_check` 还留着那一页 = 收窄未生效——普查/审计的判据是**页列**，不是你想改的那一行。
- **无页丢失是硬门**：owner 侧必须仍有开窗记录（preview 或 owner 名下页行），否则 fail-closed。
- **通道分层**：块层改道 / 记录层剪除 / **行内层收窄**——先定位分层再动手，是这一族（QE-071/075/077）连续三片的主线。
- **客户端按钮文案是形状证据**：阶段推进按钮「结束对话。」= 阶段只关窗（⇒ `CLOSE`），领奖窗归 owner；页正文（谁在说话）能直接定页的归属。

## [QE-079] 七十七、**阶段页轴**：页链**入口页**丢失要补行、**跨 NPC 扩散**剪行必须只剪「页动作行」——对话入口行（`QUEST_SELECT`）删了任务就不可接（STAGE_PAGE_ENTRY_INSERT_AND_PAGE_ROW_ONLY_PRUNE）

<!-- pattern-metadata
status: CONFIRMED
scope: 登记表里**阶段页链的页**（入口 `SELECT{k+1}` / 续页 `SELECT{k+1}_x`）被下发在**非本阶段 NPC** 上，或入口页**从未被下发**时的裁决与守卫
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: ①玩家看不到阶段对话起点：审计 `CLIENT_PAGE_UNREACHED <入口页>`（入口没被任何行下发，续页因页链断开而一并不可达）；②同一页在**接取 NPC**（`unaccepted` 相位）/ **别的阶段 NPC** / **交付 owner** 上被额外下发 ⇒ 阶段对话出现在错误的 NPC 上
root_cause: 历史转写丢入口行（只留续页 + 推进）；以及"页视图按 NPC 铺开"的转写习惯——XML 把阶段页链的页同时挂在链上每个 NPC 与接取角色上（真端 `talk_npc<k>` + 客户端页链才是归属权威）
fix_or_guardrail: 1. **普查先于动手**（一行一个「任务×阶段×页链页」的三态事实）：①阶段集合必须取 **`SETPRO<k>` 推进行**，取 `s{k}` **节点**会漏**末阶段**（末阶段 dst 是 `reward`）与**压平形单阶段任务**（无 `s1` 节点）——本片首版即此假绿；②页名有**前缀关系**（`SELECT2 ⊂ SELECT2_1`）⇒ 必须**精确令牌**比对（`;` 切分后整 token），子串匹配会把续页行当成入口页的 owner（本片第二个假绿）；2. **两条通道**：`STAGE_ENTRY_MISSING` ⇒ 在阶段 NPC 上补 `QUEST_SELECT` 入口行（同阶梯通道形状：`DIALOG:SHOW_QUEST_PAGE:<入口页>` + `<页>=CLIENT`）；`STAGE_PAGE_OWNER_SPREAD` ⇒ 剪越界行；3. **只剪页动作行**（本片关键取舍）：`QUEST_SELECT` 行是被剪 NPC 自己的**对话入口**（接取/报告窗）——删了该 NPC 的对话整条消失、任务不可接；只剪 after-commit 下发该页的**页动作行**，入口行保留并**逐条登记**（转 `ACCEPT_ENTRY_PAGE_WRONG_PENDING`：入口行下发的应是接取入口页而非阶段页）；4. 七轴 fail-closed：code 域；与**块级五通道**（阶梯/接取入口/改道/多余 owner/角色收窄）互斥；owner 必须 `RETAIL_TABLE`；**owner == 真端 `talk_npc<k>` 唯一解析**；页必须在**客户端该阶段页链**上（入口 →（按钮动作落页号共号空间）→ 续页 → 带 `HACTION_SETPRO{k}` 的末页）；越界者角色可判（客户端 `start`/`end`/`progress` 序位 ≠ k，或真端别的 `talk_npc<j>`）；5. 收尾双守卫：裁定表**每条必须命中/插入**（未消费 = 表腐化）+ **无页丢失**（剪除后每页仍由 owner 名下记录下发）；6. **IR 惰性要诚实记录**：SystemGrant 起手任务（`unaccepted` 相位 IR 只有一条 `SystemGrant → START/0` 边）的 `unaccepted` 行本就不编译 ⇒ 那里的剪行是登记表形态清理，指纹不动**不是**取证失败
evidence: .agents/summary/scriptdll-quest-driver/p0c54-stage-page-census.tsv; .agents/summary/scriptdll-quest-driver/p0c54-stage-page-decisions.tsv; .agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c54-stage-page-axis.zh-CN.md; .agents/summary/scriptdll-quest-driver/p0c54-ir-pre.txt; .agents/summary/scriptdll-quest-driver/p0c54-audit-post.txt
validation: P0c-54 实测：普查 404 项中 48 项不合形（入口缺失 2 / 跨 NPC 扩散 44 / 客户端缺页 1 / 页链断裂 1）；落地 5076→**5052** 行（`395f4016…`→`43e37883…`，419727 字节）、爆炸半径 18 任务 `ADDED/REMOVED []`（净 -24 = 26 剪 + 2 插）；**IR 逐行对拍仅 8 行变更**（pre/post 各跑一次探针，pre 用 `mvn -o -B surefire:test` 直跑避免 `process-resources` 覆写 `target/classes`），owner 侧该页**恰好保留 1 条**（无页丢失）；**审计未达/EVIDENCE 10 → 6**（闭合 39003/49003 各 2 行、零新增）；指纹 `added=[] removed=[] changed=13` → `c9657153…`；净树 20/20、保真 22 表零漂移、T1 1F（在册 lane 20035）、T2 7F 全归因在册/全量基线既有
boundaries: 不适用于**推进行本身**挂在非声明 NPC 上的任务（那是阶段归属/阶梯轴：`STAGE_PAGE_ADVANCE_NPC_DRIFT_PENDING` 9 任务）；不适用于客户端页册就没有该页的任务（真端数据缺口，如 21033 阶段 2）；不适用于页链断裂（80320）；补行只对"入口被丢、续页与推进仍在"的形状成立（页链首页且 `len(chain) ≥ 2`）
superseded_by: none
first_check: 看到"阶段页挂在错的 NPC 上"先分类：**入口页从未下发**（补行）还是**页被铺到别的 NPC**（剪行，只剪页动作行）；动手前先确认该 NPC 的 `QUEST_SELECT` 入口行是否会被误删（删了任务不可接）
keywords: 阶段页轴、STAGE_ENTRY_MISSING、STAGE_PAGE_OWNER_SPREAD、入口页补行、页动作行、QUEST_SELECT 入口行、精确令牌匹配、SETPRO 推进行取阶段、无页丢失守卫、IR 惰性、SystemGrant、surefire:test 直跑、39003、49003、11106
-->
- **一句话判据**：阶段页链的归属 = 真端 `talk_npc<k>` + 客户端页链；入口页**缺**要补行，页被**铺到别人**剪行——但**只剪页动作行**。
- **两个自伤假绿**（普查层）：①`SELECT2 ⊂ SELECT2_1` ⇒ 精确令牌；②阶段集合取 `SETPRO<k>` 行，不取 `s{k}` 节点（末阶段/压平形没有 `s{k}`）。
- **对话入口行不可删**：`QUEST_SELECT` 是被剪 NPC 自己的对话门，删了该 NPC 对话整条消失、任务不可接（本片首版实测踩到）。
- **pre/post 对拍要绕资源阶段**：`mvn test` 会 `process-resources` 覆写 `target/classes`（把 pre 态覆盖成 post）⇒ 用 `mvn -o -B surefire:test` 直跑，并 md5 自检 pre/post 两次的 `target/classes` 内容。
- **指纹不动 ≠ 没修**：SystemGrant 起手任务的 `unaccepted` 行本就 IR 惰性——用 IR 行级对拍 + 审计前后差给出真实变化面，别用指纹当唯一判据。

## [QE-080] 七十八、**阶段腿参数塌缩**：多阶段任务的「每条腿都叫 `SETPRO1`、每个阶段都下发 `SELECT2`」是转写模板没按阶段参数化——按真端 `talk_npc<k>` + 客户端页链**逐阶段复原**（STAGE_LEG_PARAM_COLLAPSE_REBUILD）

<!-- pattern-metadata
status: CONFIRMED
scope: 登记表里多阶段任务（真端 `talk_npc1..K` + 客户端 K 段页链）的阶段腿**参数**被压成第一阶段值时的裁决、重建与守卫
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: ①阶段 2..K 的客户端页（`SELECT{k+1}`/`SELECT{k+1}_1`）与领奖页（`SELECT5`）在审计里 `CLIENT_PAGE_UNREACHED`（页链从未下发）；②玩家侧：K≥2 时阶段推进行把 var0 设回 1（`SETPRO1` 名字塌缩）⇒ 后续阶段的页/交付状态机**不可达**（任务链中途死）；③越界：同一组阶段页/腿行同时挂在接取 NPC 与领奖 NPC 上
root_cause: XML 期「对话链按客户端按钮图重建」的重建模板未按阶段参数化——K 条腿全写 `action="SETPRO1"`、每阶段全下发 `SELECT2`/`SELECT2_1`（HEAD 版退役 XML 即此形）。生成物纪律下**不能直接改登记表**：必须回到生成器 + 裁定表 + fail-closed 轴，否则下次重生成静默回退
fix_or_guardrail: 1. **腿的锚定 = owner 优先**：阶段 k 的腿 = `owner == talk_npc<k>` 且逐跳相接（`src` = 前一跳 dst，首跳 `src == started`，末跳 dst 节点 packed == K）；**别用页名/动作名自证**——它们正是被塌缩的那两项；`talk1 == talk3`（2538 回访）、`reward == talk1`（11010）是合法重复，按**链位**消歧；2. **页行按动作类分派**：页名动作行（`SELECT\d+(_\d+)?`）= 续页行（动作+页 → `SELECT{k+1}_1`）、其余（`QUEST_SELECT`/`USE_OBJECT`）= 入口页行（页 → `SELECT{k+1}`）——同阶段可有**两条入口行**（1394 `actions="USE_OBJECT QUEST_SELECT"`，两条交互路线都对）；3. **领奖行三形互斥**：①已正确（1323）②塌缩（领奖 NPC 的 `QUEST_SELECT` 行下发阶段页 ⇒ **改写其页**）③缺（在 **packed == K 的 `REWARD` 节点**上**插入**一条 `QUEST_SELECT → SELECT5`）——注意有的任务 reward 节点 packed 是 K+1（1323），该形不适用；4. **节点名不必归一**：节点名是内部标识、IR 指纹按 `(status, packed)` 取键 ⇒ 改名是 IR 惰性的（本片保持 `v1`/`stage1` 原样，最小改动面）；5. **通道共存域**：与**接取入口**表可共存（接取段 vs 阶段/领奖段，页/动作不相交；1323 同时受两表裁定），与**阶梯**表互斥（两者都重写阶段段）；6. 越界剪除同 P0c-54 域（阶段页非 walk 归属行 + 非腿链/非领奖窗的 `SETPRO` 行），**接取页行（`SELECT1*`）一律不动**；7. **验收三件套**：普查（pre 缺陷 → post 全绿，逐阶段 owner 页序列 == 客户端页链）+ 审计未达行数（本片 34 → 0，全表 1348 → 1314 且 `ONLY_POST = 0`）+ IR 逐行对拍（改动面 == 缺陷面）；8. 收尾双守卫：逐阶段页 owner 集 == `{talk_npc<k>}`、领奖页 owner 集 == `{reward NPC}`
evidence: .agents/summary/scriptdll-quest-driver/p0c55_stage_leg_census.py; .agents/summary/scriptdll-quest-driver/p0c55-stage-leg-decisions.tsv; .agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c55-stage-leg-rebuild.zh-CN.md; /Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs/QUEST_Q<id>.html（阶段按钮 `HACTION_SETPRO{k}` + 领奖页 `HACTION_SELECT_QUEST_REWARD`）
validation: P0c-55 实测：9 任务（1323/1394/1484/2480/2538/3093/4052/11010/11103）普查 pre `{OK 6, REPORT_OK 1, 缺陷 24}` → post `{OK 22, REPORT_OK 9}`；爆炸半径 `CHANGED` 恰 9 任务 / `ADDED/REMOVED []`（净 +1 行：8 改写 7 剪 1 插）；审计 9 任务未达 34 → **0**、全表 1348 → 1314（净 −34，零新增）；IR `-49/+49` 行全落在三类靶面（阶段 2/3 页路由、阶段 2/3 腿 dialogId、领奖页 2375）；冻结指纹 9 值重冻（`c9657153…` → `aaf2b9c0…`，added/removed 空）；保真逐字节 + 23 表零漂移；净树 20/20、T1 1F（在册 lane 20035）、T2 4F 全既存；T3 2013 例 117F/22E/1S（`gates/T3-221828.log`）对装入前两基线 `T3-212101`/`T3-213222` 的失败身份集**逐条相同**（各 139 条失败行 / 107 唯一身份，`only_in_base = []`、`only_in_post = []`）⇒ 零新增零消失
boundaries: 只服务**无收集段**的多阶段链（真端 `collect_progress` 缺席：赠礼/传话链）——有收集段的走 **阶梯轴**（`apply_talk_ladder`，K 段 var0 投影 + 收集/交付段迁移）；只支持**两页阶段链**（入口 + 续页；`len(chain) != 2` 即 fail-closed）；不改节点结构（末阶段腿落 `REWARD/K` 是 XML 形，客户端行显示与 packed 值一致，不需要补 `sK` 节点）；接取侧扩散（`SELECT1_1` 铺到非接取 NPC）属另轴，不在本通道
superseded_by: none
first_check: 看到"多阶段任务子阶段对话/页进不去"，先查**推进动作名是否逐阶段不同**（`SETPRO{k}`）与**页是否逐阶段不同**（`SELECT{k+1}`）——两项都塌缩成第一阶段值时不要逐行手改登记表，按 owner + 客户端页链重建并落进生成器
keywords: 阶段腿塌缩、SETPRO{k}、SELECT{k+1} 页链、talk_npc<k> owner 锚定、页行动作类分派、领奖行三形、packed K、任务书行 K+1、rebuild_stage_legs、11010 FOBJ 阶段、2538 talk1==talk3、1323 领奖在 START/K
-->
- **一句话判据**：阶段腿 = `owner == talk_npc<k>` + 客户端 `SELECT{k+1}` 页链 + 动作名 `SETPRO{k}`；塌缩形（全 `SETPRO1`/全 `SELECT2`）要按阶段重建，**别逐行手改**（生成物会回退）。
- **领奖行三形**：已正确 / 塌缩改写其页 / 缺则插在 **packed == K 的 `REWARD` 节点**上（reward 节点 packed 可能是 K+1，那形不适用）。
- **owner 优先于页名**：页名与动作名正是塌缩项，拿它们自证会得到循环论证；用真端 `talk_npc<k>` + 客户端任务书第 k 行（K+1 行契约）锚定。
- **合法重复**：`talk1 == talk3`（回访）、`reward == talk1`（同 NPC 接取+领奖）都常见——按链位/状态消歧，别当成重复行剪掉。

## [QE-081] 七十九、**接取入口角色扩散（块级根因）**：链上每个 NPC 各有一个 `NPC_START` 块 = 遗留 XML 把真端步骤行误读成起始行——接取 owner 用**双侧唯一对拍**收窄，剪块不剪页（ACCEPT_ENTRANCE_ROLE_NARROW）

<!-- pattern-metadata
status: CONFIRMED
scope: 登记表接取侧「接取页/接取流被铺到非接取 NPC」（`SELECT1*`/`NPC_START` 块 / `R:unaccepted` 行）的普查、owner 判定与守卫剪除
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: ①接取窗续页 `SELECT1_1` 等出现在链上步骤 NPC/领奖 NPC 的行里；②客户端任务书里接取相位**只有 owner 一行有按钮**，其余 NPC 的"接取按钮"在客户端不存在；③审计里这些行全 `PAGE_ACTION_MATCHED`（非致命）⇒ 只看致命数会漏掉整面
root_cause: **块级**：遗留 XML 把真端步骤行（`talk_npc<k>`）误读成起始行、每链 NPC 各写一个 `<dialog type="NPC_START">` 块 ⇒ 编译器 `acceptFlowChain` 每块合成一条接取流，链上每个 NPC 都成接取人（HEAD `1484.xml` 自证：`NPC_START npc-id="204045"` 紧贴注释 `retail 步骤0:type="TALK" ids="204045"`）。行级普查的 34 项只是**投影**，根因在块级——先复算登记口径（RECOMPUTED == REGISTERED，QE-072），再下钻块级定根因
fix_or_guardrail: 1. **owner 判定 = 双侧唯一对拍**：真端 `acquired_npc_name` == 客户端模板索引 `start_npc_ids`（恰一员）；**块级 `B NPC_START` 不是 owner 权威**（链上每 NPC 一块，拿它当 owner 会把扩散面判成 0）；客户端 `start_npc_ids` 可为空（35017/45010/45017 `_faction_` 系）⇒ 该面暂缓不硬裁；2. **六守卫 G1..G6 fail-closed**：owner 双侧唯一且相等 / 被剪 NPC 是链上 NPC 且 ≠ owner / 接取投影 ⊆ owner / owner 侧有接取族行（剪完仍可接取）/ 不得剪 `QUEST_SELECT` 入口行 / 只剪 `source == unaccepted`（运行期相位不碰）；3. **剪块不剪页**：缺陷单位是 `NPC_START` 块（37 块/18 任务）+ 其下的 `R:unaccepted` 接取族行（15 条/8 任务），阶段族页行原样保留；逐行过滤再叠 `source != unaccepted` 即拒；4. **收口守卫**：剪后每任务恰剩一个 `NPC_START` 块（= owner），否则 fail；5. **装载轴**：与既有接取入口表（P0c-42）互斥、retention==RETAIL_TABLE、真端名字/ID 逐字对拍、每任务恰一 owner；6. 与 P0c-55 阶段轴的**越界剪除**正交（那边"接取页行一律不动"是怕误伤本轴，本轴有 G1..G6 后才动）
evidence: .agents/summary/scriptdll-quest-driver/p0c57_accept_entrance_census.py; .agents/summary/scriptdll-quest-driver/p0c57-accept-entrance-decisions.tsv; .agents/summary/scriptdll-quest-driver/p0c57-accept-axis-census.tsv; .agents/summary/scriptdll-quest-driver/reports/2026-09-26-P0c57-accept-entrance-role-narrowing.zh-CN.md; src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv
validation: P0c-57 实测：登记口径 34 项 / 25 任务复算逐项相等；块轴普查 27 任务（18 重复入口 37 块 / 1 变体差 2266 / 5 owner 缺失 / 3 `_faction_` 未解）；爆炸半径恰 18 任务 `added=0 removed=52`（37 块 + 15 行，5052→5000 行，`5bf47fde…` → `0ae2d924…`）；指纹重冻 18 值（声明面 == 实测面 == 裁定表三面互证，267 行不动）；IR −353/+0；审计 `unreachedOrEvidence=10` pre/post 同值；保真逐字节 + 24 表零漂移；净树 20/20；T1 75 例 1F（在册 lane 20035）；T2 97 例 7F（1514×5/15613×1 六身份逐字见装机前基线 ⇒ 车道既有）；T3 2015 例 117F/22E/1S（`gates/T3-230841.log`）对 P0c-55 基线身份集 105==105 ADDED 0 / REMOVED 0
boundaries: 只剪**接取相位**（`source == unaccepted`）的接取族块/行——入口行错位（`ACCEPT_ENTRY_PAGE_WRONG_PENDING`：`QUEST_SELECT` 下发阶段页 7 条）其 `source != unaccepted`，G6 剪不到，另轴；owner 无法双侧解析的面（`_faction_` 声明未解 12 任务、owner 缺失/变体差 5 任务）暂缓带原因留阻塞表；本轴不碰阶段页/领奖页（P0c-54/55 域）
superseded_by: none
first_check: 看到"接取对话出现在多个 NPC 上"，先分**块级**（`NPC_START` 块数 > 1）还是**行级**（块唯一但行扩散）；owner 一律真端 `acquired_npc_name` × 客户端 `start_npc_ids` 双侧对拍，别拿块计数当 owner；判据先在登记时刻的坏快照上触发（QE-072）
keywords: 接取入口扩散、NPC_START 块级根因、acceptFlowChain、acquired_npc_name、start_npc_ids 双侧对拍、G1..G6 六守卫、source=unaccepted、剪块不剪页、P0C57-ACCEPT-KEEP、1484 自证注释、_faction_ 暂缓、35017 start_npc_ids 空
-->
- **一句话判据**：接取 owner = 真端 `acquired_npc_name` × 客户端 `start_npc_ids` **双侧唯一且相等**；每链 NPC 一个 `NPC_START` 块是遗留误读的块级根因，剪块（守卫后）而非逐行改页。
- **两个坑**：①块级 `B NPC_START` 不是 owner 权威（每 NPC 一块 ⇒ 拿它当 owner 扩散面恒 0）；②客户端 `start_npc_ids` 可为空（`_faction_` 系 35017/45010/45017）⇒ 无法双侧对拍的面**暂缓不硬裁**。
- **G1..G6 六守卫**里 G5/G6 是安全底线：不剪 `QUEST_SELECT` 入口行、只剪 `source == unaccepted`——保证运行期接取相位之外零影响。

## [QE-082] 八十、**契约门行走器必须协议忠实**：页上下文是包语义的一部分——1009 冷发被真实包路径拒绝、`-1` 是 CM_SHOW_DIALOG 开对话、页 10 残留污染动作语义（LIFECYCLE_CONTRACT_GATE_PROTOCOL_FAITHFUL_WALKER）

<!-- pattern-metadata
status: CONFIRMED
scope: 真端表驱动任务的黑盒生命周期契约门（接取→进度→交付→结算）与一切"图引导 + 真实协议原语"的行走器/测试驱动
first_seen: 2026-09-26
last_verified: 2026-09-27
symptom: ①行走器按 IR 边"冷发"对话动作，`handled=false` 且 `matchedRouteResult=UNKNOWN`，但直接构造同值快照调 `QuestMutationPlanner.plan` 却通过（planner 四关全过）⇒ 排查时极易误判成前置条件/事实族问题；②错误归因组合 `matchedTransition≠null` 且 `matchedRouteResult=UNKNOWN` 只能由 `attributeProtocolResult`（真实 CM 包路径）产生，是"请求走了协议回环而非运行时桥"的指纹；③自动 BFS 规划器报 `production graph has no client-reachable path to a COMPLETE node` 且 `node=<起点>, page=0`——这是 `firstBlocker == null` 时的**兜底文案**（真实卡点是某页上 `visibleActions` 为空且无原生回退导致的静默死端），别按字面去查起点节点
root_cause: `QuestProtocolLoop` 除 WORLD_EVENT 外全部走**真实 CM_DIALOG_SELECT/CM_SHOW_DIALOG 包路径**，包里携带的 `lastPage`（当前页）与动作 id 共同决定语义：①`SELECT_QUEST_REWARD(1009)` 必须携带报告页上下文（1352/2375/10002）才路由，冷发被拒；②上一步 afterCommit 停在**页 10（SELECT_QUEST）**时，后续非 31 动作被误读成"任务列表页行点击"（DD 链 SETPRO2 冷发 UNKNOWN 的根因）；③`TalkToNpc(npc,-1)`（USE_OBJECT）是 CM_SHOW_DIALOG 的开对话语义，用 CM_DIALOG_SELECT(-1) 发不出
fix_or_guardrail: 1. **行走器策略**：QUEST_SELECT 用 interact(31) 开对话；-1 用 useObject；其余动作先保证有目标 NPC 的对话页上下文（换 NPC 或页 10 残留时先开对话），再点当前页可见按钮，页上不可见才退回冷发；2. **接取提交两形态**：询问窗形（1011 页 1007→页 4→1002）与直接接取形（入口页可见 20000）——提交边按"当前页可见"挑选，禁止 findFirst 取首条（1002 恒在转移表前部，直接接取形会错过可见的 20000）；3. **进度边按序尝试全部**：配对狩猎（Cradle 形）节点上"条件门控推进边"与"计数边"并存，取第一条会在另一半未杀时全部落空；且 DD 链对话步骤是**双协议注册**（TalkToNpc + QuestDialog 两事件空间都要进进度边过滤）；4. **进度边放宽为"推进到新节点的事件"**：EnterZone/LevelUp 等场景推进也是进度（18996 s3→s4 就是 EnterZone）；5. 契约合同允许 kill-flip（末杀直达 REWARD）与交付时翻转并存，但进度期间不得 COMPLETE；6. 奖励窗断言用 `rewardWindowForTier` 查表（QE-028），接取窗页 4 与 ASK_QUEST_ACCEPT 动作 1007 分属页/动作两角色（QE-017 共号空间）；7. **自动 BFS 规划器（`QuestProductionJourneyPlanner`/`QuestProductionJourneyExecutor`）必须与行走器同口径建模客户端全局原生窗口**：页 5 奖励窗（8..23）与页 4 接取窗（1002/20000）都不登记在任务页表里，页上零可见按钮时按原生控件回退（`StepKind.NATIVE_ACCEPT_ACTION` → `journey.interact(npcId, action)`，与契约行走器的页 4 原语逐字一致）；但**任务自己的 HTML 登记了该页按钮时禁止回退**（1103 页 4 只声明接受 1002/拒绝 1003，若按动作逐个兜底，BFS 会走客户端根本不会发的 20000 并改道既有计划形状锁）
evidence: src/test/java/com/aionemu/gameserver/questEngine/e2e/RetailQuestContractTest.java; .agents/summary/quest-native-dispatch/README.zh-CN.md; .agents/summary/quest-native-dispatch/gates/contract-run1~13.log（运行1=1绿7红→运行13=8绿）; src/test/java/com/aionemu/gameserver/questEngine/e2e/client/QuestProtocolLoop.java（dispatch 分流与 attributeProtocolResult）; src/test/java/com/aionemu/gameserver/questEngine/runtime/QuestE2eRuntime.java（beginRequest/attributableTransitions/snapshot）; src/test/java/com/aionemu/gameserver/questEngine/e2e/journey/QuestProductionJourneyPlanner.java; src/test/java/com/aionemu/gameserver/questEngine/e2e/journey/QuestProductionJourneyExecutor.java; .agents/summary/quest-native-dispatch/gates/T3-105219.log
validation: RetailQuestContractTest 8 家族代表（SimpleHunt 1112/30715、SerialHunt 13918、SimpleTalk 1118、ItemPlay 13704、DD 15042/15546/18996）全生命周期全绿；每条根因都有"A/B 对拍"证据：1118 的 1009 在 IR/构造快照/e2e attributableTransitions 三通道全通过、仅真实包路径拒绝 ⇒ 页上下文归因；18996 的 SETPRO2 在页 10 残留时 UNKNOWN、换页后通过 ⇒ 页 10 归因；2026-09-27 规划器侧复验：DD 尾片规范形把接取落在页 4 后 25670/28931 由"无可达路径"转绿，首版逐动作兜底被 T3 抓出 1103 计划改道（1103 页 4 登记 1002/1003）⇒ 收窄为"该页零登记才回退"，T2（228 id）新增失败 0、T3 ADDED 0
boundaries: 契约门 v1 只覆盖 NPC 接取形（世界/系统发放形如 SimpleUseItem 1107 无 NONE 态 1002 边，不适用）；collect 族进度（背包事实种子）未纳入；行走器只锁生命周期合同不锁页链——页链形状改造（规范形）前后必须同绿；规划器同理（BFS 找到的是"一条真实可达路径"，不等于唯一路径：新增原生原语会改变选路，任何**计划形状锁**都可能被更短路径改道 ⇒ 加原语时必须跑全量计划形状类并逐条判定"改道是否合法"）；收窄后失败方向是**安全侧**——页 4 若只登记拒绝键（1003）而无接受键，规划器停在页 4 报 `no client-reachable path`（红测试），不会发明按钮；当前目录无此类任务（2026-09-27 T3 ADDED 0）
superseded_by: none
first_check: 行走器步进"planner 说行、真实路径说不"时，先看**包里的 lastPage 是什么**（上一步 afterCommit 落在哪页）与动作 id 是否页上可见，再怀疑前置条件；看到 `handled=false + matchedTransitionCandidates=[]` 先确认请求走的是运行时桥还是真实包路径（两者语义不同）；规划器报"无可达路径"时，先拿 `ClientResourceOracle.visibleActions(questId, 当前页)` 核对卡点页是否零可见按钮——页 4（接取窗）与页 5（奖励窗）对多数任务都不在任务页表里
keywords: 协议忠实、页上下文、1009 冷发、CM_SHOW_DIALOG、dialogId=-1、USE_OBJECT、页10残留、SELECT_QUEST、接取两形态、20000 直接接取、双协议注册、QuestDialog 步骤、配对狩猎、try-in-order、kill-flip、交付时翻转、RetailQuestContractTest、BFS 规划器、NATIVE_ACCEPT_ACTION、页4接取窗、可见性优先
-->
- **一句话判据**：真实 CM 包路径里"动作 id + 当前页"共同决定语义——行走器每个动作前先问"客户端此刻真的停在那个页上吗"，页上下文不对就先重开对话。
- **三个坑**：①1009 冷发必拒（要先拿报告页）；②-1 是 useObject 的活，不是 dialog-select；③页 10 残留会把后续动作误读成任务行点击。
- **契约门设计不变式**：只锁生命周期四相位合同、不锁页链——形状手术（规范形改造）前后同绿才是安全网。
- **规划器侧同源（自动 BFS journey）**：契约行走器与 BFS 规划器是同一份"页上下文"语义的两个消费者——页 4/页 5 这类客户端全局原生窗口两边都要建模，且**可见性优先**（任务 HTML 登记了该页按钮时不得回退原生控件，否则规划器会发明客户端不会发的按钮、并把既有计划形状锁改道到更短路径）。诊断提示：`no client-reachable path to a COMPLETE node` + 起点节点号是兜底文案，真正卡点要按页看 `visibleActions`。

## [QE-083] 八十一、**家族规范形竖切片（canonical 旗标穿家族重载）**：删除页链 = 编译层换形而非运行时旁路——形状变更走变体通道、审计双侧同源收窄、指纹两列同值重定基（CANONICAL_FAMILY_SLICE）

<!-- pattern-metadata
status: CONFIRMED
scope: RETAIL_TABLE 家族编译器"回归真端原生生命周期"改造的方法论：页链删除（接取/简报/报告）、审计口径收窄、IR 指纹重定基、锁定测试对齐；适用于一切"共享 flow 函数多家族复用"的形状手术
first_seen: 2026-09-26
last_verified: 2026-09-27
symptom: ①RETAIL_TABLE 任务对话被页码 TSV 驱动出 select1 阶梯/简报页链/报告页（客户端页树里根本没有这些动作行，服务端单侧下发）；②改编译器形状会同时翻转共享 static flow 的全部调用家族（DataDriven 路由与串行链共用 8/9 参重载）；③审计/等价/指纹三门对同一编译变更给出三种批量反应（introduced 批量 / stale 批量 / IR 漂移批量），逐条手改必漏
root_cause: 共享 flow 函数是全家族复用的 static ⇒ 形状变更必须做成**变体通道**（canonical 旗标按重载翻转）而不是原地改共享函数；审计基线与冻结指纹是"形状冻结器"，规范形是有意变更 ⇒ 必须走它们预留的**留痕通道**（双侧同源过滤 / rebaseline 旗标）而不是绕过或手改生成物
fix_or_guardrail: 1. canonical 旗标只穿过家族 7 参重载（生产唯一调用点 `RetailQuestDriver.compileSimpleHunt`），8/9 参（DD 路由）与串行链保持 legacy 形——并行车道的在册红（20035）不被本片搅动，T1 判据 = 与基线红**同身份**而非全绿；**落点判定（SimpleCollectItem/SimpleSerialHunt 续片实证）**：形状变更落点先画「公开入口→builder」调用图——派生家族（CollectItem 有独立编译器、SerialHunt 连编译器都没有，`buildSerialChain` 唯一调用链 = 生产驱动器与门禁夹具共享的入口）直接**原位翻转**该入口即可（生产+夹具同翻，无假绿窗口）；只有「生产 canonical 与 DD/串行 legacy 必须共存」的网格形才需要旗标穿线（SimpleHunt 7 参重载）；2. 规范形四相位：Acquire（QUEST_SELECT→页4 自环 + 1002/20000 两形提交 + 1003→页1004/1004·20001 关窗拒绝族 + FINISH_DIALOG→页10；无 1007/阶梯/续页）、Progress（kill/collect/step 边不动）、Deliver（满段 QUEST_SELECT→REWARD + `rewardWindowForTier` 分档窗，未满段**零对话路由**，窗页查表不固定 5，QE-028）、Settle（completeFlow 零改动）；3. 审计收窄必须**双侧同源**（QE-066）：audit 行与 baseline 行用同一 `RetiredQuestIds` 谓词过滤 ⇒ narrowed=2431 且默认/严格模式双绿（introduced=0/stale=0）；4. 指纹重定基走门禁自带的 `retail.hunt.rebaselineOut` 旗标（两列=真端指纹，门禁注释明说"仅限有台账记录的 canonical 编译器变更"），回写前核对 id 集：plain 集**不变**、裁定集**只增不减**、两表**零交集**；5. 锁定测试按"零售侧改、XML 侧留"分拣，先探针 dump 实际 IR 再写断言；REWARD 态 1009/-1 是预览语义（completeFlow 预览边无 SyncQuestState ⇒ 交互对象解析器不重跑，别照抄交付段的解析断言）
evidence: .agents/summary/quest-native-dispatch/README.zh-CN.md（Phase 2 节）; src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleHuntDefinitionCompiler.java（canonicalAcceptFlow/rewardWindowPage/canonical 旗标线程）; src/test/resources/quest/retail-simple-hunt-ir-fingerprints.tsv; src/test/resources/quest/retail-simple-hunt-adjudicated-ir-fingerprints.tsv; .agents/summary/quest-native-dispatch/gates/T1-022247.log（76 例唯一红=在册 20035 零新增）
validation: 聚焦 7 类 57 例全绿（gates/phase2-focused-3.log）；契约门严格模式绿（gates/phase2-contract-strict.log，stale=0）；T1 76 例唯一红与基线同身份（20035 DD 漂移登记，并行车道在飞）；T2(1112 30715) 128 例同唯一红；指纹 plain 441 行 id 集不变、裁定表 298→344（+46 = 并行车道新迁移裁定任务补齐冻结证据，纯增量、与 plain 零交集）；等价门 XML 对拍因 XML 已退役恒空转，判据由冻结指纹承担（`retail.hunt.rebaselineOut` 注释即为此变更预留的留痕通道）；T3 收口轮：首轮 ADDED 18（13 个涉事任务全为 RETAIL_TABLE SimpleHunt）→ 12 个锁旧形测试类按'零售侧改、XML 侧留'分拣到规范形（满段 QUEST_SELECT 交付 / 一步简报清 SECTION_5 / 页4 入口 / QUEST_SELECT 派发共号）→ 收口 T3（gates/T3-030925.log，2014 例）对兄弟基线 **ADDED 0 / REMOVED 2**（两个 REMOVED = 本片顺手修复的兄弟遗留红），基线自带红逐字在册不越界
boundaries: 只翻 SimpleHunt 家族（7 参重载）；DD 路由/串行链/DD 指纹/retention 登记表/拒绝登记表不动（requireBriefing 删除属后续家族续片）；completeFlow 与 auto-reward 通道（108/110+k）不动；SIMPLE_TALK 等其余家族沿用本方法论另起续片
superseded_by: none
first_check: 改任何家族编译器形状前，先数清 compile 重载的全部调用方（谁生产、谁测试、谁共享）；改后第一轮让三个"形状冻结器"（审计/等价/指纹）各自用它的留痕通道发声，再按 id 集对拍收口；见到"指纹表行数变了"先分 id 集变化（登记/迁移面变了）与指纹值变化（形状变了）——两类变化走不同收口
keywords: canonical 旗标、家族重载、规范形四相位、canonicalAcceptFlow、rewardWindowForTier 分档、页4 原生提交回退、审计双侧同源、RetiredQuestIds、rebaseline 两列同值、裁定表纯增量、id 集对拍、REWARD 预览语义、auto-reward 110+k/108、零新增失败=同身份
-->
- **一句话判据**：形状手术三件套缺一不可——canonical 走重载变体通道（不搅共享调用方）、审计走双侧同源过滤（不单侧砍分母）、指纹走 rebaseline 留痕旗标（回写前 id 集对拍）。
- **两个坑**：①REWARD 态 1009/-1 预览边没有 SyncQuestState，交互对象解析断言照抄交付段必红；②指纹表行数变化有两种（id 集变化 = 登记面移动、指纹值变化 = 形状移动），回写前必须分开核对。
- **验收口径**：T1 的判据是"唯一红与基线**同身份**"（本片 = 20035 DD 漂移登记逐字相同），不是全绿——并行车道在飞时全绿反而可疑。

## [QE-084] 八十二、**门禁收口对拍的两条操作假象**：`surefire:test` 不编译（红的是旧字节码）、`comm` 按 locale 排序（身份集对拍产出全假象）——改后必须编译、对拍必须 LC=C 集合运算（GATE_CLOSEOUT_PROBE_DISCIPLINE）

<!-- pattern-metadata
status: CONFIRMED
scope: 一切"改代码→聚焦复跑→门禁身份集对拍收口"流程的操作纪律；跨域可复用（questEngine 门禁链、任意 surefire 项目、任意日志对拍）
first_seen: 2026-09-27
last_verified: 2026-09-27
symptom: ①修完测试类用 `mvn surefire:test -Dtest=...` 复跑，失败形状与行号和改动前逐字相同（三处修复像全部没生效）；②T3 身份集对拍给出 REMOVED 98 / ADDED 36 的大漂移，逐条归因时发现"被消除"的身份（如 quest 1131）在两轮日志里明明都在红
root_cause: ①`surefire:test` 直接执行 target/test-classes 里已编译类，不触发增量编译——改的是源码、跑的是旧字节码；②sort/comm 走 locale 排序规则，不同轮次提取的身份集字典序在两侧不一致 ⇒ comm 把交集判成两侧独有，ADDED/REMOVED 全是排序假象（真相可能是 0/0）
fix_or_guardrail: ①改测试（或生产）代码后的聚焦复跑必须 `mvn test`（触发 test-compile）而不是裸 `surefire:test`；第一信号 = 失败行号——行号还在旧位置即编译产物过期；②门禁身份集提取统一 `grep ... | sed ... | LC=C sort -u`，对拍用集合运算（python set）不用 comm；并发车道共享 gates/ 目录时认领日志以链输出自报的 `[T3] log=...` 行为准（自报路径优先于目录里"看起来像别人写的"同名猜测）；③zsh 下 `$VAR` 展开不做词分割——门禁脚本传变量形式的 id 列表会被当成**单个参数**（argparse 报 usage/数字校验错），必须 `${=VAR}` 强制分割或直接写字面量列表（SimpleUseItem T2 102 id 实锤）
evidence: .agents/summary/quest-native-dispatch/gates/T1-042720.log（76 例 1F 与上轮同身份=20035 DD 车道红）; .agents/summary/quest-native-dispatch/gates/T3-042013.log; comm 假象实例 = LC 未固定的 comm 输出 REMOVED 98/ADDED 36，python 集合重算真相 0/0
validation: SimpleCollectItem 收口 = T3 身份集 138→138 对基线 T3-030925 同态（ADDED 0/REMOVED 0）、对上轮 T3-035909 REMOVED 3 且逐条=本片修复；三修复类 `mvn test` 复跑全绿（仅剩基线在册红 1131）
boundaries: 不涉及门禁脚本本身的修改；LC=C 的必要性以"同一提取命令跨会话复跑"为前提——单次会话内两次同 LC 提取的 comm 也可能碰巧正确，不能以单次正确反证纪律多余
superseded_by: none
first_check: 聚焦复跑结果与改动前逐字相同 → 先怀疑编译产物过期（看失败行号）再怀疑修复本身；身份对拍结果"大到不像本车道改动面" → 先用 python 集合重算再逐条归因
keywords: surefire:test 不编译、test-compile 新鲜度、失败行号指纹、LC=C sort、comm locale 假象、身份集对拍、python set、共享 gates 目录、日志认领、zsh 词分割、${=VAR}
-->
- **一句话判据**：复跑前先保证字节码新鲜（`mvn test`），对拍前先保证排序可比（`LC=C sort -u` + 集合运算）——两个环节都"看起来跑了"不等于"验的是真的"。
- **验收口径**：收口对拍的合法判据是 python/排序统一的集合差（本轮 138→138 同态、REMOVED 3=本片修复逐条对上），comm 在未固定 locale 下的任何大数字（98/36）先当假象处理。

## [QE-085] 八十三、**规范形伤亡的契约改写判据**：交付 NPC 提取点随形状搬家、未满段"零路由"必须收窄为"零报告通道"、形状二分断言处理混合族（CANONICAL_CASUALTY_REWRITE）

<!-- pattern-metadata
status: CONFIRMED
scope: canonical/页链删除类编译层换形后，锁"单任务对话/交付形状"的契约测试（对齐类/行契约类/运行时走查类）的批量分拣判据；DD 系多段链、talk/collect 链、SimpleTalk 家族续片直接复用
first_seen: 2026-09-26
last_verified: 2026-09-26
symptom: DD 单段网格翻 canonical 后 T2 只出 43 新增红，T3 全量再暴露 19 例（10 个对齐/行契约类）——锁每任务对话形状的测试类分散在 T2 id 选择器扫不到的角落；红因聚成三形：入口页 4762→页4、1009 报告路由消失、满段自环消失导致 reportNpc 提取 orElseThrow
root_cause: ①契约测试普遍用"满段 QUEST_SELECT 自环 / 1009 路由"当交付 NPC 的权威提取点，canonical 把交付合并进满段 QUEST_SELECT→reward 后两个提取点同时失效；②"未满段零对话路由"的断言过强——canonicalAcceptFlow 的 FINISH_DIALOG(1008) 关窗出口挂在接取目标段（a0 或 started），它不是报告通道但确实是 TalkToNpc 路由；③同一测试类常混锁 canonical 行与 XML 保留行，单一形状断言必撞其一
fix_or_guardrail: ①交付 NPC 提取点改从"满段→reward 的 QUEST_SELECT 交付边"提取（自环没了它还在）；②未满段断言收窄为"无 QUEST_SELECT/1009 报告通道"（noneMatch 过滤），只有系统发放行（EnterWorld/SystemGrant，无接取流）才可断言全零 TalkToNpc；③混合族用形状二分 helper（有 QUEST_SELECT 自环→按 legacy 断言完成页；无→按 canonical 断言零报告通道），或按"非领奖态进领奖的对话路由"提取使两种形状皆可（EventShard 判）；④期望领奖窗不写死 5：canonical=rewardWindowForTier(组数-1) 查表、legacy 1009=固定窗 1，按 reportAction 形状二分（QE-028 禁线性推算/写死）；⑤负控（gate）测试的守门对象随形状搬家：门控 1009 删除后，负控改为"交付边原样 re-source 到未满段"（仿 1347 gate 构造）；⑥T2 的 id 选择器有盲区，家族切片的 T3 首跑 ADDED 不是事故而是正常勘察步——预留一轮"T3 首跑暴露→批量分拣→T3 复跑收口"的预算
evidence: .agents/summary/quest-native-dispatch/gates/T2-065114.log（74 红=31 基线+43 新增）; .agents/summary/quest-native-dispatch/gates/T3-073402.log（首跑 ADDED 19/10 类）; .agents/summary/quest-native-dispatch/gates/T3-075615.log（收口 ADDED 0/REMOVED 2，两例=有意收口的基线红 2569/28743）; 探针实测 28743 canonical 交付边只在 804732（接取变体 206395/6/7 无交付）；16823（DD talk-hunt chain 行 939-movie）/50091（P0c-53 组表形 vs 旧修复契约）两基线红裁定归兄弟车道
validation: DD 顺序链切片（36 行漂移/21 新增伤亡/12 方法）用同三模式一轮分拣收口，T3 对基线 ADDED 0/REMOVED 0（135→135 完全同态）——判据跨 DD 网格/顺序链两形状可复用实锤；43+19 新增伤亡全部分拣修复；Growth/19637/25512/25640/25698/28932/A03/EventShard/Section0/Cradle/DataDrivenHunt 复跑 31/32 绿（唯一剩红=21065 基线在册）；T3 身份集对基线 T3-062949 ADDED 0 / REMOVED 2（逐条=本片有意收口）
boundaries: 不适用于 XML_RETENTION 行（保持 legacy 断言原样）；形状二分 helper 的 if 分支按"自环是否存在"判定，若某行两通道皆无会被 canonical 分支吃掉——对"必须仍有 legacy 形状"的行要另加显式存在断言；canonical 未满段 FINISH_DIALOG 出口保留是刻意设计（客户端本地关窗仍要有路由），不要在后续续片把它当漏网页链删掉
superseded_by: none
first_check: 家族切片翻 canonical 后，先按"入口页/交付边/提取点"三形扫契约类红因再动手；写"零路由"断言前先确认该段是否有接取流（有→收窄为报告通道判定）
keywords: canonical 伤亡、交付边提取、满段自环、FINISH_DIALOG 关窗出口、未满段零报告通道、形状二分、rewardWindowForTier 查表、负控 re-source、T2 选择器盲区、对齐契约类、28743、2569、19631 负控
-->
- **一句话判据**：锁形状的契约测试跟着形状走——提取点锚到"满段→reward 交付边"、未满段只禁报告通道、混合族形状二分，负控跟着守门对象搬家。
- **验收口径**：T3 身份集对基线 ADDED 0；REMOVED 只允许"有意收口的基线红"且逐条登记（本轮 REMOVED 2 = 2569 备选段幻影测试改锁 REWARD 态预览 + 28743 canonical 契约）。

## [QE-086] 八十四、规范形过场重挂：匹配键必须带 source 节点且 match 恰为 1（CANONICAL_CUTSCENE_SCOPED_REATTACH）

<!-- pattern-metadata
status: CONFIRMED
scope: 任何"真端表声明过场轴（movie id + 触发 haction）"的任务族做规范形（canonical 接取/交付）改造时的过场挂载
first_seen: 2026-09-27
last_verified: 2026-09-27
symptom: 规范形化后过场**静默消失**（客户端不播电影，且没有任何测试或门禁变红）；或反向：同体 NPC 的接取/交付两条 `QUEST_SELECT` 被同一次匹配命中，电影被复制到另一条边上
root_cause: 旧挂载器 `attachMovie(transitions, npcId, triggerActionId, movieId)` 以 `(npcId, dialogId == triggerActionId)` 为键，依赖旧页链里真实存在的 `ASK_QUEST_ACCEPT(1007)`/`SELECT_QUEST_REWARD(1009)` 路由；规范形把这些路由换成"`QUEST_SELECT(31)` 直发窗页"，match 数变 0 ⇒ 旧实现的"零匹配原样返回"把**丢失变成静默**；而若只把键放宽成 `(npcId, actionId=31)` 而不带 source 节点，判例 4056（接取与交付同体 NPC）会双命中
fix_or_guardrail: 1. 挂载器改为 `attachMovieToRoute(transitions, sourceNode, npcId, actionId, movieId)`：匹配键 = `(source 节点, npcId, dialogId)`，落点 = "下发对应窗页的那条 `QUEST_SELECT` 边"（接取侧 source=`unaccepted`，交付侧 source=`started`）；2. match 数必须**恰为 1**，否则抛异常 fail-closed（0 = 会静默丢电影、>1 = 作用域键失效）；3. `PlayMovie` 插在开窗/关窗动作之前（after 序 `Sync → PlayMovie → 开窗`，与老 craft 行编码一致）；4. 家族门必须**逐行**加不变量："受理且声明过场轴的行，定义内 `PlayMovie` 恰 1 个且 `movieId == 真端 cutsceneid1`"——只数受理行数或电影总数的弱断言拦不住静默丢失（T2 选择器对过场 id 零引用，抓不到）
evidence: src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkDefinitionCompiler.java（attachMovieToRoute）; src/test/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkGateTest.java（逐行过场不变量）; .agents/summary/quest-native-dispatch/2026-09-27-s1-cutscene-rows.zh-CN.md; .agents/summary/quest-native-dispatch/s1-cutscene-rows.tsv; .agents/summary/quest-native-dispatch/gates/T3-132734.log
validation: 2026-09-27 SimpleTalk S1：13 行（12×1009 交付侧 + 1×1007 接取侧，含 4056 同体 NPC 双命中风险行）全部挂载成功；家族门 4 例全绿（accepted=2149）；T2（1804 单步 id）新增失败 0、T3 对基线 ADDED 0 / REMOVED 0
boundaries: 适用于"规范形把触发动作路由换成 `QUEST_SELECT` 开窗边"的一切族；**未规范化的链面**（如 SimpleTalk S2）仍用旧 `attachMovie`，但其 4 行链式过场的 `MOVIE:` token 藏在登记表 R 记录 after 列，迁移时必须一并处理并加"电影数守恒"断言；隐藏过场任务族（QE-055，任务本身就是播片）是另一主题，不适用本卡
superseded_by: none
see_also: [QE-055], [QE-082], [QE-085]
first_check: 规范形化先问"该族有没有过场轴"（真端表 `cutsceneid1`/`cs1_haction`、登记表 `MOVIE:` token、XML `play-movie`），再逐行验证定义内 `PlayMovie` 数 == 1；"电影没了但门禁全绿"就是这个静默丢失
keywords: 过场、PlayMovie、cutsceneid1、cs1_haction、attachMovie、作用域键、source 节点、match==1、fail-closed、4056、静默丢失、规范形重挂
-->

- **判定规则**：过场挂载的匹配键必须是 `(source 节点, npcId, dialogId)`，match 数恰为 1，`PlayMovie` 插在窗页动作之前。只按 `(npcId, dialogId)` 匹配在规范形下要么零命中（静默丢电影）、要么双命中（判例 4056 接取/交付同体）。
- **为什么必须 fail-closed**：旧实现的"零匹配原样返回"把丢失变成不可观测——家族门只数受理行数、T2 又零引用过场 id，整条链上没有任何东西会红。
- **代表案例（2026-09-27 SimpleTalk S1）**：13 行过场轴（`3943/3946/3949/3952/3955/3958/4056/4947/4950/4953/4956/4959/4962`，电影 93-98/403/125-130），其中 4056 的接取与交付同为 `Jackspaner`（npcId 205203）。

---

## [QE-087] 八十五、登记表驱动链行的规范段改造：过滤按段作用域且必须有零残留守卫（CHAIN_SEGMENT_FILTER_SCOPED）

<!-- pattern-metadata
status: CONFIRMED
scope: 任务形状由**登记表记录**（而非编译器代码）驱动的族做规范段（canonical 接取/交付）改造——SimpleTalk 链行一类"逐字回放 registry + 块合成"的编译器
first_seen: 2026-09-27
last_verified: 2026-09-27
symptom: 规范段接管后旧页链仍在定义里（客户端仍看到 SELECT5 报告页 / select1 入口页 / 39-20002 检查对）；或反向：过滤过宽把中段简报/阶段推进记录一并删掉造成死端；或过滤子句结构写错让"同体 NPC"（既接取又交付，判例 1422 的 203912）**跳过交付过滤**、交付段旧形复活
root_cause: 链行形状 = registry R 记录逐字回放，规范段接管必须**同时**做两件事：块换 canonical 形 + 构成旧页链的记录退场。若过滤只按"键冲突"（key ∈ canonical 块边集）而不按"段词表 ∪ 退场页"，则 record 与块边键互斥时（实测 A=0 冲突）旧页链全部存活——勘察量化的"不做过滤 269/280 ≈ 96% 行形状不变"即此假绿；而若过滤的作用域写成一个"总是 return"的分支，两条子句就不再独立
fix_or_guardrail: 1. 切片边界取**两段都有块**的交集（G1），使变化面可逐行验证 == 缺陷面；纯 R 驱动行（γ 面）留独立切片；2. 过滤规则 = `(npc ∈ 段NPC) ∧ (动作 ∈ 段词表 ∨ 下发退场页)`，接取子句与交付子句**必须独立成项**（AND-OR，禁止 `if (接取NPC) { …return… }` 短路）；3. 退场记录载荷 fail-closed 白名单（conditions 只 `HAS_ITEM:`/`START_ELIGIBLE`、actions 只 `REMOVE_ITEM:`、after 只 `DIALOG:`/`SYNC:`/`CLOSE`/`MOVIE:`，且 MOVIE 必须可重挂：触发轴 ∈ {1007,1009} ∧ movieId==真端 cutsceneid1）；4. 退场载荷（HAS_ITEM）由规范交付门**兜底承接**（门源序：元数据 → carried work-items → 退场载荷）并逐条断言 ⊆ 规范门；5. `carriedWorkItems` 门源必须用**未过滤**回放集（过滤会窄化门源，判例 1971 中段 `remove_item1` 消耗后 carried=0 ⇒ 空门才是正确形状）；6. 编译期零残留守卫：过滤后不得有退场页下发、不得有 `started` 源交付中转（与家族门 `hasLegacyDeliveryResidue` 同口径），守卫消息必须带 `(source→target, event, after)`；7. 指纹增量对拍 == 切片行集（实测 203/0/0 且 G2/G3 逐字未动）；8. 过场承载记录在**中段 NPC** 时落点不动（判例 1422/2421/3006），只有接取中转（1007）必须重挂（判例 3020）；9. 断言重锚必须看 **retention owner**：同一测试类里的 XML_RETENTION 行由保留 XML 供货、形状保持 legacy（判例 21081），不得按编译器直编结果断言 canonical
evidence: src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkDefinitionCompiler.java（canonicalSegments 分支 + retiredChainRoute/chainPushedPages/assertRetiredChainRouteQuiet/canonicalChainHandIn/assertCanonicalSelectionSources/assertCanonicalChainResidueFree）; src/test/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkChainGateTest.java（canonicalSegmentRowsCarryCanonicalShapesAndZeroPageChainResidue / chainCutsceneRowsCarryExactlyOneRetailMovie）; .agents/summary/quest-native-dispatch/2026-09-27-s2-canonical-segments.zh-CN.md; .agents/summary/quest-native-dispatch/gates/T2-143321.log
validation: 2026-09-27 SimpleTalk S2：G1=203 行（双块）过滤面 388 条；编译期守卫在开发中抓到"接取子句短路"缺陷（17 行被拒编译）；链门 4/4（含 203 行形状 + 4 行过场不变量）、家族门 4/4；指纹重冻增量 203/0/0 == G1；T2 链选择器（285 id）改后对基线 ADDED 26 → 分拣后 ADDED 0（REMOVED 0）；T3 身份集对拍 ADDED 0 / REMOVED 0
boundaries: 适用于"形状由登记表驱动"的族；形状来自编译器代码的族用 QE-082/QE-085 的竖切片纪律。γ 面（无块行，需为 R 驱动段**合成**规范边）不适用本卡的"块接管"前提：其交付门为阶段门（`VAR_IS:var0=k`）时与物品门不同构，且含 `I` 记录（itemReportGate）为唯一交付来源的行（判例 1152/24202/80320），退场前必须迁移
superseded_by: none
see_also: [QE-082], [QE-085], [QE-086], [QE-074], [QE-075]
first_check: 改造登记表驱动的族先问三件事：①该行的两段是否都由块驱动（否则留 γ）；②过滤子句是否**独立**且按段 NPC 作用域；③退场记录的载荷（物品门/过场）有没有承接路径——三者任一没有就是假绿或丢门/丢片
keywords: 链式规范段、登记表驱动、策略A、加载期过滤、段作用域、同体NPC、零残留守卫、载荷白名单、HAS_ITEM 兜底承接、carriedWorkItems、未过滤回放集、变化面==缺陷面、XML_RETENTION 行、cutscene 承载、fail-closed
-->

- **判定规则**：规范段接管 = 块换形 **+** 旧页链记录退场；过滤必须按段 NPC 作用域、两条子句独立，且必须有"过滤后零残留"的编译期守卫兜底。
- **为什么必须 fail-closed**：`registry` 记录里的门（`HAS_ITEM`）与过场（`MOVIE:`）是**唯一载荷来源**时（元数据无门、页链承载过场），静默过滤 = 静默丢门/丢片，且没有任何门禁会红。
- **代表案例（2026-09-27 SimpleTalk S2）**：203 行双块行；过滤面 388 条；开发期缺陷"接取子句短路"被零残留守卫当场拦下（17 行拒编译）；4 行过场中 3 行落点不动、1 行（3020）重挂。

---

## [QE-088] 八十六、链行规范段的**按段接管**：段旗标解耦、守卫分治、段外载荷派生式延期（CHAIN_SEGMENT_PER_SEGMENT）

<!-- pattern-metadata
status: CONFIRMED
scope: 登记表驱动链行做**分段**规范化（只接管接取段或只接管交付段）的切片——QE-087 讲"接管段内的过滤与守卫"，本卡讲"只接管一段时两段之间的边界"
first_seen: 2026-09-27
last_verified: 2026-09-27
symptom: ①整行旗标（`canonicalSegments` = 接取块 ∧ 报告块）让"只接管一段"无法表达，可翻的行被迫与不可翻的段捆绑；②守卫若按整行口径（把 SELECT5/SELECT6 页判据也加进接取守卫），会拒绝合法的"另一段逐字保留"行；③段外载荷被静默丢弃——登记表把另一段的状态泄进本段（`selectionSources` 越界），canonical 段只登记段内关窗出口 ⇒ **在服务页上的关窗按钮成死端**（契约门 BUTTON_WITHOUT_ROUTE，致命）；④门禁若只断"本段新形"、不断"另一段仍逐字"，"零残留"会退化成把两段都删掉的假绿
root_cause: 接取段与交付段**正交**但共享同一行定义、同一过滤器与同一套守卫。用"整行旗标 + 整行守卫"表达时，唯一可行的"只接管一段"实现变成逐行例外表（硬编码 id），而登记表的段外载荷（selectionSources 是**按 NPC 合并**的客户端属性，不是按段）会被当作噪声丢掉
fix_or_guardrail: 1. 段旗标**按段独立**：`canonicalAccept`（非系统发放 ∧ 有 NPC_START 块 ∧ 接取块 selectionSources 段内）、`canonicalDelivery = canonicalAccept ∧ 有 NPC_REPORT 块`（若要纳入"只有报告块"的行须再解耦一次）；作用域集合 `acceptNpcs`/`reportNpcs` 随各自旗标填充 ⇒ 未接管行的集合为空、过滤器恒不匹配、**一字节不变**；2. **零残留守卫按段分治**：接取侧 = 块 NPC 上不得留 `select1` 族页下发与 1007/1011/1012/1013 动作路由；交付侧保持 QE-087 口径（SELECT5/SELECT6 页 + `started` 源 39/1009/20002 中转）——**不得**把交付段页判据加进接取守卫（接管一段时另一段本就该保留那些页）；3. 段外载荷用**派生式延期**而非硬编码 id：谓词（`acceptSourcesWithinSegment`）与守卫（`assertCanonicalSelectionSources`）**同源**，返回 false ⇒ 该行不接管本段、逐字保留（漂移 0），门禁侧锁"延期集 == 登记表侧复算集合"（登记表变化即红）；4. 门禁必须**同时**断本段新形与"另一段逐字保留"（对照断言：登记记录下发的报告页仍在下发），单纯断"新形存在"会被"顺手删掉另一段"的实装骗过；5. 判据仍是"指纹漂移集逐行 == 切片行集"（实测 60/0/0，且 S2 的 203 行零漂移）；6. 切片内**零测试引用行**必须显式登记（本例 10/60：`1394 2553 2963 3218 4209 4218 4970 30711 30761 80752`），**T2 全绿 ≠ 切片全绿**——这些行只由指纹表 + 门禁下限数字背书
evidence: src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkDefinitionCompiler.java（canonicalAccept/canonicalDelivery 拆分、acceptSourcesWithinSegment、assertCanonicalAcceptResidueFree、retiredChainRoute 去前置旗标）; src/test/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkChainGateTest.java（acceptOnlyRowsCarryCanonicalAcceptAndKeepTheirDeliverySegment、CANONICAL_ACCEPT_ROW_FLOOR、ACCEPT_DEFERRED_ROWS）; .agents/summary/quest-native-dispatch/2026-09-27-s3a-accept-segment.zh-CN.md; .agents/summary/quest-native-dispatch/2026-09-27-s3a-t2-prep.zh-CN.md
validation: 2026-09-27 SimpleTalk S3a：60 行只接管接取段；指纹重冻 60/0/0（逐行 == 切片，28809 逐字未动，S2 的 203 行零漂移）；链门 5/5（含新增"交付段逐字保留"对照断言 + 延期集锁定）、家族门 4/4；T2 链选择器（285 id）ADDED 2（两条恰为切片行的入口页硬断言）→ 分拣重锚 → **ADDED 0 / REMOVED 0**；聚焦验证 2/2 绿；T3 隔离副本（分拣后）对基线 ADDED 0 / REMOVED 0
boundaries: 适用于"形状由登记表驱动且只有部分段可翻"的族；形状来自编译器代码的族用 QE-082/QE-085 的竖切片纪律。**交付段的中间人翻面形**（`SETPROn` 落在中段 NPC、50 行）另需裁定（搬主人 vs 留驻加窗），不在本卡范围；"接管一段会不会把另一段的服务页变死端"三处只能靠 ①段外载荷延期 ②对照断言 ③契约门（但契约门目前剔除全部 RETAIL_TABLE 行，见风险登记）三条并进
superseded_by: none
see_also: [QE-087], [QE-086], [QE-082], [QE-085]
first_check: 做"只接管一段"的切片先问四件事：①段旗标是否按段独立（整行旗标 ⇒ 只能靠例外表，别做）；②守卫是否按段分治（整行口径会拒掉合法行）；③段外载荷（selectionSources / 段外语义的状态）有没有承接或延期路径；④门禁里有没有"另一段逐字保留"的对照断言——缺任一条就是死端或假绿
keywords: 按段接管、段旗标解耦、canonicalAccept、canonicalDelivery、守卫分治、派生式延期、selectionSources 越界、同体 NPC、对照断言、零测试引用行、变化面==缺陷面、BUTTON_WITHOUT_ROUTE、28809
-->

- **判定规则**："只接管一段"必须是编译器的**合法状态**：段旗标独立、作用域集合随之填充、守卫按段分治、段外载荷派生式延期；门禁同时断"本段新形"与"另一段逐字保留"。
- **为什么必须 fail-closed**：段外载荷（`selectionSources` 是按 NPC 合并的客户端属性）被丢掉时，被丢的是**在服务页上的关窗出口**——契约门会报致命 `BUTTON_WITHOUT_ROUTE`，而 RETAIL_TABLE 行当前被契约门剔除 ⇒ 只剩这条守卫拦得住。
- **代表案例（2026-09-27 SimpleTalk S3a）**：60 行只翻接取段；判例 28809（接取 NPC == 交付 NPC，交付段纯 R 驱动）因 `selectionSources = unaccepted s0 s1 s2` 越界而**派生式延期**（逐字保留）；指纹 60/0/0；T2 ADDED 2 → 分拣 → ADDED 0。

---

## [QE-089] 八十七、被替换机制的守卫必须四轴守恒：动作侧载荷 / 源状态口径 / 载体守恒 / 门蕴含（RETIREMENT_GUARD_AXES）

<!-- pattern-metadata
status: CONFIRMED
scope: 任何"删旧机制、换规范形"的编译器改造中，对**被删机制**（退场记录、旧页链、旧中转）写 fail-closed 守卫时
first_seen: 2026-09-27
last_verified: 2026-09-27
symptom: 四类"守卫全绿但语义丢失"：①退场记录的**动作侧**载荷（`REMOVE_ITEM` 扣物）静默消失——守卫只收条件侧（`HAS_ITEM`），覆盖断言因载荷集为空而空过 ⇒ 玩家永久持有任务物品；②残留判据写死**字面状态名**（`"started"`）而新机制把同一动作挪到了别的态（`s2`）⇒ 判据整条空过：I 记录（`39/20002`）腿存活、无页承载、**任务不可交付而全部门禁绿**（判例 24202 的 `failure_page='CLOSE'` 连 SELECT6 检查也绕开）；③被替换机制承担的**可达性载体**（reward 态奖励窗重开路由）在新形下缺席 ⇒ 领奖后关窗**死档**；④被替换机制的门（`VAR_IS:var0=k` 阶段门）退场而新边的**源节点投影不蕴含**它 ⇒ 领奖提前，且专项审计 `QuestPrematureRewardRouteAudit` **看不见**（其候选判据要求"客户端页含该 dialogId"，而规范边动作是 `31`、在客户端页动作集 0 命中）
root_cause: 守卫是按"看得见的形状"写的（条件侧、字面态名、页下发），而语义在换形后换了表达轴（动作、状态投影、载体、门）；四条轴任一漏掉都会把"丢失"变成"不可观测"
fix_or_guardrail: 1. **载荷双轴**：退场记录的 conditions 与 actions 都进载荷集（`HAS_ITEM` + `REMOVE_ITEM`），由规范边的门/动作**覆盖断言**兜底，且 `itemCheck=false ⇒ 空门` 分支不得短路覆盖断言（判例 1323 的 `REMOVE_ITEM` 无 `HAS_ITEM`）；2. **口径按状态投影**：残留/中转判据认"源节点投影状态 == START"，不认字面态名——REWARD 态同名路由是预览语义（QE-083）必须留；3. **载体守恒**：断言被替换机制承担的可达性载体在新形里存在（reward 态奖励窗重开路由），否则拒绝编译；4. **门蕴含**：放行退场的变量门（`VAR_IS`/`VAR_AT_LEAST`）但必须被规范边源节点投影蕴含（同 `QuestMutationPlanner.matchesSourceNode` 口径，`VAR_AT_LEAST` 取下界；源集 = 接取 `unaccepted` ∪ 报告块 source），不蕴含即抛并打印源集（判例 35010/35018/35024/45011/45025 的 `VAR_IS:var0=1 @ started`）；5. 四条都做成**编译期拒绝**而非审计告警——审计的候选判据可能与新形的动作 id 不同源而整片漏看
evidence: src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkDefinitionCompiler.java（retiredChainGate 动作侧收集 / isPreRewardSource / isRewardState / isRewardWindowPage / isImpliedByAnyCanonicalSource / assertCanonicalChainResidueFree 的重开载体断言）; src/test/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkChainGateTest.java（hardenedGuardsHoldAcrossCanonicalRows，测试侧异路径重算）; .agents/summary/quest-native-dispatch/2026-09-27-s3-guard-hardening.zh-CN.md; .agents/summary/quest-native-dispatch/2026-09-27-s3-special-rows.zh-CN.md §13
validation: 2026-09-27 SimpleTalk 守卫加固片：四条守卫对现存 285 行**完全惰性**（指纹 changed 0 / added 0 / removed 0、编译 problems=[]；四轴现存命中各 0：S2 203 + S3a 60 同轴复算）；链门 6/6（新增四轴不变量 + 双下限）、家族门 4/4；T2（285 选择器）ADDED 0 / REMOVED 0（595 测试 / 35F+16E）；T3（隔离副本）ADDED 0 / REMOVED 0（2020 测试 / 109F+23E）
boundaries: 本卡是**潜在**缺口的加固而非现存缺陷修复——G-1/G-4 的落地判例由后续切片提供（1323 的 `REMOVE_ITEM`、35010 系列阶段门）。适用于一切"删旧机制换规范形"的编译器；纯新增（无退场）的改造不适用。与 QE-087（过滤作用域）/QE-088（按段接管）互补：那两张讲"过滤与边界"，本卡讲"退场语义的守恒面"
superseded_by: none
see_also: [QE-087], [QE-088], [QE-083], [QE-082], [QE-060]
first_check: 给"退场/残留"写守卫时按四轴自查：①动作侧载荷收了吗？②判据是字面态名还是状态投影？③被替换机制承担的可达性载体（重开/续页/兜底出口）在新形里断言了吗？④被替换的门在新边上被蕴含了吗？——四轴任一没写就是"全绿但语义丢失"
keywords: 守卫加固、fail-open、退场载荷、动作侧、REMOVE_ITEM、源状态口径、START 投影、重开载体、奖励窗、阶段门蕴含、VAR_IS、premature、QuestPrematureRewardRouteAudit 盲区、编译期拒绝
-->

- **判定规则**：退场守卫按四轴写——载荷双轴（条件 + 动作）、口径按状态投影（非字面态名）、载体守恒（重开/续页出口）、门蕴含（`VAR_IS`/`VAR_AT_LEAST` 被新边源节点投影蕴含）；四条都必须是**编译期拒绝**。
- **为什么必须 fail-closed**：这四类丢失都不会产生任何可观测红（残留判据空过、专项审计的候选判据与新形动作 id 不同源），只能靠守卫当场拒编译。
- **代表案例（2026-09-27 SimpleTalk 守卫加固片）**：四轴对现存 285 行全惰性（指纹 0 漂移、T2/T3 各 ADDED 0/REMOVED 0）；判例预置 1323（`REMOVE_ITEM` 无 `HAS_ITEM`）与 35010 系列（`VAR_IS:var0=1 @ started` 不被锚点蕴含）。

---

## [QE-090] 八十八、R 驱动段的规范边合成：层 A 键覆盖 + 入口通道判据 + 载荷分侧（SYNTHESIS_KEY_COVERAGE）

<!-- pattern-metadata
status: CONFIRMED
scope: 登记表驱动族里**没有块**（`NPC_START`/`NPC_REPORT` 缺席）的行——规范段必须由登记表的 R 记录**合成**（而非"块换形"）时
first_seen: 2026-09-27
last_verified: 2026-09-27
symptom: ①合成的规范边**静默消失**：编译器末尾的 `explicitRoutes` 覆盖（与 XML expander 同口径）用**同键**（`source:npc:dialogId`）的登记记录反向删除刚合成的规范边（R 驱动行恰好带 `(unaccepted, npc, 31)` 的入口页下发记录）⇒ 形状回退旧形、**全部门禁绿**；②合成出来的流**不可达**：入口用的不是客户端能点的事件通道（判例 1323 的接取入口是 `USE_OBJECT` 自环、`GIVE_ITEM` 挂在自环上）⇒ 接取窗永不打开；③载荷在两侧之间**串门**：接取侧守卫把交付侧退场记录的 `REMOVE_ITEM`（`CHECK_USER_HAS_QUEST_ITEM` 检查对，由交付边承接）也算进接取侧载荷 ⇒ 误伤 11 行
root_cause: "块换形"路径里旧记录由段词表过滤掉，合成边天然无竞争；"R 记录合成"路径没有块可换，**同一键上既有旧记录又有新边**，覆盖方向（谁删谁）取决于编译器里先合成后覆盖的顺序 ⇒ 不显式处理就是反向删除；入口与载荷又各自有"通道"与"承接边"的隐含前提
fix_or_guardrail: 1. **层 A**：合成前先构造规范边键集（`source:npc:dialogId`，含入口/提交/拒绝/关窗出口 × 目标态），回放循环里**同键记录与段词表并列为独立退场子句**，并经载荷守卫审计——这是假绿防线，必须与"变化面 == 缺陷面"判据同时验证；2. **入口通道判据**：合成的入口必须是客户端可点的事件记录（判例：`(unaccepted, npc, QUEST_SELECT)` 记录存在）；物件/道具入口（`USE_OBJECT`/`USE_ITEM`）需要**自己的变体**，判据做成派生谓词（`acquired` 唯一解析 ∧ 非交互物 ∧ 有入口记录）而不是硬编码 id；3. **载荷分侧**：接取侧与交付侧的退场载荷各有**不同承接边**（接取动作 / 交付门），守卫必须按退场侧分流收集（`retiredAcceptRoutes` vs `retiredRoutes`），并各自断言"载荷 ⊆ 本侧规范边的动作/条件"；4. 退场记录的动作白名单要放行本侧承接的动作族（`GIVE_ITEM` 之于接取侧）；5. 同键冲突面必须**先普查再动手**：现存已接管行的冲突数应为 0（本片实测 S2 203 + S3a 60 共 0 条），否则先判定是不是已收口面的回归
evidence: src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkDefinitionCompiler.java（canonicalKeys / canonicalKeyRoute / tryParseDialogAction / rDrivenAccept / assertAcceptPayloadCovered / retiredAcceptRoutes / servesFinishDialog）; src/test/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkChainGateTest.java（acceptOnlyRowsCarryCanonicalAcceptAndKeepTheirDeliverySegment 的 R 驱动取数面）; .agents/summary/quest-native-dispatch/2026-09-27-s3b-r-driven-accept.zh-CN.md; .agents/summary/quest-native-dispatch/2026-09-27-s3-special-rows.zh-CN.md
validation: 2026-09-27 SimpleTalk S3b：7 行（`2611 3001 3023 21136 24202 80320` + 裁定放行的 `28809`）；指纹 **7/0/0** 逐行 == 切片，S2 203 + S3a 60 零漂移，物件入口行 1323 按设计未动；链门 6/6、家族门 4/4；T2（285 选择器）ADDED 0 / REMOVED 0；T3（隔离副本）ADDED 0 / REMOVED 0
boundaries: 适用于"无块行"的规范段合成；有块行走 QE-087/QE-088 的换形路径（那张卡里旧记录由段词表过滤，**不存在**同键竞争）。合成边的**可达性**依赖客户端事件通道：`QUEST_SELECT(31)` 是"NPC 任务列表行选择"（`CM_DIALOG_SELECT` 要求 `lastPage==SELECT_QUEST(10)`），不是页按钮（客户端页动作集 0 命中）⇒ 用页按钮语义的入口必须做对应变体
superseded_by: none
see_also: [QE-087], [QE-088], [QE-089], [QE-080], [QE-082]
first_check: 给 R 驱动段合成规范边前先问三件事：①规范边键集是否已构造、同键登记记录是否退场（否则被反删）？②入口事件是否客户端可点（有对应登记记录）？③退场载荷是否按侧分流并有本侧承接断言？——三者任一没做就是假绿、不可达或误伤
keywords: R 驱动段、规范边合成、层A、键覆盖过滤、explicitRoutes、反向删除、entry 通道、USE_OBJECT 变体、载荷分侧、GIVE_ITEM 白名单、servesFinishDialog、派生判据、28809、1323
-->

- **判定规则**：无块行合成规范边时必须做三件事——**层 A**（同键登记记录退场，否则被 `explicitRoutes` 反删）、**入口通道判据**（入口必须是客户端可点的事件记录；物件入口另做变体）、**载荷分侧**（接取侧/交付侧各有承接边，不得跨侧判定）。
- **为什么必须 fail-closed**：同键反删让形状**回退旧形而门禁全绿**（指纹/形状不变量都看不出来，因为回退后的形状本身自洽）；入口通道缺失则接取窗永不打开（"页未达"是非致命类，不红）。
- **代表案例（2026-09-27 SimpleTalk S3b）**：7 行（6 行 R 驱动 + 28809 裁定放行）；层 A 冲突面实测确认只在 R 驱动行（已收口面 0 条）；1323 因物件入口延期；守卫首版跨侧判定误伤 11 行后修为分侧。

---

## [QE-091] 八十九、交付段解耦接管：交付作用域 / 竞争翻面 / 载荷逐字承接 / 层 A 交付对偶（DELIVERY_SCOPE_TAKEOVER）

<!-- pattern-metadata
status: CONFIRMED
scope: SimpleTalk 链式族（`RetailSimpleTalkDefinitionCompiler` 的 `buildChain`）里**交付段**从"登记表逐字回放"换"规范合成"时；与接取段的接管谓词**解耦**之后
first_seen: 2026-09-27
last_verified: 2026-09-27
symptom: ①交付谓词收紧在"接取段旗标 ∧ 双块"上 ⇒ 三类真端交付形**接不上**：无 `NPC_REPORT` 块的 A 形翻面行（26 行）、系统发放行的块驱动交付段（判例 35017）、R 驱动接取行（3001/3023/28809）；②交付 NPC 判据写成**单元素集**（`resolvedRewardNpcs.size()==1`）⇒ 真端名解析到同族多 id 的变体行（35024/35025/35026 的 799800/799801、45024/45025/45026 的 799842/799843）与成员关系行（80752）整体被排除，且真端名在本服 npc 注册表**根本不存在**时（`LF4_GuardianOfDivine`/`DF4_GuardianOfTower`）交付 owner 无从判定；③只把"同形翻面"当缺陷 ⇒ 中间人 premature 形（4209/21033/21455/30711/30761）与真端交付形混为一谈；④退场载荷只收条件侧 ⇒ 阶段写（`SET_VAR:var0=k`）与扣物静默消失（packed 投影与真端写入分家、任务书行号漂移）；⑤`item_check` 的门真源是 `I` 记录（39/20002 对）却只读退场 `R` 记录 ⇒ 门静默变空（无物也能交付）且接管行**双份**展开旧检查对；⑥合成边与登记记录**同键**时被 `explicitRoutes` 覆盖反向删除（判例 80752 的 `SELECT2` 页记录：词表与页下发两臂都不命中）⇒ 形状回退旧形而指纹零漂移；⑦冻结产物只在断言通过后写出，编译失败会停留旧值 ⇒ 把"没跑成"读成"没变化"
root_cause: 交付段的接管边界有三套隐含前提——**NPC 作用域**（谁是真端交付 owner）、**翻面语形**（哪些记录才是交付入边）、**载荷承接**（承接边在哪一侧、以什么粒度）——每套都必须做成派生谓词并与验证同源；"块换形"路径（QE-087/088）里旧记录由段词表过滤，天然没有这些竞争，一旦扩到无块行/系统发放行/多 owner 行就全部暴露
fix_or_guardrail: 1. **交付作用域**：真端 `reward_npc_name` 解析集，**空集回落客户端交付 NPC 登记**（`quest_client_reward_npcs.tsv`，与单步路径同源；判例 35024/45024）；判据用**成员关系**而非单元素集；2. **A 形翻面**：源节点投影 START ∧ 落 `reward` ∧ NPC ∈ 作用域 ∧ 动作 ∈ `SELECT_QUEST_REWARD`/`SET_SUCCEED`/`SETPRO1..3` ∧ **无竞争翻面**（同形却落在作用域外 = D 类中间人 premature 形 ⇒ 交 (iii) 就地加窗，不参与同构替换）；3. **系统发放行走块路径**（无接取段可接管）：放行面必须先按数据实测（报告块行 205 − 有 START 块者 = 2611/35017）再写谓词，保证切片边界不变；4. **载荷逐字承接**（G-4「被承担」分支）：条件侧与**动作侧同轴**收集，token 逐字相等才放行退场记录（`carriedDeliveryGates`/`carriedDeliveryActions`），`canonicalDelivery` 的行**跳过**旧检查对展开；5. **I 记录 = 门真源**：与退场 `R` 记录同轴进门（`retiredChainGate(retiredRoutes, itemReports)`），接管行不再 `itemReportGate` 展开；6. **层 A 交付对偶**：规范交付边占 `(源, 交付 NPC, 31)` 键，同键登记记录必须随交付段退场（`deliveryKeys`），否则被覆盖反向删除；7. 冻结/重冻产物落盘前核对 **mtime 与本次是否重写**
evidence: src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkDefinitionCompiler.java（canonicalDelivery 谓词 / rewardNpcScope / aShapedFlips / competingFlip / deliveryKeys / carriedDeliveryGates / carriedDeliveryActions / retiredChainGate / mergeRequirementsIntoEdge / A 形交付边合成臂）; src/test/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkChainGateTest.java（s3cDeliveryRowsCarryCanonicalDeliveryShapesAndZeroResidue + deliveryTakenOverByA/aShapedFlips/competingFlips/carriedConditionPresent/carriedActionPresent）; .agents/summary/quest-native-dispatch/2026-09-27-s3c-a-delivery-segment.zh-CN.md; .agents/summary/quest-native-dispatch/2026-09-27-s3c-a-prep.zh-CN.md
validation: 2026-09-27 SimpleTalk S3c-A：31 行（计划表 30 行 ∪ {80320}，后者与 24202 逐轴同形且无区分谓词 ⇒ 同形同办）；指纹 **31/0/0** 逐行 == 切片（S2 203 / S3a 60 / S3b 7 零漂移）；链门 7/7（含新 S3c 不变量）、家族门 4/4；T2（285 选择器）与 T3（隔离副本）见收口记录
boundaries: 只换"交付段页面骨架"（旧报告页 SELECT5/SELECT6 与 1009/39/20002 中转退场），**载荷与阶段门逐字保留**；D 类中间人 premature 形（竞争翻面）不在本形适用面（走 (iii) 就地加窗）；`export` 到其它族的交付段合成必须先核对该族的"交付 owner 来源"（本族为真端名 + 客户端回落）
superseded_by: none
see_also: [QE-083], [QE-087], [QE-088], [QE-089], [QE-090]
first_check: 扩交付段接管前先问六件事：①交付 owner 作用域怎么算（真端名 / 客户端回落 / 成员关系）？②哪些登记记录才是交付入边（翻面动作族 + 源投影 + 无竞争翻面）？③有没有"无接取段可接管"的系统发放块路径行（放行面按数据实测）？④退场载荷的条件侧与动作侧是否都逐字承接？⑤门真源（I 记录 / 元数据 / carried）是否读全、旧检查对是否停展？⑥合成边与登记记录是否同键（交付侧层 A）？
keywords: 交付段、解耦谓词、reward_npc_name、客户端回落、交付作用域、成员关系、A 形翻面、竞争翻面、premature、D 类、就地加窗、SET_SUCCEED、SETPRO、SET_VAR、I 记录、item_check、39/20002、层A、deliveryKeys、载荷逐字承接、carried、同构替换、31/0/0
-->

- **判定规则**：交付段接管 = **作用域（谁）+ 语形（哪条边）+ 载荷（承接在哪）** 三件套各自派生；三者都不许用"与接取段共用一个旗标"或"单元素集"这类捷径代替。
- **为什么必须 fail-closed**：作用域写窄 ⇒ 整族交付退回 legacy（静默延期）；语形写宽 ⇒ 中间人 premature 形被"同构替换"掩盖（领奖提前且审计因动作 id 不同源看不见）；载荷只收一侧 ⇒ 阶段写/扣物静默消失（任务书行号漂移、玩家永久持有任务物品）；同键不退场 ⇒ 合成边被反删（指纹零漂移的假绿）。
- **代表案例（2026-09-27 SimpleTalk S3c-A）**：31 行；三次谓词修正均由冻结证据驱动（单元素集 → 成员关系 → 客户端回落）；80752 暴露"层 A 交付对偶"；24202 暴露"I 记录是门真源"；35017 暴露"系统发放无接取段"。

## [QE-092] 九十、中间人翻面「就地加窗」不成立：领奖动作按对话 owner 路由（MID_FLIP_WINDOW_OWNER）

<!-- pattern-metadata
status: CONFIRMED
scope: 链式族里**交付翻面落在链中间 NPC**（`SETPRO{k}(k→reward)`，owner ≠ 交付 NPC）的行，考虑"把奖励窗就地挂到翻面 after"时
first_seen: 2026-09-27
last_verified: 2026-09-27
symptom: ①翻面 after 从 `Sync;CLOSE` 改成 `Sync;ShowQuestDialog(奖励窗)` 后，**形状自洽、指纹/形状/家族门全绿**，但玩家在中间人处看到的奖励窗**选择按钮无路由**（服务端按 `TalkToNpc(中间NPC, 8..23)` 查不到完成腿，完成腿注册在交付 NPC）⇒ 致命类 `BUTTON_WITHOUT_ROUTE` 的**静默**（页是下发了的，只有"按钮点不动"这一客户端可见后果）；②若把窗**追加**在 terminal `CLOSE` 之后，窗被立即关掉（可观测"交任务后没有奖励窗"）；③词表式退场（`SELECT_QUEST_REWARD ∈ CHAIN_DELIVERY_RETIRED`）会同时打掉 reward 态重开载体（判例 4970/35025）
root_cause: 领奖选择动作（`SELECTED_QUEST_REWARD1..6`，id 8..23）与"当前对话 owner"绑定（客户端 `CM_DIALOG_SELECT` 带对话目标），因此**页与它的按钮必须同 owner**；"就地加窗"只搬了页、没搬 owner 的完成腿 ⇒ 页可达而按钮不可达。形状类门禁（指纹/零残留/家族门）只看边与页的存在性，看不见 owner 归属 ⇒ 只有 **e2e 契约门**（按真实协议原语驱动、锁四相位生命周期合同）能把它拦下
fix_or_guardrail: 1. **先证 owner 再动窗**：翻面 owner 上必须存在（或将存在）领奖腿，否则不得在该 owner 下发奖励窗；2. **窗不得追加在 terminal `CLOSE` 之后**（替换或去掉 `CLOSE`）；3. **退场判据只认页下发**，不用动作词表（词表会误伤 reward 态重开载体）；4. **e2e 契约门作为安全网**：只锁生命周期四相位合同、不锁页链形状，形状手术前后必须同绿——它是这类"形状自洽但客户端不可用"缺陷的唯一捕获者；5. 退场路径若无承接边（不合成新交付边），退场对象必须**载荷为空**，并加 fail-closed 断言
evidence: src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkDefinitionCompiler.java（R_REP_REPORT_PAGES / reportPageSide / assertReportPageRetirementQuiet）; src/test/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkChainGateTest.java（s3cDeliveryRowsCarryCanonicalDeliveryShapesAndZeroResidue 的 S3c-D 臂：报告页零下发 + 翻面 after 逐字保留 + G-3 对偶）; src/test/java/com/aionemu/gameserver/questEngine/definition/Quest3100ClientDialogAlignmentTest.java（headless journey）; .agents/summary/quest-native-dispatch/2026-09-27-s3c-d-report-page-retirement.zh-CN.md; .agents/summary/quest-native-dispatch/2026-09-27-s3-adjudication-brief.zh-CN.md
validation: 2026-09-27 SimpleTalk S3c-D：R-REP 报告页退场 39 行（指纹 **39/0/0** 逐行 == 行集；链门 7/7、家族门 4/4；T2/T3 ADDED 0 / REMOVED 0 且红集 sha256 与 W1 终版逐字节相同）；「就地加窗」在 e2e 契约门 `3100` 上实测被拒（`native reward window has no completion route`），实现回滚、翻面逐字保留
boundaries: 适用"翻面 owner ≠ 交付 owner"的中间人翻面行；翻面落在交付 NPC 的 A 形行（W1 的 31 行）不受影响——那里的窗与完成腿同 owner。若将来要走"就地加窗"，必须先做领奖腿镜像（新裁定），并重新过 e2e 契约门
superseded_by: none
see_also: [QE-082], [QE-083], [QE-089], [QE-091]
first_check: 想把奖励窗挂到某条边上前先问三件事：①该边的 owner 上有没有领奖选择腿（同 owner 的 8..23 路由）？②窗是否会被紧随的 terminal `CLOSE` 关掉？③退场/改形后 reward 态的重开载体还在吗？
keywords: 中间人翻面、就地加窗、owner 归属、领奖选择腿、BUTTON_WITHOUT_ROUTE、CM_DIALOG_SELECT、e2e 契约门、R-REP、页下发判据、terminal CLOSE、D 类、SETPRO、39 行、3100
-->

- **判定规则**：**页与按钮必须同 owner**——凡是"搬页/加页"的形状改造，都要同时核对按钮动作在**同一 owner** 上有路由；跨 owner 的页等价于死按钮。
- **为什么必须 fail-closed**：这是唯一一类"形状门禁全绿而客户端不可用"的缺陷（页下发了、边也在，只是按钮无路由）；捕获者只能是 e2e 契约门。
- **代表案例（2026-09-27 SimpleTalk S3c-D）**：D 类 50 行；「就地加窗」在 3100 的 headless journey 上被拒 ⇒ 本片只做 R-REP 报告页退场（39 行，载荷全空 + fail-closed 守卫）。

## [QE-093] 九十一、物件入口的接取梯是**客户端合同**（不是可退场的页链补丁）；发物只落一次性边 (OBJECT_ENTRY_LADDER_IS_CLIENT_CONTRACT)

<!-- pattern-metadata
status: CONFIRMED
scope: 真端 `acquired_npc_name` 解析为**可交互物件**（宝箱/机关，判例 `LF2_Lost_JewelBox`=730032）的行，其接取入口是 `USE_OBJECT` 自环；以及一切"对话窗口由服务端下发"的 owner（`quest_start_use_item` 族）
first_seen: 2026-09-27
last_verified: 2026-09-27
symptom: ①沿用 NPC 规范形的"塌缩"（入口动作直发接取窗页 4）⇒ 客户端为**该物件** authored 的入口页 `select1`(1011) 与其按钮 `HACTION_ASK_QUEST_ACCEPT(1007)` **无入路** ⇒ 页未达（`CLIENT_PAGE_UNREACHED`）+ 丢入口文案；②真端 `give_item` 挂在**可重复**的入口自环（`unaccepted→unaccepted`、无门、状态不变）⇒ 反复用物件**叠加任务物品**、拒接后残留（交付只扣 1）；③合成梯若只与"自己合成的边"对拍（零残留式守卫），客户端合同变更时**静默换形**（合成常量与登记记录分家而无人发现）
root_cause: 物件与 NPC 的**对话框驱动方式不同**——NPC 由客户端本地对话框驱动（客户端发 `QUEST_SELECT(31)`），物件必须由服务端下发对话窗（`QuestStartItemNpcAi2.handleUseItemFinish`：物件上无任务路由时回落 `SM_DIALOG_WINDOW(objectId, SELECT1)`）⇒ 物件的入口页/中转页属于**客户端合同**（客户端 5.8 按该物件的 HTML authored），不是可随段退场的页链补丁；相应地，`assertCanonicalAcceptResidueFree`（禁止 select1 族页下发与 1007/1011 动作）在物件上会与合同**直接冲突**——物件行的"零残留"判据必须换成合同等价判据
fix_or_guardrail: 1. **物件接取梯保留**（入口页 + `ASK_QUEST_ACCEPT` 中转），与 NPC 规范形的差异只有两处且都有证据：页梯保留、关窗出口取 `CLOSE`（不套 NPC 的任务列表页 10）；2. **发物落一次性边**：`give_item` 移提交边（复用 `acceptGiveItemActions`），入口自环载荷必须为空；3. **合同守卫 = 键集逐键相等 + 页 id 互证**：合成梯的 `源:动作` 键集必须与该物件登记对话面**逐键相等**，且登记侧的入口/中转页 id 必须等于合成常量（`SELECT1`/`SHOW_ASK_QUEST_ACCEPT_WINDOW`）——多一条=发明客户端不会发的按钮、少一条=静默丢出口、页变了=合同变更，三者一律**拒绝编译交裁定**；4. **层 A 对偶**：入口键（`USE_OBJECT=-1`）与中转键（`ASK_QUEST_ACCEPT=1007`）必须随段退场，否则 `explicitRoutes` 覆盖会反向删除合成边；5. **不补 inert 边**（该物件 ask 页只发 1002/1003 ⇒ 不合成 20000/20001）；6. **谓词收缩由覆盖面下限兜底**（删掉中转记录 ⇒ 行退出变体 ⇒ 测试下限红，不静默回 legacy）
evidence: src/main/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkDefinitionCompiler.java（`objectAccept` 五轴谓词 / `canonicalObjectAcceptFlow` / `assertObjectAcceptContract` / 层 A 的 `-1` 与 `1007` 键）; src/test/java/com/aionemu/gameserver/questEngine/retail/RetailSimpleTalkChainGateTest.java（`s3cObjectEntryRowsKeepTheClientLadderAndMoveTheGiveToTheCommit` + `S3C_OBJECT_ROW_FLOOR/CEILING`）; src/main/java/com/aionemu/gameserver/ai/quests/QuestStartItemNpcAi2.java（物件对话窗回落 `SELECT1`）; .agents/summary/quest-native-dispatch/2026-09-27-s3c-obj-object-accept.zh-CN.md（含与裁定简报 (a)-②/(c) 的逐条对账）; .agents/summary/quest-native-dispatch/w3_object_entry_census.py（候选行集普查）
validation: 2026-09-27 SimpleTalk S3c-obj（判例 1323）：指纹 **1/0/0** 逐行 == 该行集（S2 203 / S3a 60 / S3b 7 / S3c-A 31 / S3c-D 39 零漂移）；链门 **8/8**、家族门 **4/4**；T2/T3 **ADDED 0 / REMOVED 0** 且红身份集 sha256 与基线逐字节相同（`57bb0621…` / `ce4673c7…`）；**守卫负例三连拦**（副本内：入口页改 `SELECT1_1` ⇒ 拒绝编译 `page contract broken`；追加一条未声明记录 ⇒ 拒绝编译 `key drift`；删中转记录 ⇒ 覆盖面下限红）
boundaries: 只管**物件哨兵接取者**（`acquired` 解析唯一、入口是 `USE_OBJECT` 且下发入口页、同 owner 有 `ASK_QUEST_ACCEPT` 中转与 `QUEST_ACCEPT*` 提交；全量普查恰 1 行）；与 S3b 的 `rDrivenAccept`（入口 `QUEST_SELECT`）**互斥**且同时成立即拒绝编译；NPC 行的 select1 未达是"同形取舍"（S2 §8.3），**不得**据此把塌缩外推到物件；DataDriven 的 FOBJ `USE_OBJECT` 是**步内推进**而非接取入口（另一轴）
superseded_by: none
see_also: [QE-070], [QE-090], [QE-091], [QE-092]
first_check: 审计/改造物件入口行时先问五件事：①该物件的对话窗是客户端本地驱动还是服务端下发（决定页梯是不是合同）？②客户端为该物件 authored 的入口页与按钮是哪些（登记记录的 `page_check` / `HACTION_*`）？③真端 `give_item` 落在哪条边——是否**可重复自环**？④合成梯与登记对话面**逐键相等**吗（多/少/页变各是什么后果）？⑤入口与中转键是否进层 A（否则被 `explicitRoutes` 反删）？
keywords: 物件哨兵、USE_OBJECT、LF2_Lost_JewelBox、730032、1323、入口页、select1、1011、ASK_QUEST_ACCEPT、1007、SHOW_ASK_QUEST_ACCEPT_WINDOW、客户端合同、服务端下发对话窗、quest_start_use_item、give_item、可重复自环、叠加发物、键集逐键相等、层A、合同证人、1/0/0
-->

- **判定规则**：**页梯是不是"补丁"，取决于谁驱动对话窗**——服务端下发驱动的 owner（物件），其入口页与中转页属于客户端合同，**保留**；客户端本地驱动者（NPC），页梯是可退场的旧链。
- **为什么必须 fail-closed**：物件形同时有两类静默风险——载荷侧（自环发物叠加/残留）与合同侧（合成常量与客户端合同分家）；前者靠载荷覆盖断言、后者靠**键集逐键相等 + 页 id 互证**，且谓词收缩必须由覆盖面下限兜底。
- **代表案例（2026-09-27 SimpleTalk S3c-obj）**：判例 1323（宝箱）；1 行切片、指纹 1/0/0；三负例全拦。

## [QE-094] 九十二、页码类 TSV 退役的三步与死分支证明（生成器 / 表 / 读取者 / 消费点四环节同轴）(PAGE_CLASS_TSV_RETIREMENT_TRIPLE)

<!-- pattern-metadata
status: CONFIRMED
scope: `static_data/quest_retail/*.tsv` 与 `definitions/quest_dialog/*.tsv` 里的**页码类登记表**（承载服务端页链驱动的补丁）退役；`RetailTsvManifestGateTest` + `quest-retail-tsv-manifest.tsv` 冻结面
first_seen: 2026-09-27
last_verified: 2026-09-27
symptom: ①只删表文件而不删**唯一读取者** ⇒ 加载期找不到资源（或残留 `empty()` 兜底静默丢页）；②只删读取者而不停**生成器** ⇒ 下次重跑生成器把表写回来（或无人重跑而表永久滞留）；③只删文件不删**清单行**与 `EXPECTED_TSV_COUNT` ⇒ `RetailTsvManifestGateTest` 立刻红；④把页码类表当普通数据表删、未先证"唯一读取点在死分支" ⇒ 删掉仍在服务端的页下发
root_cause: 页码类 TSV 是**生成物**（生成器 → 表 → 读取者类 → 编译器消费点），四个环节的生命周期被独立管理；退役必须**同轴**：先证消费点为死分支（或已换规范形），再按"读取者类 → 参数穿线 → 表 → 清单行 → 计数"整体退场，生产者（生成器）另行停写
fix_or_guardrail: 1. **死分支证明（逐调用点）**：`grep` 出值读取点 → 证明其所在分支的**入口重载零调用者**（判例：`reportPage(` 唯一调用点在 `canonical=false` 分支，而 `canonical=false` 的两个公有重载全仓 3 处调用全是 canonical 形）；2. **退役三步**：删文件 + 删清单行 + `EXPECTED_TSV_COUNT` −1（同一片完成；`RetailTsvManifestGateTest` 是 fail-closed 兜底：磁盘集合 == 清单集合 == 计数）；3. **读取者同轴退场**：删类 + 删全部参数穿线（含跨族编译器与驱动）+ 删死重载/死页链流，**但保留仍被排除族使用的旧形**（判例：`acceptFlow` 因 HandinDialogFlow 三票否决排除迁移而保留）；4. **生成器移交**：生成器若属兄弟车道，只在台账登记"停写"移交项，不越界改脚本——重跑生成器会被清单门**立刻拦红**；5. **零 IR 变化**：退役片必须"全家族门绿 + T1/T2/T3 零新增"，任何行形状改变都说明删错了东西
evidence: .agents/summary/quest-native-dispatch/2026-09-27-w5g1-report-pages-retirement.zh-CN.md（死分支证明逐调用点 + 处置清单 8 项 + 事故留痕）; .agents/summary/quest-native-dispatch/2026-09-27-tsv-retirement-candidates.zh-CN.md（候选四类明细与依赖图）; src/test/java/com/aionemu/gameserver/questEngine/retail/RetailTsvManifestGateTest.java（EXPECTED_TSV_COUNT 26 → 25）; src/main/resources/aion/data/static_data/quest_retail/quest-retail-tsv-manifest.tsv
validation: 2026-09-27 W5-g1：`quest_client_report_pages.tsv`（5995 行）整体退场——删读取者类 + 2 个死重载 + 4 个死页链流 + 全部参数穿线（3 生产文件 + 1 驱动 + 3 测试夹具）；清单门 3/3、SimpleHunt 家族门 1/1（356 s）、等价门 2/2、SimpleTalk 链门 8/8 与家族门 4/4 绿；**T1/T2/T3 ADDED 0 / REMOVED 0**，T1 红集 sha256 `3b92439da8…`、T3 `ce4673c7…` 与基线逐字节相同；零 IR 变化。**W5-g2（同日）**：`quest_client_entry_pages.tsv`（2449 行）同型退场——三处读取全部零效果（链编译器接住形参不读、定义编译器读出的局部无处可传）⇒ 删读取者类 + 全部参数穿线（3 生产文件 + 1 驱动 + 1 测试夹具）+ 表 + 清单行 + 计数 25 → 24；T1/T2/T3 同样 0/0 且红集 sha256 三项恒等。两例均**无形状裁定**，可作纯机械退役模板；删除前须落快照（`static_data/quest_retail/` 未纳入 git）
boundaries: 只管**页码类**（服务端页链驱动）的生成物；客户端侧事实表（任务书行数、页链按钮、SECTION 门控、变体名单、交付对象）必须保留（真端表无对应列）；禁止"看着没人用就删"；生成器属兄弟车道时只登记不移交改脚本
superseded_by: none
see_also: [QE-066], [QE-070], [QE-089], [QE-093]
first_check: 要退役一张 TSV 前先答四问：①它的值读取点是**哪一行**、所在分支还能到达吗（入口重载有调用者吗）？②除该读取点外还有别的消费者吗（类/测试/脚本/审计）？③谁生成它、生成器在哪个车道？④删完之后谁兜底发现"磁盘/清单/计数"不一致？
keywords: 页码类 TSV、退役、死分支证明、dead branch、RetailTsvManifestGateTest、EXPECTED_TSV_COUNT、quest-retail-tsv-manifest、report_pages、entry_pages、RetailClientReportPages、RetailClientEntryPages、读取者同轴、生成器移交、零 IR 变化、26→25→24、接住不读、删除前快照
-->

- **判定规则**：页码类 TSV 的退役是**四环节同轴**（生成器 / 表 / 读取者 / 消费点），缺一环就会静默回来或静默丢页；死分支证明必须**逐调用点**落到"入口重载零调用者"这一层。
- **为什么必须 fail-closed**：清单门保证"磁盘集合 == 清单集合 == 计数"，越界重跑生成器或手工加表都会立刻红；而"零 IR 变化"证明删掉的确是死物。
- **代表案例（2026-09-27 W5-g1）**：`quest_client_report_pages.tsv` 退役；`acceptFlow` 因 HandinDialogFlow 保留属**边界判定**，不是遗漏。

## [QE-095] 九十三、活门退役：先派生可派生判据，再退役（表代表的审计结论可能早已过期）(PAGE_TSV_RETIREMENT_WITH_LIVE_GATE)

<!-- pattern-metadata
status: CONFIRMED
scope: 页码类 TSV 的**唯一读取点是活受理门**（非死分支）时的退役；尤其是"门登记的是页集、而规范形早已不再下发那些页"的过期守卫
first_seen: 2026-09-27
last_verified: 2026-09-27
symptom: ①直接把门删掉 ⇒ 受理面扩张（判例 6 行解锁，其中 5 行客户端连 HTML 与任务书都没有 ⇒ 形状静默引用不存在的任务书行，fail-open）；②只删表不改门 ⇒ 门读不到登记（fail-closed 但出现"静默拒绝码漂移"，编译过、审查看不出）；③把门"换个名字搬走" ⇒ 搬的是一个未定义判据（发明新证据源）
root_cause: 活门守的往往不是表里的**值**（值可能早已死），而是表所代表的**一个已过期的审计结论**（判例：`talk_pages` 登记"客户端页集恰为极简信件"，但 P0-2 规范形改走**全局页**后页语义已死；真正仍需的逐任务客户端证据是**任务书行**——`lastRowIndex` 的奖励投影与修复行都要它）
fix_or_guardrail: 1. **副本实验测真解锁集**（仓库外副本、双跑 dump 对比：改动版 vs 基线版，`ADDED 0 / REMOVED 0` 下逐行分类差即解锁集——判例 6 行）；2. **逐行取客户端证据分级**（HTML 存在性 / 页集 / 任务书行；5 行"三无"必须继续拒绝）；3. **在常设生产表里找可派生替代判据** + **非回归计数**（判例：`clientSummaryRows.rows(id) > 0`；登记 ∩ noProgress = 181，缺任务书 = 0 ⇒ 不误伤已受理行）；4. **识别 fail-open 点**（`Math.max(0, rows-1)` 型静默退化：缺行返回 0 而不是报错 ⇒ 必须由门拦住）；5. **门与值同片改**（页值 `orElseThrow` 与门互为前提：只改一边 = 拒绝码漂移或真 fail-open）
evidence: src/main/java/com/aionemu/gameserver/questEngine/retail/RetailDataDrivenDefinitionCompiler.java（新门 `RETAIL_TALK_JOURNAL_MISSING` + 页值常量）; src/main/java/com/aionemu/gameserver/questEngine/retail/RetailClientSummaryRows.java（`lastRowIndex` 静默 0）; .agents/summary/quest-native-dispatch/2026-09-27-w5g3-talk-pages-retirement.zh-CN.md; .agents/summary/quest-native-dispatch/w5g3-experiment/（before/after dump + 双跑日志）
validation: 2026-09-27 W5-g3：`quest_client_talk_pages.tsv`（1340 行）退役——实验 dump 行数 1508→1508、quest 集合 ADDED 0/REMOVED 0、分类变化恰 6 行（全 `ADOPTED`）；采用换判据后 **25200 受理**（HTML=DD 模板 + 任务书 1 行 + 全局页 doctrine）+ **5 行换码仍拒**；冻结指纹 1217→1218（仅 +25200，其余 1217 行零变化）；T1 86 测试唯一红 = foreign 在册红，红集 sha256 `3b92439da8…` 与基线逐字节相同
boundaries: 替代判据必须来自**常设生产表**（禁止新建页码类 TSV）；解锁行必须有逐任务客户端证据；drift 重冻**只写本片变化面**，foreign 行按原值回置（把别人的在册红改绿 = REMOVED 1）；**受理 flip = 三件套同片**（retention owner + 遗留 XML 删除 + catalog 条目），且必须清理 `target/` 孤副本（见 ENV-004）
superseded_by: none
see_also: [QE-049], [QE-051], [QE-094], [QE-096]
first_check: 要退役一张"门还活着"的表，先答：①删门后**哪些行会被解锁**（实验实测，不是推断）？②这些行有客户端证据吗（HTML/任务书）？③替代判据能从哪张**常设生产表**派生？④非回归计数是多少（登记 ∩ 受影响面 有几行缺新判据）？⑤这些门的拒绝码今天各自命中几行？
keywords: 活门退役、受理门、talk_pages、RETAIL_TALK_VOCABULARY_UNSUPPORTED、RETAIL_TALK_JOURNAL_MISSING、任务书行、clientSummaryRows、lastRowIndex 静默 0、副本双跑实验、真解锁 vs 拒绝码漂移、fail-open、受理 flip 三件套、foreign 在册红回置
-->

- **判定规则**："值死"不等于"门死"；门守的若是**审计结论**（登记某种客户端页集），先验证结论是否仍成立（规范形是否已改走全局页），再找**可派生**的替代判据。
- **为什么必须实验**：静态推断会得出"大概率是拒绝码漂移"这类错误结论（本片实测为**真解锁**）；副本双跑 dump 差集是唯一可靠的真解锁集测法。
- **代表案例（2026-09-27 W5-g3）**：`quest_client_talk_pages.tsv` 退役；25200 受理、5 行换码；foreign 44 行 drift 登记按纪律回置。

## [QE-096] 九十四、零拒绝守卫的退役：不变量迁构建期（冻结快照 + 基数冻结）(ZERO_REJECT_GUARD_RETIREMENT)

<!-- pattern-metadata
status: CONFIRMED
scope: 值已死、门仍活但**今日对全宇宙零拒绝**的守卫型登记表（典型：客户端证据登记表被用作"数据完整性交叉校验"）
first_seen: 2026-09-27
last_verified: 2026-09-27
symptom: ①把零拒绝的守卫当死物直接删 ⇒ 未来数据修订（真端表新增行）无网兜底，延迟 fail-open；②保留 ⇒ 长期维护一张载荷已死（读取者只做 null 判定 / 参数零读）的表
root_cause: 这类门守的是**结构不变量**——两份冻结件（真端模板表 × 客户端证据）之间的静态性质（判例：非空 `talk_npc1` ⇒ 客户端有 SETPRO 终点链）。既然两侧都冻结，不变量可在**构建期**用快照做门，无需运行期读表
fix_or_guardrail: 1. **先证零拒绝**（拒绝码在 retention 全表零命中 + 覆盖交叉 N/N + 逐行终点校验 + 差集行不进管线）；2. **冻结快照进 `src/test/resources`**（与服务端退役表同 sha256）+ 新增常设门（逐行断言 + **基数冻结**：登记行数与每族行数都冻结，增删行必须显式过门）；3. **门子句删除 + 表/读取者/清单/计数同片退役**；4. **新门注册 T1**（`affected_quest_tests.py` `T1_GATE_CLASSES`）；5. 保留与表无关的门（判例：`RETAIL_TALK_NPC_UNRESOLVED`/`AMBIGUOUS`/`SLOT_CONFLICT` 必须继续守）
evidence: src/test/java/com/aionemu/gameserver/questEngine/retail/RetailBriefingChainEvidenceGateTest.java（冻结登记 3225 + 47/5 基数 + SETPRO 终点断言）; src/test/resources/quest/retail-client-briefing-chains.tsv; .agents/summary/quest-native-dispatch/2026-09-27-w5g4-briefing-chains-retirement.zh-CN.md
validation: 2026-09-27 W5-g4：`quest_client_briefing_chains.tsv`（3230 行 = 4 头 + 3225）退役——47 行真端 `talk_npc1`（宇宙内 23 = 21 ADOPT_RETAIL + 2 spawn KEEP_XML；差集 24 行 `minlevel_permitted=999` 砍内容/测试行，不进管线）；六族 389/389 交叉覆盖；retention 零 `RETAIL_BRIEFING_*`；聚焦 18 测试仅 foreign 在册红（新门 1/1 绿、清单门 3/3、等价门 2/2、采集族门 6/6）
boundaries: 快照是**只读证据**（不得随生产表再生成而漂移）；基数必须冻结（否则"悄悄删一行简报列"无人发现）；不变量只覆盖**消费该列的族**（判例：SimpleTalk/UseItem 各有 6 行无登记/非 SETPRO 终点，与本门无关，不得顺手断言）
superseded_by: none
see_also: [QE-094], [QE-095], [QE-082]
first_check: 零拒绝的守卫要退役前先答：①三处门子句今天各命中几行（retention 拒绝码零命中？）？②不变量是什么、覆盖哪些族、基数是多少？③快照与常设门放哪儿、怎么注册进 T1？④与表无关的门子句是否已保留？
keywords: 零拒绝守卫、briefing_chains、覆盖不变量、SETPRO 终点、冻结快照、基数冻结、构建期门禁、T1_GATE_CLASSES、RetailBriefingChainEvidenceGateTest、fail-closed、延迟 fail-open
-->

- **判定规则**：门活但零拒绝 ⇒ 门是**不变量守卫**，不是受理面；退役 = 不变量迁构建期，而不是把守卫删掉。
- **为什么可迁**：不变量是两份冻结件的静态性质（真端表 × 客户端证据），构建期断言与运行期等价且更省。

## [QE-097] 九十五、客户端合同登记表的行级缩表：行×旗标可达性普查 + 快照恒等判据 (REGISTRY_ROW_LEVEL_TOKEN_SHRINK)

<!-- pattern-metadata
status: CONFIRMED
scope: 多旗标共用一张登记表（quest_id \t token…）且旗标死亡面只在**行级**的客户端合同表（整旗标仍活，不能整列退场）
first_seen: 2026-09-27
last_verified: 2026-09-27
symptom: ①按"交付已接管"整旗标删 token ⇒ 体级读取点（方法级后处理）失据，IR 静默变形；②不敢删 ⇒ 表里大半 token 早已不可达，长年死重
root_cause: 登记表的消费者是**逐旗标、逐行类**的——同一 token 在 A 类行是活合同、在 B 类行永不被读（读取点在特定编译分支/特定方法内）。死亡面只能按（行 × 旗标）普查，普查必须以"读取点在哪条执行路径上"为准，而不是"这条数据还有没有意义"
fix_or_guardrail: 1. **普查 = 静态调用点 × 行分类**：先 grep 出全部 `requires(`/读取点（含测试侧零调用证明），再对每行分类（哪些编译路径会处理它）；singleStep 行若编译入口**不传表**、被拒行若 `precheck` 先返，则全 token 静态不可达；2. **禁删体级仍读的旗标**（方法体无条件读它的行一律保留 token——判例：交接包明令禁删 SELECT2_CONTINUE/SELECT5_CHECK*/SELECT6）；3. **判不了的行保守保留**（少删安全）；4. **判据 = 前后定义快照逐键相同**（每行指纹 dump 前后逐字节 diff + 家族门合并跑 + 红身份集与基线恒等）；5. **行数与旗标常量不变**（缩表≠删表≠删旗标）；6. 表若由**兄弟车道生成器**生成 ⇒ 只登记"停写/同步"移交，不越界改生成器（重跑会复活 token，表头注释声明）
evidence: .agents/summary/quest-native-dispatch/r1-dialog-exits-shrink/census_dialog_exits.py + census-report.tsv; .agents/summary/quest-native-dispatch/2026-09-27-b0-wrapup.zh-CN.md; src/main/resources/aion/data/static_data/quest_retail/quest_client_dialog_exits.tsv
validation: 2026-09-27 批 0 R1：3938 数据行不变，token 13429→443（删 6493；family-None 1232 行/singleStep 1459 行全删、DD/采集 310 行只留 SELECT_NONE_1、链行体级四 token 保留）；DD 指纹 1217 行 + 链指纹 285 行前后逐字节相同；7 家族门 32 例唯一红 = 在册 20035，红身份集与 T1 基线恒等
boundaries: 只适用于"行级可达性可静态判定"的表；若 token 读取点依赖运行期分支（编译后处理按 IR 内容触发），token 必须保留（判例：SELECT2_CONTINUE 的重写边存在与否不影响"表被读过"这一事实）；快照判据只证"这批删除无害"，不证"以后也不会被读"——新读取点上马前必须重跑普查
superseded_by: none
see_also: [QE-094], [QE-095], [QE-096]
first_check: 缩一张多旗标登记表前先答：①全部读取点在哪、测试有没有直接断言表内容？②每行会走哪条编译路径（singleStep/被拒/系统发放/链式…）？③哪些旗标是方法体无条件读的（禁删）？④前后快照怎么取、生成器属哪个车道？
keywords: 行级缩表、dialog_exits、可达性普查、行×旗标、快照恒等、singleStep、precheck、体级仍读、保守保留、生成器停写、SELECT_NONE_1、SELECT1_1
-->

- **判定规则**：整旗标死亡 ⇒ 走 QE-094 退役；旗标只死一部分 ⇒ 按（行 × 旗标）普查缩 token，判据是快照恒等而不是"看起来没人用"。
- **保守原则**：普查判不了的行保留 token——少删只损失整洁，多删直接变形 IR；生成器在兄弟车道时，缩表必须同时登记停写移交。
