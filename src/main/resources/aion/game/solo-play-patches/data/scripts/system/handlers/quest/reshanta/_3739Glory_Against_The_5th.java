/*
 * =====================================================================================*
 * This file is part of Aion-Unique (Aion-Unique Home Software Development)             *
 * Aion-Unique Development is a closed Aion Project that use Old Aion Project Base      *
 * Like Aion-Lightning, Aion-Engine, Aion-Core, Aion-Extreme, Aion-NextGen, ArchSoft,   *
 * Aion-Ger, U3J, Encom And other Aion project, All Credit Content                      *
 * That they make is belong to them/Copyright is belong to them. And All new Content    *
 * that Aion-Unique make the copyright is belong to Aion-Unique                         *
 * You may have agreement with Aion-Unique Development, before use this Engine/Source   *
 * You have agree with all of Term of Services agreement with Aion-Unique Development   *
 * =====================================================================================*
 */
package quest.reshanta;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;

/****/
/** MATTYOne DainAvenger Ptich
/****/

public class _3739Glory_Against_The_5th extends QuestHandler {

	private final static int questId = 3739;
    private final static int REQUIRED_KILLS = 1; // summ of NPC needed to kill

    // NPC ID, that needs to kill
    private final static int[] mobs = {205395, 205396, 205397, 205398, 205399, 205400, 205401, 205402, 217476, 217477, 217478, 217479, 217480, 217481, 217482, 217483, 205404, 205405, 205406, 205407, 205408, 205409, 205410, 205411, 217485, 217486, 217487, 217488, 217489, 217490, 217491, 217492};

	public _3739Glory_Against_The_5th() {
		super(questId);
	}

    @Override
    public void register() {
        qe.registerQuestNpc(278535).addOnQuestStart(questId); //Maius.
        qe.registerQuestNpc(278535).addOnTalkEvent(questId); //Maius.
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
        if (env.getVisibleObject() instanceof Player && player.getWorldId() == 400010000) {
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
                qs.setStatus(QuestStatus.REWARD); // If we made MOBS_KILLS = 1 of NPC, then make status REWARD
            }
            qs.setQuestVarById(1, killCount); // Update variable
            updateQuestStatus(env);
            return true;
        }
        return false;
    }

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
		    if (env.getTargetId() == 278535) { //Maius.
			    switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ACCEPT_QUEST_SIMPLE: {
						return sendQuestStartDialog(env);
					}
					case REFUSE_QUEST_SIMPLE: {
				        return closeDialogWindow(env);
					}
				}
			} 
            if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
				if (env.getTargetId() == 278535) { //Maius.
				    if (env.getDialog() == QuestDialog.START_DIALOG) {
						return sendQuestDialog(env, 10002);
					} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
						return sendQuestDialog(env, 5);
					} else {
						return sendQuestEndDialog(env);
					}
				}
			}
		}
		return false;
	}
}