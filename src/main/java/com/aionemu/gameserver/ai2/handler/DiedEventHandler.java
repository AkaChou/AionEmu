package com.aionemu.gameserver.ai2.handler;

import com.aionemu.gameserver.ai2.AI2Logger;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.model.gameobjects.Npc;

/**
 * 死亡事件处理器，负责 NPC 死亡时的喊话、状态切换与仇恨清理。
 * Handles death events: shout on death, state transition, and aggro cleanup.
 *
 * @author ATracer
 */
public class DiedEventHandler {

	/**
	 * 处理完整死亡流程：执行简化死亡逻辑并清空目标。
	 * Handles full death flow: runs simple death logic and clears the target.
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 */
	public static void onDie(NpcAI2 npcAI) {
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "onDie");
		}
		onSimpleDie(npcAI);

		Npc owner = npcAI.getOwner();
		owner.setTarget(null);
	}

	/**
	 * 处理简化死亡：可选喊话，切换到 DIED 状态并清空仇恨。
	 * Handles simple death: optional shout, switches to DIED state, and clears aggro.
	 *
	 * @param npcAI NPC AI 实例 / NPC AI instance
	 */
	public static void onSimpleDie(NpcAI2 npcAI) {
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "onSimpleDie");
		}

		if (npcAI.poll(AIQuestion.CAN_SHOUT)) {
			ShoutEventHandler.onDied(npcAI);
		}
		npcAI.setStateIfNot(AIState.DIED);
		npcAI.setSubStateIfNot(AISubState.NONE);
		npcAI.getOwner().getAggroList().clear();
	}
}
