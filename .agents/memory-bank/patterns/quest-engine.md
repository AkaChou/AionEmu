# Quest Engine Patterns & Pitfalls (任务系统模式与避坑)

本文档记录 AionEmu 声明式 XML 任务系统、状态机与 NPC 交互的实战避坑经验。

> Pattern IDs: `QE-001`–`QE-023`
> card_status: ACTIVE; existing entries retain their historical evidence boundary
> scope: production quest XML/compiler, Quest runtime, and Aion 5.8 client/legacy evidence
> last_reviewed: 2026-09-16

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
last_verified: 2026-09-14
symptom: dialog 31 无路由、中间 NPC 丢失、NPC ID 错配、choice/fallback 冲突
root_cause: XML migration omitted intermediate NPC report nodes or copied NPC identities incorrectly
fix_or_guardrail: Restore START variable nodes, NPC_REPORT transitions and explicit choice/fallback handling
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/1900.xml; docs/quest/client-dialog-mapping/quest-dialog-action-details.csv; NPC_REPORT repair cases in this card
validation: static; production-gate; client-contract evidence required per quest
boundaries: METADATA_ONLY tasks cannot receive executable nodes until their catalog mode is changed
superseded_by: none
see_also: docs/quest/repair-playbook/PATTERNS.zh-CN.md (MULTI_NPC_HANDOFF_REWARD_OWNER, ORDERED_MULTI_NPC_REPORT_FLOW)
first_check: quest XML nodes, NPC_REPORT edges, catalog mode and client dialog mapping
-->
   - **现象**：从 Java Handler 迁移到 XML 时，多 NPC + 多 var 档流程在 XML 中只剩起始 NPC，中间交付 NPC 丢失或 ID 错配（如 804871↔804870 孪生 ID 抄错）。玩家点击 NPC 发送 dialog 31 无匹配转换。
   - **标准修复模板 (参考 `quests/1900.xml`)**：
     - 中间 NPC 各为一个 START 状态 var 档节点（`s1`/`s2`...）；
     - dialog 31 自环显示历史页（1352/1693/2034/2375 等）；
     - 推进 dialog（10000/10001...）跨节点推进 var（目标节点声明 var 自动写入）；
     - 物品交换使用 `give-item` / `remove-item`；
     - `npc-complete` 的 choice/fallback 处理 `SELECTABLE_ITEM`（choice 占 8/9 时其余用 fallback dialog-ids="10..23"，避免 `DUPLICATE_DIALOG_ID`）。
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
last_verified: 2026-09-15
symptom: 使用任务物品或技能后服务端进入 START，但客户端任务说明为空、只剩奖励或计数步骤不显示；任务推进后客户端任务说明仍停留在上一行、不跟随服务端阶段
root_cause: 多段计数被紧凑放在 offset 0/4/8，而客户端脚本和旧 QuestVars 按 SECTION_n = 6*n 读取；或把任务说明行索引（阶段）从 SECTION_0 交换到 SECTION_1，使客户端行索引读到计数槽而停在固定行
fix_or_guardrail: 当 Quest.pak 的 quest_script/HTML summary 引用 SECTION_N 时，varN 必须放在 offset=6*N 的 6-bit 位段，并用客户端脚本或旧 setQuestVarById(N) 对齐；阶段/任务说明行索引必须留在 SECTION_0，计数只能放 SECTION_1+，不得为了隔离计数而交换两者
evidence: .agents/summary/quest-11468-taloc-item-sections/2026-09-14-client-section-mismatch.zh-CN.md; .agents/summary/quest-10032/2026-09-15-section0-stage-correction.zh-CN.md; src/test/java/com/aionemu/gameserver/questEngine/definition/ClientQuestSectionAlignmentTest.java; src/test/java/com/aionemu/gameserver/questEngine/runtime/Quest10032ItemPlayClientCounterProductionFlowTest.java; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestPacketOrderRegressionTest.java
validation: focused regression ClientQuestSectionAlignmentTest, Quest10032ItemPlayClientCounterProductionFlowTest and QuestPacketOrderRegressionTest passed; production catalog 6193 definitions compiled with 0 failures and 0 whitelist violations; corrected 10032 layout still needs real-client re-verification
boundaries: 有客户端证据证明的单字段紧凑布局可以保留；只有客户端脚本或旧 handler 明确寻址独立 SECTION 时才应用该规则
superseded_by: none
first_check: Quest.pak quest_script_monster.csv 的 SECTION_N、旧 handler setQuestVarById(N)、XML offset/width、任务说明行索引是否仍读取 SECTION_0
-->

