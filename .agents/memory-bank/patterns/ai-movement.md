# AI Selection & NPC Movement Patterns (AI 选型与 NPC 移动模式)

本文档记录脚本 AI 选型、跟随/护送行为与移动控制器在非凸几何下的实战避坑经验。

> Pattern IDs: `AIM-001`–`AIM-007`
> card_status: ACTIVE; movement conclusions are tied to the observed geometry and path-data availability
> scope: AI2Engine selection, follow/escort handlers, NpcMoveController pathing, and AI2 attack-event re-entry
> last_reviewed: 2026-09-19

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
last_verified: 2026-09-17
symptom: 拐角与门廊卡墙、贴墙无法脱困、NPC 切墙穿模、护送任务因模型死角超时失败、145885 onTargetTooFar 刷屏
root_cause: canPassDirectly cleared breadcrumbs on line of sight through bars/doors, stopForPath froze follower, stuck recovery was dead code, and MOVE_VALIDATE spammed targetTooFar
fix_or_guardrail: Follow the player breadcrumb trail while line of sight is blocked and keep catch-up teleport as a bounded safety net
evidence: commit 7aab414d8; src/main/java/com/aionemu/gameserver/controllers/movement/NpcMoveController.java:92; src/test/java/com/aionemu/gameserver/controllers/movement/NpcMoveControllerPathTest.java:56; docs/movement/escort-follow-movement-repair.md:27
validation: static; focused-test; runtime/client validation not implied
boundaries: Trail points only ever follow a route the player has physically walked; catch-up teleport is a fallback and must not replace normal pathing
superseded_by: none
first_check: followTrail preservation, stopForPath fallback, stuckShadowConfirmed teleport, and MoveEventHandler onMoveValidate
-->
- **现象**：在地下牢房、走廊 90 度直角转弯等复杂构件区域，NPC 直接切入墙体内部或贴在墙面上无法脱困。
- **根因链**：
  1. 移动控制器默认采用直线欧几里得插值（`distFraction`），逐帧位置更新**只做地形 Z 轴贴地，缺乏水平 X/Y 胶囊体防穿墙碰撞**；
  2. 室内缺失连通 `.path` 网格时，A* 回退至 `geoGroundPath` 取**第一个射线撞击点**，NPC 于是径直朝墙面碰撞点移动并卡死在墙上。
- **修复方案（玩家历史足迹队列 / Breadcrumbs Trail）**：
  - **采样**：FIFO 队列 `followTrail`，玩家位移每累计 `TRAIL_STEP_DISTANCE = 2.0` 米记录一个足迹点，容量上限 20 点（`TRAIL_MAX_POINTS`）。
  - **循迹与足迹保护**：严禁在视线开阔（`canPassDirectly`）时清空足迹！牢房铁栏、门廊与矮墙均可被射线穿透，清空足迹会导致转弯时丢失门洞路点而切墙卡死。足迹队列全程保留，NPC 到达当前足迹点 1.2 米内或更靠近下一点时出队，仅在贴身停步（`CLOSE_FOLLOW_RANGE = 3.0` 米）时清空。
  - **无网格区域防定身**：在深渊牢房等无 Path 网格区域，跟随移动严禁执行 `stopForPath()`，无网格或寻路为空时直接回退朝向足迹点的 `moveToLocation`。
- **脱困拉回兜底安全网（Catch-up Teleport）**：
  - 触发条件：与跟随目标距离拉大至 20 米且视线阻隔、或达到 35 米绝对超距门槛（防止 50 米任务失败）；在复杂死角中卡死确认（`stuckShadowConfirmed`）且处于 `FOLLOWING` 状态时，立即调用 `catchupTeleportTo`，消除重试上限判断之后的死代码。
  - 动作：`catchupTeleportTo(target)` 将 NPC 拉回玩家身边并广播 `SM_MOVE` 瞬移同步包，防止任务因模型死角超时失败。
- **移动校验与过远事件解耦**：
  - `MoveEventHandler.onMoveValidate` 为常规 100ms 移动步进校验，跟随状态下跳过 `TargetEventHandler.onTargetTooFar`。
  - `FollowManager.targetTooFar` 在控制器已在向目标移动（`isMovingToTarget()`）时短路返回，根除 `addCreature` 重置 `nextUpdateAt = 0` 导致的 0ms 递归死循环与控制台日志刷屏。
