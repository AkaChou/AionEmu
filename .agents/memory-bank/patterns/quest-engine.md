# Quest Engine Patterns & Pitfalls (任务系统模式与避坑)

本文档记录 AionEmu 声明式 XML 任务系统、状态机与 NPC 交互的实战避坑经验。

> Pattern IDs: `QE-001`–`QE-056`
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

- **判定规则**：5.8 客户端的 `SECTION_0..3` 分别对应 `quest_vars` 的 `0..5`、`6..11`、`12..17`、`18..23` 位段。任务 XML 中的 `varN` 若参与客户端摘要或脚本条件，必须与 `SECTION_N` 对齐，不能仅因为当前最大值较小就紧凑改到 `var(N-1)` 的位段。
- **代表案例**：
  1. 11468/21468 需要 `SECTION_1<10`、`SECTION_2<5`、`SECTION_3<3`，且进行中要求 `SECTION_0==0`。旧 XML 把三个字段放在 `0/4/8`，第一次使用物品就把 `SECTION_0` 置 1，客户端摘要整体隐藏；修复为 `6/12/18` 后恢复计数段。
  2. 10032/20032：真机在服务端已到 s1（交换布局 wire=64，var1=1）时，客户端任务说明仍显示第 0 行；`//quest set 10032 START 65`（SECTION_0=1）后说明行立即前进，证明行索引读 SECTION_0。修正为 `var0=阶段(0..8, offset 0)`、`var1=眼泪次数(0..20, offset 6)`，掉落门禁恢复阶段 6；`Quest10032ItemPlayClientCounterProductionFlowTest` 锁定新合同，既有 `QuestPacketOrderRegressionTest` 的 `var0=7` 断言同时恢复通过。
  3. 10101/20101：客户端在 `quest_script_monster.csv` 声明为 `Progress(2~!4)`（单变量阶段行走），客户端无该任务的 `SECTION_2` 计数器。若在 `<progress>` 声明并写入 `var2`（offset 12），击杀 2 只后整型步数被打包为 `8196`（高位非零），破坏客户端步骤校验使任务追踪 HTML 完全空白；只有保持纯 `var0` 阶段行走（2→3→4）下发纯净整型步数，HTML 才能正常渲染并无缝前进。

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
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/1722.xml; src/main/resources/aion/data/static_data/quest_definition/quests/3940.xml; src/main/resources/aion/data/static_data/quest_definition/quests/4944.xml; src/main/resources/aion/data/static_data/quest_definition/quests/15321.xml; src/main/resources/aion/data/static_data/quest_definition/quests/25608.xml; src/main/resources/aion/data/static_data/quest_definition/quests/27510.xml; src/test/java/com/aionemu/gameserver/questEngine/runtime/QuestCounterProjectionLockFollowUpTest.java; src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDefinitionCompiler.java; src/main/java/com/aionemu/gameserver/questEngine/definition/QuestXmlBlockExpander.java; .agents/summary/quest-counter-projection-family/2026-09-20-counter-projection-lock-batch.zh-CN.md; .agents/summary/quest-engine-startup-debug/README.md
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
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/25400.xml; src/main/resources/aion/data/static_data/quest_definition/quests/24046.xml; .agents/summary/quest-systemic-audit/2026-09-17-systemic-quest-family-audit.zh-CN.md
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
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/16900.xml; src/main/resources/aion/data/static_data/quest_definition/quests/24151.xml; src/main/resources/aion/data/static_data/quest_definition/quests/1640.xml; src/main/resources/aion/data/static_data/quest_definition/quests/2569.xml
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
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/18992.xml
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
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/16942.xml; src/main/resources/aion/data/static_data/quest_definition/quests/26942.xml; src/main/resources/aion/data/static_data/quest_definition/quests/14047.xml; src/main/resources/aion/data/static_data/quest_definition/quests/14112.xml; src/main/resources/aion/data/static_data/quest_definition/quests/24155.xml; src/main/resources/aion/data/static_data/quest_definition/quests/28301.xml; src/main/resources/aion/data/static_data/quest_definition/quests/50010.xml; src/main/resources/aion/data/static_data/quest_definition/quests/15301.xml; src/main/resources/aion/data/static_data/quest_definition/quests/25301.xml; src/main/resources/aion/data/static_data/quest_definition/quests/1423.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMovieAndDialogLoopRegressionTest.java
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
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/2372.xml; src/main/resources/aion/data/static_data/quest_definition/quests/4907.xml; src/main/resources/aion/data/static_data/quest_definition/quests/24202.xml; src/main/resources/aion/data/static_data/quest_definition/quests/24203.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMovieAndDialogLoopRegressionTest.java
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
last_verified: 2026-09-17
symptom: 多档任务在 REWARD 阶段重新与交付 NPC 对话（USE_OBJECT / SELECT_QUEST_REWARD）时，客户端渲染的仍是第 1 档奖励文案与物品；或两条交付分支各自进入的窗口与最终发放的档位相反（在“正直奖赏”窗口里拿到“满足愿望”档位的奖励）
root_cause: 1. QuestXmlBlockExpander.expandNpcComplete 的预览路由写死 ShowQuestDialog(5)，忽略 complete-reward-index，使第 2/3 档重开窗口时回到第 1 档；2. QuestDefinitionCompiler.restoreRewardPreviewContract 用 5 + rewardIndex 线性推算页面，第 5/6 档在客户端是页面 45/46，会下发不存在的页面 9/10；3. 1114 两条交付分支的 complete-reward-index 与进入窗口相反，导致窗口文案与实发奖励错配
fix_or_guardrail: 1. 新增唯一档位查表 QuestDialogPage.rewardWindowForTier(tier)（0..5 → 5/6/7/8/45/46，越界返回空），禁止任何线性偏移推算；2. expandNpcComplete 预览改用查表页面，声明预览却落在客户端未声明档位时以 NPC_COMPLETE_REWARD_WINDOW_UNSUPPORTED 编译失败；3. restoreRewardPreviewContract 改用同一查表，无窗口可用时不合成；4. 1114 的 Asteros 分支改 complete-reward-index="0"、Namus 分支改发第 2 档并修正 reward-index；5. QuestMovieAndDialogLoopRegressionTest 新增全库门禁：档位查表锁定、合成与显式预览页面、5,351 个 npc-complete 预览块逐块断言、多档任务“进入窗口档位 = 结算档位”
evidence: src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDialogPage.java; src/main/java/com/aionemu/gameserver/questEngine/definition/QuestXmlBlockExpander.java; src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDefinitionCompiler.java; src/main/resources/aion/data/static_data/quest_definition/quests/1114.xml; src/main/resources/aion/data/static_data/quest_definition/quests/1367.xml; src/main/resources/aion/data/static_data/quest_definition/quests/2430.xml; src/main/resources/aion/data/static_data/quest_definition/quests/50023.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMovieAndDialogLoopRegressionTest.java
validation: mvn test -Dtest=QuestMovieAndDialogLoopRegressionTest,QuestDefinitionDirectoryLoaderTest,CompletedQuestPrerequisiteRegressionTest,QuestClientContractGateTest,ProductionCatalogWhitelistVerificationTest 共 30 项用例全绿；PRODUCTION_COMPILE_OK=6186、FAILURES=0、WHITELIST_VIOLATIONS=0；21 个多档任务静态审计 0 发现
boundaries: 适用于所有声明 <reward-groups> 的多档任务与所有 npc-complete 预览；单档任务（rewardGroups <= 1）继续固定第 1 档窗口；1122/2513 这类“按档位手写 grant-reward + 持久化索引 0”的体例以显式 grant 命中奖励组来判定档位
superseded_by: none
see_also: [QE-026], [QE-017]
first_check: 检查每个 npc-complete 预览下发的页面是否等于 rewardWindowForTier(complete-reward-index)，以及 REWARD 节点的进入窗口档位与实际发放档位是否一致
-->

- **判定规则**：奖励窗口页面只能由档位查表得到（第 1~4 档 = 页面 5/6/7/8，第 5/6 档 = 页面 45/46）；`npc-complete` 预览必须下发本档自己的窗口；多档任务中 `REWARD` 节点的进入窗口档位必须等于它实际结算的档位。严禁写死第 1 档页面或按 `5 + index` 线性推算，也严禁窗口与 `complete-reward-index`/`grant-reward` 档位相互矛盾。
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
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/15400.xml; src/main/resources/aion/data/static_data/quest_definition/quests/51022.xml; src/main/resources/aion/data/static_data/quest_definition/quests/50019.xml; src/main/resources/aion/data/static_data/quest_definition/quests/14016.xml; src/main/resources/aion/data/static_data/quest_definition/quests/21107.xml; src/test/resources/quest/quest-drop-retail-contract.tsv; src/test/resources/quest/quest-drop-contract-exceptions.tsv; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestDropContractGateTest.java; .agents/summary/quest/twin-pair-scan/audit_drop_shape_vs_retail.py
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
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/15010.xml; src/main/resources/aion/data/static_data/quest_definition/quests/15012.xml; src/main/resources/aion/data/static_data/quest_definition/quests/15043.xml; src/main/resources/aion/data/static_data/quest_definition/quests/15070.xml; src/main/resources/aion/data/static_data/quest_definition/quests/51021.xml; src/main/resources/aion/data/static_data/quest_definition/quests/28836.xml; src/main/resources/aion/data/static_data/quest_definition/quests/28838.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestItemSourceContractGateTest.java; src/test/resources/quest/quest-item-role-baseline.tsv; .agents/summary/quest/item-producer-scan/audit_cross_quest_item_roles.py; .agents/summary/quest/item-producer-scan/generate_item_role_baseline.py
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
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/11003.xml; src/main/resources/aion/data/static_data/quest_definition/quests/15000.xml; src/main/resources/aion/data/static_data/quest_definition/quests/15072.xml; src/main/resources/aion/data/static_data/quest_definition/quests/2307.xml; src/main/resources/aion/data/static_data/quest_definition/quests/3096.xml; src/main/resources/aion/data/static_data/quest_definition/quests/4940.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestItemSourceContractGateTest.java; .agents/summary/quest/item-producer-scan/item-handin-route-gaps.tsv
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
evidence: commits b771eef59, commit 3b4e7fc4c, commit f00d6e538; src/main/resources/aion/data/static_data/quest_definition/quests/13758.xml, src/main/resources/aion/data/static_data/quest_definition/quests/13761.xml, src/main/resources/aion/data/static_data/quest_definition/quests/13764.xml, src/main/resources/aion/data/static_data/quest_definition/quests/13765.xml, src/main/resources/aion/data/static_data/quest_definition/quests/13767.xml, src/main/resources/aion/data/static_data/quest_definition/quests/13769.xml; .agents/summary/quest-counter-audit/audit_counters.py; .agents/summary/quest-counter-audit/census_counters.py; .agents/summary/quest-counter-audit/2026-09-18-counter-kill-and-repeat-gate-alignment.zh-CN.md
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
evidence: src/test/java/com/aionemu/gameserver/questEngine/definition/QuestDefinitionCatalogManifestTest.java; src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestMutationPlanner.java; commit f00d6e538; src/main/resources/aion/data/static_data/quest_definition/quests/2677.xml, src/main/resources/aion/data/static_data/quest_definition/quests/1742.xml, src/main/resources/aion/data/static_data/quest_definition/quests/2317.xml, src/main/resources/aion/data/static_data/quest_definition/quests/11202.xml
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
evidence: commit 3b4e7fc4c, commit f00d6e538; src/main/resources/aion/data/static_data/quest_definition/quests/13841.xml, src/main/resources/aion/data/static_data/quest_definition/quests/13845.xml, src/main/resources/aion/data/static_data/quest_definition/quests/13849.xml, src/main/resources/aion/data/static_data/quest_definition/quests/13947.xml; src/main/java/com/aionemu/gameserver/questEngine/definition/QuestXmlBlockExpander.java
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
evidence: commit c34458083; src/main/resources/aion/data/static_data/quest_definition/quests/15001.xml; src/main/resources/aion/data/static_data/quest_definition/quests/15203.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMonsterProgressContractAuditTest.java; .agents/summary/quest-15001-multicounter-step/2026-09-19-15001-double-counter-step-closure.zh-CN.md; .agents/summary/quest-15001-multicounter-step/2026-09-19-section0-report-row-closure-audit.zh-CN.md; .agents/summary/quest-acceptance/15001-2026-09-19-section0-report-row-client-accepted.md; .agents/summary/quest-acceptance/15101-2026-09-19-section0-report-row-client-accepted.md
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
evidence: commit 75312dcdc; src/main/resources/aion/data/static_data/quest_definition/quests/10501.xml; docs/quest/client-dialog-mapping/quest-dialog-action-details.csv:645; src/test/java/com/aionemu/gameserver/questEngine/definition/Quest10501HandoverContinuationTest.java; src/test/java/com/aionemu/gameserver/questEngine/e2e/QuestHandoverContinuationAuditTest.java; src/test/java/com/aionemu/gameserver/questEngine/e2e/HandoverContinuationContract.java; .agents/summary/quest-10501-handover-continuation/README.md; .agents/summary/quest-acceptance/10501-2026-09-19-client-accepted.md
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
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/10503.xml; src/main/resources/aion/data/static_data/quest_definition/quests/10530.xml; src/main/resources/aion/data/static_data/quest_definition/quests/20530.xml; src/main/resources/aion/data/static_data/quest_definition/quests/10504.xml; src/main/resources/aion/data/static_data/quest_definition/quests/1373.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestCollectProgressAlignmentGateTest.java; .agents/summary/quest-collect-progress-alignment/README.md; .agents/summary/quest-acceptance/10503-2026-09-19-client-accepted.md; .agents/summary/quest-acceptance/10504-2026-09-19-client-accepted.md; .agents/summary/quest-acceptance/10507-2026-09-19-client-accepted.md
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
evidence: src/main/java/com/aionemu/gameserver/questEngine/QuestEngine.java:425 (onKill 用 getQuestNpc(npc.getNpcId()).getOnKillEvent() 判 owner) 与 src/main/java/com/aionemu/gameserver/questEngine/QuestEngine.java:2128 (installProductionDefinitions 用 KillNpc/KillNpcSet 填索引); src/main/resources/aion/data/static_data/quest_definition/quests/15546.xml（四族 × base + T_ 变体集合与 metadata kills 同口径）与 src/main/resources/aion/data/static_data/quest_definition/quests/25546.xml、src/main/resources/aion/data/static_data/quest_definition/quests/25533.xml、src/main/resources/aion/data/static_data/quest_definition/quests/25640.xml; src/main/resources/aion/data/static_data/spawns/Npcs/210100000_Iluma.xml（只刷 T_ 变体，base 模板 0 刷新）; Aion 5.8 客户端解包 quest_q15546.html（点名 T_ElementalLightF_A_66_n / T_Daru_A_66_n / T_Popoku_As_A2_67_n / T_WoodTesinon_A2_67_n，计数 [%5]/4 [%8]/4 [%11]/4 [%14]/4）与客户端 quest_monster 名单（每个 SECTION 四个变体名）及 docs/quest/client-dialog-mapping/client-monster-progress-contracts.csv; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestIlumaNorsvoldKillTargetCoverageTest.java; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestA03ShardRetailAlignmentTest.java（25533/25640 期望改为 零售 ⊆ 客户端契约快照）; src/test/resources/quest/iluma-norsvold-kill-target-contract.tsv; .agents/summary/quest-15546-kill-progress/2026-09-21-iluma-norsvold-kill-target-variants.zh-CN.md、同目录 audit_hard_broken_kill_routes.py 与 generate_kill_target_contract_tsv.py
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
evidence: src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestExecutionCoordinator.java（withoutRedundantStateSync + requiresStatePersistence）; src/main/java/com/aionemu/gameserver/questEngine/runtime/PlayerQuestStateSyncPort.java（sync 无条件下发 SM_QUEST_ACTION）; src/main/resources/aion/data/static_data/quest_definition/quests/15546.xml 与 src/main/resources/aion/data/static_data/quest_definition/quests/25546.xml（variable-below 4 累加 + variable-at-least 5 越界自愈）; src/test/java/com/aionemu/gameserver/questEngine/runtime/Quest15546KillCounterSaturationFlowTest.java; src/test/java/com/aionemu/gameserver/questEngine/runtime/QuestExecutionCoordinatorTest.java; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestIlumaNorsvoldKillTargetCoverageTest.java; .agents/summary/quest-15546-kill-progress/2026-09-21-iluma-norsvold-kill-target-variants.zh-CN.md 与同目录 audit_saturated_kill_selfloops.py
validation: static（xmllint --schema 15546/25546 通过；饱和自环审计 26 -> 18；IDE inspections 0 error）；focused-test（Maven -Dtest=QuestIlumaNorsvoldKillTargetCoverageTest,QuestA03ShardRetailAlignmentTest,QuestExecutionCoordinatorTest,Quest15546KillCounterSaturationFlowTest,QuestMonsterProgressContractAuditTest,ClientQuestSectionAlignmentTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest,QuestMutationPlannerTest,QuestDataDrivenHuntProductionFlowTest test：88/88 全绿，PRODUCTION_COMPILE_OK=6189、FAILURES=0、INTERACTION_OBJECT_FAILURES=0、WHITELIST_VIOLATIONS=0）；隔离证据（临时禁用护栏后 QuestExecutionCoordinatorTest 报 expected <[]> but was <[SyncQuestState[mode=PACKET_ONLY]]>）；full-suite（Maven -Dtest=com.aionemu.gameserver.questEngine.**.*Test test：1561 run / 5 failures / 6 errors / 1 skipped，与修复前同一批既有失败，无新增）；client-acceptance 未做（待用户实机确认超额击杀不再出现任务更新）
boundaries: 护栏只在"状态未变化且无必需动作"时生效：奖励、物品、货币等持久副作用照常下发同步；余下 18 条只带下界的收口自环在饱和后仍会匹配（提交一笔空事务，但不再下发任务更新），需要逐任务客户端上限证据后再收紧；本次未改变任何计数语义（1..4 仍逐次推进，只是第 4 次由累加路线收口）
superseded_by: none
see_also: [QE-047], [QE-048]
first_check: 玩家报告"进度没变却提示任务更新/任务闪一下"时，先看该击杀路线的条件是否只有下界（variable-at-least）而动作是 set 到上限；确认 planner 产出的 packed 与当前完全相同，再查 after-commit 是否带 sync-quest-state，最后按 audit_saturated_kill_selfloops.py 全库定位同形路线
keywords: 任务更新、进度不涨、超额击杀、多杀、饱和、收口路线、variable-at-least、variable-below、sync-quest-state、PACKET_ONLY、SM_QUEST_ACTION、updateQuest、15546、25546、25406、26802、QuestExecutionCoordinator
-->

