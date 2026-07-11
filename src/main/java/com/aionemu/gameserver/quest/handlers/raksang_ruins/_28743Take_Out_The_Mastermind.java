package com.aionemu.gameserver.quest.handlers.raksang_ruins;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 拉克桑遗迹任务脚本：Take Out The Mastermind（任务 ID 28743）。
 * Raksang Ruins quest script: Take Out The Mastermind (quest ID 28743).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _28743Take_Out_The_Mastermind extends QuestHandler {

    private final static int questId = 28743;
    public _28743Take_Out_The_Mastermind() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(206395).addOnQuestStart(questId);
		qe.registerQuestNpc(206396).addOnQuestStart(questId);
		qe.registerQuestNpc(206397).addOnQuestStart(questId);
		qe.registerQuestNpc(804732).addOnTalkEvent(questId);
		qe.registerQuestNpc(236306).addOnKillEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 206395 || targetId == 206396 || targetId == 206397) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		}
        else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804732) {
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
                case 236306: //Reviver Nasto.
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
