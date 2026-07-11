package com.aionemu.gameserver.quest.handlers.reshanta;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 雷山塔任务脚本：Quick While They Distracted（任务 ID 23843）。
 * Reshanta quest script: Quick While They Distracted (quest ID 23843).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23843Quick_While_They_Distracted extends QuestHandler {

    private final static int questId = 23843;
    public _23843Quick_While_They_Distracted() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerQuestNpc(278019).addOnTalkEvent(questId); //Lakadi.
		qe.registerOnKillInWorld(400010000, questId);
		qe.registerOnEnterZone(ZoneName.get("SIEL_WESTERN_FORTRESS_400010000"), questId);
    }
	
	@Override
	public boolean onKillInWorldEvent(QuestEnv env) {
		Player player = env.getPlayer();
		if (env.getVisibleObject() instanceof Player && player != null && player.isInsideZone(ZoneName.get("SIEL_WESTERN_FORTRESS_400010000"))) {
			if ((env.getPlayer().getLevel() >= (((Player)env.getVisibleObject()).getLevel() - 5)) &&
			    (env.getPlayer().getLevel() <= (((Player)env.getVisibleObject()).getLevel() + 9))) {
				return defaultOnKillRankedEvent(env, 0, 6, true);
			}
		}
		return false;
	}
	
    @Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (env.getTargetId() == 278019) { //Lakadi.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 10002);
				} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
            }
        }
		return false;
	}
	
    @Override
	public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
		if (zoneName == ZoneName.get("SIEL_WESTERN_FORTRESS_400010000")) {
			Player player = env.getPlayer();
			if (player == null) {
				return false;
			}
			QuestState qs = player.getQuestStateList().getQuestState(questId);
			if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
				QuestService.startQuest(env);
				return true;
			}
		}
		return false;
	}
}
