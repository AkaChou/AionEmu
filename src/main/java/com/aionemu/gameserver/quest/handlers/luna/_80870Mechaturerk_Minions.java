package com.aionemu.gameserver.quest.handlers.luna;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 露娜任务脚本：Mechaturerk Minions（任务 ID 80870）。
 * Luna quest script: Mechaturerk Minions (quest ID 80870).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _80870Mechaturerk_Minions extends QuestHandler {

	private static final int questId = 80870;
	public _80870Mechaturerk_Minions() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(833825).addOnQuestStart(questId); //Jay.
		qe.registerQuestNpc(834167).addOnTalkEvent(questId); //Jay.
		qe.registerQuestNpc(703375).addOnTalkEvent(questId); //Armored Soldier’s Footlocker.
		qe.registerQuestNpc(703376).addOnTalkEvent(questId); //Maintenance Soldier’s Footlocker.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 833825) { //Jay.
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
		} else if (targetId == 703375) { //Armored Soldier’s Footlocker.
			if (env.getDialog() == QuestDialog.USE_OBJECT) {
				closeDialogWindow(env);
				return true;
			}
		} else if (targetId == 703376) { //Maintenance Soldier’s Footlocker.
			if (env.getDialog() == QuestDialog.USE_OBJECT) {
				closeDialogWindow(env);
				return true;
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 834167) { //Jay.
				switch (env.getDialog()) {
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
			if (targetId == 834167) { //Jay.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 10002);
				} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}
