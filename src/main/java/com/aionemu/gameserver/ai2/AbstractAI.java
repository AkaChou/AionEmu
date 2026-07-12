package com.aionemu.gameserver.ai2;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.aionemu.commons.callbacks.metadata.ObjectCallback;
import com.aionemu.gameserver.ai2.event.AIEventLog;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.eventcallback.OnHandleAIGeneralEvent;
import com.aionemu.gameserver.ai2.handler.FollowEventHandler;
import com.aionemu.gameserver.ai2.handler.FreezeEventHandler;
import com.aionemu.gameserver.ai2.manager.SimpleAttackManager;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.ai2.poll.AIAnswer;
import com.aionemu.gameserver.ai2.poll.AIAnswers;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.ai2.scenario.AI2Scenario;
import com.aionemu.gameserver.ai2.scenario.AI2Scenarios;
import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemAttackType;
import com.aionemu.gameserver.model.templates.npcshout.ShoutEventType;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.WorldPosition;
import com.google.common.base.Preconditions;

/**
 * AI2 抽象基类：管理状态、事件分发、思考锁、场景与通用钩子。
 * Abstract AI2 base: manages state, event dispatch, think lock, scenario and common hooks.
 *
 * @author ATracer
 */
public abstract class AbstractAI implements AI2 {

	private Creature owner;
	private AIState currentState;
	private AISubState currentSubState;
	private final Lock thinkLock = new ReentrantLock();
	private boolean logging = false;
	protected int skillId;
	protected int skillLevel;
	private volatile AIEventLog eventLog;
	private AI2Scenario scenario;

	/**
	 * 构造 AI，初始化为 CREATED/NONE 并清空场景。
	 * Constructs the AI, initializing CREATED/NONE state and clearing scenario.
	 */
	AbstractAI() {
		this.currentState = AIState.CREATED;
		this.currentSubState = AISubState.NONE;
		clearScenario();
	}

	/**
	 * 获取当前 AI 场景。
	 * Returns the current AI scenario.
	 *
	 * scenario
	 */
	public AI2Scenario getScenario() {
		return scenario;
	}

	/**
	 * 设置 AI 场景。
	 * Sets the AI scenario.
	 *
	 * scenario
	 */
	public void setScenario(AI2Scenario scenario) {
		this.scenario = scenario;
	}

	/**
	 * 清空场景为无场景。
	 * Clears the scenario to no-scenario.
	 */
	public void clearScenario() {
		this.scenario = AI2Scenarios.NO_SCENARIO;
	}

	/**
	 * 获取事件日志（调试用）。
	 * Returns the event log (for debugging).
	 *
	 * event log
	 */
	public AIEventLog getEventLog() {
		return eventLog;
	}

	/**
	 * 获取当前主状态。
	 * Returns the current main state.
	 *
	 * main state
	 */
	@Override
	public AIState getState() {
		return currentState;
	}

	/**
	 * 判断是否处于指定主状态。
	 * Returns whether the AI is in the given main state.
	 *
	 * @param state 目标状态 / target state
	 * whether matching
	 */
	public final boolean isInState(AIState state) {
		return currentState == state;
	}

	/**
	 * 获取当前子状态。
	 * Returns the current sub-state.
	 *
	 * sub-state
	 */
	@Override
	public AISubState getSubState() {
		return currentSubState;
	}

	/**
	 * 判断是否处于指定子状态。
	 * Returns whether the AI is in the given sub-state.
	 *
	 * @param subState 目标子状态 / target sub-state
	 * whether matching
	 */
	public final boolean isInSubState(AISubState subState) {
		return currentSubState == subState;
	}

	/**
	 * 获取 AI 名称（来自 {@link AIName}，否则 "noname"）。
	 * Returns the AI name from {@link AIName}, or "noname".
	 *
	 * AI name
	 */
	@Override
	public String getName() {
		if (getClass().isAnnotationPresent(AIName.class)) {
			AIName annotation = getClass().getAnnotation(AIName.class);
			return annotation.value();
		}
		return "noname";
	}

	/**
	 * 获取当前技能 ID。
	 * Returns the current skill id.
	 *
	 * skill id
	 */
	public int getSkillId() {
		return skillId;
	}

