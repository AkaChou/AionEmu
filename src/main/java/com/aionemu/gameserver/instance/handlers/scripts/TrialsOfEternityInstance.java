package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

@InstanceID(301560000)
public class TrialsOfEternityInstance extends GeneralInstanceHandler {

	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		if (npc.getNpcId() != 731736) {
			return;
		}
		if (player.getInventory().decreaseByItemId(185000297, 1)) {
			TeleportService2.teleportTo(player, mapId, instanceId, 522.7508f, 1217.3593f, 724.3436f, (byte) 61);
		} else {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1404075));
		}
	}

}
