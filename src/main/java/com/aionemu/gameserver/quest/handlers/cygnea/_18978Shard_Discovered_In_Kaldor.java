package com.aionemu.gameserver.quest.handlers.cygnea;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 希格尼娅任务脚本：Shard Discovered In Kaldor（任务 ID 18978）。
 * Cygnea quest script: Shard Discovered In Kaldor (quest ID 18978).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _18978Shard_Discovered_In_Kaldor extends QuestHandler {
	
	private static final int questId = 18978;
	public _18978Shard_Discovered_In_Kaldor() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(802431).addOnQuestStart(questId);
		qe.registerQuestNpc(805215).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 802431) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 805215) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					} case CHECK_COLLECTED_ITEMS: {
					    return checkQuestItems(env, 0, 0, true, 5, 10001);
					}
				}
			}
		} else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805215) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
