# Companion Lifecycle & Visibility (跟随物生命周期与可见性)

本文档记录玩家跟随物（守护灵 minion；召唤兽 summon 同构）在飞行传送等特殊移动期间的
生命周期契约：**进入收起、落地恢复**，以及主人 KnownList 可见距离不得参与跟随物生命周期的原因。

> Pattern IDs: `CL-001`
> card_status: ACTIVE; 绑定服务端 KnownList/传送实现（非客户端反编译结论）
> scope: 玩家跟随物（minion 守护灵；summon 已有同构实现）在 `FLIGHT_TELEPORT`（传送门/风场/副本/任务入口）期间的收起与恢复契约；主人 KnownList 95m 可见距离的行为边界
> last_reviewed: 2026-10-07

---

## [CL-001] 飞行传送期间跟随物必须「进入收起、落地恢复」——主人 KnownList 不得参与跟随物生命周期
<!-- pattern-metadata
status: CONFIRMED
scope: 玩家跟随物（minion；summon 用同一对 suspend/restore 模式）在飞行传送（FLIGHT_TELEPORT：传送门/风场/副本 NPC/任务入口）期间的可见性与生命周期；主人 KnownList 的 95m 可见距离语义
first_seen: 2026-10-07
last_verified: 2026-10-07
symptom: 飞行传送过程中聊天窗每 4~5 秒交替出现「召唤了宠物精灵XXX」「取消召唤宠物精灵XXX」，地面移动无此现象；即 SM_MINIONS(5)/(6) 被反复成对下发
root_cause: 飞行传送期间客户端持续发 `CM_MOVE_IN_AIR`，每包都 `world().updatePosition(player,…)`（默认 updateKnownList=true）→ 主人 KnownList 重算；主人可见距离 `VisibleObject.VisibilityDistance=95`，而 minion 跟随 tick 仅 1s/2s、25m 瞬移阈值——高速移动下 minion 反复越过 95m 边界：越界 → notSee → SM_MINIONS(6)（收回家，客户端「取消召唤」）；下一 tick minion 瞬移拉回、minion 侧 doUpdate 双向重建 → see → SM_MINIONS(5)（「召唤了」）。周期≈95m÷飞行速度+tick 延迟；地面速度永远到不了 95m/s 故不出现。Summon 不受影响（CreatureAwareKnownList，超距解散是刻意的真端语义）；windstream 不设置 FLIGHT_TELEPORT 状态（PlayerActions.setPlayerMode 只写 windstreamPath），不进该路径
fix_or_guardrail: ①全部飞行传送进入点调用 `MinionService.suspendForFlyTeleport(player)`（`isSpawned()` 守卫 + `synchronized (minion)` + `world().despawn(minion)`），落地在 `PlayerController.onFlyTeleportEnd()` 非 windstream 分支、`updateZone()` 后、紧邻 `SummonsService.restoreAfterTeleport` 调用 `restoreAfterFlyTeleport(player)`（`setPosition` 到主人 + `world().spawn(minion)`）。②**收起≠收回**：禁止用 `despawnMinion`（会删授予技能、清 Buff 与自动拾取/自动 Buff 开关、`setMinion(null)`）；世界层隐藏保留全部状态。③**新增飞行传送入口必须挂 suspend**（门禁 `FlyTeleportMinionSuspendGateTest` 全仓扫描 `.setState(CreatureState.FLIGHT_TELEPORT)` 文件）。④不要在 `Player.setFlightTeleportId`/`setState` 等核心 setter 中做业务副作用，显式在入口调用（对齐 `SummonsService` 既有模式）
evidence: src/main/java/com/aionemu/gameserver/services/toypet/MinionService.java（suspendForFlyTeleport/restoreAfterFlyTeleport）; src/main/java/com/aionemu/gameserver/controllers/PlayerController.java:1176-1178（onFlyTeleportEnd 恢复，紧随召唤兽）; 入口 9 文件：services/teleport/TeleportService2.java（FLIGHT 分支）、ai/worlds/{norsvold/DF6,iluma/LF6,cygnea/LF5,enshar/DF5}_FieldAttractionAI2、ai/instance/beshmundirTemple/Plegeton_BoatmanAI2、ai/instance/IDAb1_Heroes/IDAb1_Heroes_TeleporterAI2、ai/instance/steelRake/Suspicious_CannonAI2、questEngine/runtime/PlayerQuestEffectPort.java; 机制链：network/aion/clientpackets/CM_MOVE_IN_AIR.java:53-60、world/World.java:385-387、world/knownlist/KnownList.java:213-220（forgetObjects）、controllers/PlayerController.java:147-148,168-169（see/notSee→SM_MINIONS 5/6）、controllers/MinionController.java:148-153（25m 瞬移）、serverpackets/SM_MINIONS.java 类头（5=召唤/6=收回）; 测试 src/test/java/com/aionemu/gameserver/services/toypet/FlyTeleportMinionSuspendGateTest.java; .agents/summary/fly-teleport-minion-suspend/2026-10-07-fly-teleport-minion-summon-loop.zh-CN.md
validation: static（IDE inspections 0 error；全仓入口集合↔suspend 集合逐一比对一致，9/9）; runtime（2026-10-07 用户实机复验通过：飞行传送无刷屏、落地守护灵恢复）; focused-test（FlyTeleportMinionSuspendGateTest 已编写并纳入提交，执行由用户侧进行）
boundaries: windstream（风之路）不设 FLIGHT_TELEPORT 状态，未纳入 suspend/restore（若复现按同法扩展 CM_WINDSTREAM 进出点）；Pet（玩具宠物）同用 PlayerAwareKnownList 但无服务端瞬移跟随路径，本次未纳入；未改动主人 KnownList 的 95m 可见性规则本身（其它高速场景如确需同类豁免另议）；飞行传送中断线走完整 despawnMinion（setMinion(null)），登录自动重召与进图自愈（CM_LEVEL_READY !isSpawned→spawn）保留
superseded_by: none
first_check: 飞行传送刷「取消召唤/召唤了」时：①该入口是否调用 suspendForFlyTeleport（跑 FlyTeleportMinionSuspendGateTest 或入口↔suspend 集合比对）；②包日志中 SM_MINIONS 6/5 是否成对高频；③restore 是否位于 onFlyTeleportEnd 非 windstream 分支且紧随 SummonsService.restoreAfterTeleport
keywords: 宠物精灵、守护灵、召唤了、取消召唤、刷屏、飞行传送、FLIGHT_TELEPORT、CM_MOVE_IN_AIR、KnownList、95m、可见距离、SM_MINIONS、suspendForFlyTeleport、restoreAfterFlyTeleport、onFlyTeleportEnd、风场、FieldAttraction、收起、Pet
-->