	/**
	 * 获取当前技能等级。
	 * Returns the current skill level.
	 *
	 * skill level
	 */
	public int getSkillLevel() {
		return skillLevel;
	}

	/**
	 * 判断当前状态是否允许处理该事件。
	 * Returns whether the current state allows handling the given event.
	 *
	 * event type
	 *
	 * @param eventType
	 * @return 是否可处理 / whether handleable
	 */
	protected boolean canHandleEvent(AIEventType eventType) {
		switch (this.currentState) {
		case DESPAWNED:
			return StateEvents.DESPAWN_EVENTS.hasEvent(eventType);
		case DIED:
			return StateEvents.DEAD_EVENTS.hasEvent(eventType);
		case CREATED:
			return StateEvents.CREATED_EVENTS.hasEvent(eventType);
		default:
			break;
		}
		switch (eventType) {
		case DIALOG_START:
		case DIALOG_FINISH:
			return isNonFightingState();
		case CREATURE_MOVED:
			return getName().equals("trap") || currentState != AIState.FIGHT && isNonFightingState();
		default:
			break;
		}
		return true;
	}

	/**
	 * 判断是否处于非战斗状态（WALKING 或 IDLE）。
	 * Returns whether the AI is in a non-fighting state (WALKING or IDLE).
	 *
	 * @return 是否非战斗 / whether non-fighting
	 */
	public boolean isNonFightingState() {
		return currentState == AIState.WALKING || currentState == AIState.IDLE;
	}

	/**
	 * 若状态不同则切换主状态。
	 * Changes the main state if it differs from the current one.
	 *
	 * new state
	 *
	 * @param newState
	 * @return 是否发生切换 / whether the state changed
	 */
	public synchronized boolean setStateIfNot(AIState newState) {
		if (this.currentState == newState) {
			if (this.isLogging()) {
				AI2Logger.info(this, "Can't change state to " + newState + " from " + currentState);
			}
			return false;
		}
		if (this.isLogging()) {
			AI2Logger.info(this, "Setting AI state to " + newState);
			if (this.currentState == AIState.DIED && newState == AIState.FIGHT) {
				StackTraceElement[] stack = new Throwable().getStackTrace();
				for (StackTraceElement elem : stack) {
					AI2Logger.info(this, elem.toString());
				}
			}
		}
		this.currentState = newState;
		return true;
	}

	/**
	 * 若子状态不同则切换子状态。
	 * Changes the sub-state if it differs from the current one.
	 *
	 * new sub-state
	 *
	 * @param newSubState
	 * @return 是否发生切换 / whether the sub-state changed
	 */
	public synchronized boolean setSubStateIfNot(AISubState newSubState) {
		if (this.currentSubState == newSubState) {
			if (this.isLogging()) {
				AI2Logger.info(this, "Can't change substate to " + newSubState + " from " + currentSubState);
			}
			return false;
		}
		if (this.isLogging()) {
			AI2Logger.info(this, "Setting AI substate to " + newSubState);
		}
		this.currentSubState = newSubState;
		return true;
	}

	/**
	 * 接收通用事件并在允许时分发处理。
	 * Receives a general event and dispatches it when allowed.
	 *
	 * @param event 事件类型 / event type
	 */
	@Override
	public void onGeneralEvent(AIEventType event) {
		if (canHandleEvent(event)) {
			if (this.isLogging()) {
				AI2Logger.info(this, "General event " + event);
			}
			handleGeneralEvent(event);
		}
	}

	/**
	 * 接收生物相关事件并在允许时分发处理。
	 * Receives a creature event and dispatches it when allowed.
	 *
	 * @param event 事件类型 / event type
	 * related creature
	 */
	@Override
	public void onCreatureEvent(AIEventType event, Creature creature) {
		Preconditions.checkNotNull(creature, "Creature must not be null");
		if (canHandleEvent(event)) {
			if (this.isLogging()) {
				AI2Logger.info(this, "Creature event " + event + ": " + creature.getObjectTemplate().getTemplateId());
			}
			handleCreatureEvent(event, creature);
		}
	}

