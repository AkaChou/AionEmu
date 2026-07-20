package com.aionemu.gameserver.quest.handlers.evergale_canyon;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 永风峡谷任务脚本：Legendary Black Market Trader（任务 ID 13962）。
 * Evergale Canyon quest script: Legendary Black Market Trader (quest ID 13962).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _13962Legendary_Black_Market_Trader extends QuestHandler {

	private final static int questId = 13962;
	private final static int[] npcs = {835218, 835217};
	private static final int[] traders = {835385, 835447, 835474, 835476, 835478, 835480};
    public _13962Legendary_Black_Market_Trader() {
        super(questId);
    }
	
	@Override
	public void register() {
		for (int npc: npcs) {
            qe.registerQuestNpc(npc).addOnTalkEvent(questId);
        }
		qe.registerQuestNpc(835218).addOnQuestStart(questId);
		for (int trader : traders) {
			qe.registerQuestNpc(trader).addOnAtDistanceEvent(questId);
		}
	}
	
	@Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 835218) {
				switch (env.getDialog()) {
                    case START_DIALOG: {
                        return sendQuestDialog(env, 4762);
					} case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE: {
						return sendQuestStartDialog(env);
					} case REFUSE_QUEST_SIMPLE: {
				        return closeDialogWindow(env);
					}
                }
			}
		}
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 835217) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 10002);
				} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean onAtDistanceEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.START) {
			qs.setStatus(QuestStatus.REWARD);
			updateQuestStatus(env);
			return true;
		}
		return false;
	}
}
