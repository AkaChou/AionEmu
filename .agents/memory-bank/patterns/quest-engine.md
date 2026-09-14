# Quest Engine Patterns & Pitfalls (任务系统模式与避坑)

本文档记录 AionEmu 声明式 XML 任务系统、状态机与 NPC 交互的实战避坑经验。

> Pattern IDs: `QE-001`–`QE-005`
> card_status: ACTIVE; existing entries retain their historical evidence boundary
> scope: production quest XML/compiler, Quest runtime, and Aion 5.8 client/legacy evidence
> last_reviewed: 2026-09-14

---

## 一、权威基准与对比原则 (`QE-001`)
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

## 二、核心状态机与变异规划器 (QuestMutationPlanner) (`QE-002`–`QE-003`)

1. **Target 投影覆盖 Action 变量陷阱 (2026-08-12 修复)**：
   - **现象**：同节点自环计数任务（如 18972.xml 的 `started→started` 计数、击杀怪、收集道具）在玩家交互后，计数卡在 0，`variable-at-least` 完成转换永不触发。
   - **根因**：`QuestMutationPlanner.build()` 原用 `variables.putAll(projection.variables())`，Target 节点声明的 var 会无条件覆盖 Transition Actions 里的 `SetVariable` / `IncrementVariable`。
   - **引擎机制**：现已改为记录 `actionTouchedFields`，Target 投影仅覆盖 Action 未触及的字段；同节点自环计数 Action 结果被完整保留。

2. **完成路径残留 Work-Item 自动清理 (2026-08-14 修复)**：
   - **现象**：任务完成后玩家背包仍残留任务道具（如 1122 任务完成箱子仍残留 182200216「Pernos's Robe」）。
   - **根因**：旧引擎 `QuestService.setFinishingState` 在完成时无条件清理所有 `questWorkItems`；新引擎此前只在放弃（Abandon）时清理，完成（`CompleteQuest`）路径漏了清理。
   - **引擎机制**：`QuestMutationPlanner.build()` 现已对称增加 `appendCompletionWorkItemCleanup`，凡声明了 `<work-items>` 且未显式配 `remove-item count="ALL"` 的任务，完成时由引擎自动补齐清理。

---

## 三、NPC 报告链与 Var 节点守则 (`QE-004`–`QE-005`)

1. **多 NPC 交付链系统性丢失修复**：
   - **现象**：从 Java Handler 迁移到 XML 时，多 NPC + 多 var 档流程在 XML 中只剩起始 NPC，中间交付 NPC 丢失或 ID 错配（如 804871↔804870 孪生 ID 抄错）。玩家点击 NPC 发送 dialog 31 无匹配转换。
   - **标准修复模板 (参考 `quests/1900.xml`)**：
     - 中间 NPC 各为一个 START 状态 var 档节点（`s1`/`s2`...）；
     - dialog 31 自环显示历史页（1352/1693/2034/2375 等）；
     - 推进 dialog（10000/10001...）跨节点推进 var（目标节点声明 var 自动写入）；
     - 物品交换使用 `give-item` / `remove-item`；
     - `npc-complete` 的 choice/fallback 处理 `SELECTABLE_ITEM`（choice 占 8/9 时其余用 fallback dialog-ids="10..23"，避免 `DUPLICATE_DIALOG_ID`）。
     - 注意：目录清单 `mode="METADATA_ONLY"` 的任务不能有 nodes/transitions，恢复执行定义后必须在清单中改为 `EXECUTABLE`。

2. **连续两步汇报误删中间 Var 节点 (7 个典型案例)**：
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

## 四、NPC 对话门控与任务路由冲突 (NPC_DIALOG_ROUTE_GATE_COLLISION)

- **现象**：玩家达到领奖状态（`REWARD`）后，与 NPC 对话无法打开奖励选择窗口（Page 5），直接关闭对话或无响应（如 q1006「Ascension / 重生为守护者」）。
- **根因**：
  同一 NPC（如 Pernos 790001）身上挂有多个任务。当 NPC 使用较宽的 `TalkToNpc(npcId)` 索引分发时，其他处于未授权状态的普通任务（如 q1123）与当前处于 `REWARD` 状态的主线使命任务（MISSION）发生门控判断冲突，导致错误命中未授权分支。
- **修复与排查模式**：
  在 `QuestEngine` / `QuestProductionDispatcher` 中，事件路由必须按实际 `QuestEvent.matches` 精确过滤，确保各状态（尤其是 REWARD）的专属 Dispatcher 具有正确的优先级，同时保留普通任务列表的授权门禁保护（代表提交 `59bba1a`）。

---

## 五、可选/分支收集物提前扣除致使领奖卡死 (OPTIONAL_WORK_ITEM_CLEANUP_BLOCK)

- **现象**：任务可多选一交付物品（如 q2392「美丽的羽毛」三选一、q1922、q2947），玩家选定分支进入奖励状态（`REWARD`）后，点击 NPC 领奖或预览奖励窗口时完全卡死无响应。
- **根因**：
  1. 任务在 `started -> r1/r2/r3`（分支推进）时，已执行 `<remove-item count="1"/>` 扣除了玩家选定的该分支物品（背包内该物品余额归 0，且另两种物品从未收集）。
  2. 随后的 `rN -> complete` 以及 `rN -> rN`（自环预览）路由，错误地声明了对这全部三件物品的**严格正数扣除**（`count="1"`）。
  3. `QuestMutationPlanner.removalFeasible` 判定 `remaining >= remove.count()` 为 `false`，导致完成路由与预览路由全部被判定为 `BLOCKED`！
- **修复规范**：
  - 进入奖励阶段或自环预览的清理动作，针对这种多选一/分支已提前扣除的物品，**必须使用 `count="ALL"`**。
  - `removalFeasible` 对 `removeAll()` 会短路返回 `true`，背包未持有也不会阻断领奖。

---

## 六、不可达/999级任务前置阻断接取 (UNREACHABLE_METADATA_PREREQUISITE)

- **现象**：玩家达到等级要求，但在 NPC 处完全看不到可接任务标记或点击无接取对话（如 q19055）。
- **根因**：
  任务 metadata 中的 `<prerequisites>` 或 `<condition type="finished">` 引用了已废弃、未引入当前版本或 999 级的虚拟任务（如 `19054` 在当前生产 catalog 和 `quest_data.xml` 中均不存在）。
- **排查与修复规范**：
  - 检查 metadata 中的前置任务 ID 是否存在于生产 catalog。
  - 经核实目标为缺失定义或 999 级不可达任务时，安全清理该前置引用，恢复 NPC 的正常接取（`unaccepted -> started`）路由。
