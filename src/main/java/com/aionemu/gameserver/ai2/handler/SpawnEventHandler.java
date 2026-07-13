package com.aionemu.gameserver.ai2.handler;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.NpcAI2;

/**
 * 刷新 / 消失 / 重生事件处理器。
 * Handles spawn, despawn, and respawn events.
 *
 * @author ATracer
 */
public class SpawnEventHandler {

	/**
	 * 刷新时：若区域活跃则切到空闲并触发思考。
	 * On spawn: switches to idle and thinks when the map region is active.
	 *
	 * NPC AI instance
	 */
	public static void onSpawn(NpcAI2 npcAI) {
		if (npcAI.setStateIfNot(AIState.IDLE)) {
			if (npcAI.getOwner().getPosition().isMapRegionActive()) {
				npcAI.think();
			}
		}
	}

	/**
	 * 消失时：将状态设为 DESPAWNED。
	 * On despawn: sets state to DESPAWNED.
	 *
	 * NPC AI instance
	 */
	public static void onDespawn(NpcAI2 npcAI) {
		npcAI.setStateIfNot(AIState.DESPAWNED);
	}

	/**
	 * 重生时：重置移动控制器。
	 * On respawn: resets the move controller.
	 *
	 * NPC AI instance
	 */
	public static void onRespawn(NpcAI2 npcAI) {
		npcAI.getOwner().getMoveController().clearHomeReturn();
		npcAI.getOwner().getMoveController().resetMove();
	}
}
