package com.aionemu.gameserver.quest.handlers.ishalgen;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 伊沙尔根任务脚本：Teaching A Lesson（任务 ID 2005）。
 * Ishalgen quest script: Teaching A Lesson (quest ID 2005).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _2005Teaching_A_Lesson extends QuestHandler {

	private final static int questId = 2005;
	public _2005Teaching_A_Lesson() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerOnLevelUp(questId);
		qe.registerOnEnterZoneMissionEnd(questId);
		qe.registerQuestNpc(203540).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onZoneMissionEndEvent(QuestEnv env) {
		return defaultOnZoneMissionEndEvent(env);
	}
	
	@Override
	public boolean onLvlUpEvent(QuestEnv env) {
		return defaultOnLvlUpEvent(env, 2100, false);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null)
			return false;
		int var = qs.getQuestVarById(0);
		int targetId = env.getTargetId();
		if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 203540: {
					switch (env.getDialog()) {
						case START_DIALOG:
							if (var == 0) {
								return sendQuestDialog(env, 1011);
							} else if (var == 1) {
								return sendQuestDialog(env, 1352);
							}
						break;
						case SELECT_ACTION_1012:
							playQuestMovie(env, 54);
						return sendQuestDialog(env, 1012);
						case STEP_TO_1:
							if (var == 0) {
								qs.setQuestVarById(0, var + 1);
								updateQuestStatus(env);
								return closeDialogWindow(env);
							}
						break;
						case CHECK_COLLECTED_ITEMS:
							if (var == 1) {
								if (QuestService.collectItemCheck(env, true)) {
									qs.setStatus(QuestStatus.REWARD);
									updateQuestStatus(env);
									return sendQuestDialog(env, 5);
								} else {
									return sendQuestDialog(env, 1693);
								}
							}
						break;
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203540)
				return sendQuestEndDialog(env);
		}
		return false;
	}
}
