package com.aionemu.gameserver.quest.handlers.udas_temple;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 乌达斯神殿任务脚本：Lair Of The Dragonbound（任务 ID 30103）。
 * Udas Temple quest script: Lair Of The Dragonbound (quest ID 30103).
 *
 * @author vlog
 */
public class _30103LairOfTheDragonbound extends QuestHandler {

	private static final int questId = 30103;
	public _30103LairOfTheDragonbound() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(799333).addOnQuestStart(questId);
		qe.registerQuestNpc(799333).addOnTalkEvent(questId);
		qe.registerGetingItem(182209189, questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 799333) {
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
			if (targetId == 799333) { 
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
		else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799333) { 
				removeQuestItem(env, 182209189, 1);
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}

	@Override
	public boolean onGetItemEvent(QuestEnv env) {
		return defaultOnGetItemEvent(env, 0, 1, false); // 1
	}
}