- **判定规则**：任务状态包只有在"状态或持久副作用真正变化"时才允许下发。命中转换但产出的 packed 状态与当前快照完全相同时，执行路径必须丢弃 `sync-quest-state`（引擎护栏），并且这类路线本身应当收紧守卫而不是依赖护栏。
- **为什么容易漏**：客户端只在收到状态包时刷新任务书，重复包与真实进度包在下行链路上没有区别；服务端侧"命中但状态不变"既不报错也不写库，只在 `QuestExecutionCoordinator` 的 `requiresStatePersistence` 与 after-commit 列表之间留下一条静默通道；而收口自环的 `variable-at-least` 守卫在饱和后仍成立，正是它的主要来源。
- **代表案例**：15546/25546 四计数器任务（第 4 次击杀由 `variable-below 4` 累加路线收口，越界值由 `variable-at-least 5 -> set 4` 自愈；超额击杀不再命中任何路线）；引擎护栏同时覆盖 25406/25407/25408、25580、26802、30600/30610、10112/20112、13705、17510/27510 等 18 条同形自环，由 `Quest15546KillCounterSaturationFlowTest`、`QuestExecutionCoordinatorTest#stateIdenticalExecutionDropsTheRedundantStateSync` 与 `QuestIlumaNorsvoldKillTargetCoverageTest` 守护。
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
fix_or_guardrail: 1. reward 节点 var0 = 客户端 quest_summary 的领奖行索引（默认最后一行“和 X 对话/报告”；最后一行是第 0 行重复行或步骤链折叠时按客户端验收值记例外）；2. 交接 transition 不必写该字段（目标投影权威），但值必须等于客户端领奖行；3. 已落盘的错位存档必须补无 source 的 enter-world 恢复边（status=REWARD && var0==from -> set var0=to），否则 QuestMutationPlanner#matchesSourceNode 会把旧存档挡在 reward 节点所有领奖路由之外；4. 客户端 quest_summary 的每一行都必须能在服务端找到 var0==行号的 START/REWARD 状态，缺口行在游戏里永远不会高亮；5. 迁移或修复前先跑 .agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py（判定行/状态错位）与 .agents/summary/quest-10527-reward-row/audit_legacy_reward_entry_steps.py（取 legacy packed step，区分“迁移丢 nextStep”与 legacy 本身保持 step） 并用镜像任务（q 与 q+10000）交叉验证；7. 末行是否真是领奖行用客户端 NPC 名独立复核：quest_summary 末行的 STR_DIC_N_XXX 必须在 npcs_unpacked/client_npcs_npc.xml 的 <id>/<name> 里对上**reward 路线（target=reward/source=reward）的 NPC**（批次 3 的 202 个任务即以此为收口证据；批次 2 用的是任务内 NPC 命中 + 镜像同形）；批次 2 的 120 个任务就是用这条证据收口的（双侧都缺末行、末行 NPC 命中、镜像同行数同形状）；6. progress 里 var0 的 max 必须 >= 领奖行且 max 不得超出 width（否则 ProgressLayout.pack 抛 value out of range for progress field: var0，或编译期 bit-field value range exceeds its declared width；10525/20525、26820、1921、2634/2669/24201/24202、2946/11076/14051 都踩过）；批次 3 有个别任务原投影是 0（迁移从未设置），同样按“推到客户端末行 + 自愈边”处理；8. 折叠步骤链（10529/20529 的第 10 行“带上陷入沉睡的德扎波波向代理人报告”原本直接压在 REWARD 上）必须把报告行拆成独立 START 节点（s10, var0=10），reward 投影推到真正的领奖行（11），并为旧存档补 REWARD/var0=10 -> 11 自愈边；拆出的节点要接回客户端对话链（select11 -> select11_1 -> select11_1_1 -> SET_SUCCEED），页面枚举缺项（6502 SELECT11_1_1）先补 QuestDialogPage/QuestDialogAction；9. 客户端任务书行索引读 questVars 的低 6 位（SECTION_0），因此任何 varN（N>=1）都必须落在 offset 6N：3090 的 var1@2/var2@3 让 s1-zone/s1-object 下发 SECTION_0=5/9，行号被计数位顶走；全库门禁 ClientQuestSectionAlignmentTest#sectionNamedCountersStayAtTheirFixedSixBitOffset 只放行 14 个显式挂账的历史任务；10. 行推进本身也可能是迁移丢失面：15551-15554/25551-25554 这类“进入感应区/坐骑”任务的 legacy `onEnterZoneEvent` 用两条感应区路线推进行号（A_TO_X 上 var0 0 -> 1、X_TO_A 上 var0 1 -> 2 并置 REWARD），迁移提交 79bc5d3a6 只保留了 `unaccepted -> started` 的自动接取，把推进整条丢掉，于是中间行没有状态、reward 停在倒数第二行；修法是把自动接取原样保留、按旧 handler 恢复推进路线（中间行用 `PACKET_ONLY`、走进 REWARD 用 `LEVEL_AND_VISIBILITY_REFRESH`，与 26800 的 DF_TOWER_SENSORY_AREA 同型）并同步抬高 `var0` 的 width/max；已知粒度：未接任务时进入 A_TO_X 会以 var0=0 接取，需要再次经过该感应区才推进（迁移“进入即接取”模型的固有取舍，不属本轮缺陷）；11. 多计数器任务的行推进必须按客户端 quest_script 的 SECTION 声明建模：18208/18209/28208/28209 的 `SECTION_0==0; SECTION_1<5` 与 `SECTION_0==1; SECTION_2<1` 对应“var0=行号 + var1=5 次击杀计数 + var2=精英标志”，迁移把每个击杀写成 var0 阶段（k1..k7）会把第 1 行的计数位顶走、领奖行（行 3）也永远不亮；重定阶段时 reward 仍按第 1 条投影到领奖行（本任务=2，不因旧 handler 停在 1 而保留 pre-REWARD step），并为旧值域补无 source 的 enter-world 收敛边：START 侧 `var0>=1 && var0<5 && var1<4 -> 0`（回第 1 行）与 `var0>=5 -> 1 且 var1=4`（进第 2 行），REWARD 侧 `var0<2`、`var0>=3` 统一收敛到 2；8. 报告/交付型 2 行任务（成长任务族）reward 投影必须=1，其余行仍由 START 状态承载；9. 若某个 START 事务把 var0 写成 k（行推进），必须同时存在投影 var0=k 的 START 节点，否则 QuestMutationPlanner#matchesSourceNode 会让所有以旧投影节点为 source 的 `var0>=k` 事务永远匹配不到——18036/28036 的 `started(SETPRO1)` 写 var0=1 却仍以投影 var0=0 的 started 为 source，交出物品后任务卡死，批次 7 新增 s1(START,var0=1) 并把三条事务改指 s1 才修好；12. 与 QE-046 求交（引擎外写入方直写 REWARD）时，领奖行不能只改 XML：必须同时把写入方改成在 setStatus(REWARD) 前写领奖行 var0，并把自愈边条件改成旧写入方留下的旧值，再重刷 external-reward-advance-baseline.tsv （批次 8：10522/20522/15542/25542/30211/30213/30311/30313，reward `0 -> 1`、自愈边改 `var0==0`、CM_CREATIVITY_POINTS / CoalescenceService / RiftOrbAI2 各补 qs.setQuestVarById(0, 1)）；13. 补了无 source 的 `enter-world` 自愈边之后，任何按 `sourceNode()` 过滤的测试/审计都必须用 null-safe 比较（`Objects.equals(x.sourceNode(), source)`）：批次 8 用「引用了提交里改过的任务 id + 按 sourceNode 过滤」筛出 43 个定向测试类，其中 21 个类的 helper 因 `sourceNode().equals(...)` 直接 NPE；15. 自闭合的 `started`（`<node label="started" status="START"/>`）在审计里表现为“行 0 无状态”（INTERIOR_GAP/ROW_WITHOUT_STATE）；修法是按客户端 quest_summary 行数与镜像任务补**只含行号**的投影（28932 补 var0=0，与 18932 同形），绝不能把计数器（var1/var2）一起钉进投影——否则 `started` + `var1>=1` 的“满计数恢复路线”会因 `matchesSourceNode` 要求声明变量全等而整条失配；16. `var0` 是标志位/计数槽而非任务书行号的任务不得按行号改投影：30203/30303 的四只守护者击杀标志位（legacy `_30203GroupHalttheCeremony` 逐个 setQuestVarById(n,1)、集齐后 setStatus(REWARD)，领奖态就是 var0..3=1）与客户端 `Progress(SECTION_0<1)…Progress(SECTION_3<1)` 声明一致，审计判 ROW_WITHOUT_STATE 属误报，已在审计脚本用 `VAR0_FLAG_EXCEPTIONS = {30203, 30303}` 记录；9. retail `data_driven_quest` 单步族（start + 1 step，`origin/history:src/main/resources/aion/definitions/compact/quests/scripts/zz_retail_simple_quests.xml` 共 249 个）里 184 个早已是 `reward var0=1`，落后的 18 个（1527/1528/1725/1963/1964/2135/2247/2266/3087/4020/16838/16977/18035/21455/26838/29002/80735/80736）按族内模板 reward 投影 0->1 + 无 source enter-world 自愈边收口；10. 客户端 HTML 里与相邻行共用 visible 槽位的空 `<p>`（10530 第 8/9 行同为 `[%24]`，镜像 20530 没有这一行）会让 client_rows 比真实状态多 1，禁止按“末行行号”加一（审计脚本已登记 `DUPLICATE_VISIBLE_SLOT_BLANK_ROWS`）；11. 领奖 NPC 归属的验收口径是客户端 quest_summary 行内命名 NPC（末行或该行对话对象写明的 NPC = 实现侧领奖/completion NPC，起始 NPC 不得兼任领奖；批次 12：19064/29064 的 Jucleas/Balder 203752/204075、21455 的 Unset 799244）；当 legacy/retail 单文件（如 `terath_dredgion.xml` 的 `start_npc_ids`）与客户端命名冲突时以客户端命名 + 族内一致性为准（同族 30610/30611/30612/30613 的「行内 NPC ↔ 定义 NPC」4/4），禁止把同一派生物的两个副本当双重证据（30614 教训：retail contract 与 legacy template 同源于 `terath_dredgion.xml`）；领奖态入口页必须位于 `npc-complete` 之后且唯一，插进 completion 块内部会直接 XSD 报错；批次 13（两行末行对话族）：客户端恰好 2 行且行 1 点名领奖/完成 NPC 的任务（1926/2938/39003/49003/80989/80990）用 reward 投影 0->1 + 无 source 的 `REWARD && var0==0 -> set var0=1` 自愈边收口，迁移期 `REWARD && var0==1` 入口边按任务保留或不新增；批次 14（事件族两行）：80255/80256（活动烟花族漏网项，行 1 是字面中文名所以批次 1-7 的“末行 NPC 键”筛选漏掉）与 80601/80606（legacy `_80601Fight_Of_The_Navigators` / `_80606The_Good_News_And_Bad` 击杀分支 `setQuestVarById(0, 1)` 后才置 REWARD，typed 击杀事务也保留 `set-variable var0=1`，但 reward 投影仍是 0，经无 actions 的 `NPC_REPORT` 进入领奖态的存档匹配不到 reward 路由）同用 reward 投影 0->1 + 无 source `REWARD/var0=0 -> set var0=1` 自愈边收口；批次 15（残余两行）：1123（行 1 用客户端 actor 名键 `STR_DIC_LA12`，靠同族 1006/1122/1124/30507 的 completion owner = 790001 = Pernos 解键）与 2484（legacy 在烽火对象 700267 处 `setQuestVarById(0, 1)` 后在 Hippolyta 203331 处置 REWARD）同用投影 0->1 + 无 source `REWARD/var0=0 -> set 1` 自愈边；由此“两行 + 末行对话”族（批次 13/14/15 共 12 个）收口完毕；副本/传送段的行（批次 22，24046）：被 legacy `changeQuestStep(from, from+2)` 跳过的行必须由真实事件补源——进入副本世界用 `enter-world` + `world-is <副本世界>`（副本内条件），并配 `world-is ... expected="false"` 的副本外回退，不得照抄 legacy 的 var 值，也不要把任务改挂到与任务无关的 portal 物件上；12. 批次 31：MISSING_TAIL_ROWS 的 k<末行 形态也可能是“全部 NPC 塌陷成接取+一步领奖”，修复时按 legacy 的 0 -> 1 -> 2 事件链重建 started(0)/s1(1)/reward(2) 并保留客户端页按钮链（21217/21244 的 SELECT2_1(1353)/SETPRO1(10000)、SELECT3_1(1694)/SETPRO2(10001)、select5/SELECT_QUEST_REWARD(1009)；21249 的 select1/SETPRO1、select2/SET_SUCCEED(10255)、select_success/DEFAULT_SUCCESS(10002)/SELECT_QUEST_REWARD），领奖 owner 收敛到客户端第三行点名的 NPC，工作物品的 give/remove 位置照 legacy 落点（21244 在行 1 给、开奖励窗口时移除）；13. 批次 32：客户端页链可能比 legacy 一跳更长（13800/23800：legacy 只把 packed step 推到 1，页链却还有 select3/SELECT3_1(1694)/SELECT3_1_1(1695)/SETPRO2(10001) 一段），此时阶梯按客户端页链补（started(0)/s1(1)/reward(2)），而旧存档自愈边要覆盖 `REWARD/var0=0`（旧定义一步领奖）与 `REWARD/var0=1`（legacy 一跳）**两条**；行 0 的传送点 NPC（804782/804753）必须删掉 npc-complete，owner 只留末行点名的802431/802433，书信 give/remove 跟随接取与开奖励窗口；14. 批次 33：`quest_summary` 的 `visible="[%N]"` 槽位会重复——25094 的 3 行是 `%0/%3/%3`（行 1 与行 2 共用槽位），此时客户端真实状态数比行数少 1，**行号口径多算一行**，禁止按末行索引补节点。槽位与状态的换算是「槽位 = 3 × 状态号」（10527 的 16 行 = `0,3,6,...,45`，其 reward=15 已客户端验收），所以 25094 的 reward 投影 = 1：补 `s1(1)` START 状态（CHECK_USER_HAS_QUEST_ITEM 交骨骸进入）、`s1 --SET_SUCCEED--> reward(1)`、防呆入口 `s1 --QUEST_SELECT(领奖 NPC)--> reward(1)`，并补 `REWARD/var0=0 -> 1` 自愈边；`check_user_item_ok` 这类“只有一个结束对话按钮”的页必须在目标节点保留 `FINISH_DIALOG` 路由，否则 QuestClientContractGateTest 报 BUTTON_WITHOUT_ROUTE；15. 批次 34：同一 NPC 既接取又领奖时（30504/30554 的 Lition 205438），`<dialog type="NPC_REPORT">` 与 NPC_START 的展开会撞 `AMBIGUOUS_TRANSITION: TALK_TO_NPC`，REWARD 态入口必须写成显式 `reward + QUEST_SELECT -> SHOW_QUEST_PAGE DEFAULT_SUCCESS`（与同链 30503 同形）；同时记得核对 `progress` 字段上限——30504/30554 原来 `var0` 只有 `width=1/max=1`，连行 2 都放不下，必须随阶梯放宽（批次 34 放到 `width=3/max=7`，低位偏移不变所以旧存档仍是 0）；retail 的 `ACTION action_ids=<柱物件>` 必须落成 `started --USE_OBJECT(物件)--> 下一行`，否则行 0 永远无法完成；16. 批次 35：`npc-item-report` 指令**强制 target 必须投影 REWARD**（编译期 NPC_ITEM_REPORT_TARGET_STATUS），事件任务的交付环节要落到行阶梯中间节点时，必须改写成显式 `started --CHECK_USER_HAS_QUEST_ITEM(npc)--> s1`（`has-item` + `remove-item` + `SHOW_QUEST_PAGE CHECK_USER_ITEM_OK`）；客户端 `check_user_item_ok` 页的 SETPRO2 只是确认按钮，应在当前行就地 `close-dialog`，不要再推一行；行 1 的世界物件（50019/51019 的701466/701467 巧克力塔）若由活动系统刷出、本检出无静态 spawn，追加一条“直接找领奖 NPC 对话”的防呆入口，避免塔未刷出时卡在中间行；12. 活动/事件族的行阶梯要同时满足客户端页链与生产门禁：塔族（select1 + check_user_item_ok）交付行用 CHECK_USER_HAS_QUEST_ITEM、SETPRO2 只做就地 close-dialog、塔 USE_OBJECT 推进；货箱族（select4 + select5）的 quest_use_item 货箱 can-act 资格路由必须落在 START 态且 var0 == collecting-step，零售 collecting-step=4（客户端第二组 3 bit 槽位 3..5=行 1）要按行号收敛为 1；同一 TALK_TO_NPC 事件不得挂两条无 priority 的 distinct QUEST_SELECT（AMBIGUOUS_TRANSITION），领奖入口页 10002 由交付成功 CHECK 路由承担；13. “接取 NPC 与任务书报告 NPC 分离”的 monster_hunt 族必须按 legacy 合同 start_npc_ids/end_npc_ids 拆 owner：接取 NPC 只保留 NPC_START，进度/报告/领奖/自愈边挂在 end NPC；start NPC 不得出现在任何 target=complete 路由上；击杀→报告族一律每行一个 START/REWARD 投影（中间行不复用 started），并禁止 started -> reward 直跳与“最后一个对话 NPC 再写一次 var0”的跳行残留
evidence: src/test/java/com/aionemu/gameserver/questEngine/definition/AlignedMirrorRewardRowContractTest.java（批次 1：9 组镜像领奖行 + 恢复边 + planner 计划）; src/test/java/com/aionemu/gameserver/questEngine/definition/MirrorPairRewardRowContractTest.java（批次 2：120 条合同）; src/test/java/com/aionemu/gameserver/questEngine/definition/JournalRewardRowRepairContractTest.java（批次 3：202 条合同）; src/main/resources/aion/data/static_data/quest_definition/quests/11294.xml; src/main/resources/aion/data/static_data/quest_definition/quests/15002.xml; src/main/resources/aion/data/static_data/quest_definition/quests/15010.xml; src/main/resources/aion/data/static_data/quest_definition/quests/15070.xml; src/main/resources/aion/data/static_data/quest_definition/quests/15514.xml; src/main/resources/aion/data/static_data/quest_definition/quests/19004.xml; src/main/resources/aion/data/static_data/quest_definition/quests/25062.xml; src/main/resources/aion/data/static_data/quest_definition/quests/25073.xml; src/main/resources/aion/data/static_data/quest_definition/quests/26820.xml（reward var0 改为客户端领奖行、max 同步抬高、补 enter-world 自愈边）; .agents/summary/quest-10527-reward-row/audit-missing-last-row.tsv（492 行 “只缺最后一行”清单）; .agents/summary/quest-10527-reward-row/audit-qe051-candidates.tsv（404 个末行是领奖行的候选，其中 9 组镜像一侧完全对齐：11294/21294、15002/25002、15010/25010、15070/25070、15514/25514、19004/29004、25062/15062、25073/15073、26820/16820）; src/main/resources/aion/data/static_data/quest_definition/quests/10527.xml; src/main/resources/aion/data/static_data/quest_definition/quests/20527.xml; src/main/resources/aion/data/static_data/quest_definition/quests/10528.xml; src/main/resources/aion/data/static_data/quest_definition/quests/20528.xml; src/main/resources/aion/data/static_data/quest_definition/quests/10525.xml; src/main/resources/aion/data/static_data/quest_definition/quests/20525.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/ArchdaevaRewardRowContractTest.java; src/main/java/com/aionemu/gameserver/questEngine/runtime/QuestMutationPlanner.java; origin/history 分支旧 handler _10527Finding_The_Traces_Of_The_Sage（useQuestItem(env, item, 14, 15, true)，仓库内已删除）; Aion 5.8 客户端解包数据 quest_q10527/quest_q10528 的 quest_summary（16 行与 13 行，末行均为“和代理人维达对话”）; .agents/summary/quest-10527-reward-row/2026-09-21-10527-reward-row-and-family-audit.zh-CN.md; .agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py; src/test/java/com/aionemu/gameserver/questEngine/definition/JournalReportRowSplitContractTest.java（批次 4：10529/20529 报告行拆分 + 3090 位段/领奖行合同）; src/test/java/com/aionemu/gameserver/questEngine/definition/ClientQuestSectionAlignmentTest.java#sectionNamedCountersStayAtTheirFixedSixBitOffset（全库 varN 必须落在 6N 的门禁 + 14 个挂账任务清单）; src/main/resources/aion/data/static_data/quest_definition/quests/3090.xml; src/main/resources/aion/data/static_data/quest_definition/quests/10529.xml; src/main/resources/aion/data/static_data/quest_definition/quests/20529.xml; src/main/resources/aion/data/static_data/quest_definition/quests/10530.xml; src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDialogPage.java（SELECT11_1_1=6502）; origin/history 旧 handler _10529Protection_Artifact_2 / _10530Trouble_Back_Home / _3090In_Search_Of_Pippi_The_Porgus; Aion 5.8 客户端 quest_q10529/quest_q20529（12 行）与 QUEST_Q3090（5 行）+ 客户端 quest_script 的 Progress(SECTION_1==0; SECTION_0==1); src/test/java/com/aionemu/gameserver/questEngine/definition/SensoryAreaRideRowContractTest.java（批次 5：8 条感应区坐骑行推进合同，含 planner 两段推进、自愈边、镜像同形）; src/main/resources/aion/data/static_data/quest_definition/quests/{15550,25550,15551,15552,15553,15554,25551,25552,25553,25554}.xml（批次 5：领奖行 + 感应区行推进）; origin/history 旧 handler _15550Iluma_Field_Guide/_25550A_Norsvold_Story（STEP_TO_1/STEP_TO_2/SELECT_REWARD）与 _15551Giddyup_Starturtle/_15552Surfing_The_Ancient_Well/_15553Ride_An_Iluman_Butterfly/_15554A_Rickety_Ride/_25551Springleaf_Shortcut/_25552Pull_The_Lever/_25553Butterfly_March/_25554Reed_Patch_Rush 的 onEnterZoneEvent 行推进（仓库内已随 79bc5d3a6 删除）; Aion 5.8 客户端 quest_q15550-15554 与 quest_q25550-25554 的 quest_summary（各 3 行）; src/main/resources/aion/data/static_data/quest_definition/quests/18208.xml、18209.xml、28208.xml、28209.xml（批次 6：2 阶段 + 2 计数器、reward=2、4 条旧存档收敛边）; origin/history 旧 handler _18208IllusionOrInfiltration/_18209ARiftInTheSpaceTwineContinuum/_28208ARiftAdrift/_28209CatchingTheRift（var0 0->1、var1 0..4、var2 0->1、setStatus(REWARD) 且不改写 var0，仓库内已随 7e9f0316c 删除）; Aion 5.8 客户端 quest_q18208/18209/28208/28209 的 quest_summary（各 3 行）与 客户端 quest_script_monster 的 SECTION_0==0; SECTION_1<5 / SECTION_0==1; SECTION_2<1; .agents/summary/quest-10527-reward-row/check_section0_requirements.py 与 dump_section0_evidence.py（SECTION_0 约束审计：UNREACHABLE 5 -> 3）; src/test/java/com/aionemu/gameserver/questEngine/definition/ReportRowRewardProjectionContractTest.java（批次 7：21 条合同）; .agents/summary/quest-10527-reward-row/apply_batch7_report_row_contract.py 与 batch7-evidence.tsv; src/main/resources/aion/data/static_data/quest_definition/quests/19672.xml（成长任务族代表）; src/main/resources/aion/data/static_data/quest_definition/quests/18036.xml（s1 节点修复代表）; src/main/resources/aion/data/static_data/quest_definition/quests/3210.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/ExternalRewardAdvanceReentryContractTest.java（批次 8 基线：10 任务 writer step=1 / projection=1 / 入口页 / recovery=[0]）; .agents/summary/quest-10522-reward-reentry/apply_batch8_external_writer_reward_row.py（批次 8 重放脚本，--check 幂等报 BATCH8_VERIFY_OK quests=8）; src/test/java/com/aionemu/gameserver/questEngine/definition/Quest28932RewardRowContractTest.java（批次 9：started 只声明 var0=0 且与镜像 18932 同形、单次击杀计划到领奖行、var0=0/var1=1 满计数存档的四条恢复对话仍可规划）；.agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py（`VAR0_FLAG_EXCEPTIONS` 记录 30203/30303 的 var0 标志位例外）; src/test/java/com/aionemu/gameserver/questEngine/definition/RetailSingleStepRewardRowContractTest.java（批次 10：18 条单步/单侧合同的 reward 行、镜像同值、自愈边条件/动作/after-commit 与 planner 计划）; src/test/java/com/aionemu/gameserver/questEngine/definition/Quest25608RetailSevenStepAlignmentTest.java（批次 11：25608 七步行、两个 ENTER_AREA zone、HUNT 自环、reward 交付行、旧存档自愈与 drop collecting-step 合同）; .agents/summary/quest-10527-reward-row/batch11-quest25608-evidence.tsv; .agents/summary/quest-10527-reward-row/apply_batch10_retail_single_step_rows.py 与 batch10-evidence.tsv（批次 10 可重放脚本与逐任务证据：client_rows=2、领奖行 1、旧投影 0、领奖/交付 NPC）; src/test/java/com/aionemu/gameserver/questEngine/definition/MirrorRewardProjectionLagContractTest.java（批次 18：6 条镜像单侧投影落后合同 + 自愈边 + 无事务写非领奖行 var0 + 16837/16986/16988 锁定护栏 + 两侧 var0 一致）; .agents/summary/quest-10527-reward-row/apply_batch18_mirror_projection_lag.py 与 batch18-evidence.tsv（批次 18 重放脚本 + 逐任务证据：镜像对照、client_rows、领奖行、completion owner、audit 前后状态）; src/test/java/com/aionemu/gameserver/questEngine/definition/CollapsedSingleStepLadderContractTest.java（批次 19：单步塌陷对 15000/15670 的行阶梯、客户端页按钮链、证据消耗、collecting-step 行号、旧存档自愈与无塌陷跳转 7 例）; .agents/summary/quest-10527-reward-row/apply_batch19_collapsed_single_step_ladder.py 与 batch19-evidence.tsv（批次 19 重放脚本 + 逐任务证据）；批次 22：src/main/resources/aion/data/static_data/quest_definition/quests/24046.xml（副本段 3->4->5->6->REWARD(7) 串行阶梯 + `REWARD/var0=6 -> 7` 自愈边）与 quests/14046.xml（对照基线）；src/test/java/com/aionemu/gameserver/questEngine/definition/ShadowCourtRowLadderContractTest.java（6 例：行状态齐备 / 副本段逐格推进 / 乱序无计划 / 旧存档自愈 / 出口物件只在副本内 / 两侧变体不互污）；.agents/summary/quest-10527-reward-row/apply_batch22_shadow_court_row_ladder.py、batch22-evidence.tsv、2026-09-21-10527-reward-row-and-family-audit.zh-CN.md（§二十六）；legacy 证据：迁移前 handler `_24046The_Shadow_Calls`（已删除，仅存在于 git 历史，用 `git show '7e9f0316c^'` 取回：`changeQuestStep(env, 3, 5, false)` 跳格 + 700369 出口 + die/enter-world 回退）；批次 36 追加：.agents/summary/quest-10527-reward-row/apply_batch36_event_row_ladder.py（--check 幂等 13/13）、batch36-evidence.tsv、2026-09-21-10527-reward-row-and-family-audit.zh-CN.md（§四十）；quests/50020、50021、50022、51021、51022、80300、80301、80302、80303、80306、80307、80308、80309.xml；Batch36EventRowLadderContractTest（5 例）；quest_data.xml 的 quest_drop npc_id=219316/219639/219640 与 701470/701774 collecting_step=4 证据；批次 37 追加：.agents/summary/quest-10527-reward-row/apply_batch37_talk_kill_report_row_ladder.py（--check 幂等 5/5）、batch37-evidence.tsv、2026-09-21-10527-reward-row-and-family-audit.zh-CN.md（§四十一）；quests/26905、26906、26908、3711、4711.xml；Batch37TalkKillReportRowLadderContractTest（4 例）；docs/quest/client-dialog-mapping/legacy-quest-dialog-contracts.csv 的 26905-26908 start/end NPC 分离与 3711/4711 report_source_status=REWARD 合同
validation: static（xmllint 语法 + quest_definition.xsd：10525/20525/10527/20527/10528/20528 + 批次 1 的 11294/15002/15010/15070/15514/19004/25062/25073/26820 validates；审计脚本 6222 定义全库运行成功并产出 audit-output.tsv（2026-09-21 全库复核，客户端 quest_summary 覆盖 5572：形状 ALIGNED 2348 / STATES_BEYOND_ROWS 2627 / MISSING_LAST_ROW 149 / INTERIOR_GAP 267 / MISSING_TAIL_ROWS 92 / NO_STATE 89 / NO_CLIENT_HTML 650（批次 5 后）；“每行都要有 START/REWARD 状态”判定 ROW_STATE_ALIGNED 2348 / ROW_WITHOUT_STATE 597 / STATE_OUT_OF_RANGE 2450 / BOTH_MISALIGNED 177；领奖行指标 ROW_ALIGNED 2566 / ROW_BEHIND 277 / ROW_AHEAD 2593 / NO_REWARD_ROW 178；“reward 投影 > progress var0 max” 0 个）；focused-test 已通过（用户授权后 mvn -B test -Dtest=JournalRewardRowRepairContractTest,MirrorPairRewardRowContractTest,AlignedMirrorRewardRowContractTest,ArchdaevaRewardRowContractTest,QuestCollectProgressAlignmentGateTest,Quest10520ClientDialogAlignmentTest,QuestDialog31RegressionTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest => Tests run 45 / Failures 0 / Errors 0，含批次 2/3 共 322 条合同 + ExternalRewardAdvanceReentryContractTest）；production-gate 通过（PRODUCTION_COMPILE_OK=6189、PRODUCTION_COMPILE_FAILURES=0、PRODUCTION_WHITELIST_VIOLATIONS=0；首轮暴露 10525/20525 var0 max=6、次轮暴露 26820 var0 max=1 并已修）；批次 5 追加验证（用户授权后）：mvn -B test -Dtest=SensoryAreaRideRowContractTest,JournalReportRowSplitContractTest,JournalRewardRowRepairContractTest,ClientQuestSectionAlignmentTest,ArchdaevaRewardRowContractTest,AlignedMirrorRewardRowContractTest,MirrorPairRewardRowContractTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest,ExternalRewardAdvanceReentryContractTest,QuestCollectProgressAlignmentGateTest => Tests run 57 / Failures 0 / Errors 0，PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0，xmllint 10 个文件全部 validates、IDE lint 0 警告；同工作树前后对照审计（先 git checkout HEAD 这 10 个文件取基线再回放）ROW_ALIGNED 2556 -> 2566、ROW_BEHIND 287 -> 277、ROW_STATE_ALIGNED 2338 -> 2348、ROW_WITHOUT_STATE 607 -> 597、ALIGNED 2338 -> 2348、MISSING_LAST_ROW 159 -> 149；批次 6 追加验证（用户授权后）：mvn -B test -Dtest=ArenaPhaseRowContractTest,SensoryAreaRideRowContractTest,JournalReportRowSplitContractTest,JournalRewardRowRepairContractTest,ClientQuestSectionAlignmentTest,ArchdaevaRewardRowContractTest,AlignedMirrorRewardRowContractTest,MirrorPairRewardRowContractTest,ProductionCatalogWhitelistVerificationTest,QuestDefinitionCatalogManifestTest,ExternalRewardAdvanceReentryContractTest,QuestCollectProgressAlignmentGateTest => Tests run 63 / Failures 0 / Errors 0，PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0，xmllint 4 个文件全部 validates、IDE lint 0 警告、git diff --check 干净；check_section0_requirements.py 复跑 UNREACHABLE 5 -> 3；同工作树前后对照审计（先 git checkout HEAD 这 4 个文件取基线再回放）ROW_ALIGNED 2566 -> 2570、ROW_BEHIND 277 -> 275、ROW_AHEAD 2593 -> 2591、ROW_STATE_ALIGNED 2348 -> 2352、ROW_WITHOUT_STATE 597 -> 595、STATE_OUT_OF_RANGE 2450 -> 2448、MISSING_TAIL_ROWS 92 -> 90、STATES_BEYOND_ROWS 2627 -> 2625、ALIGNED 2348 -> 2352（MISSING_LAST_ROW 149 不变），两轮审计输出逐行 diff 只有这 4 个任务变化；client-acceptance 未做（PENDING_CLIENT）；批次 7 focused-test（2026-09-21 用户授权 Maven：ReportRowRewardProjectionContractTest + 批次 1-6 的 12 个测试类 = 68 例全绿；PRODUCTION_COMPILE_OK=6189 / PRODUCTION_COMPILE_FAILURES=0 / PRODUCTION_INTERACTION_OBJECT_FAILURES=0 / PRODUCTION_WHITELIST_VIOLATIONS=0）；批次 7 static：xmllint quest_definition.xsd 21 文件全 validates、IDE lint 0 警告、git diff --check 干净；批次 7 同工作树前后对照（只回放这 21 个文件）：ROW_ALIGNED 2570 -> 2591、ROW_BEHIND 275 -> 254、ROW_STATE_ALIGNED 2352 -> 2372、ROW_WITHOUT_STATE 595 -> 575、ALIGNED 2352 -> 2372、MISSING_LAST_ROW 149 -> 129（ROW_AHEAD 2591、STATE_OUT_OF_RANGE 2448、INTERIOR_GAP 267、STATES_BEYOND_ROWS 2625 不变），两轮审计逐行 diff 只有这 21 个任务变化；批次 7 client-acceptance 未做（PENDING_CLIENT）；批次 8 focused-test（2026-09-21 用户授权 Maven：17 个测试类 => Tests run 84 / Failures 0 / Errors 0，含 Quest10522AutoStartDialogTest 的 reward 投影 1 与 `var0==0` 自愈边断言；PRODUCTION_COMPILE_OK=6189 / PRODUCTION_COMPILE_FAILURES=0 / PRODUCTION_INTERACTION_OBJECT_FAILURES=0 / PRODUCTION_WHITELIST_VIOLATIONS=0）；批次 8 static（xmllint 8 文件 validates、IDE lint 0 警告、git diff --check 干净）；批次 8 同工作树前后对照（只回放这 8 个文件）：ROW_ALIGNED 2591 -> 2599、ROW_BEHIND 254 -> 246、ROW_STATE_ALIGNED 2372 -> 2380、ROW_WITHOUT_STATE 575 -> 567、ALIGNED 2372 -> 2380、MISSING_LAST_ROW 129 -> 121（ROW_AHEAD 2591、STATE_OUT_OF_RANGE 2448、INTERIOR_GAP 267、MISSING_TAIL_ROWS 90、STATES_BEYOND_ROWS 2625 不变），两轮审计逐行 diff 只有这 8 个任务变化；批次 8 client-acceptance 未做（PENDING_CLIENT）；批次 8 回归收口（2026-09-21 用户授权 Maven）：43 个定向回归类 + 客户端契约门禁 + 批次 1-8 各门禁 => Tests run 302 / Failures 0 / Errors 0；PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0；改动的 21 个测试类改为 Objects.equals(sourceNode(), …)、7 个任务的 reward 行期望按客户端行数更新（1553=2、1988/2988=3、3082=3、14026/24026=5、14051=4、15550/25550=2、24030=9）；xmllint 10 文件 validates；IDE lint 无 error；批次 9（2026-09-21 用户授权 Maven）：Quest28932RewardRowContractTest（3 例）+ ArenaPhaseRowContractTest + QuestClientContractGateTest + QuestDefinitionCatalogManifestTest + ProductionCatalogWhitelistVerificationTest => Tests run 21 / Failures 0 / Errors 0；PRODUCTION_COMPILE_OK=6189 / PRODUCTION_COMPILE_FAILURES=0；xmllint quest_definition.xsd 28932 validates；全库审计同工作树前后对照：ROW_STATE_ALIGNED 2380 -> 2381、ROW_WITHOUT_STATE 567 -> 566、ALIGNED 2380 -> 2381、INTERIOR_GAP 267 -> 266（ROW_ALIGNED 2599、ROW_BEHIND 246、MISSING_LAST_ROW 121 不变），逐行 diff 仅 28932（last_start_var0 None -> 0）；批次 9 client-acceptance 未做（PENDING_CLIENT）；批次 10（2026-09-22，用户授权）：xmllint quest_definition.xsd 18/18 validates；聚焦 Maven 11 个测试类 38 例全绿（RetailSingleStepRewardRowContractTest 4 + MirrorPairRewardRowContractTest 4 + JournalRewardRowRepairContractTest 3 + QuestClientContractGateTest 1 + QuestDefinitionCatalogManifestTest 10 + ProductionCatalogWhitelistVerificationTest 1 + QuestDialog31RegressionTest 2 + QuestMinionTutorialRetailAlignmentTest 3 + ItemCollectingDialogProtocolAlignmentTest 6 + QuestHandoverContinuationAuditTest 2 + QuestRepeatLifecycleTest 2；因并行任务把 PlayerCommonData.java / CM_HOTSPOT_TELEPORT.java 留在编辑中的语法错误态，主工作树无法编译，按项目规则改用临时 worktree 验证并已 `git worktree remove --force` + `git worktree prune`）；PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0；全库审计同工作树前后对照 ROW_ALIGNED 2599->2617、ROW_BEHIND 246->228、ROW_STATE_ALIGNED 2381->2394、ROW_WITHOUT_STATE 566->553、ALIGNED 2381->2394、MISSING_LAST_ROW 121->108（ROW_AHEAD 2591、STATE_OUT_OF_RANGE 2448、INTERIOR_GAP 266、MISSING_TAIL_ROWS 90、STATES_BEYOND_ROWS 2625、BOTH_MISALIGNED 177、NO_REWARD_ROW 178、NO_CLIENT_HTML 608/650 不变），逐行 diff 仅这 18 个任务；客户端实机复测未做（PENDING_CLIENT）；批次 11（2026-09-22）：25608 单任务审计 ROW_BEHIND/MISSING_LAST_ROW -> ROW_ALIGNED（reward_var0 5->6、visible 0..6、rows_without_state 空）；临时 worktree（主工作树被并行任务 13 个 AMBIGUOUS_TRANSITION 阻塞）28 例全绿，PRODUCTION_COMPILE_OK=6189 / FAILURES=0；quest_definition.xsd 与 zones.xsd 均 validates；IDE lint 0 warning；批次 13 static（apply_batch13 脚本 --check 6/6 OK、xmllint --schema 6/6 validates、IDEA lint 0 problem、单任务审计 6/6 ROW_BEHIND/MISSING_LAST_ROW/ROW_WITHOUT_STATE -> ROW_ALIGNED/ALIGNED/ROW_STATE_ALIGNED，全库 ROW_ALIGNED 2633->2639、ROW_BEHIND 211->205、MISSING_LAST_ROW 99->93；RewardRowTwoRowTalkFamilyContractTest 6 例未跑 Maven，PENDING_MAVEN/PENDING_CLIENT）；批次 14 static（apply_batch14 脚本 --check 4/4 OK、xmllint --schema 4/4 validates、IDEA lint 0 problem、单任务审计 4/4 ROW_BEHIND/MISSING_LAST_ROW/ROW_WITHOUT_STATE -> ROW_ALIGNED/ALIGNED/ROW_STATE_ALIGNED，全库 ROW_ALIGNED 2639->2643、ROW_BEHIND 205->201、MISSING_LAST_ROW 93->89）；Maven 原授权命令重跑 29 例全绿 （PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / WHITELIST_VIOLATIONS=0，覆盖本批 4 个 XML 的生产目录编译）；追加授权后 8 个测试类 35 例全绿（含新增 RewardRowEventTwoRowContractTest 6/6）；客户端 PENDING_CLIENT；批次 15 static（apply_batch15 脚本 --check 2/2 OK、xmllint 2/2 validates、IDEA lint 0 problem、单任务审计 2/2 ROW_BEHIND/MISSING_LAST_ROW -> ROW_ALIGNED/ALIGNED/ROW_STATE_ALIGNED，全库 ROW_ALIGNED 2643->2645、ROW_BEHIND 201->199、MISSING_LAST_ROW 89->87）；Maven 已授权 8 类重跑 35 例全绿（PRODUCTION_COMPILE_OK=6189/FAILURES=0）；追加授权后 9 个测试类 41 例全绿（含新增 RewardRowResidualTwoRowContractTest 6/6）；客户端 PENDING_CLIENT；批次 17 追加授权后 mvn 12 个测试类 58 例全绿（2026-09-22 12:31，含 DurableDaevanionWeaponRewardRowContractTest 7/7，PRODUCTION_COMPILE_OK=6189 / FAILURES=0）；批次 18（2026-09-22）：MirrorRewardProjectionLagContractTest（5 例：镜像领奖行参照 + planner 收敛 + 无事务写非领奖行 var0 + 16837/16986/16988 锁定护栏 + 两侧 var0 一致）；15 个测试类 92 例全绿（PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0）；xmllint quest_definition.xsd 6/6 validates、IDEA lint 0 problem；单任务审计 6/6 ROW_BEHIND -> ROW_ALIGNED（recovery=True、rows_without_state 空）、全库 ROW_ALIGNED 2650 -> 2656 / ROW_BEHIND 196 -> 190（row_state 计数不变）；客户端 PENDING_CLIENT；批次 19（2026-09-22）：CollapsedSingleStepLadderContractTest（7 例：reward 投影 == 镜像领奖行 + 每行一个状态 + 客户端页按钮链 + 证据消耗 + can-act/collecting-step 行号 + 旧存档自愈 + 无塌陷跳转）；16 个测试类 99 例全绿（PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0）；首轮 QuestClientContractGateTest 报 BUTTON_WITHOUT_ROUTE（15000：select2 页 1353 无路由）后按客户端页按钮链拆成 1352/1353 两条路由；xmllint 2/2 validates；单任务审计 2/2 ROW_BEHIND -> ROW_ALIGNED、全库 ROW_ALIGNED 2656 -> 2658 / ROW_BEHIND 190 -> 188 / ROW_STATE_ALIGNED 2428 -> 2430 / ROW_WITHOUT_STATE 521 -> 519；客户端 PENDING_CLIENT；批次 31：static（xmllint + quest_definition.xsd 3/3 validates；apply 脚本 --check 幂等 3/3）+ audit（MISSING_TAIL_ROWS 82 -> 79、ROW_BEHIND 179 -> 176、ROW_WITHOUT_STATE 511 -> 508、ROW_ALIGNED 2669 -> 2672、ROW_STATE_ALIGNED 2441 -> 2444、ALIGNED 2441 -> 2444、STATES_BEYOND_ROWS 2622 不变；21217/21244/21249 全部转 ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED、visible=0 1 2、recovery=True）+ focused-test（27 个 reward/row/ladder/owner/catalog 测试类 151 例全绿，含新增 Batch31GelkmarosRowLadderContractTest 8 例；PRODUCTION_COMPILE_OK=6191 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0，2026-09-22）+ 审计脚本新增 [11] 节盘点 MISSING_TAIL_ROWS（本批修复 3 + 已登记例外 6 + 待逐族收口 73）；客户端实机 PENDING_CLIENT；批次 32：static（xmllint + quest_definition.xsd 2/2 validates；apply 脚本 --check 幂等 2/2）+ audit（MISSING_TAIL_ROWS 79 -> 77、ROW_BEHIND 176 -> 174、ROW_WITHOUT_STATE 508 -> 506、ROW_ALIGNED 2672 -> 2674、ROW_STATE_ALIGNED 2444 -> 2446、ALIGNED 2444 -> 2446、STATES_BEYOND_ROWS 2622 不变；13800/23800 转 ROW_ALIGNED / ALIGNED / ROW_STATE_ALIGNED、visible=0 1 2、recovery=True）+ focused-test（28 个 reward/row/ladder/owner/catalog 测试类 159 例全绿，含新增 Batch32KaldorRowLadderContractTest 8 例；PRODUCTION_COMPILE_OK=6191 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0，2026-09-22）；客户端实机 PENDING_CLIENT；批次 33：static（xmllint + quest_definition.xsd 1/1 validates；apply 脚本 --check 幂等 1/1）+ audit（MISSING_TAIL_ROWS 77 -> 76、MISSING_LAST_ROW 77 -> 78（25094 由“缺两行”变“只差共享槽位行”）、25094 visible 0 -> 0 1、recovery=True，ROW_ALIGNED/ROW_STATE_ALIGNED/ALIGNED/STATES_BEYOND_ROWS/INTERIOR_GAP 不变）+ focused-test（29 个 reward/row/ladder/owner/catalog 测试类 167 例全绿，含新增 Batch33SharedVisibleSlotContractTest 8 例；PRODUCTION_COMPILE_OK=6191 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0，2026-09-22）；客户端实机 PENDING_CLIENT；批次 34：static（xmllint + quest_definition.xsd 2/2 validates；apply 脚本 --check 幂等 2/2）+ audit（MISSING_TAIL_ROWS 76 -> 74、ROW_BEHIND 174 -> 172、ROW_WITHOUT_STATE 506 -> 504、ROW_ALIGNED 2674 -> 2676、ROW_STATE_ALIGNED/ALIGNED 2446 -> 2448；30504/30554 转 ALIGNED + ROW_ALIGNED + ROW_STATE_ALIGNED、visible=0 1 2、recovery=True）+ focused-test（30 个 reward/row/ladder/owner/catalog 测试类 175 例全绿，含新增 Batch34RentusBaseRowLadderContractTest 8 例；PRODUCTION_COMPILE_OK=6191 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0，2026-09-22）；客户端实机 PENDING_CLIENT；批次 35：static（xmllint + quest_definition.xsd 2/2 validates；apply 脚本 --check 幂等 2/2）+ audit（MISSING_TAIL_ROWS 74 -> 72、ROW_BEHIND 172 -> 170、ROW_WITHOUT_STATE 504 -> 502、ROW_ALIGNED 2676 -> 2678、ROW_STATE_ALIGNED/ALIGNED 2448 -> 2450；50019/51019 转 ALIGNED + ROW_ALIGNED + ROW_STATE_ALIGNED、visible=0 1 2、recovery=True）+ focused-test（31 个 reward/row/ladder/owner/catalog 测试类 183 例全绿，含新增 Batch35ValentineTowerRowLadderContractTest 8 例；PRODUCTION_COMPILE_OK=6191 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0，2026-09-22）；客户端实机 PENDING_CLIENT；批次 36 追加：static（xmllint quest_definition.xsd 13/13 validates；apply_batch36 --check 幂等 13/13；全库审计 MISSING_TAIL_ROWS 72->62、MISSING_LAST_ROW 78->77、ROW_BEHIND 170->159、ROW_WITHOUT_STATE 502->491、ROW_ALIGNED 2678->2691、ROW_STATE_ALIGNED 2450->2463、STATES_BEYOND_ROWS 2622->2620；13 个任务 ALIGNED + ROW_ALIGNED + ROW_STATE_ALIGNED + visible=0 1 2 + recovery=True）+ focused-test（35 个测试类全绿，含新增 Batch36EventRowLadderContractTest 5 例；PRODUCTION_COMPILE_OK=6191 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0，2026-09-22）；客户端实机 PENDING_CLIENT；批次 37 追加：static（xmllint quest_definition.xsd 5/5 validates；apply_batch37 --check 幂等 5/5；全库审计 MISSING_TAIL_ROWS 62->57、ROW_BEHIND 159->154、ROW_WITHOUT_STATE 491->486、ROW_ALIGNED 2691->2696、ROW_STATE_ALIGNED 2463->2468；5 个任务 ALIGNED + ROW_ALIGNED + ROW_STATE_ALIGNED + visible=行全量 + recovery=True）+ focused-test（36 个测试类全绿，含新增 Batch37TalkKillReportRowLadderContractTest 4 例；PRODUCTION_COMPILE_OK=6191 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0，2026-09-22）；客户端实机 PENDING_CLIENT
boundaries: 与 QE-045 是同一迁移的两条收口方式，不得互相批量套用（批次 2 已按该边界排除 QE-045 锁 10 个与 QE-046 引擎外直写 8 个）：15300/25300 的客户端验收值是进入前的 packed step（13），15001 与 10527 的验收值是客户端领奖行（1、15）；判定必须以客户端 quest_summary 行清单 + 镜像任务为准；10529/10530 等折叠步骤链需要拆分状态而不是只改 reward 行；全库筛查（MISSING_LAST_ROW 492，其中末行是“对话/报告/见面”417、QE-045 锁 10、镜像同样缺末行 192；STATES_BEYOND_ROWS 2627 里 2545 个客户端只有 1-2 行、var0 实际是计数/阶段槽）中多数任务 var0 并非任务书行索引（反例：1221 的 var0 是击杀计数、1001 客户端 5 行而服务端 var0 0..8、10504 的 reward=4 而客户端只声明 4 行），禁止机械批量改；行号判据的另一处已知偏差：任务书里可能有与相邻状态共用 visible 槽位的空行（10530 第 8 行与第 9 行同为 [%24]），此时 client_rows 比真实状态数多 1，必须用“可见槽位集合 + 镜像 20530 + 旧 handler 步骤序列”收口（10530 reward=9 而不是行号推出来的 10）；SECTION 位段隔离的 14 个挂账任务（1842/1843/1844/2843/2844/2845、4928、16800、18738、19078、20034、28738、29074、29078）需要逐个客户端脚本证据，禁止机械搬 offset；24201/24202 仍未收口（quest_script 是 Progress(SECTION_0<12; SECTION_5==0) 与 Progress(2)，SECTION_0 可能同时充当击杀计数槽，先观测再决定补 stage1 还是改计数模型）；多计数器任务（18208/18209/28208/28209 族）的领奖行投影按第 4 条行状态判据（reward=2），与 QE-045 的 pre-REWARD step 例外族（15300/25300 等有客户端验收记录）分开；这一族的客户端 script 只声明到行 0/1 的 SECTION_0，不代表领奖行不需要独立状态；SECTION_0 约束审计的 `UNREACHABLE` 分两类：18208/18209/28208/28209 是迁移丢阶段（可按第 11 条修），1922/2947/14054 是 legacy 从未实现客户端声明的中间阶段（1922/2947 用 `defaultCloseDialog(env, 0, 4)` 直接跳过 var0=1/2/3 的对话+收集+野外击杀；14054 只实现第一轮，缺第二轮 12/13、Barantina 两次对话与最终报告），后者属于任务链实现缺口（需要补对话/传送/掉落/击杀阶段并逐页核对客户端 HTML），不得按行投影模板机械修；批次 7 边界：只收口“末行 NPC 命中 reward 路线”的报告/交付型 21 个；10522/20522/15542/25542/30211/30213/30311/30313 被 QE-046 基线（src/test/resources/quest/external-reward-advance-baseline.tsv）锁住——reward 投影必须等于引擎外写入方留下的 packed step（批次 8 已把这 8 个任务的写入方与投影统一到领奖行 1：写入方先写 var0=1 再置 REWARD、投影 0->1、自愈边改 `var0==0`，并重刷 src/test/resources/quest/external-reward-advance-baseline.tsv），不能只改 XML；30614（末行 Astella=800327，定义 NPC 却是 Aluna=800326）、26838（末行 Jarik01=806574，定义 806575）、19064/29064（末行 Jucleas/Balder，定义 Lavirintos/Kvasir）需先核实领奖 NPC 身份；28932/30203/30303 的 started 节点没有 var 投影，要补 var0 投影才能让行 0 有状态；1607/1990/2990/3502/14012/14013/17511/27511 属多阶段/内部缺口（INTERIOR_GAP、MISSING_TAIL_ROWS），必须单独设计阶段推进；批次 8 补充边界：无 source 自愈边会让所有 `sourceNode().equals(...)` 形式的测试 helper 抛 NPE，新增该族边时必须同步做 null-safe 化（本次 21 个类）；领奖态入口页与 `npc-complete` 必须成对出现在同一个 owner 上，只加半边的写法会被 QuestClientContractGateTest 直接判失败；引擎外写入者任务（10522/20522/15542/25542/30211/30213/30311/30313）的 reward 投影、写入方与基线 TSV 三处必须同改；28208/28209 的 Inggness(205320) 与 Anja(205321) 目前并存为领奖 NPC，实机复测若确认 retail 只允许 Anja，再按 QE-046 收口 Inggness 的 NPC_REPORT/npc-complete；批次 9 边界：自闭合 `started` 的补投影只允许声明行号字段，声明计数器会把“满计数恢复路线”挡掉（28932 的 var1>=1 对话路线有回归门禁）；`var0` 作为标志位/计数槽的任务（30203/30303 等）属审计误报族，必须用 legacy handler 与客户端 SECTION 声明核对后再决定是否收口，禁止按“末行行号”机械推进；批次 10 边界：retail 单步族的收口模板是族内 184 个已对齐任务，不要再按“末行 NPC 命中”批量推进；21455 的 retail end_npc_ids=799244（server `name_desc=Unset`，与客户端 `STR_DIC_N_Unset` 一致）与 typed 定义当前把领奖放在起始 NPC 799404 上不一致，NPC 归属需单独核实（本批只改行投影）；25608 已在批次 11 收口（retail 7 step ↔ 客户端 7 行；实际缺 ENTER_AREA 206534 与 ENTER_AREA 206542 两行，旧链把 HUNT/Mumu/交付行错放在 var0=2/3/4、reward 停在 5；修复为 step2/step5 两个 enter-zone、HUNT→step3、Mumu SELECT5→step4、交付行归 step6、reward 5→6，并从客户端 Levels/DF6/Level.pak 的 mission_mission0.xml 取触发点注册两个 sensory zone）；多阶段 ENTER_AREA 行对齐必须同时补 zones_quest.xml（zone 名沿用同族 A/B_DYNAMIC_ENV 规则），只改 quest XML 会因 zone 未注册而永远不触发；29002 的 legacy handler 语义是“204099 的 STEP_TO_1 写 var0=1、204257 领奖不改 var0”，与镜像 19002 同值；19008/19014/19020/19026/19032 等“名人考试”族的 reward=1 属 legacy 语义（19057/29057 的 handler 另有 var0=2 的失败分支），不得按“末行行号 2”修改；剩余 MISSING_LAST_ROW 95 个（108 − 13）里镜像同缺 32、QE-045 锁 10，其余需逐族 legacy/retail 证据；批次 12（2026-09-22）已核实 19064/29064/21455/30614 的领奖 NPC 归属（30614 由 Aluna 800326 回滚为 Astella 800327：客户端行 1「向 Astella 报告」+ quest_complete「阿斯泰拉说必须…」+ 同族命名规则 4/4 + 2026-08-06 f737cfef1 客户端/真端交叉审计；legacy `terath_dredgion.xml` 单源不足以覆盖），26838（末行 Jarik01=806574 / 定义 806575）仍待单独取证；批次 13 边界：同形的 QE-045 锁任务（13965/23965 由 enter-zone、15674/25674 由 CHECK_COLLECTED_ITEMS 置 REWARD）不得随族批量翻动——它们的 `reward var0=0` + `REWARD && var0==1` 恢复边是 f6aff952a 的基线并被 LegacyRewardStepProjectionRegressionTest 20 例硬锁，重定基线需一次客户端观测（REWARD 态任务书高亮行 0 还是行 1）；`last_row_npc_matches_quest=False` 在客户端表名带前缀（LF2a_dromik_G_DHM、DF2a_Nevma_G_LHM、IDRUN_Entrance_guide）时只是 STR_DIC key 不同形，不等于归属错误；批次 14 边界：8060x 族里只有头本 80601/80606 的 var0 是行索引，80602..80605/80607..80610 的 var0=3/4/5/9 是阶段计数（审计 STATES_BEYOND_ROWS），禁止按行号机械压成 1；行内用字面中文名（80255/80256）“和帕尔图对话”或非 NPC 键（1123 的 STR_DIC_LA12）时，归属必须用客户端 NPC 表 + npc-complete owner 交叉核对；1466 的 reward 节点没有 var0 投影（NO_REWARD_ROW）、2484/4712 的 completion owner 不唯一、2842 由击杀自环推进，均须单独设计；批次 15 边界：1466 被 `Quest1466ClientDialogAlignmentTest` 硬锁（reward 必须无投影、报告路由必须写 `var0=2`、headless journey 通过），按行号收口需先重定该基线（要有客户端观测）；4712 的行 1“向 Henir 报告”与 completion owner（279042 / 798327 / 798330 = Dreadgion_prisoner_dark1/4）不一致，属归属核实题；2842 的 var0 是 0..39 击杀计数且行 1 槽位是 `[%15]`（值 5）；行内键若属 actor 命名空间（`STR_DIC_LA*`）必须用同族 completion owner 交叉解键，不能用 `last_row_npc_matches_quest=False` 直接判缺陷；批次 17 边界：LEGACY_NEXTSTEP_DROPPED 族已清空，修领奖行前先跑 .agents/summary/quest-10527-reward-row/audit_legacy_reward_entry_steps.py 取 legacy packed step（区分“迁移丢 nextStep”与“legacy 本身保持 step”）；STEP_EQUALS_NEXTSTEP 的 392 个任务（如 2600/1920/2945 的 defaultCloseDialog(env, s, s, true, ...)）与 QE-045 基线（15300/25300 的 reward=13、3722/4722 等 QE045_LOCKED）不得按“客户端末行”机械推进；同族变体若客户端只有 1 行，禁止保留越界投影（80290/80294 即 STATE_OUT_OF_RANGE）；批次 16 边界：4712/2484 的 completion owner 已按 QE-052 从行 0 囚犯（798327/798330）与接取 NPC/烽火对象（204407/700267）收敛到领奖行 NPC（279042/203331），这两个任务不再属于“owner 不唯一”的挂账；其余 owner 归属只允许在保留入口路由的前提下收敛；批次 18 边界：镜像单侧投影落后族一律以“已对齐的那一侧”为参照基线（两侧 quest_summary 行数相同、末行 NPC 都能在任务内对上），只改 reward 投影 + 补自愈边，不动状态阶梯；16837/16986/16988 被 QuestPrematureRewardRouteExclusionTest 的 RewardCase.reward={var0:0} 锁定（完成报告后 packed var0 保持 0），属既有契约，未取到客户端观测前禁止按镜像推进；镜像两侧投影比较只比行号 var0，镜像侧可另有计数槽（27160/27161 带 var1=10）；批次 19 边界：单步塌陷对（Elyos 存根：镜像已有完整阶梯、本侧只剩行 0）**不能只改 reward 投影**——那只指向一个服务端没有状态的行，必须按客户端 quest_summary 行数与对话页 HACTION 按钮链补回 START 阶梯；`npc-complete` 的 `<preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>` 已展开 reward 自环，不得再显式声明 SELECT_QUEST_REWARD 的 reward 自环（AMBIGUOUS_TRANSITION）；客户端页上的按钮必须在服务端有落地页路由（1352 -> SELECT2、1353 -> SELECT2_1），否则 QuestClientContractGateTest 报 BUTTON_WITHOUT_ROUTE；剩余塌陷对 23809/13809（还需 owner 收敛）、23918/13918（串行阶梯 vs 组合计数口径未定）、24046/14046（INTERIOR_GAP，缺行 4/7）、1000/11000、39713/49713（页动作不同形）需逐族处理；批次 31 补充：MISSING_TAIL_ROWS 残留 73 个分三类——legacy 递增但需先判 var0 是否行号、legacy 阶梯短于客户端末行（末行不亮属 legacy 行为）、无 legacy handler（需数据侧取证），禁止按客户端行号机械补阶梯；21249 的换装 spawn（删 799416、生成 799529）未在本批落地，行 1 仍依赖世界中的 799529；批次 32 补充：单个任务的自愈边可能不止一条——旧定义与 legacy 会留下不同旧值（13800/23800 的 0 与 1），只补其中一条会让另一类存档匹配不到 reward 路由；同形态候选 14200/24155 属 STATES_BEYOND_ROWS 计数行、25094 是 use-item 族、30504/30554 是 SET_SUCCEED 塌陷族，均不适用本族模板；批次 33 补充：只有确认 `quest_summary` 的相邻行**共用同一个 `%N`** 时才能把状态数减一；槽位序列为 `0,3,6,...`（大多数任务）时行号与状态号一致，仍按行号口径处理。已知未收口同族形态：10525/20525 的槽位是 `0,3,6,18,21,24,27,30`（状态 3/4/5 缺槽、两套口径不一致）、10530 的第 8/9 行共用槽位，都需要各自取证，**不得**用 25094 的结论直接套改；批次 34 补充：retail `end_npc_ids` 与客户端末行 NPC 冲突时（30504/30554 的 799536 vs 205438），按 QE-052 取客户端末行 NPC，把冲突登记为实机复测点而不是当场回退；`var0` 字段宽度是契约的一部分，补行阶梯前必须确认 `progress` 上限容纳目标行；无迁移前 handler 的任务（retail/数据驱动迁移）没有 legacy 落盘值可依，只能用客户端槽位/页链 + retail step 取证；批次 35 补充：事件任务族（50019/50020/50022/51019 与 80300-80309）共享同一页链形态（select1 的 CHECK→ check_user_item_ok 的 SETPRO2 → select_success 的 SELECT_QUEST_REWARD），但各自的收集物与世界物件 id 不同（塔 701466/701467、箱 701470/701774 等），必须逐任务确认后才能套用同一模板；`npc-item-report` 只适用于“交付即领奖”的任务，不能用于需要中间行的阶梯；批次 36 追加（并修正批次 35 的临时归族）：塔族 select1/check_user_item_ok/collect_progress=0 与货箱族 select4/select5/collect_progress=4 页链不同，物件 id 与 collecting_step 不得互相套用——50022 与 80302/80303/80308/80309 属货箱族，不是塔族；活动怪/NPC/塔/箱（219315/219316/219639/219640、202549/799763、701466/701467/831402/831403、701470/701774）在本检出无静态 spawn，整族由活动系统下发，不新增“未击杀即锁死”的门；51022 的 var0 由 width=1 放宽到 6 bit，50021/51021 的 15 计数槽按行号归零重建；80304/80305 是另一页链形态（回答->去见术古，无 items/drop）仍挂账；塔/箱未刷出时保留“直接找领奖 NPC 对话”的防呆入口；批次 37 追加：只有 legacy 合同明确给出两个不同 start_npc_ids/end_npc_ids 时才允许拆 owner（26905/26906/26908 的 Aegir/Nerthus 只管接取，Gangleri/Tyr/Svafnir 负责进度与领奖）；3711/4711 的 start=end 同为报告 NPC，owner 不得拆；三行 bounty 族的行 1 击杀行与行 2 报告行必须分开，四行 Dredgion 族必须保留“行 1 术古情报”中间行——不得把情报行并进击杀行；中间行的 NPC 对话只显示进行中反馈，不写 var0
superseded_by: none
see_also: [QE-012], [QE-045], [QE-046]
first_check: 领奖阶段任务书停在“使用道具/调查”一行或空白时（先确认末行确实是领奖行：末行 STR_DIC_N_XXX 要能在 npcs_unpacked/client_npcs_npc.xml 的 <name> 里对上任务内 NPC，对不上就先别改）（审计脚本的客户端索引必须大小写不敏感：Dialogs 下同时存在 quest_q<id>.html 与 QUEST_Q<id>.html，否则 2016 个任务会被误判成 NO_CLIENT_HTML），先跑 .agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py 比对 reward 投影与客户端 quest_summary 行索引、并核对“每一行都有 START/REWARD 状态”（缺口行 = 永远不亮的行），再看 (q, q+10000) 镜像是否一致，最后按客户端领奖行同时交付投影与无 source enter-world 恢复边；动手改 reward 投影前先确认任务书的可见槽位与行数是否一一对应（client_rows != client_states 时空行只与相邻状态共槽，不要按行号加一），再确认所有 varN（N>=1）落在 6N，否则客户端读到的 SECTION_0 根本不是 var0
keywords: 领奖任务书不切换、10527、reward 投影、SECTION_0、quest_summary、领奖行、useQuestItem、changeQuestStep reward、REWARD var0、任务书停在上一行、活动巧克力塔族、术古货箱族、collecting_step、can-act ACTION_ITEM_USE、select4/select5、AMBIGUOUS_TRANSITION、start/end NPC 分离、26905/26906/26908、3711/4711、Dredgion、Dark Raider、SETPRO1/SETPRO2、50019/51019
-->

