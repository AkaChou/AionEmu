package com.aionemu.gameserver.quest.handlers.cygnea;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 希格尼娅任务脚本：A Token Of Victory（任务 ID 15200）。
 * Cygnea quest script: A Token Of Victory (quest ID 15200).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _15200A_Token_Of_Victory extends QuestHandler {

	private final static int questId = 15200;
	public _15200A_Token_Of_Victory() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(804699).addOnTalkEvent(questId);
		qe.registerQuestItem(182215750, questId);
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
				if (env.getDialog() == QuestDialog.REFUSE_QUEST) {
					return closeDialogWindow(env);
				}
			}
		} 
        else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 804699) {
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
        else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804699) {
			    return sendQuestEndDialog(env);
            }
		}
		return false;
	}
}
