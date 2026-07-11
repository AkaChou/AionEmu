package com.aionemu.gameserver.quest.handlers.daevanion;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 大天使之路任务脚本：A Meeting With A Sage（任务 ID 1988）。
 * Daevanion quest script: A Meeting With A Sage (quest ID 1988).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _1988A_Meeting_With_A_Sage extends QuestHandler {
	
	private final static int questId = 1988;
	public _1988A_Meeting_With_A_Sage() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(203725).addOnQuestStart(questId);
		qe.registerQuestNpc(203725).addOnTalkEvent(questId);
		qe.registerQuestNpc(203989).addOnTalkEvent(questId);
		qe.registerQuestNpc(203771).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 203725) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
		} 
		else if (targetId == 203989) {
			if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 0) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1352);
				else if (env.getDialog() == QuestDialog.STEP_TO_1) {
                    return defaultCloseDialog(env, 0, 1);
				}
			}
		} 
		else if (targetId == 798018) {
			if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 1) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1693);
				else if (env.getDialog() == QuestDialog.STEP_TO_2) {
                    return defaultCloseDialog(env, 1, 2);
				}
			}
		} 
		else if (targetId == 203771) {
			if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 2) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 2034);
				else if (env.getDialogId() == 1009) {
					removeQuestItem(env, 186000039, 1);
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
