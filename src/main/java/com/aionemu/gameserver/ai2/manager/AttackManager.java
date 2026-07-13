package com.aionemu.gameserver.ai2.manager;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2Logger;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.AttackIntention;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.handler.TargetEventHandler;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;

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
		npcAI.getOwner().getMoveController().clearHomeReturn();
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
		if (stopRetailChase(npcAI)) {
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
		if (stopRetailChase(npcAI)) {
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
	 * 按真端 NPC 数据检查是否停止追击。
	 * Checks whether chase should stop according to retail NPC data.
	 *
	 * NPC AI instance
	 *
	 * @param npcAI
	 * @return 应放弃目标时为 {@code true} / {@code true} if the target should be given up
	 */
	private static boolean checkStopChase(NpcAI2 npcAI) {
		Npc npc = npcAI.getOwner();
		double distanceToHome = npc.getDistanceToSpawnLocation();
		return shouldStopRetailChase(npc, distanceToHome, System.currentTimeMillis());
	}

	private static boolean stopRetailChase(NpcAI2 npcAI) {
		if (!checkStopChase(npcAI)) {
			return false;
		}
		if (npcAI.getOwner().getMoveController().isReturningToWaypoint()) {
			npcAI.getOwner().getAggroList().clear();
			npcAI.onGeneralEvent(AIEventType.TARGET_GIVEUP);
		} else {
			TargetEventHandler.returnToSpawn(npcAI, false);
		}
		return true;
	}

	private static boolean shouldStopRetailChase(Npc npc, double distanceToHome, long now) {
		var definition = DataManager.NPC_PATH_BEHAVIOR_DATA == null ? null
			: DataManager.NPC_PATH_BEHAVIOR_DATA.get(npc.getNpcId());
		String maxChaseTime = definition == null ? null : definition.maxChaseTime();
		if (maxChaseTime == null || maxChaseTime.isBlank() || "0".equals(maxChaseTime)) {
			return false;
		}
		if (!"sp".equalsIgnoreCase(maxChaseTime)) {
			try {
				boolean stop = shouldStopTimedChase(npc.getGameStats().getFightStartingTime(),
						npc.getGameStats().getLastAttackTime(), Integer.parseInt(maxChaseTime), now);
				if (stop) {
					npc.getMoveController().requestReturnToCurrentWaypoint();
				}
				return stop;
			} catch (NumberFormatException ignored) {
				return false;
			}
		}
		if (distanceToHome < 3) {
			npc.getGameStats().setLastSpawnPointChaseCheck(0);
			return false;
		}
		long lastCheck = npc.getGameStats().getLastSpawnPointChaseCheck();
		if (lastCheck == 0) {
			npc.getGameStats().setLastSpawnPointChaseCheck(now);
			return false;
		}
		if (!shouldCheckSpawnPointChase(lastCheck, now)) {
			return false;
		}
		npc.getGameStats().setLastSpawnPointChaseCheck(now);
		return shouldStopSpawnPointChase(Rnd.get(1, 100));
	}

	static boolean shouldStopTimedChase(long fightStartedAt, long lastAttackAt, int maxSeconds, long now) {
		long chaseRefreshedAt = Math.max(fightStartedAt, lastAttackAt);
		return maxSeconds > 0 && chaseRefreshedAt > 0 && now - chaseRefreshedAt >= maxSeconds * 1000L;
	}

	static boolean shouldCheckSpawnPointChase(long lastCheck, long now) {
		return lastCheck > 0 && now - lastCheck >= 2_000;
	}

	static boolean shouldStopSpawnPointChase(int roll) {
		return roll > 69;
	}

}
