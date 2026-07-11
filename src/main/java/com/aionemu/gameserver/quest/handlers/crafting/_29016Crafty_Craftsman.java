package com.aionemu.gameserver.quest.handlers.crafting;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 制作任务脚本：Crafty Craftsman（任务 ID 29016）。
 * Crafting quest script: Crafty Craftsman (quest ID 29016).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _29016Crafty_Craftsman extends QuestHandler {

	private static final int questId = 29016;
	public _29016Crafty_Craftsman() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(204106).addOnQuestStart(questId);
		qe.registerQuestNpc(204106).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204106) {
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env, 152232013, 1);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 204106) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					} case CHECK_COLLECTED_ITEMS: {
						if (QuestService.collectItemCheck(env, true)) {
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							return sendQuestDialog(env, 10000);
						} else {
							return sendQuestDialog(env, 10001);
						}
					}
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204106) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
