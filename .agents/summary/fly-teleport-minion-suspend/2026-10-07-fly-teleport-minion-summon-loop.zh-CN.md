# 飞行传送期间守护灵「召唤了/取消召唤」消息刷屏：进入收起 + 落地恢复

```text
report: 飞行传送过程中，若已召唤 minion，客户端聊天窗不停交替出现「召唤了宠物精灵XXX」「取消召唤宠物精灵XXX」
status: ACCEPTED（2026-10-07 用户实机验证通过）+ IDE 静态检查 0 error
changed: MinionService、TeleportService2、PlayerController、PlayerQuestEffectPort、7 个飞行传送 AI（DF6/LF6/LF5/DF5/Plegeton_Boatman/IDAb1_Heroes_Teleporter/Suspicious_Cannon）、新增 FlyTeleportMinionSuspendGateTest
working tree: dirty（18602 等并行改动严格保留未动）
```

## 1. 报障与现象

实机截图（22:02）：聊天窗约每 4~5 秒一对交替消息「取消召唤宠物精灵Grendal。」「召唤了宠物精灵Grendal。」，
仅在**飞行传送过程中**出现；地面跑动、站立时无此现象。截图中间夹一条「翅膀的传阅任务信息已更新」。

## 2. 根因链（诊断，静态证据）

| # | 环节 | 证据 |
| --- | --- | --- |
| E1 | 飞行传送期间客户端持续上报位置：`CM_MOVE_IN_AIR` 在 `FLIGHT_TELEPORT` 状态下每个包都执行 `world().updatePosition(player, x, y, z, 0)`，该重载默认 `updateKnownList=true` → `player.updateKnownlist()` | `clientpackets/CM_MOVE_IN_AIR.java:53-60`；`world/World.java:385-387` |
| E2 | 玩家 KnownList 按 **95m** 可见距离遗忘对象：minion 超出即 `del` → `PlayerController.notSee(Minion)` → `SM_MINIONS(6)`（收回家）→ 客户端「取消召唤」 | `world/knownlist/KnownList.java:213-220`、`VisibleObject.VisibilityDistance=95`（`VisibleObject.java:34`）；`controllers/PlayerController.java:168-169` |
| E3 | 守护灵跟随任务每 1s/2s tick，距离 > 25m 即 `teleportToPlayer`；瞬移后 minion 侧 `doUpdate` 重新建立双向已知 → 玩家 `see(Minion)` → `SM_MINIONS(5)`（召唤）→ 客户端「召唤了」 | `controllers/MinionController.java:63-75,94-108,148-153`；`PlayerController.java:147-148` |
| E4 | 循环周期 ≈ 95m ÷ 飞行速度 + tick 延迟；飞行 ~20m/s 时约 4.7s，与截图节奏吻合。地面速度远低于 95m/s，永远碰不到边界，故只在飞行传送出现 | 推算 + 截图节奏 |
| E5 | `SM_MINIONS` action 语义：5=召唤、6=收回，与两条客户端消息一一对应 | `serverpackets/SM_MINIONS.java` 类头注释 |

补充边界：**Summon（召唤兽）不受影响**——它用 `CreatureAwareKnownList`（超距解散是刻意的真端语义）；
**windstream（风之路）不进入该路径**——`CM_WINDSTREAM` 不设置 `FLIGHT_TELEPORT` 状态（`PlayerActions.setPlayerMode` 只写 `windstreamPath`）。

## 3. 修复（按用户裁定：飞行开始时解除、结束后自动召唤）

与召唤兽的 `SummonsService.suspendForTeleport/restoreAfterTeleport` 完全对称：**世界层隐藏而非收回**，
保留主人关系、授予技能、增益与自动拾取/自动 Buff 功能开关（不做 `despawnMinion` 的技能/增益清理）。

