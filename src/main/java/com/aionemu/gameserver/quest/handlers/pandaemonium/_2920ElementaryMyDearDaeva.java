package com.aionemu.gameserver.quest.handlers.pandaemonium;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 潘德莫尼姆任务脚本：Elementary My Dear Daeva（任务 ID 2920）。
 * Pandaemonium quest script: Elementary My Dear Daeva (quest ID 2920).
 *
 * @author vlog
 */
public class _2920ElementaryMyDearDaeva extends QuestHandler {

	private static final int questId = 2920;
	private int choice = 0;

	public _2920ElementaryMyDearDaeva() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204141).addOnQuestStart(questId);
		qe.registerQuestNpc(204141).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();

		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204141) { // Deyla
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				}
				else {
					return sendQuestStartDialog(env);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 204141) { // Deyla
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					case STEP_TO_1: {
						return sendQuestDialog(env, 1352);
					}
					case STEP_TO_2: {
						return sendQuestDialog(env, 1693);
					}
					case STEP_TO_11: {
						changeQuestStep(env, 0, 0, true); // reward
						choice = 0;
						return sendQuestDialog(env, 5);
					}
					case STEP_TO_12: {
						changeQuestStep(env, 0, 0, true); // reward
						choice = 1;
						return sendQuestDialog(env, 6);
					}
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204141) { // Deyla
				return sendQuestEndDialog(env, choice);
			}
		}
		return false;
	}
}
