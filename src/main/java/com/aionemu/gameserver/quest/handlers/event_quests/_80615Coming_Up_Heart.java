package com.aionemu.gameserver.quest.handlers.event_quests;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 活动任务脚本：Coming Up Heart（任务 ID 80615）。
 * Event quest script: Coming Up Heart (quest ID 80615).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _80615Coming_Up_Heart extends QuestHandler
{
	private final static int questId = 80615;
	
	public _80615Coming_Up_Heart() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(832866).addOnTalkEvent(questId); //Lukrunerk.
		qe.registerQuestItem(182215581, questId); //[Event] Right Of Luck.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			return false;
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 832866) { //Lukrunerk.
				removeQuestItem(env, 182215581, 1); //[Event] Right Of Luck.
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
			if (QuestService.startQuest(env)) {
				changeQuestStep(env, 0, 0, true);
				return HandlerResult.fromBoolean(sendQuestDialog(env, 4));
			}
		}
		return HandlerResult.UNKNOWN;
	}
}
