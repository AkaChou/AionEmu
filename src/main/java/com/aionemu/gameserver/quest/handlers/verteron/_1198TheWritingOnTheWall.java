package com.aionemu.gameserver.quest.handlers.verteron;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 沃特伦任务脚本：The Writing On The Wall（任务 ID 1198）。
 * Verteron quest script: The Writing On The Wall (quest ID 1198).
 *
 * @author Cheatkiller
 */
public class _1198TheWritingOnTheWall extends QuestHandler {

	private final static int questId = 1198;
	public _1198TheWritingOnTheWall() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestItem(182200559, questId);
		qe.registerQuestNpc(700009).addOnTalkEvent(questId);
		qe.registerQuestNpc(203098).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 0) {
				if (dialog == QuestDialog.ACCEPT_QUEST) {
				    return sendQuestStartDialog(env);
				}
				else if (dialog == QuestDialog.REFUSE_QUEST) {
					return closeDialogWindow(env);
				}
			}
			else if (targetId == 700009) {
				return giveQuestItem(env, 182200559, 1);
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 203098) {
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 2375);
				}
				else if (dialog == QuestDialog.SELECT_REWARD) {
					return defaultCloseDialog(env, 0, 1, true, true);
				}
			}
		}
		else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203098) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}

	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			return HandlerResult.fromBoolean(sendQuestDialog(env, 4));
		}
		return HandlerResult.FAILED;
	}
}
