package com.aionemu.gameserver.ai2.handler;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.model.gameobjects.Npc;

/**
 * NPC 激活 / 停用事件处理器，负责在 AI 被激活或停用时更新已知列表、思考与清理状态。
 * deactivate events: updates known list, triggers think, and clears state.
 *
 * @author ATracer
 */
public class ActivateEventHandler {

	/**
	 * 处理 NPC AI 激活：若当前为空闲状态，则刷新已知列表并触发思考。
	 * Handles NPC AI activation: when idle, refreshes known list and triggers think.
	 *
	 * NPC AI instance
	 */
	public static void onActivate(NpcAI2 npcAI) {
		if (npcAI.isInState(AIState.IDLE)) {
			npcAI.getOwner().updateKnownlist();
			npcAI.think();
		}
	}

	/**
	 * 处理 NPC AI 停用：停止行走、重新思考，并清理仇恨与效果。
	 * Handles NPC AI deactivation: stops walking, re-thinks, and clears aggro and effects.
	 *
	 * NPC AI instance
	 */
	public static void onDeactivate(NpcAI2 npcAI) {
		if (npcAI.isInState(AIState.WALKING)) {
			WalkManager.stopWalking(npcAI);
		}
		npcAI.think();
		Npc npc = npcAI.getOwner();
		npc.updateKnownlist();
		npc.getAggroList().clear();
		npc.getEffectController().removeAllEffects();
	}
}
