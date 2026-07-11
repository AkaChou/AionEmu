package com.aionemu.gameserver.quest.handlers.stigma_vision;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.*;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.handlers.*;
import com.aionemu.gameserver.questEngine.model.*;
import com.aionemu.gameserver.services.*;
import com.aionemu.gameserver.services.mail.*;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.zone.*;

/**
 * 污名幻象任务脚本：Stigmore（任务 ID 13833）。
 * Stigma Vision quest script: Stigmore (quest ID 13833).
 *
 * @author Rinzler (Encom) correct DragonicK?
 */
public class _13833Stigmore extends QuestHandler {
    private final static int questId = 13833;
    public _13833Stigmore() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerOnLevelUp(questId);
		qe.registerQuestNpc(203711).addOnTalkEvent(questId); //Miriya.
		qe.registerQuestItem(182216121, questId);
		qe.registerOnEnterWorld(questId);
	}
	
	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			if (qs.getQuestVarById(0) == 0) {
				qs.setQuestVar(1);
				changeQuestStep(env, 1, 1, true);
				return HandlerResult.SUCCESS;
			}
		}
		return HandlerResult.FAILED;
	}
	
	@Override
	public boolean onLvlUpEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (player.getLevel() >= 50 && (qs == null || qs.getStatus() == QuestStatus.NONE) && player.getRace() == Race.ELYOS) {
			giveQuestItem(env, 182216121, 1);
			return QuestService.startQuest(env);
		}
		return false;
	}
	
	@Override
	public boolean onEnterWorldEvent(QuestEnv env) { // Fix for player who already have this Quest
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START && player.getInventory().getItemCountByItemId(182216121) < 1 && player.getWorldId() == 110010000) {
			return giveQuestItem(env, 182216121, 1);
		}
		return false;
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null) {
			return false;
		}
		if (qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 203711) { //Miriya.
                if (env.getDialog() == QuestDialog.USE_OBJECT) {
                    return sendQuestDialog(env, 10002);
				} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
			}
			else { // 赏金任务（DragonicK？） / Bounty Quest made DragonicK?
				// 所选物品不是可选的。 / Selected item is not optional.
				env.setDialogId(QuestDialog.SELECTED_QUEST_REWARD1.id());
				env.setExtendedRewardIndex(1);
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(203711, 0));
				if (QuestService.finishQuest(env)) {
					return closeDialogWindow(env);
				}
			}
		}
		return false;
	}
}
