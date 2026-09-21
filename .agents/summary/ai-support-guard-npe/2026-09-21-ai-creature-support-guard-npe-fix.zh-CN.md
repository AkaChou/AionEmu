# AI 支援与守卫反击目标重读空指针修复

## 症状 / Symptom

- 运行日志出现已知列表遍历 NPC 异常：
  ```
  KnownList - 对所有 NPC 运行访问器时异常：所有者=Npc#160900 LF6_T_NewMerman_A_66_n@210100000，NPC=Npc#15949 LF6_OP_Guard_Ra_75_Ah@210100000，已遍历=6
  java.lang.NullPointerException: Cannot invoke "com.aionemu.gameserver.model.gameobjects.Creature.getTarget()" because "attacker" is null
  	at com.aionemu.gameserver.ai2.handler.AggroEventHandler.onGuardAgainstAttacker(AggroEventHandler.java:111)
  	at com.aionemu.gameserver.ai.AggressiveNpcAI2.handleGuardAgainstAttacker(AggressiveNpcAI2.java:135)
  	at com.aionemu.gameserver.ai2.AbstractAI.handleCreatureEvent(AbstractAI.java:693)
  	at com.aionemu.gameserver.ai2.AbstractAI.onCreatureEvent(AbstractAI.java:344)
  	at com.aionemu.gameserver.controllers.CreatureController$1.visit(CreatureController.java:281)
  ```
- 触发场景：人鱼 NPC（`LF6_T_NewMerman_A_66_n`）在受到攻击结算（`CreatureController#onAttack`）时，通知周围所有已知 NPC（包含哨所守卫 `LF6_OP_Guard_Ra_75_Ah`，AI 为 `RangerAI2` -> 继承 `AggressiveNpcAI2` -> `AbstractAI`）处理 `CREATURE_NEEDS_SUPPORT` 事件。

## 根因 / Root cause

1. **目标重读与并发竞态**：
   在 `AbstractAI.java`（原第 688–696 行）：
   ```java
   case CREATURE_NEEDS_SUPPORT:
   	if (!handleCreatureNeedsSupport(creature)) {
   		if (creature.getTarget() instanceof Creature) {
   			if (!handleCreatureNeedsSupport((Creature) creature.getTarget())
   					&& !handleGuardAgainstAttacker(creature)) {
   				handleGuardAgainstAttacker((Creature) creature.getTarget());
   			}
   		}
   	}
   ```
   代码在没有局部变量暂存的情况下，对 `creature.getTarget()` 进行了连续 3 次重新求值。在服务端多线程战斗环境下，实体的 target 随时可能因目标死亡、脱战、清除目标或切换目标而被置空（`setTarget(null)`）。当第 1 次求值 `instanceof Creature` 通过后，第 3 次调用 `(Creature) creature.getTarget()` 时该字段已被置为 `null`，导致向 `handleGuardAgainstAttacker` 传入了 `null`。
2. **AI 与事件处理器层防御缺失**：
   - `AggressiveNpcAI2#handleGuardAgainstAttacker` 未作判空直接透传给 `AggroEventHandler.onGuardAgainstAttacker(this, attacker)`。
   - `AggroEventHandler#onGuardAgainstAttacker` 第 111 行直接执行 `VisibleObject target = attacker.getTarget();`，在 `attacker == null` 时抛出 `NullPointerException`。

## 修复方案与同族排查 / Fix and Family Audit

1. **根源修复（`AbstractAI.java`）**：
   改用模式匹配单次求值并绑定到局部不可变引用：
   ```java
   case CREATURE_NEEDS_SUPPORT:
   	if (!handleCreatureNeedsSupport(creature)) {
   		if (creature.getTarget() instanceof Creature targetCreature) {
   			if (!handleCreatureNeedsSupport(targetCreature)
   					&& !handleGuardAgainstAttacker(creature)) {
   				handleGuardAgainstAttacker(targetCreature);
   			}
   		}
   	}
   	logEvent(event);
   	break;
   ```
   从结构上杜绝由于多线程并发重读导致的 `null` 穿透。
2. **AI 层防线强化（`AggressiveNpcAI2.java`）**：
   `handleGuardAgainstAttacker` 增加 `attacker != null` 卫语句。
3. **事件处理器全链路空值与存活保护（`AggroEventHandler.java`）**：
   - `onGuardAgainstAttacker`：补充对 `npcAI`、`attacker`（含 `isAlreadyDead()`）、`owner`（含 `isAlreadyDead()`）的前置判空与死亡态拦截；反击时保护的目标玩家增加非死亡判断。
   - `onCreatureNeedsSupport`：补充对 `npcAI`、`notMyTarget` 及 `owner` 的判空与存活保护。
   - `onAggro`：补充对 `npcAI`、`myTarget` 及 `owner` 的判空与存活保护。
   - `AggroNotifier#run`：延迟 500ms 任务执行时，增加对 `aggressive` 与 `target` 的存活与判空校验。
   - `canReceiveSupport`：入参判空保护。
4. **同族排查（`ShoutEventHandler.java` & `EmoteManager.java`）**：
   - `ShoutEventHandler.java:81`：使用模式匹配 `if (npc.getTarget() instanceof Creature targetCreature)` 替代再次调用 `(Creature) npc.getTarget()`。
   - `EmoteManager.java:47`：使用模式匹配 `if (owner.getTarget() instanceof Player playerTarget)` 替代再次调用 `(Player) owner.getTarget()`。

## 验证与门禁 / Validation and Gates

- 聚焦单元测试（已执行并通过）：
  `mvn test -Dtest=AggroEventHandlerTest,AbstractAITargetSafetyTest,SimpleAttackManagerTargetSafetyTest`（5/5 通过）。
  - `AggroEventHandlerTest` 增加了针对 `onGuardAgainstAttacker` 与 `onCreatureNeedsSupport` 传入 `null` 或非存活生物时的安全断言。
  - 新增 `AbstractAITargetSafetyTest` 源码契约测试，门禁确保 `AbstractAI` 不得再次出现对 `creature.getTarget()` 的重复读取。
- AI2 模块全套回归（已执行并通过）：
  `mvn test -Dtest="com.aionemu.gameserver.ai2.**.*Test"`（42/42 通过，0 Failures，0 Errors）。
