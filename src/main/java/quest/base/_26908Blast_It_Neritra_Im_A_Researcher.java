/*
 * =====================================================================================*
 * This file is part of Aion-Unique (Aion-Unique Home Software Development)             *
 * Aion-Unique Development is a closed Aion Project that use Old Aion Project Base      *
 * Like Aion-Lightning, Aion-Engine, Aion-Core, Aion-Extreme, Aion-NextGen, ArchSoft,   *
 * Aion-Ger, U3J, Encom And other Aion project, All Credit Content                      *
 * That they make is belong to them/Copyright is belong to them. And All new Content    *
 * that Aion-Unique make the copyright is belong to Aion-Unique                         *
 * You may have agreement with Aion-Unique Development, before use this Engine/Source   *
 * You have agree with all of Term of Services agreement with Aion-Unique Development   *
 * =====================================================================================*
 */
package quest.base;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/****/
/** Author Ghostfur & Unknown (Aion-Unique). correct DainAvenger.
/****/

public class _26908Blast_It_Neritra_Im_A_Researcher extends QuestHandler {

    private final static int questId = 26908;
    public _26908Blast_It_Neritra_Im_A_Researcher() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(204702).addOnQuestStart(questId);
        qe.registerQuestNpc(204702).addOnTalkEvent(questId);
		qe.registerQuestNpc(204817).addOnTalkEvent(questId);
		qe.registerQuestNpc(231570).addOnKillEvent(questId);
		qe.registerQuestNpc(231571).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 204702) {
				switch (env.getDialog()) {
					case START_DIALOG:
						return sendQuestDialog(env, 1011);
					case ACCEPT_QUEST_SIMPLE:
					if (QuestService.startQuest(env)) {
						qs = player.getQuestStateList().getQuestState(questId);
					    qs.setQuestVarById(5, 1);
						updateQuestStatus(env);
				        return closeDialogWindow(env);
					}
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		}
        else if (qs.getStatus() == QuestStatus.START) {
        	int var = qs.getQuestVarById(0);
			switch (targetId) {
				case 204817: {
					switch (env.getDialog()) {
						case START_DIALOG:
                    	if (var == 1) {
                    		return sendQuestDialog(env, 2375);
                    	}
                    	return sendQuestDialog(env, 1352);
						case STEP_TO_1:
					        qs.setQuestVarById(5, 0);
						    qs.setQuestVarById(0, 0);
            				updateQuestStatus(env);
            				return closeDialogWindow(env);
					    case SELECT_REWARD:
        				    qs.setStatus(QuestStatus.REWARD);
        				    updateQuestStatus(env);
						    return sendQuestEndDialog(env);
					}
				}
			}
		}
        else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204817) {
				switch (env.getDialog()) {
				case SELECT_REWARD: {
					return sendQuestDialog(env, 5);
				} default:
					return sendQuestEndDialog(env);
                }
		    }
		}
        return false;
    }
	
    public boolean onKillEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            switch (env.getTargetId()) {
				case 231570:
				case 231571:
                if (qs.getQuestVarById(0) < 1) {
                    qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
                    qs.setQuestVarById(0, 1);
                    updateQuestStatus(env);
                    return true;
                }
            }
        }
        return false;
    }
}