package com.aionemu.gameserver.ai.instance.cradleOfEternity;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Cradle Of Eternity 副本 NPC AI：Eternity Rift（@AIName "Eternity_Rift"），继承 NpcAI2。
 * Cradle Of Eternity instance NPC AI: Eternity Rift (@AIName "Eternity_Rift"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Eternity_Rift")
public class Eternity_RiftAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		if (player.isArchDaeva()) {
		    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
		}
	}
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 104) {
			switch (getNpcId()) {
			    case 806053: //Eternity Rift.
				    if (player.getCommonData().getRace() == Race.ASMODIANS) {
					    TeleportService2.teleportTo(player, 220120000, 448.4795f, 499.93314f, 299.85013f, (byte) 0);
				    } else if (player.getCommonData().getRace() == Race.ELYOS) {
					    TeleportService2.teleportTo(player, 210110000, 448.4795f, 499.93314f, 299.85013f, (byte) 0);
				    }
				break;
			}
		}
		return true;
	}
}
