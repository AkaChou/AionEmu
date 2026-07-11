package com.aionemu.gameserver.quest.handlers.verteron;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 沃特伦任务脚本：Altenos Wedding Ring（任务 ID 1162）。
 * Verteron quest script: Altenos Wedding Ring (quest ID 1162).
 *
 * @author Balthazar. redone DainAvenger
 */
public class _1162AltenosWeddingRing extends QuestHandler {

	private final static int questId = 1162;
	private int rewardId;
	public _1162AltenosWeddingRing() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203095).addOnQuestStart(questId);
		qe.registerQuestNpc(203095).addOnTalkEvent(questId);
		qe.registerQuestNpc(203093).addOnTalkEvent(questId);
		qe.registerQuestNpc(700005).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 203095) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
		}
		if (qs == null)
			return false;
		if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 700005: {
					switch (env.getDialog()) {
						case USE_OBJECT:
						    return sendQuestDialog(env, 3739);
						case STEP_TO_1:
						if (player.getInventory().getItemCountByItemId(182200563) == 0) {
							giveQuestItem(env, 182200563, 1);
							changeQuestStep(env, 0, 1, false);
						    return closeDialogWindow(env);
						}
					}
				}
			    case 203095:
                switch (env.getDialog()) {
				    case START_DIALOG: {
                        return sendQuestDialog(env, 1352);
                    }
                    case CHECK_COLLECTED_ITEMS:
                    if (QuestService.collectItemCheck(env, true)) {
					   rewardId = 0;
					   changeQuestStep(env, 1, 1, true);
                       return sendQuestDialog(env, 5);
                       } else {
                       return sendQuestDialog(env, 1693);
                    }
					case STEP_TO_2:
						return closeDialogWindow(env);
				}
			    case 203093:
                switch (env.getDialog()) {
				    case START_DIALOG: {
                        return sendQuestDialog(env, 2034);
                    }
                    case CHECK_COLLECTED_ITEMS:
                    if (QuestService.collectItemCheck(env, true)) {
					   rewardId = 1;
                       qs.setStatus(QuestStatus.REWARD);
					   changeQuestStep(env, 1, 11, false);
                       return sendQuestDialog(env, 6);
                       } else {
                       return sendQuestDialog(env, 2375);
                    }
					case STEP_TO_2:
						return closeDialogWindow(env);
			    }
	        }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 203095 && qs.getQuestVarById(0) == 1) {
				if (env.getDialogId() == 1009) {
					return sendQuestDialog(env, 5);
			    } else
					return sendQuestEndDialog(env, rewardId);
			}
			if (targetId == 203093 && qs.getQuestVarById(0) == 11) {
				if (env.getDialogId() == 1009) {
					return sendQuestDialog(env, 6);
				} else
					return sendQuestEndDialog(env, rewardId);
			}
		}
		return false;
	}
}
