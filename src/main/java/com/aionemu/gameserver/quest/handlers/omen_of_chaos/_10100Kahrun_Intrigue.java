package com.aionemu.gameserver.quest.handlers.omen_of_chaos;

import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 混沌预兆任务脚本：Kahrun Intrigue（任务 ID 10100）。
 * Omen of Chaos quest script: Kahrun Intrigue (quest ID 10100).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _10100Kahrun_Intrigue extends QuestHandler
{
    private final static int questId = 10100;
	
    public _10100Kahrun_Intrigue() {
        super(questId);
    }
	
	@Override
	public void register() {
		int[] npcIds = {804699, 802359, 802462};
		qe.registerOnLevelUp(questId);
		qe.registerQuestItem(182215448, questId);
		for (int npcId: npcIds) {
			qe.registerQuestNpc(npcId).addOnTalkEvent(questId);
		}
		qe.registerOnEnterZone(ZoneName.get("LUMEWILLOW_GLADE_SCOUT_BASE_600100000"),questId);
	}
	
	@Override
	public boolean onLvlUpEvent(QuestEnv env) {
		return defaultOnLvlUpEvent(env);
	}
	
	@Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null) {
            return false;
        }
        int var = qs.getQuestVarById(0);
        if (qs.getStatus() == QuestStatus.START) {
            switch (targetId) {
                case 804699: { //Atmis.
                    switch (dialog) {
                        case START_DIALOG: {
                            if (var == 0) {
                                return sendQuestDialog(env, 1011);
                            }
                        } case STEP_TO_1: {
                            TeleportService2.teleportTo(player, 600100000, 199.6679f, 182.91f, 375.02075f, (byte) 64, TeleportAnimation.BEAM_ANIMATION);
							changeQuestStep(env, 0, 1, false);
							return closeDialogWindow(env);
                        }
                    }
                    break;
                } case 802359: { //Tarena.
                    switch (dialog) {
                        case START_DIALOG: {
                            if (var == 2) {
                                return sendQuestDialog(env, 1693);
                            }
                        } case STEP_TO_3: {
                            return defaultCloseDialog(env, 2, 3);
                        }
                    }
                    break;
                } case 802462: { //Kahrun.
                    switch (dialog) {
                        case START_DIALOG: {
							if (var == 3) {
                                return sendQuestDialog(env, 2034);
                            }
                        } case STEP_TO_4: {
                            return defaultCloseDialog(env, 3, 4, 182215448, 1, 0, 0);
                        }
                    }
                }
            }
        } else if (qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 802462) { //Kahrun.
                if (env.getDialog() == QuestDialog.USE_OBJECT) {
                    return sendQuestDialog(env, 10002);
                } else {
                    return sendQuestEndDialog(env);
                }
            }
        }
        return false;
    }
	
	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getQuestVarById(0) == 4) {
			return HandlerResult.fromBoolean(useQuestItem(env, item, 4, 4, true));
		}
		return HandlerResult.FAILED;
	}
	
	@Override
    public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
        if (zoneName == ZoneName.get("LUMEWILLOW_GLADE_SCOUT_BASE_600100000")) {
            Player player = env.getPlayer();
            if (player == null)
                return false;
            QuestState qs = player.getQuestStateList().getQuestState(questId);
            if (qs != null && qs.getStatus() == QuestStatus.START) {
                int var = qs.getQuestVarById(0);
                if (var == 1) {
					changeQuestStep(env, 1, 2, false);
                    return true;
                }
            }
        }
        return false;
    }
}
