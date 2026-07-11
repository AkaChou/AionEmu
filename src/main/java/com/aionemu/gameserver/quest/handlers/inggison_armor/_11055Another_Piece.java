package com.aionemu.gameserver.quest.handlers.inggison_armor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 因吉森防具任务脚本：Another Piece（任务 ID 11055）。
 * Inggison armor quest script: Another Piece (quest ID 11055).
 */
public class _11055Another_Piece extends QuestHandler {

	private final static int questId = 11055;
	public _11055Another_Piece() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(798990).addOnQuestStart(questId); //Titus
		qe.registerQuestNpc(798990).addOnTalkEvent(questId); //Titus
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 798990) { //Titus
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
			if (targetId == 798990) { //Titus
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 2375);
				} else if (dialog == QuestDialog.CHECK_COLLECTED_ITEMS) {
					long itemCount = player.getInventory().getItemCountByItemId(182206840);
					long itemCount1 = player.getInventory().getItemCountByItemId(182206841);
					if (player.getInventory().tryDecreaseKinah(200000) && itemCount > 3 && itemCount1 > 19) {
						player.getInventory().decreaseByItemId(182206840, 4);
						player.getInventory().decreaseByItemId(182206841, 20);
						changeQuestStep(env, 0, 0, true);
						return sendQuestDialog(env, 5);
					} else
						return sendQuestDialog(env, 2716);
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798990) { //Titus
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
