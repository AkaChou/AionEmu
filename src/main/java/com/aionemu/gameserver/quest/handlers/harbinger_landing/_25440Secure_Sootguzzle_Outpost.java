package com.aionemu.gameserver.quest.handlers.harbinger_landing;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 先驱者登陆点任务脚本：Secure Sootguzzle Outpost（任务 ID 25440）。
 * Harbinger Landing quest script: Secure Sootguzzle Outpost (quest ID 25440).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25440Secure_Sootguzzle_Outpost extends QuestHandler {

    private final static int questId = 25440;
    public _25440Secure_Sootguzzle_Outpost() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805418).addOnQuestStart(questId);
        qe.registerQuestNpc(805418).addOnTalkEvent(questId);
		qe.registerQuestNpc(883441).addOnKillEvent(questId);
		qe.registerQuestNpc(883442).addOnKillEvent(questId);
		qe.registerQuestNpc(883443).addOnKillEvent(questId);
		qe.registerQuestNpc(883444).addOnKillEvent(questId);
		qe.registerQuestNpc(883445).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805418) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805418) {
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
                case 883441:
				case 883442:
				case 883443:
				case 883444:
				case 883445:
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
