package com.aionemu.gameserver.quest.handlers.event_quests.enemy_land;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 活动任务脚本：Protect The Illumined Pergolas 2（任务 ID 80927）。
 * Event quest script: Protect The Illumined Pergolas 2 (quest ID 80927).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _80927Protect_The_Illumined_Pergolas_2 extends QuestHandler
{
    private final static int questId = 80927;
	
    public _80927Protect_The_Illumined_Pergolas_2() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(835067).addOnQuestStart(questId);
        qe.registerQuestNpc(835067).addOnTalkEvent(questId);
		qe.registerQuestNpc(241656).addOnKillEvent(questId);
		qe.registerQuestNpc(241657).addOnKillEvent(questId);
		qe.registerQuestNpc(241658).addOnKillEvent(questId);
		qe.registerQuestNpc(241659).addOnKillEvent(questId);
		qe.registerQuestNpc(241660).addOnKillEvent(questId);
		qe.registerQuestNpc(241661).addOnKillEvent(questId);
		qe.registerQuestNpc(241662).addOnKillEvent(questId);
		qe.registerQuestNpc(241663).addOnKillEvent(questId);
		qe.registerQuestNpc(241664).addOnKillEvent(questId);
		qe.registerQuestNpc(241665).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 835067) {
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } else if (qs.getStatus() == QuestStatus.START) {
            if (targetId == 835067) {
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
            if (targetId == 835067) {
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
                case 241656:
				case 241657:
				case 241658:
				case 241659:
				case 241660:
				case 241661:
				case 241662:
				case 241663:
				case 241664:
				case 241665:
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
