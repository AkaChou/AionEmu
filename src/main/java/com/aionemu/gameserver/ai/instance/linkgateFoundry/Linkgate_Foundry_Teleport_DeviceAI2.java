package com.aionemu.gameserver.ai.instance.linkgateFoundry;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Linkgate Foundry 副本 NPC AI：Linkgate Foundry Teleport Device（@AIName "linkgate_foundry_teleport_device"），继承 NpcAI2。
 * Linkgate Foundry instance NPC AI: Linkgate Foundry Teleport Device (@AIName "linkgate_foundry_teleport_device"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("linkgate_foundry_teleport_device")
public class Linkgate_Foundry_Teleport_DeviceAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getOwner().getObjectId(), 1011));
	}
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		int instanceId = getPosition().getInstanceId();
		switch (getNpcId()) {
		    case 702591: //Linkgate Foundry Teleport Device.
		        switch (player.getWorldId()) {
		            case 301270000: //Linkgate Foundry 4.7
				        if (dialogId == 10000) {
							// 秘密实验室位置已暴露三次。实验室空无一物，研究人员已逃离。 / The Secret Lab's location was revealed three times. Nothing remains in the lab and the reserachers have fled.
							PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_IDLDF4_Re_01_secret_room_03, 0);
							TeleportService2.teleportTo(player, 301270000, instanceId, 174.68114f, 258.55096f, 312.1969f, (byte) 93);
					    } else if (dialogId == 10001) {
							// 秘密实验室位置已暴露三次。实验室空无一物，研究人员已逃离。 / The Secret Lab's location was revealed three times. Nothing remains in the lab and the reserachers have fled.
							PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_IDLDF4_Re_01_secret_room_03, 0);
							TeleportService2.teleportTo(player, 301270000, instanceId, 174.68114f, 258.55096f, 353.1969f, (byte) 93);
					    } else if (dialogId == 10002) {
							// 秘密实验室位置已暴露三次。实验室空无一物，研究人员已逃离。 / The Secret Lab's location was revealed three times. Nothing remains in the lab and the reserachers have fled.
							PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_IDLDF4_Re_01_secret_room_03, 0);
							TeleportService2.teleportTo(player, 301270000, instanceId, 174.68114f, 258.55096f, 393.1969f, (byte) 93);
					    }
				    break;
			    }
			break;
		}
		return true;
	}
}
