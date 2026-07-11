package com.aionemu.gameserver.quest.handlers.reshanta;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 雷山塔任务脚本：Siel Western Fortress Commander（任务 ID 13921）。
 * Reshanta quest script: Siel Western Fortress Commander (quest ID 13921).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _13921Siel_Western_Fortress_Commander extends QuestHandler {

    private final static int questId = 13921;
	private final static int[] Ab1131Guard = {263021, 263036, 263051, 263066, 263230, 263026, 263041, 263056, 263071, 263235};
    public _13921Siel_Western_Fortress_Commander() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(278517).addOnQuestStart(questId); //Nereus.
        qe.registerQuestNpc(278517).addOnTalkEvent(questId); //Nereus.
		for (int mob: Ab1131Guard) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		}
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
		int targetId = env.getTargetId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 278517) { //Nereus.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 278517) { //Nereus.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 10002);
				} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
        return false;
    }
	
    public boolean onKillEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            switch (env.getTargetId()) {
				case 263021:
				case 263036:
				case 263051:
				case 263066:
				case 263230:
				case 263026:
				case 263041:
				case 263056:
				case 263071:
				case 263235:
                if (qs.getQuestVarById(1) < 3) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 3) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
