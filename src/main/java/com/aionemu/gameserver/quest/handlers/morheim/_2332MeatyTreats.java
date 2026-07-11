package com.aionemu.gameserver.quest.handlers.morheim;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 莫尔海姆任务脚本：Meaty Treats（任务 ID 2332）。
 * Morheim quest script: Meaty Treats (quest ID 2332).
 *
 * @author stpavel
 */
public class _2332MeatyTreats extends QuestHandler {

	private final static int questId = 2332;
	private int rewardId;
	public _2332MeatyTreats() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(798084).addOnQuestStart(questId);
		qe.registerQuestNpc(798084).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 798084) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 4762);
				else
					return sendQuestStartDialog(env);
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.START) {
			if (targetId == 798084) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					if (QuestService.collectItemCheck(env, true))
						return sendQuestDialog(env, 1352);
					else
						return sendQuestDialog(env, 1693);
				}
				else if (env.getDialogId() == 10000) {
					rewardId = 0;
					qs.setQuestVar(1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
                    return sendQuestDialog(env, 5);

				}
				else if (env.getDialogId() == 10001) {
					rewardId = 1;
					qs.setQuestVar(1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
                    return sendQuestDialog(env, 6);

				}
				else if (env.getDialogId() == 10002) {
					rewardId = 2;
					qs.setQuestVar(1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
                    return sendQuestDialog(env, 7);

				}
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798084) {
				return sendQuestEndDialog(env, rewardId);
			}	
		}
		return false;
	}
}
