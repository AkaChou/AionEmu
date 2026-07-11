package com.aionemu.gameserver.quest.handlers.harbinger_landing;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 先驱者登陆点任务脚本：Secure Stokebellow Outpost（任务 ID 25442）。
 * Harbinger Landing quest script: Secure Stokebellow Outpost (quest ID 25442).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25442Secure_Stokebellow_Outpost extends QuestHandler {

    private final static int questId = 25442;
    public _25442Secure_Stokebellow_Outpost() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805420).addOnQuestStart(questId);
        qe.registerQuestNpc(805420).addOnTalkEvent(questId);
		qe.registerQuestNpc(883479).addOnKillEvent(questId);
		qe.registerQuestNpc(883480).addOnKillEvent(questId);
		qe.registerQuestNpc(883481).addOnKillEvent(questId);
		qe.registerQuestNpc(883482).addOnKillEvent(questId);
		qe.registerQuestNpc(883483).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805420) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805420) {
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
                case 883479:
				case 883480:
				case 883481:
				case 883482:
				case 883483:
                if (qs.getQuestVarById(1) < 5) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 5) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
