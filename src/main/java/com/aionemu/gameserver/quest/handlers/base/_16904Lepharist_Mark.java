package com.aionemu.gameserver.quest.handlers.base;

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
 * 基地任务脚本：Lepharist Mark（任务 ID 16904）。
 * Base quest script: Lepharist Mark (quest ID 16904).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _16904Lepharist_Mark extends QuestHandler {

	private final static int questId = 16904;
	public _16904Lepharist_Mark() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(801189).addOnTalkEvent(questId);
		qe.registerQuestItem(182213273, questId); //Lepharist's Mark.
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
        else if (qs.getStatus() == QuestStatus.START) {
            if (targetId == 801189) {
			if (env.getDialog() == QuestDialog.START_DIALOG) {
				return sendQuestDialog(env, 2375);
		}
        else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
				qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
				qs.setStatus(QuestStatus.REWARD);
				updateQuestStatus(env);
                return sendQuestEndDialog(env);
             } 
          }   
       }
       else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 801189) {
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
		return HandlerResult.UNKNOWN;
	}
}
