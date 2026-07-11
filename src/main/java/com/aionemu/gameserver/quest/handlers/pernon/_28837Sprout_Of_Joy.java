package com.aionemu.gameserver.quest.handlers.pernon;

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
 * 佩尔农任务脚本：Sprout Of Joy（任务 ID 28837）。
 * Pernon quest script: Sprout Of Joy (quest ID 28837).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _28837Sprout_Of_Joy extends QuestHandler
{
	private static final int questId = 28837;
	
	public _28837Sprout_Of_Joy() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestItem(182213211, questId);
		qe.registerQuestNpc(830656).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			return false;
		} else if(qs != null && qs.getStatus() == QuestStatus.START){
			if (targetId == 830656) {		
				switch (dialog) {
					case START_DIALOG:
						return sendQuestDialog(env, 2375);
					case SELECT_REWARD:
						changeQuestStep(env, 0, 0, true);
						removeQuestItem(env, 182213211, 1);
						return sendQuestDialog(env, 5);
				}
			}
		} else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 830656) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
	
	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		if (item.getItemId() != 182213211) 
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
