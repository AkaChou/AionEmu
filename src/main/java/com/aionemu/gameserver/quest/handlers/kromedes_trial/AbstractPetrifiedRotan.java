package com.aionemu.gameserver.quest.handlers.kromedes_trial;

import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.world.zone.ZoneName;

abstract class AbstractPetrifiedRotan extends QuestHandler {

	static final String ZONE_NAME = "IDCROMEDE_SENSORYAREA_Q18604_300230000";
	static final int STATUE_ID = 730309;

	protected AbstractPetrifiedRotan(int questId) {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerOnEnterZone(ZoneName.get(ZONE_NAME), getQuestId());
		qe.registerQuestNpc(STATUE_ID).addOnTalkEvent(getQuestId());
	}

	@Override
	public boolean onEnterZoneEvent(QuestEnv env, ZoneName zoneName) {
		return defaultOnEnterZoneEvent(env, zoneName, ZoneName.get(ZONE_NAME));
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		if (env.getTargetId() != STATUE_ID) {
			return false;
		}
		QuestState state = env.getPlayer().getQuestStateList().getQuestState(getQuestId());
		if (state == null || state.getStatus() != QuestStatus.START && state.getStatus() != QuestStatus.REWARD) {
			return false;
		}
		if (env.getDialog() == QuestDialog.USE_OBJECT) {
			return sendQuestDialog(env, QuestDialog.STEP_TO_3.id());
		}
		if (env.getDialog() != QuestDialog.STEP_TO_3) {
			return false;
		}
		if (state.getStatus() == QuestStatus.START) {
			state.setStatus(QuestStatus.REWARD);
		}
		return QuestService.finishQuest(env) && closeDialogWindow(env);
	}
}
