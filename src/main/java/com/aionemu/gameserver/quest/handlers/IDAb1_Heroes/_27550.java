package com.aionemu.gameserver.quest.handlers.IDAb1_Heroes;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 英雄联盟副本任务脚本（任务 ID 27550）。
 * IDAb1 Heroes instance quest script (quest ID 27550).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _27550 extends QuestHandler {

    private final static int questId = 27550;
    public _27550() {
        super(questId);
    }
	
	@Override
	public void register() {
		int[] npcs = {806135, 806790};
        for (int npc: npcs) {
            qe.registerQuestNpc(npc).addOnTalkEvent(questId);
        }
        qe.registerQuestNpc(806135).addOnQuestStart(questId);
	}
	
	@Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 806135) { 
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
				case 806790: {
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
		    if (targetId == 806790) {
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
