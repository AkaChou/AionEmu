# AI Selection & NPC Movement Patterns (AI 选型与 NPC 移动模式)


> **生成物清理提示（2026-09-30）**：本文件正文提到的 `*.tsv` / `*.log` / `*.txt` / `*.xml` 等中间转储已随 `chore(agents)` 清理删除；原文与留痕仍可从 git 历史取回，被删清单与再生成入口见 `.agents/summary/CLEANUP-LEDGER.zh-CN.md`。

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
evidence: commit e718118c5; src/main/java/com/aionemu/gameserver/controllers/CreatureController.java; src/main/java/com/aionemu/gameserver/ai/instance/drakenspireDepths/immortalOrissanAI2.java; src/main/java/com/aionemu/gameserver/ai/instance/azoturanFortress/Betrayer_IcaronixAI2.java; src/test/java/com/aionemu/gameserver/ai/ThresholdTransformDeathFallbackGateTest.java; src/test/java/com/aionemu/gameserver/ai/instance/drakenspireDepths/ImmortalOrissanAI2Test.java; quest/retail/retail-xml-retention.xml 的 quest 15300 行（XML已退役并删除，见git历史）; src/main/resources/aion/definitions/compact/ai/npcaipatterns_idseal_q_yjh.xml; .agents/summary/quest-15300-orissan/2026-09-19-immortal-orissan-death-fallback.zh-CN.md
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

---

## [AIM-008] 八、服务端关窗必须收尾 NPC 对话态：行进中的对话 NPC 会停在半路 (SERVER_CLOSE_MUST_FINISH_TALK)
<!-- pattern-metadata
status: CONFIRMED
scope: 所有由服务端单方面下发 SM_DIALOG_WINDOW(0,0) 的收尾路径（任务车道推进/拒绝/失败关窗、引擎兜底关窗、重发断路、任务提交口）与带 walker 的对话 NPC（模板 is_dialog=true）
first_seen: 2026-10-06
last_verified: 2026-10-06
symptom: 与 NPC 对话后 NPC 不再巡逻（停在半路不动，区域无人 60s 后才随区域去激活恢复）；玩家报告「点按钮后弹窗关了，而且 NPC 不巡逻了」（2026-10-06 实机 NPC 203111 Spiros，quest 14110）
root_cause: 开门（CM_SHOW_DIALOG → DIALOG_START → TalkEventHandler.onSimpleTalk）对 is_dialog 模板把行进中的 NPC 置 AISubState.TALK 并设玩家为目标；下一次移动 tick 的到达判定把 TALK 直接短路为「已到达」（AbstractAI#isDestinationReached），于是 WalkManager.targetReached 命中 case TALK → abortMove() 停半路且不再选下一个 waypoint（state 仍 WALKING、substate 仍 TALK）。恢复巡逻的唯一常规入口是 DIALOG_FINISH → TalkEventHandler.onFinishTalk → think() → WalkManager.startWalking；而 DIALOG_FINISH 全仓只有客户端 CM_CLOSE_DIALOG 一个发送方，服务端单方面关窗只发包、不碰 AI ⇒ 客户端不回发关闭包时 NPC 永久停住
fix_or_guardrail: 服务端收尾关窗统一走 DialogService.closeDialog(npc, player) / closeDialog(player, targetObjectId)——按客户端 CM_CLOSE_DIALOG 同链补发 AIEventType.DIALOG_FINISH + 既有 onCloseDialog 清扫 + 关窗包；只通知「已装配 AI」的生物（Creature#getAi2IfPresent，不懒建 dummy：对话态本就挂在 AI 上，收尾路径不得有装配副作用）；重复触发安全（目标已清时 onFinishTalk 空操作）
evidence: src/main/java/com/aionemu/gameserver/services/DialogService.java（closeDialog 两个重载）; src/main/java/com/aionemu/gameserver/model/gameobjects/Creature.java（getAi2IfPresent）; src/main/java/com/aionemu/gameserver/ai2/handler/TalkEventHandler.java:93-114; src/main/java/com/aionemu/gameserver/ai2/AbstractAI.java:753; src/main/java/com/aionemu/gameserver/ai2/manager/WalkManager.java:254-256; src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_CLOSE_DIALOG.java:52（唯一 DIALOG_FINISH 发送方）; 改造面（11 文件 22 站点）：SimpleTalk/SimpleCollectItem/SimpleCombineTask/SimpleHunt/SimpleSerialHunt/SimpleItemPlay/DataDrivenNativeRuntime 各关窗点 + DialogService 兜底 + CM_DIALOG_SELECT 断路 + PlayerQuestDialogPort; src/test/java/com/aionemu/gameserver/services/DialogServiceTest.java（3 例）; .agents/summary/quest-14110-check-button/DIAGNOSIS.zh-CN.md §7.1
validation: focused-test（DialogServiceTest 3/3；相关车道族门 SimpleTalk 16/16、SimpleCollectItem 16/16、DataDriven 30/30、CombineTask 11/11、ItemPlay 15/15、UseItem 11/11、Hunt 5/5、SerialHunt 4/4、PlayerQuestDialogPort 11/11、QuestEngineNpcDialogDispatch 6/6、QuestProductionStartupGate 2/2 全绿，2026-10-06 IDEA MCP runner）；runtime 实机复测通过（2026-10-06 用户实测确认：203111 关窗后恢复巡逻）
boundaries: ① 未装配 AI 的生物不通知（无对话态可结束）；② 无 NPC 上下文的关窗（如 SimpleUseItem 的 ownerless 接取面）保持纯关窗；③ 客户端收到服务端关窗包后是否回发 CM_CLOSE_DIALOG 未实测（本修复对两种情形都安全：重复 DIALOG_FINISH 幂等）；④ 区域去激活/重生仍会清除 TALK 子状态（不是本缺陷的常规恢复路径，只是兜底）
superseded_by: none
first_check: 玩家报「NPC 不巡逻/站住不动」时先看：① 该 NPC 模板 is_dialog 且 spawn 带 walker_id？② 最近一次对话结束是客户端关窗还是服务端关窗（quests.log 的 0x5d8 关窗行）？③ 关窗站点走的是 DialogService.closeDialog 还是裸 SM_DIALOG_WINDOW(0,0)？
keywords: NPC 不巡逻, 停止巡逻, 站住不动, 关窗, SM_DIALOG_WINDOW, 0x5d8, DIALOG_FINISH, CM_CLOSE_DIALOG, AISubState.TALK, abortMove, WalkManager, targetReached, Spiros, 203111, walker, is_dialog, 对话收尾
-->

