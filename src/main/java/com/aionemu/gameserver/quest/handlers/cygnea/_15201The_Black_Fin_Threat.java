package com.aionemu.gameserver.quest.handlers.cygnea;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 希格尼娅任务脚本：The Black Fin Threat（任务 ID 15201）。
 * Cygnea quest script: The Black Fin Threat (quest ID 15201).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _15201The_Black_Fin_Threat extends QuestHandler {

    private final static int questId = 15201;
	private final static int[] mobs = {235836, 235837, 235842, 235843, 235848, 235849};
    public _15201The_Black_Fin_Threat() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(804704).addOnQuestStart(questId);
        qe.registerQuestNpc(804704).addOnTalkEvent(questId);
		for (int mob: mobs) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		}
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 804704) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804704) {
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
				case 235836:
				case 235837:
				case 235842:
				case 235843:
				case 235848:
				case 235849:
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
