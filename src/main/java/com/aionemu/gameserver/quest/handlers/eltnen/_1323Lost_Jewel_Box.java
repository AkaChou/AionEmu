package com.aionemu.gameserver.quest.handlers.eltnen;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 艾特南任务脚本：Lost Jewel Box（任务 ID 1323）。
 * Eltnen quest script: Lost Jewel Box (quest ID 1323).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _1323Lost_Jewel_Box extends QuestHandler {
	
	private final static int questId = 1323;
	public _1323Lost_Jewel_Box() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(730032).addOnTalkEvent(questId);
		qe.registerQuestNpc(730019).addOnTalkEvent(questId);
		qe.registerQuestNpc(203939).addOnTalkEvent(questId);
		qe.registerQuestItem(182201309, questId);
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
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		int targetId = env.getTargetId();
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 0) { 
				if (env.getDialog() == QuestDialog.ACCEPT_QUEST) {
					return sendQuestStartDialog(env);
				    }
			    }
				if (env.getDialog() == QuestDialog.REFUSE_QUEST) {
					return closeDialogWindow(env);
			    }
			}	
		    if (targetId == 730032) {
				switch (env.getDialog()) {
					case USE_OBJECT: {
						return giveQuestItem(env, 182201309, 1);
				}
			}
		}
        if (qs == null)
			return false;
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 730019) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1352);
				}
				if (env.getDialog() == QuestDialog.SELECT_ACTION_1353) {
					return sendQuestDialog(env, 1353);
				}
                else if (env.getDialog() == QuestDialog.STEP_TO_1) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					updateQuestStatus(env);
				    return closeDialogWindow(env);
				}
			} else if (targetId == 203939) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 2375);
				} else if (env.getDialogId() == 1009) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
			        removeQuestItem(env, 182201309, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return sendQuestEndDialog(env);
				}
			}
		} else if (qs != null &&  qs.getStatus() == QuestStatus.REWARD && targetId == 203939) {
			return sendQuestEndDialog(env);
		}
		return false;
	}
}
