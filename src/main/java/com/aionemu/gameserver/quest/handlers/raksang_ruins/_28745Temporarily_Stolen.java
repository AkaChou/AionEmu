package com.aionemu.gameserver.quest.handlers.raksang_ruins;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 拉克桑遗迹任务脚本：Temporarily Stolen（任务 ID 28745）。
 * Raksang Ruins quest script: Temporarily Stolen (quest ID 28745).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _28745Temporarily_Stolen extends QuestHandler {

	private static final int questId = 28745;
	public _28745Temporarily_Stolen() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(206395).addOnQuestStart(questId);
		qe.registerQuestNpc(206396).addOnQuestStart(questId);
		qe.registerQuestNpc(206397).addOnQuestStart(questId);
		qe.registerQuestNpc(804732).addOnTalkEvent(questId);
		qe.registerQuestNpc(702958).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 206395 || targetId == 206396 || targetId == 206397) {
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
		} else if (targetId == 702958) {
			if (dialog == QuestDialog.USE_OBJECT) {
				return true;
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 804732) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					} case CHECK_COLLECTED_ITEMS: {
						return checkQuestItems(env, 0, 0, true, 5, 10001);
					}
				}
			}
		} else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804732) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
