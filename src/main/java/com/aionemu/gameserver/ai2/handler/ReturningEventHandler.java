package com.aionemu.gameserver.ai2.handler;

import com.aionemu.gameserver.ai2.AI2Logger;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.EmoteManager;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.spawnengine.SpawnEngine;

/**
 * 返回出生点事件处理器，负责 NPC 不在家 / 回到家时的移动、重生与空闲恢复。
 * Handles return-home events: movement, respawn, and idle recovery when NPC is away from or back at home.
 *
 * @author ATracer
 * @modified Yon (Aion Reconstruction Project) -- added handling to {@link #onNotAtHome(NpcAI2)} for when the entity cannot move;
 * removed deprecated method calls
 */
public class ReturningEventHandler {

	/**
	 * 不在出生点时触发：进入 RETURNING，按路径行走、归家移动，或删除后在原点重生。
	 * Fired when not at home: enters RETURNING, walks routes, moves home, or deletes and respawns at origin.
	 *
	 * NPC AI instance
	 */
	public static void onNotAtHome(NpcAI2 npcAI) {
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "onNotAtHome");
		}
		if (npcAI.setStateIfNot(AIState.RETURNING)) {
			if (npcAI.isLogging()) {
				AI2Logger.info(npcAI, "returning and restoring");
			}
			EmoteManager.emoteStartReturning(npcAI.getOwner());
		}
		if (npcAI.isInState(AIState.RETURNING)) {
			Npc npc = (Npc) npcAI.getOwner();
			if (npc.hasWalkRoutes()) {
				WalkManager.startWalking(npcAI);
			}else if (npcAI.isMoveSupported() && npc.getDistanceToSpawnLocation() < 100) { //Arbitrary distance
//					Point3D prevStep = npc.getMoveController().recallPreviousStep();
//					npcAI.getOwner().getMoveController().moveToPoint(prevStep.getX(), prevStep.getY(), prevStep.getZ());
				npc.getMoveController().abortMove();
				npc.getMoveController().moveToHome();
			} else {
				if (npc.isDeleteDelayed()) {
					onBackHome(npcAI);
				} else {
					/*
					 * The idea is the entity cannot move, but has been moved from its spawn...
					 * so instead of moving it back to spawn (not possible), it should just
					 * despawn and then respawn back at the original spawn point.
					 *
					 * Or, if the entity can move, but is too far away from spawn to worry about
					 * moving back directly (which can happen since mob leashes have been removed).
					 */
					SpawnTemplate spawn = npc.getSpawn();
					int instanceId = npc.getInstanceId();
					npc.getController().onDelete();
					SpawnEngine.spawnObject(spawn, instanceId);
				}
			}
		}
	}

	/**
	 * 回到出生点时触发：切回空闲、播放空闲表情并通知控制器归家。
	 * Fired when back at home: returns to idle, plays idle emote, and notifies controller of return.
	 *
	 * NPC AI instance
	 */
	public static void onBackHome(NpcAI2 npcAI) {
		if (npcAI.isLogging()) {
			AI2Logger.info(npcAI, "onBackHome");
		}
//		npcAI.getOwner().getMoveController().clearBackSteps();
		if (npcAI.setStateIfNot(AIState.IDLE)) {
			EmoteManager.emoteStartIdling(npcAI.getOwner());
			ThinkEventHandler.thinkIdle(npcAI);
		}
		Npc npc = (Npc) npcAI.getOwner();
		npc.getController().onReturnHome();
	}
}