	/**
	 * 接收自定义事件并分发处理。
	 * Receives a custom event and dispatches it.
	 *
	 * event id
	 * arguments
	 */
	@Override
	public void onCustomEvent(int eventId, Object... args) {
		if (this.isLogging()) {
			AI2Logger.info(this, "Custom event - id = " + eventId);
		}
		handleCustomEvent(eventId, args);
	}

	/**
	 * 获取 AI 所有者生物（NpcAI2 以下可隐藏更具体类型）。
	 * Returns the AI owner creature (more specific types are hidden below NpcAI2).
	 *
	 * owner
	 */
	public Creature getOwner() {
		return owner;
	}

	/**
	 * 获取所有者对象 ID。
	 * Returns the owner's object id.
	 *
	 * object id
	 */
	public int getObjectId() {
		return owner.getObjectId();
	}

	/**
	 * 获取世界坐标。
	 * Returns the world position.
	 *
	 * world position
	 */
	public WorldPosition getPosition() {
		return owner.getPosition();
	}

	/**
	 * 获取当前目标。
	 * Returns the current target.
	 *
	 * target object
	 */
	public VisibleObject getTarget() {
		return owner.getTarget();
	}

	/**
	 * 判断所有者是否已死亡。
	 * Returns whether the owner is already dead.
	 *
	 * @return 是否已死亡 / whether already dead
	 */
	public boolean isAlreadyDead() {
		return owner.getLifeStats().isAlreadyDead();
	}

	/**
	 * 绑定所有者生物。
	 * Binds the owner creature.
	 *
	 * owner
	 */
	void setOwner(Creature owner) {
		this.owner = owner;
	}

	/**
	 * 尝试获取思考锁（非阻塞）。
	 * Tries to acquire the think lock (non-blocking).
	 *
	 * @return 是否获取成功 / whether acquired
	 */
	public final boolean tryLockThink() {
		return thinkLock.tryLock();
	}

	/**
	 * 释放思考锁。
	 * Releases the think lock.
	 */
	public final void unlockThink() {
		thinkLock.unlock();
	}

	/**
	 * 是否开启调试日志。
	 * Returns whether debug logging is enabled.
	 *
	 * whether logging
	 */
	@Override
	public final boolean isLogging() {
		return logging;
	}

	/**
	 * 设置调试日志开关。
	 * Sets the debug logging flag.
	 *
	 * whether enable
	 */
	public void setLogging(boolean logging) {
		this.logging = logging;
	}

	/** 处理激活 / Handle activate */
	protected abstract void handleActivate();

	/** 处理停用 / Handle deactivate */
	protected abstract void handleDeactivate();

	/** 处理刷新完成 / Handle spawned */
	protected abstract void handleSpawned();

	/** 处理重生 / Handle respawned */
	protected abstract void handleRespawned();

	/** 处理消失 / Handle despawned */
	protected abstract void handleDespawned();

	/** 处理死亡 / Handle died */
	protected abstract void handleDied();

	/** 处理移动校验 / Handle move validate */
	protected abstract void handleMoveValidate();

	/** 处理到达路径点 / Handle move arrived */
	protected abstract void handleMoveArrived();

	/** 处理攻击完成 / Handle attack complete */
	protected abstract void handleAttackComplete();

	/** 处理结束攻击 / Handle finish attack */
	protected abstract void handleFinishAttack();

	/** 处理到达目标 / Handle target reached */
	protected abstract void handleTargetReached();

	/** 处理目标过远 / Handle target too far */
	protected abstract void handleTargetTooFar();

	/** 处理放弃目标 / Handle target giveup */
	protected abstract void handleTargetGiveup();

	/** 处理不在出生点 / Handle not at home */
	protected abstract void handleNotAtHome();

	/** 处理返回出生点 / Handle back home */
	protected abstract void handleBackHome();

	/** 处理掉落注册完成 / Handle drop registered */
	protected abstract void handleDropRegistered();

	/** 处理被攻击 / Handle attack */
	protected abstract void handleAttack(Creature creature);

	/** 处理生物需要支援 / Handle creature needs support */
	protected abstract boolean handleCreatureNeedsSupport(Creature creature);

