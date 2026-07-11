package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 邮箱 AI：打开玩家邮箱界面。
 * Postbox AI that opens the player mail UI.
 *
 * @author Encom
 */
@AIName("postbox")
public class PostboxAI2 extends NpcAI2
{
	/**
	 * 玩家开始与本 NPC 对话/交互。
	 * Player starts dialog/interaction with this NPC.
	 *
	 * 玩家 / player
	 */
	@Override
	protected void handleDialogStart(Player player) {
		int level = player.getLevel();
		if (level < 10) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FREE_EXPERIENCE_CHARACTER_CANT_SEND_ITEM("10"));
			return;
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 18));
		player.getMailbox().sendMailList(false);
	}
	
	/**
	 * 玩家结束与本 NPC 对话。
	 * Player finishes dialog with this NPC.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	protected void handleDialogFinish(Player player) {
	}
}
