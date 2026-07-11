package com.aionemu.gameserver.quest.handlers.stonespear_reach;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 石矛领地任务脚本：Frenzied Monsters In The Fray（任务 ID 17161）。
 * Stonespear Reach quest script: Frenzied Monsters In The Fray (quest ID 17161).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _17161Frenzied_Monsters_In_The_Fray extends QuestHandler {

    private final static int questId = 17161;
    public _17161Frenzied_Monsters_In_The_Fray() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(804699).addOnTalkEvent(questId);
		qe.registerQuestNpc(219699).addOnKillEvent(questId);
		qe.registerQuestNpc(219776).addOnKillEvent(questId);
		qe.registerQuestNpc(219787).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 0) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804699) {
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
            switch (env.getTargetId()) {
                case 219699:
				case 219776:
				case 219787:
                if (qs.getQuestVarById(1) < 10) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 10) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
