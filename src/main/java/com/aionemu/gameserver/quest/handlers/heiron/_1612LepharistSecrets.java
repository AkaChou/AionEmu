package com.aionemu.gameserver.quest.handlers.heiron;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 希隆任务脚本：Lepharist Secrets（任务 ID 1612）。
 * Heiron quest script: Lepharist Secrets (quest ID 1612).
 *
 * @author Balthazar
 */
public class _1612LepharistSecrets extends QuestHandler {

	private final static int questId = 1612;
	public _1612LepharistSecrets() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204530).addOnQuestStart(questId);
		qe.registerQuestNpc(204530).addOnTalkEvent(questId);
		qe.registerQuestNpc(700352).addOnTalkEvent(questId);

	}

	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204530) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				}
				else
					return sendQuestStartDialog(env);
			}
		}
		if (qs == null)
			return false;
		if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			switch (targetId) {
				case 700352: {
					switch (env.getDialog()) {
						case USE_OBJECT: {
							switch (qs.getQuestVarById(0)) {
								case 0:
								case 1:
								case 2: {
									return useQuestObject(env, var, var + 1, false, true); // var++
								}
								case 3: {
									return useQuestObject(env, 3, 3, true, true); // reward
								}
							}
						}
					}
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204530) {
				if (env.getDialogId() == 1009)
					return sendQuestDialog(env, 5);
				else
					return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
