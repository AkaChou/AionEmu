package com.aionemu.gameserver.ai2;

import com.aionemu.gameserver.ai2.handler.ActivateEventHandler;
import com.aionemu.gameserver.ai2.handler.DiedEventHandler;
import com.aionemu.gameserver.ai2.handler.ShoutEventHandler;
import com.aionemu.gameserver.ai2.handler.SpawnEventHandler;
import com.aionemu.gameserver.ai2.poll.AIAnswer;
import com.aionemu.gameserver.ai2.poll.AIAnswers;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.ai2.poll.NpcAIPolls;
import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.controllers.attack.AggroList;
import com.aionemu.gameserver.controllers.effect.EffectController;
import com.aionemu.gameserver.controllers.movement.NpcMoveController;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.skill.NpcSkillList;
import com.aionemu.gameserver.model.stats.container.NpcLifeStats;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.knownlist.KnownList;

/**
 * 通用 NPC AI：封装 NPC 数据访问，并处理激活/刷新/死亡/喊话等基础事件。
 * General NPC AI: wraps NPC data access and handles basic activate/spawn/death/shout events.
 *
 * @author ATracer
 */
@AIName("npc")
public class NpcAI2 extends AITemplate {

	/**
	 * 获取 NPC 所有者。
	 * Returns the NPC owner.
	 *
	 * NPC owner
	 */
	@Override
	public Npc getOwner() {
		return (Npc) super.getOwner();
	}

	/**
	 * 获取 NPC 模板。
	 * Returns the NPC object template.
	 *
	 * NPC template
	 */
	protected NpcTemplate getObjectTemplate() {
		return getOwner().getObjectTemplate();
	}

	/**
	 * 获取刷新模板。
	 * Returns the spawn template.
	 *
	 * spawn template
	 */
	protected SpawnTemplate getSpawnTemplate() {
		return getOwner().getSpawn();
	}

	/**
	 * 获取生命状态。
	 * Returns the life stats.
	 *
	 * life stats
	 */
	protected NpcLifeStats getLifeStats() {
		return getOwner().getLifeStats();
	}

	/**
	 * 获取种族。
	 * Returns the race.
	 *
	 * @return 阵营 / race
	 */
	protected Race getRace() {
		return getOwner().getRace();
	}

	/**
	 * 获取部落。
	 * Returns the tribe.
	 *
	 * tribe
	 */
	protected TribeClass getTribe() {
		return getOwner().getTribe();
	}

	/**
	 * 获取效果控制器。
	 * Returns the effect controller.
	 *
	 * @return 效果控制器 / effect controller
	 */
	protected EffectController getEffectController() {
		return getOwner().getEffectController();
	}

	/**
	 * 获取已知对象列表。
	 * Returns the known list.
	 *
	 * known list
	 */
	protected KnownList getKnownList() {
		return getOwner().getKnownList();
	}

	/**
	 * 获取仇恨列表。
	 * Returns the aggro list.
	 *
	 * aggro list
	 */
	protected AggroList getAggroList() {
		return getOwner().getAggroList();
	}

	/**
	 * 获取技能列表。
	 * Returns the NPC skill list.
	 *
	 * skill list
	 */
	protected NpcSkillList getSkillList() {
		return getOwner().getSkillList();
	}

	/**
	 * 获取创建者对象。
	 * Returns the creator object.
	 *
	 * creator
	 */
	protected VisibleObject getCreator() {
		return getOwner().getCreator();
	}

	/**
	 * 获取移动控制器（已不推荐直接使用，移动应由 AI 命令驱动）。
	 * Returns the move controller (deprecated: movements should be AI-driven commands).
	 *
	 * @return 移动控制器 / move controller
	 */
	protected NpcMoveController getMoveController() {
		return getOwner().getMoveController();
	}

	/**
	 * 获取 NPC 模板 ID。
	 * Returns the NPC template id.
	 *
	 * NPC id
	 */
	protected int getNpcId() {
		return getOwner().getNpcId();
	}

	/**
	 * 获取创建者 ID。
	 * Returns the creator id.
	 *
	 * creator id
	 */
	protected int getCreatorId() {
		return getOwner().getCreatorId();
	}

