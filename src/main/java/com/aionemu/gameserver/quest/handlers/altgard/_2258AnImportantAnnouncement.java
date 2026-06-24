package com.aionemu.gameserver.quest.handlers.altgard;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

public class _2258AnImportantAnnouncement extends QuestHandler {

	private final static int questId = 2258;
	public _2258AnImportantAnnouncement() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203650).addOnQuestStart(questId);
		qe.registerQuestNpc(203650).addOnTalkEvent(questId);
		qe.registerQuestNpc(204190).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestDialog dialog = env.getDialog();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 203650) {
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				}
                else if (dialog == QuestDialog.ASK_ACCEPTION) {
					return sendQuestDialog(env, 4);
				}
                else if (dialog == QuestDialog.ACCEPT_QUEST) {
					return sendQuestStartDialog(env, 182203239, 1);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 204190) {
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 2375);
				} else if (dialog == QuestDialog.SELECT_REWARD) {
					removeQuestItem(env, 182203239, 1);
					playQuestMovie(env, 123);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return sendQuestEndDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204190) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}