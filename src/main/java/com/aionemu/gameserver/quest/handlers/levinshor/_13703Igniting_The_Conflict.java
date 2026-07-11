package com.aionemu.gameserver.quest.handlers.levinshor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 莱文绍尔任务脚本：Igniting The Conflict（任务 ID 13703）。
 * Levinshor quest script: Igniting The Conflict (quest ID 13703).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _13703Igniting_The_Conflict extends QuestHandler {

    private final static int questId = 13703;
    public _13703Igniting_The_Conflict() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(802350).addOnQuestStart(questId);
        qe.registerQuestNpc(802350).addOnTalkEvent(questId);
		qe.registerQuestNpc(234162).addOnKillEvent(questId);
		qe.registerQuestNpc(234645).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 802350) {
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 1011);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } else if (qs.getStatus() == QuestStatus.START) {
            if (targetId == 802350) {
                if (dialog == QuestDialog.START_DIALOG) {
                    if (qs.getQuestVarById(0) == 1) {
                        return sendQuestDialog(env, 2375);
                    }
                } if (dialog == QuestDialog.SELECT_REWARD) {
                    changeQuestStep(env, 1, 2, true);
                    return sendQuestEndDialog(env);
                }
			}
        } else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 802350) {
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
                case 234162:
                case 234645:
                if (qs.getQuestVarById(0) < 1) {
                    qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					qs.setQuestVarById(0, 1);
                    updateQuestStatus(env);
                    return true;
                }
            }
        }
        return false;
    }
}
