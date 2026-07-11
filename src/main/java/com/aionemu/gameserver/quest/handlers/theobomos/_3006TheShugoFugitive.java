package com.aionemu.gameserver.quest.handlers.theobomos;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 西奥博莫斯任务脚本：The Shugo Fugitive（任务 ID 3006）。
 * Theobomos quest script: The Shugo Fugitive (quest ID 3006).
 *
 * @author Balthazar
 */
public class _3006TheShugoFugitive extends QuestHandler {

	private final static int questId = 3006;
	public _3006TheShugoFugitive() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(798132).addOnQuestStart(questId);
		qe.registerQuestNpc(798132).addOnTalkEvent(questId);
		qe.registerQuestNpc(798146).addOnTalkEvent(questId);
		qe.registerQuestNpc(700339).addOnTalkEvent(questId);
		qe.registerOnMovieEndQuest(361, questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 798132) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					default:
						return sendQuestStartDialog(env);
				}
			}
		}
		if (qs == null)
			return false;
		if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 798146: {
					switch (env.getDialog()) {
						case START_DIALOG: {
							if (qs.getQuestVarById(0) == 0) {
								return sendQuestDialog(env, 1352);
							}
						}
						case STEP_TO_1: {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
				            return closeDialogWindow(env);
						}
					}
				}
				case 700339: {
					switch (env.getDialog()) {
						case USE_OBJECT: {
							if (qs.getQuestVarById(0) == 1) {
								return sendQuestDialog(env, 1693);
							}
						}
						case STEP_TO_2: {
							if (qs.getQuestVarById(0) == 1) {
								playQuestMovie(env, 361);
								return closeDialogWindow(env);
							}
						}
					}
				}
		    case 798132: {
				switch (env.getDialog()) {
					case START_DIALOG: {
						if (qs.getQuestVarById(0) == 2) {
							return sendQuestDialog(env, 2375);
						}
					}
					case SELECT_REWARD: {
						if (qs.getQuestVarById(0) == 2) {
                            qs.setQuestVarById(0, qs.getQuestVarById(0) + 1); 
                            qs.setStatus(QuestStatus.REWARD);
                            updateQuestStatus(env);
						return sendQuestEndDialog(env);
						}
					}
				}
			}
		}
	} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798132) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}

	@Override
	public boolean onMovieEndEvent(QuestEnv env, int movieId) {
		if (movieId != 361)
			return false;
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() != QuestStatus.START)
			return false;
		qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
		updateQuestStatus(env);
		return true;
	}
}
