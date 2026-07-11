package com.aionemu.gameserver.quest.handlers.verteron;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.*;
import com.aionemu.gameserver.questEngine.model.*;
import com.aionemu.gameserver.services.*;

/**
 * 沃特伦任务脚本：Headless Stone Statue（任务 ID 1170）。
 * Verteron quest script: Headless Stone Statue (quest ID 1170).
 *
 * @author Rinzler (Encom)
 */
public class _1170Headless_Stone_Statue extends QuestHandler {

	private final static int questId = 1170;
	public _1170Headless_Stone_Statue() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(730000).addOnQuestStart(questId);
		qe.registerQuestNpc(730000).addOnTalkEvent(questId);
		qe.registerQuestNpc(700033).addOnTalkEvent(questId);
		qe.registerOnMovieEndQuest(16, questId);
		qe.registerGetingItem(182200504, questId);
	}
	
	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 730000) { //Headless Stone Statue.
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 700033) { //Head Of Stone Statue.
				if (dialog == QuestDialog.USE_OBJECT) {
					return giveQuestItem(env, 182200504, 1);
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 730000) { //Headless Stone Statue.
				if (dialog == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 1352);
				} else if (dialog == QuestDialog.STEP_TO_1) {
					playQuestMovie(env, 16);
					return closeDialogWindow(env);
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean onGetItemEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			changeQuestStep(env, 0, 0, true);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onMovieEndEvent(QuestEnv env, int movieId) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (movieId == 16) {
				removeQuestItem(env, 182200504, 1);
				return QuestService.finishQuest(env);
			}
		}
		return false;
	}
}