	/** 处理守卫反击攻击者 / Handle guard against attacker */
	protected abstract boolean handleGuardAgainstAttacker(Creature creature);

	/** 处理看见生物 / Handle creature see */
	protected abstract void handleCreatureSee(Creature creature);

	/** 处理看不见生物 / Handle creature not see */
	protected abstract void handleCreatureNotSee(Creature creature);

	/** 处理生物移动 / Handle creature moved */
	protected abstract void handleCreatureMoved(Creature creature);

	/** 处理生物仇恨 / Handle creature aggro */
	protected abstract void handleCreatureAggro(Creature creature);

	/** 处理目标变更 / Handle target changed */
	protected abstract void handleTargetChanged(Creature creature);

	/** 处理开始跟随 / Handle follow me */
	protected abstract void handleFollowMe(Creature creature);

	/** 处理停止跟随 / Handle stop follow me */
	protected abstract void handleStopFollowMe(Creature creature);

	/** 处理对话开始 / Handle dialog start */
	protected abstract void handleDialogStart(Player player);

	/** 处理对话结束 / Handle dialog finish */
	protected abstract void handleDialogFinish(Player player);

	/** 处理自定义事件 / Handle custom event */
	protected abstract void handleCustomEvent(int eventId, Object... args);

	/**
	 * 处理模式喊话。
	 * Handles a pattern shout.
	 *
	 * @param event 喊话事件 / shout event
	 * pattern
	 * skill number
	 * whether handled
	 */
	public abstract boolean onPatternShout(ShoutEventType event, String pattern, int skillNumber);

	/**
	 * 分发通用事件到具体处理器（带回调注解）。
	 * Dispatches a general event to the concrete handler (with callback annotation).
	 *
	 * @param event 事件类型 / event type
	 */
	@ObjectCallback(OnHandleAIGeneralEvent.class)
	protected void handleGeneralEvent(AIEventType event) {
		if (this.isLogging()) {
			AI2Logger.info(this, "Handle general event " + event);
		}
		logEvent(event);
		switch (event) {
		case MOVE_VALIDATE:
			handleMoveValidate();
			break;
		case MOVE_ARRIVED:
			handleMoveArrived();
			break;
		case SPAWNED:
			handleSpawned();
			break;
		case RESPAWNED:
			handleRespawned();
			break;
		case DESPAWNED:
			handleDespawned();
			break;
		case DIED:
			handleDied();
			break;
		case ATTACK_COMPLETE:
			handleAttackComplete();
			break;
		case ATTACK_FINISH:
			handleFinishAttack();
			break;
		case TARGET_REACHED:
			handleTargetReached();
			break;
		case TARGET_TOOFAR:
			handleTargetTooFar();
			break;
		case TARGET_GIVEUP:
			handleTargetGiveup();
			break;
		case NOT_AT_HOME:
			handleNotAtHome();
			break;
		case BACK_HOME:
			handleBackHome();
			break;
		case ACTIVATE:
			handleActivate();
			break;
		case DEACTIVATE:
			handleDeactivate();
			break;
		case FREEZE:
			FreezeEventHandler.onFreeze(this);
			break;
		case UNFREEZE:
			FreezeEventHandler.onUnfreeze(this);
			break;
		case DROP_REGISTERED:
			handleDropRegistered();
			break;
		default:
			break;
		}
	}

	/**
	 * 在开启 EVENT_DEBUG 时记录事件。
	 * Logs the event when EVENT_DEBUG is enabled.
	 *
	 * @param event 事件类型 / event type
	 */
	protected void logEvent(AIEventType event) {
		if (AIConfig.EVENT_DEBUG) {
			if (eventLog == null) {
				synchronized (this) {
					if (eventLog == null) {
						eventLog = new AIEventLog(10);
					}
				}
			}
			eventLog.addFirst(event);
		}
	}

