package com.aionemu.gameserver.quest.handlers.event_quests.shugo_ranger;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.*;

/**
 * 活动任务脚本（任务 ID 50079）。
 * Event quest script (quest ID 50079).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _50079 extends QuestHandler
{
	private final static int questId = 50079;
	
	public _50079() {
		super(questId);
	}
	
	public void register() {
        qe.registerOnKillInWorld(400010000, questId);
		qe.registerQuestNpc(835578).addOnQuestStart(questId);
        qe.registerQuestNpc(835578).addOnTalkEvent(questId);
    }
	
	@Override
	public boolean onKillInWorldEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		if (env.getVisibleObject() instanceof Player && player != null) {
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
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (env.getTargetId() == 835578) {
			if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env);
				}
			} else if (qs.getStatus() == QuestStatus.REWARD) {
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
}
