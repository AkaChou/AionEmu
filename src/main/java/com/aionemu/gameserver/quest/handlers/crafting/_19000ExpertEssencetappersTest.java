package com.aionemu.gameserver.quest.handlers.crafting;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 制作任务脚本：Expert Essencetappers Test（任务 ID 19000）。
 * Crafting quest script: Expert Essencetappers Test (quest ID 19000).
 */
public class _19000ExpertEssencetappersTest extends QuestHandler {

	private final static int questId = 19000;
	public _19000ExpertEssencetappersTest() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(203780).addOnQuestStart(questId);
		qe.registerQuestNpc(203780).addOnTalkEvent(questId);
		qe.registerQuestNpc(203781).addOnTalkEvent(questId);
		qe.registerGetingItem(122001250, questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 203780) { //Cornelius.
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			switch (targetId) {
				case 203781: { //Sabotes.
					switch (dialog) {
						case START_DIALOG: {
							if (var == 0) {
								return sendQuestDialog(env, 1011);
							}
						} case STEP_TO_1: {
							giveQuestItem(env, 122001250, 1);
						    return closeDialogWindow(env);
						}
					}
					break;
				} case 203780: { //Cornelius.
					switch (dialog) {
						case START_DIALOG: {
							if (var == 1) {
								return sendQuestDialog(env, 1352);
							}
						} case CHECK_COLLECTED_ITEMS: {
							return checkQuestItems(env, 1, 1, true, 5, 10001);
						}
					}
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203780) { //Cornelius.
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
	
	@Override
	public boolean onGetItemEvent(QuestEnv env) {
		return defaultOnGetItemEvent(env, 0, 1, false);
	}
}
