package com.aionemu.gameserver.quest.handlers.oriel;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 奥里尔任务脚本：Blessed Be Thy Hame（任务 ID 18807）。
 * Oriel quest script: Blessed Be Thy Hame (quest ID 18807).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _18807Blessed_Be_Thy_Hame extends QuestHandler
{
	private static final int questId = 18807;
	
	public _18807Blessed_Be_Thy_Hame() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(830194).addOnQuestStart(questId);
		qe.registerQuestNpc(830194).addOnTalkEvent(questId);
		qe.registerQuestNpc(730524).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestDialog dialog = env.getDialog();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 830194) {
				switch (dialog) {
					case START_DIALOG:
						playQuestMovie(env, 803);
						return sendQuestDialog(env, 1011);
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 730524: {
					switch (dialog) {
						case START_DIALOG:
							return sendQuestDialog(env, 1352);
						case SELECT_ACTION_1353:
							return sendQuestDialog(env, 1353);
						case STEP_TO_1:
							return defaultCloseDialog(env, 0, 1);
					}
					break;
				} case 830194: {
					switch (dialog) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2375);
						} case SELECT_REWARD:
							changeQuestStep(env, 1, 1, true);
							return sendQuestDialog(env, 5);
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 830194) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
