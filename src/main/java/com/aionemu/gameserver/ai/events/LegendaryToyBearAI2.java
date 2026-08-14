package com.aionemu.gameserver.ai.events;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;

@AIName("legendary_toy_bear")
// NPC 模板 ID：833669 / NPC template ID: 833669

/**
 * 活动事件 NPC AI：Legendary Toy Bear（@AIName "legendary_toy_bear"），继承 ActionItemNpcAI2。
 * Event NPC AI: Legendary Toy Bear (@AIName "legendary_toy_bear"), extends ActionItemNpcAI2.
 *
 * @author Falke_34 & FrozenKiller
 */
public class LegendaryToyBearAI2 extends ActionItemNpcAI2 {

	@Override
	protected void handleUseItemFinish(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
	}

	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		QuestEnv env = new QuestEnv(getOwner(), player, questId, dialogId);
		env.setExtendedRewardIndex(extendedRewardIndex);
		if (GameEngineServices.questEngine().onDialog(env)) {
			return true;
		}
		if (dialogId == 10000) {
			GameEngineServices.skillEngine().getSkill(getOwner(), 22788, 1, player).useWithoutPropSkill();
		}
		return true;
	}
}
