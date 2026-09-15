# 任务引擎启动编译冲突修复取证 (Quest Engine Startup Compilation Collision Fix)

- 状态: 修复完成，独立 Java Catalog 编译全量 6193 条 EXECUTABLE 任务通过（0 failures）
- 日期: 2026-09-15
- 关联提交:
  - `94636797a` (fix(quest): guard SETPRO reward routes globally)
  - `0823653a7` (fix(quest): align Taloc and Miragent progress flows)

## 1. 根因分析 (Root Cause)

### 故障 1：任务 1722 同事件重叠且无唯一优先级 (AMBIGUOUS_TRANSITION: TALK_TO_NPC)
- **触发点**: `src/main/resources/aion/data/static_data/quest_definition/quests/1722.xml`
- **直接原因**:
  - NPC 278544 在 `s2` 阶段存在两条无条件、无优先级的同动作（`SETPRO3`）转换：
    - 行 102：`<transition source="s2" target="s3">`，推进到 `s3` 阶段并关闭对话。
    - 行 288：`<transition source="s2" target="s2">`，历史遗留的自循环关闭对话边。
  - 提交 `94636797a` 将行 102 的动作由 `SELECT_QUEST_REWARD` 对齐为客户端真实的 `SETPRO3` 时，未删除行 288 的同名冗余自循环边，导致编译器抛出 `AMBIGUOUS_TRANSITION: same event has overlapping transitions without unique priorities: TALK_TO_NPC`。
- **修复方式**:
  - 删除行 288 的冗余 `<transition source="s2" target="s2">`（NPC 278544 `SETPRO3`）。

### 故障 2：任务 3940 节点投影重复 (DUPLICATE_NODE_PROJECTION: START:0)
- **触发点**: `src/main/resources/aion/data/static_data/quest_definition/quests/3940.xml`
- **连锁排查发现**:
  - 修复 1722 后，全量 Catalog 编译立即触发 3940 编译失败。
  - 提交 `0823653a7` 尝试重构 3940 米拉詹特武器任务流程，引入 `<node label="hunt" status="START"/>` 与 `<counter source="hunt" target="hunt-done" field="var0" required="306">`。
  - 因 `<counter>` 积木规定 source 节点不得固定计数字段（`COUNTER_SOURCE_PROJECTION_CONFLICT`），作者在 `hunt` 节点中省略了变量声明，导致其缺省投影为 `START:0`，与 `started` 节点的 `START:0` 发生唯一投影碰撞（`DUPLICATE_NODE_PROJECTION`）。
  - 参考同系魔族对应任务 `4944.xml`（潘利尔武器任务）规范：
    - `progress` 位域布局分立阶段字段 `var0`（6-bit）与击杀计数位段 `var1`（9-bit，0..300）。
    - `hunt` 节点固定阶段 `var0=6`。
    - `hunt-done` 节点固定 `var0=6, var1=300`。
    - `<counter source="hunt" target="hunt-done" field="var1" required="300">`。
- **修复方式**:
  - 对齐 `4944.xml` 规范，分离 `var0` 与 `var1`。

## 2. 验证证据 (Verification Evidence)

- 针对全量 `quest_definition_catalog.xml` 中全部 6193 个 `mode="EXECUTABLE"` 的任务进行了 `QuestDefinitionXmlCompiler.compile()` 逐任务全量编译，结果：`Failed count: 0`。
- `QuestDefinitionCatalogManifest.compile()` 加载通过：`SUCCESS! Loaded 6231 entries, executables: 6193`。
- `git diff --check` 无空白缺陷。
