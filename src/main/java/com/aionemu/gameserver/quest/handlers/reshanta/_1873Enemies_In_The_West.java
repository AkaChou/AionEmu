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
 * 雷山塔任务脚本：Enemies In The West（任务 ID 1873）。
 * Reshanta quest script: Enemies In The West (quest ID 1873).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _1873Enemies_In_The_West extends QuestHandler {

    private final static int questId = 1873;
    public _1873Enemies_In_The_West() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerQuestNpc(278502).addOnTalkEvent(questId); //Sakmis.
		qe.registerOnKillInWorld(400010000, questId);
		qe.registerOnEnterZone(ZoneName.get("SIEL_WESTERN_FORTRESS_400010000"), questId);
    }
	
	@Override
	public boolean onKillInWorldEvent(QuestEnv env) {
		Player player = env.getPlayer();
		if (env.getVisibleObject() instanceof Player && player != null && player.isInsideZone(ZoneName.get("SIEL_WESTERN_FORTRESS_400010000"))) {
			if ((env.getPlayer().getLevel() >= (((Player)env.getVisibleObject()).getLevel() - 5)) &&
			    (env.getPlayer().getLevel() <= (((Player)env.getVisibleObject()).getLevel() + 9))) {
				return defaultOnKillRankedEvent(env, 0, 3, true);
			}
		}
		return false;
	}
	
    @Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = env.getTargetId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (env.getTargetId() == 278502) { //Sakmis.
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
