package com.aionemu.gameserver.quest.handlers.rentus_base;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 伦图斯基地任务脚本：Reian Last Hope（任务 ID 30514）。
 * Rentus Base quest script: Reian Last Hope (quest ID 30514).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _30514Reian_Last_Hope extends QuestHandler {

    private final static int questId = 30514;
    public _30514Reian_Last_Hope() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(799592).addOnQuestStart(questId);
        qe.registerQuestNpc(799670).addOnTalkEvent(questId);
		qe.registerQuestNpc(217310).addOnKillEvent(questId);
		qe.registerQuestNpc(217317).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 799592) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799670) {
				if (env.getDialogId() == 10002) {
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
			if (defaultOnKillEvent(env, 217310, 0, 1, 1) || defaultOnKillEvent(env, 217317, 0, 1, 2)) {
				int var1 = qs.getQuestVarById(1);
				int var2 = qs.getQuestVarById(2);
				if (var1 == 1 && var2 == 1) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
			}
        }
        return false;
    }
}
