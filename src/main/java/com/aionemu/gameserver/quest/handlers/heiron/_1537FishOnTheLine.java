package com.aionemu.gameserver.quest.handlers.heiron;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 希隆任务脚本：Fish On The Line（任务 ID 1537）。
 * Heiron quest script: Fish On The Line (quest ID 1537).
 *
 * @author Nephis and quest helper team
 */
public class _1537FishOnTheLine extends QuestHandler {

	private final static int questId = 1537;
	public _1537FishOnTheLine() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204588).addOnQuestStart(questId);
		qe.registerQuestNpc(204588).addOnTalkEvent(questId);
		qe.registerQuestNpc(730189).addOnTalkEvent(questId);
		qe.registerQuestNpc(730190).addOnTalkEvent(questId);
		qe.registerQuestNpc(730191).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204588) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 730189: {
					if (env.getDialog() == QuestDialog.USE_OBJECT) {
					    return sendQuestDialog(env, 1352);
                    }
				    else if (env.getDialog() == QuestDialog.SELECT_ACTION_1353) {
					    return sendQuestDialog(env, 1353);
				    }
				    else if (env.getDialog() == QuestDialog.STEP_TO_1) {
					    qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					    updateQuestStatus(env);
                       return closeDialogWindow(env);
					}
				}
				case 730190: {
					if (env.getDialog() == QuestDialog.USE_OBJECT) {
					    return sendQuestDialog(env, 1693);
                    }
				    else if (env.getDialog() == QuestDialog.SELECT_ACTION_1694) {
					    return sendQuestDialog(env, 1694);
				    }
				    else if (env.getDialog() == QuestDialog.STEP_TO_2) {
					    qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					    updateQuestStatus(env);
                        return closeDialogWindow(env);
					}
				}
				case 730191: {
					if (qs.getQuestVarById(0) == 2 && env.getDialog() == QuestDialog.USE_OBJECT) {
					    return sendQuestDialog(env, 2034);
                    }
				    else if (env.getDialog() == QuestDialog.SELECT_ACTION_2035) {
					    return sendQuestDialog(env, 2035);
				    }
				    else if (env.getDialog() == QuestDialog.STEP_TO_3) {
					    qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
						updateQuestStatus(env);
                        return closeDialogWindow(env);
					}
				}
				case 204588: {
					if (qs.getQuestVarById(0) == 3 && env.getDialog() == QuestDialog.START_DIALOG) {
					    return sendQuestDialog(env, 2375);
                    }
				    else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
					    qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
				        return sendQuestEndDialog(env);
					}
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204588)
				return sendQuestEndDialog(env);
		}
		return false;
	}
}
