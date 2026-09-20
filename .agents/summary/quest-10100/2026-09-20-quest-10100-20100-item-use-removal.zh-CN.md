# 10100/20100 use-item 进入 REWARD 未扣除阅读道具

日期：2026-09-20
状态：实现完成；Maven 专项测试、生产目录/白名单已通过；Aion 5.8 客户端验收待完成

## 玩家症状

任务 10100「Kahrun's Intrigue」完成后，任务道具 182215448「Kahrun's Drawing」仍留在背包。20100「Ghost of a Bygone Age」是魔族镜像任务，使用同构道具 182215449 和相同状态机。

## 根因证据

- 初始旧服 handler `911440146:src/main/java/quest/omen_of_chaos/_10100Kahrun_Intrigue.java` 的 `onItemUseEvent` 调用
  `useQuestItem(env, item, 4, 4, true)`。
- 同提交的 `src/main/java/quest/prelude_to_chaos/_20100Ghost_Of_A_Bygone_Age.java` 同样调用
  `useQuestItem(env, item, 4, 4, true)`。
- 旧服 `QuestHandler.useQuestItem` 在动画结束后先执行 `removeQuestItem(env, itemId, 1)`，再执行
  `changeQuestStep(env, 4, 4, true)`；因此道具应在使用事件中、进入 REWARD 前扣除。
- 迁移后 `10100.xml`/`20100.xml` 的 `s4 -> reward` `<use-item>` 路由没有 `<actions>`，也没有
  `<remove-item>`；metadata 没有 `<work-items>`，所以完成时也不会自动清理。
- 道具模板 182215448/182215449 只有 `<read/>` 动作。`ReadAction` 不消费道具，和
  `WORK_ITEM_DECLARATION_LOST_ON_MIGRATION` 中已经记录的契约一致。

## 修复合同

- `10100.xml` 的 `s4 -> reward` use-item 路由增加
  `<remove-item item-id="182215448" count="1"/>`。
- `20100.xml` 的 `s4 -> reward` use-item 路由增加
  `<remove-item item-id="182215449" count="1"/>`。
- `count="1"` 对齐旧 helper 的 `removeQuestItem(env, itemId, 1)`；该路由只有在客户端实际使用道具时才会执行，不用 `ALL` 掩盖缺失道具。
- 新增 `Quest10100And20100ItemUseRemovalTest`，锁定节点投影、唯一非 REWARD 入 REWARD 路由、use-item 事件、var0 条件、事务内扣除和 `LEVEL_AND_VISIBILITY_REFRESH` 提交后顺序。

## 同类审计

- 审计脚本：`.agents/summary/quest-10100/audit_use_item_reward_cleanup.py`（按需生成 `use-item-reward-audit.tsv`）。
- 全生产目录共发现 71 条 `use-item -> REWARD` 路由，其中 47 条满足“道具由本任务发放且 source 非 REWARD”；在旧 handler 明确以 `useQuestItem(..., true)` 消费道具的同型合同中，只有 10100/20100 同时没有路由扣除、work-item 完成清理或所有 reward->complete 显式清理。
- 15334/25334 的旧 handler 不在 use 时扣除，而是在 reward->complete 扣除，现行 XML 已覆盖，不纳入本修复。
- 18738/28738 的旧 handler 不在 use 时扣除；炸弹由技能使用链处理，不属于本次阅读道具合同。
- 10034、13830..13834 等 quest_data/typed work-item 任务由完成清理覆盖，不重复在 use-item 路由扣除。

## 此前审计为什么漏掉

- `QuestWorkItemMigrationCoverageTest` 的审计宇宙来自 `quest_data.xml` 的 `<quest_work_items>`；10100/20100 的 quest_data 条目没有该声明，因此根本不在 1337 条可执行任务集合内。
- 旧 handler 的 `useQuestItem(..., true)` 是独立的消费语义来源，但 `quest_data.xml` 没有把它表达为 work item；迁移时只对照了 XML/quest_data，没有对旧 handler 做全量消费合同提取。
- `QuestItemSourceContractGateTest` 覆盖的是 `collect-item` 监听对象和收集交付扣除；它不会检查 `give-item -> use-item -> REWARD` 生命周期。
- 10 个旧 handler 明确使用 `useQuestItem(..., true)` 的任务中：10527/20527 有 work item 和路由扣除，3722/4722、1636 有 work item，11031/11032 有 work item，11033 有路由扣除加 work item；只有 10100/20100 同时没有被两条既有权威覆盖。
- 新增 `QuestUseItemRewardCleanupGateTest`，以编译后 IR 为权威扫描所有 `GiveItem -> UseItem -> REWARD` 路由；每条路由必须满足：路由内 `remove-item`、metadata work-item 清理、或所有 `reward -> complete` 路由显式清理。18738/28738 的 164000342 作为有旧 handler/炸弹技能证据的非消费例外登记。

## 验证

- `xmllint --noout --schema src/main/resources/aion/data/static_data/quest_definition/quest_definition.xsd`：10100、20100 均通过。
- Python XML 解析：通过。
- `git diff --check`：通过。
- IntelliJ 文件检查：两个 XML 和新测试均无错误。
- 已执行：

```bash
rtk mvn -q -Dtest=Quest10100And20100ItemUseRemovalTest,QuestUseItemRewardCleanupGateTest,QuestWorkItemMigrationCoverageTest,QuestDefinitionCatalogManifestTest,ProductionCatalogWhitelistVerificationTest test
```

- 结果：全部通过；生产 catalog `PRODUCTION_COMPILE_OK=6189`、`FAILURES=0`、`INTERACTION_OBJECT_FAILURES=0`、`WHITELIST_VIOLATIONS=0`。
- 客户端验收：待用户在 Aion 5.8 客户端确认“使用图画后背包立即扣除、任务进入 REWARD、领奖后可完成”。

## 存档边界

修复后新的 `s4 -> reward` 使用路径会扣除道具。修复前已经处于 REWARD 或 COMPLETE 的存档不会被该路由追溯处理；如需清理存量角色，应另行按明确对象执行 GM/DB 清理，不在本次任务 XML 中增加无条件完成扣除。

## 修改文件

- `src/main/resources/aion/data/static_data/quest_definition/quests/10100.xml`
- `src/main/resources/aion/data/static_data/quest_definition/quests/20100.xml`
- `src/test/java/com/aionemu/gameserver/questEngine/definition/Quest10100And20100ItemUseRemovalTest.java`
- `src/test/java/com/aionemu/gameserver/questEngine/definition/QuestUseItemRewardCleanupGateTest.java`
- `.agents/summary/quest-10100/audit_use_item_reward_cleanup.py`
- `.agents/summary/quest-10100/2026-09-20-quest-10100-20100-item-use-removal.zh-CN.md`
