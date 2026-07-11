package com.aionemu.gameserver.quest.handlers.bastion_of_souls;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 灵魂堡垒任务脚本：Logion Journal（任务 ID 13952）。
 * Bastion of Souls quest script: Logion Journal (quest ID 13952).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _13952Logion_Journal extends QuestHandler {

	private final static int questId = 13952;
	public _13952Logion_Journal() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestItem(182216177, questId);
		qe.registerQuestNpc(806582).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 0) {
                switch (env.getDialog()) {
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
				case 806582: {
				switch (env.getDialog()) {
					case START_DIALOG:
						return sendQuestDialog(env, 10002);
					case SELECT_REWARD:
				        removeQuestItem(env, 182216177, 1);
						changeQuestStep(env, 0, 0, true);
						return sendQuestEndDialog(env);
                    }
			   }
		   }
	   }
       else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 806582) {
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
