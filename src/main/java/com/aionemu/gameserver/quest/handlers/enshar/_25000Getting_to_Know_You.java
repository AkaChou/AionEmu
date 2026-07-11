package com.aionemu.gameserver.quest.handlers.enshar;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 恩沙尔任务脚本：Getting to Know You（任务 ID 25000）。
 * Enshar quest script: Getting to Know You (quest ID 25000).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25000Getting_to_Know_You extends QuestHandler {

	private final static int questId = 25000;
	public _25000Getting_to_Know_You() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(804718).addOnQuestStart(questId); //CogelHogan.
		qe.registerQuestNpc(804718).addOnTalkEvent(questId); //CogelHogan.
		qe.registerQuestNpc(804753).addOnTalkEvent(questId); //Migrak.
		qe.registerQuestNpc(804990).addOnTalkEvent(questId); //Toporinerk.
		qe.registerQuestNpc(805000).addOnTalkEvent(questId); //Funen.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		} if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804718) { //CogelHogan.
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} if (qs == null) {
			return false;
		} if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 804753: { //Migrak.
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1011);
						} case STEP_TO_1: {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
					        return closeDialogWindow(env);
						}
					}
				} case 804990: { //Toporinerk.
				    switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1352);
						} case STEP_TO_2: {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
					        return closeDialogWindow(env);
						}
					}
				} case 805000: { //Funen.
				    switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1693);
						} case SET_REWARD: {
							qs.setStatus(QuestStatus.REWARD);
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
					        return closeDialogWindow(env);
						}
					}
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 804718) { //CogelHogan.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 10002);
                } else {
                    return sendQuestEndDialog(env);
                }
            }
        }
		return false;
	}
}
