package com.aionemu.gameserver.quest.handlers.drakenspire_depths;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 龙脊深渊任务脚本：Hand It To The Expedition（任务 ID 18954）。
 * Drakenspire Depths quest script: Hand It To The Expedition (quest ID 18954).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _18954Hand_It_To_The_Expedition extends QuestHandler {

	private static final int questId = 18954;
	public _18954Hand_It_To_The_Expedition() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(804711).addOnQuestStart(questId);
		qe.registerQuestNpc(804711).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804711) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 804711) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					} case CHECK_COLLECTED_ITEMS: {
						return checkQuestItems(env, 0, 0, true, 5, 2716);
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804711) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
