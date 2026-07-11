package com.aionemu.gameserver.quest.handlers.fatebound_abbey;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.model.gameobjects.*;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.questEngine.handlers.*;
import com.aionemu.gameserver.questEngine.model.*;
import com.aionemu.gameserver.utils.*;

/**
 * 命运修道院任务脚本：Protect The Morninglight Plains（任务 ID 29667）。
 * Fatebound Abbey quest script: Protect The Morninglight Plains (quest ID 29667).
 *
 * @author Rinzler (Encom)
 */
public class _29667Protect_The_Morninglight_Plains extends QuestHandler {

	private final static int questId = 29667;
	private final static int[] mobs = {240369, 240370, 240371, 240372, 240373, 240374, 240375, 240376, 240377, 240378};
	public _29667Protect_The_Morninglight_Plains() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(806244).addOnQuestStart(questId);
		qe.registerQuestNpc(806244).addOnTalkEvent(questId);
		qe.registerQuestNpc(806101).addOnTalkEvent(questId);
		for (int mob: mobs) {
			qe.registerQuestNpc(mob).addOnKillEvent(questId);
		}
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		} if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 806244) {
				switch (dialog) {
					case START_DIALOG: {
						if (player.getInventory().getItemCountByItemId(164000336) >= 1) { //修道院返回石。 / Abbey Return Stone.
						    return sendQuestDialog(env, 4762);
						} else {
							PacketSendUtility.broadcastPacket(player, new SM_MESSAGE(player, "You must have <Abbey Return Stone>", ChatType.BRIGHT_YELLOW_CENTER), true);
							return true;
						}
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		} if (qs == null) {
			return false;
		}
        else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 806101) {
				if (env.getDialogId() == 1352) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean onKillEvent(QuestEnv env) {
		Player player = env.getPlayer();
		int targetId = env.getTargetId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() != QuestStatus.START) {
			return false;
		} switch (targetId) {
			case 240369:
			case 240370:
			case 240371:
			case 240372:
			case 240373:
			case 240374:
			case 240375:
			case 240376:
			case 240377:
			case 240378:
				if (qs.getQuestVarById(1) < 12) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 12) {
                    qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
			break;
		}
		return false;
	}
}
