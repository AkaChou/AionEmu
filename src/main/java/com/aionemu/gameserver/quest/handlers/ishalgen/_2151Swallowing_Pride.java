package com.aionemu.gameserver.quest.handlers.ishalgen;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 伊沙尔根任务脚本：Swallowing Pride（任务 ID 2151）。
 * Ishalgen quest script: Swallowing Pride (quest ID 2151).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _2151Swallowing_Pride extends QuestHandler {

    private final static int questId = 2151;
    public _2151Swallowing_Pride() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(801035).addOnQuestStart(questId); //Nowlan.
		qe.registerQuestNpc(801034).addOnTalkEvent(questId); //Rian.
		qe.registerQuestNpc(801034).addOnTalkEvent(questId); //Rian.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 801035) { //Nowlan.
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
				}
			}
		} 
        else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 801034: { //Rian.
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2375);
						} case SELECT_REWARD: {
							changeQuestStep(env, 0, 0, true);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		} else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 801034) { //Rian.
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
