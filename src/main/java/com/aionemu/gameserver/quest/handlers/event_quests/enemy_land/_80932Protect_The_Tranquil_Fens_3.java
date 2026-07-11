package com.aionemu.gameserver.quest.handlers.event_quests.enemy_land;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 活动任务脚本：Protect The Tranquil Fens 3（任务 ID 80932）。
 * Event quest script: Protect The Tranquil Fens 3 (quest ID 80932).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _80932Protect_The_Tranquil_Fens_3 extends QuestHandler
{
    private final static int questId = 80932;
	
    public _80932Protect_The_Tranquil_Fens_3() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(835070).addOnQuestStart(questId);
        qe.registerQuestNpc(835070).addOnTalkEvent(questId);
		qe.registerQuestNpc(240381).addOnKillEvent(questId);
		qe.registerQuestNpc(240382).addOnKillEvent(questId);
		qe.registerQuestNpc(240383).addOnKillEvent(questId);
		qe.registerQuestNpc(240384).addOnKillEvent(questId);
		qe.registerQuestNpc(240385).addOnKillEvent(questId);
		qe.registerQuestNpc(240386).addOnKillEvent(questId);
		qe.registerQuestNpc(240387).addOnKillEvent(questId);
		qe.registerQuestNpc(240388).addOnKillEvent(questId);
		qe.registerQuestNpc(240389).addOnKillEvent(questId);
		qe.registerQuestNpc(240390).addOnKillEvent(questId);
		qe.registerQuestNpc(241518).addOnKillEvent(questId);
		qe.registerQuestNpc(241519).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 835070) {
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } else if (qs.getStatus() == QuestStatus.START) {
            if (targetId == 835070) {
                if (dialog == QuestDialog.START_DIALOG) {
                    if (qs.getQuestVarById(0) == 27) {
                        return sendQuestDialog(env, 2375);
                    }
                } if (dialog == QuestDialog.SELECT_REWARD) {
                    changeQuestStep(env, 27, 28, true);
                    return sendQuestEndDialog(env);
                }
			}
        } else if (qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 835070) {
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
				case 240381:
				case 240382:
				case 240383:
				case 240384:
				case 240385:
				case 240386:
				case 240387:
				case 240388:
				case 240389:
				case 240390:
				case 241518:
				case 241519:
                if (qs.getQuestVarById(1) < 27) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 27) {
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
