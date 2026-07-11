package com.aionemu.gameserver.quest.handlers.inggison;

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
 * 因吉森任务脚本：That Pretty Box（任务 ID 11046）。
 * Inggison quest script: That Pretty Box (quest ID 11046).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _11046That_Pretty_Box extends QuestHandler {

	private final static int questId = 11046;
	public _11046That_Pretty_Box() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(798954).addOnTalkEvent(questId);
		qe.registerQuestItem(182206745, questId);
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
		int targetId = 0;
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		} if (targetId == 0) {
			if (env.getDialog() == QuestDialog.ACCEPT_QUEST) {
				return sendQuestStartDialog(env);
			}
			if (env.getDialog() == QuestDialog.REFUSE_QUEST) {
				return closeDialogWindow(env);
			}
		} else if (targetId == 798954) {
			if (qs != null && qs.getStatus() == QuestStatus.START) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 2375);
				} else if (env.getDialogId() == 1009) {
					removeQuestItem(env, 182206745, 1);
					qs.setQuestVar(1);
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
