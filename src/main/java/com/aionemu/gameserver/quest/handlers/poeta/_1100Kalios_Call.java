package com.aionemu.gameserver.quest.handlers.poeta;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 波伊塔任务脚本：Kalios Call（任务 ID 1100）。
 * Poeta quest script: Kalios Call (quest ID 1100).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _1100Kalios_Call extends QuestHandler {
	private final static int questId = 1100;
	public _1100Kalios_Call() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(203067).addOnTalkEvent(questId);
		qe.registerOnEnterZone(ZoneName.get("AKARIOS_VILLAGE_210010000"), questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null)
			return false;
		int targetId = env.getTargetId();
		if (targetId != 203067)
			return false;
		if (qs.getStatus() == QuestStatus.START) {
			if (env.getDialog() == QuestDialog.START_DIALOG) {
				return sendQuestDialog(env, 1011);
			} else if (env.getDialogId() == 1009) {
				qs.setStatus(QuestStatus.REWARD);
				updateQuestStatus(env);
                return sendQuestEndDialog(env);
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (env.getDialogId() == 23) {
				int[] ids = {1001, 1002, 1003, 1004, 1005};
				for (int id: ids) {
					GameEngineServices.questEngine().onEnterZoneMissionEnd(new QuestEnv(env.getVisibleObject(), env.getPlayer(), id, env.getDialogId()));
				}
			}
			return sendQuestEndDialog(env);
		}
		return false;
	}
	
	@Override
	public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
		return defaultOnEnterZoneEvent(env, zoneName, ZoneName.get("AKARIOS_VILLAGE_210010000"));
	}
}
