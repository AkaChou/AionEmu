package com.aionemu.gameserver.quest.handlers.eltnen;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 艾特南任务脚本：A Giftof Love（任务 ID 1464）。
 * Eltnen quest script: A Giftof Love (quest ID 1464).
 *
 * @author Balthazar
 */
public class _1464AGiftofLove extends QuestHandler {

	private final static int questId = 1464;
	public _1464AGiftofLove() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204424).addOnQuestStart(questId);
		qe.registerQuestNpc(204424).addOnTalkEvent(questId);
		qe.registerQuestNpc(203755).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204424) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				}
				else
					return sendQuestStartDialog(env);
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 204424: {
					switch (env.getDialog()) {
						case START_DIALOG: {
							long itemCount1 = player.getInventory().getItemCountByItemId(152000455);
							if (qs.getQuestVarById(0) == 0 && itemCount1 >= 15) {
								qs.setQuestVar(0);
								qs.setStatus(QuestStatus.REWARD);
								removeQuestItem(env, 152000455, 1);
								updateQuestStatus(env);
								return sendQuestDialog(env, 10000);
							}
							else
								return sendQuestDialog(env, 10001);
						}
					}
				}
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203755) {
				if (env.getDialogId() == 1009)
					return sendQuestDialog(env, 5);
				else
					return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
