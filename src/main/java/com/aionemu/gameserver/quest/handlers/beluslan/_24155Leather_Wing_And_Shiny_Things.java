package com.aionemu.gameserver.quest.handlers.beluslan;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 贝勒斯兰任务脚本：Leather Wing And Shiny Things（任务 ID 24155）。
 * Beluslan quest script: Leather Wing And Shiny Things (quest ID 24155).
 *
 * @author Ghostfur & Unknown (Aion-Unique). correct DainAvenger
 */
public class _24155Leather_Wing_And_Shiny_Things extends QuestHandler {

    private final static int questId = 24155;
    public _24155Leather_Wing_And_Shiny_Things() {
        super(questId);
    }
	
    @Override
    public void register() {
    	qe.registerQuestItem(182204318, questId);
		qe.registerQuestNpc(204701).addOnQuestStart(questId); //Hod
    	qe.registerQuestNpc(204701).addOnTalkEvent(questId); //Hod
    	qe.registerQuestNpc(204785).addOnTalkEvent(questId); //Gwendolin
        qe.registerQuestNpc(700290).addOnKillEvent(questId); //Field Suppressor
    }
	
    @Override
    public boolean onDialogEvent(final QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        int targetId = env.getTargetId();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 204701) { //Hod
            	switch (env.getDialog()) {
        			case START_DIALOG:
        				return sendQuestDialog(env, 1011);
        			case ASK_ACCEPTION:
        				return sendQuestDialog(env, 4);
        			case ACCEPT_QUEST:
					if (QuestService.startQuest(env)) {
						qs = player.getQuestStateList().getQuestState(questId);
					    qs.setQuestVarById(5, 1);
						updateQuestStatus(env);
				        return closeDialogWindow(env);
                    }
        			case REFUSE_QUEST:
        				return sendQuestDialog(env, 1004);
            	}
            }
        }
        if (qs == null) {
		    return false;
		} 
        else if (qs.getStatus() == QuestStatus.START) {
        	switch (targetId) {
                case 204785: { //Gwendolin
                    switch (env.getDialog()) {
                        case START_DIALOG:
                            return sendQuestDialog(env, 1352);
                        case STEP_TO_2:
						    qs.setQuestVarById(5, 0);
						    qs.setQuestVarById(0, 0);
						    updateQuestStatus(env);
                            return closeDialogWindow(env);
                    }
                } 
                case 204701: { //Hod
                    switch (env.getDialog()) {
                        case START_DIALOG:
                        	return sendQuestDialog(env, 2375);
                        case SELECT_REWARD:
                        	qs.setStatus(QuestStatus.REWARD);
                            updateQuestStatus(env);
            		        return sendQuestEndDialog(env);
                        }
                   }
              }
         } 
         else if (qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 204701) { //Hod
            	if (env.getDialog() == QuestDialog.SELECT_REWARD) {
            		return sendQuestDialog(env, 5);
            	} else {
            		return sendQuestEndDialog(env);
            	}
            }
        }
        return false;
    }
	
    @Override
    public boolean onKillEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() != QuestStatus.START) {
            return false;
        }
		int var = qs.getQuestVarById(0);
        if (var < 3) {
            return defaultOnKillEvent(env, 700290, var, var + 1);
        }
        return false;
    }
}
