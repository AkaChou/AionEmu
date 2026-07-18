package com.aionemu.gameserver.quest.handlers.reshanta;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

abstract class AbstractReshantaEmergencyPvp extends QuestHandler {

	private static final int RESHANTA = 400010000;
	static final int REQUIRED_KILLS = 5;
	private final int rewardNpcId;

	protected AbstractReshantaEmergencyPvp(int questId, int rewardNpcId) {
		super(questId);
		this.rewardNpcId = rewardNpcId;
	}

	@Override
	public void register() {
		qe.registerQuestNpc(rewardNpcId).addOnTalkEvent(getQuestId());
		qe.registerOnKillInWorld(RESHANTA, getQuestId());
		qe.registerOnEnterWorld(getQuestId());
	}

	@Override
	public boolean onEnterWorldEvent(QuestEnv env) {
		Player player = env.getPlayer();
		if (player.getWorldId() != RESHANTA) {
			return false;
		}
		QuestState state = player.getQuestStateList().getQuestState(getQuestId());
		return (state == null || state.getStatus() == QuestStatus.NONE || state.canRepeat())
			&& QuestService.startQuest(env);
	}

	@Override
	public boolean onKillInWorldEvent(QuestEnv env) {
		return env.getVisibleObject() instanceof Player && defaultOnKillRankedEvent(env, 0, REQUIRED_KILLS, true);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		QuestState state = env.getPlayer().getQuestStateList().getQuestState(getQuestId());
		if (state == null || state.getStatus() != QuestStatus.REWARD || env.getTargetId() != rewardNpcId) {
			return false;
		}
		if (env.getDialog() == QuestDialog.START_DIALOG) {
			return sendQuestDialog(env, 10002);
		}
		if (env.getDialog() == QuestDialog.SELECT_REWARD) {
			return sendQuestDialog(env, 5);
		}
		return sendQuestEndDialog(env);
	}
}
