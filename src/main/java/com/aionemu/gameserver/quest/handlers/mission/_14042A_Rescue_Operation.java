package com.aionemu.gameserver.quest.handlers.mission;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.teleport.TeleportService2;

/**
 * 主线任务脚本：A Rescue Operation（任务 ID 14042）。
 * Campaign mission quest script: A Rescue Operation (quest ID 14042).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _14042A_Rescue_Operation extends QuestHandler {

    private final static int questId = 14042;
    private final static int[] npcs = {278502, 278517, 278590, 253623};
    public _14042A_Rescue_Operation() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerOnEnterZoneMissionEnd(questId);
        qe.registerOnLevelUp(questId);
        for (int npc : npcs) {
            qe.registerQuestNpc(npc).addOnTalkEvent(questId);
        }
        qe.registerOnLogOut(questId);
        qe.registerAddOnReachTargetEvent(questId);
        qe.registerAddOnLostTargetEvent(questId);
        qe.registerQuestNpc(278517).addOnTalkEvent(questId); 
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
        if (qs.getStatus() == QuestStatus.START) {
            switch (targetId) {
                case 278502:
                    switch (env.getDialog()) {
                        case START_DIALOG: {
                            if (var == 0) {
                                return sendQuestDialog(env, 1011);
                            }
						} case STEP_TO_1: {
                            return defaultCloseDialog(env, 0, 1);
						}
                    }
				case 278517:
                    switch (env.getDialog()) {
                        case START_DIALOG: {
                            if (var == 1) {
                                return sendQuestDialog(env, 1352);
                            }
						} case STEP_TO_2: {
                            TeleportService2.teleportTo(player, 400010000, 384.70587f, 360.83548f, 1675.3441f, (byte) 33);
							return defaultCloseDialog(env, 1, 2);
						}
                    }
				case 278590:
                    switch (env.getDialog()) {
                        case START_DIALOG: {
                            if (var == 2) {
                                return sendQuestDialog(env, 1693);
                            }
						} case STEP_TO_3: {
                            TeleportService2.teleportTo(player, 400010000, 1014.8797f, 1077.7836f, 1543.2881f, (byte) 34);
							return defaultCloseDialog(env, 2, 3);
						}
                    }
				case 253623:
                    switch (env.getDialog()) {
                        case START_DIALOG: {
                            if (var == 3) {
                                return sendQuestDialog(env, 2034);
                            }
						} case SELECT_ACTION_2035: {
                            playQuestMovie(env, 269);
                            return sendQuestDialog(env, 2035);
                        } case STEP_TO_4: {
                            return defaultStartFollowEvent(env, (Npc) env.getVisibleObject(), 253635, 3, 4); //Prison Camp Elyos Search Squad.
                        }
                    }
            }
        } else if (qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 278517) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 10002);
                } else if (env.getDialogId() == 1009) {
                    return sendQuestDialog(env, 5);
                } else {
                    return sendQuestEndDialog(env);
                }
            }
        }
        return false;
    }
	
    @Override
    public boolean onLogOutEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
            if (var == 4) {
                changeQuestStep(env, 4, 3, false);
            }
        }
        return false;
    }
	
    @Override
    public boolean onNpcReachTargetEvent(QuestEnv env) {
        return defaultFollowEndEvent(env, 4, 4, true, 270);
    }
	
    @Override
    public boolean onNpcLostTargetEvent(QuestEnv env) {
        return defaultFollowEndEvent(env, 4, 3, false);
    }
	
    @Override
    public boolean onZoneMissionEndEvent(QuestEnv env) {
        return defaultOnZoneMissionEndEvent(env);
    }
	
    @Override
    public boolean onLvlUpEvent(QuestEnv env) {
        return defaultOnLvlUpEvent(env, 14040, false);
    }
}
