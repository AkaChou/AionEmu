package com.aionemu.gameserver.quest.handlers.archives_of_eternity;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 永恒档案馆任务脚本：Completed History Of Atreia（任务 ID 16809）。
 * Archives of Eternity quest script: Completed History Of Atreia (quest ID 16809).
 *
 * @author (Encom)
 */
public class _16809Completed_History_Of_Atreia extends QuestHandler {

	private final static int questId = 16809;
	public _16809Completed_History_Of_Atreia() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(806075).addOnTalkEvent(questId);
		qe.registerQuestItem(182215986, questId);
	}
	
	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (QuestService.startQuest(env)) {
				changeQuestStep(env, 0, 0, true);
				return HandlerResult.fromBoolean(sendQuestDialog(env, 4));
			}
		}
		return HandlerResult.UNKNOWN;
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
        if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 806075) {
				removeQuestItem(env, 182215986, 1);
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
