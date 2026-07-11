package com.aionemu.gameserver.quest.handlers.harbinger_landing;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 先驱者登陆点任务脚本：Handle The Intruder To Protect Asteria（任务 ID 25413）。
 * Harbinger Landing quest script: Handle The Intruder To Protect Asteria (quest ID 25413).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25413Handle_The_Intruder_To_Protect_Asteria extends QuestHandler {

    private final static int questId = 25413;
    public _25413Handle_The_Intruder_To_Protect_Asteria() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805402).addOnQuestStart(questId);
        qe.registerQuestNpc(805402).addOnTalkEvent(questId);
		qe.registerQuestNpc(884021).addOnKillEvent(questId);
		qe.registerQuestNpc(884022).addOnKillEvent(questId);
		qe.registerQuestNpc(884023).addOnKillEvent(questId);
		qe.registerQuestNpc(884024).addOnKillEvent(questId);
		qe.registerQuestNpc(884025).addOnKillEvent(questId);
		qe.registerQuestNpc(884026).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 805402) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805402) {
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
                case 884021:
				case 884022:
				case 884023:
				case 884024:
				case 884025:
				case 884026:
                if (qs.getQuestVarById(1) < 2) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 2) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
