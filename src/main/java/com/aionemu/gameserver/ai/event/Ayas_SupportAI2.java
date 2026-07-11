package com.aionemu.gameserver.ai.event;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 活动事件 NPC AI：Ayas Support（@AIName "ayas_support"），继承 GeneralNpcAI2。
 * Event NPC AI: Ayas Support (@AIName "ayas_support"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("ayas_support")
public class Ayas_SupportAI2 extends GeneralNpcAI2
{
    @Override
	protected void handleDialogStart(Player player) {
        switch (getNpcId()) {
            // 天族。 / Elyos.
			case 833671: //Helpful Ayas.
			case 833672: //Friendly Ayas.
            // 魔族。 / Asmodians.
            case 833673: //Helpful Ayas.
			case 833674: { //Friendly Ayas.
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
		PlayerEffectController effectController = player.getEffectController();
		if (GameEngineServices.questEngine().onDialog(env) && dialogId != 1011) {
			return true;
		} if (dialogId == 10000) {
			int skillId = 0;
			switch (getNpcId()) {
			    case 833671: //Helpful Ayas E.
				case 833673: //Helpful Ayas A.
					skillId = 11047; //Helpful Ayas' Cheer.
					effectController.removeEffect(11049);
				break;
			    case 833672: //Friendly Ayas E.
				case 833674: //Friendly Ayas A.
					skillId = 11049; //Friendly Ayas' Cheer.
					effectController.removeEffect(11047);
				break;
			}
			GameEngineServices.skillEngine().getSkill(getOwner(), skillId, 1, player).useNoAnimationSkill();
		} else if (dialogId == 1011 && questId != 0) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), dialogId, questId));
		}
        return true;
    }
}
