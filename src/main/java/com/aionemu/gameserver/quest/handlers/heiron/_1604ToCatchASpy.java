package com.aionemu.gameserver.quest.handlers.heiron;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 希隆任务脚本：To Catch A Spy（任务 ID 1604）。
 * Heiron quest script: To Catch A Spy (quest ID 1604).
 *
 * @author Balthazar
 */
public class _1604ToCatchASpy extends QuestHandler {

	private final static int questId = 1604;
	public _1604ToCatchASpy() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204576).addOnQuestStart(questId);
		qe.registerQuestNpc(204576).addOnTalkEvent(questId);
		qe.registerQuestNpc(212615).addOnAttackEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204576) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 4762);
				else
					return sendQuestStartDialog(env);
			}
		}
		if (qs == null)
			return false;
		if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204576) {
				if (env.getDialogId() == 1009)
					return sendQuestDialog(env, 10002);
				else
					return sendQuestEndDialog(env);
			}
			return false;
		}
		return false;
	}

	@Override
	public boolean onAttackEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() != QuestStatus.START || qs.getQuestVars().getQuestVars() != 0) {
			return false;
		}
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		}
		if (targetId != 212615) {
			return false;
		}
		if (MathUtil.getDistance(env.getVisibleObject(), 717.78f, 623.50f, 130) < 8) {
			((Npc) env.getVisibleObject()).getController().onDie(player);
			qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
			qs.setStatus(QuestStatus.REWARD);
			updateQuestStatus(env);
		}
		return false;
	}
}
