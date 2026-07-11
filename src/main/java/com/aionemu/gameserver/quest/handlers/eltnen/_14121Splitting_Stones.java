package com.aionemu.gameserver.quest.handlers.eltnen;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 艾特南任务脚本：Splitting Stones（任务 ID 14121）。
 * Eltnen quest script: Splitting Stones (quest ID 14121).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _14121Splitting_Stones extends QuestHandler {

    private final static int questId = 14121;
    public _14121Splitting_Stones() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerQuestNpc(203903).addOnQuestStart(questId); //Valerius
        qe.registerQuestNpc(203903).addOnTalkEvent(questId); //Valerius
		qe.registerQuestNpc(204032).addOnTalkEvent(questId); //Lakaias
    }
	
	@Override
    public boolean onDialogEvent(final QuestEnv env) {
        final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
        int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 203903) { //Valerius
			    if (env.getDialog() == QuestDialog.START_DIALOG) {
				   return sendQuestDialog(env, 1011);
			    } else {
				   return sendQuestStartDialog(env);
			    }
			}
		} else if (qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
			if (targetId == 204032) { //Lakaias
				switch (env.getDialog()) {
					case START_DIALOG: {
						if (var == 0) {
							return sendQuestDialog(env, 1352);
						} else if (var == 1) {
							return sendQuestDialog(env, 2375);
						}
					} case STEP_TO_1: {
						return defaultCloseDialog(env, 0, 1);
					} case CHECK_COLLECTED_ITEMS_SIMPLE: {
						return checkQuestItems(env, 1, 1, true, 5, 0);
					}
				}
			}
        } else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204032) { //Lakaias
                return sendQuestEndDialog(env);
			}
		}
        return false;
    }
}
