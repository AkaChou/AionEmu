package com.aionemu.gameserver.quest.handlers.fenris_fang;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 芬里尔之牙任务脚本：Fang Of Construction（任务 ID 29064）。
 * Fenris Fang quest script: Fang Of Construction (quest ID 29064).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _29064Fang_Of_Construction extends QuestHandler {
	
	private final static int questId = 29064;
	public _29064Fang_Of_Construction() {
		super(questId);
	}
	
	@Override
	public void register() {
		int[] npcs = {798452, 204075, 204053};
		qe.registerQuestNpc(204053).addOnQuestStart(questId);
		for (int npc: npcs) {
			qe.registerQuestNpc(npc).addOnTalkEvent(questId);
		}
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204053) {
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} if (qs == null)
			return false;
		int var = qs.getQuestVarById(0);
		if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 798452:
					switch (dialog) {
						case START_DIALOG:
							return sendQuestDialog(env, 1352);
						case STEP_TO_1:
							return defaultCloseDialog(env, 0, 1);
					}
				case 204075:
					switch (dialog) {
						case START_DIALOG: {
							if (var == 1) {
								return sendQuestDialog(env, 2375);
							}
						}						
						case CHECK_COLLECTED_ITEMS_SIMPLE: {
					        return checkQuestItemsSimple(env, 1, 2, true, 5, 0, 0);
						}
						case SELECT_NO_REWARD: {
							return sendQuestEndDialog(env);
						}
					}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			return sendQuestEndDialog(env);
		}
		return false;
	}
}
