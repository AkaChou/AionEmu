package com.aionemu.gameserver.quest.handlers.verteron;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 沃特伦任务脚本：Odella Recipe（任务 ID 1152）。
 * Verteron quest script: Odella Recipe (quest ID 1152). Quest starter: Nemia (203132). Give the Odella (182200526) (1) to Eradis (203130) and ask him to cook it. Buy some Verteron Pepper (169400112) for Eradis.
 *
 * @author vlog
 */
public class _1152OdellaRecipe extends QuestHandler {

	private final static int questId = 1152;
	public _1152OdellaRecipe() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203132).addOnQuestStart(questId);
		qe.registerQuestNpc(203132).addOnTalkEvent(questId);
		qe.registerQuestNpc(203130).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (env.getTargetId() == 203132) { // Nemia
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				}
				else {
					return sendQuestStartDialog(env, 182200526, 1);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (env.getTargetId() == 203130) { // Eradis
				switch (env.getDialog()) {
					case START_DIALOG: {
						if (var == 0) {
							return sendQuestDialog(env, 1352);
						}
						else if (var == 1) {
							return sendQuestDialog(env, 2375);
						}
					}
					case STEP_TO_1: {
						return defaultCloseDialog(env, 0, 1, 0, 0, 182200526, 1); // 1
					}
					case CHECK_COLLECTED_ITEMS: {
						return checkQuestItems(env, 1, 1, true, 5, 2716); // reward
					}
				}
			}
		}
		else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (env.getTargetId() == 203130) { // Eradis
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
