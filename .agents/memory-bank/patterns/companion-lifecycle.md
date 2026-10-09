# Companion Lifecycle & Visibility (跟随物生命周期与可见性)

本文档记录玩家跟随物（守护灵 minion；召唤兽 summon 同构）在飞行传送等特殊移动期间的
生命周期契约：**主仆对在 KnownList 上距离免疫 + 飞行传送入口收起/落地恢复**，
以及主人 KnownList 可见距离不得参与跟随物生命周期的原因。

> Pattern IDs: `CL-001`
> card_status: ACTIVE; 绑定服务端 KnownList/传送实现（非客户端反编译结论）
> scope: 玩家跟随物（minion；summon 用同一对 suspend/restore 模式）在飞行传送（FLIGHT_TELEPORT：传送门/风场/副本/任务入口）期间的收起与恢复契约；主仆对在 KnownList 的双向距离豁免（MinionKnownList）；主人 KnownList 95m 可见距离的行为边界
> last_reviewed: 2026-10-09

---

## [CL-001] 跟随物生命周期不得由主人 KnownList 可见距离决定——主仆对距离免疫 + 飞行传送进入收起/落地恢复
<!-- pattern-metadata
status: CONFIRMED
scope: 玩家跟随物（minion；summon 用同一对 suspend/restore 模式）在飞行传送（FLIGHT_TELEPORT：传送门/风场/副本/任务入口）期间的可见性与生命周期；主仆对在 KnownList 的双向距离豁免；主人 KnownList 的 95m 可见距离语义
first_seen: 2026-10-07
last_verified: 2026-10-09
symptom: 飞行传送、风之路（windstream）或高移速下聊天窗每 4~5 秒交替出现「召唤了宠物精灵XXX」「取消召唤宠物精灵XXX」，地面正常速度无此现象；即 SM_MINIONS(5)/(6) 被反复成对下发
root_cause: 飞行传送/风之路期间客户端持续上报位置（FLIGHT_TELEPORT 走 CM_MOVE_IN_AIR），每包都 `world().updatePosition(player,…)`（默认 updateKnownList=true）→ 主人 KnownList 重算；主人可见距离 `VisibleObject.VisibilityDistance=95`，而 minion 跟随 tick 仅 1s/2s、25m 瞬移阈值——高速移动下 minion 反复越过 95m 边界：越界 → notSee → SM_MINIONS(6)（收回家，「取消召唤」）；下一 tick 瞬移拉回、双向重建 → see → SM_MINIONS(5)（「召唤了」）。周期≈95m÷速度+tick 延迟；地面正常速度到不了 95m/s 故不出现。Summon 不受影响（CreatureAwareKnownList，超距解散是刻意的真端语义）。**入口收起方案的边界**：风之路不设置 FLIGHT_TELEPORT 状态（PlayerActions.setPlayerMode 只写 windstreamPath），普通高移速更无独立入口可挂钩——高速场景无法逐入口枚举，必须根因修复
fix_or_guardrail: ①**根因修复（覆盖一切高速场景）**：minion 装配 `MinionKnownList`（继承 PlayerAwareKnownList），覆写 `checkObjectInRange` 与 `checkReversedObjectInRange` 对主人恒真——主人侧 forget/find 经 minion 反向判定保留、minion 侧自身距离判定豁免，**双向缺一不可**（minion 侧遗忘 master 会经 `master.knownList.del(minion)` 再次触发 notSee→SM_MINIONS(6)）；Pet 保持 PlayerAwareKnownList 不变。②飞行传送（FLIGHT_TELEPORT）仍保留「进入收起、落地恢复」：入口调用 `MinionService.suspendForFlyTeleport`（`isSpawned()` 守卫 + `synchronized (minion)` + `world().despawn(minion)`），`onFlyTeleportEnd()` 非 windstream 分支、`updateZone()` 后、紧邻 `SummonsService.restoreAfterTeleport` 调用 `restoreAfterFlyTeleport`。③**收起≠收回**：禁止用 `despawnMinion`（删授予技能、清 Buff 与功能开关、`setMinion(null)`）。④**新增飞行传送入口必须挂 suspend**（门禁 `FlyTeleportMinionSuspendGateTest` 全仓扫描 `.setState(CreatureState.FLIGHT_TELEPORT)` 文件）；**minion 装配必须是 MinionKnownList**（门禁 `MinionKnownListTest` 锁 spawner 装配行与 Pet 不受影响）。⑤不要在 `Player.setFlightTeleportId`/`setState` 等核心 setter 做业务副作用，显式在入口调用
evidence: src/main/java/com/aionemu/gameserver/world/knownlist/MinionKnownList.java（主仆双向豁免）; src/main/java/com/aionemu/gameserver/spawnengine/VisibleObjectSpawner.java（spawnMinion 装配 MinionKnownList，Pet 保持 PlayerAwareKnownList）; src/main/java/com/aionemu/gameserver/services/toypet/MinionService.java（suspendForFlyTeleport/restoreAfterFlyTeleport）; src/main/java/com/aionemu/gameserver/controllers/PlayerController.java:1176-1178（onFlyTeleportEnd 恢复，紧随召唤兽）; 入口 9 文件：services/teleport/TeleportService2.java（FLIGHT 分支）、ai/worlds/{norsvold/DF6,iluma/LF6,cygnea/LF5,enshar/DF5}_FieldAttractionAI2、ai/instance/beshmundirTemple/Plegeton_BoatmanAI2、ai/instance/IDAb1_Heroes/IDAb1_Heroes_TeleporterAI2、ai/instance/steelRake/Suspicious_CannonAI2、questEngine/runtime/PlayerQuestEffectPort.java; 机制链：network/aion/clientpackets/CM_MOVE_IN_AIR.java:53-60、world/World.java:385-387、world/knownlist/KnownList.java:213-220（forgetObjects）、controllers/PlayerController.java:147-148,168-169（see/notSee→SM_MINIONS 5/6）、controllers/MinionController.java:148-153（25m 瞬移）、serverpackets/SM_MINIONS.java 类头（5=召唤/6=收回）; 测试 src/test/java/com/aionemu/gameserver/world/knownlist/MinionKnownListTest.java、src/test/java/com/aionemu/gameserver/services/toypet/FlyTeleportMinionSuspendGateTest.java; .agents/summary/fly-teleport-minion-suspend/2026-10-07-fly-teleport-minion-summon-loop.zh-CN.md、.agents/summary/fly-teleport-minion-suspend/2026-10-08-windstream-fast-move-minion-root-fix.zh-CN.md
validation: static（IDE inspections 0 error；入口集合↔suspend 集合 9/9 一致）; runtime（2026-10-07 用户实机：飞行传送无刷屏、落地恢复通过；2026-10-08 风之路/高移速场景的根因修复实机复验待用户回报）; focused-test（2026-10-09 IDEA MCP：MinionKnownListTest 2/2 + FlyTeleportMinionSuspendGateTest 3/3 全绿）
boundaries: Pet（玩具宠物）同用 PlayerAwareKnownList 但无服务端瞬移跟随路径，未纳入（保持最小改动）；主人 KnownList 95m 可见性规则本身未改（只有主仆对豁免，旁观玩家仍按各自 95m 感知 minion）；MinionKnownListTest 经反射写 Minion.master final 字段，JVM 打印 final-field mutation 警告（与仓库既有 Objenesis 测试同款，不阻断，未来 JDK 收紧时需改测试构造方式）；飞行传送中断线走完整 despawnMinion（setMinion(null)），登录自动重召与进图自愈（CM_LEVEL_READY !isSpawned→spawn）保留
superseded_by: none
first_check: 高速移动刷「取消召唤/召唤了」时：①该 minion 是否装配 MinionKnownList（查 spawner 装配行 + 跑 MinionKnownListTest）；②若发生在飞行传送，跑 FlyTeleportMinionSuspendGateTest 核对入口收起与落地恢复；③包日志中 SM_MINIONS 6/5 是否成对高频
keywords: 宠物精灵、守护灵、召唤了、取消召唤、刷屏、飞行传送、风之路、风道、windstream、高移速、加速、FLIGHT_TELEPORT、CM_MOVE_IN_AIR、KnownList、95m、可见距离、SM_MINIONS、MinionKnownList、suspendForFlyTeleport、restoreAfterFlyTeleport、onFlyTeleportEnd、风场、FieldAttraction、收起、Pet
-->

