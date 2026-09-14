# NPC 跟随、护送与寻路移动机制修复记录

日期：2026-09-14

## 问题现象

1. **对话后原地不动**：在任务 14042（拯救俘虏作战，A Rescue Operation）中，玩家在硫磺要塞地下牢房与天族俘虏（NPC 253623）对话完毕推进至步骤 4（`SETPRO4`）后，俘虏 NPC 始终停留在出生点，完全不跟随玩家移动。玩家离开超过 50 米后，任务因 `npc-lost-target` 回退至步骤 3。魔族镜像任务 24042（NPC 253626）存在相同问题。
2. **玩家停下后 NPC 无限转圈**：在修复启动跟随之后，当玩家停下脚步时，NPC 跟随到玩家附近不会平稳停下，而是以玩家为圆心不断做圆周旋转运动（“一直转圈圈”）。
3. **拐弯与门廊处卡墙切角**：在地下牢房、走廊 90 度直角转弯等复杂构件区域，由于缺少细分 NavMesh 网格，NPC 实时向玩家当前坐标做三维直线插值，导致移动时直接切入墙体或贴在墙面上无法脱困。

---

## 根因分析

### 1. 脚本 AI 被 RetailPatternAI2 错误覆盖
- NPC 253623 和 253626 在 NPC 模板中配置了原生 `ai="following"`（即 `FollowingNpcAI2`，实现跟随、移动监听与距离检测）。
- 但在 `npc-ai.xml` 中配置了模式 `AD2_Prisoner`，`AI2Engine.selectNpcAi` 未将 `"following"` 和 `"deliveryman"` 纳入脚本保护白名单，导致生成的 NPC 实例被覆盖为 `RetailPatternAI2`。
- `RetailPatternAI2` 继承自 `AggressiveNpcAI2`，基类未重写 `handleFollowMe`（空实现），任务引擎通过 `start-follow-current-target-npc` 发送的 `AIEventType.FOLLOW_ME` 被静默丢弃，NPC 从未切换到 `AIState.FOLLOWING` 状态，也未启动移动控制器。

### 2. 跟随状态误分配战斗站位槽与卡死恢复轮转
- 俘虏 NPC 的原生模板配置了 `attack_range="4"`。
- 在 `NpcMoveController.updateTargetDestination` 中，`shouldUseAttackSlot` 仅根据攻击距离（`0.75f < attackDistance <= 4.0f`）判断，未检查 AI 状态，将跟随状态下的 NPC 误当作战斗怪分配了 `findAttackSlot`，目标点定在玩家周围半径 3.75 米处。
- 但 `FollowEventHandler.isInRange` 与 `AbstractAI.isDestinationReached` 判定的到达距离为 `CLOSE_FOLLOW_RANGE = 3.0` 米。
- 3.75 米大于 3.0 米，NPC 永远无法满足到达条件，移动任务不会注销。停在 3.75 米处后被 `sampleStuckShadow` 判定为卡死，进而调用 `refreshAttackSlotForRecovery` 将站位角度按 `[0, 20, -20, 40, -40]` 度不断旋转重寻路，造成 NPC 在玩家身边持续无限转圈。
- 同时 `FollowEventHandler.isInRange` 遗留了 `object.isInInstance() ? 9999 : (hpPercentage < 100 ? 30 : 3)` 的历史代码，在残血和副本环境下极度放大了跟随停步距离。

### 3. 实时直线连线模型与非凸几何体的冲突
- 移动控制器默认采用直线欧几里得插值（`distFraction`），逐帧位置更新只做地形 Z 轴贴地，缺乏水平 X/Y 胶囊体防穿墙碰撞。
- 在深渊要塞牢房等室内复杂区域，当玩家走出牢门、拐过走廊 90 度直角弯时，NPC 实时直连玩家会直接切过墙体内部；且当室内缺失连通 `.path` 网格时，A* 回退至 `geoGroundPath` 取第一个射线撞击点，导致 NPC 径直朝墙面碰撞点移动并卡死在墙上。

---

## 修复方案

### 1. 保护跟随类脚本 AI 不被 Retail Pattern 覆盖
在 [AI2Engine.java](../../src/main/java/com/aionemu/gameserver/ai2/AI2Engine.java) 的 `selectNpcAi` 中增加白名单判断：
```java
if ("quest_use_item".equals(fallback) || "quest_start_use_item".equals(fallback)
    || "empyrean_blessing".equals(fallback) || "following".equals(fallback)
    || "deliveryman".equals(fallback)) {
    return fallback;
}
```
确保护送与送信类 NPC 始终保持其脚本 AI 协议。

