package com.aionemu.gameserver.quest.handlers.raksang_ruins;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 拉克桑遗迹任务脚本：Suspicious Minds（任务 ID 28737）。
 * Raksang Ruins quest script: Suspicious Minds (quest ID 28737).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _28737Suspicious_Minds extends QuestHandler {

    private final static int questId = 28737;
    public _28737Suspicious_Minds() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(804732).addOnQuestStart(questId);
		qe.registerQuestNpc(804732).addOnTalkEvent(questId);
		qe.registerQuestNpc(206395).addOnTalkEvent(questId);
		qe.registerQuestNpc(206396).addOnTalkEvent(questId);
		qe.registerQuestNpc(206397).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804732) {
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
				case 206395:
				case 206396:
				case 206397: {
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
		    if (targetId == 206395 ||
			    targetId == 206396 ||
				targetId == 206397) {
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
