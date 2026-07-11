package com.aionemu.gameserver.quest.handlers.heiron;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 希隆任务脚本：Enemy Harassing The Elim（任务 ID 3547）。
 * Heiron quest script: Enemy Harassing The Elim (quest ID 3547).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _3547Enemy_Harassing_The_Elim extends QuestHandler {

    private final static int questId = 3547;
    public _3547Enemy_Harassing_The_Elim() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerQuestNpc(730024).addOnQuestStart(questId);
        qe.registerQuestNpc(730024).addOnTalkEvent(questId);
        qe.registerQuestNpc(204647).addOnTalkEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        int targetId = env.getTargetId();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 730024) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 1011);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } if (qs == null) {
            return false;
        } if (qs != null && qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
            if (targetId == 204647) {
                switch (env.getDialog()) {
                    case START_DIALOG:
                        if (var == 0) {
                            return sendQuestDialog(env, 1352);
                        } else if (var == 1) {
                            return sendQuestDialog(env, 2375);
                        }
                    case STEP_TO_1: {
                        return defaultCloseDialog(env, 0, 1);
                    } case CHECK_COLLECTED_ITEMS_SIMPLE: {
                        if (QuestService.collectItemCheck(env, true)) {
                            changeQuestStep(env, 1, 1, true);
                            return sendQuestDialog(env, 5);
                        } else {
                            return closeDialogWindow(env);
                        }
                    }
                }
            }
        } else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204647) {
				return sendQuestEndDialog(env);
			}
		}
        return false;
    }
}
