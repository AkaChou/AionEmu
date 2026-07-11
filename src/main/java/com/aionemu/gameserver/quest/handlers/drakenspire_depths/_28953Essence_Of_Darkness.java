package com.aionemu.gameserver.quest.handlers.drakenspire_depths;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 龙脊深渊任务脚本：Essence Of Darkness（任务 ID 28953）。
 * Drakenspire Depths quest script: Essence Of Darkness (quest ID 28953).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _28953Essence_Of_Darkness extends QuestHandler {

	private static final int questId = 28953;
	public _28953Essence_Of_Darkness() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(209743).addOnQuestStart(questId);
		qe.registerQuestNpc(209743).addOnTalkEvent(questId);
		qe.registerQuestNpc(804738).addOnTalkEvent(questId);
		qe.registerQuestNpc(702769).addOnTalkEvent(questId); //Ominous Darkness.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 209743) {
				switch (env.getDialog()) {
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
			if (env.getDialog() == QuestDialog.USE_OBJECT) {
				closeDialogWindow(env);
				return true;
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 804738) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					} case CHECK_COLLECTED_ITEMS: {
						return checkQuestItems(env, 0, 0, true, 5, 2716);
					}
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804738) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
