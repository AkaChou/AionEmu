package com.aionemu.gameserver.quest.handlers.cygnea;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 希格尼娅任务脚本：Time For Your Close Up（任务 ID 15101）。
 * Cygnea quest script: Time For Your Close Up (quest ID 15101).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _15101Time_For_Your_Close_Up extends QuestHandler {

    private final static int questId = 15101;
    public _15101Time_For_Your_Close_Up() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(804711).addOnQuestStart(questId);
		qe.registerQuestNpc(804715).addOnTalkEvent(questId);
		qe.registerQuestNpc(235939).addOnKillEvent(questId);
		qe.registerQuestNpc(235940).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 804711) {
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } else if (qs.getStatus() == QuestStatus.START) {
            if (targetId == 804715) {
                if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
                } if (dialog == QuestDialog.STEP_TO_1) {
					qs.setQuestVarById(0, 1);
					updateQuestStatus(env);
					return closeDialogWindow(env);
                }
            }
        } 
        else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804715) {
				if (env.getDialogId() == 1352) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
        return false;
    }
	
    public boolean onKillEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (var == 1) {
            switch (env.getTargetId()) {
				case 235939:
				case 235940:
                if (qs.getQuestVarById(1) < 10) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 10) {
					qs.setQuestVar(2);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
                    }
				}
            }
        }
        return false;
    }
}
