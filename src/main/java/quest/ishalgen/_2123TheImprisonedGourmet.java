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
package quest.ishalgen;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * @author vlog
 */
public class _2123TheImprisonedGourmet extends QuestHandler {

	private final static int questId = 2123;
	private int rewardId;
	public _2123TheImprisonedGourmet() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203550).addOnQuestStart(questId);
		qe.registerQuestNpc(203550).addOnTalkEvent(questId);
		qe.registerQuestNpc(700128).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 203550) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					default: {
						return sendQuestStartDialog(env);
					}
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 203550: {
					switch (env.getDialog()) {
						case START_DIALOG: {
			                boolean itemCount = player.getInventory().getItemCountByItemId(182203121) >= 1;
			                boolean itemCount1 = player.getInventory().getItemCountByItemId(182203122) >= 1;
			                boolean itemCount2 = player.getInventory().getItemCountByItemId(182203123) >= 1;
							return sendQuestDialog(env, 1352);
						}
						case STEP_TO_1: {
							if (player.getInventory().getItemCountByItemId(182203121) >= 1) {
					            rewardId = 0;
                                deleteQuestItems(player, new int[]{182203121, 182203122, 182203123});
								qs.setStatus(QuestStatus.REWARD);
								updateQuestStatus(env);
								return sendQuestDialog(env, 5);
							}
							else {
								return sendQuestDialog(env, 1693);
							}
						}
						case STEP_TO_2: {
							if (player.getInventory().getItemCountByItemId(182203122) >= 1) {
					            rewardId = 1;
                                deleteQuestItems(player, new int[]{182203121, 182203122, 182203123});
								qs.setStatus(QuestStatus.REWARD);
								updateQuestStatus(env);
								return sendQuestDialog(env, 6);
							}
							else {
								return sendQuestDialog(env, 1693);
							}
						}
						case STEP_TO_3: {
							if (player.getInventory().getItemCountByItemId(182203123) >= 1) {
					            rewardId = 2;
                                deleteQuestItems(player, new int[]{182203121, 182203122, 182203123});
								qs.setStatus(QuestStatus.REWARD);
								updateQuestStatus(env);
								return sendQuestDialog(env, 7);
							}
							else {
								return sendQuestDialog(env, 1693);
							}
						}
					}
					break;
				}
				case 700128: {
					return true;
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203550) { // Munin
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