package com.aionemu.gameserver.quest.handlers.sanctum;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 圣所任务脚本：Ring Imbued Aether（任务 ID 1900）。
 * Sanctum quest script: Ring Imbued Aether (quest ID 1900).
 *
 * @author Mr. Poke, Dune11
 */
public class _1900RingImbuedAether extends QuestHandler {

	private final static int questId = 1900;
	public _1900RingImbuedAether() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203757).addOnQuestStart(questId);
		qe.registerQuestNpc(203757).addOnTalkEvent(questId);
		qe.registerQuestNpc(203739).addOnTalkEvent(questId);
		qe.registerQuestNpc(203766).addOnTalkEvent(questId);
		qe.registerQuestNpc(203797).addOnTalkEvent(questId);
		qe.registerQuestNpc(203795).addOnTalkEvent(questId);
		qe.registerQuestNpc(203830).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
		   if (env.getTargetId() == 203757) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env, 182206003, 1);
			}
		}
		if (env.getTargetId() == 203739) {
			if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 0) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1352);
				else if (env.getDialog() == QuestDialog.STEP_TO_1) {
					return defaultCloseDialog(env, 0, 1);
				}
			}
		}
		else if (env.getTargetId() == 203766) {
			if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 1) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1693);
				else if (env.getDialog() == QuestDialog.STEP_TO_2) {
					return defaultCloseDialog(env, 1, 2);
				}
			}
		}
		else if (env.getTargetId() == 203797) {
			if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 2) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 2034);
				else if (env.getDialog() == QuestDialog.STEP_TO_3) {
					return defaultCloseDialog(env, 2, 3);
				}
			}
		}
		else if (env.getTargetId() == 203795) {
			if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 3) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 2375);
				else if (env.getDialog() == QuestDialog.STEP_TO_4) {
					return defaultCloseDialog(env, 3, 4, true, false);
				}
			}
		}
		else if (env.getTargetId() == 203830) {
            if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
				removeQuestItem(env, 182206003, 1);
				return sendQuestEndDialog(env);
			}
		}
        return false;
	}
}
