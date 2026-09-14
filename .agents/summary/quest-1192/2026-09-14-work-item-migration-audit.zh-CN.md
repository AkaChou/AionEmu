# 任务工作物品迁移缺口全量审计（2026-09-14）

## 触发任务

任务 1192「Verteron Reinforcements / 贝尔特伦要塞的支援请求」：任务完成后，任务道具
182200556「Reinforcement Request / 支援请求书」残留在背包。

## 权威证据链

1. `quest_data.xml:824` 的 `<quest_work_items>` 声明 `1192` 的工作物品为 `182200556`。
2. retail 模板 `origin/history:.../compact/quests/scripts/zz_retail_simple_quests.xml:8532`：
   接取时 `start_give_item_id="182200556"`，步骤 NPC 203701 处 `remove_item_id="182200556"`。
3. 旧引擎 `QuestService.setFinishingState`（commit `911440146`）在完成任务时**无条件**清理
   全部 `questWorkItems`；同一份 `quest_work_items` 还参与掉落抑制判定。
4. 当前 typed 引擎只在 metadata 声明了 `<work-items>` 时执行同样的清理：
   `QuestMutationPlanner.appendCompletionWorkItemCleanup`（`QuestMutationPlanner.java:291-302`）
   与放弃路径 `QuestService.java:1450`。

## 根因

`quests/1192.xml` 的 `<metadata>` 缺少 `<work-items>` 声明，且 `f07723711` 为 NPC 203701 添加
`started → reward` 的 `SETPRO1` 转换时标注「无物品变更」。两者叠加：玩家走 203701/203833
路径进入 REWARD 时道具不会被交出，完成时引擎也不会自动清理。

**关键盲区**：审计若只检查「XML 中是否存在该物品的 remove-item」会漏报 1192 ——
`1192.xml` 确实有一处 `remove-item item-id="182200556"`，但它挂在 `SELECT_QUEST_REWARD`
的转换上，玩家实际走的 `SETPRO1` 路径没有覆盖。

## 全量审计结果

对 `quest_data.xml` 中全部 1337 个声明了 `quest_work_items` 且为 `EXECUTABLE` 的任务：

| 阶段 | 分类 | 数量 |
|---|---|---:|
| 修复前 | 已正确迁移 `<work-items>` | 1156 |
| 修复前 | 有显式 remove-item 但无声明 | 57 |
| 修复前 | 部分物品无兜底 | 8 |
| 修复前 | **无声明且无兜底（必然残留）** | **111** |
| 修复前 | 声明内容与 legacy 不一致 | 5 |
| **修复后** | 已正确迁移 | **1336** |
| **修复后** | 证据冲突待裁定 | 1（4942） |

## 修复内容

1. **111 个任务**：补齐 `<work-items>` 声明，对齐 `quest_data.xml`。
2. **64 个任务**：补齐 `<work-items>` 声明（覆盖「某条进入 REWARD 的路由不交出物品」的
   51 个真缺陷，以及 13 个「每条路由都 remove 但无声明」的同类）。
3. **4 个任务**（19026/19032/29026/29032）：`<work-items>` 去除重复条目，回归 legacy 集合。
   （这 4 个原本多了一条重复 id，会让引擎在完成时重复追加清理动作。）
4. **1192**：补齐 `<work-items>`，并把 203701 的 `SETPRO1` 转换补回
   `<remove-item item-id="182200556" count="ALL"/>`（按 QE-007 规范，旧存档缺物不阻断路由）。

## 未收敛案例：4942

三方证据冲突，需人工裁定，已从自动断言排除并在测试中显式登记：

- 旧 handler `_4942Proving_Proficiency` 完成时移除的是 `186000085`（收集物）；
- `quest_data.xml:18838` 声明 work item 为 `182207122`（任务道具「Insignia of the Fenris's Fangs」）；
- 当前 XML 声明 6 个 `152206*`（均为 `category="RECIPE"` 图纸，在 `sN → s7` 时由 `give-item` 发放）。

三者互不相同。`152206*` 确实是分支路径上实际发放的物品，但旧 handler 并未在完成时回收它们。

## 验证

- `xmllint --noout --schema quest_definition.xsd`：180 个改动文件全部通过。
- `git diff --check`：通过，无空白污染。
- `PRODUCTION_COMPILE_OK=6193`、`PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`。
- 新增 `QuestWorkItemMigrationCoverageTest`：
  - `verteronReinforcementsDeclaresItsWorkItemAndTurnsItInAtLavirintos`：锁定 1192 的
    接取发放、`<work-items>` 声明与 203701 交出契约。
  - `everyExecutableWorkItemQuestIsCleanedUpOnCompletion`：目录级审计，要求每个 EXECUTABLE
    任务声明的 `<work-items>` 覆盖 legacy `quest_work_items`。
  - `everyItemEnteringRewardIsHandedInOrDeclaredAsAWorkItem`：未声明 `<work-items>` 时，
    每条「源非 REWARD、目标 REWARD」的路由都必须自己交出物品。

## 既有失败基线（与本修复无关）

以下失败在改动前即存在，已通过 `git stash` 基线对比确认：

- `QuestClientContractGateTest`：`count=23`，全部为 `BUTTON_WITHOUT_ROUTE`（页面路由问题）。
- `QuestDraupnirNpcVariantContractTest`：quest 80805 npc 213780 的 kill route 不可规划。
- `InstanceWalkerFormationsPositionGroupingTest`、`RetailAiDefinitionLoaderTest`、
  `NpcMoveControllerPathTest`、`AI2EngineRetailSelectionTest`：属于工作区其他未提交改动
  （Talocs Hollow / retail AI / geo 相关）。

## 审计脚本

- `audit_work_item_migration.py`：分类统计 legacy 工作物品的迁移状态。
- `audit_precise_invariant.py`：按「每条入 REWARD 路由」的精确不变量定位真缺陷。
- `backfill_work_items.py`：回填第一批 111 个任务。
- `backfill_remaining_work_items.py`：回填剩余 64 个任务。
- `fix_work_items_order.py`：按 XSD 的 metadata 子元素顺序重排误插的 `<work-items>`。
