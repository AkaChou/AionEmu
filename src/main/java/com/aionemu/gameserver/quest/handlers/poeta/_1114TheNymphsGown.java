package com.aionemu.gameserver.quest.handlers.poeta;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 波伊塔任务脚本：The Nymphs Gown（任务 ID 1114）。
 * Poeta quest script: The Nymphs Gown (quest ID 1114).
 *
 * @author Rhys2002
 */
public class _1114TheNymphsGown extends QuestHandler {

	private final static int questId = 1114;
	private final static int[] npc_ids = { 203075, 203058, 700008 };
	public _1114TheNymphsGown() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestItem(182200214, questId);
		for (int npc_id : npc_ids)
			qe.registerQuestNpc(npc_id).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 0) { 
				if (env.getDialog() == QuestDialog.ACCEPT_QUEST) {
					giveQuestItem(env, 182200226, 1);
					removeQuestItem(env, 182200214, 1); // Namus's Diary with double-click to start the quest
					return sendQuestStartDialog(env);
				}
				if (env.getDialog() == QuestDialog.REFUSE_QUEST) {
					return closeDialogWindow(env);
				}
			}
		}
		if (qs == null)
			return false;
		int var = qs.getQuestVarById(0);
		if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203075 && var == 4) { // Namus
				if (env.getDialog() == QuestDialog.USE_OBJECT)
					return sendQuestDialog(env, 2375);
				else if (env.getDialogId() == 1009)
					return sendQuestDialog(env, 6);
				else
					return sendQuestEndDialog(env);
			}
			else if (targetId == 203058 && var == 3) // Asteros
				if (env.getDialog() == QuestDialog.USE_OBJECT)
					return sendQuestDialog(env, 5);
				else
					return sendQuestEndDialog(env);
		}
		else if (qs.getStatus() != QuestStatus.START)
			return false;
		if (targetId == 203075) { // Namus
			switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 0)
						return sendQuestDialog(env, 1011);
					else if (var == 2)
						return sendQuestDialog(env, 1693);
					else if (var == 3)
						return sendQuestDialog(env, 2375);
				case SELECT_REWARD:
					if (var == 2) {
						qs.setQuestVarById(0, var + 2);
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						removeQuestItem(env, 182200217, 1);
						return sendQuestDialog(env, 6);
					}
					if (var == 3) {
						qs.setQuestVarById(0, var + 1);
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						removeQuestItem(env, 182200217, 1);
						return sendQuestDialog(env, 6);
					}
				case STEP_TO_1:
					if (var == 0) {
						qs.setQuestVarById(0, var + 1);
						updateQuestStatus(env);
						removeQuestItem(env, 182200226, 1);
					    return closeDialogWindow(env);
					}
				case STEP_TO_2:
					if (var == 2) {
						qs.setQuestVarById(0, var + 1);
						updateQuestStatus(env);
					    return closeDialogWindow(env);
					}
			}
		}
		else if (targetId == 700008) { // Seirenia's clothes
			switch (env.getDialog()) {
				case USE_OBJECT:
					if (var == 1) {
						for (VisibleObject obj : player.getKnownList().getKnownObjectsSnapshot()) {
							if (!(obj instanceof Npc))
								continue;
							if (((Npc) obj).getNpcId() != 203175) // Seirenia
								continue;
							((Npc) obj).getAggroList().addDamage(player, 50);
						}
						giveQuestItem(env, 182200217, 1); // Nymph's Dress
						qs.setQuestVarById(0, 2);
						updateQuestStatus(env);
					}
					return true;
			}
		}
		if (targetId == 203058) {// Asteros
			switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 3)
						return sendQuestDialog(env, 2034);
				case STEP_TO_3:
					if (var == 3) {
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						removeQuestItem(env, 182200217, 1);
						return sendQuestDialog(env, 5);
					}
				case STEP_TO_2:
					if (var == 3) {
					    return closeDialogWindow(env);
					}
			}
		}
		return false;
	}

	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			return HandlerResult.fromBoolean(sendQuestDialog(env, 4));
		}
		return HandlerResult.FAILED;
	}
}
