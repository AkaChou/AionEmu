/*
 * This file is part of aion-unique <aion-unique.org>.
 *
 *  aion-unique is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  aion-unique is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with aion-unique.  If not, see <http://www.gnu.org/licenses/>.
 */
package quest.heiron;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
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