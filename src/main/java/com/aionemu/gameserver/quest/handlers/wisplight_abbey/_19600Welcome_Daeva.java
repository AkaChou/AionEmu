package com.aionemu.gameserver.quest.handlers.wisplight_abbey;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 微光修道院任务脚本：Welcome Daeva（任务 ID 19600）。
 * Wisplight Abbey quest script: Welcome Daeva (quest ID 19600).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _19600Welcome_Daeva extends QuestHandler {

	private final static int questId = 19600;
	public _19600Welcome_Daeva() {
		super(questId);
	}
	
	@Override
	public void register() {
		int[] npcs = {806698};
        for (int npc: npcs) {
            qe.registerQuestNpc(npc).addOnTalkEvent(questId);
        }
		qe.registerQuestNpc(806698).addOnAtDistanceEvent(questId);
	}
	
	@Override
    public boolean onDialogEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
        if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 806698) {
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
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			QuestService.startQuest(env);
			changeQuestStep(env, 0, 1, true);
			return true;
		}
		return false;
	}
}
