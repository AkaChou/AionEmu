package com.aionemu.gameserver.quest.handlers.verteron;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 沃特伦任务脚本：Reducing Tursin Strength（任务 ID 1194）。
 * Verteron quest script: Reducing Tursin Strength (quest ID 1194).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _1194Reducing_Tursin_Strength extends QuestHandler {

	private final static int questId = 1194;
	public _1194Reducing_Tursin_Strength() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(203098).addOnQuestStart(questId);
		qe.registerQuestNpc(203098).addOnTalkEvent(questId);
		qe.registerQuestNpc(210185).addOnKillEvent(questId);
		qe.registerQuestNpc(210186).addOnKillEvent(questId);
		qe.registerOnEnterZone(ZoneName.get("LF1A_SENSORY_AREA_Q1194_210030000"), questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 203098) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				} else
					return sendQuestStartDialog(env);
			}
		} 
        if (qs == null)
			return false;
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203098) {
				switch (env.getDialog()) {
					case USE_OBJECT: {
						return sendQuestDialog(env, 1352);
					} case SELECT_REWARD: {
						return sendQuestDialog(env, 5);
					} default:
						return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
		if (zoneName != ZoneName.get("LF1A_SENSORY_AREA_Q1194_210030000"))
			return false;
		final Player player = env.getPlayer();
		if (player == null)
			return false;
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null)
			return false;
		if (qs.getQuestVarById(0) == 0) {
			qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
			updateQuestStatus(env);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onKillEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() != QuestStatus.START) {
			return false;
		}
		int targetId = 0;
		int questVar = qs.getQuestVarById(0);
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (targetId == 210185 || targetId == 210186) {
           if (questVar >= 1 && questVar < 10) {
			    if (questVar == 9)
				qs.setStatus(QuestStatus.REWARD);
				else
				qs.setQuestVarById(0, questVar+1);
				updateQuestStatus(env);
				return true;
			}
		}
		return false;
	}
}
