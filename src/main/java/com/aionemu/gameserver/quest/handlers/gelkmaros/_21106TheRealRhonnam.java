package com.aionemu.gameserver.quest.handlers.gelkmaros;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 格尔克马洛斯任务脚本：The Real Rhonnam（任务 ID 21106）。
 * Gelkmaros quest script: The Real Rhonnam (quest ID 21106).
 *
 * @author Cheatkiller
 */
public class _21106TheRealRhonnam extends QuestHandler {

	private final static int questId = 21106;
	public _21106TheRealRhonnam() {
		super(questId);
	}

	public void register() {
		qe.registerQuestNpc(799271).addOnQuestStart(questId);
		qe.registerQuestNpc(799271).addOnTalkEvent(questId);
		qe.registerQuestNpc(799272).addOnTalkEvent(questId);
		qe.registerQuestNpc(799273).addOnTalkEvent(questId);
		qe.registerQuestNpc(799274).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 799271) { 
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				}
				else {
					return sendQuestStartDialog(env);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 799272) {
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1352);
				}
				else if (dialog == QuestDialog.STEP_TO_1) {
					return defaultCloseDialog(env, 0, 1);
				}
			}
			else if (targetId == 799273) {
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1693);
				}
				else if (dialog == QuestDialog.STEP_TO_2) {
					return defaultCloseDialog(env, 1, 2);
				}
			}
			else if (targetId == 799274) {
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 2034);
				}
				else if (dialog == QuestDialog.STEP_TO_3) {
					qs.setQuestVar(3);
					return defaultCloseDialog(env, 3, 3, true, false);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799271) {
				if (dialog == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 2375);
				}
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