- **症状**：和巡逻 NPC 对话后它停在半路一直不动；服务端悄悄关窗（任务推进/拒绝/检查失败）时尤其明显，玩家观感是「点了按钮弹窗关了，NPC 也不巡逻了」。
- **根因链**：`TalkEventHandler.onSimpleTalk`（`is_dialog` 模板）置 `AISubState.TALK` + `setTarget` → `AbstractAI#isDestinationReached` 在 WALKING 下把 TALK 短路成「已到达」 → `WalkManager.targetReached` 的 `case TALK: abortMove()` 停半路不选下一点 → 恢复只发生在 `DIALOG_FINISH` → 全仓只有 `CM_CLOSE_DIALOG` 发它，服务端关窗站点全都只发包。
- **修复契约**：服务端关窗 = `DialogService.closeDialog(...)`（`DIALOG_FINISH` + `onCloseDialog` + 关窗包）；只对已装配 AI 的生物发事件（`getAi2IfPresent`）；重复触发幂等。
- **验证边界**：单测锁的是「关窗即发 DIALOG_FINISH」这条接线；「NPC 重新沿路线走」实机复测通过（2026-10-06）；区域去激活是另一条兜底恢复路径，容易造成"过一会儿自己好了"的误判）。

## [AIM-009] 九、编队航点 Z 必须按航点自身坐标采样：偏移航点距上一步最多 8m，斜坡/台阶段误差达米级 (FORMATION_WAYPOINT_Z_AT_WAYPOINT)

