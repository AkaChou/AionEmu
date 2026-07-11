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
 * 奥里尔任务脚本：Eye Cutting（任务 ID 18837）。
 * Oriel quest script: Eye Cutting (quest ID 18837).
 *
 * @author zhkchi
 */
public class _18837EyeCutting extends QuestHandler {

	private static final int questId = 18837;

	public _18837EyeCutting() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestItem(182213204, questId);
		qe.registerQuestNpc(830655).addOnTalkEvent(questId);
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
		else if(qs != null && qs.getStatus() == QuestStatus.START){
			if (targetId == 830655) {		
				switch (dialog) {
					case START_DIALOG:
						return sendQuestDialog(env, 2375);
					case SELECT_REWARD:
						changeQuestStep(env, 0, 0, true);
						removeQuestItem(env, 182213204, 1);
						return sendQuestDialog(env, 5);
				}
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 830655) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}

	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		if(item.getItemId() != 182213204) 
			return HandlerResult.FAILED;
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (QuestService.startQuest(env)) {
				return HandlerResult.SUCCESS;
			}
		}
		return HandlerResult.FAILED;
	}
}
