package com.aionemu.gameserver.quest.handlers.inggison_armor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 因吉森防具任务脚本：Paris I Love You（任务 ID 11051）。
 * Inggison armor quest script: Paris I Love You (quest ID 11051).
 */
public class _11051Paris_I_Love_You extends QuestHandler {

	private final static int questId = 11051;
	public _11051Paris_I_Love_You() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(798989).addOnQuestStart(questId); //Corocota
		qe.registerQuestNpc(798989).addOnTalkEvent(questId); //Corocota
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 798989) { //Corocota
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.START) {
			if (targetId == 798989) { //Corocota
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 2375);
				} else if (dialog == QuestDialog.CHECK_COLLECTED_ITEMS) {
					long itemCount = player.getInventory().getItemCountByItemId(182206836);
					if (player.getInventory().tryDecreaseKinah(50000) && itemCount > 29) {
						player.getInventory().decreaseByItemId(182206836, 30);
						changeQuestStep(env, 0, 0, true);
						return sendQuestDialog(env, 5);
					} else
						return sendQuestDialog(env, 2716);
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798989) //Corocota
				return sendQuestEndDialog(env);
		}
		return false;
	}
}