<!-- pattern-metadata
status: CONFIRMED
scope: OFFSET/方阵编队（WalkerGroup）成员的路线航点（NpcMoveController.setRouteStep 的编队分支）与 WALK_PATH 行走态的 Z 处理；不含无编队行走者（行为未变）
first_seen: 2026-10-06
last_verified: 2026-10-06
symptom: 编队巡逻的小动物上台阶悬空/上下跳、下台阶入地，编队偏移越大越明显、队长（偏移 0）完全正常（2026-10-06 实机 Verteron 210030000 LF1A_NPCPath_Ermona：799731/799736/799737/799746 偏移 −2/−3/−6/−8m）
root_cause: setRouteStep 编队分支把航点 Z 取 resolveRouteStepZ(上一步)——上一步坐标处地面——而航点 X/Y 已按编队偏移沿路径后退 offsetsy 米，两者相距最多 8m；斜坡/台阶段上 Z 误差=该 8m 内的地面高差（离线复制 0.26/0.27/0.84/1.17m，随偏移放大）。且行走态 substate=WALK_PATH 被逐 tick 贴地修正排除（shouldApplyGeoHeightCorrection && ... && getSubState() != AISubState.WALK_PATH），错误 Z 即 NPC 真实 Z 与下发客户端的航点 Z：正误差=悬空、负误差=入地、相邻航点交替=上下跳
fix_or_guardrail: 编队航点 Z 按航点自身 (pointX,pointY) 采样（新增 NpcMoveController#resolveGroundZ(x,y,fallbackZ)；resolveRouteStepZ 改为其薄封装；回退=上一步模板 Z，保持旧语义）；队长偏移 0 时采样坐标与旧值相同、无编队行走者完全不变。诊断口径：离线下投复刻 GeoMap.getZ = PHYSICAL 网格面 ∪ 地形高度图取 [z−100,z+2] 带内最高面（PNG 高度=u16/32，行列按 readHeightData 转置），可对任意路线出误差表
evidence: src/main/java/com/aionemu/gameserver/controllers/movement/NpcMoveController.java（setRouteStep 编队分支/resolveGroundZ；WALK_PATH 贴地排除条件）; src/main/java/com/aionemu/gameserver/spawnengine/WalkerGroup.java（getLinePoint/offsets）; src/main/resources/aion/data/static_data/npc_walker/210030000_Verteron_Walkers.xml（LF1A_NPCPath_Ermona）; 实机 log/aidebug.log 2026-10-06 16:55-17:00（799737 航点目标与到达 Z）; .agents/summary/npc-walker-stairs/DIAGNOSIS.zh-CN.md + probe_route_ground.py（17/17 路线点与实机 resolveRouteStepZ 逐位吻合）+ decode_walker_offsets.py（13/13 目标反解命中）
validation: 离线复刻对拍（17/17 与实机日志一致）；focused-test（NpcMoveControllerPathTest 62/62、InstanceWalkerFormationsPositionGroupingTest 7/7、WalkManagerTest 2/2，2026-10-06 IDEA MCP）；runtime 实机复测通过（2026-10-06 用户：不再悬空、不再入地；残留台阶段轻微上下跳=航段直线插值几何，见 boundaries ①）
boundaries: ① 航段内仍按直线插值（每航点一个 SM_MOVE，服务端与客户端同直连）——台阶段弦切差约半个台阶高，实机残留轻微上下跳，未处理（备选：航段贴地子航点/加密路线点）；② 队长（偏移 0）与无编队行走者行为不变；③ 采样沿用 resolveRouteStepZ 的 instanceId=1 取样带
superseded_by: none
first_check: 编队 NPC 上下台阶悬空/入地/上下跳先答：① 该成员编队偏移几米（spawn walker_index 查 walker_template 的 offsetsy）？② 航点 Z 取航点自身坐标还是上一步坐标？③ substate 是否 WALK_PATH（被逐 tick 贴地修正排除）？
keywords: 编队、OFFSET 队形、航点 Z、悬空、入地、上下跳、小动物、walker group、getLinePoint、offsetsy、resolveRouteStepZ、resolveGroundZ、WALK_PATH、贴地修正、GeoMap.getZ、台阶、799731、799736、799737、799746、Ermona、FORMATION_WAYPOINT_Z_AT_WAYPOINT
-->

