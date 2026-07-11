package com.aionemu.gameserver.quest.handlers.adma_fall;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 阿德玛陷落任务脚本：Fallen Doorways（任务 ID 28990）。
 * Adma Fall quest script: Fallen Doorways (quest ID 28990).
 *
 * @author (Encom)
 */
public class _28990Fallen_Doorways extends QuestHandler {

    private final static int questId = 28990;
    public _28990Fallen_Doorways() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(806079).addOnQuestStart(questId); //Feregran.
		qe.registerQuestNpc(806079).addOnTalkEvent(questId); //Feregran.
		qe.registerQuestNpc(806216).addOnTalkEvent(questId); //Petur.
		qe.registerQuestNpc(220417).addOnKillEvent(questId);
	    qe.registerQuestNpc(220418).addOnKillEvent(questId);
		qe.registerQuestNpc(220427).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 806079) { //Feregran.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.START) {
            if (targetId == 806216) { //Petur.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
                } if (env.getDialog() == QuestDialog.STEP_TO_1) {
					qs.setQuestVarById(0, 1);
					updateQuestStatus(env);
					return closeDialogWindow(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 806079) { //Feregran.
				if (env.getDialogId() == 1352) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
        return false;
    }
	
    @Override
    public boolean onKillEvent(QuestEnv env) {
    Player player = env.getPlayer();
    QuestState qs = player.getQuestStateList().getQuestState(questId);
    if (qs == null) {
        return false;
    }
    int targetId = env.getTargetId();
    if (qs.getStatus() != QuestStatus.START) {
        return false;
    }
    int var = qs.getQuestVarById(0);
    
    if (var == 1) {
        if (targetId == 220417) {
            qs.setQuestVarById(1, 1);
        } else if (targetId == 220418) {
            qs.setQuestVarById(2, 1);
        }
        updateQuestStatus(env);
        
        if (qs.getQuestVarById(1) == 1 && qs.getQuestVarById(2) == 1) {
            qs.setQuestVarById(1, 0);
            qs.setQuestVarById(2, 0);
            qs.setQuestVarById(0, 2);
            return true;
        }
    } else if (var == 2) {
        if (targetId == 220427) {
            qs.setQuestVarById(0, 3);
            updateQuestStatus(env);
            qs.setStatus(QuestStatus.REWARD);
            updateQuestStatus(env);
            return true;
        }
    }
    return false;
    }
}
