package com.aionemu.gameserver.quest.handlers.harbinger_landing;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 先驱者登陆点任务脚本：Defend Brokenblade Artifact Outpost（任务 ID 25423）。
 * Harbinger Landing quest script: Defend Brokenblade Artifact Outpost (quest ID 25423).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25423Defend_Brokenblade_Artifact_Outpost extends QuestHandler {

    private final static int questId = 25423;
    public _25423Defend_Brokenblade_Artifact_Outpost() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805417).addOnQuestStart(questId);
        qe.registerQuestNpc(805417).addOnTalkEvent(questId);
		qe.registerQuestNpc(883257).addOnKillEvent(questId);
		qe.registerQuestNpc(883258).addOnKillEvent(questId);
		qe.registerQuestNpc(883259).addOnKillEvent(questId);
		qe.registerQuestNpc(883261).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805417) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805417) {
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
                case 883257:
				case 883258:
				case 883259:
				case 883261:
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
