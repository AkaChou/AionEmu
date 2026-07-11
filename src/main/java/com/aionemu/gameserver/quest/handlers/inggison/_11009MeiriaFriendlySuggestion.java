package com.aionemu.gameserver.quest.handlers.inggison;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 因吉森任务脚本：Meiria Friendly Suggestion（任务 ID 11009）。
 * Inggison quest script: Meiria Friendly Suggestion (quest ID 11009).
 *
 * @author dta3000
 */
public class _11009MeiriaFriendlySuggestion extends QuestHandler {

	private final static int questId = 11009;
	public _11009MeiriaFriendlySuggestion() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(798945).addOnQuestStart(questId);
		qe.registerQuestNpc(798945).addOnTalkEvent(questId);
		qe.registerQuestNpc(799008).addOnTalkEvent(questId);
		qe.registerQuestNpc(799017).addOnTalkEvent(questId);
		qe.registerQuestNpc(798941).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
		    if (targetId == 798945) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				}
				else if (env.getDialogId() == 1007) {
					return sendQuestDialog(env, 4);
				}
				else if (env.getDialogId() == 1002) {
					return sendQuestStartDialog(env, 182206711, 2);
				}
			}
		}
		if (qs == null)
			return false;
		if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 799008: {
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1352);
						}
						case STEP_TO_1: {
							removeQuestItem(env, 182206711, 1);
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
				            return closeDialogWindow(env);
						}
					}
				}
				case 799017: {
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1693);
						}
						case STEP_TO_2: {
							removeQuestItem(env, 182206711, 1);
							giveQuestItem(env, 182206712, 1);
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
				            return closeDialogWindow(env);
						}
					}
				}
				case 798941: {
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2034);
						}
						case STEP_TO_3: {
							removeQuestItem(env, 182206712, 1);
							qs.setQuestVar(3);
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
				            return closeDialogWindow(env);
						}
					}
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798945) {
				if (env.getDialog() == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 2375);
				}
				else
					return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
