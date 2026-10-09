# Companion Lifecycle & Visibility (跟随物生命周期与可见性)

本文档记录玩家跟随物（守护灵 minion；召唤兽 summon 同构）的移动架构与生命周期契约：
**移动客户端权威（真端架构）+ 主仆对在 KnownList 上距离免疫 + 飞行传送入口收起/落地恢复**，
以及主人 KnownList 可见距离不得参与跟随物生命周期的原因。

> Pattern IDs: `CL-001`
> card_status: ACTIVE; 绑定服务端 KnownList/传送实现与真端 Familiar 反编译证据
> scope: 玩家跟随物（minion）的移动权威归属（客户端模拟、服务端零参与）；minion 在飞行传送（FLIGHT_TELEPORT：传送门/风场/副本/任务入口）期间的收起与恢复契约；主仆对在 KnownList 的双向距离豁免（MinionKnownList）；主人 KnownList 95m 可见距离的行为边界
> last_reviewed: 2026-10-09

---

## [CL-001] minion 移动客户端权威——服务端零参与；主仆对距离免疫；飞行传送进入收起/落地恢复
<!-- pattern-metadata
status: CONFIRMED
scope: 玩家跟随物（minion）的移动权威归属与生命周期；飞行传送（FLIGHT_TELEPORT：传送门/风场/副本/任务入口）期间的收起与恢复；主仆对在 KnownList 的双向距离豁免；主人 KnownList 的 95m 可见距离语义；真端 Familiar 架构对照
first_seen: 2026-10-07
last_verified: 2026-10-09
symptom: 飞行传送、风之路（windstream）或高移速下聊天窗每 4~5 秒交替出现「召唤了宠物精灵XXX」「取消召唤宠物精灵XXX」且 minion 反复瞬移拉回，地面正常速度无此现象；即 SM_MINIONS(5)/(6) 被反复成对下发
root_cause: 三层叠加：①飞行传送/风之路期间客户端持续上报位置（每包 `world().updatePosition(player,…)` 默认刷新主人 KnownList），主人可见距离 `VisibleObject.VisibilityDistance=95` 会让 minion 反复越界遗忘/重发现（see/notSee → SM_MINIONS 5/6 循环）；②服务端自制跟随（1s/2s tick + >25m `teleportToPlayer` 瞬拉 + 陈旧位置 SM_MOVE 走向包）以 minion 默认速度驱动，追不上主人速度→反复坠落→反复瞬拉，且干扰客户端本就存在的本地跟随；③真端架构证据：5.8 真端服务端 `Familiar.cpp`（762 行）/`FamiliarMapMgr.cpp`（4311 行）中 move/follow/position/speed 关键词零命中，方法全集只有数据管理与召唤/收回状态帧（`SummonFamiliar` 仅调 `User_SetSummonFamiliar`），NPCServer 无 minion，SM/CM_MINIONS 协议无移动通道——**零售 minion 同速跟随由每个客户端基于主人移动流本地模拟，服务端零参与**；本仓宠物（PetController 无任何跟随调度）即同架构活体佐证
fix_or_guardrail: ①**移动客户端权威（真端对齐）**：MinionController 不得含服务端跟随 tick（`scheduleAtFixedRate`）、SM_MOVE 移动包（import `serverpackets.SM_MOVE`）、距离瞬拉（`teleportToPlayer`）；MinionService 召唤路径不得重启跟随调度——门禁 `MinionKnownListTest#minionMovementStaysClientAuthored` 守护；服务端只在生命周期事件触碰 minion：召唤/收回帧、跨图传送位置重置、飞行传送收起/恢复、死亡/登出收回。②**根因可见性层**：minion 装配 `MinionKnownList`（继承 PlayerAwareKnownList），`checkObjectInRange`/`checkReversedObjectInRange` 对主人恒真——双向缺一不可（minion 侧遗忘 master 会经 `master.knownList.del(minion)` 再次触发 notSee→SM_MINIONS(6)）；Pet 保持 PlayerAwareKnownList。③飞行传送（FLIGHT_TELEPORT）保留「进入收起、落地恢复」：入口 `MinionService.suspendForFlyTeleport`、`onFlyTeleportEnd()` 非 windstream 分支 `restoreAfterFlyTeleport`（与召唤兽 suspend/restore 同位同序）；**收起≠收回**（禁止 `despawnMinion` 清技能/Buff/开关）。④新增飞行传送入口必须挂 suspend（门禁 `FlyTeleportMinionSuspendGateTest` 全仓扫描 `.setState(CreatureState.FLIGHT_TELEPORT)`）。⑤门禁断言匹配代码形态（import/调用），不匹配注释术语字样（曾因 Javadoc 含「SM_MOVE」红灯）
evidence: 真端（5.8 服务端反编译源，文件级路径与完整证据链见 summary 2026-10-09 文档）：Familiar 类与 FamiliarMapMgr 类中 move/follow/position/speed 关键词零命中、符号改名表 Familiar 方法全集无 Move/Follow、NPCServer 无 minion、SM/CM_MINIONS 协议无移动通道; 本仓：src/main/java/com/aionemu/gameserver/controllers/MinionController.java（空壳化+真端证据注释）、services/toypet/MinionService.java（无跟随调度）、world/knownlist/MinionKnownList.java（主仆双向豁免）、spawnengine/VisibleObjectSpawner.java（装配）、controllers/PlayerController.java:1176-1178（落地恢复）、controllers/PetController.java（无跟随调度的活体对照）、机制链 network/aion/clientpackets/CM_MOVE_IN_AIR.java:53-60、world/knownlist/KnownList.java:213-220、controllers/PlayerController.java:147-148,168-169; 测试 world/knownlist/MinionKnownListTest.java、services/toypet/FlyTeleportMinionSuspendGateTest.java; summary .agents/summary/fly-teleport-minion-suspend/2026-10-07-fly-teleport-minion-summon-loop.zh-CN.md、.agents/summary/fly-teleport-minion-suspend/2026-10-08-windstream-fast-move-minion-root-fix.zh-CN.md、.agents/summary/fly-teleport-minion-suspend/2026-10-09-retail-client-authored-minion-movement.zh-CN.md
validation: static（IDE inspections 0 error）；focused-test（2026-10-09 IDEA MCP：MinionKnownListTest 3/3 + FlyTeleportMinionSuspendGateTest 3/3 全绿）；runtime（2026-10-07 飞行传送场景实机通过；2026-10-09 客户端权威移动实机通过——高速/风之路无瞬拉无刷屏、正常速度跟随与宠物一致）；retail（真端 Familiar 反编译证据链）
boundaries: Pet（玩具宠物）保持 PlayerAwareKnownList（无距离豁免）但同享客户端权威移动；主人 KnownList 95m 规则本身未改（只有主仆对豁免，旁观者仍按各自 95m 感知 minion）；minion 服务端位置仅在生命周期事件更新（真端同构，无消费者）；若未来发现客户端不跑本地跟随的场景（实机表现为 minion 原地不动），回退=恢复 git 历史中被删的跟随子系统，或改服务端同速 chase（200ms tick + 主人实测速度推进）；`TaskId.MINION_UPDATE`/`MINION_TELEPORT_CHECK` 枚举常量保留但不再调度；MinionKnownListTest 反射写 final 字段有 JVM 警告（不阻断）
superseded_by: none
first_check: minion 跟随/可见性异常时：①确认移动归客户端（MinionKnownListTest#minionMovementStaysClientAuthored 全绿，服务端无跟随 tick/SM_MOVE）；②主人侧遗忘/刷屏→确认装配 MinionKnownList；③飞行传送场景→跑 FlyTeleportMinionSuspendGateTest 核对入口收起与落地恢复；④包日志 SM_MINIONS 6/5 成对高频=KnownList 循环，孤立 6/5 各一条=生命周期事件（正常）
keywords: 宠物精灵、守护灵、召唤了、取消召唤、刷屏、瞬移、瞬拉、跟随、同速、飞行传送、风之路、风道、windstream、高移速、加速、FLIGHT_TELEPORT、CM_MOVE_IN_AIR、KnownList、95m、可见距离、SM_MINIONS、MinionKnownList、suspendForFlyTeleport、restoreAfterFlyTeleport、onFlyTeleportEnd、风场、FieldAttraction、收起、Pet、真端、Familiar、客户端模拟、客户端权威
-->

