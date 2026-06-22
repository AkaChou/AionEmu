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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with aion-unique.  If not, see <http://www.gnu.org/licenses/>.
 */
package quest.eltnen;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * @author Balthazar
 */
public class _1467TheFourLeaders extends QuestHandler {

	private final static int questId = 1467;
	private int reward;
	public _1467TheFourLeaders() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204045).addOnQuestStart(questId);
		qe.registerQuestNpc(204045).addOnTalkEvent(questId);
		qe.registerQuestNpc(211696).addOnKillEvent(questId);
		qe.registerQuestNpc(211697).addOnKillEvent(questId);
		qe.registerQuestNpc(211698).addOnKillEvent(questId);
		qe.registerQuestNpc(211699).addOnKillEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204045) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ASK_ACCEPTION: {
						return sendQuestDialog(env, 4);
					} 
					case ACCEPT_QUEST: {
                        return sendQuestStartDialog(env);
					}
					case REFUSE_QUEST: {
				        return closeDialogWindow(env);
					}
				}
			}
		}
		if (qs == null)
			return false;
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 204045) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					case STEP_TO_1: {
						qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
						reward = 0;
					    updateQuestStatus(env);
				        return closeDialogWindow(env);
					}
					case STEP_TO_2: {
						qs.setQuestVarById(0, qs.getQuestVarById(0) + 2);
						reward = 1;
						updateQuestStatus(env);
				        return closeDialogWindow(env);
					}
					case STEP_TO_3: {
						qs.setQuestVarById(0, qs.getQuestVarById(0) + 3);
						reward = 2;
						updateQuestStatus(env);
				        return closeDialogWindow(env);
					}
					case STEP_TO_4: {
						qs.setQuestVarById(0, qs.getQuestVarById(0) + 4);
						reward = 3;
						updateQuestStatus(env);
				        return closeDialogWindow(env);
					}
                }
            }
        }
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204045)
               return sendQuestEndDialog(env, reward);
		}
		return false;
	}

	@Override
	public boolean onKillEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() != QuestStatus.START) {
			return false;
		}
		int var = 0;
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		switch (targetId) {
			case 211696: {
				if (qs.getQuestVarById(0) == 1) {
					if (var == 0) {
						var = 1;
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return true;
					}
				}
			}
			case 211697: {
				if (qs.getQuestVarById(0) == 2) {
					if (var == 0) {
						var = 1;
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return true;
					}
				}
			}
			case 211698: {
				if (qs.getQuestVarById(0) == 3) {
					if (var == 0) {
						var = 1;
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return true;
					}
				}
			}
			case 211699: {
				if (qs.getQuestVarById(0) == 4) {
					if (var == 0) {
						var = 1;
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return true;
					}
				}
			}
		}
		return false;
	}
}