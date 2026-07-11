package com.aionemu.gameserver.ai.event.rainbowSnakeFestival;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Rainbow Snake Festival 活动 NPC AI：Dapplie（@AIName "dapplie"），继承 GeneralNpcAI2。
 * Rainbow Snake Festival event NPC AI: Dapplie (@AIName "dapplie"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("dapplie")
public class DapplieAI2 extends GeneralNpcAI2
{
  	@Override
	protected void handleDialogStart(Player player) {
        switch (getNpcId()) {
            case 832964: //Dapplie E.
			case 832975: { //Dapplie A.
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
				case 832964: //Dapplie E.
				case 832975: //Dapplie A.
					switch (Rnd.get(1, 2)) {
						case 1:
							skillId = 10978;
							effectController.removeEffect(10976);
							effectController.removeEffect(10977);
							effectController.removeEffect(10979);
						break;
						case 2:
							skillId = 10979;
							effectController.removeEffect(10976);
							effectController.removeEffect(10977);
							effectController.removeEffect(10978);
						break;
					}
				break;
			}
			GameEngineServices.skillEngine().getSkill(getOwner(), skillId, 1, player).useNoAnimationSkill();
		} else if (dialogId == 1011 && questId != 0) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), dialogId, questId));
		}
		return true;
	}
}
