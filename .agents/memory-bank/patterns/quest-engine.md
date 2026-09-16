# Quest Engine Patterns & Pitfalls (任务系统模式与避坑)

本文档记录 AionEmu 声明式 XML 任务系统、状态机与 NPC 交互的实战避坑经验。

> Pattern IDs: `QE-001`–`QE-018`
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
status: PROVISIONAL
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
boundaries: 仅适用于客户端当前页按钮存在后续页或明确关闭/状态推进合同；纯过场无 NPC 对话入口的任务按系统页或自动完成处理，不强制补对话页。契约基于 tracked 客户端 CSV，未有 CSV 证据的页面不在护栏覆盖内；编译期加载依赖运行目录 definitions/quest_dialog/*.tsv（package.sh/start-silent.sh 会同步），缺失即 fail-closed 而不会静默放行。movie-page-turn 积木只覆盖「同名页面或显式 page」形态，额外条件/动作或不同副作用仍需显式 transition；运行侧断路器是启发式（同一选择连续 4 次、相邻间隔 0.8–5 秒，人类连点被排除），只关闭窗口并跳过本次路由，不改变任务状态，真实客户端未复现该路径
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
validation: XML XSD 与 IDE/diff 静态检查通过；2026-09-16 用户回复「出现下一步了」确认最终计数事件后的下一步/REWARD 流转；专项 Maven 与生产 catalog/白名单门禁未运行，奖励领取/COMPLETE 未单独复验
boundaries: 适用于多个实时计数字段共享 START 状态、最终事件同时完成计数与状态迁移的任务；若根因是 START 节点固定投影了实时字段，复用 QE-002 与 COUNTER_SOURCE_PROJECTION_NO_LOCK；位掩码多地点侦察复用 MULTI_LOCATION_SCOUTING_FINAL_REWARD_TRANSITION
superseded_by: none
first_check: 每个计数字段的 continuing/completing priority、最终字段条件、target status、事务动作与完整 after-commit；旧 handler 是否在全部计数满足后立即进入 REWARD
-->

- **判定规则**：多段计数的每个字段都需要成对的 continuing 与 completing 路线。只在未完成时自环、到上限后返回空计划，会让所有计数满但状态仍停在 START；最后一个事件必须以 priority 0 直接进入 `REWARD`，不能等下一次交互或只依赖无门禁的报告路线。
- **代表案例**：Playbook 案例 8.37（`7f824dc78`），11468/21468 的三个 UseSkill 计数分别为 `var1=10`、`var2=5`、`var3=3`，最终技能使用后目标 `reward`；`Quest11468And21468SkillCompletionTest#finalItemSkillEntersRewardAndPersistedFullCountersCanReport` 锁定三条 completing 路线和满计数恢复路线。
