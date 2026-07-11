package com.aionemu.gameserver.quest.handlers.reshanta;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 雷山塔任务脚本：Battle For Siel Western Fortress（任务 ID 23920）。
 * Reshanta quest script: Battle For Siel Western Fortress (quest ID 23920).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23920Battle_For_Siel_Western_Fortress extends QuestHandler {

    private final static int questId = 23920;
	private final static int[] Ab1131Guard = {263016, 263031, 263046, 263061, 263225, 263026, 263041, 263056, 263071, 263235};
    public _23920Battle_For_Siel_Western_Fortress() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(278019).addOnQuestStart(questId); //Lakadi.
        qe.registerQuestNpc(278019).addOnTalkEvent(questId); //Lakadi.
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
            if (targetId == 278019) { //Lakadi.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 278019) { //Lakadi.
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
				case 263016:
				case 263031:
				case 263046:
				case 263061:
				case 263225:
				case 263026:
				case 263041:
				case 263056:
				case 263071:
				case 263235:
                if (qs.getQuestVarById(1) < 10) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 10) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
