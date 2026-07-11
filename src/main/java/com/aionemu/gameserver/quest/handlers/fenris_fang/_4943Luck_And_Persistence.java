package com.aionemu.gameserver.quest.handlers.fenris_fang;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 芬里尔之牙任务脚本：Luck And Persistence（任务 ID 4943）。
 * Fenris Fang quest script: Luck And Persistence (quest ID 4943).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _4943Luck_And_Persistence extends QuestHandler {

	private final static int questId = 4943;
	public _4943Luck_And_Persistence() {
		super(questId);
	}
	
	@Override
	public void register() {
		int[] npcs = {204096, 204097, 204075, 204053, 700538};
		qe.registerQuestNpc(204053).addOnQuestStart(questId);
		for (int npc: npcs) {
		    qe.registerQuestNpc(npc).addOnTalkEvent(questId);
		}
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204053) {
				if (dialog == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 4762);
				else
					return sendQuestStartDialog(env);
			}
		} if (qs == null)
			return false;
		int var = qs.getQuestVarById(0);
		if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 204096:
					if (var == 0) {
						switch (dialog) {
							case START_DIALOG:
								return sendQuestDialog(env, 1011);
							case STEP_TO_1:
								return defaultCloseDialog(env, 0, 1);
						}
					} if (var == 2) {
						switch (dialog) {
							case START_DIALOG:
								return sendQuestDialog(env, 1693);
							case CHECK_COLLECTED_ITEMS:
								if (QuestService.collectItemCheck(env, true)) {
									removeQuestItem(env, 182207123, 1);
									changeQuestStep(env, 2, 3, false);
									return sendQuestDialog(env, 10000);
								} else
									return sendQuestDialog(env, 10001);
						}
					}
				case 204097:
					if (var == 1) {
						switch (dialog) {
							case START_DIALOG:
								return sendQuestDialog(env, 1352);
							case SELECT_ACTION_1354:
								if (player.getInventory().tryDecreaseKinah(3400000)) {
									if (player.getInventory().getItemCountByItemId(182207123) == 0) {
										if (!giveQuestItem(env, 182207123, 1))
											return true;
									}
									changeQuestStep(env, 1, 2, false);
									return sendQuestDialog(env, 1354);
								} else {
									return sendQuestDialog(env, 1438);
								}
						}
					}
				case 700538:
					if (dialog == QuestDialog.USE_OBJECT && var == 2) {
						return useQuestObject(env, 2, 2, false, 0);
					}
				case 204075:
					switch (dialog) {
						case START_DIALOG: {
							if (var == 3) {
								return sendQuestDialog(env, 2034);
							}
						} case SET_REWARD: {
							if (player.getInventory().getItemCountByItemId(186000084) >= 1) {
								removeQuestItem(env, 186000084, 1);
								return defaultCloseDialog(env, 3, 3, true, false, 0);
							} else {
								return sendQuestDialog(env, 2120);
							}
						}
					}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204053) {
				if (dialog == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 10002);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}
