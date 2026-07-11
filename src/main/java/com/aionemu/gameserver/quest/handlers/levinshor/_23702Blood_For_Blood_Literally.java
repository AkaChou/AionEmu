package com.aionemu.gameserver.quest.handlers.levinshor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 莱文绍尔任务脚本：Blood For Blood Literally（任务 ID 23702）。
 * Levinshor quest script: Blood For Blood Literally (quest ID 23702).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23702Blood_For_Blood_Literally extends QuestHandler {

    private final static int questId = 23702;
    public _23702Blood_For_Blood_Literally() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(802353).addOnQuestStart(questId);
        qe.registerQuestNpc(802353).addOnTalkEvent(questId);
		qe.registerQuestNpc(802354).addOnTalkEvent(questId);
		qe.registerQuestNpc(235366).addOnKillEvent(questId);
		qe.registerQuestNpc(235367).addOnKillEvent(questId);
		qe.registerQuestNpc(235368).addOnKillEvent(questId);
		qe.registerQuestNpc(235369).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 802353) {
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 1011);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } else if (qs.getStatus() == QuestStatus.START) {
            if (targetId == 802354) {
                if (dialog == QuestDialog.START_DIALOG) {
                    if (qs.getQuestVarById(0) == 4) {
                        return sendQuestDialog(env, 2375);
                    }
                } if (dialog == QuestDialog.SELECT_REWARD) {
                    changeQuestStep(env, 4, 5, true);
                    return sendQuestEndDialog(env);
                }
			}
        } else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 802354) {
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
                case 235366:
                case 235367:
				case 235368:
				case 235369:
                if (qs.getQuestVarById(0) < 4) {
                    qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
                    updateQuestStatus(env);
                    return true;
                }
            }
        }
        return false;
    }
}