- **判定规则**：`reward` 节点的 `var0` 投影就是客户端任务书 `quest_summary` 的行索引（SECTION_0），必须等于领奖行——默认是最后一行“和 X 对话/报告”；交接 transition 不改写该字段时由目标投影补足；已落盘的错位存档必须有 `status=REWARD && var0==from` 的无 source `enter-world` 恢复边。交互物族（批次 20 补充）：由世界物件/交互物（`ai="quest_use_item"` 的 FOBJ、`USE_OBJECT` 路由）承载中间行的任务，每一行必须是**该行交互物**的两条边——`USE_OBJECT -> SHOW_QUEST_PAGE 该行自己的页`（不能三行共用同一页，页 id 见客户端 `select2/select3/select4`）+ `SETPROx -> 下一行` 并**发放本行采集物**（`give-item`）；发放依据是 `quest_data.xml` 只声明 `quest_work_items`、交互物没有 `quest_drop`（否则玩家永远拿不到交付物，`has-item` 交付分支永不可达）。同步模式对标 legacy `sendUpdatePacket`：START 行只发 `SM_QUEST_ACTION`（`PACKET_ONLY`），进入 REWARD 的那一行额外 `updateZone/updateNearbyQuests`（`LEVEL_AND_VISIBILITY_REFRESH`）。副本段补充（批次 22）：与副本 NPC 对话后**不得**一步跳到副本内行；进副本的行要由 `enter-world + world-is <副本世界>` 驱动，并保留副本外的回退（`die` / `expected=false` 的 `enter-world`）回到“可重新进入”的那一行，否则被跳过的行永远不亮、副本内掉线或死亡也会卡在没有对话的行号上。
- **为什么容易漏**：旧 `QuestHandler.changeQuestStep(..., reward=true)` 只置状态、不写 `nextStep`，迁移时 `to` 看起来“没有产物”，reward 投影就地抄了 `from`；静态门禁、目录编译、白名单全绿，只有玩家跑到领奖那一步任务书才停在上一行；同批镜像任务分别迁移，两侧差 1 也没人比对。
- **代表案例**：10527（`useQuestItem(env, item, 14, 15, true)`，reward 投影应为 15，镜像 20527 即 15；2026-09-21 修复并补 `REWARD/var0=14` 恢复边）、10528（reward 投影 11 -> 12、交接写 12，镜像 20528 本就是 12）与 10525/20525（两侧都是 6、客户端 8 行只到第 6 行，第 7 行领奖行无状态；同日改为 7 并补 `REWARD/var0=6` 恢复边）；批次 1（2026-09-21，用户授权后执行）：镜像一侧已对齐的单侧缺陷 9 组——11294->1（镜像 21294）、15002/15010/15070/15514->1（镜像 25002/25010/25070/25514）、19004->2（镜像 29004）、25062->1（镜像 15062）、25073->1（镜像 15073）、26820->2（镜像 16820），全部按“reward 投影 + 无 source enter-world 自愈边”收口并由 AlignedMirrorRewardRowContractTest 锁定；批次 2（2026-09-21）：天/魔镜像两侧都缺末行的 120 个任务（10110/20110、13700/23700、13961/23961、14031/24031、15323/25323、15502/25502、18806/28806、28940/28940 一类的对话/报告末行），统一 reward 投影 N-2->N-1 + enter-world 自愈边；未纳入：QE-046 引擎外直写 8 个、末行与首行重复 12 个（15300/25300 型）、末行 NPC 对不上 68 个、reward 路线已写 var0 的 12 个（25052/28253 已单独补齐）、以及条件不自洽的 18036/28036；批次 3（202 个任务）：末行 STR_DIC_N_XXX 命中 reward 路线 NPC 的单侧任务（11001/11009 交信链、1218/1319/1322/1324 交付链、1361/1430/1452/1464 对话交付链等），统一 reward 投影->末行 + enter-world 自愈边；未纳入：2600/21075（末行 NPC 不在 reward 路线）、3090（紧凑布局、加宽会与 var1/var2 重叠）、24201/24202（reward 行已修，但仍缺 var0=1 的击杀阶段，形状为 INTERIOR_GAP）、10529/20529/10530/20530 折叠链；三批累计修复 331 个任务；同族待确认：10529/20529（缺第 11 行、第 10 行报告目标压在 REWARD 上，需拆折叠链）、10530/20530（缺第 9/10 行且两侧客户端行数不同）、10522/20522（外部写入者，见 QE-046）；批次 4（2026-09-21，用户授权后执行）：10529/20529 把压在 REWARD 上的报告行拆成独立 START 节点 s10（var0=10）并把 reward 推到最后一行 11，接回 select11/select11_1/select11_1_1 报告链并补 QuestDialogPage/QuestDialogAction 的 SELECT11_1_1(6502)，10530 与镜像 20530 对齐到 reward=9（第 8 行是与第 9 行共槽的空行，行号判据会多算一行），3090 把 var1/var2 从 SECTION_0（bit 2/3）移到 bit 6/12 并把领奖行从 3 推到 4，新增全库 varN=6N 门禁（14 个历史任务显式挂账），三者由 JournalReportRowSplitContractTest 与 ClientQuestSectionAlignmentTest 锁定；仍未收口：24201/24202（INTERIOR_GAP，SECTION_0 疑似计数槽）以及位段清单里其余 14 个任务；批次 5（2026-09-21，用户授权后执行）：15550/25550（三段对话链）与 15551-15554/25551-25554（感应区坐骑）共 10 个任务——按 origin/history 旧 handler 恢复行推进（坐骑族 A_TO_X var0 0->1 + X_TO_A var0 1->2 并置 REWARD）、新增中间行 START 节点 s1、reward 推到第 3 行并同步 `var0` width/max 1->2、保留迁移的两条 unaccepted->started 自动接取、补 REWARD/var0=1 -> 2 自愈边，由 SensoryAreaRideRowContractTest（8 条合同）与 MirrorPairRewardRowContractTest（追加 15550/25550）锁定；批次 1-5 累计修复 345 个任务（331 + 批次 4 的 4 个 + 批次 5 的 10 个）；批次 6（2026-09-21，用户授权后执行）：18208/18209/28208/28209（苍穹试炼场单人竞技场）——按客户端 quest_script 的 SECTION_0 声明恢复“2 阶段 + 2 计数器”模型（5 次 217819 击杀在 var1、精英 218185/218200 写 var2 并进入 REWARD，并把客户端未声明的 218192 移出目标）、reward 投影按领奖行合同设为 2、补 4 条旧存档收敛边（旧 k1..k4 回第 1 行、k5..k7 进第 2 行、REWARD 低/高值收敛到领奖行），由 ArenaPhaseRowContractTest（6 条合同）锁定；批次 1-6 累计修复 349 个任务（331 + 4 + 10 + 4），并新增 SECTION_0 约束审计（check_section0_requirements.py，UNREACHABLE 5 -> 3）；批次 7（2026-09-21，用户授权后执行）：报告/交付型末行 21 个——成长任务族 18 个（19672..19694 / 29672..29694，客户端 2 行“收集成长货币 → 向成长支援教官报告”，定义里唯一 NPC 806698/806700 既接取又领奖）reward 0->1 + enter-world 自愈边；3210（客户端 3 行，末行“和 Shugo_Shulack_02 对话”，镜像 4210 已是 2）reward 0->2 + var0=0/1 两条自愈边；18036/28036（2 行“和德拉坎战士对话 → 向 Demades/Latkel 报告”）新增 s1(START,var0=1) 节点、SETPRO1 事务改指 s1、两条 `var0>=1` 报告事务改以 s1 为 source、reward 0->1 并补自愈边（修掉“交出物品后无任何事务可匹配”的卡死），由 ReportRowRewardProjectionContractTest（5 条合同）锁定；批次 1-7 累计修复 370 个任务（349 + 21）。批次 8（2026-09-21，用户授权后执行）：QE-046 引擎外写入方与领奖行求交的 8 个任务（10522/20522 CM_CREATIVITY_POINTS、15542/25542 CoalescenceService、30211/30213/30311/30313 RiftOrbAI2）——写入方在置 REWARD 前写领奖行 `setQuestVarById(0, 1)`、reward 投影 `0 -> 1`、旧自愈边改 `var0==0`，重刷 external-reward-advance-baseline.tsv，由 ExternalRewardAdvanceReentryContractTest 与 Quest10522AutoStartDialogTest 锁定；批次 1-8 累计修复 378 个任务（370 + 8）。；批次 10（2026-09-22，用户授权后执行）：retail 单步族（start + 1 step，249 个里 184 个早已 reward var0=1）落后的 18 个 — A 组 12 个末行完全没有状态（1527/1528/1725/2135/2247/2266/3087/4020/21455/26838/80735/80736）、B 组 5 个末行有中间态但 reward 停在行 0（1963/1964/16838/16977/18035）、C 组 29002（镜像 19002 已对齐 + legacy handler 证据）——统一 reward 投影 0->1 + 无 source enter-world 自愈边，由 RetailSingleStepRewardRowContractTest 锁定；审计 ROW_ALIGNED 2599->2617、MISSING_LAST_ROW 121->108；批次 11（2026-09-22，用户授权后执行）：25608 补齐两个 ENTER_AREA 行（step2 206534 / step5 206542），HUNT→step3、Mumu SELECT5→step4、交付行归 step6，reward 投影 5→6 并补 REWARD/var0<6 的 enter-world + QUEST_SELECT 自愈边；drop collecting-step 0→6；从客户端 Levels/DF6/Level.pak 的 mission_mission0.xml 触发点注册两个 zones_quest zone，由 Quest25608RetailSevenStepAlignmentTest（6 条合同）锁定；单任务审计 ROW_BEHIND→ROW_ALIGNED；批次 12（2026-09-22）：领奖 NPC 归属核实族 4 个任务——19064/29064 新增 s1(START,var0=1)、行 0 路由交 Undin/Darfen(798450/798452)、领奖/completion 从起始 NPC 203701/204053 改到 Jucleas/Balder(203752/204075)、reward 0→1 + `var0=0/2` 双自愈边；21455 领奖/completion 799404→Unset(799244)、删除客户端无按钮的 SETPRO2、换物品并入 SETPRO1；30614 reward 0→1 + `var0=0` 自愈边 + 领奖态入口页 `reward + QUEST_SELECT(31) -> DEFAULT_SUCCESS(10002)`（位于 npc-complete 之后且唯一），并把归属从 Aluna(800326) 回滚为 Astella(800327)（客户端行 1 + quest_complete + 同族 4/4 + f737cfef1；10a2e7e57 仅据 legacy 单源改错并登记在 report-npc-mismatch.csv 第 64 行）；由 RewardNpcOwnershipContractTest（6 条合同：owner 归属、talk 路线集合、Dredgion 族命名规则、自愈边与 planner、入口页唯一且在 npc-complete 之后、SETPRO2 删除）锁定；单任务审计 4/4 ROW_ALIGNED/ALIGNED/ROW_STATE_ALIGNED，`last_row_npc_matches_quest` 523→519；批次 13（2026-09-22）：5.8“两行、末行是与领奖 NPC 的对话”族 6 个任务（1926/2938 承 `_1926/_2938Secret_Library_Access`，39003/49003 客户端行 1 = 800504/800505，80989/80990 事件链 836196）reward 投影 0->1 + 无 source 自愈边，由 RewardRowTwoRowTalkFamilyContractTest（6 条合同，含“QE-045 锁的 4 个同形姊妹任务必须保持 reward var0=0”的显式边界）锁定，单任务审计 6/6 ROW_ALIGNED/ALIGNED/ROW_STATE_ALIGNED；批次 14（2026-09-22）：事件族 4 个任务（80255/80256 是 80255..80260 活动烟花族的漏网项，同族 80257-80260 已由批次 1-7 `7a7d27809` 对齐；80601/80606 是德雷得奇安事件链头本，legacy `setQuestVarById(0, 1)` 是硬证据）reward 投影 0->1 + 无 source 自愈边，由 RewardRowEventTwoRowContractTest（6 条合同：投影/owner/自愈边 + planner/无事务写非领奖行 var0/同族参照 80257-80260 保持 1/击杀事务保留 legacy var0=1）锁定，单任务审计 4/4 ROW_ALIGNED/ALIGNED/ROW_STATE_ALIGNED；批次 15（2026-09-22）：残余两行族 1123/2484（STR_DIC_LA12 跨族解键 = Pernos 790001；legacy `_2484OurManInElysea` 的烽火 `setQuestVarById(0, 1)`）投影 0->1 + 自愈边，由 RewardRowResidualTwoRowContractTest（6 条合同：投影/owner/入口路由保真/自愈边 + planner/无事务写非领奖行 var0/STR_DIC_LA12 解键护栏）锁定；至此两行 + 末行对话族 12 个全部收口；批次 16（2026-09-22）：领奖 owner 收敛 4712/2484（见 QE-052）；批次 17（2026-09-22）：圣灵守护者武器事件族 80290/80291/80294/80295——先用 .agents/summary/quest-10527-reward-row/audit_legacy_reward_entry_steps.py 扫描迁移前 handler 的进入 REWARD 调用（651 个任务），把“nextStep 被丢弃”精确到 15300/25300（QE-045 基线）+ 80291/80295（收口后 LEGACY_NEXTSTEP_DROPPED 为空）；80291/80295（客户端 2 行）投影 0->1、80290/80294（客户端仅 1 行、原投影 1 属 STATE_OUT_OF_RANGE）投影 1->0，各补对应自愈边，由 DurableDaevanionWeaponRewardRowContractTest（7 例）锁定；批次 18（2026-09-22）：镜像单侧投影落后族 11110/14201/16974/17160/17161/17526——同形镜像对（q/q±10000）客户端行数相同、末行 NPC 都能在任务内对上，但只有一侧 reward 投影等于领奖行，已对齐侧作基线（11110/21110=1、14201/24201=2、16974/26974=1、17160/27160=1、17161/27161=1、17526/27526=1），6 个任务本就有完整 0..N-1 行状态，修复只需投影 + REWARD/var0=0 自愈边，由 MirrorRewardProjectionLagContractTest（5 例）锁定；批次 20（2026-09-22）：焦树族 23809/13809（三棵共用世界物件 DeadTree 730969/730970/730971，客户端 4 行：行 0/1/2 依次调查三棵树并采集 quest_<id>a/b/c，行 3 向 LDF5_Fortress_Village_Guard01_D(802429)/L(802427) 报告）——23809 被塌陷成“三棵树各一条 started -> reward 直跳 + 整包交付”（缺行 1 2）、13809 阶梯已对但三棵树各挂 NPC_START/npc-complete（QE-052）；本批两侧同形重建阶梯、每棵树只开自己的页（SELECT2/3/4）并按 SETPRO1/2/3 推进行 + 发放本行采集物、行 3 用 `reward --QUEST_SELECT --> SELECT5` 承接、owner 收敛到守卫，由 TreeLadderOwnerTrimContractTest（7 例，含 planner 顺序门禁）锁定；单任务审计 ROW_BEHIND -> ROW_ALIGNED（23809）、全库 ROW_ALIGNED 2658 -> 2659 / ROW_BEHIND 188 -> 187 / ROW_STATE_ALIGNED 2430 -> 2431 / ROW_WITHOUT_STATE 519 -> 518；批次 22（2026-09-22）：24046（The Shadow Calls）——legacy `changeQuestStep(env, 3, 5, false)` 把行 4「进入 DC1_door_Q2076 寻找沉默审判官」整格跳过、reward 投影停在 6（客户端末行是行 7“向 Muninn 报告”）；修复补 `s4` 让 3→4 只推一格并保留副本传送、4→5 由进入副本 320120000 驱动、4→3 做副本外回退、reward 投影 6→7 + 无 source `REWARD/var0=6 -> 7` 自愈边，由 ShadowCourtRowLadderContractTest（6 例）与 JournalRewardRowRepairContractTest 的 `Contract(24046, 7, 6)` 锁定，报告 §二十六；批次 23（2026-09-22）：39713（[Daily] Fresh Powder，绿帽团阵营日任天族侧）被迁移把 3 行任务书塌陷成 9 条无守卫 `started -> reward` 直跳（npc-item-report ×3 + SET_SUCCEED ×3 + SELECT_QUEST_REWARD ×3），行 1/行 2 没有任何 START/REWARD 状态、`started --QUEST_SELECT` 错开成报告页 select5、reward 投影停在 0（客户端领奖行是 2）、报告路由还重复要求已经用掉的净化粉末 182215285；按客户端行号重建阶梯（`started --SETPRO1--> powder-received(1)` 发 182215285、`powder-received --use-item--> powder-used(2)` 消耗道具、`powder-used --SELECT_QUEST_REWARD--> reward(2)`、`started`/`powder-used` 的 QUEST_SELECT 分别给 select2/select5）+ 无 source `REWARD/var0=0 -> 2` 自愈边，`var0` width=1/max=1 -> width=2/max=3，与已对齐的魔族镜像 49713 同形，由 FactionDailyRowLadderContractTest（7 例）与 JournalRewardRowRepairContractTest 的 `Contract(39713, 2, 0)` 锁定，报告 §二十七；批次 24（2026-09-22）：空槽位族登记 —— 只读扫描 9116 个客户端任务书，13 个任务的 quest_summary `<step>` 可见文本全空或尾随为空（序幕 1000/2000 的 4 个空槽只挂 `[%collectitem]` 占位符，quest.xml 既无 collect_item 也无 NPC，服务端 enter-zone → movie → complete、无 REWARD 节点；1400 的行 0 是击杀计数、var0/var1 是 8x4 组合），行号口径会误判成 NO_REWARD_ROW / MISSING_TAIL_ROWS / ROW_AHEAD；登记为审计脚本的 `BLANK_JOURNAL_SLOT_EXCEPTIONS`（判定不变），由 BlankJournalSlotBoundaryContractTest（4 例）锁定“不得按空槽补行阶梯”，扫描脚本 audit_blank_journal_slots.py 与明细 blank-journal-slots.tsv，报告 §二十八；批次 31（2026-09-22）：Gelkmaros 三行交接塌陷族 21217/21244/21249——旧定义把全部 NPC 塌陷成“接取 + 一步领奖”（客户端 3 行里行 1/行 2 拿不到 START/REWARD 状态，审计 MISSING_TAIL_ROWS + ROW_WITHOUT_STATE），按 legacy `defaultCloseDialog(0, 1)` / `setQuestVar(2)` + `defaultCloseDialog(2, 2, true, false)` 与客户端页按钮链重建 `started(0)/s1(1)/reward(2)`、owner 收敛到 799226/799317/799417、补 `REWARD/var0=0 -> 2` 自愈边，由 Batch31GelkmarosRowLadderContractTest（8 例）锁定（报告 §三十五、证据 batch31-evidence.tsv、脚本 apply_batch31_gelkmaros_row_ladder.py）；批次 32（2026-09-22）：卡多尔迎新镜像对 13800/23800 是“客户端页链长于 legacy 一跳”的形态——legacy 在传送点写 1、在迎宾 NPC 只 setStatus(REWARD)，旧定义又把 804699/804782/802431（及 804719/804753/802433）全塌陷成接取+一步领奖，reward 投影停在 0；两侧按页链重建 started(0)/s1(1)/reward(2)、owner 收敛到 802431/802433、补 `REWARD/var0=0 -> 2` 与 `REWARD/var0=1 -> 2` 两条自愈边，由 Batch32KaldorRowLadderContractTest（8 例）锁定（报告 §三十六、证据 batch32-evidence.tsv、脚本 apply_batch32_kaldor_row_ladder.py）；批次 33（2026-09-22）：25094 是“共享可见槽位”唯一成员——客户端 3 行槽位 `%0/%3/%3`（全库 3 行任务里唯一重复），retail 单步 `COLLECT_ITEM 702768`、领奖合同 31 -> DEFAULT_SUCCESS / 1009 -> REWARD，迁移前 `checkQuestItems(env, 0, 1, true, ...)` 的 nextStep=1 与槽位口径一致（旧 helper 的 reward 分支不写 nextStep，落盘仍是 0）；本批把 reward 投影 0 -> 1、补 s1(1) START 状态与 `REWARD/var0=0 -> 1` 自愈边，由 Batch33SharedVisibleSlotContractTest（8 例）锁定（报告 §三十七、证据 batch33-evidence.tsv、脚本 apply_batch33_shared_visible_slot_row.py）；批次 34（2026-09-22）：Rentus Base 营救 Paios 镜像族 30504(Elyos)/30554(Asmodian)——客户端三行（槽位 %0/%3/%6）是“挪开柱子 701098(IDYun_Column_Q30504)”->“Paios(799536) 遗言页 select2/SET_SUCCEED”->“向 Lition(205438) 报告并领奖（select_success/SELECT_QUEST_REWARD）”；retail 只登记一个 ACTION action_ids=701098、end_npc_ids=799536、领奖合同 31->10002/1009->REWARD；迁移前无 Java handler，旧定义把三行塌陷成 started 传 SET_SUCCEED(205438) 直达 reward（该按钮只在 Paios 页上）、reward 投影 0、var0 仅 1 bit、柱物件未接 IR；本批重建 started(0)/s1(1)/reward(2) + 柱物件路由 + owner 收敛 205438 + 0/1 两条自愈边，由 Batch34RentusBaseRowLadderContractTest（8 例）锁定（报告 §三十八、证据 batch34-evidence.tsv、脚本 apply_batch34_rentus_base_row_ladder.py）；批次 35（2026-09-22）：情人节巧克力塔镜像族 50019(Elyos)/51019(Asmodian)——客户端三行（槽位 %0/%3/%6）是“消灭布朗尼收集 3 个巧克力交给术古(202549)”->“在巧克力塔 701466/701467 上使用装饰”->“和术古对话领奖”；页链 check_user_item_ok 的按钮是 SETPRO2，select_success 的按钮是 SELECT_QUEST_REWARD；真端 authority 为 event.xml 的 monster_hunt(start_npc_ids=202549) + quest.xml 的 collect quest_50011a/51011a ×3，本检出无 Java handler；旧定义用 npc-item-report 直送 REWARD(0) 并伪造 SET_SUCCEED，塔物件未接 IR；本批改为显式 CHECK_USER_HAS_QUEST_ITEM 推进 0 -> 1、SETPRO2 就地关闭、塔 USE_OBJECT 推进 1 -> 2、补防呆入口与 0/1 两条自愈边，由 Batch35ValentineTowerRowLadderContractTest（8 例）锁定（报告 §三十九、证据 batch35-evidence.tsv、脚本 apply_batch35_valentine_tower_row_ladder.py）；批次 36（2026-09-22）：活动巧克力塔族 + 术古货箱族共 13 个任务——塔族 50020/80300/80301/80306/80307（select1/check_user_item_ok，NPC 202549/799763，塔 701466/831402/831403，收集物 182215173/182215288/182215289/182215294/182215295），货箱族 50021/51021/50022/51022/80302/80303/80308/80309（select4/select5，货箱 701470/701774，收集物 182215176/182215182/182215177/182215183/182215292/182215293/182215298/182215299）；旧定义里 50021/51021 的 reward var0=15（STATES_BEYOND_ROWS，50021 可见槽含 15、51021 为 0..15）、51022 reward var0=1 缺末行，其余直跳 REWARD(0)；统一重建为 unaccepted(0)/started(0)/s1(1)/reward(2)/complete(0) + 0/1 双自愈边、owner 收敛末行 NPC、货箱 can-act 落 s1 且 collecting_step 4->1；全库 MISSING_TAIL_ROWS 72->62 与 ROW_ALIGNED +13，由 Batch36EventRowLadderContractTest（5 例）与报告 §四十 锁定；批次 37（2026-09-22）：两组“交谈/情报→击杀→报告”任务——26905/26906/26908（start 204301/204301/204702，end 204372/204369/204817，kill 231555 231556/231558 231559/231570 231571：nodes started(0)/t1(1)/k2(2)/reward(2)，接取 NPC 只留 NPC_START，end NPC 负责 select2 SETPRO1、击杀、select5 SELECT_QUEST_REWARD 与 npc-complete，补 REWARD 0/1 自愈边）与 3711/4711（Mias/Henir 279045/279042 -> 术古 730196 -> 击杀 214823 -> Taranis/Votan 278501/278001：nodes started(0)/s1(1)/s2(2)/reward(3)，删 started->reward 直跳与末尾 SETPRO2 跳行，reward+QUEST_SELECT->DEFAULT_SUCCESS(10002)，补 0/1/2 自愈边）；由 Batch37TalkKillReportRowLadderContractTest（4 例）与报告 §四十一 锁定
- **批次 47 勘误（2026-09-22）**：`1123` 属客户端脚本驱动行（`ProgressAll` + `sensoryArea`），其 reward 投影权威值是 `REWARD/var0=0`；本节元数据里批次 15 记录的“1123 投影 0 -> 1”与 `REWARD/0 -> 1` 自愈边已被推翻，详见 `QE-056`。


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
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/4712.xml（owner 收敛 279042，删 798327/798330 的 npc-complete，保留三条 NPC_REPORT 入口）与 quests/2484.xml（owner 收敛 203331，删 204407/700267）；src/test/java/com/aionemu/gameserver/questEngine/definition/RewardOwnerTrimContractTest.java（7 例，Maven 48 例批次内全绿）；.agents/summary/quest-10527-reward-row/apply_batch16_reward_owner_trim.py（--check 幂等 2/2）、batch16-evidence.tsv、报告 §二十；legacy 证据：`git show '7e9f0316c^'` 中的迁移前 handler `_4712Escape_From_The_Dredgion` 与 `_2484OurManInElysea`（迁移已删除，仅存在于 git 历史）；批次 26 追加：quests/30600.xml 与 quests/30610.xml 的 owner 收敛到客户端任务书行点名的 NPC（30600 接取/报告 800325 Hejitor + 简报 800324 Linocus；30610 接取/报告 800327 Astella + 简报 800326 Aluna，静态 spawn 见 src/main/resources/aion/data/static_data/spawns/Npcs/210070000_Cygnea.xml 与 src/main/resources/aion/data/static_data/spawns/Npcs/220080000_Enshar.xml），删除只在 legacy 出现的 205842(Ancanus)/205864(Udvi)；CounterChainBriefingStageContractTest（6 例）、apply_batch26_briefing_and_named_ladders.py、batch26-evidence.tsv、报告 §三十
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
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/13918.xml（五槽 6n 位域 + 9 节点串行阶梯 + 旧步骤号自愈边）与 src/main/resources/aion/data/static_data/quest_definition/quests/23918.xml（删除 32 节点 counter-grid，改同形阶梯并补 kills 五条）; src/test/java/com/aionemu/gameserver/questEngine/definition/ChainEliteLadderContractTest.java（7 例：逐槽推进、乱序/回看无计划、owner 唯一、奖励格三格、旧存档自愈、23918 不得有步骤号迁移边）; .agents/summary/quest-10527-reward-row/apply_batch21_chain_elite_ladder.py（可重放，--check 幂等）; .agents/summary/quest-10527-reward-row/batch21-evidence.tsv; src/main/resources/aion/data/static_data/quest_definition/quests/18033.xml 与 quests/28033.xml（三槽阶梯 + owner 收敛 + 旧网格 0/1 自愈边）、quests/28313.xml（三槽阶梯 + 步骤号迁移边，保留 27 条按职业展开的 reward 分支）; src/test/java/com/aionemu/gameserver/questEngine/definition/CounterChainTripletContractTest.java（8 例：三槽 6n 对齐、逐槽推进、乱序/回看无计划、owner 收敛、旧存档自愈、职业奖励分支保真）; .agents/summary/quest-10527-reward-row/apply_batch25_counter_chain_triplets.py（可重放，--check 幂等）; .agents/summary/quest-10527-reward-row/batch25-evidence.tsv; .agents/summary/quest-10527-reward-row/2026-09-21-10527-reward-row-and-family-audit.zh-CN.md（§二十五、§二十九）; .agents/summary/quest-15001-multicounter-step/audit_section0_report_row_closure.py（COUNTER_CHAIN 口径，本族两侧 COUNTER_CHAIN_OK；行号口径 audit_reward_row_vs_client_steps.py 已登记 VAR0_FLAG_EXCEPTIONS）；客户端侧：Aion 5.8 解包 Dialogs 的 quest_q13918 / quest_q23918 的 quest_summary 6 行与 quest_monster 的 SECTION_0..4 链式门控（客户端文件外置，不入库）；批次 26 追加：src/main/resources/aion/data/static_data/quest_definition/quests/24112.xml（var0 + var5@30 单槽，reward 投影 var0=1;var5=0）与 src/main/resources/aion/data/static_data/quest_definition/quests/30600.xml、src/main/resources/aion/data/static_data/quest_definition/quests/30610.xml（var0/var1 + var5@30 双层，reward 投影 var0=1;var1=1;var5=0），三任务由 COUNTER_CHAIN_GAP → COUNTER_CHAIN_OK，COUNTER_CHAIN_GAP 归零；CounterChainBriefingStageContractTest（6 例）、apply_batch26_briefing_and_named_ladders.py、batch26-evidence.tsv、报告 §三十
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
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/18805.xml 与 src/main/resources/aion/data/static_data/quest_definition/quests/28805.xml（批次 27 修复：reward 投影 1 -> 2、删除 s1 -> reward 的 set-variable var0=1、补 REWARD/var0=1 的 enter-world 自愈边）；src/test/java/com/aionemu/gameserver/questEngine/definition/HousingRecycleRewardRowContractTest.java（5 例）；.agents/summary/quest-10527-reward-row/apply_batch27_housing_reward_row.py（--check 幂等）；.agents/summary/quest-10527-reward-row/batch27-evidence.tsv；.agents/summary/quest-10527-reward-row/2026-09-21-10527-reward-row-and-family-audit.zh-CN.md（§三十一）；口径例外（投影 = legacy 落盘 step，禁止按末行索引改）：src/main/resources/aion/data/static_data/quest_definition/quests/15300.xml 与 src/main/resources/aion/data/static_data/quest_definition/quests/25300.xml（changeQuestStep(env, 13, 14, true) -> step 停 13，2026-09-19/20 用户真机全程验收含领奖，提交 075464ebd 与 src/test/java/com/aionemu/gameserver/questEngine/definition/Quest15300And25300RewardProjectionTest.java 锁定）以及 src/main/resources/aion/data/static_data/quest_definition/quests/10100.xml 与 src/main/resources/aion/data/static_data/quest_definition/quests/20100.xml（useQuestItem(env, item, 4, 4, true) -> step=4，src/test/java/com/aionemu/gameserver/questEngine/definition/Quest10100And20100ItemUseRemovalTest.java 锁定道具消耗）；批次 28（2026-09-22）修复同族真缺陷 src/main/resources/aion/data/static_data/quest_definition/quests/16800.xml 与 src/main/resources/aion/data/static_data/quest_definition/quests/26800.xml（legacy `changeQuestStep(env, 2, 3, true)` 落盘 step=2，旧投影分别写成 1 / 3）：两侧重建 unaccepted(0)/started(0)/s1(1)/s2(2)/reward(2)/complete(0)，16800 补 LF_TOWER_SENSORY_AREA_Q16800_210110000 与 IDETERNITY_01_Q16800_301540000 两条 zone 推进并删掉 var1 影片旗标与 6 条直跳领奖捷径，26800 的 s2 -> reward 不再回写 var0=3，两侧各补 `REWARD/旧投影值 -> reward` 的 enter-world 自愈边；门禁 src/test/java/com/aionemu/gameserver/questEngine/definition/ArchivesRewardStepLadderContractTest.java（7 例），同步更新 src/test/java/com/aionemu/gameserver/questEngine/definition/Quest26800ClientDialogAlignmentTest.java（reward 由 3 改为 2）与 src/test/java/com/aionemu/gameserver/questEngine/definition/ClientQuestSectionAlignmentTest.java（16800 退役 var1 后移出 SECTION_LAYOUT_DEBT）；应用脚本 .agents/summary/quest-10527-reward-row/apply_batch28_archives_reward_row.py（--check 幂等）、证据 .agents/summary/quest-10527-reward-row/batch28-evidence.tsv 与报告 §三十二；行号审计脚本补登记 LEGACY_STEP_EXCEPTION={15300,25300,10100,20100}；批次 29（2026-09-22）修复 src/main/resources/aion/data/static_data/quest_definition/quests/1876.xml、src/main/resources/aion/data/static_data/quest_definition/quests/2876.xml、src/main/resources/aion/data/static_data/quest_definition/quests/14123.xml、src/main/resources/aion/data/static_data/quest_definition/quests/2600.xml、src/main/resources/aion/data/static_data/quest_definition/quests/11010.xml、src/main/resources/aion/data/static_data/quest_definition/quests/1466.xml（reward 投影分别 1->2、1->2、0->1、0->1、0->3、无->1，逐任务补 REWARD/旧值 -> 新值的 enter-world 自愈边），门禁 src/test/java/com/aionemu/gameserver/questEngine/definition/Batch29RewardRowClosureContractTest.java（4 例），同步更新 src/test/java/com/aionemu/gameserver/questEngine/definition/Quest1466ClientDialogAlignmentTest.java；全库分类明细 .agents/summary/quest-10527-reward-row/batch29-triage.tsv（81 行）、证据 .agents/summary/quest-10527-reward-row/batch29-evidence.tsv、报告 §三十三；应用脚本 .agents/summary/quest-10527-reward-row/apply_batch29_reward_row_closure.py（--check 幂等）；审计脚本补登记 LEGACY_STEP_EXCEPTION（64 项）、COUNTER_SLOT_EXCEPTIONS（2303/50008/51008/11467/1114/80690）、NO_LEGACY_HANDLER_OBSERVED（1005/1479/24120/24123/51010/51020/51022）
validation: static（xmllint + quest_definition.xsd 2/2 validates；apply 脚本 --check 幂等）+ audit（18805/28805 MISSING_LAST_ROW -> ALIGNED；全库 MISSING_LAST_ROW 84 -> 82、ROW_ALIGNED 2661 -> 2663、ROW_STATE_ALIGNED 2433 -> 2435、ROW_WITHOUT_STATE 516 -> 514）+ focused-test（批次 27：32 个测试类 204 例全绿，含 HousingRecycleRewardRowContractTest 5 例；批次 28：27 个测试类 146 例全绿，含新增 ArchivesRewardStepLadderContractTest 7 例；PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0，2026-09-22）+ audit（批次 28：16800 由 MISSING_LAST_ROW、26800 由 STATES_BEYOND_ROWS 双双转为 ALIGNED / ROW_ALIGNED / ROW_STATE_ALIGNED / recovery=True、visible_state_var0=0 1 2；全库 MISSING_LAST_ROW 82 -> 81、ROW_ALIGNED 2663 -> 2665、ROW_STATE_ALIGNED 2435 -> 2437、ROW_WITHOUT_STATE 514 -> 513、STATES_BEYOND_ROWS 2623 -> 2622、ROW_AHEAD 2589 -> 2588、ROW_BEHIND 183 -> 182；section0 residual 837 不变）；批次 29：static（xmllint 6/6 validates；apply 脚本 --check 幂等 6/6）+ audit（MISSING_LAST_ROW 81 -> 77，1466/1876/2876/11010 转 ALIGNED，14123/2600 按 QE-054 保留 legacy 落盘 1 并转入登记；剩余 77 个 100% 落在三组登记集合内；ROW_ALIGNED 2665 -> 2669、ROW_BEHIND 182 -> 179、NO_REWARD_ROW 179 -> 178）+ focused-test（65 个 reward/row/ladder/journal/counter 测试类 299 例，与本次改动相关的全绿，含新增 Batch29RewardRowClosureContractTest 4 例；PRODUCTION_COMPILE_OK=6189 / FAILURES=0 / INTERACTION_OBJECT_FAILURES=0 / WHITELIST_VIOLATIONS=0，2026-09-22）；客户端实机 PENDING_CLIENT
boundaries: 本卡只约束 reward 投影的取值来源，不改变 QE-051 的“行号口径用于发现缺行”作用；无 legacy handler（9 个）或 handler 走 defaultCloseDialog/setStatus 的组合形态（61 个）必须先逐族解出落盘 step 再分类，禁止用启发式批量改数据；15300/25300 的 REWARD/var0=14 恢复边与 10100/20100 的道具消耗属于已验收行为，不得随投影“归一化”一起重构；批次 28 补充：同族两侧旧投影值可能不同（16800=1、26800=3），自愈边只能按侧写各自的旧值；行阶梯必须由 legacy 事件（zone/dialog）驱动，禁止保留 started -> reward 的捷径路线；批次 29 补充：`useQuestItem` 在旧引擎里落盘的也是 old（不是 new），取 newStep 的唯一依据是那个任务自己的客户端 quest_summary 契约与用户报障，不能推广；族 B 的“末行不亮”是 legacy 行为，未复测前不要重开；族 C 的 var0 是计数器/分支标记（2303 的 11..15/21..25、50008/51008 的 sensoryArea 计数、11467 的四投影、1114 的分支领奖行、80690 的槽位 15）；族 D（1005/1479/24120/24123/51010/51020/51022）缺 legacy 依据，需要客户端/数据侧取证后才能判定
superseded_by: none
see_also: [QE-051], [QE-052], [QE-046]
first_check: 审计报 MISSING_LAST_ROW 时，先查该任务是否已有真机验收记录或门禁锁定（Quest15300And25300RewardProjectionTest、Quest10100And20100ItemUseRemovalTest），再从迁移前 handler 提取 reward 落盘 step（changeQuestStep(..., old, new, true) 与 useQuestItem(..., old, new, true) 都取 oldStep；setQuestVar(N)/changeQuestStep(..., false) 之后的 setStatus 取 N；defaultOnKillEvent(..., var, true) 取匹配值 var）；落盘值与 XML 投影一致时登记 LEGACY_STEP_EXCEPTION，不要按末行索引改
keywords: 领奖行、MISSING_LAST_ROW、legacy step、changeQuestStep、useQuestItem、SM_QUEST_ACTION、reward 投影、15300、25300、10100、20100、18805、28805、16800、26800、1876、2876、14123、2600、11010、1466、2303、50008、51008、11467、1114、80690、1005、1479、24120、24123、51010、51020、51022、领奖阶阶梯、Archives、LEGACY_REWARD_STEP_IS_AUTHORITATIVE
-->

