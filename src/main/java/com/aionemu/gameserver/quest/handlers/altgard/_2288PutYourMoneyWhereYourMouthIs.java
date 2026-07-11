package com.aionemu.gameserver.quest.handlers.altgard;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 奥特加德任务脚本：Put Your Money Where Your Mouth Is（任务 ID 2288）。
 * Altgard quest script: Put Your Money Where Your Mouth Is (quest ID 2288).
 *
 * @author Atomics
 * @modified Gigi
 */
public class _2288PutYourMoneyWhereYourMouthIs extends QuestHandler {

	private final static int questId = 2288;
	public _2288PutYourMoneyWhereYourMouthIs() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203621).addOnQuestStart(questId);
		qe.registerQuestNpc(203621).addOnTalkEvent(questId);
		qe.registerQuestNpc(210564).addOnKillEvent(questId);
		qe.registerQuestNpc(210584).addOnKillEvent(questId);
		qe.registerQuestNpc(210581).addOnKillEvent(questId);
		qe.registerQuestNpc(201047).addOnKillEvent(questId);
		qe.registerQuestNpc(210436).addOnKillEvent(questId);
		qe.registerQuestNpc(210437).addOnKillEvent(questId);
		qe.registerQuestNpc(210440).addOnKillEvent(questId);
		qe.registerOnQuestTimerEnd(questId);
		qe.registerOnLogOut(questId);
	}

	@Override
	public boolean onKillEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		}
		if (qs != null && qs.getStatus() == QuestStatus.START) {
		int var = qs.getQuestVarById(0);
		if (targetId == 210564 || targetId == 210584 || targetId == 210581 || targetId == 210436 || targetId == 201047 || targetId == 210437 || targetId == 210440) {
			if (var > 0 && var < 3) {
				qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
				updateQuestStatus(env);
				return true;
			}
			else if (var == 3) {
				qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
				updateQuestStatus(env);
				QuestService.questTimerEnd(env);
				return true;
                }
			}
		}
		return false;
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 203621) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				}
				else {
					return sendQuestStartDialog(env);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 203621) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						if (qs.getQuestVarById(0) == 4) {
							return sendQuestDialog(env, 1352);
						}
						else if (qs.getQuestVarById(0) == 0) {
							return sendQuestDialog(env, 1003);
						}
					}
					case STEP_TO_1: {
						QuestService.questTimerStart(env, 600);
						qs.setQuestVarById(0, 1);
						updateQuestStatus(env);
						return closeDialogWindow(env);
					}
					case SELECT_REWARD: {
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						QuestService.questTimerEnd(env);
						return sendQuestDialog(env, 5);
					}
				}
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203621) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}

	@Override
	public boolean onQuestTimerEndEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			QuestService.abandonQuest(player, questId);
			return true;
		}
		return false;
	}

	@Override
	public boolean onLogOutEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			QuestService.abandonQuest(player, questId);
			return true;
		}
		return false;
	}
}
