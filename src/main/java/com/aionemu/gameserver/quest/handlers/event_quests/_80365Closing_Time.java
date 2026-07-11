package com.aionemu.gameserver.quest.handlers.event_quests;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 活动任务脚本：Closing Time（任务 ID 80365）。
 * Event quest script: Closing Time (quest ID 80365).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _80365Closing_Time extends QuestHandler
{
    private final static int questId = 80365;
	
    public _80365Closing_Time() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(831827).addOnQuestStart(questId);
        qe.registerQuestNpc(831827).addOnTalkEvent(questId);
		qe.registerQuestNpc(831819).addOnTalkEvent(questId);
    }
	
    @Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		QuestDialog dialog = env.getDialog();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 831827) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 831819) {
				switch (env.getDialog()) {
					case START_DIALOG: {
                        return sendQuestDialog(env, 2375);
					} case CHECK_COLLECTED_ITEMS_SIMPLE: {
						if (QuestService.collectItemCheck(env, true)) {
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							return sendQuestDialog(env, 10000);
						} else {
							return sendQuestDialog(env, 10001);
						}
					} case FINISH_DIALOG: {
						return sendQuestSelectionDialog(env);
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 831819) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
