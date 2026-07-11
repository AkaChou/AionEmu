package com.aionemu.gameserver.quest.handlers.sanctum;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 圣所任务脚本：A Lingering Mystery（任务 ID 1917）。
 * Sanctum quest script: A Lingering Mystery (quest ID 1917).
 *
 * @author zhkchi
 */
public class _1917ALingeringMystery extends QuestHandler {

	private final static int questId = 1917;
	private int rewardId;
	public _1917ALingeringMystery() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203835).addOnQuestStart(questId);
		qe.registerQuestNpc(203835).addOnTalkEvent(questId);
		qe.registerQuestNpc(203075).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
	    if (qs == null || qs.getStatus() == QuestStatus.NONE) {
		    if (targetId == 203835) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
        }
		if (qs == null || qs.getStatus() == QuestStatus.START) {
			if (targetId == 203075) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1352);
				}
				else if (env.getDialog() == QuestDialog.STEP_TO_1) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					updateQuestStatus(env);
                    return closeDialogWindow(env);
				}
			}
			if (targetId == 203835) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1693);
				}
				else if (env.getDialogId() == 1694) {
					rewardId = 0;
                    return sendQuestDialog(env, 1694);
				}
				else if (env.getDialogId() == 1779) {
					rewardId = 1;
                    return sendQuestDialog(env, 1779);
				}
				else if (env.getDialogId() == 1864) {
					rewardId = 2;
                    return sendQuestDialog(env, 1864);
				}
				else if (env.getDialog() == QuestDialog.STEP_TO_1) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
                    qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
                    return sendQuestDialog(env, 5);
				}
				else if (env.getDialog() == QuestDialog.STEP_TO_2) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
                    qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
                    return sendQuestDialog(env, 6);
				}
				else if (env.getDialog() == QuestDialog.STEP_TO_3) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
                    qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
                    return sendQuestDialog(env, 7);
				}
			}
		}
		else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203835)
				return sendQuestEndDialog(env, rewardId);
		}
		return false;
	}
}
