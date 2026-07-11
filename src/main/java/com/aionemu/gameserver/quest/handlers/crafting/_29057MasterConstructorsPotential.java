package com.aionemu.gameserver.quest.handlers.crafting;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 制作任务脚本：Master Constructors Potential（任务 ID 29057）。
 * Crafting quest script: Master Constructors Potential (quest ID 29057).
 *
 * @author Ritsu
 */
public class _29057MasterConstructorsPotential extends QuestHandler {

	private final static int questId = 29057;
	private final static int[]	recipesItemIds = {152208541, 152208542};
	private final static int[]	recipesIds = {155008541, 155008542};
	public _29057MasterConstructorsPotential() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(798452).addOnQuestStart(questId);
		qe.registerQuestNpc(798452).addOnTalkEvent(questId);
		qe.registerQuestNpc(798453).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 798452) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 4762);
				else
					return sendQuestStartDialog(env);
			}
		}
		if (qs == null)
			return false;
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			switch (targetId) {
				case 798453:
					long kinah = player.getInventory().getKinah();
					switch (dialog) {
						case START_DIALOG: {
							switch (var) {
								case 0: 
									return sendQuestDialog(env, 1011);
								case 2: 
									return sendQuestDialog(env, 4080);
							}
						}
						case STEP_TO_10:
							if (kinah >= 167500) { //Need check how many kinah decrased
								giveQuestItem(env, 152208541, 1);
								player.getInventory().decreaseKinah(167500);
								qs.setQuestVarById(0, 1);
								updateQuestStatus(env);
					            return closeDialogWindow(env);
							}							
							else
								return sendQuestDialog(env, 4400);		
						case STEP_TO_20:
							if (kinah >= 223000) {
								giveQuestItem(env, 152208542, 1);
								player.getInventory().decreaseKinah(223000);
								qs.setQuestVarById(0, 1);
								updateQuestStatus(env);
					            return closeDialogWindow(env);
							}
							else
								return sendQuestDialog(env, 4400);								
					}
				case 798452:
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1352);
						}
						case CHECK_COLLECTED_ITEMS:
							if(QuestService.collectItemCheck(env, true)) {
								qs.setStatus(QuestStatus.REWARD);
								updateQuestStatus(env);
								return sendQuestDialog(env, 5);
							}
							else {
								int checkFailId = 3398;
								if(player.getRecipeList().isRecipePresent(recipesIds[0]) || player.getRecipeList().isRecipePresent(recipesIds[1]))
									checkFailId = 2716;
								else if(player.getInventory().getItemCountByItemId(recipesItemIds[0]) > 0 || player.getInventory().getItemCountByItemId(recipesItemIds[1]) > 0)
									checkFailId = 3057;

								if(checkFailId == 3398) {
									qs.setQuestVar(2);
									updateQuestStatus(env);
								}
								return sendQuestDialog(env, checkFailId);
							}
					}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798452)
				return sendQuestEndDialog(env);
		}
		return false;
	}
}
