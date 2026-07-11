package com.aionemu.gameserver.quest.handlers.enshar;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 恩沙尔任务脚本：An Offering Of Friendship（任务 ID 25094）。
 * Enshar quest script: An Offering Of Friendship (quest ID 25094).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25094An_Offering_Of_Friendship extends QuestHandler {

	private static final int questId = 25094;
	public _25094An_Offering_Of_Friendship() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(804929).addOnQuestStart(questId);
		qe.registerQuestNpc(804929).addOnTalkEvent(questId);
		qe.registerQuestNpc(702768).addOnTalkEvent(questId);
		qe.registerQuestNpc(804740).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804929) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
            if (targetId == 702768) { 
			if (env.getDialog() == QuestDialog.USE_OBJECT) {
				return true;
			   }
		    }   
            if (targetId == 804929) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					} case CHECK_COLLECTED_ITEMS: {
						return checkQuestItems(env, 0, 1, true, 10000, 10001);
					}
				}
			}
		} else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804740) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
