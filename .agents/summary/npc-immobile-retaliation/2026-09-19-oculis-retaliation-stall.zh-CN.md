# 0 移速怪被打不还手（Lurking Clamshell Oculis 235805）

## 症状 / Symptom

- 真端反馈：Cygnea（210070000）的 `Lurking Clamshell Oculis`（npc 235805）被打后不移动、不反击，像卡死。
- 客户端 target dump（管理员 `//info`）：`AI: retail_pattern`、`isEnemy/isAggressive: true`、`HP 12814/14712`、
  `[AgroList] Ww Dmg: 1898`（仇恨已入表）、`//ai2 log` 可切换 → AI2 实例存在，不是“AI 未装配”。

## 真端数据 / Retail data

- `src/main/resources/aion/data/static_data/npcs/npc_template_235749_247606.xml:349`：235805
  `walk_speed="0" run_speed="0" run_speed_fight="0"`、`attack_range="37"`（同族 235798 Large Clamshell Oculis 同为 0 速度）
  → 真端就是“站着不动的远程怪”，不移动是符合数据的。
- `src/main/resources/aion/definitions/compact/ai/npc-ai.xml:22156`：`ai="D2_FnA"`、`sensory_range=8`、
  `max_chase_time=8`、`react_to_pathfind_fail=return_to_sp`。
- `src/main/resources/aion/definitions/compact/ai/npcaipatterns.xml:351`：`D2_FnA` 的 `<event_handlers/>` 为空
  → 该 NPC 没有任何脚本事件，行为全部来自 `RetailPatternAI2 → AggressiveNpcAI2` 的通用逻辑。
- 技能组 `NS_16D2744C54E4F837`（`npc-skills.xml:978`，:11723 归属 235805）：16901 `NRA_SmallShot_Thornspew_LC`、
  16902 `NRA_SnareLong_Nr_LC`，15s CD。

## 根因 / Root cause

1. 伤害入仇恨时会回调 AI：`AggroList.java:138` → `owner.getAi2().onAttacked(...)`（事件确实到达 AI）。
2. 攻击链只在“进入 FIGHT”那一次启动：`AttackEventHandler#onAttack`（:44）只有 `tryEnterFight()`
   （能发生 IDLE→FIGHT 状态切换）成功才会 `setTarget + AttackManager.startAttacking`。
3. 目标不在射程/视线时只发 `TARGET_TOOFAR`（`SimpleAttackManager` :48-52、:188）→ `AttackManager#targetTooFar`（:114）：
   - 可移动 NPC：`moveToTargetObject()` 追击，到达后由 `TargetEventHandler#onTargetReached`（:36-45）重排攻击；
   - **0 移速 NPC（本怪）：不能追击，也没有重排下一次攻击 → 攻击链静默停摆**。
4. 此后每次受击：`tryEnterFight()` 因“已经在 FIGHT”返回 false → 不重设目标、不排下一次攻击；而
   `renewLastAttackedTime()`（:43）每次都被刷新，`max_chase_time=8` 的脱战判定永远到不了点
   → 永久“挂着仇恨却不还手”。

## 处置 / Change

- `AttackManager`：新增统一的“攻击链复位”通道，按对象 ID 去重（`PENDING_ATTACK_RETRIES` /
  `shouldQueueAttackRetry(Future)`：在途任务不重复入队，任务结束可再次入队）：
  - `resumeInterruptedAttack(npcAI)`：受击复位，延迟 0，交给 `GameThreadPoolServices.threadPoolManager().schedule(...)`；
  - `scheduleImmobileRetry(npcAI)`：0 移速够不着目标，按攻击间隔（最小 500ms）重排；
  - 线程池任务 `runAttackRetry(...)`：先撤销自身占位，再校验 FIGHT / 已生成 / 存活 / 有目标，清 `TARGET_LOST` 后
    `scheduleNextAttack(npcAI)`（`isNextAttackScheduled()` 自带时间幂等）。
