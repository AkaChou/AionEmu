package com.aionemu.gameserver.quest.handlers.reshanta;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 雷山塔任务脚本（任务 ID 23945）。
 * Reshanta quest script (quest ID 23945).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23945 extends QuestHandler {

    private final static int questId = 23945;
	private final static int[] Ab1Boss = {884544, 884622, 884700, 884778, 884856};
    public _23945() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(835724).addOnQuestStart(questId);
        qe.registerQuestNpc(835724).addOnTalkEvent(questId);
		qe.registerQuestNpc(836180).addOnTalkEvent(questId);
		for (int mob: Ab1Boss) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		}
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 835724) { 
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					} case ACCEPT_QUEST_SIMPLE: {
						return sendQuestStartDialog(env);
					} case REFUSE_QUEST_SIMPLE: {
				        return closeDialogWindow(env);
					}
				}
			}
		}
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 835724) {
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
	
	@Override
    public boolean onKillEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
            if (var == 1) {
                int var1 = qs.getQuestVarById(1);
                if (var1 >= 0 && var1 < 1) {
                    return defaultOnKillEvent(env, Ab1Boss, var1, var1 + 1, 1);
                } else if (var1 == 1) {
                    qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
                    return true;
                }
            }
        }
        return false;
    }
}
