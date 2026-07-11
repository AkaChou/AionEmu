package com.aionemu.gameserver.quest.handlers.hero_alliance;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 英雄联盟任务脚本：Bracing For Balaurea（任务 ID 13902）。
 * Hero Alliance quest script: Bracing For Balaurea (quest ID 13902).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _13902Bracing_For_Balaurea extends QuestHandler {

    private final static int questId = 13902;
    public _13902Bracing_For_Balaurea() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(798514).addOnQuestStart(questId); //Adrass.
		qe.registerQuestNpc(798514).addOnTalkEvent(questId); //Adrass.
		qe.registerQuestNpc(798926).addOnTalkEvent(questId); //Outremus.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 798514) { //Outremus.
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
				case 798926: { //Outremus.
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
		    if (targetId == 798926) { //Outremus.
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
