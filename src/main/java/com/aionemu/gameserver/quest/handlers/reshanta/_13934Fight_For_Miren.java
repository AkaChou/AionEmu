package com.aionemu.gameserver.quest.handlers.reshanta;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 雷山塔任务脚本：Fight For Miren（任务 ID 13934）。
 * Reshanta quest script: Fight For Miren (quest ID 13934).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _13934Fight_For_Miren extends QuestHandler {

    private final static int questId = 13934;
	private final static int[] Ab1_1241Guard = {279456, 279471, 279481, 279496, 279511, 279526, 882616, 882621, 882626, 882631, 882636};
    public _13934Fight_For_Miren() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805380).addOnQuestStart(questId); //Lagranjia.
        qe.registerQuestNpc(805380).addOnTalkEvent(questId); //Lagranjia.
		for (int mob: Ab1_1241Guard) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		}
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
		int targetId = env.getTargetId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805380) { //Lagranjia.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 805380) { //Lagranjia.
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
				case 279456:
				case 279471:
				case 279481:
				case 279496:
				case 279511:
				case 279526:
				case 882616:
				case 882621:
				case 882626:
				case 882631:
				case 882636:
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
