package com.aionemu.gameserver.quest.handlers.morheim;

import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.teleport.TeleportService2;

/**
 * 莫尔海姆任务脚本：There And Back Again（任务 ID 2423）。
 * Morheim quest script: There And Back Again (quest ID 2423).
 *
 * @author Cheatkiller
 */
public class _2423ThereAndBackAgain extends QuestHandler {

	private final static int questId = 2423;
	public _2423ThereAndBackAgain() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204326).addOnQuestStart(questId); 
		qe.registerQuestNpc(204375).addOnTalkEvent(questId);  
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204326){
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 4762);
				else
					return sendQuestStartDialog(env);
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 204375: { 
					switch (env.getDialog()) {
						case START_DIALOG: {
							if (qs.getQuestVarById(0) == 0)
								return sendQuestDialog(env, 1011);
							else if(qs.getQuestVarById(0) == 1)
								return sendQuestDialog(env, 1352);
							else if(qs.getQuestVarById(0) == 2)
								return sendQuestDialog(env, 1693);
						}
						case CHECK_COLLECTED_ITEMS: {
							return checkQuestItems(env, 1, 2, false, 10000, 10001);
						}
						case STEP_TO_3: {
							TeleportService2.teleportTo(player, 210020000, 1, 370.13f, 2682.59f, 171, (byte) 30, TeleportAnimation.BEAM_ANIMATION);
							qs.setQuestVar(3);
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
					        return closeDialogWindow(env);
						}
						case SELECT_ACTION_1779: {
							return sendQuestDialog(env, 1779);
						}
						case STEP_TO_1: {
							TeleportService2.teleportTo(player, 210020000, 1, 370.13f, 2682.59f, 171, (byte) 30, TeleportAnimation.BEAM_ANIMATION);
							qs.setQuestVar(1);
					        return closeDialogWindow(env);
						}
					}
				}
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204375) { 
				if (env.getDialog() == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 10002);
				}
				else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}
