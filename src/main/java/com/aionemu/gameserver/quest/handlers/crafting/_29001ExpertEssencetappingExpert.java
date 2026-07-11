package com.aionemu.gameserver.quest.handlers.crafting;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 制作任务脚本：Expert Essencetapping Expert（任务 ID 29001）。
 * Crafting quest script: Expert Essencetapping Expert (quest ID 29001).
 */
public class _29001ExpertEssencetappingExpert extends QuestHandler {

	private final static int questId = 29001;
	public _29001ExpertEssencetappingExpert() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(204096).addOnQuestStart(questId);
		qe.registerQuestNpc(204096).addOnTalkEvent(questId);
		qe.registerQuestNpc(204052).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		} if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204096) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				} else {
					return sendQuestStartDialog(env, 182207141, 1);
				}
			}
		} if (qs == null) {
			return false;
		} if (qs != null && qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 204052: {
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
			if (targetId == 204052) {
				if (env.getDialogId() == 39) {
					return sendQuestDialog(env, 5);
				} else {
					player.getSkillList().addSkill(player, 30002, 400);
					removeQuestItem(env, 182207141, 1);
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}
