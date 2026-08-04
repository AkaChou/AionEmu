package com.aionemu.gameserver.quest.handlers.norsvold;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/** 诺斯沃尔德任务脚本：Onward To Illuma（任务 ID 25566）。 */
public class _25566Onward_To_Illuma extends QuestHandler {
	private static final int QUEST_ID = 25566;

	public _25566Onward_To_Illuma() {
		super(QUEST_ID);
	}

	@Override
	public void register() {
		qe.registerOnEnterWorld(QUEST_ID);
		qe.registerOnKillInWorld(210100000, QUEST_ID);
		qe.registerQuestNpc(806116).addOnTalkEvent(QUEST_ID);
	}

	@Override
	public boolean onEnterWorldEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState state = player.getQuestStateList().getQuestState(QUEST_ID);
		if (player.getWorldId() == 210100000 && state == null) {
			env.setQuestId(QUEST_ID);
			return QuestService.startQuest(env);
		}
		return false;
	}

	@Override
	public boolean onKillInWorldEvent(QuestEnv env) {
		Player player = env.getPlayer();
		if (env.getVisibleObject() instanceof Player victim && player != null
				&& player.getLevel() >= victim.getLevel() - 5
				&& player.getLevel() <= victim.getLevel() + 9) {
			return defaultOnKillRankedEvent(env, 0, 5, true);
		}
		return false;
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState state = player.getQuestStateList().getQuestState(QUEST_ID);
		if ((state == null || state.getStatus() == QuestStatus.REWARD) && env.getTargetId() == 806116) {
			if (env.getDialog() == QuestDialog.START_DIALOG) {
				return sendQuestDialog(env, 10002);
			}
			if (env.getDialog() == QuestDialog.SELECT_REWARD) {
				return sendQuestDialog(env, 5);
			}
			return sendQuestEndDialog(env);
		}
		return false;
	}
}
