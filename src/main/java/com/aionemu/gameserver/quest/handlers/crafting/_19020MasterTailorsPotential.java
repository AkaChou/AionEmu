package com.aionemu.gameserver.quest.handlers.crafting;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 制作任务脚本：Master Tailors Potential（任务 ID 19020）。
 * Crafting quest script: Master Tailors Potential (quest ID 19020).
 *
 * @author Thuatan
 */
public class _19020MasterTailorsPotential extends QuestHandler {

	private final static int questId = 19020;
	public _19020MasterTailorsPotential() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203793).addOnQuestStart(questId);
		qe.registerQuestNpc(203793).addOnTalkEvent(questId);
		qe.registerQuestNpc(203794).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 203793) {
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
				case 203794:
					switch (env.getDialog()) {
						case START_DIALOG:
							return sendQuestDialog(env, 1011);
						case STEP_TO_10:
							giveQuestItem(env, 152201962, 1);
							qs.setQuestVarById(0, 1);
							updateQuestStatus(env);
					        return closeDialogWindow(env);
						case STEP_TO_20:
							giveQuestItem(env, 152201963, 1);
							qs.setQuestVarById(0, 1);
							updateQuestStatus(env);
					        return closeDialogWindow(env);
					}
				case 203793:
					switch (env.getDialog()) {
						case START_DIALOG:
							long itemCount1 = player.getInventory().getItemCountByItemId(182206766);
							if (itemCount1 > 0) {
								removeQuestItem(env, 182206766, 1);
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
			if (targetId == 203793) {
				if (env.getDialogId() == 39)
					return sendQuestDialog(env, 5);
				else
					return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
