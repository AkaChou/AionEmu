package com.aionemu.gameserver.quest.handlers.enshar;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 恩沙尔任务脚本：Ominous Advice（任务 ID 25062）。
 * Enshar quest script: Ominous Advice (quest ID 25062).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25062Ominous_Advice extends QuestHandler {

	private static final int questId = 25062;
	public _25062Ominous_Advice() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(804917).addOnQuestStart(questId);
		qe.registerQuestNpc(804917).addOnTalkEvent(questId);
		qe.registerQuestNpc(804918).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804917) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		} 
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 804917) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					} case CHECK_COLLECTED_ITEMS: {
						return checkQuestItems(env, 0, 0, true, 10000, 10001);
					}
				}
			}
		} 
		else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804918) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
