package com.aionemu.gameserver.quest.handlers.verteron;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 沃特伦任务脚本：Stolen Village Seal（任务 ID 1156）。
 * Verteron quest script: Stolen Village Seal (quest ID 1156).
 *
 * @author Rhys2002
 * @reworked vlog
 */
public class _1156StolenVillageSeal extends QuestHandler {

	private final static int questId = 1156;
	public _1156StolenVillageSeal() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203128).addOnQuestStart(questId);
		qe.registerQuestNpc(203128).addOnTalkEvent(questId);
		qe.registerQuestNpc(700003).addOnTalkEvent(questId);
		qe.registerQuestNpc(798003).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 203128) { // Santenius
				if (dialog == QuestDialog.START_DIALOG) {
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
				case 700003: { // Item Stack
					if (dialog == QuestDialog.USE_OBJECT) {
						if (var == 0) {
							return sendQuestDialog(env, 1352);
						}
					}
					else if (dialog == QuestDialog.STEP_TO_1) {
						changeQuestStep(env, 0, 1, false); // 1
						return closeDialogWindow(env);
					}
					break;
				}
				case 798003: { // Gaphyrk
					if (dialog == QuestDialog.START_DIALOG) {
						if (var == 1) {
							return sendQuestDialog(env, 2375);
						}
					}
					else if (dialog == QuestDialog.SELECT_REWARD) {
						changeQuestStep(env, 1, 1, true); // reward
						return sendQuestDialog(env, 5);
					}
				}
			}
		}
		else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798003) { // Gaphyrk
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
