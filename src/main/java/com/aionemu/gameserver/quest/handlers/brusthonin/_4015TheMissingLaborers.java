package com.aionemu.gameserver.quest.handlers.brusthonin;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 布鲁斯特霍宁任务脚本：The Missing Laborers（任务 ID 4015）。
 * Brusthonin quest script: The Missing Laborers (quest ID 4015).
 *
 * @author Mr. Poke remod by Nephis and quest helper team
 */
public class _4015TheMissingLaborers extends QuestHandler {

	private final static int questId = 4015;
	public _4015TheMissingLaborers() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(205130).addOnQuestStart(questId);
		qe.registerQuestNpc(205130).addOnTalkEvent(questId);
		qe.registerQuestNpc(730107).addOnTalkEvent(questId);
		qe.registerQuestNpc(205130).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 205130) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 730107: {
					if (qs.getQuestVarById(0) == 0 && env.getDialog() == QuestDialog.USE_OBJECT) {
					    return sendQuestDialog(env, 1352);
					}
				    else if (env.getDialog() == QuestDialog.STEP_TO_1) {
					    qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					    updateQuestStatus(env);
				        return closeDialogWindow(env);
				    }
				}
				case 205130: {
					if (qs.getQuestVarById(0) == 1) {
						if (env.getDialog() == QuestDialog.START_DIALOG)
							return sendQuestDialog(env, 2375);
						else if (env.getDialogId() == 1009) {
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 205130)
				return sendQuestEndDialog(env);
		}
		return false;
	}
}
