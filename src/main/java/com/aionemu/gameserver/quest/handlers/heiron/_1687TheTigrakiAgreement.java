package com.aionemu.gameserver.quest.handlers.heiron;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 希隆任务脚本：The Tigraki Agreement（任务 ID 1687）。
 * Heiron quest script: The Tigraki Agreement (quest ID 1687). Go to Draupnir Cave in Asmodae and get Blue Balaur Blood (186000035) (2) and Balaur Rainbow Scales (186000036) (6) for Brosia (204601). Go to Brosia to choose your reward.
 *
 * @author Balthazar
 * @reworked vlog
 */
public class _1687TheTigrakiAgreement extends QuestHandler {

	private final static int questId = 1687;
	private int rewardId;
	public _1687TheTigrakiAgreement() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204601).addOnQuestStart(questId);
		qe.registerQuestNpc(204601).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 204601) { // Brosia
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 4762);
				else
					return sendQuestStartDialog(env);
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (targetId == 204601) { // Brosia
				switch (env.getDialog()) {
					case START_DIALOG:
						return sendQuestDialog(env, 1011);
					case CHECK_COLLECTED_ITEMS: {
						if (QuestService.collectItemCheck(env, true)) {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
						    updateQuestStatus(env);
							return sendQuestDialog(env, 1352); // choose your reward
						}
						else
							return sendQuestDialog(env, 1097);
					}
                    case SELECT_ACTION_1354: {
						rewardId = 0;
						return sendQuestDialog(env, 1354);
                    }
				    case SELECT_ACTION_1375: {
						rewardId = 1;
						return sendQuestDialog(env, 1375);
                    }  
					case SELECT_ACTION_1396: {
						rewardId = 2;
						return sendQuestDialog(env, 1396);
                    }
					case STEP_TO_10: {
						return defaultCloseDialog(env, var, var, true, true, 0); // reward 1
					}
					case STEP_TO_20: {
						return defaultCloseDialog(env, var, var, true, true, 1); // reward 2
					}
					case STEP_TO_30: {
						return defaultCloseDialog(env, var, var, true, true, 2); // reward 3
					}
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204601) {
				return sendQuestEndDialog(env, rewardId);
			}
		}
		return false;
	}
}
