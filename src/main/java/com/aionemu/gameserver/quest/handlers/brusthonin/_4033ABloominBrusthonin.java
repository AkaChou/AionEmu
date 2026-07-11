package com.aionemu.gameserver.quest.handlers.brusthonin;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 布鲁斯特霍宁任务脚本：A Bloomin Brusthonin（任务 ID 4033）。
 * Brusthonin quest script: A Bloomin Brusthonin (quest ID 4033).
 *
 * @author Nephis
 */
public class _4033ABloominBrusthonin extends QuestHandler {

	private final static int questId = 4033;
	public _4033ABloominBrusthonin() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(205155).addOnQuestStart(questId); // Heintz
		qe.registerQuestNpc(205155).addOnTalkEvent(questId);
		qe.registerQuestNpc(700379).addOnTalkEvent(questId); // Portaro's Tomb
	}

	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (targetId == 205155) { // Heintz
			if (qs == null || qs.getStatus() == QuestStatus.NONE) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
			else if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 0) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1352);
				else if (env.getDialogId() == 39) {
					if (QuestService.collectItemCheck(env, true)) {
						return sendQuestDialog(env, 1353);
					}
					else {
						return sendQuestDialog(env, 1438);
					}
				}
				else if (env.getDialogId() == 10000) {
				    giveQuestItem(env, 182209042, 1);
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 2);
					updateQuestStatus(env);
					return closeDialogWindow(env);
				}
			}
			else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
				return sendQuestEndDialog(env);
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 2) {
			switch (targetId) {
				case 700379: { // Portaro's Tomb
					if (env.getDialog() == QuestDialog.USE_OBJECT) {
					    return sendQuestDialog(env, 1693);
					}
				    else if (env.getDialog() == QuestDialog.SELECT_ACTION_1694) {
						removeQuestItem(env, 182209042, 1);
					    qs.setStatus(QuestStatus.REWARD);
					    updateQuestStatus(env);
				        return closeDialogWindow(env);
				    }
				}
			}
		}
		return false;
	}
}