- **排查与修复规范**：
  - 足迹点**只能来自玩家实际走过的路线**，不得用插值或几何投影生成，否则会制造新的穿墙路径。
  - 拉回是**兜底安全网而非寻路替代**：正常路线可用时不应触发；新增室内场景时要先确认该区域的 `.path` 网格连通性与视线判定是否可靠。

---

## [AIM-004] 四、不可移动 NPC 不因“够不着”放弃目标 (IMMOBILE_NPC_KEEPS_UNREACHABLE_TARGET)
<!-- pattern-metadata
status: CONFIRMED
scope: AttackManager#targetTooFar 与 0 移速 NPC（卵、固定炮台、结构物）的战斗状态机
first_seen: 2026-09-16
last_verified: 2026-09-16
symptom: 卵/固定怪每次被攻击都“脱离战斗”，客户端反复播放脱战表现（Taloc's Hollow 的 mosqua egg 282006 一波多只时连续响）
root_cause: AttackManager#targetTooFar 里对不可移动 NPC 存在“有伤害就放弃目标”分支，清空仇恨后又被下一次受击或视野事件立刻拉回战斗，形成 FIGHT→IDLE→FIGHT 抖动
fix_or_guardrail: 不可移动 NPC 在此处不放弃目标；战斗只由目标离开已知列表（NpcController#notSee 移出仇恨）或真端 max_chase_time 规则结束
evidence: src/main/java/com/aionemu/gameserver/ai2/manager/AttackManager.java:145; src/main/java/com/aionemu/gameserver/ai2/handler/TargetEventHandler.java:105; src/main/java/com/aionemu/gameserver/ai2/handler/ThinkEventHandler.java:95; src/test/java/com/aionemu/gameserver/ai2/manager/AttackManagerTest.java:37
validation: focused-test（AttackManagerTest / AttackManagerLeashTest / TargetEventHandlerTest / RetailPatternAI2Test 等 95 例 0 失败，2026-09-16）；真端数据核对（58Server/Map/XML/npcs_std_monsters.xml 的 282006：0 移速、max_chase_time=0；NpcAIPatterns_IDElim_OSY.xml 的 Elim_NeutflyEgg：on_enter_attack_state=do_nothing）；客户端实机验证通过（2026-09-16，用户报告：卵不再反复脱战）
boundaries: 仅适用于不可移动 NPC；可移动 NPC 仍按 max_chase_time / react_to_pathfind_fail 处理。被删除的 shouldKeepTargetWhenImmobile / hasOffensiveSkill / isOffensiveSkill 若要恢复，必须先给出真端证据
superseded_by: none
first_check: AttackManager#targetTooFar 是否对 !isMoveSupported() 发送 TARGET_GIVEUP
-->

- **症状**：固定怪被远程攻击后每次攻击周期都“脱离战斗”一次；一波多只时客户端连续播放脱战表现与音效。
- **根因链**：
  1. 卵这类 NPC 是 0 移速（真端 `npcs_std_monsters.xml` 的 `move_speed_normal_run=0`），`NpcAI2#isMoveSupported()` 因速度为 0 返回 false；
  2. 目标够不着时 `AttackManager#targetTooFar` 原先走到“有伤害就放弃目标”分支（`shouldKeepTargetWhenImmobile` 语义与真端相悖）→ `TARGET_GIVEUP`；
  3. `TargetEventHandler#onTargetGiveup` 清仇恨并 `think()` → `ThinkEventHandler#thinkAttack` 无最高仇恨 → `BACK_HOME` → `ReturningEventHandler#onBackHome` 广播 `SM_EMOTION(NEUTRALMODE)`（客户端脱战表现）；
  4. 卵是 aggressive、感知 20m，且每次受击都会重新加仇恨 → 立刻回到 FIGHT，循环往复。
- **真端依据**：卵的 `max_chase_time=0`（不设追击超时）、0 移速不会产生寻路失败、pattern `Elim_NeutflyEgg` 在 `on_enter_attack_state` / `on_enter_idle_state` 都是 `do_nothing`——真端没有任何“够不着就清仇恨回位”的驱动。
- **修复与后续**：删除该分支后，攻击管理器只在“目标离开已知列表”或真端 `max_chase_time` 规则生效时结束战斗，固定怪不再抖动。若将来要为某些固定怪恢复“脱战”，必须先从真端数据找到驱动字段（例如该 NPC 的 `max_chase_time` 取值）而不是在代码里按伤害大小判断。

