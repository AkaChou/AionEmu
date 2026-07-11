package com.aionemu.gameserver.quest.handlers.oriel;

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
 * 奥里尔任务脚本：Leafy Cutting（任务 ID 18833）。
 * Oriel quest script: Leafy Cutting (quest ID 18833).
 *
 * @author zhkchi
 */
public class _18833LeafyCutting extends QuestHandler {

	private final static int questId = 18833;

	public _18833LeafyCutting() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(830655).addOnTalkEvent(questId);
		qe.registerQuestItem(182213202, questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			return false;
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if(targetId == 830655){
				switch (dialog)
				{
					case START_DIALOG: 
					{
						return sendQuestDialog(env, 2375);
					}
					case SELECT_REWARD:
					{
						changeQuestStep(env, 0, 0, true);
						return sendQuestDialog(env, 5);
					}
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 830655) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}

	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		if(item.getItemId() != 182213202) return HandlerResult.FAILED;
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (QuestService.startQuest(env)) {
				return HandlerResult.fromBoolean(sendQuestDialog(env, 4));
			}
		}
		return HandlerResult.FAILED;
	}
}
