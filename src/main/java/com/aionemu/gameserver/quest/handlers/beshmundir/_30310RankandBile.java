package com.aionemu.gameserver.quest.handlers.beshmundir;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 贝希蒙迪尔任务脚本：Rankand Bile（任务 ID 30310）。
 * Beshmundir quest script: Rankand Bile (quest ID 30310).
 *
 * @author Gigi
 */
public class _30310RankandBile extends QuestHandler {

	private final static int questId = 30310;
	public _30310RankandBile() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204225).addOnQuestStart(questId);
		qe.registerQuestNpc(204225).addOnTalkEvent(questId);
		qe.registerQuestNpc(799322).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		QuestDialog dialog = env.getDialog();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204225) {
				if (dialog == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 4762);
				else
					return sendQuestStartDialog(env);
			}
		}
		
		if (qs == null)
			return false;

		if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (targetId == 204225) {
				if (dialog == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else if (dialog == QuestDialog.CHECK_COLLECTED_ITEMS) {
					if (var == 0 && player.getInventory().getItemCountByItemId(182209713) >= 40) {
						removeQuestItem(env, 182209713, 40);
						changeQuestStep(env, 0, 0, true, 0);
						return sendQuestDialog(env, 10000);
					}
					else
						return sendQuestDialog(env, 10001);
				}
				return false;
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799322) {
				switch (dialog) {
					case USE_OBJECT:
						return sendQuestDialog(env, 10002);
					case SELECT_REWARD:
						return sendQuestDialog(env, 5);
					default:
						return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}
