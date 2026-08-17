package com.aionemu.gameserver.controllers;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameMovementLoopServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.handler.ShoutEventHandler;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.controllers.attack.AttackResult;
import com.aionemu.gameserver.controllers.attack.AttackStatus;
import com.aionemu.gameserver.controllers.attack.AttackUtil;
import com.aionemu.gameserver.controllers.observer.TerrainZoneCollisionMaterialActor;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.item.ItemAttackType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOVE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_CANCEL;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.model.HealType;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.model.Skill.SkillMethod;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.skillengine.model.SkillType;
import com.aionemu.gameserver.taskmanager.tasks.MovementNotifyTask;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneUpdateService;

import java.util.Map;

/**
 * 生物控制器基类，管理 NPC/玩家等生物的移动、攻击、技能与区域逻辑。
 * Base controller for creatures (NPCs, players, etc.) managing movement, attacks, skills and zones.
 *
 * @author -Nemesiss-, ATracer(2009-09-29), Sarynth
 * @modified by Wakizashi
 * @param <T> 所有者生物类型 / owner creature type
 */

@Slf4j
public abstract class CreatureController<T extends Creature> extends VisibleObjectController<Creature> {

	/** 任务 ID 到 Future 的映射 / Map from task id to Future */
	private final Map<Integer, Future<?>> tasks = new ConcurrentHashMap<Integer, Future<?>>();
	/** 地形区域碰撞材质角色 / Terrain zone collision material actor */
	private volatile TerrainZoneCollisionMaterialActor terrainMaterialActor;
	/** 治疗技能增益倍率。 / Healing skill boost multiplier. */
	private float healingSkillBoost = 1.0f;
	/** 简易攻击类型。 / Simple attack type. */
	private int SimpleAttackType;

	/**
	 * 当对象离开视野时回调。
	 * Callback when an object leaves sight.
	 *
	 * @param object 离开视野的对象 / the object leaving sight
	 * @param isOutOfRange 是否因超出距离离开 / whether the leave is due to being out of range
	 */
	@Override
	public void notSee(VisibleObject object, boolean isOutOfRange) {
		super.notSee(object, isOutOfRange);
		if (object == getOwner().getTarget() && getOwner().getAi2().getSubState() != AISubState.TARGET_LOST) {
			getOwner().setTarget(null);
		}
	}

	/**
	 * 生物开始移动时执行的任务。
	 * Perform tasks when the creature starts moving.
	 *
	 */
	public void onStartMove() {
		getOwner().getObserveController().notifyMoveObservers();
		notifyAIOnMove();
	}

	/**
	 * 生物移动过程中执行的任务。
	 * Perform tasks while the creature is moving.
	 *
	 */
	public void onMove() {
		notifyAIOnMove();
		updateZone();
	}

	/**
	 * 生物停止移动时执行的任务。
	 * Perform tasks when the creature stops moving.
	 *
	 */
	public void onStopMove() {
		notifyAIOnMove();
	}

	/**
	 * 生物返回出生点时执行的任务。
	 * Perform tasks when the creature returns home.
	 *
	 */
	public void onReturnHome() {
	}

	/**
	 * 通知已知列表中的对象发生了移动事件。
	 * Notify everyone in the known list about the move event.
	 *
	 */
	protected void notifyAIOnMove() {
		GameMovementLoopServices.movementNotifyTask().add(getOwner());
	}

	/**
	 * 无论当前区域如何，完整刷新区域。
	 * Refresh zones completely irrespective of the current zone.
	 *
	 */
	public void refreshZoneImpl() {
		getOwner().revalidateZones();
	}

	/**
	 * 区域更新掩码管理。
	 * Zone update mask management.
	 *
	 */
	public final void updateZone() {
		GameMovementLoopServices.zoneUpdateService().add(getOwner());
	}

	/**
	 * 进入区域时回调。
	 * Callback when entering a zone.
	 *
	 * @param zoneInstance 进入的区域实例 / entered zone instance
	 */
	public void onEnterZone(ZoneInstance zoneInstance) {
	}

	/**
	 * 离开区域时回调。
	 * Callback when leaving a zone.
	 *
	 * @param zoneInstance 离开的区域实例 / left zone instance
	 */
	public void onLeaveZone(ZoneInstance zoneInstance) {
	}

