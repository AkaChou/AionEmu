package com.aionemu.gameserver.quest.handlers.dimensional_vortex;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 次元漩涡任务脚本：A Lesson To Learn（任务 ID 39003）。
 * Dimensional Vortex quest script: A Lesson To Learn (quest ID 39003).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _39003A_Lesson_To_Learn extends QuestHandler {

	private final static int questId = 39003;
	public _39003A_Lesson_To_Learn() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(800500).addOnQuestStart(questId); //Giniki.
		qe.registerQuestNpc(800500).addOnTalkEvent(questId); //Giniki.
		qe.registerQuestNpc(800512).addOnTalkEvent(questId); //Ionia.
		qe.registerQuestNpc(800504).addOnTalkEvent(questId); //Nevma.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 800500) { //Giniki.
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
		} if (qs == null)
			return false;
		if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 800512: { //Ionia.
					switch (env.getDialog()) {
						case START_DIALOG: {
							playQuestMovie(env, 404);
							return sendQuestDialog(env, 1352);
						} case STEP_TO_1: {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
				            return closeDialogWindow(env);
						}
					}
				} case 800504: { //Nevma.
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2375);
						} case SELECT_REWARD: {
							qs.setQuestVar(1);
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 800504) { //Nevma.
				switch (env.getDialog()) {
					case SELECT_REWARD: {
						return sendQuestDialog(env, 5);
					} default:
						return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}
