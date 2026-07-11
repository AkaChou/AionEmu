package com.aionemu.gameserver.quest.handlers.levinshor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 莱文绍尔任务脚本：The Unknown Balaurea（任务 ID 13700）。
 * Levinshor quest script: The Unknown Balaurea (quest ID 13700).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _13700The_Unknown_Balaurea extends QuestHandler {

    private final static int questId = 13700;
    public _13700The_Unknown_Balaurea() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(804699).addOnQuestStart(questId); //Atmis.
		qe.registerQuestNpc(804699).addOnTalkEvent(questId); //Atmis.
		qe.registerQuestNpc(802350).addOnTalkEvent(questId); //Eljer.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804699) { //Atmis.
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 802350: { //Eljer.
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2375);
						} case SELECT_REWARD: {
							playQuestMovie(env, 716);
							changeQuestStep(env, 0, 0, true);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 802350) { //Eljer.
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
