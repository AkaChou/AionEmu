package com.aionemu.gameserver.quest.handlers.beluslan;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 贝勒斯兰任务脚本：Reclaiming The Damned（任务 ID 24151）。
 * Beluslan quest script: Reclaiming The Damned (quest ID 24151).
 *
 * @author Ghostfur & Unknown (Aion-Unique). correct DainAvenger
 */
public class _24151Reclaiming_The_Damned extends QuestHandler {

    private final static int questId = 24151;
    private final static int[] mob_ids = {213044, 213045, 214092, 214093};
    public _24151Reclaiming_The_Damned() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerQuestNpc(204715).addOnQuestStart(questId); //Grundt
        qe.registerQuestNpc(204715).addOnTalkEvent(questId); //Grundt
        qe.registerQuestNpc(204801).addOnTalkEvent(questId); //Gigrite
        for (int mob_id: mob_ids) {
            qe.registerQuestNpc(mob_id).addOnKillEvent(questId);
        }
    }
	
    @Override
    public boolean onDialogEvent(final QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 204715) { //Grundt
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
        	int var = qs.getQuestVarById(0);
            if (targetId == 204801) { //Gigrite
                switch (env.getDialog()) {
                    case START_DIALOG: {
                    	if (var == 5) {
                    		return sendQuestDialog(env, 2375);
                    	}
                    	return sendQuestDialog(env, 1352);
                    } case STEP_TO_1: {
					    qs.setQuestVarById(5, 0);
					    qs.setQuestVarById(0, 0);
                        updateQuestStatus(env);
                        return closeDialogWindow(env);
                    } case SELECT_REWARD: {
                    	qs.setStatus(QuestStatus.REWARD);
                        updateQuestStatus(env);
            		    return sendQuestEndDialog(env);
                    }
                }
            }
        } else if (qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 204801) { //Gigrite
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
        if (var < 5) {
            return defaultOnKillEvent(env, mob_ids, var, var + 1);
        }
        return false;
    }
}
