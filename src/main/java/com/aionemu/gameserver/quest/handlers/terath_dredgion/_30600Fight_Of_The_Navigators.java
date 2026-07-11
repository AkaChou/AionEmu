package com.aionemu.gameserver.quest.handlers.terath_dredgion;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 特拉斯德雷金任务脚本：Fight Of The Navigators（任务 ID 30600）。
 * Terath Dredgion quest script: Fight Of The Navigators (quest ID 30600).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _30600Fight_Of_The_Navigators extends QuestHandler {

	private final static int questId = 30600;
	public _30600Fight_Of_The_Navigators() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(205842).addOnQuestStart(questId);
		qe.registerQuestNpc(205842).addOnTalkEvent(questId); 
		qe.registerQuestNpc(800325).addOnTalkEvent(questId);
		qe.registerQuestNpc(219264).addOnKillEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestDialog dialog = env.getDialog();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 205842) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		} if (qs == null) {
			return false;
		}
		int var = qs.getQuestVarById(0);
		if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 800325) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1352);
					} case STEP_TO_1: {
						return defaultCloseDialog(env, 0, 0);
					}
				}
			} else if (targetId == 205842) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 2375);
					} case SELECT_REWARD: {
						if (var == 1) {
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							return sendQuestDialog(env, 5);
						}
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 205842) {
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
	
	@Override
    public boolean onKillEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            switch (env.getTargetId()) {
				case 219264: //Captain Anusa.
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