	/**
	 * 生物死亡时的处理。
	 * Handles creature death.
	 *
	 * @param lastAttacker 最后攻击者 / last attacker
	 */
	public void onDie(Creature lastAttacker) {
		this.getOwner().getMoveController().abortMove();
		abortCast();
		this.getOwner().getEffectController().removeAllEffects();

		if (getOwner() instanceof Player && ((Player) getOwner()).getIsFlyingBeforeDeath()) {
			getOwner().unsetState(CreatureState.ACTIVE);
			getOwner().setState(CreatureState.FLOATING_CORPSE);
		} else {
			this.getOwner().setState(CreatureState.DEAD);
		}
		this.getOwner().getObserveController().notifyDeathObservers(lastAttacker);

	}

	/**
	 * 受到攻击时的完整处理（含技能与日志类型）。
	 * Full on-attack handling including skill and log type.
	 *
	 * @param attacker 攻击者 / attacker
	 * @param skillId 技能 ID / skill id
	 * @param type 伤害类型 / damage type
	 * @param damage 伤害值 / damage
	 * @param notifyAttack 是否通知攻击 / whether to notify attack
	 * @param log 日志类型 / log type
	 */
	public void onAttack(final Creature attacker, int skillId, TYPE type, int damage, boolean notifyAttack, LOG log) {
		onAttack(attacker, skillId, type, damage, notifyAttack, log, AttackStatus.NORMALHIT);
	}

	public void onAttack(final Creature attacker, int skillId, TYPE type, int damage, boolean notifyAttack, LOG log,
			AttackStatus attackStatus) {
		if (damage != 0 && !((getOwner() instanceof Npc) && ((Npc) getOwner()).isBoss())) {
			Skill skill = getOwner().getCastingSkill();
			if (skill != null && log != LOG.BLEED && log != LOG.SPELLATK && log != LOG.POISON) {
				if (skill.getSkillMethod() == SkillMethod.ITEM) {
					cancelCurrentSkill();
				} else {
					int cancelRate = skill.getSkillTemplate().getCancelRate();
					if (cancelRate == 100000) {
						cancelCurrentSkill();
					} else if (cancelRate > 0) {
						int conc = getOwner().getGameStats().getStat(StatEnum.CONCENTRATION, 0).getCurrent();
						float maxHp = getOwner().getGameStats().getMaxHp().getCurrent();

						float cancel = ((7f * (damage / maxHp) * 100f) - conc / 2f) * (cancelRate / 100f);
						if (Rnd.get(100) < cancel) {
							cancelCurrentSkill();
						}
					}
				}
			}
		}

		// 伤害为 0 且护盾存在时不要通知受击观察者。 / Do NOT notify attacked observers if the damage is 0 and shield is up (means
		// 攻击已被吸收） / the attack has been absorbed)
		if (damage == 0 && getOwner().getEffectController().isUnderShield()) {
			notifyAttack = false;
		}
		if (notifyAttack) {
			SkillTemplate attack = DataManager.SKILL_DATA.getSkillTemplate(skillId);
			boolean magical = attack != null ? attack.getType() == SkillType.MAGICAL : attacker.getAttackType().isMagical();
			getOwner().getObserveController().notifyAttackedObservers(attacker, magical);
		}

		// 将伤害降到恰好能确保死亡所需。 / Reduce the damage to exactly what is required to ensure death.
		// 重要：不要在……时计入 7000 伤害 / - Important that we don't include 7k worth of damage when the
		// 生物仅剩 100 生命。（用于仇恨列表伤害统计。） / creature only has 100 hp remaining. (For AggroList dmg count.)
		if (damage > getOwner().getLifeStats().getCurrentHp()) {
			damage = getOwner().getLifeStats().getCurrentHp() + 1;
		}
		getOwner().getAggroList().addDamage(attacker, damage, attackStatus);
		getOwner().getLifeStats().reduceHp(damage, attacker);
		if (damage > 0) {
			getOwner().getAi2().onDamaged(attacker, skillId);
		}

		if (getOwner() instanceof Npc) {
			AI2 ai = getOwner().getAi2();
			if (ai.poll(AIQuestion.CAN_SHOUT)) {
				if (attacker instanceof Player) {
					ShoutEventHandler.onHelp((NpcAI2) ai, attacker);
				} else {
					ShoutEventHandler.onEnemyAttack((NpcAI2) ai, attacker);
				}
			}
		} else if (getOwner() instanceof Player && attacker instanceof Npc) {
			AI2 ai = attacker.getAi2();
			if (ai.poll(AIQuestion.CAN_SHOUT)) {
				ShoutEventHandler.onAttack((NpcAI2) ai, getOwner());
			}
		}
		getOwner().incrementAttackedCount();

		// 通知周围所有 NPC：该生物正在攻击我 / notify all NPC's around that creature is attacking me
		getOwner().getKnownList().doOnAllNpcs(new Visitor<Npc>() {
			@Override
			public void visit(Npc object) {
				object.getAi2().onCreatureEvent(AIEventType.CREATURE_NEEDS_SUPPORT, getOwner());
				object.getAi2().onSeeAttack(attacker, getOwner());
			}
		});
	}

