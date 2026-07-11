package com.aionemu.gameserver.quest.handlers.brusthonin;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 布鲁斯特霍宁任务脚本：The Seeds Of Hope（任务 ID 4004）。
 * Brusthonin quest script: The Seeds Of Hope (quest ID 4004).
 *
 * @author Nephis
 */
public class _4004TheSeedsOfHope extends QuestHandler {

	private final static int questId = 4004;

	public _4004TheSeedsOfHope() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(205128).addOnQuestStart(questId); // Randet
		qe.registerQuestNpc(205128).addOnTalkEvent(questId); // Randet
		qe.registerQuestNpc(700340).addOnTalkEvent(questId); // Earth Mound
	}

	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);

		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 205128) { // Randet
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 4762);
				else
					return sendQuestStartDialog(env);
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 205128) {
				if (env.getDialog() == QuestDialog.USE_OBJECT)
					return sendQuestDialog(env, 10002);
				else
					return sendQuestEndDialog(env);
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.START) {
			final int var = qs.getQuestVarById(0);
			switch (targetId) {
				case 700340: { // Earth Mound
					if (qs != null && env.getDialog() == QuestDialog.USE_OBJECT) {
						if (var < 4) {
							return useQuestObject(env, var, var + 1, false, true);
						}
						else if (var == 4) {
							return useQuestObject(env, 4, 4, true, true); // reward
						}
					}
				}
			}
		}
		return false;
	}
}
