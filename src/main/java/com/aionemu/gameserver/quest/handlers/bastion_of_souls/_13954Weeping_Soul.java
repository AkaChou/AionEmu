package com.aionemu.gameserver.quest.handlers.bastion_of_souls;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 灵魂堡垒任务脚本：Weeping Soul（任务 ID 13954）。
 * Bastion of Souls quest script: Weeping Soul (quest ID 13954).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _13954Weeping_Soul extends QuestHandler {

	private final static int questId = 13954;
	private final static int[] npcs = {806590, 203840, 806582};
	public _13954Weeping_Soul() {
		super(questId);
	}
	
	public void register() {
		for (int npc: npcs) {
            qe.registerQuestNpc(npc).addOnTalkEvent(questId);
        }
        qe.registerQuestNpc(247093).addOnKillEvent(questId);
		qe.registerQuestItem(182216179, questId);
		qe.registerQuestNpc(806590).addOnAtDistanceEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.START) {
			if (targetId == 806590) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						if (qs.getQuestVarById(0) == 0) {
							return sendQuestDialog(env, 1011);
						}
					} case SELECT_ACTION_1012: {
						if (qs.getQuestVarById(0) == 0) {
							return sendQuestDialog(env, 1012);
						}
					} case SELECT_ACTION_1013: {
						if (qs.getQuestVarById(0) == 0) {
							return sendQuestDialog(env, 1013);
						}
					} case STEP_TO_1: {
						giveQuestItem(env, 182216179, 1);
						changeQuestStep(env, 0, 1, false);
						return closeDialogWindow(env);
					}
				}
			} if (targetId == 203840) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						if (qs.getQuestVarById(0) == 1) {
							return sendQuestDialog(env, 1352);
						} if (qs.getQuestVarById(0) == 3) {
							return sendQuestDialog(env, 2120);
						}
					} case SELECT_ACTION_1353: {
						if (qs.getQuestVarById(0) == 1) {
							return sendQuestDialog(env, 1353);
						}
					} case SELECT_ACTION_2034: {
						if (qs.getQuestVarById(0) == 3) {
							return sendQuestDialog(env, 2034);
						}
					} case SELECT_ACTION_2035: {
						if (qs.getQuestVarById(0) == 3) {
							return sendQuestDialog(env, 2035);
						}
					} case STEP_TO_2: {
						changeQuestStep(env, 1, 2, false);
						QuestService.addNewSpawnForSeconds(110010000, 1, 247093, 1939, 1768, 576, (byte) 55, 100);
						QuestService.addNewSpawnForSeconds(110010000, 1, 247093, 1939, 1768, 576, (byte) 55, 100);
						QuestService.addNewSpawnForSeconds(110010000, 1, 247093, 1939, 1768, 576, (byte) 55, 100);
						return closeDialogWindow(env);
					} case SET_REWARD: {
					    qs.setQuestVar(4);
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return closeDialogWindow(env);
					}
				}
			}
		} 
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 806582) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 10002);
				} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean onAtDistanceEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			QuestService.startQuest(env);
			return true;
		}
		return false;
	}
	
	@Override
    public boolean onKillEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        int targetId = env.getTargetId();
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
            if (var == 2) {
                switch (targetId) {
                    case 247093: {
                        qs.setQuestVar(3);
						updateQuestStatus(env);
						return true;
                    }
                }
            }
        }
        return false;
    }
}
