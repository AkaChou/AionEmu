package com.aionemu.gameserver.quest.handlers.levinshor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 莱文绍尔任务脚本：Excitement In The North（任务 ID 23767）。
 * Levinshor quest script: Excitement In The North (quest ID 23767).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23767Excitement_In_The_North extends QuestHandler {

    private final static int questId = 23767;
    public _23767Excitement_In_The_North() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805282).addOnQuestStart(questId);
		qe.registerQuestNpc(805283).addOnQuestStart(questId);
		qe.registerQuestNpc(805284).addOnQuestStart(questId);
        qe.registerQuestNpc(805282).addOnTalkEvent(questId);
		qe.registerQuestNpc(805283).addOnTalkEvent(questId);
		qe.registerQuestNpc(805284).addOnTalkEvent(questId);
		qe.registerQuestNpc(235350).addOnKillEvent(questId);
		qe.registerQuestNpc(235351).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805282 || targetId == 805283 || targetId == 805284) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805282 || targetId == 805283 || targetId == 805284) {
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
                case 235350:
				case 235351:
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
