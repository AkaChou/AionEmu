package com.aionemu.gameserver.quest.handlers.nightmare_circus;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

public class _80341A_Hallowed_Eve extends QuestHandler {
	private static final int QUEST_ID = 80341;
	private static final int NPC_ID = 831709;

	public _80341A_Hallowed_Eve() {
		super(QUEST_ID);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(NPC_ID).addOnQuestStart(QUEST_ID);
		qe.registerQuestNpc(NPC_ID).addOnTalkEvent(QUEST_ID);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(QUEST_ID);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == NPC_ID) {
				switch (dialog) {
				case START_DIALOG:
					return sendQuestDialog(env, 1011);
				case ASK_ACCEPTION:
					return sendQuestDialog(env, 4);
				case ACCEPT_QUEST:
					QuestService.startQuest(env);
					return sendQuestDialog(env, 1003);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == NPC_ID) {
				switch (dialog) {
				case START_DIALOG:
					return sendQuestDialog(env, 2375);
				case SELECT_REWARD:
					changeQuestStep(env, 0, 0, true);
					return sendQuestEndDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD && targetId == NPC_ID) {
			return sendQuestEndDialog(env);
		}
		return false;
	}
}