- **判定规则**：客户端任务书的当前行由 `SM_QUEST_ACTION` 的 step 决定，而 step 就是 reward/STEP 节点投影打包后的 var0。因此 `reward` 投影的正确值是 **legacy 进入 REWARD 时真正落盘的 step**。旧引擎只有一条写入通道 `changeQuestStep(env, step, nextStep, reward, varNum)`：reward=true 时它只 `setStatus(REWARD)`、**不写 nextStep**，所以 `changeQuestStep(env, old, new, true)` 落盘 old；`useQuestItem(env, item, old, new, true)` / `defaultCloseDialog` / `checkQuestItems` / `checkItemExistence` / `useQuestObject` / `defaultFollowEndEvent` 最终都委托给同一个方法，落盘的也是 **old**（批次 29 逐行核对迁移前 `QuestHandler` 确认）。只有 `setQuestVar(N)` / `setQuestVarById(0, N)` / `changeQuestStep(..., false)` 之后的 `setStatus(REWARD)` 才写 N。行号口径（QE-051）算出的“末行索引”只是待验证的候选，不是权威。
- **为什么容易漏**：`MISSING_LAST_ROW` 的形状（可见状态 0..N-1、客户端 N+1 行）在两种成因下完全一样：真缺陷（legacy step 是 N，XML 写成 N-1）与口径例外（legacy step 本来就是 N-1）。只看审计输出会想当然地按末行索引批量加一；而 15300/25300 这种族已经真机验收过（reward=13 正确），10100/20100 又由道具消耗门禁锁住，批量替换会同时破坏两条已验证路径。
- **代表案例（批次 29，全库 81 个 MISSING_LAST_ROW 的四族分类）**：族 A 只有 6 个真缺陷——1876/2876（`changeQuestStep(env, 1, 2, false)` 写 2 后 `setStatus(REWARD)`，旧投影写成 1）、14123（`defaultOnKillEvent(env, 206360, 0, 1)` 写 1，旧 XML 的 4 条交接又回写 `var0=0`）、2600（legacy 只在 `var0 == 1` 时响应 `SELECT_REWARD`，旧投影 0）、11010（`defaultCloseDialog(env, 2, 3)` 写 3，旧投影 0）、1466（两条进入路径分别落盘 0 与越界的 2，客户端只有 2 行，统一为 1），本批按 legacy 落盘值修投影并各补 `REWARD/旧值 -> 新值` 自愈边；族 B 62 个（`defaultFollowEndEvent(1,1,true,12)`、`defaultCloseDialog(1,1,true,false)`、`checkQuestItems(1,1,true,...)`、`changeQuestStep(13,14,true)`、`checkItemExistence(11,11,true,...)` 等形态）投影已经等于 legacy 落盘值，只是小于末行索引，全部登记为 `LEGACY_STEP_EXCEPTION`；族 C 6 个（2303 的 11..15/21..25 击杀计数、50008/51008 的 sensoryArea 计数、11467 的 reward0..3 四投影、1114 的分支领奖行、80690 的槽位 15）var0 不承载行号；族 D 7 个（1005/1479/24120/24123/51010/51020/51022）在 legacy 侧既无 handler 也无脚本，登记待取证。反例提醒：`useQuestItem` 并不写 newStep——10527 的 reward=15 来自它自己的客户端 16 行契约与用户报障，不是通用规则。；批次 31（2026-09-22）：Gelkmaros 三行交接族 21217/21244/21249 的 legacy 落盘 step=2（`setQuestVar(2)` + `defaultCloseDialog(2, 2, true, false)` 的 reward 分支只置状态、不写 nextStep），reward 投影 0 -> 2 并按 QE-051 补 `REWARD/var0=0 -> 2` 自愈边；同批把 MISSING_TAIL_ROWS 桶按“legacy 事件链 vs 客户端行数 vs XML 阶梯”全量 triage（残留 73 个待逐族收口）；批次 32（2026-09-22）：卡多尔迎新族 13800/23800 的 legacy 落盘 step 同为 1（传送点 `STEP_TO_1` 写 1 + 迎宾 NPC `setStatus(REWARD)` 不写 nextStep），但客户端 quest_summary 有 3 行、页链还有 SETPRO2(10001) 一段；本批按 QE-051 把 reward 投影定为 2 并为 0/1 两个旧值各补一条自愈边——**legacy 落盘 step 只解释旧存档为什么会停在 1，不等于行阶梯的上限**，当客户端页链还有推进段时以页链为准；批次 33（2026-09-22）：25094 的 `checkQuestItems(env, 0, 1, true, ...)` 说明旧 helper 丢掉的不只是 nextStep 的**写入**，调用点里 nextStep 参数本身常常就是作者意图（这里 =1，与客户端共享槽位状态一致）；因此“legacy 落盘 step”只用于给旧存档写自愈边，投影取值仍按客户端槽位/页链核定；批次 34（2026-09-22）：无迁移前 handler 的任务（30504/30554）没有 legacy 落盘值可依，本卡口径不适用——其行阶梯只能由客户端槽位/页链 + retail step 取证，自愈边按旧定义实际写过的值（0 与 1）各补一条
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
