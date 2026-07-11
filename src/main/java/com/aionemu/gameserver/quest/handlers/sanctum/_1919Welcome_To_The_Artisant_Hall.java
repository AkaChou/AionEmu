package com.aionemu.gameserver.quest.handlers.sanctum;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 圣所任务脚本：Welcome To The Artisant Hall（任务 ID 1919）。
 * Sanctum quest script: Welcome To The Artisant Hall (quest ID 1919).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _1919Welcome_To_The_Artisant_Hall extends QuestHandler {

    private final static int questId = 1919;
    public _1919Welcome_To_The_Artisant_Hall() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(203779).addOnQuestStart(questId);
		qe.registerQuestNpc(203779).addOnTalkEvent(questId);
		qe.registerQuestNpc(798316).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (env.getTargetId() == 203779) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			switch (env.getTargetId()) {
				case 798316: {
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 10002);
						} case SELECT_REWARD: {
							changeQuestStep(env, 0, 0, true);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		} else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
		    if (env.getTargetId() == 798316) {
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
