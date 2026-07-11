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

/**
 * 艾特南任务脚本：The Fire Temple Key（任务 ID 1355）。
 * Eltnen quest script: The Fire Temple Key (quest ID 1355).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _1355The_Fire_Temple_Key extends QuestHandler {

	private final static int questId = 1355;
	public _1355The_Fire_Temple_Key() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(203933).addOnTalkEvent(questId);
		qe.registerQuestItem(182201400, questId);
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
		int targetId = 0;
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		} 
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
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 203933) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 2375);
				} else if (env.getDialogId() == 1009) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
                    qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return sendQuestEndDialog(env);
				}
			}
	    }
        else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203933) {
				removeQuestItem(env, 182201400, 1);
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