**规则**：跟随物的生命周期由**显式的进入/退出事件**决定，不得由主人 KnownList 的可见距离被动决定。
飞行传送（含传送门/风场/副本/任务全部入口）**进入即收起、落地即恢复**：

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
  1s/2s（25m 瞬移阈值）——两者不匹配，高速移动必然反复穿越 95m，产生 see/notSee 循环消息。收起后
  minion 不在世界（`!isSpawned()`），跟随任务自动跳过（`MinionController` 两个 task 均有该守卫）。
- **收起 ≠ 收回**：`despawnMinion` 会删授予技能、清 `isLooting/isBuffing` 与 `setMinion(null)`——
  飞行传送只是"暂时看不见"，全部状态必须原样保留（`restoreAfterFlyTeleport` 仅位置同步 + spawn）。
- **覆盖契约**：任何新增的飞行传送入口（AI 对话 10000 分支、任务 `flightTeleport`、传送服务 FLIGHT 分支、
  风场）都必须挂 `suspendForFlyTeleport`；`FlyTeleportMinionSuspendGateTest` 以全仓扫描
  `.setState(CreatureState.FLIGHT_TELEPORT)` 文件的方式守护这条契约（点号前缀精确匹配，避免误匹配 `unsetState`）。
- 本契约与召唤兽的 `SummonsService.suspendForTeleport/restoreAfterTeleport` 完全对称（同一位置、同一顺序），
  排查任一类跟随物时先核对这一对调用是否成对。
