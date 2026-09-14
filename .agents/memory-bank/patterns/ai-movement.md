# AI Selection & NPC Movement Patterns (AI 选型与 NPC 移动模式)

本文档记录脚本 AI 选型、跟随/护送行为与移动控制器在非凸几何下的实战避坑经验。

> Pattern IDs: `AIM-001`–`AIM-003`
> card_status: ACTIVE; movement conclusions are tied to the observed geometry and path-data availability
> scope: AI2Engine selection, follow/escort handlers, and NpcMoveController pathing
> last_reviewed: 2026-09-14

---

## [AIM-001] 一、Retail Pattern 覆盖脚本 AI 导致跟随失灵
<!-- pattern-metadata
status: CONFIRMED
scope: AI2Engine NPC AI selection for script-protocol follow and delivery NPCs
first_seen: 2026-09-14
last_verified: 2026-09-14
symptom: 护送 NPC 对话后原地不动、跟随不启动、FOLLOW_ME 被静默丢弃、npc-lost-target 回退
root_cause: selectNpcAi omitted following/deliveryman from the script-protection whitelist so a retail pattern replaced the follow AI
fix_or_guardrail: Short-circuit the script-protocol AI names in AI2Engine.selectNpcAi before retail pattern selection
evidence: commit 97fcba667; src/main/java/com/aionemu/gameserver/ai2/AI2Engine.java:136; src/main/java/com/aionemu/gameserver/ai/RetailPatternAI2.java; src/test/java/com/aionemu/gameserver/ai2/AI2EngineRetailSelectionTest.java:79; docs/movement/escort-follow-movement-repair.md:15
validation: static; focused-test; runtime/client validation not implied
boundaries: The whitelist only protects AIs that own their interaction or follow protocol; ordinary NPCs must still fall through to retail pattern selection
superseded_by: none
first_check: AI2Engine.selectNpcAi whitelist, npc-ai.xml pattern and FollowEventHandler FOLLOW_ME
-->
- **现象**：任务 14042 / 魔族镜像 24042 的俘虏 NPC（253623 / 253626）在对话推进到 `SETPRO4` 后停在出生点完全不跟随；玩家离开超过 50 米后任务因 `npc-lost-target` 回退到步骤 3。
- **根因链**：
  1. NPC 模板配置了原生 `ai="following"`（`FollowingNpcAI2`，自带跟随、移动监听与距离检测）；
  2. 但 `npc-ai.xml` 为其配置了模式 `AD2_Prisoner`，而 `AI2Engine.selectNpcAi` 未把 `"following"` / `"deliveryman"` 纳入脚本保护白名单；
  3. 实例因此被覆盖为 `RetailPatternAI2`。该类继承 `AggressiveNpcAI2`，基类未重写 `handleFollowMe`（空实现）；
  4. 任务引擎经 `start-follow-current-target-npc` 发出的 `AIEventType.FOLLOW_ME` 被静默丢弃，NPC 从未进入 `AIState.FOLLOWING`，移动控制器也从未启动。
- **排查与修复规范**：
  - 症状是"跟随类行为整体失效"而非"寻路走错"时，先确认最终生效的 AI 类，而不是直接排查移动控制器。
  - 凡**自身拥有交互或跟随协议**的脚本 AI，必须在 `selectNpcAi` 中先于 retail pattern 短路返回。当前白名单：`quest_use_item`、`quest_start_use_item`、`empyrean_blessing`、`following`、`deliveryman`。
  - 普通 NPC 仍应正常落入 `RetailPatternAI2.supports(...)` 选择；扩白名单时不得把 retail pattern 整体旁路。

---

## [AIM-002] 二、跟随停步半径与战斗站位槽冲突（无限转圈）
<!-- pattern-metadata
status: CONFIRMED
scope: Follow-state destination selection and stop range in NpcMoveController
first_seen: 2026-09-14
last_verified: 2026-09-14
symptom: 跟随 NPC 在玩家身边无限转圈、停不下来、到达条件永不满足、卡死恢复角度轮转
root_cause: Attack-slot assignment ignored AI state and parked the NPC outside the follow arrival radius, so arrival never registered and stuck recovery rotated it forever
fix_or_guardrail: Exclude attack slots while following and align the stop offset with CLOSE_FOLLOW_RANGE on both producer and consumer sides
evidence: commit 97fcba667; src/main/java/com/aionemu/gameserver/controllers/movement/NpcMoveController.java:383; src/main/java/com/aionemu/gameserver/ai2/handler/FollowEventHandler.java:21; src/test/java/com/aionemu/gameserver/controllers/movement/NpcMoveControllerPathTest.java; docs/movement/escort-follow-movement-repair.md:20
validation: static; focused-test; runtime/client validation not implied
boundaries: The stop radius must stay single-sourced; removing the legacy instance or low-HP branches requires confirming no caller depends on them
superseded_by: none
first_check: shouldUseAttackSlot, CLOSE_FOLLOW_RANGE and refreshAttackSlotForRecovery
-->
- **现象**：修复启动跟随之后，玩家一停下脚步，NPC 就以玩家为圆心持续做圆周运动（"一直转圈圈"），永远不进入平稳停步。
- **根因链**（一个数值不一致被卡死恢复放大成无限循环）：
  1. 俘虏 NPC 原生模板配置 `attack_range="4"`；
  2. `NpcMoveController.updateTargetDestination` 中 `shouldUseAttackSlot` **仅按攻击距离**（`0.75f < attackDistance <= TARGET_SLOT_MAX_ATTACK_RANGE`）判断，未检查 AI 状态，把跟随中的 NPC 误当作战斗怪分配了 `findAttackSlot`，目标点落在玩家周围 **3.75 米**；
  3. 但 `FollowEventHandler.isInRange` 与 `AbstractAI.isDestinationReached` 的到达判定是 `CLOSE_FOLLOW_RANGE = 3.0` 米；
  4. `3.75 > 3.0`，到达条件**永不成立**，移动任务不注销；停在 3.75 米后被 `sampleStuckShadow` 判为卡死，进而触发 `refreshAttackSlotForRecovery` 按 `[0, 20, -20, 40, -40]` 度不断旋转重寻路 → 无限转圈。
