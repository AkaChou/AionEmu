package com.aionemu.gameserver.quest.handlers.linkgate_foundry;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 连结之门铸造厂任务脚本：A Jump Ahead（任务 ID 26942）。
 * Linkgate Foundry quest script: A Jump Ahead (quest ID 26942).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _26942A_Jump_Ahead extends QuestHandler {

	public static final int questId = 26942;
	public _26942A_Jump_Ahead() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(802353).addOnQuestStart(questId); //Yasan.
		qe.registerQuestNpc(802353).addOnTalkEvent(questId); //Yasan.
		qe.registerQuestNpc(206362).addOnTalkEvent(questId); //Aitu.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
		   if (targetId == 802353) { //Yasan.
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					} case SELECT_ACTION_4763: {
						return sendQuestDialog(env, 4763);
					} case ACCEPT_QUEST_SIMPLE: {
						return sendQuestStartDialog(env);
					} case REFUSE_QUEST_SIMPLE: {
				        return closeDialogWindow(env);
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 206362) { //Aitu.
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					} case SELECT_ACTION_1012: {
						return sendQuestDialog(env, 1012);
					} case STEP_TO_1: {
						playQuestMovie(env, 900);
						return defaultCloseDialog(env, 0, 1);
					}
				}
			} if (targetId == 802353) { //Yasan.
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1352);
					} case CHECK_COLLECTED_ITEMS: {
						if (QuestService.collectItemCheck(env, true)) {
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							return sendQuestDialog(env, 5);
						} else {
							return sendQuestDialog(env, 10001);
						}
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 802353) { //Yasan.
				removeQuestItem(env, 182215786, 3);
				removeQuestItem(env, 182215788, 1);
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
