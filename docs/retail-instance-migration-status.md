# 真端副本机制迁移状态

## Harmony

- 地图：`300450000`、`300570000`、`301100000`
- 所有权：真端 world spawn、NPC AI Pattern、waypoint 与条件变量驱动局内阶段；服务端仅保留匹配、计分和结算边界。
- 生成器提交：`f60ea05`、`8922606`
- 服务端提交：`9f18b89 refactor(instance): restore retail Harmony mechanics`
- 验证：三张地图的条件出生、路径、Pattern 支持判定和 PvP Arena 迁移测试已随提交覆盖。

## Drakenseer Lair

- 地图：`301620000`
- 权威证据：`58Server/Map/Worlds/IDF6_Dragon/world_N.xml`、`58Server/Map/XML/NpcAIPatterns_IDF6_Dragon_SSH.xml`。
- 所有权：`IDF6_Dragno_TimeCheck`、`IDF6_Dragon_Control_01/02`、`IDF6_Dragon_Wave_Start_A/B/C`、`IDF6_Dragon_Named` 负责计时、消息、波次、Boss、失败传送与出口；`npc_drops_part_009.xml` 负责 `220450` 的六组掉落。
- 生成器提交：`6f2b1de fix(converter): restore Drakenseer wave conditions`
- 服务端提交：本次 `refactor(instance): restore retail Drakenseer Lair`
- 数据验证：`12` 个变量、`133` 条条件、`133` 个出生槽、`100` 条 Wave 条件，拒绝条件为 `0`；核心 Pattern 全部可启用。
- 删除的错误机制：手写 FlyRing 计时、三塔死亡计数、强制技能 `21791`、手写 Boss 出口与自定义掉落。
