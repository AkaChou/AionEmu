package com.aionemu.gameserver.ai.instance.raksangRuins;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Raksang Ruins 副本 NPC AI：Abiso（@AIName "abiso"），继承 GeneralNpcAI2。
 * Raksang Ruins instance NPC AI: Abiso (@AIName "abiso"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("abiso")
public class AbisoAI2 extends GeneralNpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
        switch (getNpcId()) {
			case 206378: //Abiso Way A.
			case 206379: //Abiso Way B.
			case 206380: { //Abiso Way C.
				super.handleDialogStart(player);
				break;
			} default: {
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
				break;
			}
		}
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		int instanceId = getPosition().getInstanceId();
		QuestEnv env = new QuestEnv(getOwner(), player, questId, dialogId);
		env.setExtendedRewardIndex(extendedRewardIndex);
		if (GameEngineServices.questEngine().onDialog(env) && dialogId != 1011) {
			return true;
		} if (dialogId == 10000) {
		    switch (getNpcId()) {
			    case 206378: //Abiso Way A.
				    TeleportService2.teleportTo(player, 300610000, instanceId, 519.06165f, 419.07617f, 927.697644f, (byte) 74);
				break;
				case 206379: //Abiso Way B.
				    TeleportService2.teleportTo(player, 300610000, instanceId, 810.93933f, 830.2498f, 733.6704f, (byte) 3);
				break;
				case 206380: //Abiso Way C.
				    TeleportService2.teleportTo(player, 300610000, instanceId, 384.6957f, 451.9979f, 120.786255f, (byte) 64);
				break;
			}
		} else if (dialogId == 1011 && questId != 0) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), dialogId, questId));
		}
		return true;
	}
}
