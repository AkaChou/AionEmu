package com.aionemu.gameserver.quest.handlers.eternal_bastion;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 永恒堡垒任务脚本：Bastions Are Eternal Positions Are Not（任务 ID 18036）。
 * Eternal Bastion quest script: Bastions Are Eternal Positions Are Not (quest ID 18036).
 */
public class _18036Bastions_Are_Eternal_Positions_Are_Not extends QuestHandler
{
    private final static int questId = 18036;

    public _18036Bastions_Are_Eternal_Positions_Are_Not() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerQuestNpc(801037).addOnQuestStart(questId);
        qe.registerQuestNpc(801037).addOnTalkEvent(questId);
        qe.registerQuestNpc(802008).addOnTalkEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 801037) {
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } else if (qs.getStatus() == QuestStatus.START) {
            if (targetId == 802008) {
                if (dialog == QuestDialog.START_DIALOG)
                      return sendQuestDialog(env, 1011);
                if (dialog == QuestDialog.STEP_TO_1) {
                    changeQuestStep(env, 0, 1, true);
                    updateQuestStatus(env);
                    return closeDialogWindow(env);
                }
            }
        } else if (qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 801037) {
                return sendQuestEndDialog(env);
            }
        }
        return false;
    }
}
