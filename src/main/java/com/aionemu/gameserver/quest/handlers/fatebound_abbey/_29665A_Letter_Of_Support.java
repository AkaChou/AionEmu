package com.aionemu.gameserver.quest.handlers.fatebound_abbey;

import com.aionemu.gameserver.model.gameobjects.*;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.*;
import com.aionemu.gameserver.questEngine.model.*;

/**
 * 命运修道院任务脚本：A Letter Of Support（任务 ID 29665）。
 * Fatebound Abbey quest script: A Letter Of Support (quest ID 29665).
 *
 * @author Rinzler (Encom)
 */
public class _29665A_Letter_Of_Support extends QuestHandler {

	private final static int questId = 29665;
	public _29665A_Letter_Of_Support() {
		super(questId);
	}
	
	public void register() {
		qe.registerQuestNpc(804662).addOnQuestStart(questId);
		qe.registerQuestNpc(804662).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 804662) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 804662: {
					switch (dialog) {
						case START_DIALOG: {
							return sendQuestDialog(env, 10002);
						} case SELECT_REWARD: {
							changeQuestStep(env, 0, 0, true);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 804662) {
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