	/**
	 * 分发生物相关事件到具体处理器。
	 * Dispatches a creature event to the concrete handler.
	 *
	 * @param event 事件类型 / event type
	 * related creature
	 */
	void handleCreatureEvent(AIEventType event, Creature creature) {
		switch (event) {
		case ATTACK:
			if (DataManager.TRIBE_RELATIONS_DATA.isFriendlyRelation(getOwner().getTribe(), creature.getTribe())) {
				return;
			}
			handleAttack(creature);
			logEvent(event);
			break;
		case CREATURE_NEEDS_SUPPORT:
			if (!handleCreatureNeedsSupport(creature)) {
				if (creature.getTarget() instanceof Creature) {
					if (!handleCreatureNeedsSupport((Creature) creature.getTarget())
							&& !handleGuardAgainstAttacker(creature)) {
						handleGuardAgainstAttacker((Creature) creature.getTarget());
					}
				}
			}
			logEvent(event);
			break;
		case CREATURE_SEE:
			handleCreatureSee(creature);
			break;
		case CREATURE_NOT_SEE:
			handleCreatureNotSee(creature);
			break;
		case CREATURE_MOVED:
			handleCreatureMoved(creature);
			break;
		case CREATURE_AGGRO:
			handleCreatureAggro(creature);
			logEvent(event);
			break;
		case TARGET_CHANGED:
			handleTargetChanged(creature);
			break;
		case FOLLOW_ME:
			handleFollowMe(creature);
			logEvent(event);
			break;
		case STOP_FOLLOW_ME:
			handleStopFollowMe(creature);
			logEvent(event);
			break;
		case DIALOG_START:
			handleDialogStart((Player) creature);
			logEvent(event);
			break;
		case DIALOG_FINISH:
			handleDialogFinish((Player) creature);
			logEvent(event);
			break;
		default:
			break;
		}
	}

	/**
	 * 投票式查询：先问实例，再处理通用问题。
	 * Polls a question: instance first, then common questions.
	 *
	 * AI question
	 * whether positive
	 */
	@Override
	public boolean poll(AIQuestion question) {
		AIAnswer instanceAnswer = pollInstance(question);
		if (instanceAnswer != null) {
			return instanceAnswer.isPositive();
		}
		switch (question) {
		case DESTINATION_REACHED:
			return isDestinationReached();
		case CAN_SPAWN_ON_DAYTIME_CHANGE:
			return isCanSpawnOnDaytimeChange();
		case CAN_SHOUT:
			return isMayShout();
		default:
			break;
		}
		return false;
	}

	/**
	 * 向具体 AI 实例投票；无特有答案时返回 null。
	 * Polls the concrete AI instance; returns null when no specific answer.
	 *
	 * AI question
	 * answer or null
	 */
	protected AIAnswer pollInstance(AIQuestion question) {
		return null;
	}

	/**
	 * 询问问题，默认否定。
	 * Asks a question; defaults to negative.
	 *
	 * AI question
	 * AI answer
	 */
	@Override
	public AIAnswer ask(AIQuestion question) {
		return AIAnswers.NEGATIVE;
	}

	/**
	 * 按当前状态判断是否已到达目的地。
	 * Returns whether the destination is reached based on current state.
	 *
	 * whether reached
	 */
	protected boolean isDestinationReached() {
		AIState state = currentState;
		switch (state) {
		case FEAR:
			return MathUtil.isNearCoordinates(getOwner(), owner.getMoveController().getTargetX2(),
					owner.getMoveController().getTargetY2(), owner.getMoveController().getTargetZ2(), 1);
		case FIGHT:
			return SimpleAttackManager.isTargetInAttackRange((Npc) owner);
		case RETURNING:
			SpawnTemplate spawn = getOwner().getSpawn();
			return MathUtil.isNearCoordinates(getOwner(), spawn.getX(), spawn.getY(), spawn.getZ(), 1);
		case FOLLOWING:
			return FollowEventHandler.isInRange(this, getOwner().getTarget());
		case WALKING:
			return currentSubState == AISubState.TALK || WalkManager.isArrivedAtPoint((NpcAI2) this);
		default:
			break;
		}
		return true;
	}

	/**
	 * 昼夜切换时是否允许刷新（仅 DESPAWNED/CREATED）。
	 * Returns whether spawn on daytime change is allowed (only DESPAWNED/CREATED).
	 *
	 * whether allowed
	 */
	protected boolean isCanSpawnOnDaytimeChange() {
		return currentState == AIState.DESPAWNED || currentState == AIState.CREATED;
	}

