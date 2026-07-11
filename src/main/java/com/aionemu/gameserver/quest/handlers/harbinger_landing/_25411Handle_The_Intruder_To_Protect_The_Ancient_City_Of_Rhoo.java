package com.aionemu.gameserver.quest.handlers.harbinger_landing;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 先驱者登陆点任务脚本：Handle The Intruder To Protect The Ancient City Of Rhoo（任务 ID 25411）。
 * Harbinger Landing quest script: Handle The Intruder To Protect The Ancient City Of Rhoo (quest ID 25411).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25411Handle_The_Intruder_To_Protect_The_Ancient_City_Of_Rhoo extends QuestHandler {

    private final static int questId = 25411;
    public _25411Handle_The_Intruder_To_Protect_The_Ancient_City_Of_Rhoo() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805402).addOnQuestStart(questId);
        qe.registerQuestNpc(805402).addOnTalkEvent(questId);
		qe.registerQuestNpc(884009).addOnKillEvent(questId);
		qe.registerQuestNpc(884010).addOnKillEvent(questId);
		qe.registerQuestNpc(884011).addOnKillEvent(questId);
		qe.registerQuestNpc(884012).addOnKillEvent(questId);
		qe.registerQuestNpc(884013).addOnKillEvent(questId);
		qe.registerQuestNpc(884014).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 805402) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805402) {
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
                case 884009:
				case 884010:
				case 884011:
				case 884012:
				case 884013:
				case 884014:
                if (qs.getQuestVarById(1) < 2) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 2) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
