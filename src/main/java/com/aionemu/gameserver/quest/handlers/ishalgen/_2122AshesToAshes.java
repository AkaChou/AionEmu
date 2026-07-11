package com.aionemu.gameserver.quest.handlers.ishalgen;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 伊沙尔根任务脚本：Ashes To Ashes（任务 ID 2122）。
 * Ishalgen quest script: Ashes To Ashes (quest ID 2122).
 *
 * @author Cheatkiller
 * @reworked GiGatR00n
 */
public class _2122AshesToAshes extends QuestHandler {

	private final static int questId = 2122;
	private int[] npcs = { 203551, 700148, 730029};
	public _2122AshesToAshes() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestItem(182203120, questId);
		for (int npc : npcs) {
			qe.registerQuestNpc(npc).addOnTalkEvent(questId);
		}
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 0) {
				if (env.getDialog() == QuestDialog.ACCEPT_QUEST) {
					return sendQuestStartDialog(env);
				}
				else if (env.getDialog() == QuestDialog.REFUSE_QUEST) {
					return closeDialogWindow(env);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 203551) {
			int var = qs.getQuestVarById(0);
				switch (env.getDialog()) {
					case START_DIALOG: {
						if (var == 0) {
						return sendQuestDialog(env, 1011);
						}
					}
					case SELECT_ACTION_1012: {
						if (var == 0) {
						removeQuestItem(env, 182203120, 1);
						return sendQuestDialog(env, 1012);
						}
					}
					case STEP_TO_1: {
						return defaultCloseDialog(env, 0, 1);
					}
					default:
						break;
				}
			}
			else if (targetId == 730029) {
				switch (env.getDialog()) {
					case USE_OBJECT: {
						if (player.getInventory().getItemCountByItemId(182203133) == 1) {
							return sendQuestDialog(env, 1352);
						}
						else {
							return sendQuestDialog(env, 1693);
						}
					}
					case SELECT_ACTION_1353: {
						return sendQuestDialog(env, 1353);
					}
					case STEP_TO_2: {
						removeQuestItem(env, 182203133, 1);
						return defaultCloseDialog(env, 1, 1, true, false);
					}
					default:
						break;
				}
			}
			else if (targetId == 700148) {
				return true; // just give quest drop on use
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203551) {
				if (env.getDialog() == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 2375);
				}
				else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}

	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			return HandlerResult.fromBoolean(sendQuestDialog(env, 4));
		}
		return HandlerResult.FAILED;
	}
}
