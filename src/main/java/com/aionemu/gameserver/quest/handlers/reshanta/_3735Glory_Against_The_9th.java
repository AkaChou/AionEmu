package com.aionemu.gameserver.quest.handlers.reshanta;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;

/**
 * 雷山塔任务脚本：Glory Against The 9th（任务 ID 3735）。
 * Reshanta quest script: Glory Against The 9th (quest ID 3735).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _3735Glory_Against_The_9th extends QuestHandler {

	private final static int questId = 3735;
	public _3735Glory_Against_The_9th() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(278535).addOnQuestStart(questId); //Maius.
		qe.registerQuestNpc(278535).addOnTalkEvent(questId); //Maius.
		qe.registerOnKillRanked(AbyssRankEnum.GRADE9_SOLDIER, questId);
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
		    if (env.getTargetId() == 278535) { //Maius.
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
				if (env.getTargetId() == 278535) { //Maius.
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
