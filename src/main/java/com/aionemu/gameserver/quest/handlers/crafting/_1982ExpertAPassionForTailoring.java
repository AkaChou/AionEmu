package com.aionemu.gameserver.quest.handlers.crafting;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 制作任务脚本：Expert A Passion For Tailoring（任务 ID 1982）。
 * Crafting quest script: Expert A Passion For Tailoring (quest ID 1982).
 *
 * @author Gigi
 */
public class _1982ExpertAPassionForTailoring extends QuestHandler {

	private final static int questId = 1982;
	public _1982ExpertAPassionForTailoring() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203793).addOnQuestStart(questId);
		qe.registerQuestNpc(203793).addOnTalkEvent(questId);
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
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
		}
		if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 203793: {
					switch (env.getDialog()) {
						case START_DIALOG: {
							long itemCount1 = player.getInventory().getItemCountByItemId(182206913);
							if (itemCount1 > 0) {
								removeQuestItem(env, 182206913, 1);
								qs.setStatus(QuestStatus.REWARD);
								updateQuestStatus(env);
								return sendQuestDialog(env, 2375);
							}
							else
								return sendQuestDialog(env, 2716);
						}
					}
				}
			}
		}
		else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
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
