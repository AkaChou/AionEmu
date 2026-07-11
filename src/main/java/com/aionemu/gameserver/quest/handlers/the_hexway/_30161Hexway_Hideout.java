package com.aionemu.gameserver.quest.handlers.the_hexway;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 六芒星通道任务脚本：Hexway Hideout（任务 ID 30161）。
 * The Hexway quest script: Hexway Hideout (quest ID 30161).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _30161Hexway_Hideout extends QuestHandler {

	private final static int questId = 30161;
	public _30161Hexway_Hideout() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(799225).addOnTalkEvent(questId); //Richelle.
		qe.registerQuestNpc(799383).addOnTalkEvent(questId); //Vergelan.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.START) {
			if (targetId == 799225) { //Richelle.
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1352);
					} case STEP_TO_1: {
						return defaultCloseDialog(env, 0, 1);
					}
				}
			} else if (targetId == 799383) { //Vergelan.
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 2375);
					} case SELECT_REWARD: {
						changeQuestStep(env, 1, 1, true);
						return sendQuestDialog(env, 5);
					}
				}
			}
		} 
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799383) { //Vergelan.
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
