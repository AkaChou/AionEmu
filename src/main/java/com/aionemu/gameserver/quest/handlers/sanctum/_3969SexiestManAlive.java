package com.aionemu.gameserver.quest.handlers.sanctum;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 圣所任务脚本：Sexiest Man Alive（任务 ID 3969）。
 * Sanctum quest script: Sexiest Man Alive (quest ID 3969).
 *
 * @author Rolandas
 */
public class _3969SexiestManAlive extends QuestHandler {

	private final static int questId = 3969;
	public _3969SexiestManAlive() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(798390).addOnQuestStart(questId);// Palentine
		qe.registerQuestNpc(798391).addOnTalkEvent(questId);// Andu
		qe.registerQuestNpc(798390).addOnTalkEvent(questId);// Palentine
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (targetId == 798390) {
			if (qs == null || qs.getStatus() == QuestStatus.NONE) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else if (env.getDialogId() == 1007) {
					return sendQuestDialog(env, 4);
				}
				else if (env.getDialogId() == 1002) {
				    return sendQuestStartDialog(env, 182206126, 1);
				}
			}
			else if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 1) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 2375);
				else if (env.getDialogId() == 1009) {
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return sendQuestEndDialog(env);
				}
			}
            else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
		        return sendQuestEndDialog(env);
			}
		}
		else if (targetId == 798391) {
			if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 0) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1352);
				else if (env.getDialog() == QuestDialog.STEP_TO_1) {
                    removeQuestItem(env, 182206126, 1);
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					updateQuestStatus(env);
                    return closeDialogWindow(env);
				}
			}
		}
		return false;
	}
}
