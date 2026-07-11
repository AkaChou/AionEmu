package com.aionemu.gameserver.quest.handlers.beluslan;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 贝勒斯兰任务脚本：Curseofthe Were Ribbit（任务 ID 2538）。
 * Beluslan quest script: Curseofthe Were Ribbit (quest ID 2538).
 *
 * @author Ritsu
 */
public class _2538CurseoftheWereRibbit extends QuestHandler {

	private final static int questId = 2538;
	public _2538CurseoftheWereRibbit() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204827).addOnQuestStart(questId);
		qe.registerQuestNpc(204827).addOnTalkEvent(questId);
		qe.registerQuestNpc(790002).addOnTalkEvent(questId);
		qe.registerQuestNpc(204805).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if(env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204827) {
				if (dialog == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			if (targetId == 204827) {
				switch (dialog) {
					case START_DIALOG:
						if (var == 3) {
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							return sendQuestDialog(env, 2375);
						}
					case SELECT_REWARD:
						return sendQuestDialog(env, 5);
				}
			}
			if (targetId == 790002) {
				switch (dialog) {
					case START_DIALOG:
						if (var == 0)
							return sendQuestDialog(env, 1352);
						if (var == 2)
							return sendQuestDialog(env, 2034);
					case STEP_TO_1:
						if (var == 0) {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
                            return closeDialogWindow(env);
						}
					case STEP_TO_3:
						if (var == 2) {
							removeQuestItem(env, 182204517, 1);
							giveQuestItem(env, 182204518, 1);	
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
                            return closeDialogWindow(env);
						}
				}
			}
			if (targetId == 204805) {
				switch (dialog) {
					case START_DIALOG:
						if (var == 1)
							return sendQuestDialog(env, 1693);
					case STEP_TO_2:
						if (var == 1) {
							giveQuestItem(env, 182204517, 1);
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
                            return closeDialogWindow(env);
						}
				}
			}
		}
		else if (qs == null || qs.getStatus() == QuestStatus.REWARD){
			if (targetId == 204827) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
