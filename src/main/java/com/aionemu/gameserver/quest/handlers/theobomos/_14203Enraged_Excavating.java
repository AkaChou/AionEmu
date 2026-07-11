package com.aionemu.gameserver.quest.handlers.theobomos;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 西奥博莫斯任务脚本：Enraged Excavating（任务 ID 14203）。
 * Theobomos quest script: Enraged Excavating (quest ID 14203).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _14203Enraged_Excavating extends QuestHandler {

    private final static int questId = 14203;
    public _14203Enraged_Excavating() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(798201).addOnQuestStart(questId);
        qe.registerQuestNpc(798201).addOnTalkEvent(questId);
		qe.registerQuestNpc(214337).addOnKillEvent(questId);
		qe.registerQuestNpc(214338).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
        int targetId = env.getTargetId();
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 798201) {
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798201) {
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
        final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            switch (env.getTargetId()) {
                case 214337:
				case 214338:
                if (qs.getQuestVarById(1) < 8) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 8) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
