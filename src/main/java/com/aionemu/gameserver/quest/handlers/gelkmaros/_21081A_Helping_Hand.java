package com.aionemu.gameserver.quest.handlers.gelkmaros;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 格尔克马洛斯任务脚本：A Helping Hand（任务 ID 21081）。
 * Gelkmaros quest script: A Helping Hand (quest ID 21081).
 *
 * @author zhkchi
 */
public class _21081A_Helping_Hand extends QuestHandler {

	private final static int questId = 21081;
	public _21081A_Helping_Hand() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(799225).addOnQuestStart(questId); // Richelle
		qe.registerQuestNpc(799225).addOnTalkEvent(questId); // Richelle
		qe.registerQuestNpc(799332).addOnTalkEvent(questId); // Agovard
		qe.registerQuestNpc(799217).addOnTalkEvent(questId); // Renato
		qe.registerQuestNpc(799202).addOnTalkEvent(questId); // Ipses
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 799225) {
				switch (dialog) {
					case START_DIALOG:
						return sendQuestDialog(env, 1011);
					case ASK_ACCEPTION: {
						return sendQuestDialog(env, 4);
					} 
					case ACCEPT_QUEST: {
						return sendQuestStartDialog(env, 182214017, 1);
					}
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 799332: { // Brontes
					switch (dialog) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1353);
						}
						case SELECT_ACTION_1353: {
							return sendQuestDialog(env, 1353);
						}
						case STEP_TO_1: {
							return defaultCloseDialog(env, 0, 1);
						}
					}
				}
				case 799217: { // Pilipides
					switch (dialog) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1693);
						}
						case SELECT_ACTION_1694: {
							return sendQuestDialog(env, 1694);
						}
						case STEP_TO_2: {
							return defaultCloseDialog(env, 1, 2);
						}
					}
				}
				case 799202: { // Drenia
					switch (dialog) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2375);
						}
						case SELECT_REWARD: {
							removeQuestItem(env, 182214017, 1);
							changeQuestStep(env, 2, 3, true);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799202) { // Drenia
                return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
