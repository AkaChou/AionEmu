package com.aionemu.gameserver.quest.handlers.levinshor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 莱文绍尔任务脚本：Levinshor Chagrin（任务 ID 23701）。
 * Levinshor quest script: Levinshor Chagrin (quest ID 23701).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23701Levinshor_Chagrin extends QuestHandler {

    private final static int questId = 23701;
    public _23701Levinshor_Chagrin() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(799225).addOnQuestStart(questId); //Richelle.
		qe.registerQuestNpc(799225).addOnTalkEvent(questId); //Richelle.
		qe.registerQuestNpc(802353).addOnTalkEvent(questId); //Yasan.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 799225) { //Richelle.
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 802353: { //Yasan.
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
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 802353) { //Yasan.
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
