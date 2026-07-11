package com.aionemu.gameserver.quest.handlers.pandaemonium;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 潘德莫尼姆任务脚本：Tales Of Heroes（任务 ID 4906）。
 * Pandaemonium quest script: Tales Of Heroes (quest ID 4906).
 *
 * @author Cheatkiller
 */
public class _4906TalesOfHeroes extends QuestHandler {

	private final static int questId = 4906;

	public _4906TalesOfHeroes() {
		super(questId);
	}

	public void register() {
		qe.registerQuestNpc(204211).addOnQuestStart(questId);
		qe.registerQuestNpc(204211).addOnTalkEvent(questId);
		qe.registerQuestNpc(205188).addOnTalkEvent(questId);
		qe.registerQuestNpc(278003).addOnTalkEvent(questId);
		qe.registerQuestNpc(204057).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204211) { 
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				}
				else {
					return sendQuestStartDialog(env, 182207075, 1);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 205188) {
				if (dialog == QuestDialog.START_DIALOG) {
					if(qs.getQuestVarById(0) == 0) {
						return sendQuestDialog(env, 1352);
					}
				}
				else if (dialog == QuestDialog.STEP_TO_1) {
					removeQuestItem(env, 182207075, 1);
					giveQuestItem(env, 182207076, 1);
					return defaultCloseDialog(env, 0, 1);
				}
			}
			if (targetId == 278003) {
				if (dialog == QuestDialog.START_DIALOG) {
					if(qs.getQuestVarById(0) == 1) {
						return sendQuestDialog(env, 1693);
					}
				}
				else if (dialog == QuestDialog.STEP_TO_2) {
					removeQuestItem(env, 182207076, 1);
					giveQuestItem(env, 182207077, 1);
					return defaultCloseDialog(env, 1, 2);
				}
			}
			if (targetId == 204057) {
				if (dialog == QuestDialog.START_DIALOG) {
					if(qs.getQuestVarById(0) == 2) {
						return sendQuestDialog(env, 2034);
					}
				}
				else if (dialog == QuestDialog.STEP_TO_3) {
					removeQuestItem(env, 182207077, 1);
					giveQuestItem(env, 182207078, 1);
					qs.setQuestVar(3);
					return defaultCloseDialog(env, 3, 3, true, false);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204211) {
				if (dialog == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 2375);
				}
				removeQuestItem(env, 182207078, 1);
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
