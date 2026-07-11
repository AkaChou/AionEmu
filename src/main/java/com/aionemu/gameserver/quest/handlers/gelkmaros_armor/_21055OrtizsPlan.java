package com.aionemu.gameserver.quest.handlers.gelkmaros_armor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 格尔克马洛斯防具任务脚本：Ortizs Plan（任务 ID 21055）。
 * Gelkmaros armor quest script: Ortizs Plan (quest ID 21055).
 */
public class _21055OrtizsPlan extends QuestHandler {

	private final static int questId = 21055;
	public _21055OrtizsPlan() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(799295).addOnQuestStart(questId); //Ortiz
		qe.registerQuestNpc(799295).addOnTalkEvent(questId); //Ortiz
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 799295) { //Ortiz
				switch (dialog) {
					case START_DIALOG:
						return sendQuestDialog(env, 1011);
					case SELECT_ACTION_1012: {
						return sendQuestDialog(env, 1012);
					} case ASK_ACCEPTION: {
						return sendQuestDialog(env, 4);
					} case ACCEPT_QUEST: {
						return sendQuestStartDialog(env);
					} case REFUSE_QUEST: {
						return sendQuestDialog(env, 1004);
					}
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.START) {
			if (targetId == 799295) { //Ortiz
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 2375);
				} else if (dialog == QuestDialog.CHECK_COLLECTED_ITEMS) {
					long itemCount = player.getInventory().getItemCountByItemId(182207843);
					long itemCount1 = player.getInventory().getItemCountByItemId(182207844);
					if (player.getInventory().tryDecreaseKinah(200000) && itemCount > 3  && itemCount1 > 19) {
						player.getInventory().decreaseByItemId(182207843, 4);
						player.getInventory().decreaseByItemId(182207844, 20);
						changeQuestStep(env, 0, 0, true);
						return sendQuestDialog(env, 5);
					} else
						return sendQuestDialog(env, 2716);
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799295) { //Ortiz
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
