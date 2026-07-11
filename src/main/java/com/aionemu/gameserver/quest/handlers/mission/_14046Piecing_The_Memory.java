package com.aionemu.gameserver.quest.handlers.mission;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 主线任务脚本：Piecing The Memory（任务 ID 14046）。
 * Campaign mission quest script: Piecing The Memory (quest ID 14046).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _14046Piecing_The_Memory extends QuestHandler {

    private final static int questId = 14046;
    private final static int[] npc_ids = {278500, 203834, 203786, 203754, 203704};
    public _14046Piecing_The_Memory() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerOnEnterZoneMissionEnd(questId);
        qe.registerOnLevelUp(questId);
        qe.registerQuestItem(182215354, questId);
        for (int npc_id : npc_ids) {
            qe.registerQuestNpc(npc_id).addOnTalkEvent(questId);
        }
    }
	
    @Override
    public boolean onZoneMissionEndEvent(QuestEnv env) {
        return defaultOnZoneMissionEndEvent(env);
    }
	
    @Override
    public boolean onLvlUpEvent(QuestEnv env) {
        return defaultOnLvlUpEvent(env, 14044, false);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null) {
            return false;
        }
        int var = qs.getQuestVarById(0);
		int targetId = env.getTargetId();
        if (qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 203704) {
                if (env.getDialog() == QuestDialog.USE_OBJECT) {
                    return sendQuestDialog(env, 10002);
                } else if (env.getDialogId() == 1009) {
                    return sendQuestDialog(env, 5);
                } else {
                    return sendQuestEndDialog(env);
                }
            }
            return false;
        } else if (qs.getStatus() != QuestStatus.START) {
            return false;
        } if (targetId == 278500) {
            switch (env.getDialog()) {
                case START_DIALOG:
                    if (var == 0) {
                        return sendQuestDialog(env, 1011);
                    }
                case STEP_TO_1:
                    if (var == 0) {
                        return defaultCloseDialog(env, 0, 1);
                    }
            }
        } else if (targetId == 203834) {
            switch (env.getDialog()) {
                case START_DIALOG:
                    if (var == 1) {
                        return sendQuestDialog(env, 1352);
                    } else if (var == 3) {
                        return sendQuestDialog(env, 2034);
                    } else if (var == 5) {
                        return sendQuestDialog(env, 2716);
                    }
                case SELECT_ACTION_1353:
                    playQuestMovie(env, 102);
                    break;
                case STEP_TO_2:
                    if (var == 1) {
                        return defaultCloseDialog(env, 1, 2);
                    }
                case STEP_TO_4:
                    if (var == 3) {
                        return defaultCloseDialog(env, 3, 4);
                    }
                case STEP_TO_6:
                    if (var == 5) {
                        removeQuestItem(env, 182215354, 1);
                        return defaultCloseDialog(env, 5, 6);
                    }
            }
        } else if (targetId == 203786) {
            switch (env.getDialog()) {
                case START_DIALOG:
                    if (var == 2) {
                        return sendQuestDialog(env, 1693);
                    }
                case CHECK_COLLECTED_ITEMS:
                    return checkQuestItems(env, 2, 3, false, 10000, 10001, 182215354, 1);
            }
        } else if (targetId == 203754) {
            switch (env.getDialog()) {
                case START_DIALOG:
                    if (var == 6) {
                        return sendQuestDialog(env, 3057);
                    }
                case SET_REWARD:
                    if (var == 6) {
                        return defaultCloseDialog(env, 6, 6, true, false);
                    }
            }
        }
        return false;
    }
	
    @Override
    public HandlerResult onItemUseEvent(final QuestEnv env, Item item) {
        final Player player = env.getPlayer();
        final int id = item.getItemTemplate().getTemplateId();
        final int itemObjId = item.getObjectId();
        if (id != 182215354) {
            return HandlerResult.UNKNOWN;
        }
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() != QuestStatus.START || qs.getQuestVarById(0) != 4) {
            return HandlerResult.UNKNOWN;
        }
        PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), itemObjId, id, 1000, 0, 0), true);
        GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
            @Override
            public void run() {
                PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), itemObjId, id, 0, 1, 0), true);
                playQuestMovie(env, 170);
                qs.setQuestVar(5);
                updateQuestStatus(env);
            }
        }, 1000);
        return HandlerResult.SUCCESS;
    }
}
