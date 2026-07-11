package com.aionemu.gameserver.quest.handlers.eltnen;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 艾特南任务脚本：Ganimerks Espionage（任务 ID 1472）。
 * Eltnen quest script: Ganimerks Espionage (quest ID 1472).
 *
 * @author MrPoke remod By Xitanium
 */
public class _1472GanimerksEspionage extends QuestHandler {

	private final static int questId = 1472;
	public _1472GanimerksEspionage() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203903).addOnQuestStart(questId); // Valerius
		qe.registerQuestNpc(203903).addOnTalkEvent(questId); // Valerius
		qe.registerQuestNpc(798114).addOnTalkEvent(questId); // Ganimerk
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (targetId == 203903) {
			if (qs == null || qs.getStatus() == QuestStatus.NONE) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 4762);
				else
					return sendQuestStartDialog(env);
			}
			else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
				return sendQuestEndDialog(env);
			}
		}
		else if (targetId == 798114) {
			if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 0) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else if (env.getDialog() == QuestDialog.STEP_TO_1) {
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				    return closeDialogWindow(env);
				}
			}
		}
		return false;
	}
}
