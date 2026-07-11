package com.aionemu.gameserver.quest.handlers.event_quests.a_higher_calling;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 活动任务脚本：A Higher Calling 19th Edition（任务 ID 80505）。
 * Event quest script: A Higher Calling 19th Edition (quest ID 80505).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _80505A_Higher_Calling_19th_Edition extends QuestHandler
{
    private final static int questId = 80505;
	
    public _80505A_Higher_Calling_19th_Edition() {
        super(questId);
    }
/*		
	@Override
	public boolean onLvlUpEvent(QuestEnv env) {
		return defaultOnLvlUpEvent(env);
	}
*/	
	@Override
	public void register() {
		qe.registerOnLevelUp(questId);
		qe.registerQuestNpc(831031).addOnQuestStart(questId); //Nebrith.
		qe.registerQuestNpc(831031).addOnTalkEvent(questId); //Nebrith.
		qe.registerQuestNpc(831031).addOnTalkEvent(questId); //Nebrith.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 831031) { //Nebrith.
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
				case 831031: { //Nebrith.
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
		    if (targetId == 831031) { //Nebrith.
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
