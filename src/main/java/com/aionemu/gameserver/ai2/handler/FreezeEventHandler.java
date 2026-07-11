package com.aionemu.gameserver.ai2.handler;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.model.gameobjects.Npc;

/**
 * 冻结事件处理器，负责 AI 进入 / 解除冻结子状态时的行走与清理。
 * Handles freeze events: walking and cleanup when AI enters or leaves the FREEZE sub-state.
 */
public class FreezeEventHandler {

	/**
	 * 解除冻结：清除 FREEZE 子状态，按需恢复行走并触发思考。
	 * Unfreezes AI: clears FREEZE sub-state, restores walking if needed, and triggers think.
	 *
	 * @param ai AI 实例 / AI instance
	 */
	public static void onUnfreeze(AbstractAI ai) {
		if (ai.isInSubState(AISubState.FREEZE)) {
			ai.setSubStateIfNot(AISubState.NONE);
			if (ai instanceof NpcAI2) {
				Npc npc = ((NpcAI2) ai).getOwner();
				if (npc.getWalkerGroup() != null) {
					ai.setStateIfNot(AIState.WALKING);
					ai.setSubStateIfNot(AISubState.WALK_WAIT_GROUP);
				} else if (npc.getSpawn().getRandomWalk() > 0) {
					ai.setStateIfNot(AIState.WALKING);
					ai.setSubStateIfNot(AISubState.WALK_RANDOM);
				}
				npc.updateKnownlist();
			}
			ai.think();
		}
	}

	/**
	 * 进入冻结：停止行走，设为空闲 + FREEZE，并清理仇恨与效果。
	 * Freezes AI: stops walking, sets IDLE + FREEZE, and clears aggro and effects.
	 *
	 * @param ai AI 实例 / AI instance
	 */
	public static void onFreeze(AbstractAI ai) {
		if (ai.isInState(AIState.WALKING)) {
			WalkManager.stopWalking((NpcAI2) ai);
		}
		ai.setStateIfNot(AIState.IDLE);
		ai.setSubStateIfNot(AISubState.FREEZE);
		ai.think();
		if (ai instanceof NpcAI2) {
			Npc npc = ((NpcAI2) ai).getOwner();
			npc.updateKnownlist();
			npc.getAggroList().clear();
			npc.getEffectController().removeAllEffects();
		}
	}
}
