package com.aionemu.gameserver.ai.instance.lowerUdasTemple;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Lower Udas Temple 副本 NPC AI：Toxic Caverns Teleport Device（@AIName "toxic_caverns_teleport_device"），继承 ActionItemNpcAI2。
 * Lower Udas Temple instance NPC AI: Toxic Caverns Teleport Device (@AIName "toxic_caverns_teleport_device"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("toxic_caverns_teleport_device")
public class Toxic_Caverns_Teleport_DeviceAI2 extends ActionItemNpcAI2
{
	@Override
	protected void handleUseItemFinish(Player player) {
		switch (getNpcId()) {
		    case 700966: //Toxic Caverns Teleport Device.
				if (player.getInventory().decreaseByItemId(186000110, 1)) { //Surkana Crystal.
				    TeleportService2.teleportTo(player, 300160000, 1338.5336f, 806.8095f, 113.52537f, (byte) 0, TeleportAnimation.JUMP_ANIMATION);
                } else {
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
				}
		    break;
		}
	}
}
