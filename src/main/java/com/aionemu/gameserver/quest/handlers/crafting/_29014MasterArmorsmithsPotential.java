package com.aionemu.gameserver.quest.handlers.crafting;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 制作任务脚本：Master Armorsmiths Potential（任务 ID 29014）。
 * Crafting quest script: Master Armorsmiths Potential (quest ID 29014).
 *
 * @author Thuatan
 */
public class _29014MasterArmorsmithsPotential extends QuestHandler {

	private final static int questId = 29014;
	public _29014MasterArmorsmithsPotential() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204106).addOnQuestStart(questId);
		qe.registerQuestNpc(204106).addOnTalkEvent(questId);
		qe.registerQuestNpc(204107).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204106) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 4762);
				else
					return sendQuestStartDialog(env);
			}
		}
		if (qs == null)
			return false;
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 204107:
					switch (env.getDialog()) {
						case START_DIALOG:
							return sendQuestDialog(env, 1011);
						case STEP_TO_10:
							giveQuestItem(env, 152206808, 1);
							qs.setQuestVarById(0, 1);
							updateQuestStatus(env);
					        return closeDialogWindow(env);
						case STEP_TO_20:
							giveQuestItem(env, 152206809, 1);
							qs.setQuestVarById(0, 1);
							updateQuestStatus(env);
					        return closeDialogWindow(env);
					}
				case 204106:
					switch (env.getDialog()) {
						case START_DIALOG:
							long itemCount1 = player.getInventory().getItemCountByItemId(182207899);
							if (itemCount1 > 0) {
								removeQuestItem(env, 182207899, 1);
								qs.setStatus(QuestStatus.REWARD);
								updateQuestStatus(env);
								return sendQuestDialog(env, 1352);
							}
							else
								return sendQuestDialog(env, 10001);
					}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204106) {
				if (env.getDialogId() == 39)
					return sendQuestDialog(env, 5);
				else
					return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
