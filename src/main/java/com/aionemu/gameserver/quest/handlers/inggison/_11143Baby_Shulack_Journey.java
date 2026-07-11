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
 * 因吉森任务脚本：Baby Shulack Journey（任务 ID 11143）。
 * Inggison quest script: Baby Shulack Journey (quest ID 11143).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _11143Baby_Shulack_Journey extends QuestHandler {

	private final static int questId = 11143;
	public _11143Baby_Shulack_Journey() {
		super(questId);
	}
	
	public void register() {
		qe.registerQuestItem(182206866, questId);
		qe.registerQuestNpc(700909).addOnTalkEvent(questId);
		qe.registerQuestNpc(798985).addOnTalkEvent(questId);
		qe.registerQuestNpc(798984).addOnTalkEvent(questId);
		qe.registerQuestNpc(798976).addOnTalkEvent(questId);
		qe.registerQuestNpc(798948).addOnTalkEvent(questId);
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
		QuestDialog dialog = env.getDialog();
		if (env.getVisibleObject() instanceof Npc) {
            targetId = ((Npc) env.getVisibleObject()).getNpcId();
        } if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 0) { 
				if (dialog == QuestDialog.ACCEPT_QUEST) {
				   return sendQuestStartDialog(env);
				}
			    if (dialog == QuestDialog.REFUSE_QUEST) {
				   return closeDialogWindow(env);
			    }
			} else if (targetId == 700909) {
				Npc npc = (Npc) env.getVisibleObject();
				giveQuestItem(env, 182206866, 1);
				npc.getController().scheduleRespawn();
				npc.getController().onDelete();
				return true;
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 798985) { 
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1352);
				} else if (dialog == QuestDialog.STEP_TO_1) {
					return defaultCloseDialog(env, 0, 1);
				}
			} else if (targetId == 798984) { 
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1693);
				} else if (dialog == QuestDialog.STEP_TO_2) {
					return defaultCloseDialog(env, 1, 2);
				}
			} else if (targetId == 798976) { 
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 2034);
				} else if (dialog == QuestDialog.STEP_TO_3) {
					qs.setQuestVar(3);
					return defaultCloseDialog(env, 3, 3, true, false);
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798948) {
				switch (dialog) {
					case USE_OBJECT: {
						return sendQuestDialog(env, 2375);
					} default: {
						removeQuestItem(env, 182206866, 1);
						return sendQuestEndDialog(env);
					}
				}
			}
		}
		return false;
	}
}
