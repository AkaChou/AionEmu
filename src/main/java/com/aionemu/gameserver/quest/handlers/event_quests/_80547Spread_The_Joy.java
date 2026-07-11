package com.aionemu.gameserver.quest.handlers.event_quests;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 活动任务脚本：Spread The Joy（任务 ID 80547）。
 * Event quest script: Spread The Joy (quest ID 80547).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _80547Spread_The_Joy extends QuestHandler
{
    private final static int questId = 80547;
	
    public _80547Spread_The_Joy() {
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
