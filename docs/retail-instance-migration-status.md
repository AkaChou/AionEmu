# 真端副本机制迁移状态

## Harmony

- 地图：`300450000`、`300570000`、`301100000`
- 所有权：真端 world spawn、NPC AI Pattern、waypoint 与条件变量驱动局内阶段；服务端仅保留匹配、计分和结算边界。
- 生成器提交：`f60ea05`、`8922606`
- 服务端提交：`9f18b89 refactor(instance): restore retail Harmony mechanics`
- 验证：三张地图的条件出生、路径、Pattern 支持判定和 PvP Arena 迁移测试已随提交覆盖。

## Upper Abyss Storerooms

- 地图：`300120000`、`300130000`、`300140000`
- 状态：`HYBRID`；零手写玩法机制，仅保留下线钥匙清理。
- 权威证据：三图 `world_N.xml`、`NpcAIPatterns_IDAbRe_Up3_SSH.xml`、真端出生与 `ownership_world` 钥匙定义。
- 所有权：条件出生、静态出生、门/宝箱/守财者/Boss Pattern 与掉落均由真端数据负责；服务端仅保留下线时清理五把地图钥匙。
- 数据验证：`12/11/11` 条件、`32/38/38` 条件 NPC、`212/219/221` 静态点；三图宝箱模板、守财者组和 Boss 钥匙均已覆盖。
- 删除的错误机制：手写阶段计时、宝箱生成、Boss/门处理、掉落注入和正常离图清理；保留 `onPlayerLogOut` 防止登出携带副本钥匙。

## Drakenseer Lair

- 地图：`301620000`
- 权威证据：`58Server/Map/Worlds/IDF6_Dragon/world_N.xml`、`58Server/Map/XML/NpcAIPatterns_IDF6_Dragon_SSH.xml`。
- 所有权：`IDF6_Dragno_TimeCheck`、`IDF6_Dragon_Control_01/02`、`IDF6_Dragon_Wave_Start_A/B/C`、`IDF6_Dragon_Named` 负责计时、消息、波次、Boss、失败传送与出口；`npc_drops_part_009.xml` 负责 `220450` 的六组掉落。
- 生成器提交：`6f2b1de fix(converter): restore Drakenseer wave conditions`
- 服务端提交：本次 `refactor(instance): restore retail Drakenseer Lair`
- 数据验证：`12` 个变量、`133` 条条件、`133` 个出生槽、`100` 条 Wave 条件，拒绝条件为 `0`；核心 Pattern 全部可启用。
- 删除的错误机制：手写 FlyRing 计时、三塔死亡计数、强制技能 `21791`、手写 Boss 出口与自定义掉落。
