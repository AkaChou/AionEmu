package com.aionemu.gameserver.quest.handlers.beluslan;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 贝勒斯兰任务脚本：Acquiringan Antidote（任务 ID 2663）。
 * Beluslan quest script: Acquiringan Antidote (quest ID 2663).
 *
 * @author VladimirZ
 */
public class _2663AcquiringanAntidote extends QuestHandler {

	private final static int questId = 2663;
	public _2663AcquiringanAntidote() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204777).addOnQuestStart(questId);
		qe.registerQuestNpc(204777).addOnTalkEvent(questId);
		qe.registerQuestNpc(204790).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
		    if (targetId == 204777) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
		if (targetId == 204790) {
			switch (env.getDialog()) {
				case START_DIALOG:
					return sendQuestDialog(env, 1352);
				case STEP_TO_1:
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					giveQuestItem(env, 182204489, 1);
					updateQuestStatus(env);
					return closeDialogWindow(env);
                }
			}
		if (targetId == 204777) {
			switch (env.getDialog()) {
				case START_DIALOG:
					return sendQuestDialog(env, 2375);
				case SELECT_REWARD:
					removeQuestItem(env, 182204489, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				    return sendQuestEndDialog(env);
                }
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204777) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
