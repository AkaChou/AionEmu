package com.aionemu.gameserver.quest.handlers.raksang_ruins;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 拉克桑遗迹任务脚本：Where Did Abiso Go（任务 ID 18737）。
 * Raksang Ruins quest script: Where Did Abiso Go (quest ID 18737).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _18737Where_Did_Abiso_Go extends QuestHandler {

    private final static int questId = 18737;
    public _18737Where_Did_Abiso_Go() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(804707).addOnQuestStart(questId);
		qe.registerQuestNpc(804707).addOnTalkEvent(questId);
		qe.registerQuestNpc(206378).addOnTalkEvent(questId);
		qe.registerQuestNpc(206379).addOnTalkEvent(questId);
		qe.registerQuestNpc(206380).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804707) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 206378:
				case 206379:
				case 206380: {
					switch (dialog) {
						case START_DIALOG: {
							return sendQuestDialog(env, 10002);
						} case SELECT_REWARD: {
							changeQuestStep(env, 0, 0, true);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 206378 ||
			    targetId == 206379 ||
				targetId == 206380) {
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
