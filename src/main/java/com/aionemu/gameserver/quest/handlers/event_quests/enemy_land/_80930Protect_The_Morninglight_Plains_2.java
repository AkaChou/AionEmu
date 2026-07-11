package com.aionemu.gameserver.quest.handlers.event_quests.enemy_land;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 活动任务脚本：Protect The Morninglight Plains 2（任务 ID 80930）。
 * Event quest script: Protect The Morninglight Plains 2 (quest ID 80930).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _80930Protect_The_Morninglight_Plains_2 extends QuestHandler
{
    private final static int questId = 80930;
	
    public _80930Protect_The_Morninglight_Plains_2() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(835069).addOnQuestStart(questId);
        qe.registerQuestNpc(835069).addOnTalkEvent(questId);
		qe.registerQuestNpc(240369).addOnKillEvent(questId);
		qe.registerQuestNpc(240370).addOnKillEvent(questId);
		qe.registerQuestNpc(240371).addOnKillEvent(questId);
		qe.registerQuestNpc(240372).addOnKillEvent(questId);
		qe.registerQuestNpc(240373).addOnKillEvent(questId);
		qe.registerQuestNpc(240374).addOnKillEvent(questId);
		qe.registerQuestNpc(240375).addOnKillEvent(questId);
		qe.registerQuestNpc(240376).addOnKillEvent(questId);
		qe.registerQuestNpc(240377).addOnKillEvent(questId);
		qe.registerQuestNpc(240378).addOnKillEvent(questId);
		qe.registerQuestNpc(241177).addOnKillEvent(questId);
		qe.registerQuestNpc(241178).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 835069) {
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } else if (qs.getStatus() == QuestStatus.START) {
            if (targetId == 835069) {
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
            if (targetId == 835069) {
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
                case 240370:
				case 240371:
				case 240372:
				case 240373:
				case 240374:
				case 240375:
				case 240376:
				case 240377:
				case 240378:
				case 241177:
				case 241178:
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
