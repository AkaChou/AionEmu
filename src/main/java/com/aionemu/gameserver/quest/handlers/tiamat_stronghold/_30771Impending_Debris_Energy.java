package com.aionemu.gameserver.quest.handlers.tiamat_stronghold;

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
import com.aionemu.gameserver.world.zone.ZoneName;

public class _30771Impending_Debris_Energy extends QuestHandler {

	private static final int questId = 30771;

	public _30771Impending_Debris_Energy() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(804728).addOnQuestStart(questId);
		qe.registerQuestNpc(804728).addOnTalkEvent(questId);
		qe.registerQuestNpc(804871).addOnTalkEvent(questId);
		qe.registerQuestNpc(804869).addOnTalkEvent(questId);
		qe.registerQuestItem(182215699, questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		QuestState qs = env.getPlayer().getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804728) {
				return dialog == QuestDialog.START_DIALOG ? sendQuestDialog(env, 4762) : sendQuestStartDialog(env);
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (targetId == 804871 && var == 0) {
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				}
				if (dialog == QuestDialog.STEP_TO_1) {
					return defaultCloseDialog(env, 0, 1);
				}
			} else if (targetId == 804869) {
				if (dialog == QuestDialog.START_DIALOG) {
					if (var == 1) {
						return sendQuestDialog(env, 1352);
					}
					if (var == 3) {
						return sendQuestDialog(env, 2034);
					}
				}
				if (dialog == QuestDialog.STEP_TO_2 && var == 1) {
					giveQuestItem(env, 182215699, 1);
					return defaultCloseDialog(env, 1, 2);
				}
				if (dialog == QuestDialog.SET_REWARD && var == 3) {
					return defaultCloseDialog(env, 3, 4, true, false);
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD && targetId == 804871) {
			return dialog == QuestDialog.START_DIALOG ? sendQuestDialog(env, 10002) : sendQuestEndDialog(env);
		}
		return false;
	}

	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() != QuestStatus.START || qs.getQuestVarById(0) != 2
				|| !player.isInsideZone(ZoneName.get("DF5_ITEMUSEAREA_Q30771"))) {
			return HandlerResult.FAILED;
		}
		boolean accepted = useQuestItem(env, item, 2, 3, false);
		if (accepted) {
			GameThreadPoolServices.threadPoolManager().schedule(() -> {
				Npc first = (Npc) QuestService.addNewSpawnForSeconds(player.getWorldId(), player.getInstanceId(), 236654,
						player.getX() - 2, player.getY(), player.getZ(), player.getHeading(), 120);
				Npc second = (Npc) QuestService.addNewSpawnForSeconds(player.getWorldId(), player.getInstanceId(), 236654,
						player.getX() + 2, player.getY(), player.getZ(), player.getHeading(), 120);
				first.getAggroList().addHate(player, 1000);
				second.getAggroList().addHate(player, 1000);
			}, 3000);
		}
		return HandlerResult.fromBoolean(accepted);
	}
}
