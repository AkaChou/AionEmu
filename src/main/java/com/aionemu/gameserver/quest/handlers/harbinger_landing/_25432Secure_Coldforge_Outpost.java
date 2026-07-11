package com.aionemu.gameserver.quest.handlers.harbinger_landing;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 先驱者登陆点任务脚本：Secure Coldforge Outpost（任务 ID 25432）。
 * Harbinger Landing quest script: Secure Coldforge Outpost (quest ID 25432).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25432Secure_Coldforge_Outpost extends QuestHandler {

    private final static int questId = 25432;
    public _25432Secure_Coldforge_Outpost() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805410).addOnQuestStart(questId);
        qe.registerQuestNpc(805410).addOnTalkEvent(questId);
		qe.registerQuestNpc(883365).addOnKillEvent(questId);
		qe.registerQuestNpc(883366).addOnKillEvent(questId);
		qe.registerQuestNpc(883367).addOnKillEvent(questId);
		qe.registerQuestNpc(883368).addOnKillEvent(questId);
		qe.registerQuestNpc(883369).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805410) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805410) {
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
                case 883365:
				case 883366:
				case 883367:
				case 883368:
				case 883369:
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
