package com.aionemu.gameserver.quest.handlers.empyrean_crucible;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 苍穹试炼场任务脚本：A Rift Adrift（任务 ID 28208）。
 * Empyrean Crucible quest script: A Rift Adrift (quest ID 28208).
 *
 * @author vlog
 */
public class _28208ARiftAdrift extends QuestHandler {

	private static final int questId = 28208;

	public _28208ARiftAdrift() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(205320).addOnQuestStart(questId);
		qe.registerQuestNpc(205320).addOnTalkEvent(questId);
		qe.registerQuestNpc(205321).addOnTalkEvent(questId);
		qe.registerQuestNpc(217819).addOnKillEvent(questId);
		qe.registerQuestNpc(218185).addOnKillEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 205320) { // Inggness
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				}
				else {
					return sendQuestStartDialog(env);
				}
			}
		}
		else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 205321) { // Anja
				if (env.getDialog() == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 10002);
				}
				else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}

	@Override
	public boolean onKillEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (var == 0) {
				int var1 = qs.getQuestVarById(1);
				if (var1 < 4) {
					return defaultOnKillEvent(env, 217819, 0, 4, 1); // 1: 0 - 4
				}
				else if (var1 == 4 && targetId == 217819) {
					qs.setQuestVar(1); // 1
					updateQuestStatus(env);
					return true;
				}
			}
			else if (var == 1 && targetId == 218185) {
				qs.setQuestVarById(2, 1); // 2: 1
				qs.setStatus(QuestStatus.REWARD); // reward
				updateQuestStatus(env);
				return true;
			}
		}
		return false;
	}
}
