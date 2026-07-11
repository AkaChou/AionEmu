package com.aionemu.gameserver.quest.handlers.harbinger_landing;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 先驱者登陆点任务脚本：Taking The Outposts（任务 ID 25472）。
 * Harbinger Landing quest script: Taking The Outposts (quest ID 25472).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25472Taking_The_Outposts extends QuestHandler {

    private final static int questId = 25472;
    public _25472Taking_The_Outposts() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805815).addOnQuestStart(questId);
        qe.registerQuestNpc(805815).addOnTalkEvent(questId);
		qe.registerQuestNpc(883088).addOnKillEvent(questId);
		qe.registerQuestNpc(883112).addOnKillEvent(questId);
		qe.registerQuestNpc(883136).addOnKillEvent(questId);
		qe.registerQuestNpc(882986).addOnKillEvent(questId);
		qe.registerQuestNpc(882998).addOnKillEvent(questId);
		qe.registerQuestNpc(883010).addOnKillEvent(questId);
		qe.registerQuestNpc(883090).addOnKillEvent(questId);
		qe.registerQuestNpc(883114).addOnKillEvent(questId);
		qe.registerQuestNpc(883138).addOnKillEvent(questId);
		qe.registerQuestNpc(882988).addOnKillEvent(questId);
		qe.registerQuestNpc(883000).addOnKillEvent(questId);
		qe.registerQuestNpc(883012).addOnKillEvent(questId);
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
				case 883088:
				case 883112:
				case 883136:
				case 882986:
				case 882998:
				case 883010:
				case 883090:
				case 883114:
				case 883138:
				case 882988:
				case 883000:
				case 883012:
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
