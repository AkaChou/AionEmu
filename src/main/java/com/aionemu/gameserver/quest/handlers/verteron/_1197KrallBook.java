package com.aionemu.gameserver.quest.handlers.verteron;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 沃特伦任务脚本：Krall Book（任务 ID 1197）。
 * Verteron quest script: Krall Book (quest ID 1197).
 *
 * @author MrPoke
 * @modified Nephis, Rolandas
 */
public class _1197KrallBook extends QuestHandler {

	private final static int questId = 1197;
	public _1197KrallBook() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(700004).addOnTalkEvent(questId);
		qe.registerQuestNpc(203129).addOnTalkEvent(questId);
		qe.registerQuestItem(182200558, questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (targetId == 0) {
			if (env.getDialogId() == 1002) {
				return sendQuestStartDialog(env);
			}
			if (env.getDialogId() == 1003) {
                return closeDialogWindow(env);
			}
		}
		else if (targetId == 700004) {
			if ((qs == null || qs.getStatus() == QuestStatus.NONE)) {
				if (player.getInventory().getItemCountByItemId(182200558) == 0) {
					if (giveQuestItem(env, 182200558, 1)) {
						VisibleObject target = player.getTarget();
						if (target != null && target instanceof Npc) {
							((Npc) target).getController().scheduleRespawn();
							target.getController().onDelete();
						}
					}
				}
			}
			return true;
		}
		else if (targetId == 203129) {
			if (qs != null && qs.getStatus() == QuestStatus.START) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 2375);
				}
				else if (env.getDialogId() == 1009) {
					removeQuestItem(env, 182200558, 1);
					qs.setQuestVar(1);
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

	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			return HandlerResult.fromBoolean(sendQuestDialog(env, 4));
		}
		return HandlerResult.FAILED;
	}
}
