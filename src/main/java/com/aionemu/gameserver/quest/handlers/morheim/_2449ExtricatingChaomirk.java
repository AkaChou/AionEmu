package com.aionemu.gameserver.quest.handlers.morheim;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 莫尔海姆任务脚本：Extricating Chaomirk（任务 ID 2449）。
 * Morheim quest script: Extricating Chaomirk (quest ID 2449).
 *
 * @author Cheatkiller
 */
public class _2449ExtricatingChaomirk extends QuestHandler {

	private final static int questId = 2449;
	public _2449ExtricatingChaomirk() {
		super(questId);
	}

	public void register() {
		qe.registerQuestNpc(798080).addOnQuestStart(questId);
		qe.registerQuestNpc(798115).addOnTalkEvent(questId);
		qe.registerQuestNpc(798080).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 798080) { 
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				}
				else {
					return sendQuestStartDialog(env);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 798115) { 
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				}
			else if (dialog == QuestDialog.STEP_TO_1) {
				return defaultCloseDialog(env, 0, 1, true, false);
			}
		  }
		}
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798080) {
				if (dialog == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 10002);
				}
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
