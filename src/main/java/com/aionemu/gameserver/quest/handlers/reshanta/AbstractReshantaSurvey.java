package com.aionemu.gameserver.quest.handlers.reshanta;

import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.zone.ZoneName;

abstract class AbstractReshantaSurvey extends QuestHandler {

	private final int startNpcId;
	private final int surveyNpcId;
	private final ZoneName[] route;

	protected AbstractReshantaSurvey(int questId, int startNpcId, int surveyNpcId, String... route) {
		super(questId);
		this.startNpcId = startNpcId;
		this.surveyNpcId = surveyNpcId;
		this.route = new ZoneName[route.length];
		for (int index = 0; index < route.length; index++) {
			this.route[index] = ZoneName.get(route[index]);
		}
	}

	@Override
	public void register() {
		qe.registerQuestNpc(startNpcId).addOnQuestStart(getQuestId());
		qe.registerQuestNpc(startNpcId).addOnTalkEvent(getQuestId());
		qe.registerQuestNpc(surveyNpcId).addOnTalkEvent(getQuestId());
		for (ZoneName zoneName : route) {
			qe.registerOnEnterZone(zoneName, getQuestId());
		}
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		QuestState state = env.getPlayer().getQuestStateList().getQuestState(getQuestId());
		if (state == null || state.getStatus() == QuestStatus.NONE || state.canRepeat()) {
			if (env.getTargetId() != startNpcId) {
				return false;
			}
			return env.getDialog() == QuestDialog.START_DIALOG
				? sendQuestDialog(env, 1011)
				: sendQuestStartDialog(env);
		}
		if (state.getStatus() == QuestStatus.START && state.getQuestVarById(0) == 0
			&& env.getTargetId() == surveyNpcId) {
			if (env.getDialog() == QuestDialog.START_DIALOG) {
				return sendQuestDialog(env, 1352);
			}
			if (env.getDialog() == QuestDialog.STEP_TO_1) {
				changeQuestStep(env, 0, 1, false);
				return closeDialogWindow(env);
			}
		}
		if (state.getStatus() == QuestStatus.REWARD && env.getTargetId() == startNpcId) {
			if (env.getDialog() == QuestDialog.START_DIALOG) {
				return sendQuestDialog(env, 10002);
			}
			if (env.getDialog() == QuestDialog.SELECT_REWARD) {
				return sendQuestDialog(env, 5);
			}
			return sendQuestEndDialog(env);
		}
		return false;
	}

	@Override
	public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
		QuestState state = env.getPlayer().getQuestStateList().getQuestState(getQuestId());
		if (state == null || state.getStatus() != QuestStatus.START) {
			return false;
		}
		int step = state.getQuestVarById(0);
		int next = nextStep(route, step, zoneName);
		if (next < 0) {
			return false;
		}
		changeQuestStep(env, step, next, next == route.length + 1);
		return true;
	}

	static int nextStep(ZoneName[] route, int step, ZoneName zoneName) {
		return step >= 1 && step <= route.length && route[step - 1] == zoneName ? step + 1 : -1;
	}

	ZoneName[] route() {
		return route.clone();
	}
}
