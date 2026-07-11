package com.aionemu.gameserver.quest.handlers.eltnen;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 艾特南任务脚本：A Leaf From Lodas（任务 ID 1322）。
 * Eltnen quest script: A Leaf From Lodas (quest ID 1322).
 *
 * @author MrPoke remod By Xitanium
 */
public class _1322ALeafFromLodas extends QuestHandler {

	private final static int questId = 1322;
	public _1322ALeafFromLodas() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(730019).addOnQuestStart(questId);
		qe.registerQuestNpc(730019).addOnTalkEvent(questId);
		qe.registerQuestNpc(730008).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (targetId == 730019) { // Lodas
			if (qs == null || qs.getStatus() == QuestStatus.NONE) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				if (env.getDialog() == QuestDialog.SELECT_ACTION_1012) {
					return sendQuestDialog(env, 1012);
				}
				if (env.getDialog() == QuestDialog.ASK_ACCEPTION) {
					return sendQuestDialog(env, 4);
				}
				if (env.getDialog() == QuestDialog.ACCEPT_QUEST) {
					return sendQuestStartDialog(env, 182201308, 1);
				}
				if (env.getDialog() == QuestDialog.REFUSE_QUEST) {
				    return closeDialogWindow(env);
				}
			}
			else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
				if (env.getDialog() == QuestDialog.USE_OBJECT)
					return sendQuestDialog(env, 2375);
				if (env.getDialog() == QuestDialog.SELECT_ACTION_2376) {
			        removeQuestItem(env, 182201374, 1);
					return sendQuestDialog(env, 2376);
				}
				else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
		else if (targetId == 730008) { // Daminu
			if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 0) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1352);
				if (env.getDialog() == QuestDialog.SELECT_ACTION_1353) {
			        removeQuestItem(env, 182201308, 1);
					return sendQuestDialog(env, 1353);
				}
				else if (env.getDialog() == QuestDialog.STEP_TO_1) {
			        giveQuestItem(env, 182201374, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				    return closeDialogWindow(env);
				}
			}
		}
		return false;
	}
}
