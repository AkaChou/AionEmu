package com.aionemu.gameserver.quest.handlers.mission;

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
 * 主线任务脚本：Orders From Reshanta（任务 ID 14040）。
 * Campaign mission quest script: Orders From Reshanta (quest ID 14040).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _14040Orders_From_Reshanta extends QuestHandler {

    private final static int questId = 14040;
    public _14040Orders_From_Reshanta() {
        super(questId);
    }
	
    @Override
	public void register() {
		qe.registerQuestNpc(278501).addOnTalkEvent(questId);
		qe.registerOnEnterZone(ZoneName.get("TEMINON_FORTRESS_400010000"), questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null)
			return false;
		int targetId = env.getTargetId();
		if (targetId != 278501)
			return false;
		if (qs.getStatus() == QuestStatus.START) {
			if (env.getDialog() == QuestDialog.START_DIALOG) {
				return sendQuestDialog(env, 10002);
			} else if (env.getDialogId() == 1009) {
				qs.setStatus(QuestStatus.REWARD);
				updateQuestStatus(env);
				return sendQuestDialog(env, 5);
			}
			return false;
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (env.getDialogId() == 23) {
				int[] ids = {14041, 14042, 14043, 14044, 14045, 14046, 14047};
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
		return defaultOnEnterZoneEvent(env, zoneName, ZoneName.get("TEMINON_FORTRESS_400010000"));
	}
}
