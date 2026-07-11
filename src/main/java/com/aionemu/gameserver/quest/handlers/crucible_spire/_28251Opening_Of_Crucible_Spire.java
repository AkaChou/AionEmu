package com.aionemu.gameserver.quest.handlers.crucible_spire;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 试炼尖塔任务脚本：Opening Of Crucible Spire（任务 ID 28251）。
 * Crucible Spire quest script: Opening Of Crucible Spire (quest ID 28251).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _28251Opening_Of_Crucible_Spire extends QuestHandler {

    private final static int questId = 28251;
    public _28251Opening_Of_Crucible_Spire() {
        super(questId);
    }
	
	@Override
	public void register() {
		int[] npcs = {806730, 798804};
        for (int npc: npcs) {
            qe.registerQuestNpc(npc).addOnTalkEvent(questId);
        }
		qe.registerQuestNpc(806730).addOnQuestStart(questId);
	}
	
	@Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 806730) { //Alik.
				switch (env.getDialog()) {
                    case START_DIALOG: {
                        return sendQuestDialog(env, 4762);
					} case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE: {
						return sendQuestStartDialog(env);
					} case REFUSE_QUEST_SIMPLE: {
				        return closeDialogWindow(env);
					}
                }
			}
		}
		if (qs == null || qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 798804: {
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 10002);
						} case SELECT_REWARD: {
							changeQuestStep(env, 0, 0, true);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		} 
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 798804) {
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
