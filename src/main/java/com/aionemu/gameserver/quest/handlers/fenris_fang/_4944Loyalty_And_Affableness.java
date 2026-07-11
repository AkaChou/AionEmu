package com.aionemu.gameserver.quest.handlers.fenris_fang;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 芬里尔之牙任务脚本：Loyalty And Affableness（任务 ID 4944）。
 * Fenris Fang quest script: Loyalty And Affableness (quest ID 4944).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _4944Loyalty_And_Affableness extends QuestHandler {

	private static final int questId = 4944;
	private static final int[] npcs = {204053, 204075};
	private static final int[] mobs = {214823, 220257, 220258, 220259, 220260, 220261, 220262, 220263, 220265, 220266, 220267, 220268, 220269, 220270, 220271, 220272, 220273, 220274, 220275, 220276, 220277, 220278, 220279, 220280, 220281, 220282, 220283, 220284, 220285, 220286, 220287, 220288, 220289, 220290, 220291, 220292, 220293, 220294, 220295, 220296};
	public _4944Loyalty_And_Affableness() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204053).addOnQuestStart(questId);
		for (int npc: npcs) {
			qe.registerQuestNpc(npc).addOnTalkEvent(questId);
		} for (int mob: mobs) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		}
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		QuestDialog dialog = env.getDialog();
		if (qs == null) {
			if (targetId == 204053) {
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVars().getQuestVars();
			switch (targetId) {
				case 204053: {
					switch (dialog) {
						case START_DIALOG: {
							if (var == 0) {
								return sendQuestDialog(env, 1011);
							} else if (var == 306) {
								return sendQuestDialog(env, 1693);
							} else if (var == 4) {
								return sendQuestDialog(env, 2375);
							}
						} case CHECK_COLLECTED_ITEMS: {
							return checkQuestItems(env, 0, 6, false, 10000, 10001);
						} case STEP_TO_3: {
							qs.setQuestVar(3);
							updateQuestStatus(env);
							return sendQuestSelectionDialog(env);
						} case STEP_TO_5: {
							return defaultCloseDialog(env, 4, 5);
						}
					}
				} case 204075: {
					switch (dialog) {
						case START_DIALOG: {
							if (var == 5) {
								return sendQuestDialog(env, 2716);
							}
						} case SELECT_ACTION_2718: {
							if (player.getCommonData().getDp() >= 4000) {
								return checkItemExistence(env, 5, 5, false, 186000087, 1, true, 2718, 2887, 0, 0);
							} else {
								return sendQuestDialog(env, 2802);
							}
						} case SET_REWARD: {
							player.getCommonData().setDp(0);
							return defaultCloseDialog(env, 5, 5, true, false);
						}
					}
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204053) {
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 10002);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean onKillEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVars().getQuestVars();
			if (var >= 6 && var < 306) {
				int[] npcids = {220257, 220258, 220259, 220260, 220261, 220262,
				220263, 220265, 220266, 220267, 220268, 220269, 220270, 220271,
				220272, 220273, 220274, 220275, 220276, 220277, 220278, 220279,
				220280, 220281, 220282, 220283, 220284, 220285, 220286, 220287,
				220288, 220289, 220290, 220291, 220292, 220293, 220294, 220295, 220296};
				for (int id : npcids) {
					if (targetId == id) {
						qs.setQuestVar(var + 1);
						updateQuestStatus(env);
						return true;
					}
				}
			} else if (var == 3) {
				int[] npcids = {214823};
				return defaultOnKillEvent(env, npcids, 3, 4);
			}
		}
		return false;
	}
}
