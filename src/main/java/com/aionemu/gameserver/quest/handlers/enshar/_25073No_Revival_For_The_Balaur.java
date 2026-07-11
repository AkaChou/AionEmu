package com.aionemu.gameserver.quest.handlers.enshar;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 恩沙尔任务脚本：No Revival For The Balaur（任务 ID 25073）。
 * Enshar quest script: No Revival For The Balaur (quest ID 25073).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25073No_Revival_For_The_Balaur extends QuestHandler {

	private static final int questId = 25073;
	public _25073No_Revival_For_The_Balaur() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(804918).addOnQuestStart(questId);
		qe.registerQuestNpc(804918).addOnTalkEvent(questId);
		qe.registerQuestNpc(731556).addOnTalkEvent(questId);
		qe.registerQuestNpc(804732).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804918) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		} else if (targetId == 731556) {
				if (dialog == QuestDialog.USE_OBJECT) {
					if (player.getInventory().getItemCountByItemId(182215725) == 1) {
						return sendQuestDialog(env, 1011);
					}
				}
				else if (dialog == QuestDialog.SET_REWARD) {
					removeQuestItem(env, 182215725, 1);
					qs.setStatus(QuestStatus.REWARD);
					changeQuestStep(env, 0, 1, false);
					updateQuestStatus(env);
					return closeDialogWindow(env);
				}
			}	
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804732) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
