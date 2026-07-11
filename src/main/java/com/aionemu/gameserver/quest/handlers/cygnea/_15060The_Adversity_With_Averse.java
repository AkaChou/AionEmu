package com.aionemu.gameserver.quest.handlers.cygnea;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 希格尼娅任务脚本：The Adversity With Averse（任务 ID 15060）。
 * Cygnea quest script: The Adversity With Averse (quest ID 15060).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _15060The_Adversity_With_Averse extends QuestHandler {

    private final static int questId = 15060;
	private final static int[] LF5FDrakan60An = {235876, 235877};
    public _15060The_Adversity_With_Averse() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(804707).addOnQuestStart(questId);
        qe.registerQuestNpc(804707).addOnTalkEvent(questId);
		for (int mob: LF5FDrakan60An) {
            qe.registerQuestNpc(mob).addOnKillEvent(questId);
        }
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 804707) {
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 804707) {
                if (dialog == QuestDialog.USE_OBJECT) {
                    return sendQuestDialog(env, 10002);
                }
                return sendQuestEndDialog(env);
            }
        }
        return false;
    }
	
    @Override
    public boolean onKillEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
            if (var == 0) {
                int var1 = qs.getQuestVarById(1);
                if (var1 >= 0 && var1 < 7) {
                    return defaultOnKillEvent(env, LF5FDrakan60An, var1, var1 + 1, 1);
                } else if (var1 == 7) {
                    qs.setQuestVar(1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
                    return true;
                }
            }
        }
        return false;
    }
}
