package com.aionemu.gameserver.ai.event.summerBlockParty;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;

/**
 * Summer Block Party 活动 NPC AI：Slim（@AIName "slim"），继承 GeneralNpcAI2。
 * Summer Block Party event NPC AI: Slim (@AIName "slim"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("slim")
public class SlimAI2 extends GeneralNpcAI2 {

  	@Override
	protected void handleDialogStart(Player player) {
        switch (getNpcId()) {
            case 831815:
			case 831818:
			case 831827:
			case 831830: {
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
		} else if (dialogId == 1011 && questId != 0) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), dialogId, questId));
		}
		return true;
	}
	
	@Override
	protected void handleSpawned() {
		ZonedDateTime now = ZonedDateTime.now();
		int currentDay = now.getDayOfWeek().getValue(); // 1 (Monday) to 7 (Sunday)
		
		switch (getNpcId()) {
			case 831815:
			case 831827:
				if (currentDay >= 1 && currentDay <= 4) {
					super.handleSpawned();
				} else {
					if (!isAlreadyDead()) {
						getOwner().getController().onDelete();
					}
				}
			break;
			case 831818:
			case 831830:
				if (currentDay >= 5 && currentDay <= 7) {
					super.handleSpawned();
				} else {
					if (!isAlreadyDead()) {
						getOwner().getController().onDelete();
					}
				}
			break;
		}
	}
}
