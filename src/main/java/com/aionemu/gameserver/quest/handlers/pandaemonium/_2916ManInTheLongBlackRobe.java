package com.aionemu.gameserver.quest.handlers.pandaemonium;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 潘德莫尼姆任务脚本：Man In The Long Black Robe（任务 ID 2916）。
 * Pandaemonium quest script: Man In The Long Black Robe (quest ID 2916).
 *
 * @author Cheatkiller
 */
public class _2916ManInTheLongBlackRobe extends QuestHandler {

	private final static int questId = 2916;

	public _2916ManInTheLongBlackRobe() {
		super(questId);
	}

	public void register() {
		qe.registerQuestNpc(204141).addOnQuestStart(questId);
		qe.registerQuestNpc(204141).addOnTalkEvent(questId);
		qe.registerQuestNpc(204152).addOnTalkEvent(questId);
		qe.registerQuestNpc(204150).addOnTalkEvent(questId);
		qe.registerQuestNpc(798033).addOnTalkEvent(questId);
		qe.registerQuestNpc(203673).addOnTalkEvent(questId);
		qe.registerQuestNpc(700211).addOnTalkEvent(questId);
		qe.registerQuestNpc(700211).addOnAtDistanceEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204141) { 
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				}
				else {
					return sendQuestStartDialog(env);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 204152) {
				if (dialog == QuestDialog.START_DIALOG) {
					if(qs.getQuestVarById(0) == 0)
						return sendQuestDialog(env, 1352);
				}
				else if (dialog == QuestDialog.STEP_TO_1) {
					return defaultCloseDialog(env, 0, 1);
				}
			}
			else if (targetId == 204150) {
				if (dialog == QuestDialog.START_DIALOG) {
					if(qs.getQuestVarById(0) == 1)
						return sendQuestDialog(env, 1693);
				}
				else if (dialog == QuestDialog.STEP_TO_2) {
					return defaultCloseDialog(env, 1, 2);
				}
			}
			else if (targetId == 204151) {
				if (dialog == QuestDialog.START_DIALOG) {
					if(qs.getQuestVarById(0) == 2)
						return sendQuestDialog(env, 2034);
				}
				else if (dialog == QuestDialog.STEP_TO_3) {
					return defaultCloseDialog(env, 2, 3);
				}
			}
			else if (targetId == 798033) {
				if (dialog == QuestDialog.START_DIALOG) {
					if(qs.getQuestVarById(0) == 3)
						return sendQuestDialog(env, 2375);
				}
				else if (dialog == QuestDialog.STEP_TO_4) {
					return defaultCloseDialog(env, 3, 4);
				}
			}
			else if (targetId == 203673) {
				if (dialog == QuestDialog.START_DIALOG) {
					if(qs.getQuestVarById(0) == 4)
						return sendQuestDialog(env, 2716);
				}
				else if (dialog == QuestDialog.STEP_TO_5) {
					return defaultCloseDialog(env, 4, 5);
				}
			}
			else if (targetId == 700211) {
				if(qs.getQuestVarById(0) == 6)
					return true;
			}
			else if (targetId == 204141) {
				if (dialog == QuestDialog.START_DIALOG) {
					if(qs.getQuestVarById(0) == 6)
						return sendQuestDialog(env, 3057);
				}
				else if (dialog == QuestDialog.CHECK_COLLECTED_ITEMS) {
					return checkQuestItems(env, 6, 6, true, 5, 3143);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204141) {
				if (dialog == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 5);
				}
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
	
	@Override
	public boolean onAtDistanceEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
  		int var = qs.getQuestVarById(0);
  		if (var == 5) {
  			changeQuestStep(env, 5, 6, false);
  			return true;
  		}
  	}
		return false;
	}
}
