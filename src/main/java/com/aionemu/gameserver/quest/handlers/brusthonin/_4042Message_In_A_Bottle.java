package com.aionemu.gameserver.quest.handlers.brusthonin;

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
 * 布鲁斯特霍宁任务脚本：Message In A Bottle（任务 ID 4042）。
 * Brusthonin quest script: Message In A Bottle (quest ID 4042).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _4042Message_In_A_Bottle extends QuestHandler {

	private final static int questId = 4042;
	public _4042Message_In_A_Bottle() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(205192).addOnTalkEvent(questId); //Sahnu.
		qe.registerQuestNpc(204225).addOnTalkEvent(questId); //Gunter.
		qe.registerQuestItem(182209024, questId); //Wet Letter.
		qe.registerQuestItem(182209025, questId); //A letter From Ntuamu To Sahnu.
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
	public boolean onDialogEvent(final QuestEnv env) {
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
				if (env.getDialog() == QuestDialog.REFUSE_QUEST) {
					return closeDialogWindow(env);
				}
			}
		} 
        else if (qs != null && qs.getStatus() == QuestStatus.START) {
			if (targetId == 205192) { //Sahnu.
				if (qs.getQuestVarById(0) == 0) {
					if (env.getDialog() == QuestDialog.START_DIALOG) {
						return sendQuestDialog(env, 1352);
					} else if (env.getDialog() == QuestDialog.STEP_TO_1) {
						removeQuestItem(env, 182209024, 1); //Wet Letter.
						giveQuestItem(env, 182209025, 1); //A letter From Ntuamu To Sahnu.
						qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
						updateQuestStatus(env);
						return closeDialogWindow(env);
					}
				} else if (qs.getQuestVarById(0) == 2) {
					if (env.getDialog() == QuestDialog.START_DIALOG) {
						return sendQuestDialog(env, 2375);
					} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
				        return sendQuestEndDialog(env);
					}
				}
			} 
            if (targetId == 204225) { //Gunter.
				if (qs.getQuestVarById(0) == 1) {
					if (env.getDialog() == QuestDialog.START_DIALOG) {
						return sendQuestDialog(env, 1693);
					} else if (env.getDialog() == QuestDialog.STEP_TO_2) {
						removeQuestItem(env, 182209025, 1); //A letter From Ntuamu To Sahnu.
						qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
						updateQuestStatus(env);
						return closeDialogWindow(env);
					}
				}
			}
		} 
        else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 205192) { //Sahnu.
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
