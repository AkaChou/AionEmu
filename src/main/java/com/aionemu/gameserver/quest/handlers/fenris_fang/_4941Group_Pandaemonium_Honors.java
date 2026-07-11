package com.aionemu.gameserver.quest.handlers.fenris_fang;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 芬里尔之牙任务脚本：Group Pandaemonium Honors（任务 ID 4941）。
 * Fenris Fang quest script: Group Pandaemonium Honors (quest ID 4941).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _4941Group_Pandaemonium_Honors extends QuestHandler {

	private final static int questId = 4941;
	public _4941Group_Pandaemonium_Honors() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(204060).addOnQuestStart(questId);
		qe.registerQuestNpc(204060).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || (qs.getStatus() == QuestStatus.NONE || qs.getStatus() == QuestStatus.COMPLETE)) {
			if (targetId == 204060) {
				if (dialog == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 4762);
				else
					return sendQuestStartDialog(env);
			}
		} if (qs == null)
			return false;
		int var = qs.getQuestVarById(0);
		if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 204060:
					switch (dialog) {
						case START_DIALOG:
							if (var == 0)
								return sendQuestDialog(env, 1011);
							else if (var == 1)
								return sendQuestDialog(env, 1352);
						case STEP_TO_1:
							return defaultCloseDialog(env, 0, 1);
						case CHECK_COLLECTED_ITEMS:
							long itemCount1 = player.getInventory().getItemCountByItemId(182207121);
							if (itemCount1 >= 1) {
								removeQuestItem(env, 182207121, 1);
								changeQuestStep(env, 1, 1, true);
								return sendQuestDialog(env, 5);
							}
							else
								return sendQuestDialog(env, 10001);
					}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204060)
				return sendQuestEndDialog(env);
		}
		return false;
	}
}
