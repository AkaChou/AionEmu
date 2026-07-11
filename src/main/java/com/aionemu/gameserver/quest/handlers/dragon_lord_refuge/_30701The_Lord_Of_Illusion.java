package com.aionemu.gameserver.quest.handlers.dragon_lord_refuge;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 龙王避难所任务脚本：The Lord Of Illusion（任务 ID 30701）。
 * Dragon Lord Refuge quest script: The Lord Of Illusion (quest ID 30701).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _30701The_Lord_Of_Illusion extends QuestHandler {

	private final static int questId = 30701;
	private final static int npcs [] = {804868, 800430, 800350};
	public _30701The_Lord_Of_Illusion() {
		super(questId);
	}
	
	@Override
	public boolean onKillEvent(QuestEnv env) {
	  return defaultOnKillEvent(env, 219362, 0, 1);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(804868).addOnQuestStart(questId);
		for (int npc: npcs) {
			qe.registerQuestNpc(npc).addOnTalkEvent(questId);
		}
		qe.registerQuestNpc(219362).addOnKillEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804868) {
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} if (qs == null) {
			return false;
		}
		if (qs.getStatus() == QuestStatus.START) {
		int var = qs.getQuestVarById(0);
			if (targetId == 800430) {
				switch (dialog) {
					case START_DIALOG: {
						if (var == 1) {
							return sendQuestDialog(env, 1352);
						}
					} case STEP_TO_2: {
						return defaultCloseDialog(env, 1, 2);
					}
				}
			} else if (targetId == 800350) {
				switch (dialog) {
					case START_DIALOG: {
						if (var == 2) {
							return sendQuestDialog(env, 1693);
						}
					} case SET_REWARD: {
						return defaultCloseDialog(env, 2, 3, true, false);
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804868) {
				if (dialog == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 10002);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}
