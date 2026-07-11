package com.aionemu.gameserver.ai.instance.archivesOfEternity;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Archives Of Eternity 副本 NPC AI：Viola（@AIName "weatha"），继承 GeneralNpcAI2。
 * Archives Of Eternity instance NPC AI: Viola (@AIName "weatha"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("weatha")
public class ViolaAI2 extends GeneralNpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
        switch (getNpcId()) {
			case 806148: { //Viola.
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
		QuestEnv env = new QuestEnv(getOwner(), player, questId, dialogId);
		env.setExtendedRewardIndex(extendedRewardIndex);
		if (GameEngineServices.questEngine().onDialog(env) && dialogId != 1011) {
			return true;
		} if (dialogId == 10000) {
		    switch (getNpcId()) {
			    case 806148: //Viola.
				    switch (Rnd.get(1, 3)) {
					    case 1:
						    getPosition().getWorldMapInstance().getDoors().get(349).setOpen(true);
							// 通往永恒档案的门已打开。 / The door to the Archives of Eternity has opened.
							PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_IDEternity_01_Road_Set, 0);
						break;
						case 2:
						    getPosition().getWorldMapInstance().getDoors().get(352).setOpen(true);
							// 通往永恒档案的门已打开。 / The door to the Archives of Eternity has opened.
							PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_IDEternity_01_Road_Set, 0);
						break;
						case 3:
						    getPosition().getWorldMapInstance().getDoors().get(359).setOpen(true);
							// 通往永恒档案的门已打开。 / The door to the Archives of Eternity has opened.
							PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_IDEternity_01_Road_Set, 0);
						break;
					}
				break;
			}
		} else if (dialogId == 1011 && questId != 0) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), dialogId, questId));
		}
		return true;
	}
}
