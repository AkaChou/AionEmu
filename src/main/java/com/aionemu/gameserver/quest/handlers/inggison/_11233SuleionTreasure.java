package com.aionemu.gameserver.quest.handlers.inggison;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 因吉森任务脚本：Suleion Treasure（任务 ID 11233）。
 * Inggison quest script: Suleion Treasure (quest ID 11233).
 *
 * @author Cheatkiller
 */
public class _11233SuleionTreasure extends QuestHandler {

	private final static int questId = 11233;
	public _11233SuleionTreasure() {
		super(questId);
	}

	public void register() {
		qe.registerQuestItem(182206875, questId);
		qe.registerQuestNpc(799075).addOnTalkEvent(questId);
		qe.registerQuestNpc(798976).addOnTalkEvent(questId);
		qe.registerQuestNpc(798948).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 0) { 
				if (dialog == QuestDialog.ACCEPT_QUEST) {
					return sendQuestStartDialog(env);
				}
			    if (dialog == QuestDialog.REFUSE_QUEST) {
				   return closeDialogWindow(env);
			    }
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 799075) {
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1352);
				}
				else if (dialog == QuestDialog.STEP_TO_1) {
					giveQuestItem(env, 182206876, 1);
					return defaultCloseDialog(env, 0, 1);
				}
			}
			else if (targetId == 798976) {
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1693);
				}
				else if (dialog == QuestDialog.STEP_TO_2) {
					giveQuestItem(env, 182206877, 1);
					qs.setQuestVar(2);
					return defaultCloseDialog(env, 2, 2, true, false);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798948) {
				if (dialog == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 2375);
				}
				removeQuestItem(env, 182206875, 1);
				removeQuestItem(env, 182206876, 1);
				removeQuestItem(env, 182206877, 1);
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
					
					
	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
				return HandlerResult.fromBoolean(sendQuestDialog(env, 4));
		}
		return HandlerResult.FAILED;
	}
}