- **历史遗留放大项**：`FollowEventHandler.isInRange` 曾残留 `object.isInInstance() ? 9999 : (hpPercentage < 100 ? 30 : 3)`，在副本内与残血时把停步距离成倍放大，掩盖了真实的 3.0 米契约，已清理。
- **排查与修复规范**：
  - **产消两侧半径必须同源**。目标点生产端（移动控制器 offset）与到达判定消费端（handler 判定）只要出现任意差值，就会退化为"永不抵达 + 卡死恢复"死循环；把停步偏移统一对齐到 `FollowEventHandler.CLOSE_FOLLOW_RANGE`。
  - 行为性目标点选择不能只看距离阈值，**必须同时纳入 AI 状态**。跟随态下 `shouldUseAttackSlot` 恒为 `false`，彻底禁用战斗圆周站位槽与角度轮转。
  - 在 `moveToDestination` 入口增加主动刹车：处于 `FOLLOWING` 且与目标 3D 距离 `<= CLOSE_FOLLOW_RANGE` 时立即 `abortMove()` 并广播停止移动包。

---

## [AIM-003] 三、非凸几何下的跟随寻路：足迹队列与脱困拉回
<!-- pattern-metadata
status: CONFIRMED
scope: Follow pathing through indoor non-convex geometry with missing path grids
first_seen: 2026-09-14
last_verified: 2026-09-14
symptom: 拐角与门廊卡墙、贴墙无法脱困、NPC 切墙穿模、护送任务因模型死角超时失败
root_cause: Straight-line interpolation has no horizontal collision and the indoor A-star fallback aims at the first raycast hit
fix_or_guardrail: Follow the player breadcrumb trail while line of sight is blocked and keep catch-up teleport as a bounded safety net
evidence: commit 7aab414d8; src/main/java/com/aionemu/gameserver/controllers/movement/NpcMoveController.java:92; src/test/java/com/aionemu/gameserver/controllers/movement/NpcMoveControllerPathTest.java:56; docs/movement/escort-follow-movement-repair.md:27
validation: static; focused-test; runtime/client validation not implied
boundaries: Trail points only ever follow a route the player has physically walked; catch-up teleport is a fallback and must not replace normal pathing
superseded_by: none
first_check: followTrail sampling, canPassDirectly and catchupTeleportTo
-->
- **现象**：在地下牢房、走廊 90 度直角转弯等复杂构件区域，NPC 直接切入墙体内部或贴在墙面上无法脱困。
- **根因链**：
  1. 移动控制器默认采用直线欧几里得插值（`distFraction`），逐帧位置更新**只做地形 Z 轴贴地，缺乏水平 X/Y 胶囊体防穿墙碰撞**；
  2. 室内缺失连通 `.path` 网格时，A* 回退至 `geoGroundPath` 取**第一个射线撞击点**，NPC 于是径直朝墙面碰撞点移动并卡死在墙上。
- **修复方案（玩家历史足迹队列 / Breadcrumbs Trail）**：
  - **采样**：FIFO 队列 `followTrail`，玩家位移每累计 `TRAIL_STEP_DISTANCE = 2.0` 米记录一个足迹点，容量上限 20 点（`TRAIL_MAX_POINTS`）。
  - **循迹**：当 NPC 与玩家视线被墙壁、门廊阻隔（`!canPassDirectly`）时，目标点指向队列头部的历史足迹点，沿玩家**踩过**的安全路线走出牢房与直角弯。
  - **消费与直达优化**：NPC 到达当前足迹点 1.2 米内即出队并切换下一点；一旦视线完全开阔（`canPassDirectly` 为 true）**立即清空足迹直达玩家**，避免无谓绕行。
- **脱困拉回兜底安全网（Catch-up Teleport）**：
  - 触发条件：与跟随目标距离拉大至 30 米且视线阻隔（`tryFollowCatchupTeleport`），或复杂死角中连续 2 次卡死恢复（`tryStuckRecovery`）仍无法推进；
  - 动作：`catchupTeleportTo(target)` 将 NPC 拉回玩家身边并广播 `SM_MOVE` 瞬移同步包，防止任务因模型死角超时失败。
- **排查与修复规范**：
  - 足迹点**只能来自玩家实际走过的路线**，不得用插值或几何投影生成，否则会制造新的穿墙路径。
  - 拉回是**兜底安全网而非寻路替代**：正常路线可用时不应触发；新增室内场景时要先确认该区域的 `.path` 网格连通性与视线判定是否可靠。
