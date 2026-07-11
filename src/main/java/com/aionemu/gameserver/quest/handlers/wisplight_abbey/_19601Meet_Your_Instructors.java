package com.aionemu.gameserver.quest.handlers.wisplight_abbey;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.model.gameobjects.*;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.questEngine.handlers.*;
import com.aionemu.gameserver.questEngine.model.*;
import com.aionemu.gameserver.utils.*;

/**
 * 微光修道院任务脚本：Meet Your Instructors（任务 ID 19601）。
 * Wisplight Abbey quest script: Meet Your Instructors (quest ID 19601).
 *
 * @author Rinzler (Encom)
 */
public class _19601Meet_Your_Instructors extends QuestHandler {

	private final static int questId = 19601;
	public _19601Meet_Your_Instructors() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(804651).addOnQuestStart(questId); //Lena.
		qe.registerQuestNpc(804651).addOnTalkEvent(questId); //Lena.
		qe.registerQuestNpc(805304).addOnTalkEvent(questId); //Margges.
		qe.registerQuestNpc(804652).addOnTalkEvent(questId); //Rosette.
		qe.registerQuestNpc(804653).addOnTalkEvent(questId); //Deronis.
		qe.registerQuestNpc(804654).addOnTalkEvent(questId); //Agnes.
		qe.registerQuestNpc(804655).addOnTalkEvent(questId); //Kiran.
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
			if (targetId == 804651) { //Lena.
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
		} if (qs == null) {
			return false;
		} if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 805304: { //Margges.
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1011);
						} case STEP_TO_1: {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
					        return closeDialogWindow(env);
						}
					}
				} case 804652: { //Rosette.
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1352);
						} case STEP_TO_2: {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
					        return closeDialogWindow(env);
						}
					}
				} case 804653: { //Deronis.
				    switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1693);
						} case STEP_TO_3: {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
					        return closeDialogWindow(env);
						}
					}
				} case 804654: { //Agnes.
				    switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2034);
						} case SET_REWARD: {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
					        return closeDialogWindow(env);
						}
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 804655) { //Kiran.
			    switch (dialog) {
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
