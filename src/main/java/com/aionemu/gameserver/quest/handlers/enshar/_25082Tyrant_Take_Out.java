package com.aionemu.gameserver.quest.handlers.enshar;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

public class _25082Tyrant_Take_Out extends QuestHandler {

	private static final int questId = 25082;

	public _25082Tyrant_Take_Out() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(804923).addOnQuestStart(questId);
		qe.registerQuestNpc(804923).addOnTalkEvent(questId);
		qe.registerQuestNpc(731559).addOnTalkEvent(questId);
		qe.registerQuestNpc(804924).addOnTalkEvent(questId);
		qe.registerQuestNpc(220037).addOnKillEvent(questId);
	}

	@Override
	public boolean onKillEvent(QuestEnv env) {
		QuestState qs = env.getPlayer().getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 2 && env.getTargetId() == 220037) {
			qs.setStatus(QuestStatus.REWARD);
			updateQuestStatus(env);
			return true;
		}
		return false;
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804923) {
				return dialog == QuestDialog.START_DIALOG ? sendQuestDialog(env, 4762) : sendQuestStartDialog(env);
			}
		} else if (qs.getStatus() == QuestStatus.START && targetId == 731559) {
			int var = qs.getQuestVarById(0);
			if (dialog == QuestDialog.START_DIALOG) {
				if (var == 0 && player.getInventory().getItemCountByItemId(182215728) == 1) {
					return sendQuestDialog(env, 1011);
				}
				if (var == 1) {
					return sendQuestDialog(env, 1352);
				}
			} else if (dialog == QuestDialog.CHECK_COLLECTED_ITEMS && var == 0) {
				removeQuestItem(env, 182215728, 1);
				QuestService.addNewSpawnForSeconds(220080000, player.getInstanceId(), 833465, 1056, 194, 227, (byte) 90, 30);
				changeQuestStep(env, 0, 1, false);
				return closeDialogWindow(env);
			} else if (dialog == QuestDialog.STEP_TO_2 && var == 1) {
				QuestService.addNewSpawnForSeconds(220080000, player.getInstanceId(), 220037, 1050.5f, 200.4f, 228.2f,
						(byte) 90, 300);
				changeQuestStep(env, 1, 2, false);
				return closeDialogWindow(env);
			}
		} else if (qs.getStatus() == QuestStatus.REWARD && targetId == 804924) {
			return dialog == QuestDialog.START_DIALOG ? sendQuestDialog(env, 2034) : sendQuestEndDialog(env);
		}
		return false;
	}
}
