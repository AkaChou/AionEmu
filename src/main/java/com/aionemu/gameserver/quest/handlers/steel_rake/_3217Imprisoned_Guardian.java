package com.aionemu.gameserver.quest.handlers.steel_rake;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 钢耙号任务脚本：Imprisoned Guardian（任务 ID 3217）。
 * Steel Rake quest script: Imprisoned Guardian (quest ID 3217).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _3217Imprisoned_Guardian extends QuestHandler {

	private final static int questId = 3217;
	public _3217Imprisoned_Guardian() {
		super(questId);
	}
	
	public void register() {
		qe.registerQuestNpc(798335).addOnQuestStart(questId);
		qe.registerQuestNpc(798335).addOnTalkEvent(questId);
		qe.registerQuestNpc(204590).addOnTalkEvent(questId);
    }
	
    @Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (env.getVisibleObject() instanceof Npc) {
            targetId = ((Npc) env.getVisibleObject()).getNpcId();
        } 
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 798335) { //Nasuri.
                switch (env.getDialog()) {
				case START_DIALOG: {
					return sendQuestDialog(env, 4762);
				}  case ASK_ACCEPTION: {
					return sendQuestDialog(env, 4);
				}  case ACCEPT_QUEST: {
					return sendQuestStartDialog(env);
				}  case REFUSE_QUEST: {
					return sendQuestDialog(env, 1004);
				}
			}
			return false;
		  }
       }
       if (qs == null || qs.getStatus() == QuestStatus.START) {
			if (targetId == 798335) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}  case CHECK_COLLECTED_ITEMS: {
						if (QuestService.collectItemCheck(env, true)) {
							return sendQuestDialog(env, 10000);
						} else {
							return sendQuestDialog(env, 10001);
						   }
                       }
                       case SET_REWARD: {
						qs.setQuestVar(1);
                        qs.setStatus(QuestStatus.REWARD);  
						updateQuestStatus(env);
                        return closeDialogWindow(env);
					}
				}
			}
		} 
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204590) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
