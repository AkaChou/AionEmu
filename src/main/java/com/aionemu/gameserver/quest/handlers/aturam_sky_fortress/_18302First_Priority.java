package com.aionemu.gameserver.quest.handlers.aturam_sky_fortress;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 阿图拉姆天空要塞任务脚本：First Priority（任务 ID 18302）。
 * Aturam Sky Fortress quest script: First Priority (quest ID 18302).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _18302First_Priority extends QuestHandler
{
	private final static int questId = 18302;
	private int[] dredgionGenerator = new int[] {702650, 702651, 702652, 702653, 702654};
	
	public _18302First_Priority() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(799530).addOnQuestStart(questId);
		qe.registerQuestNpc(799530).addOnTalkEvent(questId);
		qe.registerQuestNpc(730375).addOnTalkEvent(questId);
		for (int id: dredgionGenerator) {
			qe.registerQuestNpc(id).addOnKillEvent(questId);
		}
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		} if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 799530) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					playQuestMovie(env, 469);
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} if (qs == null) {
			return false;
		}
		int var = qs.getQuestVarById(0);
		if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 730375) {
				if (var == 5) {
					switch (env.getDialog()) {
						case USE_OBJECT:
							return sendQuestDialog(env, 1352);
						case SET_REWARD:
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							return closeDialogWindow(env);
						default:
							return sendQuestDialog(env, 2716);
					}
				}
			} else if (targetId == 799530) {
				return sendQuestDialog(env, 1004);
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799530) {
				if (env.getDialog().equals(QuestDialog.SELECT_REWARD)) {
					return sendQuestDialog(env, 5);
				} else
					return sendQuestEndDialog(env);
				}
			}
		return false;
	}
	
	@Override
	public boolean onKillEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() != QuestStatus.START) {
			return false;
		}
		int targetId = 0;
		int var = qs.getQuestVarById(0);
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		} if (var > 4) {
			return false;
		} for (int id : dredgionGenerator) {
			if (targetId == id) {
				qs.setQuestVarById(0, var+1);
                updateQuestStatus(env);
                return true;
			}
		}
		return false;
	}
}
