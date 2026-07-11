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
 * 主线任务脚本：Aegirs Orders（任务 ID 24020）。
 * Campaign mission quest script: Aegirs Orders (quest ID 24020).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _24020Aegirs_Orders extends QuestHandler {

    private final static int questId = 24020;
    public _24020Aegirs_Orders() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerQuestNpc(204301).addOnTalkEvent(questId);
        qe.registerOnEnterZone(ZoneName.get("MORHEIM_ICE_FORTRESS_220020000"), questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null) {
            return false;
        }
		int targetId = env.getTargetId();
        if (targetId != 204301) {
            return false;
        } if (qs.getStatus() == QuestStatus.START) {
            if (env.getDialog() == QuestDialog.START_DIALOG) {
                return sendQuestDialog(env, 1011);
            } else if (env.getDialogId() == 1009) {
				qs.setStatus(QuestStatus.REWARD);
				updateQuestStatus(env);
				return sendQuestEndDialog(env);
			}
        } else if (qs.getStatus() == QuestStatus.REWARD) {
            if (env.getDialogId() == 23) {
                int[] ids = {24021, 24022, 24023, 24024, 24025, 24026};
                for (int id : ids) {
                    GameEngineServices.questEngine().onEnterZoneMissionEnd(new QuestEnv(env.getVisibleObject(), env.getPlayer(), id, env.getDialogId()));
                }
            }
            return sendQuestEndDialog(env);
        }
        return false;
    }
	
    @Override
    public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
        return defaultOnEnterZoneEvent(env, zoneName, ZoneName.get("MORHEIM_ICE_FORTRESS_220020000"));
    }
}
