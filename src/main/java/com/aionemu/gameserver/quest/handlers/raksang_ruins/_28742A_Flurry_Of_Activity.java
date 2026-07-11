package com.aionemu.gameserver.quest.handlers.raksang_ruins;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 拉克桑遗迹任务脚本：A Flurry Of Activity（任务 ID 28742）。
 * Raksang Ruins quest script: A Flurry Of Activity (quest ID 28742).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _28742A_Flurry_Of_Activity extends QuestHandler {

    private final static int questId = 28742;
    public _28742A_Flurry_Of_Activity() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(206395).addOnQuestStart(questId);
		qe.registerQuestNpc(206396).addOnQuestStart(questId);
		qe.registerQuestNpc(206397).addOnQuestStart(questId);
		qe.registerQuestNpc(804732).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 206395 || targetId == 206396 || targetId == 206397) {
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
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 804732) {
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
				    } case CHECK_COLLECTED_ITEMS: {
                        return checkQuestItems(env, 0, 1, true, 5, 10001);
                    }
				}
			}
		} else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 804732) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
