package com.aionemu.gameserver.quest.handlers.heiron;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 希隆任务脚本：A Secret Summons（任务 ID 3913）。
 * Heiron quest script: A Secret Summons (quest ID 3913).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _3913A_Secret_Summons extends QuestHandler {

	private final static int questId = 3913;
	public _3913A_Secret_Summons() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204504).addOnQuestStart(questId); //Sofne.
		qe.registerQuestNpc(204504).addOnTalkEvent(questId); //Sofne.
		qe.registerQuestNpc(204505).addOnTalkEvent(questId); //Sulates.
		qe.registerQuestNpc(204656).addOnTalkEvent(questId); //Maloren.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (env.getTargetId() == 204504) { //Sofne.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 1011);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } else if (env.getTargetId() == 204505) { //Sulates.
			if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 0) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1352);
				} else if (env.getDialog() == QuestDialog.STEP_TO_1) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					updateQuestStatus(env);
                    return closeDialogWindow(env);
				}
			}
		} else if (env.getTargetId() == 204656) { //Maloren.
			if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 1) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 2375);
				} else if (env.getDialogId() == 1009) {
					qs.setQuestVar(2);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return sendQuestEndDialog(env);
				}
			}
            else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
		    return sendQuestEndDialog(env);
            }
		}
		return false;
	}
}
