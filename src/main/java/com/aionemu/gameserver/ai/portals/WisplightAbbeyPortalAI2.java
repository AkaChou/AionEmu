package com.aionemu.gameserver.ai.portals;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 传送门/传送点 AI：Wisplight Abbey Portal（@AIName "wisplight"），继承 NpcAI2。
 * Portal/teleporter AI: Wisplight Abbey Portal (@AIName "wisplight"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("wisplight")
public class WisplightAbbeyPortalAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        if (player.getInventory().getFirstItemByItemId(164000335) != null) { //修道院返回石。 / Abbey Return Stone.
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
        } else {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Telepoter_Arena_Clobby_User);
        }
    }
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
		    switch (getNpcId()) {
				// 圣所至光辉修道院。 / Sanctum To Wisplight Abbey.
				case 209676:
				    TeleportService2.teleportTo(player, 130090000, 247.05283f, 223.70496f, 129.38268f, (byte) 31, TeleportAnimation.BEAM_ANIMATION);
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
}
