package com.aionemu.gameserver.quest.handlers.beluslan;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 贝勒斯兰任务脚本：The Starving Sprigg（任务 ID 2527）。
 * Beluslan quest script: The Starving Sprigg (quest ID 2527). Collect Food Pouches and take them to Gark (204811).
 *
 * @author vlog
 */
public class _2527TheStarvingSprigg extends QuestHandler {

	private final static int questId = 2527;
	public _2527TheStarvingSprigg() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204811).addOnQuestStart(questId);
		qe.registerQuestNpc(204811).addOnTalkEvent(questId);
		qe.registerQuestNpc(700328).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204811) { // Gark
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				}
				else {
					return sendQuestStartDialog(env);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 204811) { // Gark
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 2375);
				}
				else if (dialog == QuestDialog.CHECK_COLLECTED_ITEMS) {
					return checkQuestItems(env, 0, 0, true, 5, 2716); // reward
				}
			}
			else if (targetId == 700328) { // Food Pouche
				if (dialog == QuestDialog.USE_OBJECT) {
					return true; // loot
				}
			}
		}
		else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204811) { // Gark
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
