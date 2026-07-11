package com.aionemu.gameserver.quest.handlers.abyss_entry;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 欧比斯入场任务脚本：The Abyss Quiz（任务 ID 1921）。
 * Abyss entry quest script: The Abyss Quiz (quest ID 1921).
 */
public class _1921The_Abyss_Quiz extends QuestHandler
{
	private final static int questId = 1921;

	public _1921The_Abyss_Quiz() {
		super(questId);
	}
	
	@Override
	public boolean onLvlUpEvent(QuestEnv env) {
		return defaultOnLvlUpEvent(env, 1920);
	}
	
	@Override
	public void register() {
		qe.registerOnLevelUp(questId);
		qe.registerQuestNpc(203752).addOnTalkEvent(questId);
		qe.registerQuestNpc(203836).addOnTalkEvent(questId);
		qe.registerQuestNpc(203834).addOnTalkEvent(questId);
		qe.registerQuestNpc(203833).addOnTalkEvent(questId);
		qe.registerQuestNpc(203830).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null) {
			return false;
		}
		int var = qs.getQuestVarById(0);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		}
		if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 203752: {
					switch (env.getDialog()) {
						case START_DIALOG:
						if (var == 0) {
							return sendQuestDialog(env, 1011);
						} case STEP_TO_1: {
							return defaultCloseDialog(env, 0, 1);
						}
					}
				}
				break;
				case 203836: {
					switch (env.getDialog()) {
						case START_DIALOG:
						if (var == 1) {
							return sendQuestDialog(env, 1352);
						} case STEP_TO_2: {
							return defaultCloseDialog(env, 1, 2);
						}
					}
				}
				break;
				case 203834: {
					switch (env.getDialog()) {
						case START_DIALOG:
						if (var == 2) {
							return sendQuestDialog(env, 1693);
						} case STEP_TO_3: {
							return defaultCloseDialog(env, 2, 3);
						}
					}
				}
				break;
				case 203833: {
					switch (env.getDialog()) {
						case START_DIALOG:
						if (var == 3) {
							return sendQuestDialog(env, 2034);
						} case SET_REWARD: {
							return defaultCloseDialog(env, 3, 3, true, false);
						}	
					}
				}
				break;
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203830) {
				if (env.getDialog() == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 10002);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}
