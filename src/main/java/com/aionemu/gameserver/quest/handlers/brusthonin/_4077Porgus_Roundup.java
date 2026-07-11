package com.aionemu.gameserver.quest.handlers.brusthonin;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 布鲁斯特霍宁任务脚本：Porgus Roundup（任务 ID 4077）。
 * Brusthonin quest script: Porgus Roundup (quest ID 4077).
 */
public class _4077Porgus_Roundup extends QuestHandler
{

	private final static int questId = 4077;
	
	public _4077Porgus_Roundup() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(205158).addOnQuestStart(questId); //Holekk.
		qe.registerQuestNpc(205158).addOnTalkEvent(questId); //Holekk.
		qe.registerQuestNpc(214732).addOnAttackEvent(questId); //Runaway Porgus.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		} if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 205158) { //Holekk.
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 205158) { //Holekk.
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
	
	@Override
	public boolean onAttackEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		} if (targetId != 214732) { //Runaway Porgus.
			return false;
		}
		final Npc npc = (Npc) env.getVisibleObject();
		if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 0) {
			if (MathUtil.getDistance(1356, 1901, 46, npc.getX(), npc.getY(), npc.getZ()) > 10) {
				return false;
			}
			qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
			updateQuestStatus(env);
			npc.getController().scheduleRespawn();
			npc.getController().onDelete();
			return true;
		} else if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 1) {
			if (MathUtil.getDistance(1356, 1901, 46, npc.getX(), npc.getY(), npc.getZ()) > 10) {
				return false;
			}
			qs.setStatus(QuestStatus.REWARD);
			updateQuestStatus(env);
			npc.getController().scheduleRespawn();
			npc.getController().onDelete();
			return true;
		}
		return false;
	}
}
