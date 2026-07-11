package com.aionemu.gameserver.quest.handlers.kaldor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 卡尔多尔任务脚本：Eliminate The Commanders Of Wealhtheow Keep（任务 ID 23941）。
 * Kaldor quest script: Eliminate The Commanders Of Wealhtheow Keep (quest ID 23941).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23941Eliminate_The_Commanders_Of_Wealhtheow_Keep extends QuestHandler
{
    private final static int questId = 23941;
	
	private final static int[] LDF5Fortress7011GBBoss = {251880, 251881, 251882, 252040, 252041, 252042};
	
    public _23941Eliminate_The_Commanders_Of_Wealhtheow_Keep() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(802433).addOnQuestStart(questId); //Pintor.
        qe.registerQuestNpc(802433).addOnTalkEvent(questId); //Pintor.
		for (int mob: LDF5Fortress7011GBBoss) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		}
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
		int targetId = env.getTargetId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
        QuestDialog dialog = env.getDialog();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 802433) { //Pintor.
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } else if (qs.getStatus() == QuestStatus.START) {
            if (targetId == 802433) { //Pintor.
                if (dialog == QuestDialog.START_DIALOG) {
                    if (qs.getQuestVarById(0) == 1) {
                        return sendQuestDialog(env, 2375);
                    }
                } if (dialog == QuestDialog.SELECT_REWARD) {
                    changeQuestStep(env, 1, 2, true);
                    return sendQuestEndDialog(env);
                }
			}
        } else if (qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 802433) { //Pintor.
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
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            switch (env.getTargetId()) {
				case 251880:
				case 251881:
				case 251882:
				case 252040:
				case 252041:
				case 252042:
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
