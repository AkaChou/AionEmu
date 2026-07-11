package com.aionemu.gameserver.quest.handlers.harbinger_landing;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 先驱者登陆点任务脚本：Defend Wildersage Artifact Outpost（任务 ID 25420）。
 * Harbinger Landing quest script: Defend Wildersage Artifact Outpost (quest ID 25420).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25420Defend_Wildersage_Artifact_Outpost extends QuestHandler {

    private final static int questId = 25420;
    public _25420Defend_Wildersage_Artifact_Outpost() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805414).addOnQuestStart(questId);
        qe.registerQuestNpc(805414).addOnTalkEvent(questId);
		qe.registerQuestNpc(883221).addOnKillEvent(questId);
		qe.registerQuestNpc(883222).addOnKillEvent(questId);
		qe.registerQuestNpc(883223).addOnKillEvent(questId);
		qe.registerQuestNpc(883225).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805414) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805414) {
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
                case 883221:
				case 883222:
				case 883223:
				case 883225:
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
