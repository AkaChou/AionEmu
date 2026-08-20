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
 * Archives Of Eternity 副本 NPC AI：Peregrine（@AIName "feregran"），继承 GeneralNpcAI2。
 * Archives Of Eternity instance NPC AI: Peregrine (@AIName "feregran"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("feregran")
public class PeregrineAI2 extends GeneralNpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
        switch (getNpcId()) {
			case 806149: { //Peregrine.
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
			    case 806149: //Peregrine.
					openRandomRoadDoor();
				break;
			}
		} else if (dialogId == 1011 && questId != 0) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), dialogId, questId));
		}
		return true;
	}

	private void openRandomRoadDoor() {
		if (getPosition() == null || getPosition().getWorldMapInstance() == null
			|| getPosition().getWorldMapInstance().getDoors() == null) {
			return;
		}
		int doorId = switch (Rnd.get(1, 3)) {
			case 1 -> 349;
			case 2 -> 352;
			default -> 359;
		};
		var door = getPosition().getWorldMapInstance().getDoors().get(doorId);
		if (door == null) {
			return;
		}
		door.setOpen(true);
		// 通往永恒档案的门已打开。 / The door to the Archives of Eternity has opened.
		PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_IDEternity_01_Road_Set, 0);
	}
}
