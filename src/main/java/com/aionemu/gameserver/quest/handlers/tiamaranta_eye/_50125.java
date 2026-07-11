package com.aionemu.gameserver.quest.handlers.tiamaranta_eye;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 提亚玛兰塔之眼任务脚本（任务 ID 50125）。
 * Tiamaranta Eye quest script (quest ID 50125).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _50125 extends QuestHandler {

	private final static int questId = 50125;
	public _50125() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerOnKillInWorld(600040000, questId);
		qe.registerQuestNpc(205958).addOnQuestStart(questId); //Marmara.
		qe.registerQuestNpc(205958).addOnAtDistanceEvent(questId); //Marmara.
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
		if (qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 205958) { //Marmara.
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
