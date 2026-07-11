package com.aionemu.gameserver.quest.handlers.iluma;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 伊卢玛任务脚本：Discovery Of A New Technology（任务 ID 15541）。
 * Iluma quest script: Discovery Of A New Technology (quest ID 15541).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _15541Discovery_Of_A_New_Technology extends QuestHandler {

	private static final int questId = 15541;
	public _15541Discovery_Of_A_New_Technology() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(834136).addOnQuestStart(questId); //Kalio.
		qe.registerQuestNpc(834136).addOnTalkEvent(questId); //Kalio.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 834136) { //Kalio.
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					} case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE: {
						return sendQuestStartDialog(env);
					} case REFUSE_QUEST_SIMPLE: {
				        return closeDialogWindow(env);
					}
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.START) {
			if (targetId == 834136) { //Kalio.
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					} case CHECK_COLLECTED_ITEMS: {
						return checkQuestItems(env, 0, 0, true, 5, 2716);
					}
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 834136) { //Kalio.
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
