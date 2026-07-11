package com.aionemu.gameserver.quest.handlers.tiamaranta_eye;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 提亚玛兰塔之眼任务脚本（任务 ID 51125）。
 * Tiamaranta Eye quest script (quest ID 51125).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _51125 extends QuestHandler {

	private final static int questId = 51125;
	public _51125() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerOnKillInWorld(600040000, questId);
		qe.registerQuestNpc(205960).addOnQuestStart(questId); //Grimron.
		qe.registerQuestNpc(205960).addOnAtDistanceEvent(questId); //Grimron.
	}
	
	@Override
    public boolean onKillInWorldEvent(QuestEnv env) {
        return defaultOnKillRankedEvent(env, 0, 20, true);
    }
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 205960) { //Grimron.
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
