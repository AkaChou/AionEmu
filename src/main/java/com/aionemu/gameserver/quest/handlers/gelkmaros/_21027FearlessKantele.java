package com.aionemu.gameserver.quest.handlers.gelkmaros;

import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 格尔克马洛斯任务脚本：Fearless Kantele（任务 ID 21027）。
 * Gelkmaros quest script: Fearless Kantele (quest ID 21027).
 *
 * @author VladimirZ
 */
public class _21027FearlessKantele extends QuestHandler {

	private final static int questId = 21027;
	public _21027FearlessKantele() {
		super(questId);
	}

	@Override
	public void register() {
		int[] npcs = {799254, 799255};
		for (int npc : npcs)
			qe.registerQuestNpc(npc).addOnTalkEvent(questId);
		qe.registerQuestNpc(799254).addOnQuestStart(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		QuestState qs = env.getPlayer().getQuestStateList().getQuestState(questId);
		if (env.getTargetId() == 799254) {
			if (qs == null || qs.getStatus() == QuestStatus.NONE) {
				switch (env.getDialog()) {
					case START_DIALOG:
					   return sendQuestDialog(env, 4762);
                    case ASK_ACCEPTION: {
                      return sendQuestDialog(env, 4);
                    }
				    case ACCEPT_QUEST: {
					   return sendQuestStartDialog(env);
				    } 
                 }   
			}
		} if (qs == null)
			return false;
		if (qs.getStatus() == QuestStatus.START) {
		int var = qs.getQuestVarById(0);
			if (env.getTargetId() == 799255) {
				switch (env.getDialog()) {
					case START_DIALOG:
						if (var == 0)
							return sendQuestDialog(env, 1011);
						else if (var == 1)
							return sendQuestDialog(env, 1352);
					case CHECK_COLLECTED_ITEMS:
						return checkQuestItems(env, 1, 2, true, 10000, 10001);
					case STEP_TO_1:
						return defaultCloseDialog(env, 0, 1);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (env.getTargetId() == 799254) {
				switch (env.getDialog()) {
					case START_DIALOG:
					return sendQuestDialog(env, 10002);
				} 
					return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
