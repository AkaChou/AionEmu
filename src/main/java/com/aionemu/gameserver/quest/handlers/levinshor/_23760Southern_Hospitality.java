package com.aionemu.gameserver.quest.handlers.levinshor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 莱文绍尔任务脚本：Southern Hospitality（任务 ID 23760）。
 * Levinshor quest script: Southern Hospitality (quest ID 23760).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23760Southern_Hospitality extends QuestHandler {

    private final static int questId = 23760;
    public _23760Southern_Hospitality() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805291).addOnQuestStart(questId);
		qe.registerQuestNpc(805292).addOnQuestStart(questId);
		qe.registerQuestNpc(805293).addOnQuestStart(questId);
        qe.registerQuestNpc(805291).addOnTalkEvent(questId);
		qe.registerQuestNpc(805292).addOnTalkEvent(questId);
		qe.registerQuestNpc(805293).addOnTalkEvent(questId);
		qe.registerQuestNpc(235368).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805291 || targetId == 805292 || targetId == 805293) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805291 || targetId == 805292 || targetId == 805293) {
				if (env.getDialogId() == 1009) {
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
                case 235368:
                if (qs.getQuestVarById(1) < 5) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 5) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
