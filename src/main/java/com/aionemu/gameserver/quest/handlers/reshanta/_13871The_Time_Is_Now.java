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
 * 雷山塔任务脚本：The Time Is Now（任务 ID 13871）。
 * Reshanta quest script: The Time Is Now (quest ID 13871).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _13871The_Time_Is_Now extends QuestHandler {

    private final static int questId = 13871;
    public _13871The_Time_Is_Now() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerQuestNpc(805380).addOnTalkEvent(questId); //Lagranjia.
		qe.registerOnKillInWorld(400010000, questId);
		qe.registerOnEnterZone(ZoneName.get("KYSIS_FORTRESS_400010000"), questId);
    }
	
	@Override
	public boolean onKillInWorldEvent(QuestEnv env) {
		Player player = env.getPlayer();
		if (env.getVisibleObject() instanceof Player && player != null && player.isInsideZone(ZoneName.get("KYSIS_FORTRESS_400010000"))) {
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
            if (env.getTargetId() == 805380) { //Lagranjia.
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
		if (zoneName == ZoneName.get("KYSIS_FORTRESS_400010000")) {
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
