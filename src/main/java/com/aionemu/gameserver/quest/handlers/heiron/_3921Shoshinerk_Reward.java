package com.aionemu.gameserver.quest.handlers.heiron;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 希隆任务脚本：Shoshinerk Reward（任务 ID 3921）。
 * Heiron quest script: Shoshinerk Reward (quest ID 3921).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _3921Shoshinerk_Reward extends QuestHandler {

    private final static int questId = 3921;
    public _3921Shoshinerk_Reward() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(804603).addOnQuestStart(questId);
		qe.registerQuestNpc(804603).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804603) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
                    case ASK_ACCEPTION:
                        return sendQuestDialog(env, 4);
					case ACCEPT_QUEST:
						return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 804603: {
					switch (dialog) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2375);
						} case SELECT_REWARD: {
							changeQuestStep(env, 0, 0, true);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 804603) {
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
