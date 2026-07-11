package com.aionemu.gameserver.quest.handlers.cradle_of_eternity;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 永恒摇篮任务脚本：Source Of The Contamination At The 3rd Library（任务 ID 16831）。
 * Cradle of Eternity quest script: Source Of The Contamination At The 3rd Library (quest ID 16831).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _16831Source_Of_The_Contamination_At_The_3rd_Library extends QuestHandler {

    private final static int questId = 16831;
    public _16831Source_Of_The_Contamination_At_The_3rd_Library() {
        super(questId);
    }
	
	@Override
	public void register() {
        qe.registerQuestNpc(806283).addOnTalkEvent(questId);
		qe.registerOnEnterWorld(questId);
		qe.registerOnEnterZone(ZoneName.get("IDETERNITY_02_Q16831_A_301550000"), questId);
		qe.registerOnEnterZone(ZoneName.get("IDETERNITY_02_Q16831_B_301550000"), questId);
		qe.registerOnEnterZone(ZoneName.get("IDETERNITY_02_Q16831_C_301550000"), questId);
	}
	
	@Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 806283) {
                if (env.getDialogId() == 31) {
                    return sendQuestDialog(env, 10002);
				} else if (env.getDialogId() == 1009) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
			}
			else { // 赏金任务（DragonicK？） / Bounty Quest made DragonicK?
				// 所选物品不是可选的。 / Selected item is not optional.
				env.setDialogId(8);
				env.setExtendedRewardIndex(1);
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(806283, 0));
				if (QuestService.finishQuest(env)) {
					return closeDialogWindow(env);
				}
			}
		}
		return false;
	}
	
	@Override
    public boolean onEnterWorldEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (player.getWorldId() == 301550000) { //?? ?.
            if (qs == null) {
                env.setQuestId(questId);
                if (QuestService.startQuest(env)) {
					return true;
				}
            }
        }
        return false;
    }
	
	@Override
    public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
			if (zoneName == ZoneName.get("IDETERNITY_02_Q16831_A_301550000")) {
				if (var == 0) {
					changeQuestStep(env, 0, 1, false);
					return true;
				}
			} else if (zoneName == ZoneName.get("IDETERNITY_02_Q16831_B_301550000")) {
				if (var == 1) {
					changeQuestStep(env, 1, 2, false);
					return true;
				}
			} else if (zoneName == ZoneName.get("IDETERNITY_02_Q16831_C_301550000")) {
				if (var == 2) {
					qs.setQuestVar(3);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return true;
				}
			}
		}
		return false;
	}
}
