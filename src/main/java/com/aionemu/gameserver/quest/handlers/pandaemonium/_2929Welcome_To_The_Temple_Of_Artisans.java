package com.aionemu.gameserver.quest.handlers.pandaemonium;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 潘德莫尼姆任务脚本：Welcome To The Temple Of Artisans（任务 ID 2929）。
 * Pandaemonium quest script: Welcome To The Temple Of Artisans (quest ID 2929).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _2929Welcome_To_The_Temple_Of_Artisans extends QuestHandler
{
    private final static int questId = 2929;
	
    public _2929Welcome_To_The_Temple_Of_Artisans() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(204092).addOnQuestStart(questId);
		qe.registerQuestNpc(204092).addOnTalkEvent(questId);
		qe.registerQuestNpc(798317).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204092) {
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
				case 798317: {
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
		    if (targetId == 798317) {
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