- **判定规则**：5.8 客户端的 `SECTION_0..3` 分别对应 `quest_vars` 的 `0..5`、`6..11`、`12..17`、`18..23` 位段。任务 XML 中的 `varN` 若参与客户端摘要或脚本条件，必须与 `SECTION_N` 对齐，不能仅因为当前最大值较小就紧凑改到 `var(N-1)` 的位段。
- **代表案例**：
  1. 11468/21468 需要 `SECTION_1<10`、`SECTION_2<5`、`SECTION_3<3`，且进行中要求 `SECTION_0==0`。旧 XML 把三个字段放在 `0/4/8`，第一次使用物品就把 `SECTION_0` 置 1，客户端摘要整体隐藏；修复为 `6/12/18` 后恢复计数段。
  2. 10032/20032：真机在服务端已到 s1（交换布局 wire=64，var1=1）时，客户端任务说明仍显示第 0 行；`//quest set 10032 START 65`（SECTION_0=1）后说明行立即前进，证明行索引读 SECTION_0。修正为 `var0=阶段(0..8, offset 0)`、`var1=眼泪次数(0..20, offset 6)`，掉落门禁恢复阶段 6；`Quest10032ItemPlayClientCounterProductionFlowTest` 锁定新合同，既有 `QuestPacketOrderRegressionTest` 的 `var0=7` 断言同时恢复通过。

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
last_verified: 2026-09-15
symptom: 任务引擎启动崩溃、Can't initialize typed quest engine、AMBIGUOUS_TRANSITION: same event has overlapping transitions without unique priorities: TALK_TO_NPC、DUPLICATE_NODE_PROJECTION
root_cause: 同阶段同 NPC 动作被多次注册（例如修复直达奖励时对齐 SETPRO 却未清理历史自循环），或多阶段任务引入 counter 积木时因 source 节点不得固定计数字段导致省略声明退化为 START:0 碰撞
fix_or_guardrail: 对齐客户端动作时彻底清理原同动作自循环边；多阶段且含 counter 的任务，progress 必须分离阶段位段（如 var0）与计数字段（如 var1，参考 4944 潘利尔规范），source 节点固定阶段 var0，counter 绑定 var1
evidence: src/main/resources/aion/data/static_data/quest_definition/quests/1722.xml; src/main/resources/aion/data/static_data/quest_definition/quests/3940.xml; src/main/resources/aion/data/static_data/quest_definition/quests/4944.xml; .agents/summary/quest-engine-startup-debug/README.md; src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDefinitionCompiler.java; src/main/java/com/aionemu/gameserver/questEngine/definition/QuestXmlBlockExpander.java
validation: static; production catalog 6193 executable definitions compile with 0 failures; QuestDefinitionCatalogManifest.compile() loaded 6231 entries successfully; git diff --check clean
boundaries: 同一事件在不同源节点、或有互斥条件（如 class/has-item）、或声明了唯一优先级的属于合法分支；仅适用于同一可达源节点下动作、条件、优先级完全相同的重叠，以及 counter source 节点的字段分配
superseded_by: none
first_check: 冲突任务 XML 的 transitions 中同 NPC/同 action 的边、nodes 列表中的投影 (status + var)、counter 的 field 与 source/target 节点定义
-->

- **判定规则**：
  1. `QuestDefinitionCompiler` 在编译期对所有转换建立冲突索引：对于同一 NPC 的相同 `TALK_TO_NPC` 动作，如果两者可能从同一节点触发、条件非互斥且均未声明唯一 `priority`，即判定为无歧义解析保证（`AMBIGUOUS_TRANSITION`）并拒绝启动。
  2. `<counter>` 领域积木强制要求其 `source` 节点不得固定计数字段（`COUNTER_SOURCE_PROJECTION_CONFLICT`），因为击杀计数递增过程中该字段值动态变化。若任务拥有多个 `START` 阶段，不可直接省略变量声明（缺省会退化为 `START:0` 与初始 `started` 节点重叠触发 `DUPLICATE_NODE_PROJECTION`）。
