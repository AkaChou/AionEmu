/*
 * This file is part of aion-lightning <aion-lightning.com>.
 *
 *  aion-lightning is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  aion-lightning is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with aion-lightning.  If not, see <http://www.gnu.org/licenses/>.
 */
package quest.beluslan;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * @author VladimirZ
 */
public class _2505LetCookingExpertsCook extends QuestHandler {

	private final static int questId = 2505;
	public _2505LetCookingExpertsCook() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204720).addOnQuestStart(questId);
		qe.registerQuestNpc(204720).addOnTalkEvent(questId);
		qe.registerQuestNpc(204731).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
		    if (targetId == 204720) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else {
					return sendQuestStartDialog(env, 182204404, 1);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
		int var = qs.getQuestVarById(0);
		if (targetId == 204731) {
			switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 0)
						return sendQuestDialog(env, 1352);
				case STEP_TO_1:
					if (var == 0) {
						removeQuestItem(env, 182204404, 1);
						giveQuestItem(env, 182204405, 1);
                        qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
						updateQuestStatus(env);
                        return closeDialogWindow(env);
					}
			    }
			}
		if (targetId == 204720) {
			switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 1)
						return sendQuestDialog(env, 2375);
				case SELECT_REWARD:
					if (var == 1) {
						removeQuestItem(env, 182204405, 1);
                        qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
					    return sendQuestEndDialog(env);
					}
			    }
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204720) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}