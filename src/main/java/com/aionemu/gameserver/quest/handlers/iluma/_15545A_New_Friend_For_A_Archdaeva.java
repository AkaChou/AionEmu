package com.aionemu.gameserver.quest.handlers.iluma;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 伊卢玛任务脚本：A New Friend For A Archdaeva（任务 ID 15545）。
 * Iluma quest script: A New Friend For A Archdaeva (quest ID 15545).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _15545A_New_Friend_For_A_Archdaeva extends QuestHandler {

	private final static int questId = 15545;
	public _15545A_New_Friend_For_A_Archdaeva() {
		super(questId);
	}
	
	@Override
	public void register() {
        qe.registerQuestNpc(835514).addOnQuestStart(questId);
        qe.registerQuestNpc(835514).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 835514) { //Leona.
                switch (env.getDialog()) {
                    case START_DIALOG: {
                        return sendQuestDialog(env, 4762);
					} 
                    case ACCEPT_QUEST_SIMPLE: {
                    if (player.getInventory().getItemCountByItemId(190080010) == 0) {
			              giveQuestItem(env, 190080010, 1);
                        }
						return sendQuestStartDialog(env);
					} 
                    case REFUSE_QUEST_SIMPLE: {
				        return closeDialogWindow(env);
					}
                }
			}
		}
		if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 835514) { //Leona.
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
}
