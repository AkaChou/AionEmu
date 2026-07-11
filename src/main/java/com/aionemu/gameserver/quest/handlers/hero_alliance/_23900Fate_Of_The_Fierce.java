package com.aionemu.gameserver.quest.handlers.hero_alliance;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 英雄联盟任务脚本：Fate Of The Fierce（任务 ID 23900）。
 * Hero Alliance quest script: Fate Of The Fierce (quest ID 23900).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23900Fate_Of_The_Fierce extends QuestHandler {

    private final static int questId = 23900;
    public _23900Fate_Of_The_Fierce() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(804719).addOnQuestStart(questId);
		qe.registerQuestNpc(804719).addOnTalkEvent(questId); 
		qe.registerQuestNpc(799225).addOnTalkEvent(questId);
		qe.registerQuestNpc(204182).addOnTalkEvent(questId);
		qe.registerQuestNpc(798718).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		} 
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804719) { 
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
				}
			}
		}
        if (qs == null || qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 799225: {
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1352);
						} case SELECT_ACTION_1353: {
							return sendQuestDialog(env, 1353);
						} case STEP_TO_1: {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
				            return closeDialogWindow(env);
						}
					}
				} case 204182: {
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1693);
						} case SELECT_ACTION_1694: {
							return sendQuestDialog(env, 1694);
						} case STEP_TO_2: {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(env);
				            return closeDialogWindow(env);
						}
					}
				} case 798718: {
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2375);
						} case SELECT_REWARD: {
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		} 
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798718) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
