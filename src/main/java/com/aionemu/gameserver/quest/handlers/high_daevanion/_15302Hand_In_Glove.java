package com.aionemu.gameserver.quest.handlers.high_daevanion;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 高等大天使任务脚本：Hand In Glove（任务 ID 15302）。
 * High Daevanion quest script: Hand In Glove (quest ID 15302).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _15302Hand_In_Glove extends QuestHandler {

    private final static int questId = 15302;
    public _15302Hand_In_Glove() {
        super(questId);
    }
	
	public void register() {
		qe.registerQuestNpc(805327).addOnQuestStart(questId); //Rike.
		qe.registerQuestNpc(805327).addOnTalkEvent(questId); //Rike.
		qe.registerQuestNpc(805329).addOnTalkEvent(questId); //Argon.
	}
	
	@Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
        int targetId = env.getTargetId();
        if (qs == null) {
            return false;
        }
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 805327) { //Rike.
                switch (env.getDialog()) {
                    case START_DIALOG: {
                        return sendQuestDialog(env, 4762);
					} case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE: {
						return sendQuestStartDialog(env);
					} case REFUSE_QUEST_SIMPLE: {
				        return closeDialogWindow(env);
					}
                }
			}
		} else if (qs.getStatus() == QuestStatus.START) {
		    int var = qs.getQuestVarById(0);
			if (targetId == 805329) { //Argon.
				switch (env.getDialog()) {
                    case START_DIALOG: {
                        if (var == 0) {
							return sendQuestDialog(env, 1011);
						} else if (var == 1) {
							return sendQuestDialog(env, 1352);
						} else if (var == 2) {
							return sendQuestDialog(env, 1693);
						}
					} case SELECT_ACTION_1012: {
						if (var == 0) {
							return sendQuestDialog(env, 1012);
						}
					} case STEP_TO_1: {
						changeQuestStep(env, 0, 1, false);
						return closeDialogWindow(env);
					} case SET_REWARD: {
						giveQuestItem(env, 182215860, 1); //Daevanion Glove Prototype.
						removeQuestItem(env, 182215832, 70); //Kaisinel's Pattern Of Tenacity.
						changeQuestStep(env, 2, 3, true);
						return closeDialogWindow(env);
					} case CHECK_COLLECTED_ITEMS: {
						return checkQuestItems(env, 1, 2, false, 10000, 10001);
					}
                }
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 805327) { //Rike.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 10002);
                } else {
                    return sendQuestEndDialog(env);
                }
            }
        }
		return false;
	}
}
