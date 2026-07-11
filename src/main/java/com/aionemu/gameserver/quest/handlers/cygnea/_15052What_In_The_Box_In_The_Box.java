package com.aionemu.gameserver.quest.handlers.cygnea;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 希格尼娅任务脚本：What In The Box In The Box（任务 ID 15052）。
 * Cygnea quest script: What In The Box In The Box (quest ID 15052).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _15052What_In_The_Box_In_The_Box extends QuestHandler {

	private static final int questId = 15052;
	public _15052What_In_The_Box_In_The_Box() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(804888).addOnQuestStart(questId);
		qe.registerQuestNpc(804888).addOnTalkEvent(questId);
		qe.registerQuestNpc(702738).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804888) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		} else if (targetId == 702738) {
			if (dialog == QuestDialog.USE_OBJECT) {
				return true;
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 804888) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					} case CHECK_COLLECTED_ITEMS: {
						return checkQuestItems(env, 0, 0, true, 5, 10001);
					}
				}
			}
		} else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804888) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
