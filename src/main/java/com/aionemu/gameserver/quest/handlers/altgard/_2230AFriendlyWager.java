package com.aionemu.gameserver.quest.handlers.altgard;

import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 奥特加德任务脚本：A Friendly Wager（任务 ID 2230）。
 * Altgard quest script: A Friendly Wager (quest ID 2230).
 *
 * @author HellBoy
 */
public class _2230AFriendlyWager extends QuestHandler {

	private final static int questId = 2230;
	public _2230AFriendlyWager() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203621).addOnQuestStart(questId);
		qe.registerQuestNpc(203621).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		QuestState qs = env.getPlayer().getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (env.getTargetId() == 203621) {
				switch (env.getDialog()) {
				case START_DIALOG:
					return sendQuestDialog(env, 1011);
				case ASK_ACCEPTION:
					return sendQuestDialog(env, 4);
				case ACCEPT_QUEST:
					return sendQuestDialog(env, 1003);
				case REFUSE_QUEST:
					return sendQuestDialog(env, 1004);
				case STEP_TO_1:
					if (QuestService.startQuest(env)) {
						QuestService.questTimerStart(env, 1800);
					return sendQuestSelectionDialog(env);
				    }
				    else {
					    return sendQuestStartDialog(env);
				    }
				}
			}
		}
		if (qs == null) {
			return false;
		}
		int var = qs.getQuestVarById(0);
		if (qs.getStatus() == QuestStatus.START) {
			if (env.getTargetId() == 203621) {
				switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 0) {
						return sendQuestDialog(env, 2375);
					}
				case CHECK_COLLECTED_ITEMS:
					if (var == 0) {
						if (QuestService.collectItemCheck(env, true)) {
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							QuestService.questTimerEnd(env);
							return sendQuestDialog(env, 5);
						}
						else {
							return sendQuestDialog(env, 2716);
						}
					}
				}
			}
		}
		return sendQuestRewardDialog(env, 203621, 0);
	}
}
