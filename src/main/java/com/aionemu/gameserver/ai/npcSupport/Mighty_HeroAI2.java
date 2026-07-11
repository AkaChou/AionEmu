package com.aionemu.gameserver.ai.npcSupport;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * NPC 支援/增益 AI：Mighty Hero（@AIName "mighty"），继承 GeneralNpcAI2。
 * NPC support/buff AI: Mighty Hero (@AIName "mighty"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("mighty")
public class Mighty_HeroAI2 extends GeneralNpcAI2
{
    @Override
	protected void handleDialogStart(Player player) {
        switch (getNpcId()) {
			case 832884: //Mighty Lovely.
			case 832885: { //Mighty Mister.
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
			int skillId = 0;
			switch (getNpcId()) {
				case 832884: //Mighty Lovely.
				    skillId = 21796; //Mighty's Passionate Cheer I.
				break;
				case 832885: //Mighty Mister.
					skillId = 21797; //Mighty's Energetic Cheer I.
				break;
			}
			GameEngineServices.skillEngine().getSkill(getOwner(), skillId, 1, player).useNoAnimationSkill();
		} else if (dialogId == 1011 && questId != 0) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), dialogId, questId));
		}
        return true;
    }
}
