# 任务 2392 可选收集物清理与领奖卡死修复审计

日期：2026-09-14

## 背景与问题陈述

在任务 1922 与 2947 的可选工作物品阻断修复及后续 29 个同形候选排查中，发现任务 2392（`Beautiful Feather` / 莫尔海姆「美丽的羽毛」）存在确定性的交任务阻断缺陷。

### 缺陷机制与历史溯源

1. **分支交付设计**：任务要求玩家从三种鸟类羽毛中任选一种带回给 NPC 798085（Kvasir / 姆奥灵）：
   - `182204159`（Green Virago Feather / 鸟妖的绿色羽毛，掉落 80%）
   - `182204160`（Dark Red Crestlich Feather / 火鸟的红黑色羽毛，掉落 80%）
   - `182204161`（White Pecku Mane / 蛇妖的白色鬃毛，掉落 80%）
2. **提前消耗**：在 `started -> r1/r2/r3` 转换中（动作为 `SETPRO1/2/3`），带有 `<has-item item-id="18220415N" count="1"/>` 并在 `<actions>` 中执行 `<remove-item item-id="18220415N" count="1"/>`。此时玩家背包内该分支羽毛已被扣除（余额归 0），其余两种羽毛通常根本未收集。
3. **完成与预览双重阻断**：
   - 随后的 `rN -> complete` 路由要求同时扣除上述三件物品，且为严格正数 `count="1"`。
   - 随后的 `rN -> rN` 自环预览路由（`USE_OBJECT` / `SELECT_QUEST_REWARD`）同样要求扣除这三件物品 `count="1"`。
   - `QuestMutationPlanner.removalFeasible` 判定正数扣除要求 `remaining >= remove.count()`；因选定羽毛已为 0 且其他两件缺失，`removalFeasible` 判定为 `false`，导致全部三个分支在进入领奖阶段后，完成路由与预览路由必然 `BLOCKED`。客户端点击领奖或预览后无响应。
4. **历史演变证据**：
   - 提交 `fb26a0d49`（`fix(quest): clean beautiful feather work items`）曾正确将此 6 条路由上的清理动作设置为 `count="ALL"`，并在 `QuestMutationPlannerTest` 中以 `QuestAction.RemoveItem.ALL` 验证。
   - 随后的机械化提交 `404c5814b`（`Fix migrated quest item removal counts`）根据元数据 `<items>` 的 `count="1"` 将其全局替换为 `count="1"`，并修改了单测预期，导致确定性卡死缺陷回归。
5. **旧脚本对照**：
   - `origin/history:src/main/java/com/aionemu/gameserver/quest/handlers/morheim/_2392Beautiful_Feather.java` 在 `REWARD` 状态无论持有与否均调用 `removeQuestItem(env, 182204161/160/159, 1)`（旧方法未持有仅返回 false，不阻断执行），随后立即执行 `sendQuestEndDialog(env, rewInd)` 完成任务。

## 修复方案

1. **任务 XML** (`2392.xml`)：
   - 保留 `started -> r1/r2/r3` 的 `has-item count="1"` 检查与 `remove-item count="1"` 扣除（保持交付分支严肃性）。
   - 将 `r1/r2/r3 -> complete` 的三项羽毛扣除改为 `count="ALL"`。
   - 将 `r1/r2/r3 -> r1/r2/r3` 自环预览的三项羽毛扣除改为 `count="ALL"`。
   - 补充中英双语注释，阐明羽毛已在前置步骤扣除、奖励阶段为可选清理物的事实。
2. **测试维护** (`QuestMutationPlannerTest.java`)：
   - 将 `beautifulFeatherCleansEveryWorkItemOnRewardRoutes` 中的预期修正回 `QuestAction.RemoveItem.ALL`。
3. **专项回归测试** (`Quest2392BeautifulFeatherTest.java`)：
   - 覆盖 3 个分支的选择性交付与扣除推进到 `r1/r2/r3`。
   - 锁定空背包状态下（羽毛在进入 REWARD 时已扣除），所有 3 个分支的所有奖励动作（8..23）均可顺利规划完成。
   - 锁定残留额外羽毛时的整叠清理。
   - 锁定空背包与持有残留物时，奖励窗口预览均不被阻断。

修复提交：`1f1af04dc`（`fix(quest): allow optional 2392 turn-in item cleanup`）。

## 验证证据

1. `xmllint --noout --schema src/main/resources/aion/data/static_data/quest_definition/quest_definition.xsd src/main/resources/aion/data/static_data/quest_definition/quests/2392.xml`：通过。
2. `git diff --check`：通过。
3. Maven 门禁：
   - `mvn test -Dtest=Quest2392BeautifulFeatherTest,QuestMutationPlannerTest,Quest1922RewardTurnInTest,Quest2947RewardTurnInTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest`
   - 测试运行结果：45 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS。
   - 编译与白名单计数：`PRODUCTION_COMPILE_OK=6193`、`PRODUCTION_COMPILE_FAILURES=0`、`PRODUCTION_WHITELIST_VIOLATIONS=0`。

## 剩余风险与状态

- 任务 2392 处于“实现完成、单测验证通过、待真机验收”状态。
- 未启动、停止或重启服务端。
- 未推送远程分支（no push）。
