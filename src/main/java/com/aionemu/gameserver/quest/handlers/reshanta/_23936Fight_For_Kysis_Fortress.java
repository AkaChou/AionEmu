package com.aionemu.gameserver.quest.handlers.reshanta;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 雷山塔任务脚本：Fight For Kysis Fortress（任务 ID 23936）。
 * Reshanta quest script: Fight For Kysis Fortress (quest ID 23936).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23936Fight_For_Kysis_Fortress extends QuestHandler {

    private final static int questId = 23936;
	private final static int[] Ab1_1231Guard = {279260, 279275, 279285, 279300, 279315, 279330, 882786, 882791, 882796, 882801, 882806};
    public _23936Fight_For_Kysis_Fortress() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805403).addOnQuestStart(questId); //Labori.
        qe.registerQuestNpc(805403).addOnTalkEvent(questId); //Labori.
		for (int mob: Ab1_1231Guard) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		}
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
		int targetId = env.getTargetId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805403) { //Labori.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
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
				case 279260:
				case 279275:
				case 279285:
				case 279300:
				case 279315:
				case 279330:
				case 882786:
				case 882791:
				case 882796:
				case 882801:
				case 882806:
                if (qs.getQuestVarById(1) < 10) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 10) {
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}