	/**
	 * 是否允许喊话。
	 * Returns whether shouting is allowed.
	 *
	 * @return 是否可喊话 / whether may shout
	 */
	public abstract boolean isMayShout();

	/**
	 * 选择攻击意图。
	 * Chooses the next attack intention.
	 *
	 * attack intention
	 */
	public abstract AttackIntention chooseAttackIntention();

	/**
	 * 处理对话框选择，默认不处理。
	 * Handles dialog select; not handled by default.
	 *
	 * always false
	 */
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		return false;
	}

	/**
	 * 获取剩余时间，默认 0。
	 * Returns remaining time; 0 by default.
	 *
	 * remaining time
	 */
	@Override
	public long getRemainigTime() {
		return 0;
	}

	/**
	 * 在与所有者相同世界/实例中刷新对象。
	 * Spawns an object in the same world/instance as the AI owner.
	 *
	 * NPC id
	 * @param x X 坐标 / x
	 * @param y Y 坐标 / y
	 * @param z Z 坐标 / z
	 * 朝向 / heading
	 * spawned object
	 */
	protected VisibleObject spawn(int npcId, float x, float y, float z, byte heading) {
		return spawn(owner.getWorldId(), npcId, x, y, z, heading, 0, getPosition().getInstanceId());
	}

	/**
	 * 在与所有者相同世界/实例中刷新带 entityId 的对象。
	 * Spawns an object with entityId in the same world/instance as the AI owner.
	 *
	 * NPC id
	 * @param x X 坐标 / x
	 * @param y Y 坐标 / y
	 * @param z Z 坐标 / z
	 * 朝向 / heading
	 * entity id
	 * spawned object
	 */
	protected VisibleObject spawn(int npcId, float x, float y, float z, byte heading, int entityId) {
		return spawn(owner.getWorldId(), npcId, x, y, z, heading, entityId, getPosition().getInstanceId());
	}

	/**
	 * 按完整参数刷新对象。
	 * Spawns an object with full parameters.
	 *
	 * 世界 ID / world id
	 * NPC id
	 * @param x X 坐标 / x
	 * @param y Y 坐标 / y
	 * @param z Z 坐标 / z
	 * 朝向 / heading
	 * entity id
	 * instance id
	 * spawned object
	 */
	protected VisibleObject spawn(int worldId, int npcId, float x, float y, float z, byte heading, int entityId,
			int instanceId) {
		SpawnTemplate template = SpawnEngine.addNewSingleTimeSpawn(worldId, npcId, x, y, z, heading);
		template.setEntityId(entityId);
		return SpawnEngine.spawnObject(template, instanceId);
	}

	/**
	 * 修改对自身伤害，默认原样返回。
	 * Modifies incoming damage; returns original by default.
	 */
	@Override
	public int modifyDamage(int damage) {
		return damage;
	}

	/**
	 * 修改所有者造成的伤害，默认原样返回。
	 * Modifies owner damage; returns original by default.
	 */
	@Override
	public int modifyOwnerDamage(int damage) {
		return damage;
	}

	/**
	 * 处理其他 NPC 的个体事件，默认空实现。
	 * Handles individual NPC event; empty by default.
	 */
	@Override
	public void onIndividualNpcEvent(Creature npc) {
	}

	/**
	 * 修改治疗值，默认原样返回。
	 * Modifies heal value; returns original by default.
	 */
	@Override
	public int modifyHealValue(int value) {
		return value;
	}

	/**
	 * 修改命中/精准值，默认原样返回。
	 * Modifies maccuracy value; returns original by default.
	 */
	@Override
	public int modifyMaccuracy(int value) {
		return value;
	}

	/**
	 * 修改感知范围，默认原样返回。
	 * Modifies sensory range; returns original by default.
	 */
	@Override
	public int modifySensoryRange(int value) {
		return value;
	}

	/**
	 * 修改攻击类型，默认原样返回。
	 * Modifies attack type; returns original by default.
	 */
	@Override
	public ItemAttackType modifyAttackType(ItemAttackType type) {
		return type;
	}
}
