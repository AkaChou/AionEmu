package com.aionemu.gameserver.ai2.manager;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AI2Logger;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * NPC 普通攻击管理器：调度并执行近战/远程普通攻击，含重复调度与射程检查。
 * NPC simple-attack manager: schedules and performs basic attacks with duplicate-schedule and range checks.
 *
 * @author ATracer Rework: Angry Catster
 */
public class SimpleAttackManager {

	/**
	 * 执行 NPC 普通攻击：防重复调度、校验射程后延迟或立即出手。
	 * Performs an NPC simple attack: guards against re-scheduling, validates range, then delays or attacks immediately.
	 *
	 * NPC AI instance
	 * @param delay 攻击延迟（毫秒） / attack delay in milliseconds
	 */
	public static void performAttack(NpcAI2 npcAI, int delay) {
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "performAttack");
		}

		// 检查是否已经调度了攻击，防止重复调度导致一次攻击多次伤害
		if (npcAI.getOwner().getGameStats().isNextAttackScheduled()) {
			if (npcAI.isLogging()) {
				AI2Logger.info(npcAI, "Attack already scheduled, scheduling checked attack");
			}
			// 如果已经调度了攻击，则安排带检查的攻击动作
			scheduleCheckedAttackAction(npcAI, delay);
			return;
		}

		if (!isTargetInAttackRange(npcAI.getOwner())) {
			if (npcAI.isLogging()) {
				AI2Logger.info(npcAI, "Attack will not be scheduled because of range");
			}
			npcAI.onGeneralEvent(AIEventType.TARGET_TOOFAR);
			return;
		}

		// 设置下次攻击时间，标记攻击已调度
		npcAI.getOwner().getGameStats().setNextAttackTime(System.currentTimeMillis() + delay);
		if (delay > 0) {
			// 延迟执行攻击
			GameThreadPoolServices.threadPoolManager().schedule(new SimpleAttackAction(npcAI), delay);
		} else {
			// 立即执行攻击
			attackAction(npcAI);
		}
	}

	/**
	 * 安排带检查的攻击动作（已有调度时使用）。
	 * Schedules a checked attack action when an attack is already scheduled.
	 *
	 * NPC AI instance
	 * @param delay 攻击延迟（毫秒） / attack delay in milliseconds
	 */
	private static void scheduleCheckedAttackAction(NpcAI2 npcAI, int delay) {
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "Scheduling checked attack " + delay);
		}
		// 安排带检查的攻击动作，在执行前会再次检查攻击是否已调度
		GameThreadPoolServices.threadPoolManager().schedule(new SimpleCheckedAttackAction(npcAI), delay);
	}

	/**
	 * 判断目标是否在攻击射程内（含存活、可见与几何范围）。
	 * Returns whether the target is in attack range (alive, visible, and within geometric range).
	 *
	 * attacking NPC
	 *
	 * @param npc
	 * @return 在射程内为 {@code true} / {@code true} if in attack range
	 */
    public static boolean isTargetInAttackRange(Npc npc) {
    if (npc == null) {
        return false;
    }

    if (npc.getTarget() == null) {
        return false;
    }

    if (!(npc.getTarget() instanceof Creature)) {
        return false;
    }

    Creature target = (Creature) npc.getTarget();

    if (!target.isSpawned() || target.getLifeStats() == null || target.getLifeStats().isAlreadyDead()) {
        return false;
    }

    if (npc.getAi2().isLogging()) {
        try {
            float distance = npc.getDistanceToTarget();
            AI2Logger.info((AbstractAI) npc.getAi2(), "isTargetInAttackRange: " + distance);
        } catch (Exception e) {
        }
    }

    try {
        if (!GameWorldServices.geoService().canSee(npc, target)) {
            return false;
        }
    } catch (NullPointerException e) {
        if (npc.getAi2().isLogging()) {
            AI2Logger.info((AbstractAI) npc.getAi2(), "GeoService.canSee NPE: " + e.getMessage());
        }
        return false;
    }

    try {
        return MathUtil.isInAttackRange(npc, target, npc.getGameStats().getAttackRange().getCurrent() / 1000f);
    } catch (NullPointerException e) {
        if (npc.getAi2().isLogging()) {
            AI2Logger.info((AbstractAI) npc.getAi2(), "MathUtil.isInAttackRange NPE: " + e.getMessage());
        }
        return false;
        }
    }

	/**
	 * 执行实际的攻击动作；目标无效或过远时触发相应 AI 事件。
	 * Performs the actual attack action; fires AI events if the target is invalid or too far.
	 *
	 * NPC AI instance
	 */
    protected static void attackAction(final NpcAI2 npcAI) {
    if (!npcAI.isInState(AIState.FIGHT)) {
        return;
    }
    if (npcAI.isLogging()) {
        AI2Logger.info(npcAI, "attackAction");
    }

    Npc npc = npcAI.getOwner();

    if (npc == null) {
        npcAI.onGeneralEvent(AIEventType.TARGET_GIVEUP);
        return;
    }

    Creature target = (Creature) npc.getTarget();

    if (target == null) {
        npcAI.onGeneralEvent(AIEventType.TARGET_GIVEUP);
        return;
    }

    if (target.getLifeStats() == null || target.getLifeStats().isAlreadyDead() || !target.isSpawned()) {
        npcAI.onGeneralEvent(AIEventType.TARGET_GIVEUP);
        return;
    }

    if (isTargetInAttackRange(npc)) {
        try {
            if (npc.canSee(target)) {
                npc.getController().attackTarget(target, 0);
                npcAI.onGeneralEvent(AIEventType.ATTACK_COMPLETE);
                return;
            }
        } catch (NullPointerException e) {
            if (npcAI.isLogging()) {
                AI2Logger.info(npcAI, "Error in canSee check: " + e.getMessage());
            }
        }
    }

    npcAI.onGeneralEvent(AIEventType.TARGET_TOOFAR);
    }

	/**
	 * 普通攻击动作：到时执行实际攻击。
	 * Simple attack action: runs the actual attack when due.
	 */
	private final static class SimpleAttackAction implements Runnable {
		private NpcAI2 npcAI;
		SimpleAttackAction(NpcAI2 npcAI) {
			this.npcAI = npcAI;
		}

		@Override
		public void run() {
			attackAction(npcAI);
			npcAI = null;
		}
	}

	/**
	 * 带检查的攻击动作：执行前再次确认是否已调度，防止重复攻击。
	 * Checked attack action: re-checks whether an attack is already scheduled before hitting.
	 */
	private final static class SimpleCheckedAttackAction implements Runnable {
		private NpcAI2 npcAI;
		SimpleCheckedAttackAction(NpcAI2 npcAI) {
			this.npcAI = npcAI;
		}

		@Override
		public void run() {
			// 执行前再次检查攻击是否已调度，防止重复攻击
			if (!npcAI.getOwner().getGameStats().isNextAttackScheduled()) {
				attackAction(npcAI);
			} else {
				if (npcAI.isLogging()) {
					AI2Logger.info(npcAI, "Scheduled checked attacked confirmed");
				}
			}
			npcAI = null;
		}
	}
}
