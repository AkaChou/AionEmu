package com.aionemu.gameserver.quest.handlers.event_quests;

import com.aionemu.gameserver.lifecycle.GameEventServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.EventService;
import com.aionemu.gameserver.services.QuestService;

/**
 * 活动任务脚本：Fortune Opportunities（任务 ID 80230）。
 * Event quest script: Fortune Opportunities (quest ID 80230).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _80230Fortune_Opportunities extends QuestHandler
{
    private final static int questId = 80230;
	
    public _80230Fortune_Opportunities() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerOnLevelUp(questId);
		qe.registerQuestNpc(831148).addOnTalkEvent(questId); //Orung.
	}
	
    @Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (env.getTargetId() == 0) {
			if (env.getDialog() == QuestDialog.ACCEPT_QUEST) {
				QuestService.startEventQuest(env, QuestStatus.START);
				closeDialogWindow(env);
				return true;
			}
		} else if (env.getTargetId() == 831148) { //Orung.
			if (qs != null) {
				if (env.getDialog() == QuestDialog.START_DIALOG && qs.getStatus() == QuestStatus.START) {
					return sendQuestDialog(env, 2375);
				} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
					qs.setQuestVar(1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
	
	@Override
	public boolean onLvlUpEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (!GameEventServices.eventService().checkQuestIsActive(questId) && qs != null)
		QuestService.abandonQuest(player, questId);
		return true;
	}
}
