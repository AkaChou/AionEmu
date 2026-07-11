package com.aionemu.gameserver.quest.handlers.the_hexway;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 六芒星通道任务脚本：Cache Ing In On Turmoil（任务 ID 30061）。
 * The Hexway quest script: Cache Ing In On Turmoil (quest ID 30061).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _30061Cache_Ing_In_On_Turmoil extends QuestHandler {

	private final static int questId = 30061;
	public _30061Cache_Ing_In_On_Turmoil() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(798927).addOnTalkEvent(questId); //Versetti.
		qe.registerQuestNpc(799381).addOnTalkEvent(questId); //Lania.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.START) {
			if (targetId == 798927) { //Versetti.
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1352);
					} case STEP_TO_1: {
						return defaultCloseDialog(env, 0, 1);
					}
				}
			} else if (targetId == 799381) { //Lania.
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
			if (targetId == 799381) { //Lania.
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
