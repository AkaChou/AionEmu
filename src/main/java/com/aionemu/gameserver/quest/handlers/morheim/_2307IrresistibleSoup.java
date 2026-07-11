package com.aionemu.gameserver.quest.handlers.morheim;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 莫尔海姆任务脚本：Irresistible Soup（任务 ID 2307）。
 * Morheim quest script: Irresistible Soup (quest ID 2307).
 *
 * @author MrPoke remod By Nephis
 */
public class _2307IrresistibleSoup extends QuestHandler {

	private final static int questId = 2307;
	public _2307IrresistibleSoup() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(204378).addOnQuestStart(questId); // Favyr
		qe.registerQuestNpc(204378).addOnTalkEvent(questId);
		qe.registerQuestNpc(204336).addOnTalkEvent(questId); // Spedor
		qe.registerQuestNpc(700247).addOnTalkEvent(questId); // Aromatic Soup
	}

	@Override
	public boolean onDialogEvent(final QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (targetId == 204378) { // Favyr
			if (qs == null || qs.getStatus() == QuestStatus.NONE) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 4762);
				else
					return sendQuestStartDialog(env);
			}
			else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
				    removeQuestItem(env, 182204105, 1);
					removeQuestItem(env, 182204106, 1);
					removeQuestItem(env, 182204107, 1);
					removeQuestItem(env, 182204108, 1);
				return sendQuestEndDialog(env);
			}
		}
		else if (targetId == 204336) { // Spedor
			if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 1) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else if (env.getDialog() == QuestDialog.STEP_TO_1) {
					qs.setQuestVar(2);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					removeQuestItem(env, 182204106, 1);
					return closeDialogWindow(env);
				}
				else if (env.getDialogId() == 1182) {
					qs.setQuestVar(1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					removeQuestItem(env, 182204107, 1);
					return closeDialogWindow(env);
				}
				else if (env.getDialogId() == 1267) {
					qs.setQuestVar(1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					removeQuestItem(env, 182204108, 1);
					return closeDialogWindow(env);
				}
			}
		}
		else if (qs != null && qs.getStatus() == QuestStatus.START && qs.getQuestVarById(0) == 0) {
			switch (targetId) {
				case 700247: // Aromatic Soup
					if (qs.getQuestVarById(0) == 0 && env.getDialog() == QuestDialog.USE_OBJECT) {
						qs.setQuestVar(1);
						updateQuestStatus(env);
					}
				break;
			}
		}
		return false;
	}
}
