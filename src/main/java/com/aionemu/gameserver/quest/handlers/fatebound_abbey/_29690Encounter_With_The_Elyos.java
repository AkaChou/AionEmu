package com.aionemu.gameserver.quest.handlers.fatebound_abbey;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 命运修道院任务脚本：Encounter With The Elyos（任务 ID 29690）。
 * Fatebound Abbey quest script: Encounter With The Elyos (quest ID 29690).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _29690Encounter_With_The_Elyos extends QuestHandler {

	private final static int questId = 29690;
	public _29690Encounter_With_The_Elyos() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerOnKillInWorld(0, questId);
		qe.registerQuestNpc(806700).addOnQuestStart(questId);
		qe.registerQuestNpc(806700).addOnTalkEvent(questId);
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
		int targetId = env.getTargetId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
		   if (env.getTargetId() == 806700) {
			    switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE: {
						return sendQuestStartDialog(env);
					}
				}
			} 
            if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
				if (targetId == 806700) {
				    if (env.getDialog() == QuestDialog.START_DIALOG) {
						return sendQuestDialog(env, 10002);
					} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
						return sendQuestDialog(env, 5);
					} else {
						return sendQuestEndDialog(env);
					}
				}
			}
		}
		return false;
	}
}
