package com.aionemu.gameserver.quest.handlers.cygnea;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 希格尼娅任务脚本：Shelling Out Crystals（任务 ID 15070）。
 * Cygnea quest script: Shelling Out Crystals (quest ID 15070).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _15070Shelling_Out_Crystals extends QuestHandler {

	private static final int questId = 15070;
	public _15070Shelling_Out_Crystals() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(804891).addOnQuestStart(questId);
		qe.registerQuestNpc(804891).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804891) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 804891) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					} case CHECK_COLLECTED_ITEMS: {
						return checkQuestItems(env, 0, 0, true, 5, 10001);
					}
				}
			}
		} else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804891) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
