package com.aionemu.gameserver.ai.instance.sealedDanuarMysticarium;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Sealed Danuar Mysticarium 副本 NPC AI：Internal Teleport Device（@AIName "internal_teleport_device"），继承 NpcAI2。
 * Sealed Danuar Mysticarium instance NPC AI: Internal Teleport Device (@AIName "internal_teleport_device"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("internal_teleport_device")
public class Internal_Teleport_DeviceAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		if (player.getLevel() >= 65) {
		    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getOwner().getObjectId(), 10));
		} else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL);
        }
	}
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		int instanceId = getPosition().getInstanceId();
		if (dialogId == 10000) {
		    switch (getNpcId()) {
				case 731583: //Internal Teleport Device E.
				case 731584: //Internal Teleport Device A.
				    TeleportService2.teleportTo(player, 300480000, instanceId, 146.07611f, 186.12431f, 240.29831f, (byte) 115);
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
}
