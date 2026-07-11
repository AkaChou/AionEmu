package com.aionemu.gameserver.quest.handlers.reshanta;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 雷山塔任务脚本：Eliminate The Guardian General Of The Kysis Fortress（任务 ID 23868）。
 * Reshanta quest script: Eliminate The Guardian General Of The Kysis Fortress (quest ID 23868).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23868Eliminate_The_Guardian_General_Of_The_Kysis_Fortress extends QuestHandler {

    private final static int questId = 23868;
	private final static int[] Ab1231BossA = {279345, 269011};
    public _23868Eliminate_The_Guardian_General_Of_The_Kysis_Fortress() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805403).addOnQuestStart(questId); //Labori.
        qe.registerQuestNpc(805403).addOnTalkEvent(questId); //Labori.
		for (int mob: Ab1231BossA) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		}
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
		int targetId = env.getTargetId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 805403) { //Labori.
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
				case 279345:
				case 269011:
                if (qs.getQuestVarById(1) < 1) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 1) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
