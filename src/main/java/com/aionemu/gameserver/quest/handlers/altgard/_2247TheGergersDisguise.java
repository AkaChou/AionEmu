package com.aionemu.gameserver.quest.handlers.altgard;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 奥特加德任务脚本：The Gergers Disguise（任务 ID 2247）。
 * Altgard quest script: The Gergers Disguise (quest ID 2247).
 *
 * @author Ritsu
 */
public class _2247TheGergersDisguise extends QuestHandler {

	private final static int questId = 2247;
	public _2247TheGergersDisguise() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203645).addOnQuestStart(questId);
		qe.registerQuestNpc(203645).addOnTalkEvent(questId);
		qe.registerQuestNpc(798039).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE){
			if (env.getTargetId() == 203645) {
				if(env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				}
				else {
					return sendQuestStartDialog(env);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (env.getTargetId() == 798039) {
				switch (env.getDialog()) {
				case START_DIALOG: {
				     return sendQuestDialog(env, 1352);
                }
				case STEP_TO_1:
					if (var == 0){
						qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
						updateQuestStatus(env);
						giveQuestItem(env, 182203231, 1);
					    return closeDialogWindow(env);
					}
				}
			}
			if (env.getTargetId() == 203645) {
				switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 1) {
						return sendQuestDialog(env, 2375);
					}
				case SELECT_REWARD:
					if (var == 1) {
						removeQuestItem(env, 182203231, 1);
						qs.setQuestVar(2);
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return sendQuestEndDialog(env);
					}
				}
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (env.getTargetId() == 203645) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
