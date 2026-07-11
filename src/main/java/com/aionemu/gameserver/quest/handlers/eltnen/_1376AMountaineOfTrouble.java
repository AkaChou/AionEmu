package com.aionemu.gameserver.quest.handlers.eltnen;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 艾特南任务脚本：A Mountaine Of Trouble（任务 ID 1376）。
 * Eltnen quest script: A Mountaine Of Trouble (quest ID 1376).
 *
 * @author Atomics
 */
public class _1376AMountaineOfTrouble extends QuestHandler {

	private final static int questId = 1376;
	public _1376AMountaineOfTrouble() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203947).addOnQuestStart(questId); // Beramones
		qe.registerQuestNpc(203947).addOnTalkEvent(questId); // Beramones
		qe.registerQuestNpc(203964).addOnTalkEvent(questId); // Agrips
		qe.registerQuestNpc(210976).addOnKillEvent(questId); // Kerubien Hunter
		qe.registerQuestNpc(210986).addOnKillEvent(questId); // Kerubien Hunter
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 203947) { // Beramones
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203964) { // Agrips
				if (env.getDialog() == QuestDialog.USE_OBJECT)
					return sendQuestDialog(env, 1352);
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}

	@Override
	public boolean onKillEvent(QuestEnv env) {
		int[] mobs = { 210976, 210986 };
		if (defaultOnKillEvent(env, mobs, 0, 6) || defaultOnKillEvent(env, mobs, 6, true))
			return true;
		else
			return false;
	}
}
