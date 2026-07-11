package com.aionemu.gameserver.quest.handlers.norsvold;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 诺斯沃尔德任务脚本：Protect The Swamp Of Serenity（任务 ID 25503）。
 * Norsvold quest script: Protect The Swamp Of Serenity (quest ID 25503).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25503Protect_The_Swamp_Of_Serenity extends QuestHandler {

    private final static int questId = 25503;
    public _25503Protect_The_Swamp_Of_Serenity() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(806102).addOnQuestStart(questId);
        qe.registerQuestNpc(806102).addOnTalkEvent(questId);
		qe.registerQuestNpc(241508).addOnKillEvent(questId);
		qe.registerQuestNpc(241509).addOnKillEvent(questId);
		qe.registerQuestNpc(241510).addOnKillEvent(questId);
		qe.registerQuestNpc(241511).addOnKillEvent(questId);
		qe.registerQuestNpc(241512).addOnKillEvent(questId);
		qe.registerQuestNpc(241513).addOnKillEvent(questId);
		qe.registerQuestNpc(241514).addOnKillEvent(questId);
		qe.registerQuestNpc(241515).addOnKillEvent(questId);
		qe.registerQuestNpc(241516).addOnKillEvent(questId);
		qe.registerQuestNpc(241517).addOnKillEvent(questId);
		qe.registerQuestNpc(241518).addOnKillEvent(questId);
		qe.registerQuestNpc(241519).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 806102) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } 
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 806102) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 10002);
				} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
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
                case 241508:
                case 241509:
                case 241510:
                case 241511:
                case 241512:
                case 241513:
                case 241514:
                case 241515:
                case 241516:
                case 241517:
                case 241518:
                case 241519:
                if (qs.getQuestVarById(1) < 20) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 20) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
