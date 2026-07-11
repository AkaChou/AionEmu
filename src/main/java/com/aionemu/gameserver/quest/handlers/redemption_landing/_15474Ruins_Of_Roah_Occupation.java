package com.aionemu.gameserver.quest.handlers.redemption_landing;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 救赎登陆点任务脚本：Ruins Of Roah Occupation（任务 ID 15474）。
 * Redemption Landing quest script: Ruins Of Roah Occupation (quest ID 15474).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _15474Ruins_Of_Roah_Occupation extends QuestHandler
{
    private final static int questId = 15474;
	
    public _15474Ruins_Of_Roah_Occupation() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805798).addOnQuestStart(questId);
        qe.registerQuestNpc(805798).addOnTalkEvent(questId);
		qe.registerQuestNpc(883154).addOnKillEvent(questId);
		qe.registerQuestNpc(883166).addOnKillEvent(questId);
		qe.registerQuestNpc(883178).addOnKillEvent(questId);
		qe.registerQuestNpc(883016).addOnKillEvent(questId);
		qe.registerQuestNpc(883022).addOnKillEvent(questId);
		qe.registerQuestNpc(883028).addOnKillEvent(questId);
		qe.registerQuestNpc(883156).addOnKillEvent(questId);
		qe.registerQuestNpc(883168).addOnKillEvent(questId);
		qe.registerQuestNpc(883180).addOnKillEvent(questId);
		qe.registerQuestNpc(883018).addOnKillEvent(questId);
		qe.registerQuestNpc(883024).addOnKillEvent(questId);
		qe.registerQuestNpc(883030).addOnKillEvent(questId);
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
				case 883154:
				case 883166:
				case 883178:
				case 883016:
				case 883022:
				case 883028:
				case 883156:
				case 883168:
				case 883180:
				case 883018:
				case 883024:
				case 883030:
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
