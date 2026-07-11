package com.aionemu.gameserver.quest.handlers.levinshor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 莱文绍尔任务脚本：Decrease The East（任务 ID 23761）。
 * Levinshor quest script: Decrease The East (quest ID 23761).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23761Decrease_The_East extends QuestHandler {

    private final static int questId = 23761;
    public _23761Decrease_The_East() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805285).addOnQuestStart(questId);
		qe.registerQuestNpc(805286).addOnQuestStart(questId);
		qe.registerQuestNpc(805287).addOnQuestStart(questId);
        qe.registerQuestNpc(805285).addOnTalkEvent(questId);
		qe.registerQuestNpc(805286).addOnTalkEvent(questId);
		qe.registerQuestNpc(805287).addOnTalkEvent(questId);
		qe.registerQuestNpc(235358).addOnKillEvent(questId);
		qe.registerQuestNpc(235359).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805285 || targetId == 805286 || targetId == 805287) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805285 || targetId == 805286 || targetId == 805287) {
				if (env.getDialogId() == 1009) {
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
                case 235358:
				case 235359:
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