	/**
	 * 受到攻击的简化重载。
	 * Simplified on-attack overload.
	 *
	 * attacker
	 * skill id
	 * damage
	 * @param notifyAttack 是否通知攻击 / whether to notify attack
	 */
	public final void onAttack(Creature creature, int skillId, final int damage, boolean notifyAttack) {
		this.onAttack(creature, skillId, TYPE.REGULAR, damage, notifyAttack, LOG.REGULAR);
	}

	/**
	 * 受到攻击的简化重载（无技能）。
	 * Simplified on-attack overload without skill.
	 *
	 * attacker
	 * damage
	 * @param notifyAttack 是否通知攻击 / whether to notify attack
	 */
	public final void onAttack(Creature creature, final int damage, boolean notifyAttack) {
		this.onAttack(creature, 0, TYPE.REGULAR, damage, notifyAttack, LOG.REGULAR);
	}

	public final void onAttack(Creature creature, final int damage, boolean notifyAttack, AttackStatus attackStatus) {
		this.onAttack(creature, 0, TYPE.REGULAR, damage, notifyAttack, LOG.REGULAR, attackStatus);
	}

	/**
	 * 恢复生命/魔法等属性。
	 * Restores life/mp or similar stats.
	 *
	 * heal type
	 * @param value 恢复数值 / restore value
	 */
	public void onRestore(HealType hopType, int value) {
		switch (hopType) {
		case HP:
			getOwner().getLifeStats().increaseHp(TYPE.HP, value);
			break;
		case MP:
			getOwner().getLifeStats().increaseMp(TYPE.MP, value);
			break;
		case FP:
			getOwner().getLifeStats().increaseFp(TYPE.FP, value);
			break;
		default:
			break;
		}
	}

	/**
	 * 处理掉落。
	 * Handles drops.
	 *
	 * @param player 获得掉落的玩家 / player receiving drops
	 */
	public void doDrop(Player player) {
	}

	/**
	 * 处理击杀奖励。
	 * Handles kill rewards.
	 *
	 */
	public void doReward() {
	}

	/**
	 * 处理玩家对话请求。
	 * Handles a player dialog request.
	 *
	 * requesting player
	 */
	public void onDialogRequest(Player player) {
	}

	/**
	 * 获取简易攻击类型。
	 * Gets the simple attack type.
	 *
	 * @return attack type / 攻击类型 / attack type。
	 */
	public int getSimpleAttackType() {
		return this.SimpleAttackType;
	}

	/**
	 * 设置简易攻击类型。
	 * Sets the simple attack type.
	 *
	 * attack type
	 */
	public void setSimpleAttackType(int attackType) {
		this.SimpleAttackType = attackType;
	}

	/**
	 * 攻击指定目标。
	 * Attacks the specified target.
	 *
	 * attack target
	 * @param time 攻击时间参数 / attack timing parameter
	 */
	public void attackTarget(final Creature target, int time) {
		boolean addAttackObservers = true;
	/**
	 * 检查全部前置条件。
	 * Check all prerequisites
	 */
		if (target == null || !getOwner().canAttack() || getOwner().getLifeStats().isAlreadyDead()
				|| !getOwner().isSpawned()) {
			return;
		}

	/**
	 * 计算并应用伤害。
	 * Calculate and apply damage
	 */
		int attackType = 0;
		List<AttackResult> attackResult;
		if (getOwner().getAttackType() == ItemAttackType.PHYSICAL) {
			attackResult = AttackUtil.calculatePhysicalAttackResult(getOwner(), target);
		} else {
			attackResult = AttackUtil.calculateMagAttackResult(getOwner(), target,
					getOwner().getAttackType().getMagicalElement());
			attackType = 1;
		}
		int damage = 0;
		AttackStatus attackStatus = attackResult.isEmpty() ? AttackStatus.NORMALHIT : attackResult.getFirst().getAttackStatus();
		for (AttackResult result : attackResult) {
			if (result.getAttackStatus() == AttackStatus.RESIST || result.getAttackStatus() == AttackStatus.DODGE) {
				addAttackObservers = false;
			}
			damage += result.getDamage();
		}
		PacketSendUtility.broadcastPacketAndReceive(getOwner(), new SM_ATTACK(getOwner(), target,
				getOwner().getGameStats().getAttackCounter(), time, attackType, attackResult));

		getOwner().getGameStats().increaseAttackCounter();
		if (addAttackObservers) {
			getOwner().getObserveController().notifyAttackObservers(target, 0);
		}
		final Creature creature = getOwner();

		if (time == 0) {
			target.getController().onAttack(getOwner(), damage, true, attackStatus);
		} else {
			GameThreadPoolServices.threadPoolManager().schedule(new DelayedOnAttack(target, creature, damage, attackStatus), time);
		}
	}

