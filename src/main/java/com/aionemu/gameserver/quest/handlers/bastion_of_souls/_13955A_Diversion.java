package com.aionemu.gameserver.quest.handlers.bastion_of_souls;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 灵魂堡垒任务脚本：A Diversion（任务 ID 13955）。
 * Bastion of Souls quest script: A Diversion (quest ID 13955).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _13955A_Diversion extends QuestHandler {

    private final static int questId = 13955;
	private final static int[] IDAb1Ere1RoundDrakanFi = {247113, 247133, 247181, 246556, 246855, 246865};
    public _13955A_Diversion() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerOnEnterWorld(questId);
		qe.registerQuestNpc(806582).addOnTalkEvent(questId);
		for (int mob: IDAb1Ere1RoundDrakanFi) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		}
		qe.registerQuestNpc(246561).addOnKillEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 806582) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 10002);
                } else {
                    return sendQuestEndDialog(env);
                }
		    }
			else { // 赏金任务（DragonicK？） / Bounty Quest made DragonicK?
				// 所选物品不是可选的。 / Selected item is not optional.
				env.setDialogId(8);
				env.setExtendedRewardIndex(1);
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(806582, 0));
				if (QuestService.finishQuest(env)) {
					return closeDialogWindow(env);
				}
			}
		}
		return false;
	}
	
	@Override
    public boolean onEnterWorldEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (player.getWorldId() == 302340000) { //Bastion Of Souls 5.5
            if (qs == null || qs.canRepeat()) {
                env.setQuestId(questId);
                if (QuestService.startQuest(env)) {
					return true;
				}
            }
        }
        return false;
    }
	
	@Override
    public boolean onKillEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        int targetId = env.getTargetId();
        if (qs == null || qs.getStatus() != QuestStatus.START) {
            return false;
        }
        int var = qs.getQuestVarById(0);
        int var1 = qs.getQuestVarById(1);
        int var2 = qs.getQuestVarById(2);
		int[] IDAb1Ere1RoundDrakanFi = {247113, 247133, 247181, 246556, 246855, 246865};
		int[] IDAb1Ere2RoundDrakanHighFi = {246561};
            if (var == 0) {
				if (var1 + var2 < 6) {
					if (targetId == 246561) {
						if (var2 < 5) {
							return defaultOnKillEvent(env, IDAb1Ere2RoundDrakanHighFi, var2, var2 + 1, 2);
						}
					} else {
						if (var1 < 5) {
							return defaultOnKillEvent(env, IDAb1Ere1RoundDrakanFi, var1, var1 + 1, 1);
						}
					}
				} else {
					qs.setQuestVar(1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return true;
				}
			}
        return false;
    }
}
