package com.aionemu.gameserver.quest.handlers.inggison;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 因吉森任务脚本：Making Setzkiki Laugh（任务 ID 11118）。
 * Inggison quest script: Making Setzkiki Laugh (quest ID 11118).
 *
 * @author Leunam
 */
public class _11118MakingSetzkikiLaugh extends QuestHandler {

	private final static int questId = 11118;
	private final static int[] npc_ids = {798985, 798963, 798986};
	public _11118MakingSetzkikiLaugh() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(798985).addOnQuestStart(questId);
		for (int npc_id : npc_ids)
			qe.registerQuestNpc(npc_id).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
		    if (targetId == 798985) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 4762);
				else
					return sendQuestStartDialog(env);
			}
		}
		if (qs == null)
			return false;
		else if (qs.getStatus() == QuestStatus.START) {
		int var = qs.getQuestVarById(0);
		if (targetId == 798963) {
			switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 0)
						return sendQuestDialog(env, 1011);
					else if (var == 1)
						return sendQuestDialog(env, 1352);
				case STEP_TO_1:
					if (var == 0) {
						qs.setQuestVarById(0, var + 1);
						updateQuestStatus(env);
				        return closeDialogWindow(env);
					}
				case CHECK_COLLECTED_ITEMS:
					if (var == 1) {
						if (player.getInventory().getItemCountByItemId(182206794) >= 20) {
							removeQuestItem(env, 182206794, 20);
							giveQuestItem(env, 182206795, 1);
							qs.setQuestVarById(0, var + 1);
							updateQuestStatus(env);
							return sendQuestDialog(env, 10000);
						}
						else
							return sendQuestDialog(env, 10001);
					}
					return false;
			}
		}
		else if (targetId == 798984) {
			switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 2)
						return sendQuestDialog(env, 2034);
				case SET_REWARD:
					if (var == 2) {
						qs.setQuestVarById(0, var + 1);
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
				        return closeDialogWindow(env);
					}
					return false;
                }
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798985) {
				if (env.getDialog() == QuestDialog.USE_OBJECT)
					return sendQuestDialog(env, 10002);
				else if (env.getDialogId() == 1009)
					return sendQuestDialog(env, 5);
				else
					return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