**规则**：minion 移动**客户端权威**（真端架构：服务端无移动模拟、协议无移动通道，客户端基于主人
移动流本地模拟同速跟随）；跟随物的生命周期由**显式的进入/退出事件**与**主仆固有关系**决定，
不得由主人 KnownList 的可见距离被动决定。三层防护：

1. **移动权威层**：服务端不驱动 minion 移动——禁止跟随 tick、SM_MOVE 移动包、距离瞬拉
   （与 `PetController` 同待遇）；服务端只在生命周期事件触碰 minion。
2. **可见性层**：minion 装配 `MinionKnownList`——主仆对在两个方向上恒为「在范围内」，主人
   KnownList 的 forget/find 不再移除或拒绝重发现 minion；minion 侧豁免同样必需（minion 自己
   遗忘 master 会经 `master.knownList.del(minion)` 触发 `notSee → SM_MINIONS(6)`）。
3. **事件层（飞行传送）**：FLIGHT_TELEPORT 入口显式收起、落地恢复（与召唤兽
   `SummonsService.suspendForTeleport/restoreAfterTeleport` 同位同序）：

```text
进入（9 个入口之一）
  → MinionService.suspendForFlyTeleport(player)     // world().despawn(minion)，保留全部状态
  → 客户端一条 SM_MINIONS(6)「取消召唤宠物精灵」
落地（CM_EMOTION LAND_FLYTELEPORT → onFlyTeleportEnd 非 windstream 分支）
  → MinionService.restoreAfterFlyTeleport(player)   // setPosition 到主人 + world().spawn(minion)
  → 客户端一条 SM_MINIONS(5)「召唤了宠物精灵」→ 客户端从落点重新本地跟随
```

