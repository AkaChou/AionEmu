package com.aionemu.gameserver.quest.handlers.drakenspire_depths;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 龙脊深渊任务脚本：Mind Your Business（任务 ID 18953）。
 * Drakenspire Depths quest script: Mind Your Business (quest ID 18953).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _18953Mind_Your_Business extends QuestHandler {

	private static final int questId = 18953;
	public _18953Mind_Your_Business() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(209678).addOnQuestStart(questId);
		qe.registerQuestNpc(209678).addOnTalkEvent(questId);
		qe.registerQuestNpc(804711).addOnTalkEvent(questId);
		qe.registerQuestNpc(702769).addOnTalkEvent(questId); //Ominous Darkness.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 209678) {
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
		} else if (targetId == 702769) { //Ominous Darkness.
			if (dialog == QuestDialog.USE_OBJECT) {
				closeDialogWindow(env);
				return true;
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
