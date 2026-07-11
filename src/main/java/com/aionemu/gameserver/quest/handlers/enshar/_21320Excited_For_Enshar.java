package com.aionemu.gameserver.quest.handlers.enshar;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 恩沙尔任务脚本：Excited For Enshar（任务 ID 21320）。
 * Enshar quest script: Excited For Enshar (quest ID 21320).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _21320Excited_For_Enshar extends QuestHandler {

    private final static int questId = 21320;
    public _21320Excited_For_Enshar() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(205847).addOnQuestStart(questId);
		qe.registerQuestNpc(205847).addOnTalkEvent(questId);
		qe.registerQuestNpc(804753).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 205847) {
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
				case 804753: {
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
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 804753) {
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
