package com.aionemu.gameserver.quest.handlers.altgard;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 奥特加德任务脚本：A Fertile Field（任务 ID 2237）。
 * Altgard quest script: A Fertile Field (quest ID 2237).
 *
 * @author Mr.Poke
 * @reworked vlog
 */
public class _2237AFertileField extends QuestHandler {

	private final static int questId = 2237;
	public _2237AFertileField() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(832822).addOnQuestStart(questId);
		qe.registerQuestNpc(832822).addOnTalkEvent(questId);
		qe.registerQuestNpc(700145).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 832822) { // Anmurnerk
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				}
				else {
					return sendQuestStartDialog(env);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 700145: { // Fertilizer Sack
					if (env.getDialog() == QuestDialog.USE_OBJECT) {
						return true; // loot
					}
					break;
				}
				case 832822: { // Anmurnerk
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2375);
						}
						case CHECK_COLLECTED_ITEMS_SIMPLE: {
							return checkQuestItems(env, 0, 0, true, 5, 0);
						}
					}
				}
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 832822) { // Anmurnerk
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
