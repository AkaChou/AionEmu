package com.aionemu.gameserver.ai.quests;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import java.util.List;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AI2Actions.SelectDialogResult;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;

/**
 * 任务相关 NPC AI：Quest Start Item Npc Ai2（@AIName "quest_start_use_item"），继承 ActionItemNpcAI2。
 * Quest-related NPC AI: Quest Start Item Npc Ai2 (@AIName "quest_start_use_item"), extends ActionItemNpcAI2.
 *
 * @author Cheatkiller
 */
@AIName("quest_start_use_item")
public class QuestStartItemNpcAi2 extends ActionItemNpcAI2 {

	@Override
	protected void handleDialogStart(Player player) {
		super.handleDialogStart(player);
	}

	@Override
	protected void handleUseItemFinish(Player player) {
		List<Integer> relatedQuests = GameEngineServices.questEngine().getQuestNpc(getOwner().getNpcId()).getOnQuestStart();
		int dialogId = relatedQuests.isEmpty() ? -1 : 26;
		SelectDialogResult dialogResult = AI2Actions.selectDialog(this, player, 0, dialogId);
		if (!dialogResult.isSuccess()) {
			if (isDialogNpc()) {
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), QuestDialog.SELECT_ACTION_1011.id()));
			}
			return;
		}
	}

	private boolean isDialogNpc() {
		return getObjectTemplate().isDialogNpc();
	}
}
