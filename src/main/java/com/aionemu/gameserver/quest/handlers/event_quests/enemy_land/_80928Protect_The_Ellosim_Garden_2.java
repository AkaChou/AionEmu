package com.aionemu.gameserver.quest.handlers.event_quests.enemy_land;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 活动任务脚本：Protect The Ellosim Garden 2（任务 ID 80928）。
 * Event quest script: Protect The Ellosim Garden 2 (quest ID 80928).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _80928Protect_The_Ellosim_Garden_2 extends QuestHandler
{
    private final static int questId = 80928;
	
    public _80928Protect_The_Ellosim_Garden_2() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(835067).addOnQuestStart(questId);
        qe.registerQuestNpc(835067).addOnTalkEvent(questId);
		qe.registerQuestNpc(241668).addOnKillEvent(questId);
		qe.registerQuestNpc(241669).addOnKillEvent(questId);
		qe.registerQuestNpc(241670).addOnKillEvent(questId);
		qe.registerQuestNpc(241671).addOnKillEvent(questId);
		qe.registerQuestNpc(241672).addOnKillEvent(questId);
		qe.registerQuestNpc(241673).addOnKillEvent(questId);
		qe.registerQuestNpc(241674).addOnKillEvent(questId);
		qe.registerQuestNpc(241675).addOnKillEvent(questId);
		qe.registerQuestNpc(241676).addOnKillEvent(questId);
		qe.registerQuestNpc(241677).addOnKillEvent(questId);
		qe.registerQuestNpc(241678).addOnKillEvent(questId);
		qe.registerQuestNpc(241679).addOnKillEvent(questId);
		qe.registerQuestNpc(243285).addOnKillEvent(questId);
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
                case 241668:
				case 241669:
				case 241670:
				case 241671:
				case 241672:
				case 241673:
				case 241674:
				case 241675:
				case 241676:
				case 241677:
				case 241678:
				case 241679:
				case 243285:
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
