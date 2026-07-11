package com.aionemu.gameserver.quest.handlers.harbinger_landing;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 先驱者登陆点任务脚本：Secure Moltenspike Outpost（任务 ID 25445）。
 * Harbinger Landing quest script: Secure Moltenspike Outpost (quest ID 25445).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25445Secure_Moltenspike_Outpost extends QuestHandler {

    private final static int questId = 25445;
	
    public _25445Secure_Moltenspike_Outpost() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805423).addOnQuestStart(questId);
        qe.registerQuestNpc(805423).addOnTalkEvent(questId);
		qe.registerQuestNpc(883536).addOnKillEvent(questId);
		qe.registerQuestNpc(883537).addOnKillEvent(questId);
		qe.registerQuestNpc(883538).addOnKillEvent(questId);
		qe.registerQuestNpc(883539).addOnKillEvent(questId);
		qe.registerQuestNpc(883540).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805423) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805423) {
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
                case 883536:
				case 883537:
				case 883538:
				case 883539:
				case 883540:
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