	/**
	 * 停止移动。
	 * Stops movement.
	 *
	 */
	public void stopMoving() {
		Creature owner = getOwner();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().updatePosition(owner, owner.getX(), owner.getY(), owner.getZ(), owner.getHeading());
		PacketSendUtility.broadcastPacket(owner, new SM_MOVE(owner));
	}

	/**
	 * 处理对话选项选择。
	 * Handles dialog option selection.
	 *
	 * dialog id
	 * 玩家 / player
	 * quest id
	 * @param extendedRewardIndex 扩展奖励索引 / extended reward index
	 * @param unk 未知参数 / unknown parameter
	 */
	public void onDialogSelect(int dialogId, Player player, int questId, int extendedRewardIndex) {
	}

	/**
	 * 获取指定任务。
	 * Gets the task for the given id.
	 *
	 * task id
	 *
	 * @param taskId
	 * @return 任务 Future，可能为 null / task Future, may be null
	 */
	public Future<?> getTask(TaskId taskId) {
		return tasks.get(taskId.ordinal());
	}

	/**
	 * 是否存在指定任务。
	 * Whether a task with the given id exists.
	 *
	 * task id
	 *
	 * @param taskId true if present / 存在则为 true / true if present。
	 */
	public boolean hasTask(TaskId taskId) {
		return tasks.containsKey(taskId.ordinal());
	}

	/**
	 * 是否存在已调度且未完成的指定任务。
	 * Whether a scheduled unfinished task with the given id exists.
	 *
	 * task id
	 *
	 * @param taskId true if present / 存在则为 true / true if present。
	 */
	public boolean hasScheduledTask(TaskId taskId) {
		Future<?> task = tasks.get(taskId.ordinal());
		return task != null ? !task.isDone() : false;
	}

	/**
	 * 取消并移除指定任务。
	 * Cancels and removes the task with the given id.
	 *
	 * task id
	 *
	 * @param taskId cancelled Future / 被取消的 Future / cancelled Future。
	 */
	public Future<?> cancelTask(TaskId taskId) {
		Future<?> task = tasks.remove(taskId.ordinal());
		if (task != null) {
			task.cancel(false);
		}
		return task;
	}

	/**
	 * 添加或替换任务。
	 * Adds or replaces a task.
	 *
	 * task id
	 * task Future
	 */
	public void addTask(TaskId taskId, Future<?> task) {
		cancelTask(taskId);
		tasks.put(taskId.ordinal(), task);
	}

	/**
	 * 调度一个在执行前必须原子认领的任务；取消与执行只有一方可以成功。
	 * Schedules a task that must be atomically claimed before execution; only cancellation or execution can succeed.
	 *
	 * @param taskId 任务标识 / task id
	 * @param action 成功认领后执行的动作 / action executed after a successful claim
	 * @param delay 延迟毫秒 / delay in milliseconds
	 * @return 可用于观察或取消的受控任务 / controlled task for observation or cancellation
	 */
	public Future<?> scheduleTask(TaskId taskId, Runnable action, long delay) {
		TrackedTask task = new TrackedTask(taskId, action);
		Future<?> previous = tasks.put(taskId.ordinal(), task);
		if (previous != null) {
			previous.cancel(false);
		}
		try {
			scheduleTaskExecution(task, delay);
		} catch (RuntimeException | Error e) {
			tasks.remove(taskId.ordinal(), task);
			task.cancel(false);
			throw e;
		}
		return task;
	}

	/**
	 * 将受控任务提交到线程池；测试控制器可覆盖此调度边界。
	 * Submits a controlled task to the thread pool; test controllers may override this scheduling boundary.
	 *
	 * @param task 待调度任务 / task to schedule
	 * @param delay 延迟毫秒 / delay in milliseconds
	 */
	protected void scheduleTaskExecution(Runnable task, long delay) {
		GameThreadPoolServices.threadPoolManager().schedule(task, delay);
	}

