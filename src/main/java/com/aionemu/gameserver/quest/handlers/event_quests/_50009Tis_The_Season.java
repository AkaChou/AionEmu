package com.aionemu.gameserver.quest.handlers.event_quests;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 活动任务脚本：Tis The Season（任务 ID 50009）。
 * Event quest script: Tis The Season (quest ID 50009).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _50009Tis_The_Season extends QuestHandler
{
	private final static int questId = 50009;
	
	public _50009Tis_The_Season() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(831032).addOnQuestStart(questId);
		qe.registerQuestNpc(831038).addOnQuestStart(questId);
		qe.registerQuestNpc(831032).addOnTalkEvent(questId);
		qe.registerQuestNpc(831038).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 831032 || targetId == 831038) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 831032 || targetId == 831038) {
				changeQuestStep(env, 0, 0, true);
				return sendQuestDialog(env, 2375);
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 831032 || targetId == 831038)
				return sendQuestEndDialog(env);
		}
		return false;
	}
}
