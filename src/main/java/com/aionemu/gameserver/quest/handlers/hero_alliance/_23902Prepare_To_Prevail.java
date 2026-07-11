package com.aionemu.gameserver.quest.handlers.hero_alliance;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 英雄联盟任务脚本：Prepare To Prevail（任务 ID 23902）。
 * Hero Alliance quest script: Prepare To Prevail (quest ID 23902).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23902Prepare_To_Prevail extends QuestHandler {

    private final static int questId = 23902;
    public _23902Prepare_To_Prevail() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(798718).addOnQuestStart(questId); //Halia.
		qe.registerQuestNpc(798718).addOnTalkEvent(questId); //Halia.
		qe.registerQuestNpc(799225).addOnTalkEvent(questId); //Richelle.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 798718) { //Halia.
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
				case 799225: { //Richelle.
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
		    if (targetId == 799225) { //Richelle.
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
