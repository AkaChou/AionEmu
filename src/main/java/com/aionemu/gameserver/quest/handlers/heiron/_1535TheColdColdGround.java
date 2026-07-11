package com.aionemu.gameserver.quest.handlers.heiron;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 希隆任务脚本：The Cold Cold Ground（任务 ID 1535）。
 * Heiron quest script: The Cold Cold Ground (quest ID 1535).
 *
 * @author Rolandas
 */
public class _1535TheColdColdGround extends QuestHandler {

	private final static int questId = 1535;
	private int rewardId;
	public _1535TheColdColdGround() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204580).addOnQuestStart(questId);
		qe.registerQuestNpc(204580).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (targetId != 204580)
			return false;
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (env.getDialog() == QuestDialog.START_DIALOG)
				return sendQuestDialog(env, 4762);
			else
				return sendQuestStartDialog(env);
		}
		if (qs.getStatus() == QuestStatus.START) {
			boolean itemCount = player.getInventory().getItemCountByItemId(182201818) > 4;
			boolean itemCount1 = player.getInventory().getItemCountByItemId(182201819) > 2;
			boolean itemCount2 = player.getInventory().getItemCountByItemId(182201820) > 0;
			switch (env.getDialog()) {
				case USE_OBJECT:
				case START_DIALOG:
					if (itemCount || itemCount1 || itemCount2)
						return sendQuestDialog(env, 1352);
				case STEP_TO_1:
					if (itemCount) {
                        deleteQuestItems(player, new int[]{182201818, 182201819, 182201820});
					    rewardId = 0;
						qs.setQuestVarById(0, 1);
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return sendQuestDialog(env, 5);
					}
					break;
				case STEP_TO_2:
					if (itemCount1) {
                        deleteQuestItems(player, new int[]{182201818, 182201819, 182201820});
					    rewardId = 1;
						qs.setQuestVarById(0, 1);
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return sendQuestDialog(env, 6);
					}
					break;
				case STEP_TO_3:
					if (itemCount2) {
                        deleteQuestItems(player, new int[]{182201818, 182201819, 182201820});
					    rewardId = 2;
						qs.setQuestVarById(0, 1);
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return sendQuestDialog(env, 7);
					}
					break;
			}
			return sendQuestDialog(env, 1693);
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			return sendQuestEndDialog(env, rewardId);
		}
		return false;
	}

    private void deleteQuestItems(Player player, int... itemIds) {
        for (int itemId : itemIds) {
            long count = player.getInventory().getItemCountByItemId(itemId);
            if (count > 0) {
                player.getInventory().decreaseByItemId(itemId, count);
            }
        }
    }
}
