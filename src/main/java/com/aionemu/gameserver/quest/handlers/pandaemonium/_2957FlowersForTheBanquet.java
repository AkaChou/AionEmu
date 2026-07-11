package com.aionemu.gameserver.quest.handlers.pandaemonium;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 潘德莫尼姆任务脚本：Flowers For The Banquet（任务 ID 2957）。
 * Pandaemonium quest script: Flowers For The Banquet (quest ID 2957).
 *
 * @author Cheatkiller
 */
public class _2957FlowersForTheBanquet extends QuestHandler {

	private final static int questId = 2957;

	public _2957FlowersForTheBanquet() {
		super(questId);
	}

	public void register() {
		qe.registerQuestNpc(204127).addOnQuestStart(questId);
		qe.registerQuestNpc(204129).addOnTalkEvent(questId);
		qe.registerQuestNpc(798065).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204127) { 
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				}
				else {
					return sendQuestStartDialog(env);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 204129) {
				if (dialog == QuestDialog.START_DIALOG) {
					if(qs.getQuestVarById(0) == 0) {
						return sendQuestDialog(env, 1352);
					} 
                    if(qs.getQuestVarById(0) == 2) {
					    return sendQuestDialog(env, 2375);
				    }
				}
				else if (dialog == QuestDialog.STEP_TO_1) {
					return defaultCloseDialog(env, 0, 1);
				}
				else if (dialog == QuestDialog.SELECT_REWARD) {
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return sendQuestEndDialog(env);
				}
			}
		else if (targetId == 798065) {
			if (dialog == QuestDialog.START_DIALOG) {
				if(qs.getQuestVarById(0) == 1)
					return sendQuestDialog(env, 1693);
				}
				else if (dialog == QuestDialog.STEP_TO_2) {
					return defaultCloseDialog(env, 1, 2);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 204129) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