---

## [AIM-005] 五、受击回调内严禁同步重排攻击（必须异步去重）(HIT_CALLBACK_MUST_NOT_RESCHEDULE_ATTACK_SYNC)
<!-- pattern-metadata
status: CONFIRMED
scope: AI2 受击事件回调（AggroList#addDamageInternal → AbstractAI#onAttacked → AttackEventHandler）内的攻击链复位
first_seen: 2026-09-19
last_verified: 2026-09-19
symptom: NPC 停在 FIGHT 挂着仇恨不还手；为该症状加的“受击即重排攻击”修复上线后，线程池线程反复抛 java.lang.StackOverflowError（ExecuteWrapper 记录，栈循环 AttackEventHandler#onAttack → AttackManager#scheduleNextAttack → SimpleAttackManager#attackAction → CreatureController#attackTarget → 对方 AggroList#addDamageInternal）
root_cause: 受击回调直接同步调用 AttackManager#scheduleNextAttack；目标普攻间隔为 0（getNextAttackInterval 在攻击链停摆后返回 0）时立刻同步打回对方，对方的受击又进入同一回调，两生物来回递归直到爆栈
fix_or_guardrail: 受击栈内只允许异步复位：AttackEventHandler#onAttack 走 AttackManager#resumeInterruptedAttack，统一经 scheduleAttackRetry → 线程池 schedule（按 objectId 在 PENDING_ATTACK_RETRIES 去重），只有线程池任务体 runAttackRetry 可调用 scheduleNextAttack
evidence: src/main/java/com/aionemu/gameserver/ai2/handler/AttackEventHandler.java:54; src/main/java/com/aionemu/gameserver/ai2/manager/AttackManager.java:199; src/main/java/com/aionemu/gameserver/ai2/manager/AttackManager.java:224; src/main/java/com/aionemu/gameserver/ai2/manager/AttackManager.java:260; src/test/java/com/aionemu/gameserver/ai2/handler/AttackEventHandlerTest.java:70; src/test/java/com/aionemu/gameserver/ai2/manager/AttackManagerTest.java:59; src/test/java/com/aionemu/gameserver/ai2/manager/AttackManagerTest.java:88
validation: static（受击分支禁同步 scheduleNextAttack、重排必须线程池 + 去重、git diff --check）；focused-test（AttackEventHandlerTest/AttackManagerTest 等 9 类 104 例 0 失败，2026-09-19）；runtime（首版同步实现在真端日志复现 StackOverflowError，证据见 ExecuteWrapper 记录）；client/production 复验 PENDING（需重新部署服务端）
boundaries: 只约束会“回打调用方”的同步动作（攻击重排、强制移动等）；仅改自身状态、记日志、清 TARGET_LOST 子状态仍可同步。正常战斗链的重复重排由 isNextAttackScheduled 与去重表兜底
superseded_by: none
first_check: AttackEventHandler#onAttack 的受击分支是否直接调用 AttackManager#scheduleNextAttack（应为 resumeInterruptedAttack + 线程池去重）
-->

- **症状**：NPC 卡在 FIGHT 不还手（AIM-004 的“保留目标”语义不受影响）；修复上线后线程池日志出现成片 `java.lang.StackOverflowError`。
- **递归链**（约 12 帧一轮）：`AggroList#addDamageInternal` → `AbstractAI#onAttacked` → `RetailPatternAI2#handleAttack` → `AggressiveNpcAI2#handleAttack` → `AttackEventHandler#onAttack` → `AttackManager#scheduleNextAttack` → `chooseAttack` → `SimpleAttackManager#performAttack` → `attackAction` → `CreatureController#attackTarget` → 对方的 `AggroList#addDamageInternal` → 回到起点。
- **为什么原版不爆栈**：原实现在“已在 FIGHT”时把这次还手直接吞掉（`setStateIfNot(FIGHT)` 返回 false 就什么都不做）——那正是“不还手”bug 的成因，因此不能用同步重排去修。
- **修复与后续**：复位统一走 `AttackManager#resumeInterruptedAttack`（受击，延迟 0）/ `scheduleImmobileRetry`（够不着目标，按攻击间隔、最小 500ms），二者共用 `scheduleAttackRetry` + `PENDING_ATTACK_RETRIES` 去重（`shouldQueueAttackRetry(Future)`）；线程池任务体先撤销自身占位再校验 FIGHT/存活/有目标，然后 `scheduleNextAttack`（`isNextAttackScheduled()` 自带时间幂等）。
- **验证边界**：静态闸门只保证“受击栈内没有同步重排”；其它会回打调用方的同步路径（技能、强制移动等）需在实际复现时按同一条递归链核对，而不是只盯 `AttackEventHandler`。

