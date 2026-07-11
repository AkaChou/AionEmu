package com.aionemu.gameserver.quest.handlers.rentus_base;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 伦图斯基地任务脚本：Reian Whereabouts（任务 ID 30515）。
 * Rentus Base quest script: Reian Whereabouts (quest ID 30515).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _30515Reian_Whereabouts extends QuestHandler {

    private final static int questId = 30515;
    public _30515Reian_Whereabouts() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(804879).addOnQuestStart(questId);
		qe.registerQuestNpc(804879).addOnTalkEvent(questId);
		qe.registerQuestNpc(805156).addOnTalkEvent(questId);
		qe.registerQuestNpc(805156).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.START) {
			if (targetId == 804879) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
            if (targetId == 805156) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
                } if (env.getDialog() == QuestDialog.STEP_TO_1) {
					qs.setQuestVarById(0, 1);
					updateQuestStatus(env);
					return closeDialogWindow(env);
                }
            }
        } else if (qs.getStatus() == QuestStatus.START) {
            if (targetId == 805156) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    if (qs.getQuestVarById(0) == 1) {
                        return sendQuestDialog(env, 2375);
                    }
                } if (env.getDialog() == QuestDialog.SELECT_REWARD) {
                    changeQuestStep(env, 0, 1, true);
                    return sendQuestEndDialog(env);
                }
			}
        } else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805156) {
				if (env.getDialogId() == 1352) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}
