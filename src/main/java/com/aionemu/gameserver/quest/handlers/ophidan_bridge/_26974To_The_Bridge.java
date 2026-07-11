package com.aionemu.gameserver.quest.handlers.ophidan_bridge;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 奥菲丹桥任务脚本：To The Bridge（任务 ID 26974）。
 * Ophidan Bridge quest script: To The Bridge (quest ID 26974).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _26974To_The_Bridge extends QuestHandler {

    private final static int questId = 26974;
    public _26974To_The_Bridge() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(801765).addOnQuestStart(questId);
        qe.registerQuestNpc(801765).addOnTalkEvent(questId);
		qe.registerQuestNpc(235768).addOnKillEvent(questId);
		qe.registerQuestNpc(235769).addOnKillEvent(questId);
		qe.registerQuestNpc(235770).addOnKillEvent(questId);
		qe.registerQuestNpc(235771).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 801765) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } 
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 801765) {
				return sendQuestEndDialog(env);
			}
		}
        return false;
    }
	
	public boolean onKillEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            switch (env.getTargetId()) {
                case 235768:
				case 235769:
				case 235770:
				case 235771:
                if (qs.getQuestVarById(0) < 1) {
                    qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
                    qs.setStatus(QuestStatus.REWARD);
                    updateQuestStatus(env);
                    return true;
                }
            }
        }
        return false;
    }
}
