package com.aionemu.gameserver.quest.handlers.trials_of_eternity;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 永恒试炼任务脚本：Rift Monster Bullion（任务 ID 26839）。
 * Trials of Eternity quest script: Rift Monster Bullion (quest ID 26839).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _26839Rift_Monster_Bullion extends QuestHandler {

    private final static int questId = 26839;
    public _26839Rift_Monster_Bullion() {
        super(questId);
    }
	
	@Override
	public void register() {
        qe.registerQuestNpc(806578).addOnTalkEvent(questId);
        qe.registerQuestNpc(806572).addOnTalkEvent(questId);
		qe.registerQuestNpc(246440).addOnKillEvent(questId);
		qe.registerQuestNpc(806572).addOnAtDistanceEvent(questId);
		qe.registerOnEnterZone(ZoneName.get("IDETERNITY_03_Q16836_G_301560000"), questId);
	}
	
	@Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 806578) { //Peregrine.
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
	public boolean onAtDistanceEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			QuestService.startQuest(env);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onKillEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (var == 1) {
				if (env.getTargetId() == 246440) {
					changeQuestStep(env, 1, 2, true);
					return true;
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
			if (zoneName == ZoneName.get("IDETERNITY_03_Q16836_G_301560000")) {
				if (var == 0) {
					changeQuestStep(env, 0, 1, false);
					return true;
				}
			}
		}
		return false;
	}
}
