package com.aionemu.gameserver.quest.handlers.enshar;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 恩沙尔任务脚本：Treasure Of Ancient King（任务 ID 25051）。
 * Enshar quest script: Treasure Of Ancient King (quest ID 25051).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25051Treasure_Of_Ancient_King extends QuestHandler {

    private final static int questId = 25051;
    public _25051Treasure_Of_Ancient_King() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerQuestNpc(804915).addOnQuestStart(questId);
        qe.registerQuestNpc(804915).addOnTalkEvent(questId);
        qe.registerQuestNpc(731553).addOnTalkEvent(questId);
        qe.registerQuestNpc(805160).addOnTalkEvent(questId);
		qe.registerQuestNpc(731554).addOnTalkEvent(questId);
		qe.registerQuestNpc(731555).addOnTalkEvent(questId);
		qe.registerQuestNpc(804916).addOnTalkEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        int targetId = env.getTargetId();
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 804915) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (targetId == 731553) {
                if (env.getDialog() == QuestDialog.USE_OBJECT) {
                    if (var == 0) {
                        return sendQuestDialog(env, 1011);
                    }
                } else if (env.getDialog() == QuestDialog.STEP_TO_1) {
					QuestService.addNewSpawnForSeconds(220080000, player.getInstanceId(), 805160, 2046, 1588, 348, (byte) 90, 300);
					changeQuestStep(env, 0, 1, false);
                    return closeDialogWindow(env);
                }
            } if (targetId == 805160) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    if (var == 1) {
                        return sendQuestDialog(env, 1352);
                    }
                } else if (env.getDialog() == QuestDialog.STEP_TO_2) {
					Npc npc = (Npc) env.getVisibleObject();
                    npc.getController().onDelete();
					changeQuestStep(env, 1, 2, false);
                    return closeDialogWindow(env);
                }
            } if (targetId == 731554) {
                if (env.getDialog() == QuestDialog.USE_OBJECT) {
                    if (var == 2) {
                        return sendQuestDialog(env, 1693);
                    }
                } else if (env.getDialog() == QuestDialog.STEP_TO_3) {
					giveQuestItem(env, 182215720, 1);
					changeQuestStep(env, 2, 3, false);
                    return closeDialogWindow(env);
                }
            } if (targetId == 731555) {
                if (env.getDialog() == QuestDialog.USE_OBJECT) {
                    if (var == 3) {
                        return sendQuestDialog(env, 2034);
                    }
                } else if (dialog == QuestDialog.SET_REWARD) {
					removeQuestItem(env, 182215720, 1);
					QuestService.addNewSpawnForSeconds(220080000, player.getInstanceId(), 220031, player.getX(), player.getY(),
							player.getZ(), player.getHeading(), 300);
					qs.setStatus(QuestStatus.REWARD);
					changeQuestStep(env, 3, 4, false);
                    return closeDialogWindow(env);
                }
            }
        } else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 804916) {
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 2376);
                } else {
                    return sendQuestEndDialog(env);
                }
            }
        }
        return false;
    }
}
