/*
 * This file is part of aion-engine <aion-engine.com>
 *
 *  aion-engine is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  aion-engine is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with aion-engine.  If not, see <http://www.gnu.org/licenses/>.
 */
package quest.inggison;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * @author dta3000
 * @reworked Gigi
 */

public class _11010AngelToTheWounded extends QuestHandler {

	private final static int questId = 11010;
	public _11010AngelToTheWounded() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(798931).addOnQuestStart(questId);
		qe.registerQuestNpc(798931).addOnTalkEvent(questId);
		qe.registerQuestNpc(799071).addOnTalkEvent(questId);
		qe.registerQuestNpc(798906).addOnTalkEvent(questId);
		qe.registerQuestNpc(730323).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 798931) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
		}
		if (qs == null)
			return false;
		else if (qs == null || qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			switch (targetId) {
				case 799071:
					switch (env.getDialog()) {
						case START_DIALOG:
						if (var == 0)
							return sendQuestDialog(env, 1352);
						else if (var == 3)
						return sendQuestDialog(env, 2375);
						case STEP_TO_1:
							return defaultCloseDialog(env, 0, 1); // 1
					    case SELECT_REWARD:
					       qs.setStatus(QuestStatus.REWARD);
					       updateQuestStatus(env);
					       return sendQuestEndDialog(env);
				}
				case 798906:
					switch (env.getDialog()) {
						case START_DIALOG:
						if (var == 1)
							return sendQuestDialog(env, 1693);
						case STEP_TO_2:
							return defaultCloseDialog(env, 1, 2); // 2
				}
				case 730323:
					switch (env.getDialog()) {
					case USE_OBJECT:
						if (var == 2)
						return sendQuestDialog(env, 2034);
					case STEP_TO_3:
						return defaultCloseDialog(env, 2, 3);
                }
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799071) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}