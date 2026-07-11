package com.aionemu.gameserver.quest.handlers.cygnea;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 希格尼娅任务脚本：Drana Seed Deed（任务 ID 15044）。
 * Cygnea quest script: Drana Seed Deed (quest ID 15044).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _15044Drana_Seed_Deed extends QuestHandler {

	private static final int questId = 15044;
	public _15044Drana_Seed_Deed() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(804887).addOnQuestStart(questId);
		qe.registerQuestNpc(804887).addOnTalkEvent(questId);
		qe.registerQuestNpc(702737).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804887) {
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
		} else if (targetId == 702737) {
			if (dialog == QuestDialog.USE_OBJECT) {
				return true;
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 804887) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					} case CHECK_COLLECTED_ITEMS: {
						return checkQuestItems(env, 0, 0, true, 5, 10001);
					}
				}
			}
		} else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804887) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
