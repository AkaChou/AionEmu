# 任务 2947 可选工作物品同类问题审计

日期：2026-09-14

## 背景与结论

任务 1922 的可选工作物品阻断修复已提交为 `ce4690091`（`fix(quest): allow optional 1922 turn-in item cleanup`），客户端验收记录提交为 `0b1d58879`。本次在该修复之后，对全部任务 XML 中与 `complete-quest` 同事务的正数 `<remove-item>` 做同类排查。

结论：强证据同型项只有任务 2947，本轮一并修复；其余静态候选保持原状并标记 `EVIDENCE_REQUIRED`，不做批量替换。

## 任务 2947 根因

- 提交 `1da62d7d` 为 2947 的 11 条进阶职业完成分支新增 `<remove-item item-id="182207037" count="1"/>`，这些分支只有 `advanced-class-is` 条件，没有 `has-item` 守卫。
- 旧 handler `origin/history:src/main/java/com/aionemu/gameserver/quest/handlers/abyss_entry/_2947Following_Through.java` 在 `REWARD` 状态对 NPC 204301 直接执行 `sendQuestEndDialog(env, choice)`，完成时不检查、不扣除该物品。
- 物品 182207037 是 `Golden Helmet of Urgasch`（模板 `quest_2947a`，`max_stack_count=20`），来自任务中途击杀 NPC 700268 的掉落；玩家走到最终 NPC 时可能已经不在背包。
- `QuestMutationPlanner.removalFeasible` 对正数 `count` 要求余额足够，物品缺失时完成路由直接不可行，表现与 1922 相同：最终领奖动作没有响应。

修复方式与 1922 一致：11 处完成分支全部改为 `count="ALL"`，有则整叠清理、无则继续；新增 `Quest2947RewardTurnInTest` 锁定 11 条职业分支的 source/target/priority/conditions/actions/after-commit、奖励预览页与 planner 结果。

修复提交：`e5ccac81d`（`fix(quest): allow optional 2947 turn-in item cleanup`），只包含 `2947.xml` 与 `Quest2947RewardTurnInTest.java`。

## 同类扫描统计

扫描范围为 `src/main/resources/aion/data/static_data/quest_definition/quests/*.xml` 当前工作区内容：

- 与 `complete-quest` 同事务的正数 `<remove-item>` 共 109 条路由，涉及 51 个物品 ID。
- 24 条路由（6 个物品）已经带 `has-item` 守卫，不属于同类。
- 其余 45 个物品中，32 个在 `origin/history` 任务 handler 中完全没有引用；其中 29 个同时在 metadata `<items>` 中声明为任务工作物品，与 1922/2947 修复前的形状一致。
- 旧 handler 明确扣除的候选属于既有合同，不属于本次缺陷，例如 24051 `removeQuestItem(env, 182215376, 1)`、30103 `removeQuestItem(env, 182209189, 1)`。

## 处置边界

上述 29 个同形候选本轮不修改。静态扫描只能证明“严格正数扣除 + 无 `has-item` 守卫 + 旧 handler 无引用”这个形状，不能区分必需交付物与可选工作物品；把 `count` 批量改成 `ALL` 会放过本应阻止完成的必需交付。这些候选需要逐任务取证：旧 handler 的完成路径是否扣除/校验该物品、客户端页面是否允许缺物完成、以及实际运行表现。全部保持 `EVIDENCE_REQUIRED`。

## 验证证据

- `xmllint --noout --schema src/main/resources/aion/data/static_data/quest_definition/quest_definition.xsd src/main/resources/aion/data/static_data/quest_definition/quests/2947.xml`：validates。
- `git diff --check`：通过。
- 专项测试：`Quest2947RewardTurnInTest` 4/4 通过。本会话未运行 Maven，使用既有 `../../../target/classes` 加 JUnit Platform launcher 执行。
- 变异校验：同一测试指向 HEAD 中修复前的 2947.xml 时 3/4 失败（actions 仍为 `RemoveItem[count=1]`），证明测试确实锁定该缺陷；预览页测试不涉及该合同，保持通过。
- planner 前后对比探针：修复前，空背包下 11/11 职业的完成路由都不可规划，持有 1 个时才可规划；修复后，空背包与持有 3 个都可规划，`nextStatus=COMPLETE`，packed vars 均为 167772160。

## 未执行门禁与剩余风险

- 未运行 Maven focused/catalog/whitelist 门禁；未启动、停止或重启服务端。
- 2947 尚无客户端验收，状态为“实现完成、待验收”。
- 其他 29 个同形候选仅为静态筛查结果，未逐一取证。
- 未执行 push。