	/**
	 * 判断与指定对象是否在三维距离范围内。
	 * Returns whether the given object is within 3D range.
	 *
	 * target object
	 * range
	 *
	 * @return 是否在范围内 / whether in range
	 */
	protected boolean isInRange(VisibleObject object, int range) {
		return MathUtil.isIn3dRange(getOwner(), object, range);
	}

	/**
	 * 处理激活事件。
	 * Handles the activate event.
	 */
	@Override
	protected void handleActivate() {
		ActivateEventHandler.onActivate(this);
	}

	/**
	 * 处理停用事件。
	 * Handles the deactivate event.
	 */
	@Override
	protected void handleDeactivate() {
		ActivateEventHandler.onDeactivate(this);
	}

	/**
	 * 处理刷新完成事件。
	 * Handles the spawned event.
	 */
	@Override
	protected void handleSpawned() {
		SpawnEventHandler.onSpawn(this);
	}

	/**
	 * 处理重生事件。
	 * Handles the respawned event.
	 */
	@Override
	protected void handleRespawned() {
		SpawnEventHandler.onRespawn(this);
	}

	/**
	 * 处理消失事件（可先触发喊话）。
	 * Handles the despawned event (may shout first).
	 */
	@Override
	protected void handleDespawned() {
		if (poll(AIQuestion.CAN_SHOUT)) {
			ShoutEventHandler.onBeforeDespawn(this);
		}
		SpawnEventHandler.onDespawn(this);
	}

	/**
	 * 处理死亡事件。
	 * Handles the died event.
	 */
	@Override
	protected void handleDied() {
		DiedEventHandler.onSimpleDie(this);
	}

	/**
	 * 处理到达路径点事件（可触发喊话）。
	 * Handles move-arrived (may trigger path-point shout).
	 */
	@Override
	protected void handleMoveArrived() {
		if (!poll(AIQuestion.CAN_SHOUT) || getSpawnTemplate().getWalkerId() == null) {
			return;
		}
		ShoutEventHandler.onReachedWalkPoint(this);
	}

	/**
	 * 处理目标变更事件（可触发喊话）。
	 * Handles target-changed (may trigger shout).
	 *
	 * new target
	 */
	@Override
	protected void handleTargetChanged(Creature creature) {
		super.handleMoveArrived();
		if (!poll(AIQuestion.CAN_SHOUT)) {
			return;
		}
		ShoutEventHandler.onSwitchedTarget(this, creature);
	}

	/**
	 * 按问题返回 NPC 特有投票答案。
	 * Returns NPC-specific poll answers for the given question.
	 *
	 * AI question
	 *
	 * @param question @return 答案，未知问题返回 null / answer, or null if unknown
	 */
	@Override
	protected AIAnswer pollInstance(AIQuestion question) {
		switch (question) {
		case SHOULD_DECAY:
			return NpcAIPolls.shouldDecay(this);
		case SHOULD_RESPAWN:
			return NpcAIPolls.shouldRespawn(this);
		case SHOULD_REWARD:
			return AIAnswers.POSITIVE;
		case CAN_SHOUT:
			return isMayShout() ? AIAnswers.POSITIVE : AIAnswers.NEGATIVE;
		default:
			return null;
		}
	}

	/**
	 * 判断是否允许喊话。
	 * Returns whether shouting is allowed.
	 *
	 * @return 是否可喊话 / whether may shout
	 */
	@Override
	public boolean isMayShout() {
		// 临时修复；因继承关系不应依赖它 / temp fix, we shouldn't rely on it because of inheritance
		if (AIConfig.SHOUTS_ENABLE) {
			return getOwner().mayShout(0);
		}
		return false;
	}

	/**
	 * 判断是否支持移动（有移速且未冻结）。
	 * Returns whether movement is supported (has speed and not frozen).
	 *
	 * @return 是否可移动 / whether move is supported
	 */
	public boolean isMoveSupported() {
		return getOwner().getGameStats().getMovementSpeedFloat() > 0 && !this.isInSubState(AISubState.FREEZE);
	}
}
