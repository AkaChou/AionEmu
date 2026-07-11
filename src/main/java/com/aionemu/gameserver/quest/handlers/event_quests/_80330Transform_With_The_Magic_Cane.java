package com.aionemu.gameserver.quest.handlers.event_quests;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 活动任务脚本：Transform With The Magic Cane（任务 ID 80330）。
 * Event quest script: Transform With The Magic Cane (quest ID 80330).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _80330Transform_With_The_Magic_Cane extends QuestHandler
{
	private final static int questId = 80330;

	public _80330Transform_With_The_Magic_Cane() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(831527).addOnQuestStart(questId);
		qe.registerQuestNpc(831527).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if(qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 831527) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					} case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 831527)
				switch (dialog) {
					case START_DIALOG:
					case SELECT_REWARD: {
						changeQuestStep(env, 0, 0, true);
						return sendQuestDialog(env, 5);	
					}
				}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 831527)
				return sendQuestEndDialog(env);
		}
		return false;
	}
}
