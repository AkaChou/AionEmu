package com.aionemu.gameserver.ai.instance.fissureOfOblivion;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Fissure Of Oblivion 副本 NPC AI：Teleport Stone（@AIName "Teleport_Stone"），继承 NpcAI2。
 * Fissure Of Oblivion instance NPC AI: Teleport Stone (@AIName "Teleport_Stone"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Teleport_Stone")
public class Teleport_StoneAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		if (player.getLevel() >= 66) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getOwner().getObjectId(), 1011));
		} else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
        }
	}
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		int instanceId = getPosition().getInstanceId();
		switch (getNpcId()) {
		    case 834188: //Orkia Aetheric Field Observatory Square Teleport Stone.
		        switch (player.getWorldId()) {
		            case 302100000: //Fissure Of Oblivion 5.1
				        if (dialogId == 104) { //Teleport to the Orkia Observatory.
							TeleportService2.teleportTo(player, 302100000, instanceId, 594.65173f, 561.58746f, 352.84213f, (byte) 9);
					    }
				    break;
					case 302110000: //IDTransform Event 5.6
				        if (dialogId == 104) { //Teleport to the Orkia Observatory.
							TeleportService2.teleportTo(player, 302110000, instanceId, 594.65173f, 561.58746f, 352.84213f, (byte) 9);
					    }
				    break;
			    }
			break;
			case 834189: //Fallen Orkia Fortress Teleport Stone.
		        switch (player.getWorldId()) {
		            case 302100000: //Fissure Of Oblivion 5.1
				        if (dialogId == 104) { //Teleport to the Orkia Ruins.
							TeleportService2.teleportTo(player, 302100000, instanceId, 522.81244f, 575.3527f, 322.02863f, (byte) 54);
					    }
				    break;
					case 302110000: //IDTransform Event 5.6
				        if (dialogId == 104) { //Teleport to the Orkia Ruins.
							TeleportService2.teleportTo(player, 302110000, instanceId, 522.81244f, 575.3527f, 322.02863f, (byte) 54);
					    }
				    break;
			    }
			break;
			case 834190: //Orkia Spire Teleport Stone.
		        switch (player.getWorldId()) {
		            case 302100000: //Fissure Of Oblivion 5.1
				        if (dialogId == 104) { //Teleport to the Orkia Spire.
							TeleportService2.teleportTo(player, 302100000, instanceId, 408.66364f, 513.476f, 342.3292f, (byte) 90);
					    }
				    break;
					case 302110000: //IDTransform Event 5.6
				        if (dialogId == 104) { //Teleport to the Orkia Spire.
							TeleportService2.teleportTo(player, 302110000, instanceId, 408.66364f, 513.476f, 342.3292f, (byte) 90);
					    }
				    break;
			    }
			break;
		}
		return true;
	}
}
