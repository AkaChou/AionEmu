package com.aionemu.gameserver.quest.handlers.levinshor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 莱文绍尔任务脚本：Give Your Best To The West（任务 ID 23764）。
 * Levinshor quest script: Give Your Best To The West (quest ID 23764).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23764Give_Your_Best_To_The_West extends QuestHandler {

    private final static int questId = 23764;
    public _23764Give_Your_Best_To_The_West() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805288).addOnQuestStart(questId);
		qe.registerQuestNpc(805289).addOnQuestStart(questId);
		qe.registerQuestNpc(805290).addOnQuestStart(questId);
        qe.registerQuestNpc(805288).addOnTalkEvent(questId);
		qe.registerQuestNpc(805289).addOnTalkEvent(questId);
		qe.registerQuestNpc(805290).addOnTalkEvent(questId);
		qe.registerQuestNpc(235374).addOnKillEvent(questId);
		qe.registerQuestNpc(235375).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805288 || targetId == 805289 || targetId == 805290) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805288 || targetId == 805289 || targetId == 805290) {
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
                case 235374:
				case 235375:
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
