package com.aionemu.gameserver.quest.handlers.levinshor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 莱文绍尔任务脚本：No Chance For Aquamancers（任务 ID 13762）。
 * Levinshor quest script: No Chance For Aquamancers (quest ID 13762).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _13762No_Chance_For_Aquamancers extends QuestHandler {

    private final static int questId = 13762;
    public _13762No_Chance_For_Aquamancers() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805275).addOnQuestStart(questId);
		qe.registerQuestNpc(805276).addOnQuestStart(questId);
		qe.registerQuestNpc(805277).addOnQuestStart(questId);
		qe.registerQuestNpc(805275).addOnTalkEvent(questId);
		qe.registerQuestNpc(805276).addOnTalkEvent(questId);
        qe.registerQuestNpc(805277).addOnTalkEvent(questId);
		qe.registerQuestNpc(235373).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805275 || targetId == 805276 || targetId == 805277) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805275 || targetId == 805276 || targetId == 805277) {
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
                case 235373:
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
