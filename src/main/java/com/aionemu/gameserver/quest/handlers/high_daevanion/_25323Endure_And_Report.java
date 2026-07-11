package com.aionemu.gameserver.quest.handlers.high_daevanion;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 高等大天使任务脚本：Endure And Report（任务 ID 25323）。
 * High Daevanion quest script: Endure And Report (quest ID 25323).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25323Endure_And_Report extends QuestHandler {

	private static final int questId = 25323;
	public _25323Endure_And_Report() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(805342).addOnQuestStart(questId);
		qe.registerQuestNpc(805342).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
        if (qs == null) {
            return false;
        }
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 805342) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					} case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 805342) {
				switch (dialog) {
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
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805342) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
