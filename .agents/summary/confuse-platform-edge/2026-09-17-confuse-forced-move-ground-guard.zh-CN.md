# 混沌（Confuse）把玩家推出平台边缘后在"空中—回位"之间循环

- 记录时间：2026-09-17（用户实机描述）
- 分支：`quest`（未提交，工作树含并行改动）
- 状态：源码头 + 编译/定向测试通过（`mvn -q -DskipTests compile` 成功；
  `mvn -Dtest=SkillCancellationTest,SkillConfigTest,StatFunctionsTest test` → 27/27）；
  **真机验收待做**（该改动无直接单元测试覆盖，靠静态链路 + 编译保证）

## 现象（用户原话）

> 混沌状态下，如果是在平台上，可能会跑出平台边缘在空中，过一会瞬间回到起点又找一个方向跑出去然后回到起点反复直到混沌结束。

## 根因链（全部来自当前分支源码）

1. `ConfuseEffect.ConfuseTask` 每秒执行一次：随机取角度 → `目标点 = 当前坐标 + 速度向量` →
   `GeoService.getClosestCollision(effected, targetX, targetY, effected.getZ(), true, PHYSICAL|DOOR)`。
2. `GeoMap.getClosestCollision` **只判碰撞**。没有碰撞点时返回目标点本身，仅当目标点下方 2m 内
   （`getZ(x, y, z + 1, z - 2, instanceId)`）能采到地面才把 z 贴地。平台边缘之外没有地面 ⇒
   返回的点保持角色当前 z，等于一个"悬空的点"。
3. `PathService.canMoveStraight` 拦不住这种情况：非飞行/非水域走 `canPassWalker`，本质是两点射线
   碰撞测试；射线在悬崖外的空气中不会命中任何碰撞体，所以判定为"可通行"。
4. `PlayableMoveController.moveToDestination`（`PlayerMoveTaskManager` 每 200ms 驱动一次）对目标点做
   直线插值、Z 线性过渡，**没有地面/可站立校验**，于是服务器把角色坐标推进到平台外的空中并广播 `SM_MOVE`。
5. 客户端不接受"无地面"的强制位移，把角色拉回原位（= 这一轮移动的起点），下一个 tick 又随机换方向
   ⇒ "跑出去 → 瞬间回位"循环，直到 debuff 结束。

### 排除的候选

- `World.updatePosition` 的区域为空分支（I18n `log.b6b1d45cbd0f` = "检查点：新区域为空…"，会对玩家
  `setPosition` 到绑定点）：`log/*.log`（覆盖 2026-09-17 17:55–18:07 本次会话）中 `检查点` 命中数为 **0**，
  且该分支会打 WARN + 抛栈，本次报障无此痕迹 ⇒ 回位不是服务端绑定传送造成的。
- "之前修过又丢了"的历史修复：`origin/history` 与 `quest` 的 `ConfuseEffect.java` / `PlayableMoveController.java`
  差异**只有注释与 record 写法**（`git log HEAD..origin/history -- <这两个文件>` 为空）。
  更早的混乱修复 `6e649e831`（`EffectController` 过滤 `!effect.isStopped()`、`CM_MOVE` 早返回
  `isUnderFear() || isConfused()`）在本分支**已存在**（`EffectController.java:1030/1047`、`CM_MOVE.java:97`），
  它只解决了"客户端驱动的位移/异常回显"，从未覆盖平台边缘这一路径。

## 修复（3 个文件，源码头）

1. `PathService.hasStandableGround(Creature, float, float, float)`（新增，含私有 `groundProbeZ`）：
   - GEO 关闭、飞行/水域（`usesSpatialPath`）⇒ 直接返回 `true`（保持原行为）；
   - **起点自身**在同源窄带里采不到地面（哑地图、悬空）⇒ 返回 `true`（无从判定，不做拦截）；
   - 否则用与 `GeoMap.getClosestCollision` 贴地**同源**的窄带 `[z-2, z+1]` 向下探测目标点，命中即有地面。
2. `ConfuseEffect.ConfuseTask`：随机方向最多重试 8 次（`MAX_DIRECTION_ATTEMPTS`），取第一个"目标点可站立"
   的方向；全部失败则本 tick 原地不动，下一秒再选，不再把角色推向空中。
3. `PlayableMoveController.moveToDestination`：处于失控位移（`isControlled()`：恐惧/混沌）时，插值出的
   **下一步**若没有地面，则 `abortMove()` 停在边缘，由周期任务下一秒重选方向。该防线同时覆盖恐惧
   （`FearEffect.FearTask` 走同一条 `setNewDirection` + `startMovingToDestination` 路径）与"直线穿过缺口"的情形。

## 边界与风险

- 飞行、滑翔（混沌 `applyEffect` 会先 `onStopGliding`）、游泳/水域、`GEO_ENABLE=false`、无 geo 数据的哑地图：
  均不受影响（前置短路）。
- 平台边缘的小落差（≤2m，与引擎贴地带同源）仍允许迈步，不会被误判为悬空。
- 客户端驱动的 `CM_MOVE` 不经过该判定；`moveToDestination` 只服务服务器侧强制位移
  （`PlayerMoveController`、`SummonMoveController`，且仅在 `isControlled()` 时生效）。
- NPC 分支（`NpcMoveController.moveToPoint`）同样先过地面校验：NPC 最多原地停一个 tick，而不是跑向空中。

## 已执行

```bash
mvn -q -DskipTests compile                       # 成功（工作树同时含并行改动）
mvn -Dtest=SkillCancellationTest,SkillConfigTest,StatFunctionsTest test   # 27/27 通过
```

未执行的边界：全量测试套件、`clean package`、部署、重启、真机验收。

## 实机验收建议

1. 在平台/高台（如要塞平台、副本台阶）上被施加混沌，观察角色是否只在可站立范围内换向。
2. 若仍出现"瞬间回位"，请给出**报障时间戳**，用于检索 `log/console.log` 中的
   `检查点：新区域为空`（`log.b6b1d45cbd0f`，会带 `setPosition` 到绑定点的兜底）与其前后的 `SM_MOVE`。