- `AttackEventHandler#onAttack`：已在 FIGHT 的受击走新增分支——清 `TARGET_LOST` 子状态并调用
  `AttackManager.resumeInterruptedAttack(npcAI)`；**受击调用栈内不得同步重排攻击**（见下节回归）。
- `AttackManager#targetTooFar`：0 移速分支新增 `scheduleImmobileRetry(npcAI)`，按攻击间隔重排一次尝试，
  直到目标回到射程/视线，或真端 `max_chase_time` 规则结束战斗（AIM-004 的“不放弃目标”语义保持不变）。
- `Ai2Command#set`：`//ai2 set` 换 AI 后补发 `AIEventType.SPAWNED`；新 AI 实例默认停在 `CREATED`，
  缺少这一步会让 `canHandleEvent` 拒绝 ATTACK / CREATURE_SEE，NPC 直接木桩化（与 `RetailDirectPortalEngine` 既有做法一致）。

## 回归与修正 / Regression and correction

- 首版修复把“已在 FIGHT 的受击”直接同步 `AttackManager.scheduleNextAttack(npcAI)`，线上立即复现
  `java.lang.StackOverflowError`（2026-09-19 11:44:33 `pool-4-thread-5`，由 `ExecuteWrapper` 记录）。
- 递归链（约 12 帧一轮、双方来回）：`AggroList#addDamageInternal:138` → `AbstractAI#onAttacked:353`
  → `RetailPatternAI2#handleAttack:936` → `AggressiveNpcAI2#handleAttack:52` → `AttackEventHandler#onAttack`
  → `AttackManager#scheduleNextAttack:76` → `chooseAttack:103` → `SimpleAttackManager#performAttack:48/66`
  → `attackAction:177` → `CreatureController#attackTarget:434` → 对方的 `AggroList#addDamageInternal` → 回到起点。
  只要双方普攻间隔为 0（`getNextAttackInterval()` 在攻击链停摆后返回 0），就会立刻互相还手并无限递归。
- 定论：**任何来自受击调用栈的攻击重排都必须异步**。原版之所以不爆栈，只是因为“已在 FIGHT 时把这次还手直接吞掉”，
  而这正是本 bug 的成因——不能用同步重排来修。
- 闸门升级：`AttackEventHandlerTest#alreadyFightingHitResumesInterruptedAttackChain` 断言受击分支必须走
  `AttackManager.resumeInterruptedAttack(npcAI)` 且**不得**出现同步 `AttackManager.scheduleNextAttack(`；
  `AttackManagerTest#immobileNpcsRetryAttacksInsteadOfDroppingTheChain` 断言重排必须经线程池 + 去重，
  且只有线程池任务体允许 `scheduleNextAttack`；`AttackManagerTest#attackRetryDeduplicationOnlySkipsInFlightTasks`
  以 `Future` 实例校验去重规则。

## 验证边界 / Validation boundary

- Static：对照真端 compact 数据、AIM-004 既有闸门（`targetTooFar` 仍不得出现 `TARGET_GIVEUP`）。
- 已执行（2026-09-19，聚焦闸门）：`mvn -Dtest='AttackEventHandlerTest,AttackManagerTest' test` → Tests run: 7,
  Failures: 0, Errors: 0，BUILD SUCCESS。
- 已执行（2026-09-19，AI2 战斗面回归）：`mvn -Dtest='AttackEventHandlerTest,AttackManagerTest,AttackManagerLeashTest,
  TargetEventHandlerTest,SimpleAttackManagerTargetSafetyTest,RetailPatternAI2Test,AggroEventHandlerTest,
  ThinkEventHandlerTest,CreatureEventHandlerTest' test` → Tests run: 104, Failures: 0, Errors: 0，BUILD SUCCESS。
- PENDING：服务端重新部署 + 真端客户端复验。预期现象：受击后一个攻击间隔内开始吐刺/定身；跑到 37 码外后仍按 ~2.4s
  重试，`max_chase_time=8`（8s 内无攻击/受击）到点后按真端规则脱战。
- 复验时必须回看日志：不能再出现 `StackOverflowError`（尤其 `pool-*-thread-*` 上的 `ExecuteWrapper` 记录）。
