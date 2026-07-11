package com.aionemu.gameserver.quest.handlers.cradle_of_eternity;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 永恒摇篮任务脚本：How To Get To The Storm Cliff（任务 ID 26825）。
 * Cradle of Eternity quest script: How To Get To The Storm Cliff (quest ID 26825).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _26825How_To_Get_To_The_Storm_Cliff extends QuestHandler {

    private final static int questId = 26825;
    public _26825How_To_Get_To_The_Storm_Cliff() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(806288).addOnTalkEvent(questId);
		qe.registerQuestNpc(220534).addOnKillEvent(questId);
		qe.registerQuestNpc(220597).addOnKillEvent(questId);
		qe.registerOnEnterWorld(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 806288) {
                if (env.getDialogId() == 31) {
                    return sendQuestDialog(env, 10002);
				} else if (env.getDialogId() == 1009) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
			}
			else { // 赏金任务（DragonicK？） / Bounty Quest made DragonicK?
				// 所选物品不是可选的。（DainAvenger 奖励修正） / Selected item is not optional. correct for Selected item Reward DainAvenger
				env.setDialogId(8);
				env.setExtendedRewardIndex(8);
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(806288, 0));
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
            if (qs == null || qs.canRepeat()) {
                env.setQuestId(questId);
                if (QuestService.startQuest(env)) {
					return true;
				}
            }
        }
        return false;
    }
	
	public boolean onKillEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            switch (env.getTargetId()) {
                case 220534: //? ? ?.
				case 220597: //? 3   .
                if (qs.getQuestVarById(1) < 1) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 1) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
