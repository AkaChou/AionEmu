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
 * 传送门/传送点 AI：Fatebound Abbey Portal（@AIName "fatebound"），继承 NpcAI2。
 * Portal/teleporter AI: Fatebound Abbey Portal (@AIName "fatebound"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("fatebound")
public class FateboundAbbeyPortalAI2 extends NpcAI2
{
	@Override
    protected void handleDialogStart(Player player) {
        if (player.getInventory().getFirstItemByItemId(164000336) != null) { //修道院返回石。 / Abbey Return Stone.
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
				// 潘达梦宁至命运修道院。 / Pandaemonium To Fatebound Abbey.
				case 209677:
				    TeleportService2.teleportTo(player, 140010000, 284.83774f, 266.19388f, 96.48758f, (byte) 60, TeleportAnimation.BEAM_ANIMATION);
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
}
