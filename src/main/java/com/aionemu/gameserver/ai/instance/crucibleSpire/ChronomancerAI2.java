package com.aionemu.gameserver.ai.instance.crucibleSpire;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.List;

/**
 * Crucible Spire 副本 NPC AI：Chronomancer（@AIName "IDInfinity_Teleport_Odd_Number"），继承 NpcAI2。
 * Crucible Spire instance NPC AI: Chronomancer (@AIName "IDInfinity_Teleport_Odd_Number"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("IDInfinity_Teleport_Odd_Number")
public class ChronomancerAI2 extends NpcAI2 {

	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		int pfloor = player.getFloor();
		int instanceId = getPosition().getInstanceId();
		if (dialogId == 10000) {
		    switch (getNpcId()) {
				case 247376:
			    case 247386:
				    GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						@Override
						public void run() {
							despawnNpc(701772); //Stair.
						}
					}, 5000);
					spawnFloor(pfloor + 1);
					spawn(701000, 263.55551f, 1249.5244f, 240.73053f, (byte) 0, 56); //Wall.
					TeleportService2.teleportTo(player, 302400000, instanceId, 219.33264f, 1249.4528f, 240.85301f, (byte) 0);
					if (pfloor == 39) {
						TeleportService2.teleportTo(player, 302400000, instanceId, 210.42656f, 249.58434f, 971.39510f, (byte) 0);
					}
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
	
	private void spawnFloor(int next) {
		switch (next) {
			case 1:
				spawn(247247, 241.31040f, 1249.5646f, 240.63419f, (byte) 60);
				spawn(247248, 246.45409f, 1254.9410f, 240.63419f, (byte) 60);
				spawn(247248, 246.61267f, 1244.2950f, 240.63419f, (byte) 60);
			break;
		}
	}
	
	private void despawnNpc(int npcId) {
		if (getPosition().getWorldMapInstance().getNpcs(npcId) != null) {
			List<Npc> npcs = getPosition().getWorldMapInstance().getNpcs(npcId);
			for (Npc npc: npcs) {
				npc.getController().onDelete();
			}
		}
	}
}