**规则**：跟随物的生命周期由**显式的进入/退出事件**与**主仆固有关系**决定，不得由主人 KnownList
的可见距离被动决定。两层防护：

1. **根因层（覆盖一切高速场景）**：minion 装配 `MinionKnownList`——主仆对在两个方向上恒为
   「在范围内」（`checkObjectInRange` + `checkReversedObjectInRange` 对 master 短路），主人
   KnownList 的 forget/find 不再移除或拒绝重发现 minion；风之路、加速坐骑、GM 速度等无需逐入口处理。
   minion 侧的豁免同样必需：minion 自己遗忘 master 时会经 `master.knownList.del(minion)` 触发
   `notSee → SM_MINIONS(6)`。
2. **事件层（飞行传送）**：FLIGHT_TELEPORT 入口仍显式收起、落地恢复（与召唤兽
   `SummonsService.suspendForTeleport/restoreAfterTeleport` 同位同序）：

```text
进入（9 个入口之一）
  → MinionService.suspendForFlyTeleport(player)     // world().despawn(minion)，保留全部状态
  → 客户端一条 SM_MINIONS(6)「取消召唤宠物精灵」
落地（CM_EMOTION LAND_FLYTELEPORT → onFlyTeleportEnd 非 windstream 分支）
  → MinionService.restoreAfterFlyTeleport(player)   // setPosition 到主人 + world().spawn(minion)
  → 客户端一条 SM_MINIONS(5)「召唤了宠物精灵」
```

