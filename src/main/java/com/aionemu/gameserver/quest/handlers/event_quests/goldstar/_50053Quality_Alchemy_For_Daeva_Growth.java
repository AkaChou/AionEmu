package com.aionemu.gameserver.quest.handlers.event_quests.goldstar;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 活动任务脚本：Quality Alchemy For Daeva Growth（任务 ID 50053）。
 * Event quest script: Quality Alchemy For Daeva Growth (quest ID 50053).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _50053Quality_Alchemy_For_Daeva_Growth extends QuestHandler
{
	private static final int questId = 50053;
	
	public _50053Quality_Alchemy_For_Daeva_Growth() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(833982).addOnQuestStart(questId);
		qe.registerQuestNpc(833983).addOnQuestStart(questId);
		qe.registerQuestNpc(833982).addOnTalkEvent(questId);
		qe.registerQuestNpc(833983).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 833982 || targetId == 833983) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					} case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE: {
						return sendQuestStartDialog(env);
					} case REFUSE_QUEST_SIMPLE: {
				        return closeDialogWindow(env);
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 833982 || targetId == 833983) {
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
			if (targetId == 833982 || targetId == 833983) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
