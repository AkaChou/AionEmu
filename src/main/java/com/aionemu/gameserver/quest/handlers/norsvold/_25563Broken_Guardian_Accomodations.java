package com.aionemu.gameserver.quest.handlers.norsvold;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 诺斯沃尔德任务脚本：Broken Guardian Accomodations（任务 ID 25563）。
 * Norsvold quest script: Broken Guardian Accomodations (quest ID 25563).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25563Broken_Guardian_Accomodations extends QuestHandler {

	private final static int questId = 25563;
	public _25563Broken_Guardian_Accomodations() {
		super(questId);
	}
	
	@Override
	public void register() {
        qe.registerQuestItem(182215977, questId);  
		qe.registerQuestNpc(806116).addOnTalkEvent(questId); //Reinhard.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 0) {
                switch (env.getDialog()) {
                    case ACCEPT_QUEST:
				    case ACCEPT_QUEST_SIMPLE: {
					    return sendQuestStartDialog(env);
				    } case REFUSE_QUEST_SIMPLE: {
				        return closeDialogWindow(env);
                    }
                }
			}
        }
		if (qs == null || qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 806116: { //Reinhard.
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 10002);
						} case SELECT_REWARD: {
							changeQuestStep(env, 0, 0, true);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		}
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 806116) { //Reinhard.
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}

	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			QuestService.startQuest(env);
		}
		return HandlerResult.FAILED;
	}
}