- **症状**：编队小动物上台阶「上上下下地跳/升空又下来」、下台阶「入地」；偏移越大越明显，队长（偏移 0）完全正常。
- **根因链**：航点 X/Y 已按编队偏移后退（最多 8m），Z 却取上一步坐标处地面 → 斜坡/台阶段 Z 误差=8m 地面高差（实机离线复刻 0.26~1.17m）；行走态 WALK_PATH 不做逐 tick 贴地 → 错误 Z 直接成为 NPC 真实 Z 与客户端航点 Z。
- **修复契约**：编队航点 Z 按航点自身 (pointX,pointY) 采样（`resolveGroundZ`）；回退语义与旧实现一致；队长/无编队行走者不变。
- **验证边界**：离线 `GeoMap.getZ` 复刻（网格 ∪ 地形）与实机日志 17/17 对拍；残留台阶段轻微上下跳=航段直线插值几何（弦切差≈半台阶高），未处理。

## [AIM-010] 十、编队恢复必须按编队当前步：偏移 0 的队长站在路线点 1m 内会被「个体续走」抢跑下一步 (GROUP_RESUME_ON_GROUP_STEP)

<!-- pattern-metadata
status: CONFIRMED
scope: WalkerGroup 成员的行走恢复路径（WalkManager.startWalking → startRouteWalking → findNextRoutStep），触发源含对话结束（DIALOG_FINISH → think）与战斗打断等一切 thinkWalking 恢复；不含无编队行走者
first_seen: 2026-10-06
last_verified: 2026-10-06
symptom: 与编队领队对话结束后，领队「提前往前走」、后面的小动物没及时跟上，且掉队后不自愈（编队永久错开一步）。2026-10-06 实机 205294（Tenos，Verteron LF1A_NPCPath_Ermona 队，talk_info is_dialog=true）
root_cause: 对话结束链 TalkEventHandler.onFinishTalk → think → ThinkEventHandler.thinkWalking → WalkManager.startWalking → startRouteWalking → findNextRoutStep：findNextRouteStepAfterPause 在 NPC 距「当前路线点」<1m 时直接推进下一步。队长编队偏移为 0、站位=路线点本身 ⇒ 命中该分支单独走一步；追随者偏移 2~8m、距路线点 >1m 不抢步，留在 WALK_WAIT_GROUP 等齐 ⇒ WalkerGroup.setStep 把 groupStep 抬到队长新步，编队从此永久超前一步
fix_or_guardrail: 编队成员一律按编队当前步恢复（走 findClosestRouteStep 的编队分支：groupStep<2 → 第 1 步，否则 groupStep 对应点）；「个体续走」只限无编队行走者（WalkManager#shouldResumeIndividually(inWalkerGroup,currentPoint) 唯一判据）。违例症状=领队单飞一段 + 队伍永久错位
evidence: src/main/java/com/aionemu/gameserver/ai2/manager/WalkManager.java（findNextRoutStep/shouldResumeIndividually/findNextRouteStepAfterPause/findClosestRouteStep 编队分支）; src/main/java/com/aionemu/gameserver/ai2/handler/TalkEventHandler.java:106-114; src/main/java/com/aionemu/gameserver/ai2/handler/ThinkEventHandler.java（thinkWalking）; .agents/summary/npc-walker-stairs/DIAGNOSIS.zh-CN.md §3
validation: focused-test（WalkManagerTest.onlyUngroupedWalkersResumeIndividually 2/2、ThinkEventHandlerTest 1/1、FollowManagerTest 5/5，2026-10-06 IDEA MCP）；runtime 实机复测通过（2026-10-06 用户：对话后编队恢复、不再掉队）
boundaries: ① 无编队行走者语义不变（仍按个体续走/最近点分支）；② currentPoint=0（新路线/首步）两分支同走最近点，不受影响
superseded_by: none
first_check: 编队对话/战斗中断后「领队先走、追随者掉队」先答：① 该 NPC 是否编队成员（spawn walker_id/walker_index）？② 恢复是否走个体续走分支（findNextRouteStepAfterPause）？③ groupStep 是否被抬高到队长步（WalkerGroup.setStep）？
keywords: 编队、领队抢跑、对话后提前往前走、小动物掉队、WALK_WAIT_GROUP、groupStep、findNextRoutStep、findNextRouteStepAfterPause、shouldResumeIndividually、startWalking、205294、Ermona、GROUP_RESUME_ON_GROUP_STEP
-->

