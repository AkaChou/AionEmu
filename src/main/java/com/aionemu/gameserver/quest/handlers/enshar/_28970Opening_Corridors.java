package com.aionemu.gameserver.quest.handlers.enshar;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 恩沙尔任务脚本：Opening Corridors（任务 ID 28970）。
 * Enshar quest script: Opening Corridors (quest ID 28970).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _28970Opening_Corridors extends QuestHandler {

	private final static int questId = 28970;
	public _28970Opening_Corridors() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(804927).addOnQuestStart(questId);
		qe.registerQuestNpc(804927).addOnTalkEvent(questId);
		qe.registerQuestNpc(804924).addOnTalkEvent(questId);
		qe.registerQuestNpc(805218).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804927) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} if (qs == null)
			return false;
		if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 804924: {
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1011);
						} case SET_REWARD: {
							qs.setQuestVar(1);
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							return closeDialogWindow(env);
					}   }
				}
			}	
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805218) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 10002);
					}
					case SELECT_REWARD: {
						return sendQuestDialog(env, 5);
					}
					default:
						return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}
