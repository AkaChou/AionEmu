package com.aionemu.gameserver.quest.handlers.levinshor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 莱文绍尔任务脚本：Central To The Plot（任务 ID 23770）。
 * Levinshor quest script: Central To The Plot (quest ID 23770).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23770Central_To_The_Plot extends QuestHandler {

    private final static int questId = 23770;
    public _23770Central_To_The_Plot() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805294).addOnQuestStart(questId);
        qe.registerQuestNpc(805294).addOnTalkEvent(questId);
		qe.registerQuestNpc(235382).addOnKillEvent(questId);
		qe.registerQuestNpc(235383).addOnKillEvent(questId);
		qe.registerQuestNpc(235384).addOnKillEvent(questId);
		qe.registerQuestNpc(235385).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805294) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805294) {
				if (env.getDialogId() == 1009) {
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
				case 235382:
				case 235383:
				case 235384:
				case 235385:
                if (qs.getQuestVarById(1) < 12) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 12) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
