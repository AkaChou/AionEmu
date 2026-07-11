package com.aionemu.gameserver.quest.handlers.ophidan_bridge;

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
 * 奥菲丹桥任务脚本：A Mysterious Shard（任务 ID 26975）。
 * Ophidan Bridge quest script: A Mysterious Shard (quest ID 26975).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _26975A_Mysterious_Shard extends QuestHandler {

	private final static int questId = 26975;
	public _26975A_Mysterious_Shard() {
		super(questId);
	}
	
	public void register() {
		qe.registerQuestNpc(801765).addOnTalkEvent(questId); //Rohellein.
		qe.registerQuestItem(182215760, questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
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
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 801765) { //Rohellein.
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
	
	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            QuestService.startQuest(env);
            qs.setStatus(QuestStatus.REWARD);
            updateQuestStatus(env);   
		}
		return HandlerResult.FAILED;
	}
}
