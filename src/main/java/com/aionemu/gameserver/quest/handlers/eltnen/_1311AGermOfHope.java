package com.aionemu.gameserver.quest.handlers.eltnen;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 艾特南任务脚本：A Germ Of Hope（任务 ID 1311）。
 * Eltnen quest script: A Germ Of Hope (quest ID 1311).
 *
 * @author Mr.Poke remod by Nephis and quest helper team
 */
public class _1311AGermOfHope extends QuestHandler {

	private final static int questId = 1311;
	public _1311AGermOfHope() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203997).addOnQuestStart(questId);
		qe.registerQuestNpc(700164).addOnTalkEvent(questId);
		qe.registerQuestNpc(203997).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 203997) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else if (env.getDialogId() == 1013) {
					return sendQuestDialog(env, 4);
                }   
				else if (env.getDialogId() == 1002) {
					return sendQuestStartDialog(env, 182201305, 1);
				}
				else if (env.getDialogId() == 1003) {
					return closeDialogWindow(env);
				}
			}
        } 
		else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 700164: {
					if (qs.getQuestVarById(0) == 0 && env.getDialog() == QuestDialog.USE_OBJECT) {
						removeQuestItem(env, 182201305, 1);
                        qs.setStatus(QuestStatus.REWARD); 
                        qs.setQuestVarById(0, 3);
						updateQuestStatus(env);
						return true;
					}
				}
		    }
        }
        else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 203997) {
                if (env.getDialog() == QuestDialog.USE_OBJECT) {
                    return sendQuestDialog(env, 2375);
				} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}
