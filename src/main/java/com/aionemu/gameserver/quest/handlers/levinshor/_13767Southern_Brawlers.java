package com.aionemu.gameserver.quest.handlers.levinshor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 莱文绍尔任务脚本：Southern Brawlers（任务 ID 13767）。
 * Levinshor quest script: Southern Brawlers (quest ID 13767).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _13767Southern_Brawlers extends QuestHandler {

    private final static int questId = 13767;
    public _13767Southern_Brawlers() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805278).addOnQuestStart(questId);
		qe.registerQuestNpc(805279).addOnQuestStart(questId);
		qe.registerQuestNpc(805280).addOnQuestStart(questId);
        qe.registerQuestNpc(805278).addOnTalkEvent(questId);
		qe.registerQuestNpc(805279).addOnTalkEvent(questId);
		qe.registerQuestNpc(805280).addOnTalkEvent(questId);
		qe.registerQuestNpc(235362).addOnKillEvent(questId);
		qe.registerQuestNpc(235363).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805278 || targetId == 805279 || targetId == 805280) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805278 || targetId == 805279 || targetId == 805280) {
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
                case 235362:
				case 235363:
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