- **症状**：编队领队对话后单独超前走一步，追随者掉队且不自愈（永久错开一步）。
- **根因链**：恢复链 findNextRoutStep → findNextRouteStepAfterPause；队长偏移 0、站在路线点 1m 内 → 推进下一步；追随者不抢步仍在 WALK_WAIT_GROUP → groupStep 被抬高 → 永久错位。
- **修复契约**：编队成员按编队当前步恢复；个体续走仅限无编队行走者（`shouldResumeIndividually`）。
- **验证边界**：单测锁「编队成员一律不抢步」规则；实机复测通过（编队恢复、不掉队）。

## [AIM-011] 十一、客户端 NPC 移动包是「起→终直线 + 到达即停」：短段下发必须节奏对齐（前视 ≈ 一个下发周期的行程），过短=卡停+前跳、过长=入地+拉起 (NPC_MOVE_PACKET_ARRIVE_AND_STOP)

<!-- pattern-metadata
status: CONFIRMED
scope: 服务端向客户端下发行走 NPC（WALK_PATH）移动更新的策略（NpcMoveController 广播块 + SM_MOVE 组成）；不含玩家移动（PlayableMoveController 向量形）与空间寻路（path!=null）的到点逻辑
first_seen: 2026-10-06
last_verified: 2026-10-06
symptom: ① 短段下发节奏错配（包晚于客户端到达）时行走 NPC「卡在原地，然后往前瞬移」循环（v1 实机）；② 前视过长（1m，而每 tick 只走 0.1–0.2m）时台阶段「脚部入地然后拉起来」的周期性跳帧（v2 实机残留）——客户端每 tick 只爬弦线行程的 1/5–1/10，落后于地形，下一包地面真值重锚把它拉起
root_cause: SM_MOVE 的 NPC 形态只携带 起点/终点/朝向/掩码，没有时长字段（速度由客户端掩码表决定；真端客户端包亦然，见真端 `Npc::MoveNpc` 25/37 字节构造，时长只用于服务端调度）⇒ 客户端对每个包按「从起点走直线到终点、到达即停」执行；移动中收到新包即「吸附到新起点 + 重定目标」。两条失效路径都由「前视 L 与每 tick 行程 w 的比值」决定：L < w（或包晚到）⇒ 客户端先到点停下、下个包起点已在前 ⇒ 停-跳；L ≫ w ⇒ 客户端一个 tick 只走弦线的 w/L，遇台阶/坡沿时竖向爬升滞后于地面（入地或浮空），下一包吸附回地面真值 ⇒ 每 tick 一次「入地→拉起」；换腿后若延迟若干 tick 才补发（门控/粘滞），客户端会先走完整段长弦（可达 8–9m）再被一次性拉回。真端靠「时长=距离/速度」的节奏保证「包 = 刚走完的一步」、每步解算贴地、到期即发，客户端始终沿贴地短弦推进
fix_or_guardrail: ① 前视距离 = 本 tick 实测位移 × 1.3（下限 0.15m）——略大于一个下发周期的行程：客户端不会先到点，弦线又足够短（≈0.25m）贴住台阶；实测位移自动适配 100/200/500ms 三档移动周期，且休息后恢复的首个 tick 不受暂停时长影响；系数是可调旋钮（出现停顿 → 提到 1.5；仍见入地 → 降到 1.15）；② 行走态每个移动 tick 都补发（不设地形门控、换腿首 tick 即发短段，绝不发整段长弦）；③ 段端点 Z 必须贴地（AIM-009：按航点自身坐标采样）；④ 服务端逐 tick 贴地与短段下发必须成对引入（贴地是短段重锚不产生反向拉扯的前提）；⑤ v1 的教训：门控/节流让包间隔抖动到 300–500ms 即「卡在原地+前跳」——禁止再给补发加节流/门控（200ms 实机事故复现）
evidence: src/main/java/com/aionemu/gameserver/controllers/movement/NpcMoveController.java:1112-1158（前视 = 实测位移 × 1.3、每 tick 补发、短段包起点=本 tick 贴地位置）、:1070-1095（行走态逐 tick 贴地）、walkStreamLookahead/shouldStreamWalkGround/walkGroundStreamTarget 辅助（:443-500）；src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_MOVE.java（无时长字段、掩码决定目标坐标是否写入）；真端对照（每步碰撞/地表解算 + 带时长移动包）见 .agents/summary/npc-walker-stairs/RETAIL-WALK-SEMANTICS.zh-CN.md；实机日志定量复核（218 tick 贴地误差≡0；前视 1m 时 pop p90≈2.8cm/max 11.3cm/>4cm 9 次 + 换腿 6–7 tick 长弦一次拉 16.7cm；自适应 1.3 后 >4cm 仅 3 次且均在台阶棱线）见 .agents/summary/npc-walker-stairs/LOG-ANALYSIS-799737.zh-CN.md
validation: runtime 实机 2026-10-06（v1 节奏错配实机「卡在原地然后往前瞬移」回退；v2 前视 1m 实机残留「脚部入地然后拉起来」，经 ai2 日志 + 离线客户端模拟定量确认为前视过长，v3 已实现待实机复测）；static（SM_MOVE 组包体 + 真端 Npc::MoveNpc 客户端包逐字段对账 + 既有客户端动画约定测试）；focused-test v3 NpcMoveControllerWalkStreamTest 3/3 + NpcMoveControllerPathTest 62/62（2026-10-06 IDEA MCP）
boundaries: ① 只描述本仓库客户端对 NPC 移动包的行为（玩家移动走 PlayableMoveController 向量形，语义不同）；② 真端的精确对齐依赖其「包=刚走完的一步」+ 时长调度，本客户端无时长字段，只能用「前视 = 实测位移 × 1.3」近似（v3 待实机判定；余量仅 0.3 个 tick，极端 tick 抖动下理论上有 ≤30–60ms 微停）；③ 移动周期档位在行走中切换（玩家跨 30m/60m 边界）时，首个新档 tick 的余量按旧行程估计，可能有一次微停；④ AIM-009 的航点 Z 采样仍有效；⑤ v1/v2 两次实机仅证明「前视与行程错配会分别导致停-跳/入地-拉起」，不证明其它前视系数不可行
superseded_by: none
first_check: 给行走 NPC 加移动下发优化前先答：① 这个包改了目标还是只改了起点？② 本 tick 客户端实际能走多远（w），我给的弦线长度（L）与 w 的比值是多少？③ 客户端在移动中收到同掩码新包是重锚还是忽略（本客户端没有可靠证据）？④ 该改动是否只影响服务端模拟（客户端不可见）？
keywords: SM_MOVE、到达即停、停跳、卡在原地往前瞬移、脚部入地然后拉起来、台阶跳帧、短段下发、前视距离、移动节奏对齐、高频重定目标、移动动画重启、WALK_PATH、逐 tick 贴地、带时长移动包、真端持续推进、NPC_MOVE_PACKET_ARRIVE_AND_STOP
-->

- **症状**：① 节奏错配（包晚于客户端到达）⇒「卡在原地，然后往前瞬移」；② 前视过长（1m vs 每 tick 行程 0.1–0.2m）⇒ 台阶段周期性「脚部入地然后拉起来」。
- **根因**：客户端按「起→终直线、到达即停」执行移动包（无时长字段，速度由掩码表定），移动中被新包重锚到新起点。L/w 比值决定失效模式：L<w 停-跳；L≫w 弦线爬升滞后地形、每 tick 被地面真值拉起。真端以「包=刚走完的一步 + 时长=距离/速度」天然对齐。
- **契约**：前视 = 本 tick 实测位移 × 1.3（下限 0.15m）；行走态每 tick 补发（无门控，换腿首 tick 即发）；配合服务端逐 tick 贴地（成对）；系数旋钮 1.15–1.5。
- **留档**：真端客户端包与 SM_MOVE 同构（无时长），时长只用于服务端调度；v3 待实机复测（复测点与日志判据见 .agents/summary/npc-walker-stairs/LOG-ANALYSIS-799737.zh-CN.md）。
