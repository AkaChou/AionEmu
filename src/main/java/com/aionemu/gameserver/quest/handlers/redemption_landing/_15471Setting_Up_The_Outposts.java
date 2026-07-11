package com.aionemu.gameserver.quest.handlers.redemption_landing;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 救赎登陆点任务脚本：Setting Up The Outposts（任务 ID 15471）。
 * Redemption Landing quest script: Setting Up The Outposts (quest ID 15471).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _15471Setting_Up_The_Outposts extends QuestHandler {

    private final static int questId = 15471;
    public _15471Setting_Up_The_Outposts() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805798).addOnQuestStart(questId);
        qe.registerQuestNpc(805798).addOnTalkEvent(questId);
		qe.registerQuestNpc(883082).addOnKillEvent(questId);
		qe.registerQuestNpc(883106).addOnKillEvent(questId);
		qe.registerQuestNpc(883130).addOnKillEvent(questId);
		qe.registerQuestNpc(882980).addOnKillEvent(questId);
		qe.registerQuestNpc(882992).addOnKillEvent(questId);
		qe.registerQuestNpc(883004).addOnKillEvent(questId);
		qe.registerQuestNpc(883084).addOnKillEvent(questId);
		qe.registerQuestNpc(883108).addOnKillEvent(questId);
		qe.registerQuestNpc(883132).addOnKillEvent(questId);
		qe.registerQuestNpc(882982).addOnKillEvent(questId);
		qe.registerQuestNpc(882994).addOnKillEvent(questId);
		qe.registerQuestNpc(883006).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805798) {
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805798) {
				if (env.getDialogId() == 2375) {
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
				case 883082:
				case 883106:
				case 883130:
				case 882980:
				case 882992:
				case 883004:
				case 883084:
				case 883108:
				case 883132:
				case 882982:
				case 882994:
				case 883006:
                if (qs.getQuestVarById(1) < 1) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 1) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
