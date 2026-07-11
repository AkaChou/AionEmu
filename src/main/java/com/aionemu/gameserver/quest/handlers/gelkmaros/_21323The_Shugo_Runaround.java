package com.aionemu.gameserver.quest.handlers.gelkmaros;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 格尔克马洛斯任务脚本：The Shugo Runaround（任务 ID 21323）。
 * Gelkmaros quest script: The Shugo Runaround (quest ID 21323).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _21323The_Shugo_Runaround extends QuestHandler {

	private final static int questId = 21323;
	public _21323The_Shugo_Runaround() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(799226).addOnQuestStart(questId); //Barretta.
		qe.registerQuestNpc(799226).addOnTalkEvent(questId); //Barretta.
		qe.registerQuestNpc(702726).addOnTalkEvent(questId); //Taserunerk.
		qe.registerQuestNpc(702728).addOnTalkEvent(questId); //Uzirunerk.
		qe.registerQuestNpc(702746).addOnTalkEvent(questId); //Zinarunerk.
		qe.registerQuestNpc(702747).addOnTalkEvent(questId); //Ruinrunerk.
		qe.registerQuestNpc(702748).addOnTalkEvent(questId); //Potarunerk.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		}
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 799226) { //Barretta.
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ACCEPT_QUEST_SIMPLE: {
						return sendQuestStartDialog(env);
					}
				}
			}
		} if (qs == null) {
			return false;
		} if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 702726: { //Taserunerk.
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1011);
						} case STEP_TO_1: {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
                            return closeDialogWindow(env);
						}
					}
				} case 702746: { //Zinarunerk.
				    switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1352);
						} case STEP_TO_2: {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
                            return closeDialogWindow(env);
						}
					}
				} case 702748: { //Potarunerk.
				    switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1693);
						} case STEP_TO_3: {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
                            return closeDialogWindow(env);
						}
					}
				} case 702747: { //Ruinrunerk.
				    switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2034);
						} case STEP_TO_4: {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
                            return closeDialogWindow(env);
						}
					}
				} case 702728: { //Uzirunerk.
				    switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2375);
						} case SET_REWARD: {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
                            qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
                            return closeDialogWindow(env);
						}
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799226) { //Barretta.
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
