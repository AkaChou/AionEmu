package com.aionemu.gameserver.quest.handlers.eternal_bastion;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 永恒堡垒任务脚本：Interrogate Kvash（任务 ID 28036）。
 * Eternal Bastion quest script: Interrogate Kvash (quest ID 28036).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _28036Interrogate_Kvash extends QuestHandler
{
    private final static int questId = 28036;
	
    public _28036Interrogate_Kvash() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerQuestNpc(801047).addOnQuestStart(questId);
        qe.registerQuestNpc(801047).addOnTalkEvent(questId);
        qe.registerQuestNpc(802015).addOnTalkEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 801047) {
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } else if (qs.getStatus() == QuestStatus.START) {
            if (targetId == 802015) {
                if (dialog == QuestDialog.START_DIALOG)
                    return sendQuestDialog(env, 1011);
                if (dialog == QuestDialog.STEP_TO_1) {
                    changeQuestStep(env, 0, 1, true);
                    updateQuestStatus(env);
                    return closeDialogWindow(env);
                }
            }
        } else if (qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 801047) {
                return sendQuestEndDialog(env);
            }
        }
        return false;
    }
}
