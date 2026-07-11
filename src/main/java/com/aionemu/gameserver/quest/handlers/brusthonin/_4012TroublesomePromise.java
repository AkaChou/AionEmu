package com.aionemu.gameserver.quest.handlers.brusthonin;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 布鲁斯特霍宁任务脚本：Troublesome Promise（任务 ID 4012）。
 * Brusthonin quest script: Troublesome Promise (quest ID 4012).
 *
 * @author VladimirZ
 */
public class _4012TroublesomePromise extends QuestHandler {

	private final static int questId = 4012;
	public _4012TroublesomePromise() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(205142).addOnQuestStart(questId);
		qe.registerQuestNpc(205142).addOnTalkEvent(questId);
		qe.registerQuestNpc(730104).addOnTalkEvent(questId);
		qe.registerQuestNpc(700342).addOnTalkEvent(questId);
		qe.addHandlerSideQuestDrop(questId, 700342, 182209005, 1, 100);
	}

	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 205142) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 700342: {
					if (qs.getQuestVarById(0) == 0 && env.getDialog() == QuestDialog.USE_OBJECT) {
						return true; // loot
					}
					break;
				}
				case 730104: {
					if (env.getDialog() == QuestDialog.START_DIALOG)
						return sendQuestDialog(env, 1352);
					else if (env.getDialogId() == 1009) {
						removeQuestItem(env, 182209005, 1);
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return sendQuestDialog(env, 5);
					}
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 730104)
				return sendQuestEndDialog(env);
		}
		return false;
	}
}
