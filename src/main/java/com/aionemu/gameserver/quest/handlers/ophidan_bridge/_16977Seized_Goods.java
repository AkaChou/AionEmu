package com.aionemu.gameserver.quest.handlers.ophidan_bridge;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 奥菲丹桥任务脚本：Seized Goods（任务 ID 16977）。
 * Ophidan Bridge quest script: Seized Goods (quest ID 16977).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _16977Seized_Goods extends QuestHandler {

	private static final int questId = 16977;
	public _16977Seized_Goods() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(801763).addOnQuestStart(questId);
		qe.registerQuestNpc(801763).addOnTalkEvent(questId);
		qe.registerQuestNpc(801762).addOnTalkEvent(questId);
		qe.registerQuestNpc(702959).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 801763) {
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
		} else if (targetId == 702959) {
			if (env.getDialog() == QuestDialog.USE_OBJECT) {
				closeDialogWindow(env);
				return true;
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 801762) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					} case CHECK_COLLECTED_ITEMS: {
						return checkQuestItems(env, 0, 0, true, 5, 2716);
					}
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 801762) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
