package com.aionemu.gameserver.quest.handlers.reshanta;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 雷山塔任务脚本：Eliminate The Sentry Squad At The Krotan Fortress（任务 ID 23930）。
 * Reshanta quest script: Eliminate The Sentry Squad At The Krotan Fortress (quest ID 23930).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23930Eliminate_The_Sentry_Squad_At_The_Krotan_Fortress extends QuestHandler {

    private final static int questId = 23930;
	private final static int[] Ab1221Guard = {279069, 279079, 279089, 279104, 279119, 279134, 882451, 882456, 882461, 882466, 882471};
    public _23930Eliminate_The_Sentry_Squad_At_The_Krotan_Fortress() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805403).addOnQuestStart(questId); //Labori.
        qe.registerQuestNpc(805403).addOnTalkEvent(questId); //Labori.
		for (int mob: Ab1221Guard) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		}
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
		int targetId = env.getTargetId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805403) { //Labori.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 805403) { //Labori.
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
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            switch (env.getTargetId()) {
				case 279069:
				case 279079:
				case 279089:
				case 279104:
				case 279119:
				case 279134:
				case 882451:
				case 882456:
				case 882461:
				case 882466:
				case 882471:
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
