package com.aionemu.gameserver.quest.handlers.levinshor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 莱文绍尔任务脚本：Beasts In The East（任务 ID 13764）。
 * Levinshor quest script: Beasts In The East (quest ID 13764).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _13764Beasts_In_The_East extends QuestHandler {

    private final static int questId = 13764;
    public _13764Beasts_In_The_East() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805272).addOnQuestStart(questId);
        qe.registerQuestNpc(805273).addOnQuestStart(questId);
		qe.registerQuestNpc(805274).addOnQuestStart(questId);
		qe.registerQuestNpc(805272).addOnTalkEvent(questId);
		qe.registerQuestNpc(805273).addOnTalkEvent(questId);
		qe.registerQuestNpc(805274).addOnTalkEvent(questId);
		qe.registerQuestNpc(235354).addOnKillEvent(questId);
		qe.registerQuestNpc(235355).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805272 || targetId == 805273 || targetId == 805274) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805272 || targetId == 805273 || targetId == 805274) {
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
                case 235354:
				case 235355:
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
