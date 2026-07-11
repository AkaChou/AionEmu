package com.aionemu.gameserver.quest.handlers.pernon;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 佩尔农任务脚本：A Home Thats Your Own（任务 ID 28847）。
 * Pernon quest script: A Home Thats Your Own (quest ID 28847).
 *
 * @author zhkchi
 */
public class _28847AHomeThatsYourOwn extends QuestHandler {

	private static final int questId = 28847;
	public _28847AHomeThatsYourOwn(){
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(830390).addOnQuestStart(questId);
		qe.registerQuestNpc(830390).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if(qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 830390) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if(targetId == 830390) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1003);
					}
					case SELECT_REWARD: {
						changeQuestStep(env, 0, 0, true);
						return sendQuestEndDialog(env);
					}
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 830390) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
