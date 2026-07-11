package com.aionemu.gameserver.quest.handlers.sanctum;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 圣所任务脚本：Tothe Galleriaof Grandeur（任务 ID 3965）。
 * Sanctum quest script: Tothe Galleriaof Grandeur (quest ID 3965).
 *
 * @author Rolandas
 * @reworked vlog
 */
public class _3965TotheGalleriaofGrandeur extends QuestHandler {

	private final static int questId = 3965;
	public _3965TotheGalleriaofGrandeur() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(798311).addOnQuestStart(questId);
		qe.registerQuestNpc(798311).addOnTalkEvent(questId);
		qe.registerQuestNpc(798391).addOnTalkEvent(questId);
		qe.registerQuestNpc(798390).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (env.getTargetId() == 798311) { // Senarinrinerk
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				}
				else {
					return sendQuestStartDialog(env, 182206120, 2);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			switch (env.getTargetId()) {
				case 798391: { // Andu
					switch (env.getDialog()) {
						case START_DIALOG: {
							if (var == 0) {
								return sendQuestDialog(env, 1352);
							}
						}
						case STEP_TO_1: {
							return defaultCloseDialog(env, 0, 1, 0, 0, 182206120, 1); // 1
						}
					}
					break;
				}
				case 798390: { // Palentine
					switch (env.getDialog()) {
						case START_DIALOG: {
							if (var == 1) {
								return sendQuestDialog(env, 2375);
							}
						}
						case SELECT_REWARD: {
							changeQuestStep(env, 1, 1, true);
							removeQuestItem(env, 182206120, 1);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		}
		else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (env.getTargetId() == 798390) { // Palentine
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
