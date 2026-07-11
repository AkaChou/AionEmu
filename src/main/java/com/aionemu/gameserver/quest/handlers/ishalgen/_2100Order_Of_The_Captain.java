package com.aionemu.gameserver.quest.handlers.ishalgen;

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
 * 伊沙尔根任务脚本：Order Of The Captain（任务 ID 2100）。
 * Ishalgen quest script: Order Of The Captain (quest ID 2100).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _2100Order_Of_The_Captain extends QuestHandler {

	private final static int questId = 2100;
	public _2100Order_Of_The_Captain() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(203516).addOnTalkEvent(questId);
		qe.registerOnEnterZone(ZoneName.get("ALDELLE_VILLAGE_220010000"), questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null)
			return false;
		int targetId = env.getTargetId();
		if (targetId != 203516)
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
				int[] misions = {2001, 2002, 2003, 2004, 2005, 2006, 2007};
				for (int id: misions) {
					GameEngineServices.questEngine().onEnterZoneMissionEnd(new QuestEnv(env.getVisibleObject(), env.getPlayer(), id, env.getDialogId()));
				}
			}
			return sendQuestEndDialog(env);
		}
		return false;
	}
	
	@Override
	public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
		return defaultOnEnterZoneEvent(env, zoneName, ZoneName.get("ALDELLE_VILLAGE_220010000"));
	}
}
