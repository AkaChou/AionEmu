package com.aionemu.gameserver.quest.handlers.crafting;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 制作任务脚本：Expert Aethertapping Expert（任务 ID 19003）。
 * Crafting quest script: Expert Aethertapping Expert (quest ID 19003).
 */
public class _19003ExpertAethertappingExpert extends QuestHandler {

	private final static int questId = 19003;
	public _19003ExpertAethertappingExpert() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(203782).addOnQuestStart(questId);
		qe.registerQuestNpc(203782).addOnTalkEvent(questId);
		qe.registerQuestNpc(203700).addOnTalkEvent(questId);
	}
	

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		} if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 203782) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				} else {
					return sendQuestStartDialog(env, 182206128, 1);
				} 
			}
		} if (qs == null) {
			return false;
		} if (qs != null && qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 203700: {
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2375);
						}
						case SELECT_REWARD:
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
					        return sendQuestEndDialog(env);
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203700) {
				if (env.getDialogId() == 39) {
					return sendQuestDialog(env, 5);
				} else {
					player.getSkillList().addSkill(player, 30003, 400);
					removeQuestItem(env, 182206128, 1);
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}