---

## [AIM-006] 六、真端直接交互模式不得下发默认 HTML 对话 (DIRECT_INTERACTION_SKIPS_DEFAULT_DIALOG)
<!-- pattern-metadata
status: CONFIRMED
scope: RetailPatternAI2.handleTalkedByUser 的 talk 入口，以及 AI2Engine.selectNpcAi 对 useitem 回退的选择
first_seen: 2026-09-19
last_verified: 2026-09-19
symptom: 点击乘坐/操作固定炮台、坦克、攻城炮、宝箱等对象就弹 load fail!（HtmlPageId 10 / QuestId 0，客户端找不到 IDYun_Siegeweapon_* 一类 HTML 页），可骑乘对象上不去；空规则 retail pattern 还会让原生 useitem 乘坐/宝箱交互整体失效
root_cause: handleTalkedByUser 无条件调用 super.handleDialogStart，TalkEventHandler 默认分支下发 SM_DIALOG_WINDOW(objectId, 10)；真端模式的 on_talked_by_user 本就只有 use_skill / teleport_target(_alias) 直接动作，没有可加载的 HTML 页（模板 is_dialog 标记不可靠，大量直接交互 NPC 仍为 true）；另有空规则 retail pattern 在 selectNpcAi 中抢走 useitem fallback
fix_or_guardrail: on_talked_by_user 含 use_skill / teleport_target / teleport_target_alias 且无 on_hyperlink_clicked 的模式，以及带 on_gauge_* 事件的模式，都跳过默认 HTML 对话；retail pattern 没有任何可执行规则时不覆盖 useitem fallback
evidence: commit 5f7df6599; src/main/java/com/aionemu/gameserver/ai/RetailPatternAI2.java:1031; src/main/java/com/aionemu/gameserver/ai2/AI2Engine.java:153; src/test/java/com/aionemu/gameserver/ai/RetailPatternAI2Test.java:484; src/test/java/com/aionemu/gameserver/ai2/AI2EngineRetailSelectionTest.java:84; .agents/summary/retail-direct-interaction-dialog-guard/2026-09-19-retail-direct-interaction-dialog-guard.zh-CN.md
validation: static（compact pattern 扫描：直接交互模式约 393 个、覆盖约 997 条 NPC 映射；gauge 驱动约 20 条；空规则 useitem 约 25 条含 702648/702649）；focused-test（RetailPatternAI2Test + AI2EngineRetailSelectionTest 共 89 例 0 失败，2026-09-19）；client/production 复验 PENDING（需部署后复验 702685 与代表性炮台/坦克）
boundaries: 只看模式事件名与动作类型，不校验客户端 HTML 资源是否存在；带 on_hyperlink_clicked 的模式仍按 HTML 对话处理；只约束 talk 入口，其它事件链未改动
superseded_by: none
first_check: 遇到 load fail / HtmlPageId 10 时，先看该 NPC 的 retail pattern 在 on_talked_by_user 是否为 use_skill / teleport_target(_alias)，再看 AI2Engine.selectNpcAi 是否被空 pattern 抢走 useitem
-->
- **现象**：`702685`（Rentus 攻城炮）点击乘坐时客户端弹 `load fail!` / `IDYun_Siegeweapon_Li_01.html` / `(HtmlPageId 10)` / `(QuestId 0)`，乘坐动作完全不生效。
- **根因链**：
  1. `702685` 在 `npc-ai.xml` 绑定模式 `IDYun_SiezeWeapon_Li_03`，其 `on_talked_by_user` 只有 `use_skill SKILLI_INDEX_0` → `teleport_target_alias LocationsIDYun_siezeweapon3` → `despawn_self`（真端直接动作链，没有对应的 HTML 页）；
  2. `RetailPatternAI2.handleTalkedByUser` 无条件调用 `super.handleDialogStart(player)`，进入 `TalkEventHandler` 默认分支，向客户端下发 `SM_DIALOG_WINDOW(objectId, 10)`；
  3. 客户端按该包加载默认 HTML 第 10 页失败 → `load fail!`；真端乘坐动作被这个多余的前置对话包干扰。
