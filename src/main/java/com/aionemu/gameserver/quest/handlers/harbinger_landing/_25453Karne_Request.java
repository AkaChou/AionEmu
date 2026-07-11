package com.aionemu.gameserver.quest.handlers.harbinger_landing;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 先驱者登陆点任务脚本：Karne Request（任务 ID 25453）。
 * Harbinger Landing quest script: Karne Request (quest ID 25453).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25453Karne_Request extends QuestHandler {

    private final static int questId = 25453;
    public _25453Karne_Request() {
        super(questId);
    }
	
    @Override
    public void register() {
        qe.registerOnKillInWorld(400010000, questId);
		qe.registerQuestNpc(805420).addOnTalkEvent(questId); //Karne.
		qe.registerQuestNpc(805420).addOnAtDistanceEvent(questId); //Karne.
    }
	
	@Override
    public boolean onKillInWorldEvent(QuestEnv env) {
        return defaultOnKillRankedEvent(env, 0, 2, true);
    }
	
    @Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 805420) { //Karne.
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
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			QuestService.startQuest(env);
			return true;
		}
		return false;
	}
}
