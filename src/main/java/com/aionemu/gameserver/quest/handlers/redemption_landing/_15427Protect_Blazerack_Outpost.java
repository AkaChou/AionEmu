package com.aionemu.gameserver.quest.handlers.redemption_landing;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 救赎登陆点任务脚本：Protect Blazerack Outpost（任务 ID 15427）。
 * Redemption Landing quest script: Protect Blazerack Outpost (quest ID 15427).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _15427Protect_Blazerack_Outpost extends QuestHandler
{
    private final static int questId = 15427;
	
    public _15427Protect_Blazerack_Outpost() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805398).addOnQuestStart(questId);
        qe.registerQuestNpc(805398).addOnTalkEvent(questId);
		qe.registerQuestNpc(883510).addOnKillEvent(questId);
		qe.registerQuestNpc(883511).addOnKillEvent(questId);
		qe.registerQuestNpc(883512).addOnKillEvent(questId);
		qe.registerQuestNpc(883513).addOnKillEvent(questId);
		qe.registerQuestNpc(883514).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805398) {
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } else if (qs.getStatus() == QuestStatus.START) {
            if (targetId == 805398) {
                if (dialog == QuestDialog.START_DIALOG) {
                    if (qs.getQuestVarById(0) == 5) {
                        return sendQuestDialog(env, 2375);
                    }
                } if (dialog == QuestDialog.SELECT_REWARD) {
                    changeQuestStep(env, 5, 6, true);
                    return sendQuestEndDialog(env);
                }
			}
        } else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805398) {
				if (env.getDialogId() == 1352) {
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
                case 883510:
				case 883511:
				case 883512:
				case 883513:
				case 883514:
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
