package com.aionemu.gameserver.quest.handlers.reshanta;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 雷山塔任务脚本：Eliminate Elysean Officers（任务 ID 2886）。
 * Reshanta quest script: Eliminate Elysean Officers (quest ID 2886).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _2886Eliminate_Elysean_Officers extends QuestHandler {

    private final static int questId = 2886;
    public _2886Eliminate_Elysean_Officers() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerQuestNpc(278001).addOnTalkEvent(questId); //Votan.
		qe.registerOnKillRanked(AbyssRankEnum.STAR1_OFFICER, questId);
		qe.registerOnEnterZone(ZoneName.get("PRIMUM_LANDING_400010000"), questId);
    }
	
	@Override
    public boolean onKillRankedEvent(QuestEnv env) {
        return defaultOnKillRankedEvent(env, 0, 3, true);
    }
	
    @Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (env.getTargetId() == 278001) { //Votan.
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
		if (zoneName == ZoneName.get("PRIMUM_LANDING_400010000")) {
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
