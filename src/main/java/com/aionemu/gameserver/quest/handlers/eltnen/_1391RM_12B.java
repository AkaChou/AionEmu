package com.aionemu.gameserver.quest.handlers.eltnen;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 艾特南任务脚本：RM 12 B（任务 ID 1391）。
 * Eltnen quest script: RM 12 B (quest ID 1391).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _1391RM_12B extends QuestHandler {

	private final static int questId = 1391;
	public _1391RM_12B() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(204500).addOnTalkEvent(questId);
		qe.registerQuestItem(182201342, questId);
	}
	
	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			return HandlerResult.fromBoolean(sendQuestDialog(env, 4));
		}
		return HandlerResult.FAILED;
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
				else if (env.getDialog() == QuestDialog.REFUSE_QUEST) {
					return closeDialogWindow(env);
				}
			}
        }
       else if (targetId == 204500) {
		  if (qs != null && qs.getStatus() == QuestStatus.START) {
		    if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 2375);
				} else if (env.getDialogId() == 1009) {
                    qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return sendQuestEndDialog(env);
                }
			}
			else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
