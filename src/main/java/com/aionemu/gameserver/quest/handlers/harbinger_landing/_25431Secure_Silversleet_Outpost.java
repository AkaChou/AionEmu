package com.aionemu.gameserver.quest.handlers.harbinger_landing;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 先驱者登陆点任务脚本：Secure Silversleet Outpost（任务 ID 25431）。
 * Harbinger Landing quest script: Secure Silversleet Outpost (quest ID 25431).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25431Secure_Silversleet_Outpost extends QuestHandler {

    private final static int questId = 25431;
    public _25431Secure_Silversleet_Outpost() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805409).addOnQuestStart(questId);
        qe.registerQuestNpc(805409).addOnTalkEvent(questId);
		qe.registerQuestNpc(883346).addOnKillEvent(questId);
		qe.registerQuestNpc(883347).addOnKillEvent(questId);
		qe.registerQuestNpc(883348).addOnKillEvent(questId);
		qe.registerQuestNpc(883349).addOnKillEvent(questId);
		qe.registerQuestNpc(883350).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805409) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805409) {
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
                case 883346:
				case 883347:
				case 883348:
				case 883349:
				case 883350:
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