### 2. 跟随状态排除战斗站位并统一贴身停步
- 在 [NpcMoveController.java](../../src/main/java/com/aionemu/gameserver/controllers/movement/NpcMoveController.java) 中增加 `shouldUseAttackSlot(spatialPath, attackDistance, following)`，跟随状态下恒为 `false`，彻底禁用战斗圆周站位槽与卡死角度轮转。
- 跟随目标点直接对准玩家坐标，停止偏移 `offset` 统一对齐为 `FollowEventHandler.CLOSE_FOLLOW_RANGE`（3.0 米）。
- 在 `moveToDestination` 入口处增加主动刹车检查：当处于 `FOLLOWING` 且与跟随目标 3D 距离小于等于 3.0 米时，立即执行 `abortMove()` 广播停止移动包并返回。
- 在 [FollowEventHandler.java](../../src/main/java/com/aionemu/gameserver/ai2/handler/FollowEventHandler.java) 的 `isInRange` 中清理副本 9999 米与残血 30 米的历史遗留逻辑，统一使用 `CLOSE_FOLLOW_RANGE`（3.0 米）。

### 3. 工业级方案：玩家历史足迹队列（Breadcrumbs Trail）
在 [NpcMoveController.java](../../src/main/java/com/aionemu/gameserver/controllers/movement/NpcMoveController.java) 中实现足迹追踪：
1. **采样**：维护 FIFO 队列 `followTrail`，玩家位移每累计达到 `TRAIL_STEP_DISTANCE = 2.0` 米时记录一个足迹点（容量上限 20 点）。
2. **循迹**：当 NPC 与玩家视线被墙壁、门廊阻隔（`!canPassDirectly`）时，NPC 目标点指向队列头部的历史足迹点，沿着玩家踩过的安全路线走出牢房和直角弯。
3. **消费与直达优化**：NPC 到达当前足迹点 1.2 米内时出队该点并切换下一个；一旦转过拐角视线完全开阔（`canPassDirectly` 为 true），立即清空足迹直达玩家。

### 4. 脱困拉回兜底安全网（Catch-up Teleport）
- 当 NPC 与跟随目标距离拉大至 30 米且视线阻隔（`tryFollowCatchupTeleport`）；
- 或 NPC 在复杂死角连续 2 次卡死恢复（`tryStuckRecovery`）依然无法推进时；
调用 `catchupTeleportTo(target)` 将 NPC 安全拉回到玩家身边，广播 `SM_MOVE` 瞬移同步包，彻底防止任务因模型死角超时失败。

---

## 涉及文件与提交记录

| 提交哈希 | 提交说明 | 涉及文件 |
| --- | --- | --- |
| `97fcba667` | `fix(quest): fix escort follow AI selection and circling near player` | `AI2Engine.java`<br>`FollowingNpcAI2.java`<br>`AbstractAI.java`<br>`FollowEventHandler.java`<br>`NpcMoveController.java`<br>`AI2EngineRetailSelectionTest.java`<br>`FollowManagerTest.java`<br>`NpcMoveControllerPathTest.java`<br>`Quest14042RetailAlignmentTest.java` |
| `7aab414d8` | `feat(movement): implement breadcrumb trail and catch-up safety for follow NPCs` | `NpcMoveController.java`<br>`NpcMoveControllerPathTest.java` |

---

## 自动化测试验证

- **AI 路由选择测试**：`AI2EngineRetailSelectionTest` 验证 `following` / `deliveryman` 不被 `retail_pattern` 覆盖，锁定 NPC 253623 与 253626。
- **任务契约对齐测试**：`Quest14042RetailAlignmentTest` 验证任务解析后俘虏 NPC 保持跟随 AI。
- **跟随启动与停步测试**：`FollowManagerTest` 验证 `FOLLOW_ME` 与 `CREATURE_MOVED` 正确响应，普通地图与副本实例中均在 3 米处停步。
- **移动控制器路径与足迹测试**：`NpcMoveControllerPathTest` 验证跟随状态下 `shouldUseAttackSlot` 为 `false`，足迹队列采样门槛累积与路点消费出队正常。
- **静态代码检查**：IDEA Inspections 检查通过，0 错误；`git diff --check` 检查通过。
