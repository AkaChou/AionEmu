package com.aionemu.gameserver.ai.instance.seizedDanuarSanctuary;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Seized Danuar Sanctuary 副本 NPC AI：The Catacombs Entrance（@AIName "catacombs"），继承 NpcAI2。
 * Seized Danuar Sanctuary instance NPC AI: The Catacombs Entrance (@AIName "catacombs"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("catacombs")
public class TheCatacombsEntranceAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        if (player.getInventory().getFirstItemByItemId(185000181) != null) { //The Catacombs Key.
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
        } else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
        }
    }
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		int instanceId = getPosition().getInstanceId();
		switch (getNpcId()) {
		    case 701873: //The Catacombs Entrance.
		        switch (player.getWorldId()) {
		            case 301140000: //Seized Danuar Sanctuary 4.8
				        if (dialogId == 10000 && player.getInventory().decreaseByItemId(185000181, 1)) { //The Catacombs Key.
			                TeleportService2.teleportTo(player, 301140000, instanceId, 1032.0134f, 369.56287f, 297.8753f, (byte) 32);
					    }
				    break;
			    } switch (player.getWorldId()) {
				    case 301380000: //Danuar Sanctuary 4.8
					    if (dialogId == 10000 && player.getInventory().decreaseByItemId(185000181, 1)) { //The Catacombs Key.
					        TeleportService2.teleportTo(player, 301380000, instanceId, 1032.0134f, 369.56287f, 297.8753f, (byte) 32);
						}
					break;
				}
			break;
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
}
