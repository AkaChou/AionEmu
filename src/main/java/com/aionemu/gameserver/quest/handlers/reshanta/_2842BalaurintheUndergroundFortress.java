package com.aionemu.gameserver.quest.handlers.reshanta;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 雷山塔任务脚本：Balaurinthe Underground Fortress（任务 ID 2842）。
 * Reshanta quest script: Balaurinthe Underground Fortress (quest ID 2842).
 *
 * @author Hilgert
 */
public class _2842BalaurintheUndergroundFortress extends QuestHandler {

	private final static int questId = 2842;
	public _2842BalaurintheUndergroundFortress() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(266568).addOnQuestStart(questId);
		qe.registerQuestNpc(266568).addOnTalkEvent(questId);
		qe.registerQuestNpc(215447).addOnKillEvent(questId);
		qe.registerQuestNpc(214777).addOnKillEvent(questId);
		qe.registerQuestNpc(214773).addOnKillEvent(questId);
		qe.registerQuestNpc(214781).addOnKillEvent(questId);
		qe.registerQuestNpc(214784).addOnKillEvent(questId);
		qe.registerQuestNpc(700472).addOnKillEvent(questId);
		qe.registerQuestNpc(700474).addOnKillEvent(questId);
		qe.registerQuestNpc(700473).addOnKillEvent(questId);
		qe.registerOnEnterWorld(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.getStatus() == QuestStatus.COMPLETE) {
			if (targetId == 266568) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 266568)
				return true;
		}
		else if (qs == null || qs.getStatus() == QuestStatus.REWARD && targetId == 266568) {
			qs.setQuestVarById(0, 0);
			updateQuestStatus(env);
			return sendQuestEndDialog(env);
		}
		return false;
	}

	public boolean onKillEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() != QuestStatus.START)
			return false;
		if (qs.getStatus() == QuestStatus.START) {
			if (player.getCommonData().getPosition().getMapId() == 300070000) {
				if (qs.getQuestVarById(0) < 38) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					updateQuestStatus(env);
					return true;
				}
				else if (qs.getQuestVarById(0) == 38 || qs.getQuestVarById(0) > 38) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return true;
				}
			}
		}
		return false;
	}
}
