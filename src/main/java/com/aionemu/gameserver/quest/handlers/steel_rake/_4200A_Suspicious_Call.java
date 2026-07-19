package com.aionemu.gameserver.quest.handlers.steel_rake;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * 钢耙号任务脚本：A Suspicious Call（任务 ID 4200）。
 * Steel Rake quest script: A Suspicious Call (quest ID 4200).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _4200A_Suspicious_Call extends QuestHandler {

	private final static int questId = 4200;
	private final static int[] npc_ids = {204839, 798332, 700522, 205233, 805839};
	public _4200A_Suspicious_Call() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(204839).addOnQuestStart(questId); //Uikinerk.
		qe.registerQuestItem(182209097, questId); //Teleport Scroll.
		for (int npc_id: npc_ids)
		qe.registerQuestNpc(npc_id).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		} if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204839) { //Uikinerk.
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env);
				}
			}
			return false;
		} if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805839) { //Peorinerk.
				if (env.getDialog() == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 10002);
				} else if (env.getDialogId() == 1009) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
			}
			return false;
		} else if (qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
			if (targetId == 204839) { //Uikinerk.
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1003);
					} case SELECT_ACTION_1011: {
						return sendQuestDialog(env, 1011);
					} case STEP_TO_1: {
						if (!TeleportService2.teleportToInstance(player, 300100000, 403.55f, 508.11f, 885.77f)) {
							return false;
						}
						qs.setQuestVarById(0, var + 1);
						updateQuestStatus(env);
						return true;
					}
				}
			} else if (targetId == 798332 && var == 1) { //Haorunerk.
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1352);
					} case SELECT_ACTION_1353: {
						playQuestMovie(env, 431);
						return sendQuestDialog(env, 1353);
					} case STEP_TO_2: {
						qs.setQuestVarById(0, var + 1);
						updateQuestStatus(env);
					    return closeDialogWindow(env);
					}
				}
			} else if (targetId == 700522 && var == 2) { //Haorunerk's Bag.
				switch (env.getDialog()) {
				    case USE_OBJECT: {
						giveQuestItem(env, 182209097, 1);
						updateQuestStatus(env);
						return closeDialogWindow(env);
					}
				}
			} else if (targetId == 205233 && var == 3) { //Hudrunerk.
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 2034);
					} case SELECT_ACTION_2035: {
						return sendQuestDialog(env, 2035);
					} case SET_REWARD: {
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
					    return closeDialogWindow(env);
					}
				}
			}
		}
		return false;
	}
	
	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null || qs.getQuestVarById(0) != 2) {
			TeleportService2.teleportTo(player, 220040000, 367.9981f, 429.9916f, 222.11166f, (byte) 38);
			qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
			removeQuestItem(env, 182209097, 1); //Teleport Scroll.
			updateQuestStatus(env);
			return HandlerResult.SUCCESS;
		}
		return HandlerResult.FAILED;
	}
}
