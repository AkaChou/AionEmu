package com.aionemu.gameserver.quest.handlers.arena;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 竞技场任务脚本：The Coliseum Secret（任务 ID 28213）。
 * Arena quest script: The Coliseum Secret (quest ID 28213).
 *
 * @author (Encom)
 */
public class _28213The_Coliseum_Secret extends QuestHandler {

	private final static int questId = 28213;
	public _28213The_Coliseum_Secret() {
		super(questId);
	}
	
	@Override
    public void register() {
        qe.registerQuestNpc(205986).addOnQuestStart(questId);
        qe.registerQuestNpc(205320).addOnTalkEvent(questId);
        qe.registerQuestNpc(798604).addOnTalkEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 205986) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 1011);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } else if (qs == null || qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
            switch (targetId) {
                case 205320:
                    if (var == 0) {
                        switch (env.getDialog()) {
                            case START_DIALOG:
                                return sendQuestDialog(env, 1352);
                            case STEP_TO_1:
                                return defaultCloseDialog(env, 0, 1);
                        }
                    }
                    break;
                case 798804:
                    if (var == 1) {
                        switch (env.getDialog()) {
                            case START_DIALOG:
                                return sendQuestDialog(env, 1693);
                            case STEP_TO_2:
                                return defaultCloseDialog(env, 1, 3);
                        }
                    } else if (var == 3) {
                        switch (env.getDialog()) {
                            case START_DIALOG:
                                return sendQuestDialog(env, 2375);
                            case SELECT_REWARD:
                                return defaultCloseDialog(env, 3, 3, true, false);
                        }
                    }
                    break;
            }
        } else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 798804) {
                if (env.getDialog() == QuestDialog.USE_OBJECT) {
                    return sendQuestDialog(env, 5);
                } else {
                    return sendQuestEndDialog(env);
                }
            }
        }
        return false;
    }
}
