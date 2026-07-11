package com.aionemu.gameserver.quest.handlers.taloc_hollow;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 塔洛克空洞任务脚本：Mysterious Seed（任务 ID 21465）。
 * Taloc Hollow quest script: Mysterious Seed (quest ID 21465).
 *
 * @author mr.madison
 */
public class _21465MysteriousSeed extends QuestHandler {

	private final static int questId = 21465;
	public _21465MysteriousSeed() {
		super(questId);
	}

	public void register() {
		qe.registerQuestItem(182209527, questId);
		qe.registerQuestNpc(279000).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 0) { 
				if (env.getDialog() == QuestDialog.ACCEPT_QUEST) {
					return sendQuestStartDialog(env);
				}
				if (env.getDialog() == QuestDialog.REFUSE_QUEST) {
					return closeDialogWindow(env);
			    }
			}
		}
		else if (qs.getStatus() == QuestStatus.START){
			if(targetId == 279000){
				switch (env.getDialog()){
					case USE_OBJECT:
						return sendQuestDialog(env, 2375);
					case SELECT_REWARD:{
						removeQuestItem(env, 182209527, 1);
						changeQuestStep(env, 0, 0, true);
						return sendQuestDialog(env, 5);
					}
				}
			}
		}
		else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 279000) {
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
			return HandlerResult.fromBoolean(sendQuestDialog(env, 4));
		}
		return HandlerResult.FAILED;
	}
}
