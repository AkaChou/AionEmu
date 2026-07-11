package com.aionemu.gameserver.quest.handlers.iluma;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 伊卢玛任务脚本：Primordial Creatures At Ariel Rest（任务 ID 15634）。
 * Iluma quest script: Primordial Creatures At Ariel Rest (quest ID 15634).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _15634Primordial_Creatures_At_Ariel_Rest extends QuestHandler {

    private final static int questId = 15634;
    public _15634Primordial_Creatures_At_Ariel_Rest() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(731684).addOnQuestStart(questId);
        qe.registerQuestNpc(806090).addOnTalkEvent(questId);
		qe.registerQuestNpc(243353).addOnKillEvent(questId);
		qe.registerQuestNpc(243355).addOnKillEvent(questId);
		qe.registerQuestNpc(243356).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 731684) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 806090) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 10002);
				} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
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
            switch (env.getTargetId()) {
				case 243353:
				case 243355:
				case 243356:
                if (qs.getQuestVarById(1) < 3) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 3) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
