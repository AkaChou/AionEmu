package com.aionemu.gameserver.quest.handlers.iluma;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 伊卢玛任务脚本：Golem Blueprints（任务 ID 15523）。
 * Iluma quest script: Golem Blueprints (quest ID 15523).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _15523Golem_Blueprints extends QuestHandler {

	private static final int questId = 15523;
	public _15523Golem_Blueprints() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(806096).addOnQuestStart(questId);
		qe.registerQuestNpc(806096).addOnTalkEvent(questId);
		qe.registerQuestNpc(703070).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 806096) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		} else if (targetId == 703070) {
			if (env.getDialog() == QuestDialog.USE_OBJECT) {
				closeDialogWindow(env);
				return true;
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.START) {
			if (targetId == 806096) {
				switch (env.getDialog()) {
					case START_DIALOG: {
                        return sendQuestDialog(env, 1011);
					} case CHECK_COLLECTED_ITEMS: {
						if (QuestService.collectItemCheck(env, true)) {
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							return sendQuestDialog(env, 10000);
						} else {
							return sendQuestDialog(env, 10001);
						}
					}
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 806096) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
