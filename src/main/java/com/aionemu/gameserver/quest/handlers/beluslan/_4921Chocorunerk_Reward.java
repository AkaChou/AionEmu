package com.aionemu.gameserver.quest.handlers.beluslan;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 贝勒斯兰任务脚本：Chocorunerk Reward（任务 ID 4921）。
 * Beluslan quest script: Chocorunerk Reward (quest ID 4921).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _4921Chocorunerk_Reward extends QuestHandler {

    private final static int questId = 4921;
    public _4921Chocorunerk_Reward() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(804609).addOnQuestStart(questId);
		qe.registerQuestNpc(804609).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804609) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
                    case ASK_ACCEPTION:
                        return sendQuestDialog(env, 4);
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 804609: {
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
		    if (targetId == 804609) {
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
