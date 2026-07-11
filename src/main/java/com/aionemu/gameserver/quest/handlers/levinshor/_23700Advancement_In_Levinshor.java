package com.aionemu.gameserver.quest.handlers.levinshor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 莱文绍尔任务脚本：Advancement In Levinshor（任务 ID 23700）。
 * Levinshor quest script: Advancement In Levinshor (quest ID 23700).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23700Advancement_In_Levinshor extends QuestHandler {

    private final static int questId = 23700;
    public _23700Advancement_In_Levinshor() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(804719).addOnQuestStart(questId); //Haldor.
		qe.registerQuestNpc(804719).addOnTalkEvent(questId); //Haldor.
		qe.registerQuestNpc(802353).addOnTalkEvent(questId); //Yasan.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804719) { //Haldor.
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
							playQuestMovie(env, 717);
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