**机制要点**：

- 真端 `Familiar` 服务端零移动代码 + 协议无移动通道 + 宠物活体对照 ⇒ 客户端本地跟随是零售行为
  的唯一权威来源；服务端任何自制驱动都会与它打架（高速下表现为瞬拉循环）。
- **收起 ≠ 收回**：`despawnMinion` 会删授予技能、清 `isLooting/isBuffing` 与 `setMinion(null)`——
  飞行传送只是"暂时看不见"，全部状态必须原样保留。
- **覆盖契约（三门禁）**：`minionMovementStaysClientAuthored`（移动权威）、`MinionKnownListTest`
  其余用例（距离免疫 + 装配）、`FlyTeleportMinionSuspendGateTest`（飞行传送入口收起，全仓扫描
  `.setState(CreatureState.FLIGHT_TELEPORT)`，点号前缀精确匹配）。
- 排查任一类跟随物（minion/summon/pet）时先核对该类移动由谁驱动：真端均为客户端模拟，
  服务端只做状态帧与生命周期事件。

---

## 修订记录

### 2026-10-09：服务端跟随子系统移除——移动客户端权威（真端对齐）

- **trigger**：距离免疫落地后，用户实测高速下 minion 仍反复「瞬移到主人身后→离远→再瞬移」；用户指令「排查真端怎么做的」+「要和真端一样」。
- **root_cause 补充**：瞬拉循环第二来源是服务端自制跟随（1s/2s tick + 25m 瞬拉 + 陈旧位置走向包）以 minion 默认速度驱动、追不上主人，且干扰客户端本地跟随。
- **retail 证据**：`Familiar.cpp`/`FamiliarMapMgr.cpp`/`renames.tsv`/NPCServer/SM-CM 协议五路证据一致——真端服务端零移动参与，客户端本地模拟同速跟随。
- **change**：MinionController 空壳化（删 MinionFollowTask/MinionTeleportTask/teleportToPlayer），MinionService 移除两处跟随调度；新增 `minionMovementStaysClientAuthored` 门禁。
- **validation**：MinionKnownListTest 3/3 + FlyTeleportMinionSuspendGateTest 3/3（IDEA MCP）；实机通过（高速/风之路无瞬拉无刷屏，正常速度跟随与宠物一致）。
