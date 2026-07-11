package com.aionemu.gameserver.ai.portals;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 传送门/传送点 AI：Idian Depths Corridor（@AIName "idian_depths_corridor"），继承 ActionItemNpcAI2。
 * Portal/teleporter AI: Idian Depths Corridor (@AIName "idian_depths_corridor"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("idian_depths_corridor")
public class Idian_Depths_CorridorAI2 extends ActionItemNpcAI2
{
	@Override
	protected void handleUseItemFinish(Player player) {
		switch (getNpcId()) {
		    case 731631: //Cygnea To Idian Depths.
			case 731641: //Levinshor To Idian Depths.
			    if (player.getLevel() >= 65) {
				    TeleportService2.teleportTo(player, 210090000, 691.99f, 811.69055f, 514.86566f, (byte) 29, TeleportAnimation.BEAM_ANIMATION);
                } else {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Telepoter_Under_User);
				}
			break;
			case 731632: //Enshar To Idian Depths.
			case 731642: //Kaldor To Idian Depths.
				if (player.getLevel() >= 65) {
				    TeleportService2.teleportTo(player, 220100000, 691.99f, 811.69055f, 514.86566f, (byte) 29, TeleportAnimation.BEAM_ANIMATION);
                } else {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Telepoter_Under_User);
				}
		    break;
        }
	}
}
