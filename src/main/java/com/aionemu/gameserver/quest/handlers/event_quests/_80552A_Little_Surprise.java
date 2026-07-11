package com.aionemu.gameserver.quest.handlers.event_quests;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 活动任务脚本：A Little Surprise（任务 ID 80552）。
 * Event quest script: A Little Surprise (quest ID 80552).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _80552A_Little_Surprise extends QuestHandler
{
    private final static int questId = 80552;
	
    public _80552A_Little_Surprise() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(831999).addOnQuestStart(questId); //Florarinerk.
		qe.registerQuestNpc(831999).addOnTalkEvent(questId); //Florarinerk.
		qe.registerQuestNpc(831999).addOnTalkEvent(questId); //Florarinerk.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 831999) { //Florarinerk.
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
			switch (targetId) {
				case 831999: { //Florarinerk.
					switch (dialog) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2375);
						} case SELECT_REWARD: {
							changeQuestStep(env, 0, 0, true);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 831999) { //Florarinerk.
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
