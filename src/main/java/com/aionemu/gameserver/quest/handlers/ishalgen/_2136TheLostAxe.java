package com.aionemu.gameserver.quest.handlers.ishalgen;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 伊沙尔根任务脚本：The Lost Axe（任务 ID 2136）。
 * Ishalgen quest script: The Lost Axe (quest ID 2136).
 *
 * @author Rhys2002
 * @modified Hellboy
 */
public class _2136TheLostAxe extends QuestHandler {

	private final static int questId = 2136;
	private int rewardId;
	public _2136TheLostAxe() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestItem(182203130, questId);
		qe.registerQuestNpc(700146).addOnTalkEvent(questId);
		qe.registerQuestNpc(790009).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (env.getDialog() == QuestDialog.ACCEPT_QUEST) {
				return sendQuestStartDialog(env);
			}
			if (env.getDialog() == QuestDialog.REFUSE_QUEST) {
				return closeDialogWindow(env);
			}
		}
		if (qs == null)
			return false;
		if (qs.getStatus() == QuestStatus.START) {
		int var = qs.getQuestVarById(0);
		switch (targetId) {
		case 700146: {
			switch (env.getDialog()) {
				case USE_OBJECT:
					if (var == 0)
						playQuestMovie(env, 59);
						qs.setQuestVarById(0, 1);
						updateQuestStatus(env);
						QuestService.addNewSpawn(220010000, player.getInstanceId(), 790009, 1080.1555f, 2374.5107f, 247.75f, (byte) 73);
						return true;
			}	
        }
		case 790009: {
			switch (env.getDialog()) {
				case START_DIALOG: {
					if (var == 1)
						return sendQuestDialog(env, 1011);
                } 
				case STEP_TO_1:
					if (var == 1)
					    rewardId = 0;
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						removeQuestItem(env, 182203130, 1);
						return sendQuestDialog(env, 5);
				case STEP_TO_2:
					if (var == 1)
					    rewardId = 1;
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						removeQuestItem(env, 182203130, 1);
						return sendQuestDialog(env, 6);
			        }
		        }
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 790009) {
				final Npc npc = (Npc) env.getVisibleObject();
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						npc.getController().onDelete();
					}
				}, 10000);
				return sendQuestEndDialog(env, rewardId);
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