**机制要点**：

- 飞行传送中 `CM_MOVE_IN_AIR` 每包刷新主人 KnownList（`World.updatePosition` 默认 `updateKnownList=true`）；
  主人可见距离 95m（`VisibleObject.VisibilityDistance`）是**普通可见性边界**，而 minion 的跟随 tick 只有
  1s/2s（25m 瞬移阈值）——两者不匹配，高速移动必然反复穿越 95m。收起后 minion 不在世界（`!isSpawned()`），
  跟随任务自动跳过；距离免疫后 minion 永不离开主人 KnownList，超 25m 仍由跟随 tick 瞬移拉回（`SM_MOVE` 同步）。
- **收起 ≠ 收回**：`despawnMinion` 会删授予技能、清 `isLooting/isBuffing` 与 `setMinion(null)`——
  飞行传送只是"暂时看不见"，全部状态必须原样保留（`restoreAfterFlyTeleport` 仅位置同步 + spawn）。
- **覆盖契约（双门禁）**：`FlyTeleportMinionSuspendGateTest` 全仓扫描 `.setState(CreatureState.FLIGHT_TELEPORT)`
  文件（点号前缀精确匹配，避免误匹配 `unsetState`）守护入口收起；`MinionKnownListTest` 锁 spawner
  装配行与双向豁免行为，防止装配回退到 `PlayerAwareKnownList`。
- 本契约与召唤兽的 `SummonsService.suspendForTeleport/restoreAfterTeleport` 完全对称（同一位置、同一顺序），
  排查任一类跟随物时先核对这一对调用是否成对。

---

## 修订记录

### 2026-10-09：入口收起方案 → 主仆距离免疫根因修复

- **trigger**：上轮（2b483f0db）入口收起/落地恢复后，用户实测进风之路（windstream）与高移速仍刷屏。
- **root_cause 补充**：风之路不设 `FLIGHT_TELEPORT`、普通高移速无独立入口——高速场景无法逐入口枚举，入口方案结构性不可全覆盖。
- **change**：落地主仆对 KnownList 双向距离豁免（`MinionKnownList`，spawner 装配点切换 + 装配门禁）；飞行传送入口收起/恢复保留不变（已实机验收）。
- **evidence**：`MinionKnownList.java`、`VisibleObjectSpawner.spawnMinion` 装配行、`MinionKnownListTest`；summary `2026-10-08-windstream-fast-move-minion-root-fix.zh-CN.md`。
- **validation**：IDEA MCP 门禁 `MinionKnownListTest` 2/2 + `FlyTeleportMinionSuspendGateTest` 3/3 全绿（2026-10-09）；风之路/高移速实机复验待用户回报。
