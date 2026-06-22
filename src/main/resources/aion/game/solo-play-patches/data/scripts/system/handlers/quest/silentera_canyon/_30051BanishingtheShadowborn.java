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
package quest.silentera_canyon;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/****/
/** MATTYOne DainAvenger Ptich
/****/

public class _30051BanishingtheShadowborn extends QuestHandler {

	private final static int questId = 30051;
    private final static int REQUIRED_KILLS = 7; // summ of NPC needed to kill

    // NPC ID, that needs to kill
    private final static int[] mobs = {205395, 205396, 205397, 205398, 205399, 205400, 205401, 205402, 217476, 217477, 217478, 217479, 217480, 217481, 217482, 217483, 205404, 205405, 205406, 205407, 205408, 205409, 205410, 205411, 217485, 217486, 217487, 217488, 217489, 217490, 217491, 217492};

	public _30051BanishingtheShadowborn() {
		super(questId);
	}

	@Override
	public void register() {
        qe.registerQuestNpc(799381).addOnQuestStart(questId);
        qe.registerQuestNpc(799381).addOnTalkEvent(questId);
        // register NPC in case to kill
        for (int mobId : mobs) {
            qe.registerQuestNpc(mobId).addOnKillEvent(questId);
        }
	}

    @Override
    public boolean onKillEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);

        if (qs == null || qs.getStatus() != QuestStatus.START) {
            return false;
        }

        int targetId = env.getTargetId();
        int killCount = qs.getQuestVarById(1); // Killed NPC
        boolean killCounted = false;

        // If we kill the opposite faction, then:
        if (env.getVisibleObject() instanceof Player && player.getWorldId() == 600110000) {
            Player target = (Player) env.getVisibleObject();
            if ((env.getPlayer().getLevel() >= (target.getLevel() - 5)) && (env.getPlayer().getLevel() <= (target.getLevel() + 9))) {
                killCounted = true;
            }
        }

        // Check NPC ID from list in top
        for (int mobId : mobs) {
            if (targetId == mobId) {
                killCounted = true;
                break;
            }
        }

        if (killCounted) {
            killCount++; // Kill counter ++
            if (killCount >= REQUIRED_KILLS) {
                killCount = REQUIRED_KILLS; // TODO
                qs.setQuestVarById(0, 1);
                qs.setStatus(QuestStatus.REWARD); // If we made MOBS_KILLS = 2 of NPC, then make status REWARD
            }
            qs.setQuestVarById(1, killCount); // Update variable
            updateQuestStatus(env);
            return true;
        }
        return false;
    }

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (env.getTargetId() == 799381) {
			if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
			else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1352);
				else
					return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}