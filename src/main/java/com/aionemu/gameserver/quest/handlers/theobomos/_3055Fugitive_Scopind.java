package com.aionemu.gameserver.quest.handlers.theobomos;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 西奥博莫斯任务脚本：Fugitive Scopind（任务 ID 3055）。
 * Theobomos quest script: Fugitive Scopind (quest ID 3055).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _3055Fugitive_Scopind extends QuestHandler {

	private final static int questId = 3055;
	public _3055Fugitive_Scopind() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(730146).addOnQuestStart(questId);
		qe.registerQuestNpc(730146).addOnTalkEvent(questId);
		qe.registerQuestNpc(798195).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		int targetId = 0;
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		} if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 730146) {
				switch (env.getDialog()) {
					case USE_OBJECT: {
						return sendQuestDialog(env, 4762);
					} case STEP_TO_1: {
						QuestService.startQuest(env);
						return closeDialogWindow(env);
					} default:
						return sendQuestStartDialog(env);
				}
			}
		} if (qs == null) {
			return false;
		} if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 798195: {
					switch (env.getDialog()) {
						case START_DIALOG: {
							return sendQuestDialog(env, 1011);
						}
                        case CHECK_COLLECTED_ITEMS: {
                            return checkQuestItems(env, 0, 1, true, 5, 10001);
                        } 
                    }
				}
			}
        }
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798195)
			return sendQuestEndDialog(env);
		}
		return false;
	}
}
