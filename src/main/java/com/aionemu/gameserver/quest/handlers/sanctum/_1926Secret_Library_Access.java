package com.aionemu.gameserver.quest.handlers.sanctum;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 圣所任务脚本：Secret Library Access（任务 ID 1926）。
 * Sanctum quest script: Secret Library Access (quest ID 1926).
 *
 * @author Ghostfur, Unknown (Aion-Unique)
 */
public class _1926Secret_Library_Access extends QuestHandler {

	private static final int questId = 1926;

	public _1926Secret_Library_Access() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203701).addOnTalkEvent(questId); // Lavirintos
		qe.registerQuestNpc(203894).addOnTalkEvent(questId); // Latri
		qe.registerOnLevelUp(questId);
	}

	@Override
	public boolean onLvlUpEvent(QuestEnv env) {
		return defaultOnLvlUpEvent(env);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		
		if (qs == null) {
			return false;
		}

		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		}

		if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 203701) { // Lavirintos
				switch (env.getDialog()) {
					case START_DIALOG:
						return sendQuestDialog(env, 1011);
					case SELECT_ACTION_1012:
						return sendQuestDialog(env, 1012);
					case SET_REWARD:
						giveQuestItem(env, 182206022, 1);
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return closeDialogWindow(env);
					default:
						break;
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203894) { // Latri
				switch (env.getDialog()) {
					case START_DIALOG:
						return sendQuestDialog(env, 10002);
					case SELECT_REWARD:
						removeQuestItem(env, 182206022, 1);
						return sendQuestDialog(env, 5);
					default:
						return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}
