package com.aionemu.gameserver.quest.handlers.morheim;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 莫尔海姆任务脚本：A Prized Possession（任务 ID 2367）。
 * Morheim quest script: A Prized Possession (quest ID 2367).
 *
 * @author Cheatkiller
 */
public class _2367APrizedPossession extends QuestHandler {

	private final static int questId = 2367;
	int rewardIndex;
	public _2367APrizedPossession() {
		super(questId);
	}

	public void register() {
		qe.registerQuestNpc(204339).addOnQuestStart(questId);
		qe.registerQuestNpc(798080).addOnTalkEvent(questId);
		qe.registerQuestNpc(798079).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204339) { 
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				}
				else {
					return sendQuestStartDialog(env);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 204339) { 
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1003);
				}
				else if (dialog == QuestDialog.SELECT_ACTION_1012) {
					giveQuestItem(env, 182204147 , 1);
					return sendQuestDialog(env, 1012);
				}
				else if (dialog == QuestDialog.SELECT_ACTION_1097) {
					giveQuestItem(env, 182204147 , 1);
					return sendQuestDialog(env, 1097);
				}
				else if (dialog == QuestDialog.STEP_TO_10) {
					changeQuestStep(env, 0, 10, false);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return closeDialogWindow(env);
				}
				else if (dialog == QuestDialog.STEP_TO_20) {
					rewardIndex = 1;
					changeQuestStep(env, 0, 20, false);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return closeDialogWindow(env);
				}
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798079) {
				if (dialog == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 1352);
				}
			}
			else if(targetId == 798080) {
				if (dialog == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 1693);
				}
			}
			return sendQuestEndDialog(env, rewardIndex);
		}
	 return false;
	}	
}
