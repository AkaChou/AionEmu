package com.aionemu.gameserver.quest.handlers.pandaemonium;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 潘德莫尼姆任务脚本：Growth Niniss Second Charm（任务 ID 4967）。
 * Pandaemonium quest script: Growth Niniss Second Charm (quest ID 4967). Talk with Maochinicherk (798068). Bring the Glossy Aether Paper (186000091) and Kinah (50000) to Ninis (798385).
 *
 * @author undertrey
 * @modified vlog
 */
public class _4967GrowthNinissSecondCharm extends QuestHandler {

	private final static int questId = 4967;
	public _4967GrowthNinissSecondCharm() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(798385).addOnQuestStart(questId);
		qe.registerQuestNpc(798385).addOnTalkEvent(questId);
		qe.registerQuestNpc(798068).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 798385) { // Ninis
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			switch (targetId) {
				case 798068: { // Maochinicherk
					switch (env.getDialog()) {
						case START_DIALOG:
							if (var == 0)
								return sendQuestDialog(env, 1352);
						case STEP_TO_1:
							return defaultCloseDialog(env, 0, 1, 182207137, 1, 0, 0); // 1
					}
					break;
				}
				case 798385: // Ninis
					switch (env.getDialog()) {
						case START_DIALOG:
							if (var == 1) {
								removeQuestItem(env, 182207137, 1);
								return sendQuestDialog(env, 2375);
							}
						case CHECK_COLLECTED_ITEMS:
							if (var == 1 && player.getInventory().getItemCountByItemId(186000091) == 1) {
                                player.getInventory().tryDecreaseKinah(50000);
								removeQuestItem(env, 186000091, 1);
								changeQuestStep(env, 1, 1, true); // reward
								return sendQuestDialog(env, 5);
							}
							else
								return sendQuestDialog(env, 2716);
					}
					break;
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798385) { // Ninis
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
