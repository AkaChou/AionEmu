package com.aionemu.gameserver.quest.handlers.chantra_dredgion;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 钱特拉德雷金任务脚本：Disarm The Chantra Dredgion（任务 ID 3721）。
 * Chantra Dredgion quest script: Disarm The Chantra Dredgion (quest ID 3721).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _3721Disarm_The_Chantra_Dredgion extends QuestHandler {

	private static final int questId = 3721;
	public _3721Disarm_The_Chantra_Dredgion() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(798928).addOnQuestStart(questId); //Yulia.
		qe.registerQuestNpc(798928).addOnTalkEvent(questId); //Yulia.
		qe.registerQuestNpc(799069).addOnTalkEvent(questId); //Yannis.
		qe.registerQuestNpc(700948).addOnTalkEvent(questId); //Balaur Weapon.
		qe.registerQuestNpc(216886).addOnKillEvent(questId); //Captain Zanata.
		qe.registerGetingItem(182202193, questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 798928) { //Yulia.
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0); 
			if (targetId == 799069) { //Yannis.
				switch (dialog) {
					case START_DIALOG: {
						if (var == 0) {
							return sendQuestDialog(env, 1011);
						}
					} case STEP_TO_1: {
						return defaultCloseDialog(env, 0, 1);
					}
				}
			} else if (targetId == 700948) { //Balaur Weapon.
				if (dialog == QuestDialog.USE_OBJECT) {
					return closeDialogWindow(env);
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798928) { //Yulia.
				if (dialog == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 10002);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean onKillEvent(QuestEnv env) {
		return defaultOnKillEvent(env, 216886, 2, true);
	}

	@Override
	public boolean onGetItemEvent(QuestEnv env) {
		return defaultOnGetItemEvent(env, 1, 2, false);
	}
}
