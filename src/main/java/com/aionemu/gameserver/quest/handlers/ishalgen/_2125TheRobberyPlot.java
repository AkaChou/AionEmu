package com.aionemu.gameserver.quest.handlers.ishalgen;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 伊沙尔根任务脚本：The Robbery Plot（任务 ID 2125）。
 * Ishalgen quest script: The Robbery Plot (quest ID 2125).
 *
 * @author MrPoke, Dune11
 */
public class _2125TheRobberyPlot extends QuestHandler {

	private final static int questId = 2125;
	public _2125TheRobberyPlot() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203540).addOnQuestStart(questId);
		qe.registerQuestNpc(203540).addOnTalkEvent(questId);
		qe.registerQuestNpc(203514).addOnTalkEvent(questId);
		qe.registerQuestNpc(203543).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
		    if (targetId == 203540) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
		}
		else if (targetId == 203514) {
			if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 0) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1352);
				else if (env.getDialog() == QuestDialog.STEP_TO_1) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					updateQuestStatus(env);
					return closeDialogWindow(env);
				}
			}
		}
		else if (targetId == 203543) {
			if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 1) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 2375);
				else if (env.getDialogId() == 1009) {
					qs.setQuestVar(2);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return sendQuestEndDialog(env);
				}
			}
            else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
