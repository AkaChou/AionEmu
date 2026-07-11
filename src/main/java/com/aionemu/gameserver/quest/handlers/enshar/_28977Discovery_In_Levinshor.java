package com.aionemu.gameserver.quest.handlers.enshar;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 恩沙尔任务脚本：Discovery In Levinshor（任务 ID 28977）。
 * Enshar quest script: Discovery In Levinshor (quest ID 28977).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _28977Discovery_In_Levinshor extends QuestHandler {
	
	private static final int questId = 28977;
	public _28977Discovery_In_Levinshor() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(802353).addOnQuestStart(questId);
		qe.registerQuestNpc(805218).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 802353) {
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
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 805218) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					} case CHECK_COLLECTED_ITEMS: {
					    return checkQuestItems(env, 0, 0, true, 10000, 10001);
					}
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805218) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
