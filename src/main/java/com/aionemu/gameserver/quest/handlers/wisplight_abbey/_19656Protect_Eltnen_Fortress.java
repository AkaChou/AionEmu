package com.aionemu.gameserver.quest.handlers.wisplight_abbey;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.model.gameobjects.*;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.questEngine.handlers.*;
import com.aionemu.gameserver.questEngine.model.*;
import com.aionemu.gameserver.utils.*;

import java.util.Objects;

/**
 * 微光修道院任务脚本：Protect Eltnen Fortress（任务 ID 19656）。
 * Wisplight Abbey quest script: Protect Eltnen Fortress (quest ID 19656).
 *
 * @author Rinzler (Encom)
 */
public class _19656Protect_Eltnen_Fortress extends QuestHandler {

	private final static int questId = 19656;
	private final static int[] mobs = {210755, 211690, 211691};
	public _19656Protect_Eltnen_Fortress() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(804651).addOnQuestStart(questId);
		qe.registerQuestNpc(804651).addOnTalkEvent(questId);
		qe.registerQuestNpc(804651).addOnTalkEvent(questId);
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
			if (targetId == 804651) {
				switch (dialog) {
					case START_DIALOG: {
						if (player.getInventory().getItemCountByItemId(164000335) >= 1) { //修道院返回石。 / Abbey Return Stone.
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
		}
        else if (qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 804651) {
                if (Objects.requireNonNull(dialog) == QuestDialog.SELECT_REWARD) {
                    return sendQuestDialog(env, 5);
                }
                return sendQuestEndDialog(env);
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
			case 210755:
			case 211690:
			case 211691:
				if (qs.getQuestVarById(1) < 10) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 10) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
			break;
		}
		return false;
	}
}
