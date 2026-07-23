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

## Beshmundir Temple

- 地图：`300170000`
- 状态：`HYBRID`；真端数据接管全部阶段出生，Handler 仅保留两项没有数据消费者的交互桥接。
- 权威证据：`idcatacombs/world_N.xml`、当前静态/条件出生、`30208/30308` 任务工作物品和 `30231/30331` 钥匙奖励。
- 所有权：祭坛 `730274` 与蓝焰门 `730290` 由真端数据出生；`799506` 不存在于该地图 world/静态/条件出生，故仍由祭坛完成回调生成。任务结算回收 `182209610/182209710`，门交互消耗 `185000091`。
- 验证：`BeshmundirTempleInstanceTest` 锁定两项桥接的无替代依据与旧 Handler 机制已删除。

## Kromede's Trial

- 地图：`300230000`
- 状态：`HYBRID`；真端 world、Pattern、任务和静态出生接管主要流程，Handler 保留入口与无数据消费者的交互桥接。
- 权威证据：`idcromede/world_N.xml`、`Cromede_Relic1/2` Pattern、`19220/19270` 变身与 `19247/19248` 遗物技能模板。
- 所有权：遗物与石门由真端数据出生；使用遗物后的玩家增益、石门即时删除、入口变身/影片说明以及离开清理仍由 Handler 承接。入口变身现按每名玩家的实际种族选择，魔族使用 `19270`。
- 验证：`KromedesTrialInstanceTest` 覆盖两族变身映射及既有的 Pattern/任务所有权边界。

## Taloc’s Hollow

- 地图：`300190000`
- 状态：`HYBRID`；真端静态/条件出生、Pattern、门、掉落和任务接管主流程，Handler 仅保留治疗、影片与离开清理桥接。
- 权威证据：`idelim/world_N.xml`、`NpcAIPatterns_IDElim_OSY.xml`、compact NPC AI/条件出生/掉落及两族 `10032/20032` 任务。
- 所有权：五株普通治疗植物、巨型植物条件链和无重生虫卵由数据负责；`Elim_ClodwormNm`/`Elim_NeutflyNm` 通过逻辑门号 `1/2` 控制门 `48/7`，门 `49/7/48` 初始开启，门 `180` 初始关闭可点击。Handler 使用真端 `19229/19230` 技能、播放单人影片并清理副本临时状态。
- 验证：`TalocsHollowQuestMigrationTest` 覆盖任务发放/回滚、条件出生、虫卵、Handler 边界及四扇门的状态和逻辑 ID。

## Raksang Ruins

- 地图：`300610000`
- 状态：真端 Pattern + 条件出生接管；Handler 不再处理 NPC 死亡或出口生成。
- 权威证据：`IDRaksha_solo/world_N.xml` 的 `IDRaksha_Door_5F_Boss_Exit_SPG`，以及 `NpcAIPatterns_TamesSolo_KJS.xml` 的 `IDRaksha_Re_Boss_KJS`。
- 所有权：Boss `236306` 被击杀后由 Pattern 写入 `idraksha_clear=1`，条件出生在 `619.643005/685.139893/527.079773` 生成出口 `730445`。
- 数据验证：`22` 个变量、`109` 条条件；专项测试锁定生产链、真端出口坐标与错误手工回调删除。