	/**
	 * 仅当当前任务仍为预期任务时替换它，否则取消替换任务。
	 * Replaces a task only if it is still the expected task; otherwise cancels the replacement.
	 */
	public boolean replaceTask(TaskId taskId, Future<?> expected, Future<?> replacement) {
		boolean replaced = tasks.replace(taskId.ordinal(), expected, replacement);
		if (!replaced) {
			replacement.cancel(false);
		}
		return replaced;
	}

	/**
	 * 取消所有可取消任务。
	 * Cancels all cancellable tasks.
	 *
	 */
	public void cancelAllTasks() {
		while (hasCancellableTasks()) {
			for (Map.Entry<Integer, Future<?>> entry : new ArrayList<Map.Entry<Integer, Future<?>>>(tasks.entrySet())) {
				int i = entry.getKey();
				Future<?> task = entry.getValue();
				if (task != null && i != TaskId.RESPAWN.ordinal() && tasks.remove(i, task)) {
					task.cancel(false);
				}
			}
		}
	}

	private boolean hasCancellableTasks() {
		for (Integer taskId : tasks.keySet()) {
			if (taskId != TaskId.RESPAWN.ordinal()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 从世界中删除所有者。
	 * Deletes the owner from the world.
	 *
	 */
	@Override
	public void delete() {
		cancelAllTasks();
		super.delete();
	}

	/**
	 * 使所有者死亡。
	 * Kills the owner.
	 *
	 */
	public void die() {
		getOwner().getLifeStats().reduceHp(getOwner().getLifeStats().getCurrentHp() + 1, getOwner());
	}

	/**
	 * 使用默认等级的技能。
	 * Uses a skill at default level.
	 *
	 * skill id
	 *
	 * @param skillId whether successful / 是否成功 / whether successful。
	 */
	public final boolean useSkill(int skillId) {
		return useSkill(skillId, 1);
	}

	/**
	 * 使用指定等级的技能。
	 * Uses a skill at the given level.
	 *
	 * skill id
	 * skill level
	 *
	 * @return 是否成功 / whether successful, true if usage succeeded
	 */
	public boolean useSkill(int skillId, int skillLevel) {
		try {
			Creature creature = getOwner();
			Skill skill = GameEngineServices.skillEngine().getSkill(creature, skillId, skillLevel, creature.getTarget());
			if (skill != null) {
				return skill.useSkill();
			}
		} catch (Exception ex) {
			log.error(I18n.get("log.1f5bd67c5dae", skillId, ex));
		}
		return false;
	}

	/**
	 * 向仇恨列表广播仇恨值。
	 * Broadcasts hate value to the aggro list.
	 *
	 * hate value
	 */
	public void broadcastHate(int value) {
		for (VisibleObject visibleObject : getOwner().getKnownList().getKnownObjectsSnapshot()) {
			if (visibleObject instanceof Creature) {
				((Creature) visibleObject).getAggroList().notifyHate(getOwner(), value);
			}
		}
	}

	/**
	 * 中止当前施法。
	 * Aborts the current cast.
	 *
	 */
	public void abortCast() {
		Creature creature = getOwner();
		synchronized (creature) {
			Skill skill = creature.getCastingSkill();
			if (skill == null || !skill.tryCancelCast()) {
				return;
			}
			creature.clearCasting(skill);
			if (creature.getSkillNumber() > 0) {
				creature.setSkillNumber(creature.getSkillNumber() - 1);
			}
		}
	}

	/**
	 * 取消当前技能。
	 * Cancels the current skill.
	 *
	 */
	public void cancelCurrentSkill() {
		cancelCurrentSkill(getOwner().getCastingSkill());
	}

	/**
	 * 仅取消仍为当前施法的预期技能，并在成功取得取消权后通知客户端。
	 * Cancels only the expected current cast and notifies clients after cancellation ownership is acquired.
	 *
	 * @param expectedSkill 预期的当前技能 / expected current skill
	 * @return 是否成功取消 / whether cancellation succeeded
	 */
	public boolean cancelCurrentSkill(Skill expectedSkill) {
		if (expectedSkill == null) {
			return false;
		}
		Creature creature = getOwner();
		synchronized (creature) {
			if (creature.getCastingSkill() != expectedSkill || !expectedSkill.tryCancelCast()) {
				return false;
			}
			creature.removeSkillCoolDown(expectedSkill.getSkillTemplate().getDelayId());
			creature.clearCasting(expectedSkill);
		}
		PacketSendUtility.broadcastPacketAndReceive(creature,
				new SM_SKILL_CANCEL(creature, expectedSkill.getSkillTemplate().getSkillId()));
		if (getOwner().getAi2() instanceof NpcAI2) {
			NpcAI2 npcAI = (NpcAI2) getOwner().getAi2();
			npcAI.setSubStateIfNot(AISubState.NONE);
			npcAI.onGeneralEvent(AIEventType.ATTACK_COMPLETE);
			if (creature.getSkillNumber() > 0) {
				creature.setSkillNumber(creature.getSkillNumber() - 1);
			}
		}
		return true;
	}

	/**
	 * 取消物品使用，基类默认不处理。
	 * Cancels item use; the base implementation is a no-op.
	 *
	 */
	public void cancelUseItem() {
	}

	/**
	 * 消失时回调。
	 * Callback on despawn.
	 *
	 */
	@Override
	public void onDespawn() {
		if (terrainMaterialActor != null) {
			terrainMaterialActor.abort();
			getOwner().getObserveController().removeObserver(terrainMaterialActor);
			terrainMaterialActor = null;
		}
		cancelTask(TaskId.DECAY);

		Creature owner = getOwner();
		if (owner == null || !owner.isSpawned()) {
			return;
		}
		owner.getAggroList().clear();
		owner.getObserveController().clear();
	}

	private static final class DelayedOnAttack implements Runnable {

		private Creature target;
		private Creature creature;
		private int finalDamage;
		private final AttackStatus attackStatus;

		public DelayedOnAttack(Creature target, Creature creature, int finalDamage, AttackStatus attackStatus) {
			this.target = target;
			this.creature = creature;
			this.finalDamage = finalDamage;
			this.attackStatus = attackStatus;
		}

		@Override
		public void run() {
			target.getController().onAttack(creature, finalDamage, true, attackStatus);
			target = null;
			creature = null;
		}
	}

	/**
	 * 获取治疗技能增益倍率。
	 * Gets the healing skills boost multiplier.
	 *
	 * @return boost multiplier / 增益倍率 / boost multiplier。
	 */
	public float getHealingSkillsBoost() {
		return healingSkillBoost;
	}

	/**
	 * 设置治疗技能增益倍率。
	 * Sets the healing skills boost multiplier.
	 *
	 * @param value 增益倍率 / boost multiplier
	 */
	public void setHealingSkillsBoost(float value) {
		this.healingSkillBoost = value;
	}

	/**
	 * 生成后回调。
	 * Callback after spawn.
	 *
	 */
	@Override
	public void onAfterSpawn() {
		super.onAfterSpawn();
		getOwner().revalidateZones();
		if (terrainMaterialActor == null && GameWorldServices.geoService().worldHasTerrainMaterials(getOwner().getWorldId())) {
			terrainMaterialActor = new TerrainZoneCollisionMaterialActor(getOwner());
			getOwner().getObserveController().addObserver(terrainMaterialActor);
		}
	}

	/**
	 * 受控任务的原子生命周期。
	 * Atomic lifecycle for controlled tasks.
	 */
	private enum TrackedTaskState {
		WAITING, RUNNING, CANCELLED, DONE
	}

	/**
	 * 在从控制器任务表中成功移除自身后才执行的任务。
	 * Task that runs only after successfully removing itself from the controller task map.
	 */
	private final class TrackedTask extends CompletableFuture<Void> implements Runnable {
		private final int taskId;
		private final Runnable action;
		private final AtomicReference<TrackedTaskState> state = new AtomicReference<>(TrackedTaskState.WAITING);

		private TrackedTask(TaskId taskId, Runnable action) {
			this.taskId = taskId.ordinal();
			this.action = action;
		}

		@Override
		public void run() {
			if (!tasks.remove(taskId, this)
					|| !state.compareAndSet(TrackedTaskState.WAITING, TrackedTaskState.RUNNING)) {
				cancel(false);
				return;
			}
			try {
				action.run();
				complete(null);
			} catch (RuntimeException | Error e) {
				completeExceptionally(e);
				throw e;
			} finally {
				state.set(TrackedTaskState.DONE);
			}
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			if (!state.compareAndSet(TrackedTaskState.WAITING, TrackedTaskState.CANCELLED)) {
				return false;
			}
			tasks.remove(taskId, this);
			return super.cancel(false);
		}
	}
}
