package com.aionemu.gameserver.quest.handlers.kaldor;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 卡尔多尔任务脚本：An Unlikely Alliance（任务 ID 13812）。
 * Kaldor quest script: An Unlikely Alliance (quest ID 13812).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _13812An_Unlikely_Alliance extends QuestHandler {

	private final static int questId = 13812;
	public _13812An_Unlikely_Alliance() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(802428).addOnTalkEvent(questId); //Milda.
		qe.registerQuestItem(182215489, questId); //Vasharti Orders.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
        if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 802428) { //Milda.
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