- **代表案例**：
  1. `1722.xml`（拉斯汀的秘密指令）：提交 `94636797a` 将 `s2` 推进到 `s3` 的动作从 `SELECT_QUEST_REWARD` 纠正为 `SETPRO3` 时，漏删了文件下方历史遗留的 `s2 -> s2 SETPRO3` 自循环边，导致两边重叠报错。删除冗余自循环边后闭环。
  2. `3940.xml`（米拉詹特武器忠诚任务）：提交 `0823653a7` 尝试单字段承载阶段与 300 击杀（6..306），因 `<counter>` 约束移除了 `hunt` 节点的变量声明，导致其退化为 `START:0` 与 `started` 发生投影重合。对齐魔族同型任务 `4944.xml`（潘利尔武器任务）标准设计：分离阶段字段 `var0`（6-bit）与计数字段 `var1`（9-bit，0..300），`hunt` 固定 `var0=6`，`hunt-done` 固定 `var0=6, var1=300`，彻底消除节点投影碰撞。

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
evidence: commit c34458083; src/main/resources/aion/data/static_data/quest_definition/quests/15001.xml; src/main/resources/aion/data/static_data/quest_definition/quests/15203.xml; src/test/java/com/aionemu/gameserver/questEngine/definition/QuestMonsterProgressContractAuditTest.java; .agents/summary/quest-15001-multicounter-step/2026-09-19-15001-double-counter-step-closure.zh-CN.md; .agents/summary/quest-15001-multicounter-step/2026-09-19-section0-report-row-closure-audit.zh-CN.md; .agents/summary/quest-acceptance/15001-2026-09-19-section0-report-row-client-accepted.md
validation: QuestMonsterProgressContractAuditTest（含 runtime planner 断言 20801）、ClientQuestSectionAlignmentTest、ProductionCatalogWhitelistVerificationTest、QuestDefinitionCatalogManifestTest 通过（PRODUCTION_COMPILE_OK=6189、FAILURES=0、WHITELIST_VIOLATIONS=0）；2026-09-19 用户确认 15001 客户端验证完成；同型 sweep 已修复 246 个任务（244 批量 + 18994/28994）并新增 QuestSection0ReportRowContractTest + quest-section0-report-row-contract.tsv（246 行合同快照），该测试的 Maven 门禁已于 2026-09-19 授权执行并通过（PRODUCTION_COMPILE_OK=6189、FAILURES=0、WHITELIST_VIOLATIONS=0，5 个测试类 26 用例）；残余 15101/24153 需额外路线重建
boundaries: 行索引与计数字段是两个独立合同字段，只断言 status=REWARD 或计数饱和不算闭环；同型批量修复只对结构同型任务机械套用（reward 投影 + 自环钉行 + 终击写报告行 + ENTER_WORLD 迁移），多阶段/自定义节点/无击杀路线的任务必须逐个判定；若缺的是计数自环/字段错位复用 QE-012 与 COUNTER_SOURCE_PROJECTION_NO_LOCK，若缺的是进入 REWARD 的路线本身复用 QE-018；迁移修复路线只在 ENTER_WORLD 触发，在线且不重登/不切图的旧存档不自动纠正
superseded_by: none
see_also: [QE-012], [QE-018], .agents/summary/quest-15001-multicounter-step/2026-09-19-section0-report-row-closure-audit.zh-CN.md
first_check: 用 quest_monster.csv 找同一 SECTION_0==S 上并行门控多个 SECTION_n<N 的任务，再核对 reward 投影 var0、终击 actions 与 enter-world 迁移路线；旧 handler 是否在完成分支写 setQuestVarById(0, 报告行)
-->

- **判定规则**：击杀任务的“杀满即报告”由两个字段共同完成——计数（`SECTION_1+`）与任务说明行索引（`SECTION_0`）。最后一条击杀路线必须把行索引写成报告行，并且 `reward` 节点投影要与之一致；否则服务端进入 `REWARD` 而客户端任务说明停在击杀行，出现空分子与“下一步不出现”的玩家可见症状。
- **代表案例**：15001（绿雾湿地双计数）修前追踪 `状态=4 步数=20800`（`SECTION_0=0, SECTION_1=5, SECTION_2=5`），修后 `20801`；同批 15020/15073/15100/15104/15203/15406/15407/15408/15580/15671/25671/25060/18952 同型；代表测试 `QuestMonsterProgressContractAuditTest#stepZeroMultiCounterHuntsAdvanceSectionZeroToTheReportStep`。
- **同型存量**：2026-09-19 审计命中 248 个可执行任务（旧 handler 在完成分支写 var0/报告行索引、当前 XML 未推进），其中 246 个已按同一合同修复；15101 缺 0->1 对话推进行、24153 缺击杀路线，需要额外证据；另有 24 行旧 handler 证据需支持 `setQuestVar(1)` 与 `setQuestVarById(0, var+1)` 形态的二次分类。

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
