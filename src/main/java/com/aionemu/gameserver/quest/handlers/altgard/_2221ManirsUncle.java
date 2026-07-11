package com.aionemu.gameserver.quest.handlers.altgard;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 奥特加德任务脚本：Manirs Uncle（任务 ID 2221）。
 * Altgard quest script: Manirs Uncle (quest ID 2221).
 *
 * @author Mr. Poke
 * @reworked vlog
 */
public class _2221ManirsUncle extends QuestHandler {

	private final static int questId = 2221;
	public _2221ManirsUncle() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203607).addOnQuestStart(questId);
		qe.registerQuestNpc(203607).addOnTalkEvent(questId);
		qe.registerQuestNpc(203608).addOnTalkEvent(questId);
		qe.registerQuestNpc(700214).addOnTalkEvent(questId);
		qe.registerGetingItem(182203215, questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 203607) { // Manir
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				}
				else {
					return sendQuestStartDialog(env);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			switch (targetId) {
				case 203608: { // Groken
					if (env.getDialog() == QuestDialog.START_DIALOG) {
						if (var == 0) {
							return sendQuestDialog(env, 1352);
						}
						else if (var == 2) {
							return sendQuestDialog(env, 2375);
						}
					}
					else if (env.getDialog() == QuestDialog.STEP_TO_1) {
						return defaultCloseDialog(env, 0, 1); // 1
					}
					else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
						removeQuestItem(env, 182203215, 1);
						changeQuestStep(env, 2, 2, true); // reward
						return sendQuestDialog(env, 5);
					}
					break;
				}
				case 700214: { // Groken's Safe
					if (env.getDialog() == QuestDialog.USE_OBJECT) {
						if (var == 1) {
							return sendQuestDialog(env, 1693);
						}
					}
					else if (env.getDialog() == QuestDialog.STEP_TO_2) {
                        giveQuestItem(env, 182203215, 1);  
						return closeDialogWindow(env);
					}
				}
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203608) { // Groken
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}

	@Override
	public boolean onGetItemEvent(QuestEnv env) {
		return defaultOnGetItemEvent(env, 1, 2, false); // 2
	}
}