| 文件 | 变更 |
| --- | --- |
| `services/toypet/MinionService.java` | 新增 `suspendForFlyTeleport(Player)`（`isSpawned` 守卫 + `world().despawn(minion)`，加 `synchronized (minion)`）与 `restoreAfterFlyTeleport(Player)`（`!isSpawned` 守卫 + `setPosition` 到主人 + `world().spawn(minion)`）；新增 `GameWorldBootstrapServices` import |
| `controllers/PlayerController.java` | `onFlyTeleportEnd()` 非 windstream 分支：紧随 `SummonsService.restoreAfterTeleport(player)` 之后调用 `restoreAfterFlyTeleport`（`updateZone()` 之后，顺序与召唤兽对齐） |
| `services/teleport/TeleportService2.java` | 传送门 FLIGHT 分支：紧随 `SummonsService.suspendForTeleport(player)` 之后调用 suspend |
| `questEngine/runtime/PlayerQuestEffectPort.java` | `flightTeleport(...)` 任务侧入口：`setFlightTeleportId` 之后调用 suspend |
| `ai/worlds/{norsvold/DF6,iluma/LF6,cygnea/LF5,enshar/DF5}_FieldAttractionAI2.java` | `dialogId == 10000` 分支内（switch 前）调用 suspend——该分支全部 case 均为飞行传送 |
| `ai/instance/beshmundirTemple/Plegeton_BoatmanAI2.java` | 同上 |
| `ai/instance/IDAb1_Heroes/IDAb1_Heroes_TeleporterAI2.java` | 同上（位于任务引擎 `onDialog` 认领判断之后） |
| `ai/instance/steelRake/Suspicious_CannonAI2.java` | 同上 |

效果：传送开始 → 客户端一条「取消召唤宠物精灵」（`SM_MINIONS(6)`）；落地 → 一条「召唤了宠物精灵」（`SM_MINIONS(5)`）；传送期间不再有任何循环消息，守护灵位置随 `restore` 落在主人落点。

## 4. 覆盖矩阵（全仓 `.setState(CreatureState.FLIGHT_TELEPORT)` 入口 9 文件，全部已挂 suspend）

TeleportService2（传送门）、DF6/LF6/LF5/DF5（风场）、Plegeton_Boatman、IDAb1_Heroes_Teleporter、Suspicious_Cannon（副本）、PlayerQuestEffectPort（任务）。
唯一含 `unsetState(CreatureState.FLIGHT_TELEPORT)` 的 `PlayerController` 不在入口集（恢复点）。

## 5. 门禁测试（新增）

`src/test/java/com/aionemu/gameserver/services/toypet/FlyTeleportMinionSuspendGateTest.java`（3 用例）：

1. `everyFlyTeleportEntrySuspendsTheMinion`：全仓扫描含 `.setState(CreatureState.FLIGHT_TELEPORT)` 的文件，断言每个都含 `suspendForFlyTeleport(player)`（防将来新增入口遗漏；点号前缀精确匹配，避免误匹配 `unsetState`）；
2. `landingRestoresTheMinionAfterTheSummon`：`PlayerController` 中守护灵恢复必须位于 `SummonsService.restoreAfterTeleport` 之后；
3. `suspendHidesTheMinionWithoutReleasingIt`：suspend 用 `world().despawn(minion)` 且不得 `setMinion(null)`；restore 用 `setPosition` + `spawn`。

## 6. 验证状态

- IDE inspections：12 个改动/新增文件 0 error（剩余 warning 均为既有代码风格项）；
- 新增行 ≤120 字符（按字符计），双语注释、`I18n` 未新增日志键；
- **实机验证通过（2026-10-07，用户确认）**：飞行传送无「取消召唤/召唤了」循环消息，落地守护灵恢复；
- 门禁测试 `FlyTeleportMinionSuspendGateTest`（1 类 3 例）已编写并随本次提交；
- 未跑构建/测试（项目规则），冷重启已由用户侧完成。

## 7. 边界与未覆盖

- **windstream（风之路）**：不设置 `FLIGHT_TELEPORT`，未纳入 suspend/restore；若实机在风之路复现同类刷屏，按同一对方法扩展到 `CM_WINDSTREAM` 进入/退出点；
- **Pet（玩具宠物）**：同用 `PlayerAwareKnownList` 但无服务端跟随瞬移路径，本次未纳入（保持最小改动）；
- **普通传送（非 FLIGHT）**：`TeleportService2.teleportTo/changePosition` 原有 minion 位置同步逻辑不变；
- 飞行传送中断线：`PlayerLeaveWorldService` 走完整 `despawnMinion`（含 `setMinion(null)`），登录自动重召路径不受影响；进图自愈 `CM_LEVEL_READY`（`!minion.isSpawned() → world().spawn`）保留。
