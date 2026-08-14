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
    /**
	 * 打开对话窗口：特定支援 NPC 使用默认对话，其余打开通用对话。
	 * Opens the dialog window: default dialog for specific support NPCs, generic dialog otherwise.
	 */
	@Override
	protected void handleDialogStart(Player player) {
        switch (getNpcId()) {
			case 832884: //技能支援 NPC：Lovely / Mighty Lovely.
			case 832885: { //技能支援 NPC：Mister / Mighty Mister.
				super.handleDialogStart(player);
				break;
			} default: {
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
				break;
			}
		}
	}
	
	/**
	 * 对话选择处理：处理任务对话，并为玩家施加对应 NPC 的助威技能。
	 * Handles dialog selection: processes quest dialogs and applies the matching cheer skill of the NPC.
	 */
	@Override
    public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		QuestEnv env = new QuestEnv(getOwner(), player, questId, dialogId);
		env.setExtendedRewardIndex(extendedRewardIndex);
		if (GameEngineServices.questEngine().onDialog(env) && dialogId != 1011) {
			return true;
		} if (dialogId == 10000) {
			int skillId = 0;
			switch (getNpcId()) {
				case 832884: //技能支援 NPC：Lovely / Mighty Lovely.
				    skillId = 21796; //Mighty 的热情助威 I / Mighty's Passionate Cheer I.
				break;
				case 832885: //技能支援 NPC：Mister / Mighty Mister.
					skillId = 21797; //Mighty 的活力助威 I / Mighty's Energetic Cheer I.
				break;
			}
			GameEngineServices.skillEngine().getSkill(getOwner(), skillId, 1, player).useNoAnimationSkill();
		} else if (dialogId == 1011 && questId != 0) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), dialogId, questId));
		}
        return true;
    }
}
