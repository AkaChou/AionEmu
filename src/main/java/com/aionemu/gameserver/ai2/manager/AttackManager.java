package com.aionemu.gameserver.ai2.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2Logger;
import com.aionemu.gameserver.ai2.AIState;
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
 * @modified Yon (Aion Reconstruction Project) -- 移除非真端式的超距脱战处理 / removed the non-retail-like leash handling.
 */
public class AttackManager {

	/**
	 * 等待执行的攻击链复位任务，按对象 ID 去重：连续受击与连续重试都只保留一个待执行任务。
	 * Pending attack-chain retry tasks keyed by object id: repeated hits and retries keep at most one queued task.
	 */
	private static final Map<Integer, Future<?>> PENDING_ATTACK_RETRIES = new ConcurrentHashMap<>();

	/**
	 * 不可移动 NPC 重试的最小延迟（毫秒）：够不着目标时避免空转式重排。
	 * Minimum retry delay in milliseconds for immobile NPCs: avoids busy-loop rescheduling while out of reach.
	 */
	private static final int IMMOBILE_RETRY_MIN_DELAY = 500;

	/**
	 * 开始攻击目标：记录开战时间、播放攻击表情并调度下一次攻击。
	 * Starts attacking the target: records fight start time, plays attack emote, and schedules the next attack.
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
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
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 */
	public static void scheduleNextAttack(NpcAI2 npcAI) {
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "AttackManager: scheduleNextAttack");
		}
		if (stopRetailChase(npcAI)) {
			return;
		}

		// 检查是否已经调度了攻击，防止重复调度
		// Check whether an attack is already scheduled to prevent duplicate scheduling
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
	 * @param npcAI NPC AI 实例 / NPC AI instance
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
				// Simple attack
				SimpleAttackManager.performAttack(npcAI, delay);
				break;
			case SKILL_ATTACK:
				// 技能攻击
				// Skill attack
				SkillAttackManager.performAttack(npcAI, delay);
				break;
			case FINISH_ATTACK:
				// 结束攻击，进入思考状态
				// Finish attacking and enter think state
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
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 */
	public static void targetTooFar(NpcAI2 npcAI) {
		Npc npc = npcAI.getOwner();
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "AttackManager: attackTimeDelta " + npc.getGameStats().getLastAttackTimeDelta());
		}

		// 如果有更仇恨的目标，切换到那个目标
		// Switch to the target with higher aggro if present
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
		// 保留丢失目标，继续追踪；连续寻路失败由真实 react_to_pathfind_fail 处理。
		// Keep the lost target and continue tracking; repeated pathfinding failures are handled by retail react_to_pathfind_fail.
		if (!npc.canSee((Creature) npc.getTarget())) {
			npcAI.setSubStateIfNot(AISubState.TARGET_LOST);
		}
		// 检查是否应该放弃目标
		// Check whether the target should be given up
		if (stopRetailChase(npcAI)) {
			return;
		}
		// 尝试移动到目标
		// Try to move toward the target
		if (npcAI.isMoveSupported()) {
			npc.getMoveController().moveToTargetObject();
			return;
		}
		// 不可移动的 NPC（卵、固定炮台等）不因“够不着”放弃目标：真端数据里没有这个驱动
		// （0 移速不会产生寻路失败、max_chase_time=0 不设追击超时、pattern 在进入战斗时 do_nothing），
		// 而放弃会清空仇恨，并被下一次受击/视野事件立刻重新拉进战斗，客户端就会反复播放脱战表现。
		// Immobile NPCs (eggs, fixed turrets) must not abandon an unreachable target: retail data defines no such
		// driver (zero move speed cannot fail a path, max_chase_time=0 sets no chase timeout, and the pattern does
		// nothing on entering the attack state). Giving up clears hate only to be re-added by the next hit or sight
		// event, which makes the client replay the disengage animation over and over.
		// 但“保留目标”不等于“放弃出手”：可移动 NPC 靠追击到达事件复位攻击链，0 移速 NPC 没有这条路径，
		// 于是会停在 FIGHT 里既不移动也不还手（0 移速远程怪 Lurking Clamshell Oculis 235805 即为此例）。
		// 这里按攻击间隔重排一次尝试，直到目标重新进入射程/视线，或真端 max_chase_time 规则结束战斗。
		// Keeping the target must not mean dropping the attack chain: mobile NPCs resume through the chase-arrival
		// event, immobile ones have no such path and would sit in FIGHT without moving or retaliating (the zero-speed
		// ranged monster Lurking Clamshell Oculis 235805 is such a case). Retry once per attack interval until the
		// target is back in range/sight or the retail max_chase_time rule ends the fight.
		scheduleImmobileRetry(npcAI);
	}

	/**
	 * 受击后复位可能已经停摆的攻击链：异步执行，且按对象 ID 去重。
	 * Resumes a possibly stalled attack chain after a hit: asynchronous and deduplicated per object id.
	 *
	 * <p>严禁在受击调用栈内同步重排攻击。{@code AggroList#addDamageInternal} → {@code AbstractAI#onAttacked}
	 * → {@code AttackEventHandler#onAttack} → {@code AttackManager#scheduleNextAttack}
	 * → {@code SimpleAttackManager#attackAction} → {@code CreatureController#attackTarget}
	 * → 对方的 {@code AggroList#addDamageInternal} 会再次回到本方法；双方普攻间隔为 0 时就会来回无限递归，
	 * 直到 {@code StackOverflowError} 打爆线程池线程。
	 * Never reschedule synchronously inside the hit stack: damage handling → {@code onAttacked} → this handler
	 * → {@code scheduleNextAttack} → attack action → target's damage handling returns here, so two creatures with a
	 * zero attack delay recurse until the worker thread dies with {@code StackOverflowError}.</p>
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 */
	public static void resumeInterruptedAttack(NpcAI2 npcAI) {
		scheduleAttackRetry(npcAI, 0);
	}

	/**
	 * 为不可移动 NPC 按攻击间隔重排一次攻击尝试。
	 * Reschedules one attack attempt for an immobile NPC after the regular attack interval.
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 */
	private static void scheduleImmobileRetry(NpcAI2 npcAI) {
		Npc npc = npcAI.getOwner();
		if (npc == null) {
			return;
		}
		scheduleAttackRetry(npcAI, Math.max(IMMOBILE_RETRY_MIN_DELAY, npc.getGameStats().getNextAttackInterval()));
	}

	/**
	 * 在 AI 线程池上按延迟排入一次攻击链复位，按对象 ID 去重。
	 * Queues one attack-chain retry on the AI thread pool after the given delay, deduplicated per object id.
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 * @param delay 延迟毫秒 / delay in milliseconds
	 */
	private static void scheduleAttackRetry(NpcAI2 npcAI, int delay) {
		Npc npc = npcAI.getOwner();
		if (npc == null || !npc.isSpawned() || npc.getLifeStats().isAlreadyDead() || npc.getTarget() == null
				|| !npcAI.isInState(AIState.FIGHT)) {
			return;
		}
		int objectId = npc.getObjectId();
		PENDING_ATTACK_RETRIES.compute(objectId, (ignored, pending) -> {
			if (!shouldQueueAttackRetry(pending)) {
				if (npcAI.isLogging()) {
					AI2Logger.info(npcAI, "AttackManager: attack retry already queued");
				}
				return pending;
			}
			return GameThreadPoolServices.threadPoolManager().schedule(() -> runAttackRetry(npcAI, objectId), delay);
		});
	}

	/**
	 * 去重规则：只有在途任务为空或已结束时才允许排入新的重试。
	 * Deduplication rule: queue a new retry only when no in-flight task exists or the in-flight one already finished.
	 *
	 * @param pending 在途任务 / in-flight task
	 * @return 允许入队时为 {@code true} / {@code true} when a new retry may be queued
	 */
	static boolean shouldQueueAttackRetry(Future<?> pending) {
		return pending == null || pending.isDone();
	}

	/**
	 * 执行一次攻击链复位（始终运行在 AI 线程池线程上）。
	 * Runs one attack-chain retry (always on an AI thread-pool thread).
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 * @param objectId 排入任务时的对象 ID / object id captured when the task was queued
	 */
	private static void runAttackRetry(NpcAI2 npcAI, int objectId) {
		// 先撤销自身占位，后续重排出的下一次重试才能重新入队。
		// Drop the own slot first so the follow-up retry can queue again.
		PENDING_ATTACK_RETRIES.remove(objectId);
		Npc npc = npcAI.getOwner();
		if (npc == null || !npc.isSpawned() || npc.getLifeStats().isAlreadyDead() || npc.getTarget() == null) {
			return;
		}
		if (npcAI.isInState(AIState.FIGHT)) {
			if (npcAI.isLogging()) {
				AI2Logger.info(npcAI, "AttackManager: retrying stalled attack chain");
			}
			TargetEventHandler.clearTargetLostState(npcAI);
			scheduleNextAttack(npcAI);
		}
	}

	/**
	 * 按真实 NPC 数据检查是否停止追击。
	 * Checks whether chase should stop according to retail NPC data.
	 *
	 * @return NPC AI 实例 / NPC AI instance
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
						npc.getGameStats().getLastAttackTime(), npc.getGameStats().getLastAttackedTime(),
						Integer.parseInt(maxChaseTime), now);
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

	static boolean shouldStopTimedChase(long fightStartedAt, long lastAttackAt, long lastAttackedAt, int maxSeconds, long now) {
		long chaseRefreshedAt = Math.max(Math.max(fightStartedAt, lastAttackAt), lastAttackedAt);
		return maxSeconds > 0 && chaseRefreshedAt > 0 && now - chaseRefreshedAt >= maxSeconds * 1000L;
	}

	static boolean shouldCheckSpawnPointChase(long lastCheck, long now) {
		return lastCheck > 0 && now - lastCheck >= 2_000;
	}

	static boolean shouldStopSpawnPointChase(int roll) {
		return roll > 69;
	}

}
