package com.aionemu.gameserver.quest.handlers.reshanta;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;

/**
 * 雷山塔任务脚本：Victory Against The 6th（任务 ID 4738）。
 * Reshanta quest script: Victory Against The 6th (quest ID 4738).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _4738Victory_Against_The_6th extends QuestHandler {

	private final static int questId = 4738;
	public _4738Victory_Against_The_6th() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(278035).addOnQuestStart(questId); //Konto.
		qe.registerQuestNpc(278035).addOnTalkEvent(questId); //Konto.
		qe.registerOnKillRanked(AbyssRankEnum.GRADE6_SOLDIER, questId);
	}
	
	@Override
    public boolean onKillRankedEvent(QuestEnv env) {
        return defaultOnKillRankedEvent(env, 0, 1, true);
    }
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
		    if (env.getTargetId() == 278035) { //Konto.
			    switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ACCEPT_QUEST_SIMPLE: {
						return sendQuestStartDialog(env);
					}
					case REFUSE_QUEST_SIMPLE: {
				        return closeDialogWindow(env);
					}
				}
			} 
            if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
				if (env.getTargetId() == 278035) { //Konto.
				    if (env.getDialog() == QuestDialog.START_DIALOG) {
						return sendQuestDialog(env, 10002);
					} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
						return sendQuestDialog(env, 5);
					} else {
						return sendQuestEndDialog(env);
					}
				}
			}
		}
		return false;
	}
}
