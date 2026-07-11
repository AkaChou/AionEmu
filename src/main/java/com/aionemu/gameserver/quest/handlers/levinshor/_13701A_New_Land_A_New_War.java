package com.aionemu.gameserver.quest.handlers.levinshor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 莱文绍尔任务脚本：A New Land A New War（任务 ID 13701）。
 * Levinshor quest script: A New Land A New War (quest ID 13701).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _13701A_New_Land_A_New_War extends QuestHandler {

    private final static int questId = 13701;
    public _13701A_New_Land_A_New_War() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(798926).addOnQuestStart(questId); //Outremus.
		qe.registerQuestNpc(798926).addOnTalkEvent(questId); //Outremus.
		qe.registerQuestNpc(802350).addOnTalkEvent(questId); //Eljer.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 802350) { //Outremus.
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
