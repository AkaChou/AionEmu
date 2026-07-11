package com.aionemu.gameserver.quest.handlers.shugo_imperial_tomb;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 术古帝国陵墓任务脚本：The Same Thing We Do（任务 ID 80277）。
 * Shugo Imperial Tomb quest script: The Same Thing We Do (quest ID 80277).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _80277The_Same_Thing_We_Do extends QuestHandler {

    private final static int questId = 80277;
    public _80277The_Same_Thing_We_Do() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(831131).addOnQuestStart(questId); //Alberto Einshudison.
		qe.registerQuestNpc(831131).addOnTalkEvent(questId); //Alberto Einshudison.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 831131) { //Alberto Einshudison.
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} if (qs == null)
			return false;
		else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			switch (targetId) {
				case 831131: { //Alberto Einshudison.
					switch (dialog) {
						case START_DIALOG: {
							if (var == 0)
								return sendQuestDialog(env, 2375);
						} case SELECT_REWARD: {
							changeQuestStep(env, 0, 0, true);
							return sendQuestDialog(env, 5);
						} case FINISH_DIALOG: {
							return sendQuestSelectionDialog(env);
						}
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 831131) { //Alberto Einshudison.
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
