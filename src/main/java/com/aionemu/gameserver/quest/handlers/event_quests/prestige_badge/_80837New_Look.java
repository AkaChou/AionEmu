package com.aionemu.gameserver.quest.handlers.event_quests.prestige_badge;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 活动任务脚本：New Look（任务 ID 80837）。
 * Event quest script: New Look (quest ID 80837).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _80837New_Look extends QuestHandler
{
	private static final int questId = 80837;
	
	public _80837New_Look() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(833743).addOnQuestStart(questId);
		qe.registerQuestNpc(833743).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 833743) {
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
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 833743) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					} case CHECK_COLLECTED_ITEMS_SIMPLE: {
						return checkQuestItems(env, 0, 0, true, 5, 2716);
					} case FINISH_DIALOG: {
						return sendQuestSelectionDialog(env);
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 833743) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
