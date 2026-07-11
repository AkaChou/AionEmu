package com.aionemu.gameserver.ai.instance.esoterrace;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Esoterrace 副本 NPC AI：Murugan Mailbox（@AIName "murugan_mailbox"），继承 NpcAI2。
 * Esoterrace instance NPC AI: Murugan Mailbox (@AIName "murugan_mailbox"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("murugan_mailbox")
public class Murugan_MailboxAI2 extends NpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), QuestDialog.SELECT_ACTION_1011.id(), 0));
	}
}
