package com.aionemu.gameserver.quest.handlers.altgard;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 奥特加德任务脚本：Malodor Antidote（任务 ID 2239）。
 * Altgard quest script: Malodor Antidote (quest ID 2239).
 *
 * @author Ritsu
 */
public class _2239MalodorAntidote extends QuestHandler {

	private final static int questId = 2239;
	public _2239MalodorAntidote() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203613).addOnQuestStart(questId); //Gilungk
		qe.registerQuestNpc(203613).addOnTalkEvent(questId);//Gilungk
		qe.registerQuestNpc(203630).addOnTalkEvent(questId); //Vovetirn
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		}
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 203613){
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				}
				else {
					return sendQuestStartDialog(env);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (targetId == 203630) {
				switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 0) {
						return sendQuestDialog(env, 1352);
					}
					if (var == 1) {
						return sendQuestDialog(env, 1693);
					}
				case STEP_TO_1:
				if (var == 0) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					updateQuestStatus(env);
					return closeDialogWindow(env);
				}
				case STEP_TO_2:
				if (var == 2) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					updateQuestStatus(env);
					return closeDialogWindow(env);
				}
				case CHECK_COLLECTED_ITEMS:
					if (var == 1) {
						if(QuestService.collectItemCheck(env, true)) {
							if (!giveQuestItem(env, 182203227, 1)) {
								return true;
							}
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
							return sendQuestDialog(env, 1779);
						}
						else {
							return sendQuestDialog(env, 1694);
						}
					}
				}
			}
			if (targetId == 203613) {
				switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 3) {
						return sendQuestDialog(env, 2034);
					}
				case STEP_TO_3:
					if (var == 3) {
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return sendQuestDialog(env, 5);						
					}
				}
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203613) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
