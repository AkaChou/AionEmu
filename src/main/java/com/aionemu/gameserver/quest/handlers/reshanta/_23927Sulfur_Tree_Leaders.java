package com.aionemu.gameserver.quest.handlers.reshanta;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 雷山塔任务脚本：Sulfur Tree Leaders（任务 ID 23927）。
 * Reshanta quest script: Sulfur Tree Leaders (quest ID 23927).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23927Sulfur_Tree_Leaders extends QuestHandler {

    private final static int questId = 23927;
	private final static int[] Ab1141Guard = {264516, 264531, 264546, 264561, 264727, 264526, 264541, 264556, 264571, 264737};
    public _23927Sulfur_Tree_Leaders() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(278019).addOnQuestStart(questId); //Lakadi.
        qe.registerQuestNpc(278019).addOnTalkEvent(questId); //Lakadi.
		for (int mob: Ab1141Guard) {
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
				case 264516:
				case 264531:
				case 264546:
				case 264561:
				case 264727:
				case 264526:
				case 264541:
				case 264556:
				case 264571:
				case 264737:
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
