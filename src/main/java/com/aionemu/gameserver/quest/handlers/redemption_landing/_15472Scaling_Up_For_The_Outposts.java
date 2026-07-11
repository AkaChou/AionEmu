package com.aionemu.gameserver.quest.handlers.redemption_landing;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 救赎登陆点任务脚本：Scaling Up For The Outposts（任务 ID 15472）。
 * Redemption Landing quest script: Scaling Up For The Outposts (quest ID 15472).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _15472Scaling_Up_For_The_Outposts extends QuestHandler
{
    private final static int questId = 15472;
	
    public _15472Scaling_Up_For_The_Outposts() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805798).addOnQuestStart(questId);
        qe.registerQuestNpc(805798).addOnTalkEvent(questId);
		qe.registerQuestNpc(883094).addOnKillEvent(questId);
		qe.registerQuestNpc(883118).addOnKillEvent(questId);
		qe.registerQuestNpc(883142).addOnKillEvent(questId);
		qe.registerQuestNpc(882986).addOnKillEvent(questId);
		qe.registerQuestNpc(882998).addOnKillEvent(questId);
		qe.registerQuestNpc(883010).addOnKillEvent(questId);
		qe.registerQuestNpc(883096).addOnKillEvent(questId);
		qe.registerQuestNpc(883120).addOnKillEvent(questId);
		qe.registerQuestNpc(883144).addOnKillEvent(questId);
		qe.registerQuestNpc(882988).addOnKillEvent(questId);
		qe.registerQuestNpc(883000).addOnKillEvent(questId);
		qe.registerQuestNpc(883012).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805798) {
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } else if (qs.getStatus() == QuestStatus.START) {
            if (targetId == 805798) {
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
			if (targetId == 805798) {
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
				case 883094:
				case 883118:
				case 883142:
				case 882986:
				case 882998:
				case 883010:
				case 883096:
				case 883120:
				case 883144:
				case 882988:
				case 883000:
				case 883012:
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
