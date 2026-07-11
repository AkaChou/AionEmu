package com.aionemu.gameserver.quest.handlers.heiron;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 希隆任务脚本：Secrets Of The Superus（任务 ID 14150）。
 * Heiron quest script: Secrets Of The Superus (quest ID 14150).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _14150Secrets_Of_The_Superus extends QuestHandler {
	
    private final static int questId = 14150;
    public _14150Secrets_Of_The_Superus() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestItem(182215458, questId);
        qe.registerQuestNpc(204501).addOnQuestStart(questId);//Sarantus
		qe.registerQuestNpc(204501).addOnTalkEvent(questId); //Sarantus
		qe.registerQuestNpc(204582).addOnTalkEvent(questId); //Ibelia
		qe.registerQuestNpc(700217).addOnTalkEvent(questId); //Engraved Stone Tablet
	}
	
	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
        int targetId = env.getTargetId();
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		} if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 204501) { //Sarantus
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 1011);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        } if (qs == null) {
		    return false;
		} else if (qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
			if (targetId == 204501) { //Sarantus
				switch (env.getDialog()) {
					case START_DIALOG: {
						if (var == 1) {
							return sendQuestDialog(env, 2375);
						}
					} case CHECK_COLLECTED_ITEMS_SIMPLE: {
						updateQuestStatus(env);
						return checkQuestItems(env, 1, 2, true, 5, 2716);
					}
				}
			} else if (targetId == 204582) { //Ibelia
				switch (env.getDialog()) {
					case START_DIALOG: {
						if (var == 0) {
							return sendQuestDialog(env, 1352);
						}
					} case STEP_TO_1: {
						if (var == 0) {
							qs.setQuestVarById(0, var + 1);
							updateQuestStatus(env);
							return closeDialogWindow(env);
						}
					}
				}
			} else if (targetId == 700217 && qs.getQuestVarById(0) == 1) { //Engraved Stone Tablet
                switch (env.getDialog()) {
                    case USE_OBJECT: {
						return true;
					}
                }
            }
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204501) { //Sarantus
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
