package com.aionemu.gameserver.ai.worlds.abbey;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Abbey 区域 NPC AI：Balaurea Invasion Portal（@AIName "balaurea_invasion_portal"），继承 NpcAI2。
 * Abbey zone NPC AI: Balaurea Invasion Portal (@AIName "balaurea_invasion_portal"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("balaurea_invasion_portal")
public class BalaureaInvasionPortalAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		if (player.getLevel() >= 55) {
		    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getOwner().getObjectId(), 10));
		} else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL);
        }
	}
}
