package com.aionemu.gameserver.ai.portals;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.DialogAction;
import com.aionemu.gameserver.model.autogroup.MatchDefinition;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_AUTO_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 传送门/传送点 AI：Match Maker（@AIName "match_maker"），继承 GeneralNpcAI2。
 * Portal/teleporter AI: Match Maker (@AIName "match_maker"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("match_maker")
public class MatchMakerAI2 extends GeneralNpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		MatchDefinition type = MatchDefinition.forNpc(player.getLevel(), getNpcId());
		if (type != null && type.isOpen()) {
		    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
		} else {
            // 有这么多志愿者真令人振奋！真想立刻送你们上战舰。 / It's refreshing to have so many volunteers! I wish I could send you to the Dredgion right now.
            // 哦，你不知道吗？必须在附近我才能送你过去。 / Oh, didn't you know ? It has to be close by for me to send you there.
            // 等到它回来，[%username]。 / Wait until it comes back, [%username].
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
        }
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		QuestEnv env = new QuestEnv(getOwner(), player, questId, dialogId);
		env.setExtendedRewardIndex(extendedRewardIndex);
		if (GameEngineServices.questEngine().onDialog(env) && dialogId != 1011) {
			return true;
		} if (dialogId == DialogAction.MATCH_MAKER.id()) {
			MatchDefinition type = MatchDefinition.forNpc(player.getLevel(), getNpcId());
			if (type != null && type.isOpen()) {
				PacketSendUtility.sendPacket(player, new SM_AUTO_GROUP(type.getInstanceMaskId()));
			}
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		} else if (dialogId == 1011 && questId != 0) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), dialogId, questId));
		}
		return true;
    }
}
