package com.aionemu.gameserver.ai.housing;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.DialogPage;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 房屋相关 NPC AI：House Sign（@AIName "housesign"），继承 GeneralNpcAI2。
 * Housing-related NPC AI: House Sign (@AIName "housesign"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("housesign")
public class HouseSignAI2 extends GeneralNpcAI2
{
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		return pageDialog(player, DialogPage.getPageByAction(dialogId));
	}
	
	private boolean pageDialog(Player player, DialogPage page) {
		if (page == DialogPage.NULL)
		return false;
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getOwner().getObjectId(), page.id()));
		return true;
	}
}
