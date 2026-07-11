package com.aionemu.gameserver.quest.handlers.gelkmaros_armor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 格尔克马洛斯防具任务脚本：Troublein Stone（任务 ID 21051）。
 * Gelkmaros armor quest script: Troublein Stone (quest ID 21051).
 */
public class _21051TroubleinStone extends QuestHandler {

	private final static int questId = 21051;
	public _21051TroubleinStone() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(799291).addOnQuestStart(questId); //Aquila.
		qe.registerQuestNpc(799291).addOnTalkEvent(questId); //Aquila.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 799291) { //Aquila.
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					default: {
						return sendQuestStartDialog(env);
					}
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.START) {
			if (targetId == 799291) { //Aquila.
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 2375);
				} else if (dialog == QuestDialog.CHECK_COLLECTED_ITEMS) {
					long itemCount = player.getInventory().getItemCountByItemId(182207839);
					if (player.getInventory().tryDecreaseKinah(50000) && itemCount > 29) {
						player.getInventory().decreaseByItemId(182207839, 30);
						changeQuestStep(env, 0, 0, true);
						return sendQuestDialog(env, 5);
					} else
						return sendQuestDialog(env, 2716);
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799291) { //Aquila.
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
