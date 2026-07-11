package com.aionemu.gameserver.quest.handlers.poeta;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 波伊塔任务脚本：Insomnia Medicine（任务 ID 1111）。
 * Poeta quest script: Insomnia Medicine (quest ID 1111).
 *
 * @author MrPoke
 */
public class _1111InsomniaMedicine extends QuestHandler {

	private final static int questId = 1111;
	public _1111InsomniaMedicine() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203075).addOnQuestStart(questId);
		qe.registerQuestNpc(203075).addOnTalkEvent(questId);
		qe.registerQuestNpc(203061).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (targetId == 203075) {
			if (qs == null || qs.getStatus() == QuestStatus.NONE) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
			else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
				if (env.getDialog() == QuestDialog.USE_OBJECT) {
					if (qs.getQuestVarById(0) == 2) {
						removeQuestItem(env, 182200222, 1);
						return sendQuestDialog(env, 2375);
					}
					else if (qs.getQuestVarById(0) == 3) {
						removeQuestItem(env, 182200221, 1);
						return sendQuestDialog(env, 2716);
					}
					return false;
				}
				else if (env.getDialogId() == 1009)
					return sendQuestDialog(env, qs.getQuestVarById(0) + 3);
				else if (env.getDialogId() == 23) {
					QuestService.finishQuest(env);
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(env.getVisibleObject().getObjectId(), 10));
					return true;
				}
			}
		}
        else if (qs == null || qs.getStatus() == QuestStatus.START) {
		   if (targetId == 203061) {
			if (env.getDialog() == QuestDialog.START_DIALOG) {
				if (qs.getQuestVarById(0) == 0)
					return sendQuestDialog(env, 1352);
				else if (qs.getQuestVarById(0) == 1)
					return sendQuestDialog(env, 1353);
				return false;
			}
			else if (env.getDialogId() == 39) {
				if (QuestService.collectItemCheck(env, true)) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					updateQuestStatus(env);
					return sendQuestDialog(env, 1353);
				}
				else
					return sendQuestDialog(env, 1693);
			}
			else if (env.getDialog() == QuestDialog.STEP_TO_1) {
				giveQuestItem(env, 182200222, 1);
                qs.setStatus(QuestStatus.REWARD);
				qs.setQuestVarById(0, 2);
				updateQuestStatus(env);
				return closeDialogWindow(env);
			}
			else if (env.getDialog() == QuestDialog.STEP_TO_2) {
				giveQuestItem(env, 182200221, 1);
                qs.setStatus(QuestStatus.REWARD);
				qs.setQuestVarById(0, 3);
				updateQuestStatus(env);
				return closeDialogWindow(env);
                }
			}
		}
		return false;
	}
}
