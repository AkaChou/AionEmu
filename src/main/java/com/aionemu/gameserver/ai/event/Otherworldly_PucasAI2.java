package com.aionemu.gameserver.ai.event;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.autogroup.AutoGroupType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FIND_GROUP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 活动事件 NPC AI：Otherworldly Pucas（@AIName "nightmare_circus"），继承 GeneralNpcAI2。
 * Event NPC AI: Otherworldly Pucas (@AIName "nightmare_circus"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("nightmare_circus")
public class Otherworldly_PucasAI2 extends GeneralNpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
        switch (getNpcId()) {
			case 831541:
			case 831542:
			case 831543:
			case 831544:
            case 831545:
			case 831546:
			case 831547:
			case 831548: {
				super.handleDialogStart(player);
				break;
			} default: {
				if (player.getLevel() >= 30) {
				    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getOwner().getObjectId(), 1011));
				} else {
				    PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 27));
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL);
				}
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
		} if (dialogId == 105) {
			switch (getNpcId()) {
			    case 831541:
				case 831542:
				case 831543:
				case 831544:
				case 831545:
				case 831546:
				case 831547:
				case 831548:
				    AutoGroupType agt = AutoGroupType.getAutoGroup(player.getLevel(), getNpcId());
					if (agt != null) {
					    PacketSendUtility.sendPacket(player, new SM_FIND_GROUP(0x1A, agt.getInstanceMapId()));
					}
				break;
			}
		} else if (dialogId == 1011 && questId != 0) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), dialogId, questId));
		}
        return true;
    }
}
