package com.aionemu.gameserver.quest.handlers.harbinger_landing;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 先驱者登陆点任务脚本：Ruins Of Roah Occupation（任务 ID 25474）。
 * Harbinger Landing quest script: Ruins Of Roah Occupation (quest ID 25474).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25474Ruins_Of_Roah_Occupation extends QuestHandler {

    private final static int questId = 25474;
    public _25474Ruins_Of_Roah_Occupation() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805815).addOnQuestStart(questId);
        qe.registerQuestNpc(805815).addOnTalkEvent(questId);
		qe.registerQuestNpc(883148).addOnKillEvent(questId);
		qe.registerQuestNpc(883160).addOnKillEvent(questId);
		qe.registerQuestNpc(883172).addOnKillEvent(questId);
		qe.registerQuestNpc(883016).addOnKillEvent(questId);
		qe.registerQuestNpc(883022).addOnKillEvent(questId);
		qe.registerQuestNpc(883028).addOnKillEvent(questId);
		qe.registerQuestNpc(883150).addOnKillEvent(questId);
		qe.registerQuestNpc(883162).addOnKillEvent(questId);
		qe.registerQuestNpc(883174).addOnKillEvent(questId);
		qe.registerQuestNpc(883018).addOnKillEvent(questId);
		qe.registerQuestNpc(883024).addOnKillEvent(questId);
		qe.registerQuestNpc(883030).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805815) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805815) {
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
				case 883148:
				case 883160:
				case 883172:
				case 883016:
				case 883022:
				case 883028:
				case 883150:
				case 883162:
				case 883174:
				case 883018:
				case 883024:
				case 883030:
                if (qs.getQuestVarById(1) < 1) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 1) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
