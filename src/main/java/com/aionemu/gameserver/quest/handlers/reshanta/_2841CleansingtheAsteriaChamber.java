package com.aionemu.gameserver.quest.handlers.reshanta;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 雷山塔任务脚本：Cleansingthe Asteria Chamber（任务 ID 2841）。
 * Reshanta quest script: Cleansingthe Asteria Chamber (quest ID 2841).
 *
 * @author Hilgert
 */
public class _2841CleansingtheAsteriaChamber extends QuestHandler {

	private final static int questId = 2841;
	public _2841CleansingtheAsteriaChamber() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(271068).addOnQuestStart(questId);
		qe.registerQuestNpc(271068).addOnTalkEvent(questId);
		qe.registerQuestNpc(214762).addOnKillEvent(questId);
		qe.registerQuestNpc(214755).addOnKillEvent(questId);
		qe.registerQuestNpc(214752).addOnKillEvent(questId);
		qe.registerQuestNpc(214754).addOnKillEvent(questId);
		qe.registerQuestNpc(215441).addOnKillEvent(questId);
		qe.registerQuestNpc(214758).addOnKillEvent(questId);
		qe.registerQuestNpc(214766).addOnKillEvent(questId);
		qe.registerQuestNpc(214753).addOnKillEvent(questId);
		qe.registerQuestNpc(215444).addOnKillEvent(questId);
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
			if (targetId == 271068) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 271068)
				return true;
		}
		else if (qs == null || qs.getStatus() == QuestStatus.REWARD && targetId == 271068) {
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
			if (player.getCommonData().getPosition().getMapId() == 300050000) {
				if (qs.getQuestVarById(0) < 43) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					updateQuestStatus(env);
					return true;
				}
				else if (qs.getQuestVarById(0) == 43 || qs.getQuestVarById(0) > 43) {
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
