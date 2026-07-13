package com.aionemu.gameserver.ai2.handler;

import com.aionemu.gameserver.ai2.AI2Logger;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;

/**
 * 思考事件处理器，按 AI 状态在战斗 / 行走 / 空闲间调度决策，含非活跃区域逻辑。
 * walking / idle, including inactive-region logic.
 *
 * @author ATracer
 */
public class ThinkEventHandler {

	/**
	 * 主思考入口：加锁后按状态调度攻击、行走或空闲思考；非活跃区域走专用逻辑。
	 * walking / idle think; uses inactive-region logic when needed.
	 *
	 * NPC AI instance
	 */
	public static void onThink(NpcAI2 npcAI) {
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "think");
		}
		if (npcAI.isAlreadyDead()) {
			AI2Logger.info(npcAI, "can't think in dead state");
			return;
		}
		if (!npcAI.tryLockThink()) {
			AI2Logger.info(npcAI, "can't acquire lock");
			return;
		}
		try {
			if (!npcAI.getOwner().getPosition().isMapRegionActive() || npcAI.getSubState() == AISubState.FREEZE) {
				thinkInInactiveRegion(npcAI);
				return;
			}
			if (npcAI.isLogging()) {
				AI2Logger.info(npcAI, "think state " + npcAI.getState());
			}
			switch (npcAI.getState()) {
			case FIGHT:
				thinkAttack(npcAI);
				break;
			case WALKING:
				thinkWalking(npcAI);
				break;
			case IDLE:
				thinkIdle(npcAI);
				break;
			default:
				break;
			}
		} finally {
			npcAI.unlockThink();
		}
	}

	/**
	 * 非活跃区域思考：战斗时继续攻击决策，否则若不在出生点则触发归家。
	 * Thinks in inactive region: continues attack decisions in fight, otherwise fires return-home if off spawn.
	 *
	 * NPC AI instance
	 */
	private static void thinkInInactiveRegion(NpcAI2 npcAI) {
		if (!npcAI.canThink()) {
			return;
		}
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "think in inactive region: " + npcAI.getState());
		}
		switch (npcAI.getState()) {
		case FIGHT:
			thinkAttack(npcAI);
			break;
		default:

			if (!npcAI.getOwner().isAtSpawnLocation()) {
				npcAI.onGeneralEvent(AIEventType.NOT_AT_HOME);
			}
		}
	}

	/**
	 * 战斗思考：锁定最高仇恨目标，或结束攻击并触发归家 / 不在家事件。
	 * not-at-home events.
	 *
	 * NPC AI instance
	 */
	public static void thinkAttack(NpcAI2 npcAI) {
		Npc npc = npcAI.getOwner();
		Creature mostHated = npc.getAggroList().getMostHated();
		if (mostHated != null && !mostHated.getLifeStats().isAlreadyDead()) {
			npcAI.onCreatureEvent(AIEventType.TARGET_CHANGED, mostHated);
		} else {
			npc.getMoveController().requestReturnToCurrentWaypoint();
			npcAI.onGeneralEvent(AIEventType.ATTACK_FINISH);
			npcAI.onGeneralEvent(shouldReturn(npc.getMoveController().isReturningToWaypoint(), npc.isAtSpawnLocation())
					? AIEventType.NOT_AT_HOME : AIEventType.BACK_HOME);
		}
	}

	static boolean shouldReturn(boolean returningToWaypoint, boolean atSpawn) {
		return returningToWaypoint || !atSpawn;
	}

	/**
	 * 行走思考：启动行走管理器。
	 * Walking think: starts the walk manager.
	 *
	 * NPC AI instance
	 */
	public static void thinkWalking(NpcAI2 npcAI) {
		WalkManager.startWalking(npcAI);
	}

	/**
	 * 空闲思考：若应行走则启动行走，失败则保持空闲。
	 * Idle think: starts walking when applicable, otherwise stays idle.
	 *
	 * NPC AI instance
	 */
	public static void thinkIdle(NpcAI2 npcAI) {
		if (WalkManager.isWalking(npcAI)) {
			boolean startedWalking = WalkManager.startWalking(npcAI);
			if (!startedWalking) {
				npcAI.setStateIfNot(AIState.IDLE);
			}
		}
	}
}
