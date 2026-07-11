package com.aionemu.gameserver.quest.handlers.mission;

import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 主线任务脚本：A Speedy Errand（任务 ID 24045）。
 * Campaign mission quest script: A Speedy Errand (quest ID 24045).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _24045A_Speedy_Errand extends QuestHandler {

    private final static int questId = 24045;
    private final static int[] npc_ids = {278034, 279004, 279006, 279024};
    public _24045A_Speedy_Errand() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerOnLevelUp(questId);
		qe.registerOnEnterZoneMissionEnd(questId);
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
        return defaultOnLvlUpEvent(env, 24044, false);
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
            if (targetId == 278034) { //Holm.
                if (env.getDialog() == QuestDialog.USE_OBJECT) {
                    return sendQuestDialog(env, 10002);
                } else if (env.getDialogId() == QuestDialog.SELECT_REWARD.id()) {
                    return sendQuestDialog(env, 5);
                } else {
                    return sendQuestEndDialog(env);
                }
            }
            return false;
        } else if (qs.getStatus() != QuestStatus.START) {
            return false;
        } if (targetId == 278034) { //Holm.
            switch (env.getDialog()) {
                case START_DIALOG: {
                    if (var == 0) {
                        return sendQuestDialog(env, 1011);
                    }
				} case STEP_TO_1: {
                    if (var == 0) {
                        changeQuestStep(env, 0, 1, false);
						return closeDialogWindow(env);
                    }
				}
            }
        } else if (targetId == 279004) { //Tierunerk.
            switch (env.getDialog()) {
                case START_DIALOG: {
                    if (var == 1) {
                        return sendQuestDialog(env, 1352);
                    }
				} case SELECT_ACTION_1353: {
                    playQuestMovie(env, 292);
                    break;
				} case STEP_TO_2: {
                    if (var == 1) {
                        // 蒂格拉基岛。 / Tigraki Island.
						TeleportService2.teleportTo(player, 400010000, 136.4838f, 790.4456f, 2836.5127f, (byte) 48);
						changeQuestStep(env, 1, 2, false);
						return closeDialogWindow(env);
                    }
				}
            }
        } else if (targetId == 279024) { //Gaochinerk.
            switch (env.getDialog()) {
                case START_DIALOG: {
                    if (var == 2) {
                        return sendQuestDialog(env, 1693);
                    } else if (var == 4) {
                        return sendQuestDialog(env, 2375);
                    }
				} case STEP_TO_3: {
                    if (var == 2) {
                        qs.setQuestVarById(0, var + 1);
						updateQuestStatus(env);
						player.setState(CreatureState.FLIGHT_TELEPORT);
						player.unsetState(CreatureState.ACTIVE);
						player.setFlightTeleportId(55001);
						PacketSendUtility.sendPacket(player, new SM_EMOTION(player, EmotionType.START_FLYTELEPORT, 55001, 0));
					    return closeDialogWindow(env);
                    }
				} case STEP_TO_5: {
                    if (var == 4) {
                        qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						// 普里姆要塞。 / Primum Fortress.
						TeleportService2.teleportTo(player, 400010000, 577.0000f, 2541.0000f, 1636.0000f, (byte) 0);
						return closeDialogWindow(env);
                    }
				}
            }
        } else if (targetId == 279006) { //Garkbinerk.
            switch (env.getDialog()) {
                case START_DIALOG: {
                    if (var == 3) {
                        return sendQuestDialog(env, 2034);
                    }
				} case STEP_TO_4: {
                    if (var == 3) {
                        qs.setQuestVarById(0, var + 1);
						updateQuestStatus(env);
						player.setState(CreatureState.FLIGHT_TELEPORT);
						player.unsetState(CreatureState.ACTIVE);
						player.setFlightTeleportId(56001);
						PacketSendUtility.sendPacket(player, new SM_EMOTION(player, EmotionType.START_FLYTELEPORT, 56001, 0));
					    return closeDialogWindow(env);
                    }
				}
            }
        }
        return false;
    }
}
