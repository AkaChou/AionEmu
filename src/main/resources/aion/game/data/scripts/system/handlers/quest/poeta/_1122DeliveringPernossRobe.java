/*
 * This file is part of aion-unique <aion-unique.org>.
 *
 * aion-unique is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * aion-unique is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with aion-unique. If not, see <http://www.gnu.org/licenses/>.
 */
package quest.poeta;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * @author MrPoke
 */
public class _1122DeliveringPernossRobe extends QuestHandler {

	private final static int questId = 1122;
	private int rewardId;
	public _1122DeliveringPernossRobe() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203060).addOnQuestStart(questId);
		qe.registerQuestNpc(203060).addOnTalkEvent(questId);
		qe.registerQuestNpc(790001).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
		   if (targetId == 203060) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else if (env.getDialogId() == 1007) {
					return sendQuestDialog(env, 4);
                }
				else if (env.getDialogId() == 1002) {
					return sendQuestStartDialog(env, 182200216, 1);
				}
			}
		}
		else if (targetId == 790001) {
			if (qs != null && qs.getStatus() == QuestStatus.START) {
				long itemCount;
				switch (env.getDialog()) {
					case START_DIALOG:
						return sendQuestDialog(env, 1352);
					case STEP_TO_1:
						itemCount = player.getInventory().getItemCountByItemId(182200218);
						if (itemCount > 0) {
                            deleteQuestItems(player, new int[]{182200218, 182200219, 182200220});
					        rewardId = 0;
				            qs.setQuestVarById(0, 1);
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							return sendQuestDialog(env, 5);
						}
						else
							return sendQuestDialog(env, 1608);

					case STEP_TO_2:
						itemCount = player.getInventory().getItemCountByItemId(182200219);
						if (itemCount > 0) {
                            deleteQuestItems(player, new int[]{182200218, 182200219, 182200220});
					        rewardId = 1;
				            qs.setQuestVarById(0, 1);
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							return sendQuestDialog(env, 6);
						}
						else
							return sendQuestDialog(env, 1608);
					case STEP_TO_3:
						itemCount = player.getInventory().getItemCountByItemId(182200220);
						if (itemCount > 0) {
                            deleteQuestItems(player, new int[]{182200218, 182200219, 182200220});
					        rewardId = 2;
				            qs.setQuestVarById(0, 1);
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							return sendQuestDialog(env, 7);
						}
						else
							return sendQuestDialog(env, 1608);
				}
			}
			else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
				return sendQuestEndDialog(env, rewardId);
			}
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