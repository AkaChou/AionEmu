# 任务 19638 重启后无法接取根因排查与修复报告

### 1. 现象与证据链 (Evidence Chain)

玩家 `Ww`（ID: 153807）在完成 19637 并提交奖励后，尝试找凯西内尔（NPC 798926）接取后续任务 19638（天族特别任务 2，Trouble with Twos），出现点击“接受任务”（动作 20000）后服务端未下发 `SM_QUEST_ACTION 任务=19638 状态=3 步数=0`，而是直接关闭对话窗口 `SM_DIALOG_WINDOW 下发页=0` 的异常。

- **实机抓包证据**：
  ```text
  09-16 22:08:21 CM_DIALOG_SELECT 玩家=Ww npcId=798926 questId=19638 上一页=10 动作=31
  09-16 22:08:21 SM_DIALOG_WINDOW 玩家=Ww targetObj=75788 questId=19638 下发页=4762
  09-16 22:08:22 CM_DIALOG_SELECT 玩家=Ww npcId=798926 questId=19638 上一页=4762 动作=20000
  09-16 22:08:22 SM_DIALOG_WINDOW 玩家=Ww targetObj=0 questId=0 下发页=0
  ```
- **数据库状态实测**：
  - `SELECT * FROM player_quests WHERE player_id = 153807 AND quest_id IN (19637, 19638);`
  - 19637: `status=COMPLETE, quest_vars=0, complete_count=1, reward=0`（已圆满完成）
  - 19638: 无记录（处于未接取 `NONE` 状态）
  - 玩家进行中任务仅 17 个，远低于上限 40。

---

### 2. 根本原因剖析 (Root Cause)

1. **元数据契约差异**：
   - 19637 无前置条件；
   - 19638 配置了前置任务完成约束：
     ```xml
     <start-conditions>
       <condition type="finished" quest-id="19637"/>
     </start-conditions>
     ```
2. **性能优化引入的推导盲区**：
   - Commit `cbfdb8f9d` (`perf(quest): capture only the facts a transition reads`) 引入了 `QuestFactRequirements` 机制，设计目标是“根据转换静态推导只采集必要事实族”。
   - 但此前 `QuestFactRequirements.of(definition.id(), event, transition)` **只检查了 transition 显式声明的 conditions 和 actions**，完全未感知任务元数据中的 `<start-conditions>` 与 `<prerequisites>`。
   - `NPC_START` 展开的接取转换（`unaccepted -> started`, action=20000）上的条件只有 `<start-eligible/>`，没有显式声明 `<quests-finished/>`。
   - 导致 `QuestFactRequirements` 将 `questIdSets`（已完成/进行中任务集合）推导为 `false`，`PlayerQuestEventPort.snapshot` 跳过了对玩家已完成任务的采集（`completedQuestsCaptured = false`）。
3. **Fail-Closed 阻断生效**：
   - 当请求进入 `QuestMutationPlanner.plan()` 时，第 69 行执行了 `metadataPrerequisitesSatisfied(definition, snapshot, transition)`。
   - 该方法判定当前是从 `NONE` 状态进入非 `NONE` 状态（接取任务），因此读取 19638 元数据并动态构造 `QuestCondition.QuestsFinished(Set.of(19637))`。
   - `QuestConditionEvaluator.questsFinished` 判定 `!snapshot.completedQuestsCaptured()`，按照防御性安全设计直接返回 `false`。
   - 整个接取计划被判定为 `NO_MATCH`，任务引擎放弃接管，回退至 `DialogService` 关闭对话框。

---

### 3. 代码级根本解决 (Code Solution)

扩展 `QuestFactRequirements`，重载支持 `CompiledQuestDefinition`：
- 当一个转换是从 `NONE` 状态进入非 `NONE` 状态时（接取任务）：
  - 若 `metadata.prerequisites()` 非空，自动标记 `questIdSets = true`；
  - 若 `metadata.startConditionGroups()` 包含 `finished`/`unfinished`/`acquired`/`noacquired`，自动标记 `questIdSets = true`；
  - 若包含 `equipped`，自动标记 `equipment = true`。
- 在 `QuestExecutionCoordinator` 事实采集入口传入 `definition`。
- 对于无前置条件的任务（如 19637）或非接取转换（如杀怪/中途对话），依然保持零多余采集，继续享受 JFR 性能优化的红利。