- **为什么不能靠模板 `is_dialog` 判断**：静态扫描 `on_talked_by_user` 含直接动作且无 `on_hyperlink_clicked` 的模式约 393 个（覆盖约 997 条 NPC 映射），其中大量 NPC 模板带 `is_dialog="true"`；另有约 20 条 gauge 驱动交互与约 25 条空规则 `useitem` 映射（含 `702648` / `702649`）属于同类真实交互协议。
- **修复契约**：判断依据是模式自身的动作/事件形状，而不是模板标记——直接交互（`use_skill`、`teleport_target`、`teleport_target_alias`）与 `on_gauge_*` 模式一律不下发默认 HTML 页；`selectNpcAi` 里空规则 retail pattern 不得覆盖 `useitem`。
- **验证边界**：本轮只有静态审计和 focused-test 证明；客户端复验需部署后对 `702685` 及代表性炮台/坦克（`832075`、`832273`、`702346` 等）实测乘坐。

---

## [AIM-007] 七、阈值变身必须共用一次性生成闸门并在死亡路径兜底 (THRESHOLD_TRANSFORM_NEEDS_DEATH_FALLBACK)
<!-- pattern-metadata
status: CONFIRMED
scope: AI2 脚本的阈值变身（handleAttack/checkPercentage 的阈值分支 → spawn 替代形态 + AI2Actions.deleteOwner）及其死亡兜底，覆盖副本 AI 与世界 AI
first_seen: 2026-09-19
last_verified: 2026-09-19
symptom: 击杀本该“变身/换形态”的 Boss 后任务或场景不推进；爆发、一击、技能连招把人形从阈值以上直接打死时，替代形态完全不出现（例：任务 15300/25300 步骤 7「消灭盘龙巢穴的奥里萨」击杀 237230 不生成 237231，永远停在步骤 7）
root_cause: CreatureController#onAttack 先 getAggroList().addDamage(...)（内部经 ai.onAttacked → handleAttack 触发 AI 回调）再 getLifeStats().reduceHp(...)，因此 checkPercentage(getLifeStats().getHpPercentage()) 读到的是本次伤害结算前的 HP；从阈值以上一击/爆发致死时阈值分支永远不执行，替代形态从未生成，而任务只对替代形态记账
fix_or_guardrail: 每个阈值变身 AI 必须同时覆写 handleDied() → 复用与阈值路径完全相同的 *Once() 生成闸门（AtomicBoolean compareAndSet 幂等）；阈值路径保持先生成替代形态再 AI2Actions.deleteOwner，死亡路径只补生成；新增成员由 ThresholdTransformDeathFallbackGateTest 逐文件门禁（handleDied + compareAndSet + 同一 *Once() + deleteOwner + 目标 NPC id）
evidence: src/main/java/com/aionemu/gameserver/controllers/CreatureController.java; src/main/java/com/aionemu/gameserver/ai/instance/drakenspireDepths/immortalOrissanAI2.java; src/main/java/com/aionemu/gameserver/ai/instance/azoturanFortress/Betrayer_IcaronixAI2.java; src/test/java/com/aionemu/gameserver/ai/ThresholdTransformDeathFallbackGateTest.java; src/test/java/com/aionemu/gameserver/ai/instance/drakenspireDepths/ImmortalOrissanAI2Test.java; src/main/resources/aion/data/static_data/quest_definition/quests/15300.xml; src/main/resources/aion/definitions/compact/ai/npcaipatterns_idseal_q_yjh.xml; .agents/summary/quest-15300-orissan/2026-09-19-immortal-orissan-death-fallback.zh-CN.md
validation: static（CreatureController#onAttack 调用顺序 + 全仓约 136 个 checkPercentage/getHpPercentage 站点审计 + 10 文件同族清单）；focused-test（mvn -B test -Dtest='ImmortalOrissanAI2Test,Betrayer_IcaronixAI2Test,ThresholdTransformDeathFallbackGateTest,DrakenspireDepthsQOrissanSceneTest,DrakenspireDepthsQTwinSceneTest'，2026-09-19 22:17:58 11 例 0 失败 0 错误 BUILD SUCCESS）；runtime（同任务的上一段双子/米西奥内步骤已在 21:44 会话确认；奥里萨步骤本身未复测）；client/production 复验 PENDING（需重建重启新字节码后复测 15300/25300 步骤 7→8）
boundaries: 只覆盖“阈值触发的替代形态生成”，普通掉宝/宝箱/事件任务型 handleDied 不受约束；替代形态的存活、重生与清理仍由各自场景决定（如 Fountless_* 保留 scheduleRespawn）；阈值判读基于“上一击结算后的 HP”，因此阈值语义是上一击越过阈值才变身，不改变既有数值合同
superseded_by: none
first_check: 该 AI 是否在 handleAttack/checkPercentage 里 spawn 替代形态；handleDied 是否调用同一个 *Once() 生成闸门（只看次数，不重复判断 HP）
keywords: 阈值变身, 变身, 不灭之奥里萨, 虚脱的奥里萨, 237230, 237231, 15300, 25300, Immortal Orissan, Exhausted Orissan, checkPercentage, deleteOwner, 一击致死, 爆发致死, HP 读取顺序
-->

