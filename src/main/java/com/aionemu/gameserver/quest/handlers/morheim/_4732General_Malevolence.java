package com.aionemu.gameserver.quest.handlers.morheim;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 莫尔海姆任务脚本：General Malevolence（任务 ID 4732）。
 * Morheim quest script: General Malevolence (quest ID 4732).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _4732General_Malevolence extends QuestHandler {

	private static final int questId = 4732;
	public _4732General_Malevolence() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(800519).addOnQuestStart(questId); //Kelmar.
		qe.registerQuestNpc(800519).addOnTalkEvent(questId); //Kelmar.
		qe.registerQuestNpc(256694).addOnKillEvent(questId);
		qe.registerQuestNpc(256693).addOnKillEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 800519) { //Kelmar.
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env, 182205676, 1);
				}
			}
		} else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 800519) { //Kelmar.
				if (dialog == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 10002);
				} else {
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
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (var == 0) {
				return defaultOnKillEvent(env, 256694, 0, 1);
			} else if (var == 1) {
				return defaultOnKillEvent(env, 256693, 1, true);
			}
		}
		return false;
	}
}
