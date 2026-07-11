package com.aionemu.gameserver.quest.handlers.eltnen;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 艾特南任务脚本：The Bucket List（任务 ID 14120）。
 * Eltnen quest script: The Bucket List (quest ID 14120).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _14120The_Bucket_List extends QuestHandler {
	
    private final static int questId = 14120;
    public _14120The_Bucket_List() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerQuestItem(182215478, questId);
		qe.registerQuestNpc(203932).addOnQuestStart(questId); //Phomona
        qe.registerQuestNpc(203932).addOnTalkEvent(questId); //Phomona
        qe.registerQuestNpc(730020).addOnTalkEvent(questId); //Demro
        qe.registerQuestNpc(730019).addOnTalkEvent(questId); //Lodas
        qe.registerQuestNpc(700157).addOnTalkEvent(questId); //Kerubian Bucket
    }
	
	@Override
    public boolean onDialogEvent(final QuestEnv env) {
        final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
        int targetId = env.getTargetId();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 203932) { //Phomona.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 1011);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
            if (targetId == 730019) { //Lodas
                switch (env.getDialog()) {
                    case START_DIALOG: {
                        if (var == 1) {
                            return sendQuestDialog(env, 2375);
                        }
                    } case CHECK_COLLECTED_ITEMS_SIMPLE: {
                        return checkQuestItems(env, 1, 1, true, 5, 2716);
                    }
                }
            } else if (targetId == 700157) { //Kerubian Bucket
                if (env.getDialog() == QuestDialog.USE_OBJECT) {
                    return true;
                }
            } else if (targetId == 730020) { //Demro.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    if (var == 0) {
                        return sendQuestDialog(env, 1352);
                    }
                } else if (env.getDialog() == QuestDialog.STEP_TO_1) {
                    return defaultCloseDialog(env, 0, 1);
                }
            }
        } else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 730019) { //Lodas.
                return sendQuestEndDialog(env);
            }
        }
        return false;
    }
}
