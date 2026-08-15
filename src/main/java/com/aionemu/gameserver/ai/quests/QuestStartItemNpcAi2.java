package com.aionemu.gameserver.ai.quests;

import java.util.List;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.utils.PacketSendUtility;

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
	/**
	 * 使用任务启动物品后，弹出与该物品关联任务的开始对话框。
	 * After using the quest start item, opens the dialog to start the quests related to this NPC.
	 */
	protected void handleUseItemFinish(Player player) {
		List<Integer> relatedQuests = GameEngineServices.questEngine().getQuestNpc(getOwner().getNpcId()).getOnQuestStart();
		for (int dialogId : dialogIdsFor(!relatedQuests.isEmpty())) {
			if (AI2Actions.selectDialog(this, player, 0, dialogId).isSuccess()) {
				return;
			}
		}
		if (isDialogNpc()) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), QuestDialog.SELECT_ACTION_1011.id()));
		}
	}

	static List<Integer> dialogIdsFor(boolean hasQuestStart) {
		return hasQuestStart
			? List.of(QuestDialog.USE_OBJECT.id(), QuestDialog.START_DIALOG.id())
			: List.of(QuestDialog.USE_OBJECT.id());
	}

	private boolean isDialogNpc() {
		return getObjectTemplate().isDialogNpc();
	}
}
