package com.aionemu.gameserver.quest.handlers.harbinger_landing;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 先驱者登陆点任务脚本：Gearing Up For Occupation（任务 ID 25475）。
 * Harbinger Landing quest script: Gearing Up For Occupation (quest ID 25475).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25475Gearing_Up_For_Occupation extends QuestHandler {

    private final static int questId = 25475;
    public _25475Gearing_Up_For_Occupation() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805815).addOnQuestStart(questId);
        qe.registerQuestNpc(805815).addOnTalkEvent(questId);
		qe.registerQuestNpc(883184).addOnKillEvent(questId);
		qe.registerQuestNpc(883196).addOnKillEvent(questId);
		qe.registerQuestNpc(883208).addOnKillEvent(questId);
		qe.registerQuestNpc(883034).addOnKillEvent(questId);
		qe.registerQuestNpc(883040).addOnKillEvent(questId);
		qe.registerQuestNpc(883046).addOnKillEvent(questId);
		qe.registerQuestNpc(883186).addOnKillEvent(questId);
		qe.registerQuestNpc(883198).addOnKillEvent(questId);
		qe.registerQuestNpc(883210).addOnKillEvent(questId);
		qe.registerQuestNpc(883036).addOnKillEvent(questId);
		qe.registerQuestNpc(883042).addOnKillEvent(questId);
		qe.registerQuestNpc(883048).addOnKillEvent(questId);
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
				case 883184:
				case 883196:
				case 883208:
				case 883034:
				case 883040:
				case 883046:
				case 883186:
				case 883198:
				case 883210:
				case 883036:
				case 883042:
				case 883048:
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
