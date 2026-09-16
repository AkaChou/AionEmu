# 塔洛克空洞掉落索引冲突与特殊物品重复注册

- 日期：2026-09-16
- 范围：`DropRegistrationService` 掉落释放边界、`TalocsHollowInstance.onDropRegistered`、攻城宝箱 AI 掉落注册
- 状态：代码、聚焦 Maven/JUnit 回归测试与真实客户端拾取验证均已通过；待本地提交

## 现象

击杀怪物后掉落列表可显示多行，但点击任意一行都返回同一句“已经拥有限制持有物品%0，无法拥有更多”，其中 `%0` 为“须希尔的腐蚀液”。塔洛克空洞的其他 Boss 也会出现同类表现。

## 根因

1. `SM_LOOT_ITEMLIST` 只把 `DropItem.index` 写给客户端，`CM_LOOT_ITEM` 也只回传该索引；同一尸体的重复索引无法区分目标物品。
2. `215456` 的基础 NPC 掉落表已包含 `185000088` 和 `164000137`，但 `TalocsHollowInstance.onDropRegistered` 又无条件追加同两件物品。其他塔洛克 Boss 也有相同的“基础数据 + Handler 无条件追加”模式。
3. 多个实例 Handler 把追加掉落的索引硬编码为 `1`，因此同一尸体上基础掉落与 Handler 掉落共享索引。`requestDropItem` 按索引遍历 `HashSet` 时先命中已持有的限持钥匙，于是所有同索引行都走同一句限持提示。
4. 实例 Handler 还把 `regDropItem` 的第三参数写成 NPC 模板 ID，而掉落表实际使用 NPC 对象 ID；该值虽多数在后续 `requestDropItem` 中被纠正，但会增加组队分配路径的不确定性。

## 修复

- `DropRegistrationService.registerDrop` 在所有注册钩子（基础 NPC、任务、活动、全局、实例 Handler、AI `DROP_REGISTERED`）结束后，向玩家发送掉落状态和自动拾取前调用 `normalizeDropIndices(droppedItems)`，将同一尸体的索引重排为 `1..N` 的唯一连续值。
- `TalocsHollowInstance` 增加 `registerDropItemIfAbsent`：按 `DropItem.getDropTemplate().getItemId()` 判断基础数据是否已提供目标物品，仅缺失时追加，并使用 `npc.getObjectId()` 注册。
- `Treasure_Box_Success_BossAI2` 直接维护自己的掉落集合，因此在该集合填充完成后同样调用 `normalizeDropIndices`；同时改为使用宝箱 NPC 对象 ID。
- 回归测试：
  - `DropRegistrationServiceTest#normalizesDuplicateDropIndicesToUniqueSequentialValues`
  - `DropRegistrationServiceTest#normalizesDropIndicesAfterInstanceRegistrationAndBeforeLootPackets`
  - `TalocsHollowInstanceTest#handlerDeduplicatesSpecialDropsPresentInBaseData`
  - `TalocsHollowInstanceTest#baseDropDataProvidesTheGuaranteedSpecialItemsOnlyOnce`

## 关键证据

- `src/main/java/com/aionemu/gameserver/services/drop/DropRegistrationService.java:179`
- `src/main/java/com/aionemu/gameserver/services/drop/DropRegistrationService.java:420`
- `src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TalocsHollowInstance.java:123`
- `src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TalocsHollowInstance.java:169`
- `src/main/java/com/aionemu/gameserver/ai/siege/Treasure_Box_Success_BossAI2.java:142`
- `src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_005.xml` 中 `215456` 的 `common_0` 已包含 `185000088`、`164000137`
- 客户端包：`src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_LOOT_ITEM.java` 只读取 `targetObjectId + index`

## 验证边界

已执行：

- IDE 文件级检查、`git diff --check`、静态断言。
- `mvn -B test -Dtest='DropRegistrationServiceTest,TalocsHollowInstanceTest'`：10 例通过，0 失败，0 错误，BUILD SUCCESS。
- `mvn -B test -Dtest='DropServiceTest,DropDistributionServiceTest,KromedesTrialInstanceTest'`：14 例通过，0 失败，0 错误，BUILD SUCCESS。
- 真实客户端拾取验证成功（2026-09-16 用户确认）。

未执行：Maven 全量测试。未在 Agent 侧启动、停止或重启服务端。

## 适用边界

本次归一化覆盖 `DropRegistrationService.registerDrop` 结束前的初始掉落，以及直接维护掉落集合的攻城宝箱 AI。若未来在掉落列表已释放后异步追加动态掉落，仍需调用 `normalizeDropIndices` 或在追加时分配未占用索引；否则可能重新产生同索引行。