- **症状**：任务 15300（天族）/ 25300（魔族）推进到步骤 7「消灭盘龙巢穴的奥里萨」后击杀不推进；玩家观感是“杀死了奥里萨但任务不更新，而且刷出来的不是任务专属 NPC”。
- **根因链**：
  1. `15300.xml` 的 `s7 -> s8` 是 `<kill-npc npc-id="237231"/>`，任务只认**虚脱的奥里萨 237231**；
  2. 地图只刷 **237230 不灭之奥里萨**（AI `immortal_orissan_quest`），237231 必须由该 AI 在 ≤80% HP 时变生产生（真端 `IDSeal_Q_Oritsa_01` 在 `on_die` / `on_enter_abnormal_state` 都执行 `spawn(IDSeal_Q_Oritsa_65_Al_02)` + `despawn_self`）；
  3. `CreatureController#onAttack` 先在 `addDamage` 里触发受击回调（`handleAttack` → `checkPercentage`），之后才 `reduceHp`，所以**读到的是本次伤害前的 HP**；从 80% 以上一击致死时阈值分支整段跳过，237231 从未生成 → 任务永久停在步骤 7。
- **同族批量补齐**：同形状（阈值 → 生成替代形态 + `deleteOwner`）共 10 个 AI，统一补 `handleDied()` + 同一 `*Once()` 生成闸门：`immortalOrissanAI2`（80%，237231）、`Betrayer_IcaronixAI2`（原本已有死亡兜底，作为参考实现）、`Crazy_ScarAI2`（75%，281116）、`Fountless_Lava_ProtectorAI2`（30%，236227）、`Fountless_Heatvent_ProtectorAI2`（30%，236228）、`Unfaithful_NtuamuAI2`（50%，214583）与 tiamaranta_eye 的 `Aide_IranatiAI2`（218555）、`Master_At_Arms_RaniganAI2`（218558）、`TDown_M_Drakan_Pagati_Named_60_AeAI2`（249099）、`TDown_M_Drakan_Sikara_Named_60_AeAI2`（249102）。
- **不是同族**：`Lava_ProtectorAI2`（236227）与 `Heatvent_ProtectorAI2`（236228）的 `checkPercentage` 只启动 5 分钟牺牲/事件任务，不生成替代形态，其 `handleDied` 只掉宝箱，因此不进闸门名单——**同族判定看“阈值分支是否 spawn 替代形态”，不要按 NPC 名或目录聚类**。
- **验证边界**：静态闸门只能证明“阈值路径与死亡路径共用同一个生成闸门”，不能证明阈值数值与真端一致；替代形态被击杀后的任务记账仍走 questEngine，任务侧复测必须打到 237231 死亡才能闭环。
- **教训**：凡是“打到 X% 变形态/换阶段”的脚本 AI，都要问一句“如果这一击直接打死会怎样”；受击回调读到的 HP 永远滞后一次伤害，阈值分支不能作为唯一入口。
