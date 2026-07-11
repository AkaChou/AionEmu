package com.aionemu.gameserver.ai2.manager;

import com.aionemu.gameserver.ai2.AI2Logger;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.AttackIntention;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * NPC 攻击管理器：负责攻击调度、意图选择与追击/放弃目标判定。
 * NPC attack manager: schedules attacks, chooses attack intention, and handles chase/give-up logic.
 *
 * @author ATracer
 * @modified Yon (Aion Reconstruction Project) -- removed non-retail-like leash in {@link #checkGiveupDistance(NpcAI2)}.
 */
public class AttackManager {

	/**
	 * 开始攻击目标：记录开战时间、播放攻击表情并调度下一次攻击。
	 * Starts attacking the target: records fight start time, plays attack emote, and schedules the next attack.
	 *
	 * NPC AI instance
	 */
	public static void startAttacking(NpcAI2 npcAI) {
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "AttackManager: startAttacking");
		}
		npcAI.getOwner().getGameStats().setFightStartingTime();
		EmoteManager.emoteStartAttacking(npcAI.getOwner());
		scheduleNextAttack(npcAI);
	}

	/**
	 * 安排下一次攻击；含重复调度检查，避免一次攻击多次伤害。
	 * Schedules the next attack; includes a duplicate-schedule guard to avoid multi-hit from one attack.
	 *
	 * NPC AI instance
	 */
	public static void scheduleNextAttack(NpcAI2 npcAI) {
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "AttackManager: scheduleNextAttack");
		}
		if (checkGiveupDistance(npcAI)) {
			npcAI.onGeneralEvent(AIEventType.TARGET_GIVEUP);
			return;
		}

		// 检查是否已经调度了攻击，防止重复调度
		if (npcAI.getOwner().getGameStats().isNextAttackScheduled()) {
			if (npcAI.isLogging()) {
				AI2Logger.info(npcAI, "Attack already scheduled, skipping");
			}
			return;
		}

		// 施法子状态中不开始攻击 / don't start attack while in casting substate
		AISubState subState = npcAI.getSubState();
		if (subState == AISubState.NONE) {
			chooseAttack(npcAI, npcAI.getOwner().getGameStats().getNextAttackInterval());
		} else {
			if (npcAI.isLogging()) {
				AI2Logger.info(npcAI, "Will not choose attack in substate" + subState);
			}
		}
	}

	/**
	 * 按攻击意图选择普通攻击、技能攻击或结束攻击。
	 * Chooses simple attack, skill attack, or finish-attack based on attack intention.
	 *
	 * NPC AI instance
	 * @param delay 攻击延迟（毫秒） / attack delay in milliseconds
	 */
	protected static void chooseAttack(NpcAI2 npcAI, int delay) {
		AttackIntention attackIntention = npcAI.chooseAttackIntention();
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "AttackManager: chooseAttack " + attackIntention + " delay " + delay);
		}
		if (!npcAI.canThink()) {
			return;
		}
		switch (attackIntention) {
			case SIMPLE_ATTACK:
				// 普通攻击
				SimpleAttackManager.performAttack(npcAI, delay);
				break;
			case SKILL_ATTACK:
				// 技能攻击
				SkillAttackManager.performAttack(npcAI, delay);
				break;
			case FINISH_ATTACK:
				// 结束攻击，进入思考状态
				npcAI.think();
				break;
			default:
				break;
		}
	}

	/**
	 * 目标过远时的处理：切换仇恨目标、丢失视野、放弃目标或追击移动。
	 * Handles target-too-far: switch to most hated, vision loss, give up, or chase-move.
	 *
	 * NPC AI instance
	 */
	public static void targetTooFar(NpcAI2 npcAI) {
		Npc npc = npcAI.getOwner();
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "AttackManager: attackTimeDelta " + npc.getGameStats().getLastAttackTimeDelta());
		}

		// 如果有更仇恨的目标，切换到那个目标
		if (npc.getGameStats().getLastChangeTargetTimeDelta() > 5) {
			Creature mostHated = npc.getAggroList().getMostHated();
			if (mostHated != null && !mostHated.getLifeStats().isAlreadyDead()
					&& !npc.isTargeting(mostHated.getObjectId())) {
				if (npcAI.isLogging()) {
					AI2Logger.info(npcAI, "AttackManager: switching target during chase");
				}
				npcAI.onCreatureEvent(AIEventType.TARGET_CHANGED, mostHated);
				return;
			}
		}
		// 无法看到目标，2秒后仍未恢复视野才放弃目标
		if (!npc.canSee((Creature) npc.getTarget())) {
			if (npcAI.setSubStateIfNot(AISubState.TARGET_LOST)) {
				GameThreadPoolServices.threadPoolManager().schedule(() -> {
					if (npcAI.isInSubState(AISubState.TARGET_LOST) && npc.isSpawned() && !npcAI.isAlreadyDead()) {
						npcAI.onGeneralEvent(AIEventType.TARGET_GIVEUP);
					}
				}, 2000);
			}
			return;
		}
		// 检查是否应该放弃目标
		if (checkGiveupDistance(npcAI)) {
			npcAI.onGeneralEvent(AIEventType.TARGET_GIVEUP);
			return;
		}
		// 尝试移动到目标
		if (npcAI.isMoveSupported()){
			npc.getMoveController().moveToTargetObject();
			return;
		}
		npcAI.onGeneralEvent(AIEventType.TARGET_GIVEUP);
	}

	/**
	 * 检查是否应因距离（追击目标/出生点）放弃目标。
	 * Checks whether the target should be given up by chase-target or home distance.
	 *
	 * NPC AI instance
	 *
	 * @param npcAI @return 应放弃目标时为 {@code true} / {@code true} if the target should be given up
	 */
	private static boolean checkGiveupDistance(NpcAI2 npcAI) {
		Npc npc = npcAI.getOwner();
		// 若目标跑得太远 / if target run away too far
		VisibleObject target = npc.getTarget();
		if (target != null) {
			double distanceToTarget = MathUtil.getDistance(npc, target);
			if (npcAI.isLogging()) {
				AI2Logger.info(npcAI, "AttackManager: distanceToTarget " + distanceToTarget);
			}
			int chaseTarget = npc.isBoss() ? 50 : npc.getPosition().getWorldMapInstance().getTemplate().getAiInfo().getChaseTarget();
			if (distanceToTarget >= chaseTarget) {
				return true;
			}
		}

		double distanceToHome = npc.getDistanceToSpawnLocation();
		int chaseHome = npc.isBoss() ? 150 : npc.getPosition().getWorldMapInstance().getTemplate().getAiInfo().getChaseHome();
		return shouldGiveUpByHomeDistance(distanceToHome, chaseHome, npc.getGameStats().getLastAttackTimeDelta(),
			npc.getGameStats().getLastAttackedTimeDelta());
	}

	/**
	 * 按出生点距离与最近攻击/受击时间判定是否脱战放弃。
	 * Decides give-up by home distance and last attack/attacked time deltas.
	 *
	 * @param distanceToHome 到出生点的距离 / distance to spawn location
	 * @param chaseHome 追击出生点上限 / chase-home limit
	 * @param lastAttackTimeDelta 距上次攻击的秒数 / seconds since last attack
	 * @param lastAttackedTimeDelta 距上次受击的秒数 / seconds since last attacked
	 * @return 应放弃时为 {@code true} / {@code true} if should give up
	 */
	static boolean shouldGiveUpByHomeDistance(double distanceToHome, int chaseHome, int lastAttackTimeDelta,
			int lastAttackedTimeDelta) {
		if (distanceToHome > chaseHome) {
			return true;
		}
		return chaseHome <= 200 && ((lastAttackTimeDelta > 20 && lastAttackedTimeDelta > 20)
			|| (distanceToHome > chaseHome / 2.0 && lastAttackedTimeDelta > 10));
	}
}
