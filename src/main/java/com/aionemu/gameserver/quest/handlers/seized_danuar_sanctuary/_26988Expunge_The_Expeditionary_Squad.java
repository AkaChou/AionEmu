package com.aionemu.gameserver.quest.handlers.seized_danuar_sanctuary;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 被占达努阿尔圣所任务脚本：Expunge The Expeditionary Squad（任务 ID 26988）。
 * Seized Danuar Sanctuary quest script: Expunge The Expeditionary Squad (quest ID 26988).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _26988Expunge_The_Expeditionary_Squad extends QuestHandler {

    private final static int questId = 26988;
    public _26988Expunge_The_Expeditionary_Squad() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(801954).addOnQuestStart(questId);
        qe.registerQuestNpc(801954).addOnTalkEvent(questId);
		qe.registerQuestNpc(804865).addOnTalkEvent(questId);
		qe.registerQuestNpc(233126).addOnKillEvent(questId);
		qe.registerQuestNpc(233127).addOnKillEvent(questId);
		qe.registerQuestNpc(233128).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 801954) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804865) {
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
                case 233126:
				case 233127:
				case 233128:
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
