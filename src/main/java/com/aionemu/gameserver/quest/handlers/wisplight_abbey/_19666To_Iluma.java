package com.aionemu.gameserver.quest.handlers.wisplight_abbey;

import com.aionemu.gameserver.model.*;
import com.aionemu.gameserver.model.gameobjects.*;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.questEngine.handlers.*;
import com.aionemu.gameserver.questEngine.model.*;
import com.aionemu.gameserver.utils.*;

/**
 * 微光修道院任务脚本：To Iluma（任务 ID 19666）。
 * Wisplight Abbey quest script: To Iluma (quest ID 19666).
 *
 * @author Rinzler (Encom)
 */
public class _19666To_Iluma extends QuestHandler {

	private final static int questId = 19666;
	public _19666To_Iluma() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(804651).addOnQuestStart(questId); //Lena.
		qe.registerQuestNpc(806243).addOnTalkEvent(questId); //Gilios.
		qe.registerQuestNpc(806134).addOnTalkEvent(questId); //Ador.
		qe.registerQuestNpc(806114).addOnTalkEvent(questId); //Ilisia.
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
				case 806243: { //Gilios.
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1011);
						} case STEP_TO_1: {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
				            return closeDialogWindow(env);
						}
					}
				}
				case 806134: { //Ador.
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1352);
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
            if (targetId == 806114) { //Ilisia.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 10002);
                } else {
                    return sendQuestEndDialog(env);
                }
            }
        }
		return false;
	}
}
