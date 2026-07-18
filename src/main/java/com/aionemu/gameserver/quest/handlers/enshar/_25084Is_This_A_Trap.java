package com.aionemu.gameserver.quest.handlers.enshar;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 恩沙尔任务脚本：Is This A Trap（任务 ID 25084）。
 * Enshar quest script: Is This A Trap (quest ID 25084).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25084Is_This_A_Trap extends QuestHandler {

    private final static int questId = 25084;
    public _25084Is_This_A_Trap() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerQuestNpc(804926).addOnQuestStart(questId);
        qe.registerQuestNpc(804926).addOnTalkEvent(questId);
        qe.registerQuestNpc(804925).addOnTalkEvent(questId);
		qe.registerQuestNpc(804927).addOnTalkEvent(questId);
		qe.registerOnEnterZone(ZoneName.get("DF5_SENSORYAREA_Q25084_220080000"), questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        int targetId = env.getTargetId();
        QuestDialog dialog = env.getDialog();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 804926) {
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } else if (qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
			if (targetId == 804925) {
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 1011);
				} else if (dialog == QuestDialog.CHECK_COLLECTED_ITEMS) {
					if (QuestService.collectItemCheck(env, true)) {
					changeQuestStep(env, 0, 1, false);
                    return closeDialogWindow(env);
					}
                }
            } if (targetId == 804926) { 
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 1352);
				} else if (dialog == QuestDialog.STEP_TO_2) {
					changeQuestStep(env, 1, 2, false); 
                    return closeDialogWindow(env);
                }
            }
		} else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 804927) {
                if (dialog == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 2034);
                } else {
                    return sendQuestEndDialog(env);
                }
            }
        }
        return false;
    }
	
	@Override
	public boolean onKillEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() != QuestStatus.START) {
			return false;
		}	
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		} switch (targetId) {
			case 220038:
				if (qs.getQuestVarById(0) == 2) {

                    return true;
				}
			break;
		}
		return false;
	}
	
	@Override
	public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
		if (zoneName == ZoneName.get("DF5_SENSORYAREA_Q25084_220080000")) {
			Player player = env.getPlayer();
			if (player == null) {
				return false;
			}
			QuestState qs = player.getQuestStateList().getQuestState(questId);
			if (qs != null && qs.getStatus() == QuestStatus.START) {
				int var = qs.getQuestVarById(0);
				if (var == 2) {
					QuestService.addNewSpawnForSeconds(220080000, player.getInstanceId(), 220038, player.getX() - 2,
							player.getY() + 2, player.getZ(), (byte) 19, 300);
					QuestService.addNewSpawnForSeconds(220080000, player.getInstanceId(), 220038, player.getX() + 2,
							player.getY() - 2, player.getZ(), (byte) 53, 300);
					changeQuestStep(env, 2, 3, false);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return true;
				}
			}
		}
		return false;
	}
}
