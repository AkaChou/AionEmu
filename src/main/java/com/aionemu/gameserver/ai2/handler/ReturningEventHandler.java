package com.aionemu.gameserver.ai2.handler;

import com.aionemu.gameserver.ai2.AI2Logger;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.EmoteManager;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.geometry.Point3D;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOVE;
import com.aionemu.gameserver.skillengine.model.HealType;
import com.aionemu.gameserver.utils.PacketSendUtility;

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
			npcAI.getOwner().getMoveController().beginHomeReturn();
			EmoteManager.emoteStartReturning(npcAI.getOwner());
		}
		if (npcAI.isInState(AIState.RETURNING)) {
			Npc npc = npcAI.getOwner();
			var definition = DataManager.NPC_PATH_BEHAVIOR_DATA == null ? null
					: DataManager.NPC_PATH_BEHAVIOR_DATA.get(npc.getNpcId());
			if (definition == null || !"run".equalsIgnoreCase(definition.returnMoveType())) {
				npc.setState(CreatureState.WALKING);
			} else {
				npc.unsetState(CreatureState.WALKING);
			}
			if (definition != null && "teleport".equalsIgnoreCase(definition.returnMoveType())) {
				teleportHome(npc);
				onBackHome(npcAI);
				return;
			}
			if (npcAI.isMoveSupported()) {
				npc.getMoveController().abortMove();
				npc.getMoveController().moveToHome();
			} else {
				teleportHome(npc);
				onBackHome(npcAI);
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
		Npc npc = npcAI.getOwner();
		boolean returnedToWaypoint = npc.getMoveController().isReturningToWaypoint();
		boolean fullHeal = npc.getMoveController().consumeFullHealOnHomeReturn();
		npc.getMoveController().clearHomeReturn();
		if (fullHeal) {
			int restoredHp = npc.getLifeStats().getMaxHp() - npc.getLifeStats().getCurrentHp();
			npc.getLifeStats().setCurrentHpPercent(100);
			if (restoredHp > 0) {
				PacketSendUtility.broadcastPacketAndReceive(npc,
						new SM_ATTACK_STATUS(npc, npc, SM_ATTACK_STATUS.TYPE.HP, 0, restoredHp));
				npc.getObserveController().notifyLifeChangedObservers(HealType.HP, npc.getLifeStats().getCurrentHp());
			}
		}
		npc.unsetState(CreatureState.WEAPON_EQUIPPED);
		npc.getAggroList().clear();
		if (!returnedToWaypoint) {
			npc.setXYZH(null, null, null, npc.getSpawn().getHeading());
		}
		PacketSendUtility.broadcastPacket(npc, new SM_MOVE(npc));
		if (npcAI.setStateIfNot(AIState.IDLE)) {
			EmoteManager.emoteStartIdling(npcAI.getOwner());
			ThinkEventHandler.thinkIdle(npcAI);
		}
		npc.getController().onReturnHome();
	}

	private static void teleportHome(Npc npc) {
		Point3D target = npc.getMoveController().getHomeReturnDestination();
		byte heading = npc.getMoveController().isReturningToWaypoint() ? npc.getHeading() : npc.getSpawn().getHeading();
		npc.getMoveController().abortMove();
		GameWorldBootstrapServices.world().updatePosition(npc, target.getX(), target.getY(), target.getZ(), heading);
	}
}
