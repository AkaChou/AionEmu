/*
 * =====================================================================================*
 * This file is part of Aion-Unique (Aion-Unique Home Software Development)             *
 * Aion-Unique Development is a closed Aion Project that use Old Aion Project Base      *
 * Like Aion-Lightning, Aion-Engine, Aion-Core, Aion-Extreme, Aion-NextGen, ArchSoft,   *
 * Aion-Ger, U3J, Encom And other Aion project, All Credit Content                      *
 * That they make is belong to them/Copyright is belong to them. And All new Content    *
 * that Aion-Unique make the copyright is belong to Aion-Unique                         *
 * You may have agreement with Aion-Unique Development, before use this Engine/Source   *
 * You have agree with all of Term of Services agreement with Aion-Unique Development   *
 * =====================================================================================*
 */
package com.aionemu.gameserver.quest.handlers.poeta;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/****/
/** Author Ghostfur & Unknown (Aion-Unique)
/****/

public class _1005Barring_The_Gate extends QuestHandler {

	private final static int questId = 1005;
	public _1005Barring_The_Gate() {
		super(questId);
	}
	
	@Override
	public void register() {
		int[] talkNpcs = {203067, 203081, 790001, 203085, 203086, 700080, 700081, 700082, 700083};
		qe.registerOnLevelUp(questId);
		qe.registerOnEnterZoneMissionEnd(questId);
		for (int id: talkNpcs) {
		    qe.registerQuestNpc(id).addOnTalkEvent(questId);
		}
	}
	
	@Override
	public boolean onZoneMissionEndEvent(QuestEnv env) {
		int[] poetaQuests = {1100, 1001, 1002, 1003, 1004};
		return defaultOnZoneMissionEndEvent(env, poetaQuests);
	}
	
	@Override
	public boolean onLvlUpEvent(QuestEnv env) {
		int[] poetaQuests = {1100, 1001, 1002, 1003, 1004};
		return defaultOnLvlUpEvent(env, poetaQuests, false);
	}
	
	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null)
			return false;
		int var = qs.getQuestVarById(0);
		int targetId = env.getTargetId();
		if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 203067) {
				switch (env.getDialog()) {
					case START_DIALOG: {
					    if (var == 0) {
						    return sendQuestDialog(env, 1011);
					    }
					} case STEP_TO_1: {
					    if (var == 0) {
						    qs.setQuestVarById(0, var + 1);
						    updateQuestStatus(env);
						    return closeDialogWindow(env);
					    }
					}
				}
			} else if (targetId == 203081) {
				switch (env.getDialog()) {
					case START_DIALOG: {
					    if (var == 1) {
						    return sendQuestDialog(env, 1352);
					    }
					} case STEP_TO_2: {
					    if (var == 1) {
						    qs.setQuestVarById(0, var + 1);
						    updateQuestStatus(env);
						    return closeDialogWindow(env);
					    }
					}	
				}
			} else if (targetId == 790001) {
				switch (env.getDialog()) {
					case START_DIALOG: {
					    if (var == 2) {
						    return sendQuestDialog(env, 1693);
					    }
					} case STEP_TO_3: {
					    if (var == 2) {
						    qs.setQuestVarById(0, var + 1);
						    updateQuestStatus(env);
						    return closeDialogWindow(env);
					    }
					}
				}
			} else if (targetId == 203085) {
				switch (env.getDialog()) {
					case START_DIALOG: {
					    if (var == 3) {
						    return sendQuestDialog(env, 2034);
					    }
					} case STEP_TO_4: {
					    if (var == 3) {
						    qs.setQuestVarById(0, var + 1);
						    updateQuestStatus(env);
						    return closeDialogWindow(env);
					    }
					}
				}
			} else if (targetId == 203086) {
				switch (env.getDialog()) {
					case START_DIALOG: {
					    if (var == 4) {
						    return sendQuestDialog(env, 2375);
						}
					} case STEP_TO_5: {
					    if (var == 4) {
						    qs.setQuestVarById(0, var + 1);
						    updateQuestStatus(env);
						    return closeDialogWindow(env);
						}
					}
				}
			} else if (targetId == 700081) {
				if (var == 5) {
                    qs.setQuestVarById(0, var + 1);
					updateQuestStatus(env);
					return false;
				}
			} else if (targetId == 700082) {
				if (var == 6) {
                    qs.setQuestVarById(0, var + 1);
					updateQuestStatus(env);
					return false;
				}
			} else if (targetId == 700083) {
                if (var == 7) {
                    qs.setQuestVarById(0, var + 1);
					updateQuestStatus(env);
                    return false;
				}
			} else if (targetId == 700080) {
                if (var == 8) {
                    playQuestMovie(env, 21);
                    qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
                    return false;
				}
			}
		} 
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203067) { // Kalio
				switch (env.getDialog()) {
					case USE_OBJECT: {
						return sendQuestDialog(env, 2716);
                    }
					case SELECT_REWARD: {
						playQuestMovie(env, 171);
						return sendQuestDialog(env, 5);
					}
				}
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}