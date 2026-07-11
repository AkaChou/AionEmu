package com.aionemu.gameserver.quest.handlers.trials_of_eternity;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.world.zone.ZoneName;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 永恒试炼任务脚本：Weakened Artifact Of Knowledge（任务 ID 26836）。
 * Trials of Eternity quest script: Weakened Artifact Of Knowledge (quest ID 26836).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _26836Weakened_Artifact_Of_Knowledge extends QuestHandler {

    private final static int questId = 26836;
	private final static int[] npcs = {806572, 806578};
	private final static int[] IDEternity03GuardBoss = {246425, 246426};
    public _26836Weakened_Artifact_Of_Knowledge() {
        super(questId);
    }
	
	@Override
	public void register() {
		for (int npc: npcs) {
            qe.registerQuestNpc(npc).addOnTalkEvent(questId);
        } for (int mob: IDEternity03GuardBoss) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		}
        qe.registerQuestNpc(247075).addOnKillEvent(questId);
		qe.registerOnEnterWorld(questId);
		qe.registerOnEnterZoneMissionEnd(questId);
		qe.registerOnEnterZone(ZoneName.get("IDETERNITY_03_Q16836_A_301560000"), questId);
		qe.registerOnEnterZone(ZoneName.get("IDETERNITY_03_Q16836_B_301560000"), questId);
		qe.registerOnEnterZone(ZoneName.get("IDETERNITY_03_Q16836_C_301560000"), questId);
		qe.registerOnEnterZone(ZoneName.get("IDETERNITY_03_Q16836_D_301560000"), questId);
		qe.registerOnEnterZone(ZoneName.get("IDETERNITY_03_Q16836_E_301560000"), questId);
		qe.registerOnEnterZone(ZoneName.get("IDETERNITY_03_Q16836_F_301560000"), questId);
		qe.registerOnEnterZone(ZoneName.get("IDETERNITY_03_Q16836_G_301560000"), questId);
		qe.registerOnEnterZone(ZoneName.get("IDETERNITY_03_Q16836_H_301560000"), questId);
		qe.registerOnEnterZone(ZoneName.get("IDETERNITY_03_Q16836_I_301560000"), questId);
		qe.registerOnEnterZone(ZoneName.get("IDETERNITY_03_Q16836_J_301560000"), questId);
		qe.registerOnEnterZone(ZoneName.get("IDETERNITY_03_Q16836_K_301560000"), questId);
	}
	
	@Override
	public boolean onZoneMissionEndEvent(QuestEnv env) {
		return defaultOnZoneMissionEndEvent(env);
	}
	
	@Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int var = qs.getQuestVarById(0);
        int targetId = env.getTargetId();
        if (qs == null || qs.getStatus() == QuestStatus.START) {
			if (targetId == 806572) { //Peregrine Start.
				switch (env.getDialog()) {
                    case START_DIALOG: {
						if (var == 0) {
							return sendQuestDialog(env, 1011);
						}
					} case SELECT_ACTION_1012: {
						if (var == 0) {
							return sendQuestDialog(env, 1012);
						}
					} case STEP_TO_1: {
						changeQuestStep(env, 0, 1, false);
						return closeDialogWindow(env);
					}
                }
			}
		} 
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 806578) {
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
				env.setExtendedRewardIndex(8);
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(806578, 0));
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
		if (player.getWorldId() == 301560000) { //Heart Of Eternity 5.5
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
	public boolean onKillEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
            switch (env.getTargetId()) {
				case 246425:
                case 246426: {
					qs.setQuestVar(9);
					return defaultOnKillEvent(env, 246425, 0, 1, 1) && defaultOnKillEvent(env, 246426, 0, 2, 1);
                }  
				case 247075: {
					qs.setQuestVar(14);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return defaultOnKillEvent(env, 247075, 0, 1, 1);
                }
            }
        }
        return false;
    }
	
	@Override
    public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
			if (zoneName == ZoneName.get("IDETERNITY_03_Q16836_A_301560000")) {
				if (var == 1) {
					changeQuestStep(env, 1, 2, false);
					return true;
				}
			} else if (zoneName == ZoneName.get("IDETERNITY_03_Q16836_B_301560000")) {
				if (var == 2) {
					changeQuestStep(env, 2, 3, false);
					return true;
				}
			} else if (zoneName == ZoneName.get("IDETERNITY_03_Q16836_C_301560000")) {
				if (var == 3) {
					changeQuestStep(env, 3, 4, false);
					return true;
				}
			} else if (zoneName == ZoneName.get("IDETERNITY_03_Q16836_D_301560000")) {
				if (var == 4) {
					changeQuestStep(env, 4, 5, false);
					return true;
				}
			} else if (zoneName == ZoneName.get("IDETERNITY_03_Q16836_E_301560000")) {
				if (var == 5) {
					changeQuestStep(env, 5, 6, false);
					return true;
				}
			} else if (zoneName == ZoneName.get("IDETERNITY_03_Q16836_F_301560000")) {
				if (var == 6) {
					changeQuestStep(env, 6, 7, false);
					return true;
				}
			} else if (zoneName == ZoneName.get("IDETERNITY_03_Q16836_G_301560000")) {
				if (var == 7) {
					changeQuestStep(env, 7, 8, false);
					return true;
				}
			} else if (zoneName == ZoneName.get("IDETERNITY_03_Q16836_H_301560000")) {
				if (var == 9) {
					playQuestMovie(env, 964);
					changeQuestStep(env, 9, 10, false);
					return true;
				}
			} else if (zoneName == ZoneName.get("IDETERNITY_03_Q16836_I_301560000")) {
				if (var == 10) {
					changeQuestStep(env, 10, 11, false);
					return true;
				}
			} else if (zoneName == ZoneName.get("IDETERNITY_03_Q16836_J_301560000")) {
				if (var == 11) {
					changeQuestStep(env, 11, 12, false);
					return true;
				}
			} else if (zoneName == ZoneName.get("IDETERNITY_03_Q16836_K_301560000")) {
				if (var == 12) {
					changeQuestStep(env, 12, 13, false);
					return true;
				}
			}
		}
		return false;
	}
}
