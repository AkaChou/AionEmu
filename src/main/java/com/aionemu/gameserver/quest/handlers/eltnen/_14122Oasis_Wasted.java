package com.aionemu.gameserver.quest.handlers.eltnen;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 艾特南任务脚本：Oasis Wasted（任务 ID 14122）。
 * Eltnen quest script: Oasis Wasted (quest ID 14122).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _14122Oasis_Wasted extends QuestHandler {

    private final static int questId = 14122;
    public _14122Oasis_Wasted() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerQuestNpc(203917).addOnQuestStart(questId); //Gaia
        qe.registerQuestNpc(203917).addOnTalkEvent(questId); //Gaia
		qe.registerQuestNpc(203992).addOnTalkEvent(questId); //Ophelos
		qe.registerQuestNpc(203987).addOnTalkEvent(questId); //Heratos
		qe.registerQuestNpc(203934).addOnTalkEvent(questId); //Sirink
    }
	
	@Override
    public boolean onDialogEvent(final QuestEnv env) {
        final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
        int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 203917) { //Gaia
			    if (env.getDialog() == QuestDialog.START_DIALOG) {
				   return sendQuestDialog(env, 1011);
			    } else {
				   return sendQuestStartDialog(env, 182215480, 1);
			    }
			}
		} else if (qs != null && qs.getStatus() == QuestStatus.START) {
			if (targetId == 203992) { //Ophelos
			    if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1352);
				} else if (env.getDialog() == QuestDialog.STEP_TO_1) {
					return defaultCloseDialog(env, 0, 1);
				}
			} else if (targetId == 203987) { //Heratos
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1693);
				} else if (env.getDialog() == QuestDialog.STEP_TO_2) {
					return defaultCloseDialog(env, 1, 2);
				}
			} else if (targetId == 203934) { //Sirink
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 2375);
				} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
					removeQuestItem(env, 182215480, 1);
					return checkQuestItems(env, 2, 2, true, 5, 2716);
				}
			}
        } else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203934) { //Sirink
                return sendQuestEndDialog(env);
			}
		}
        return false;
    }
}
