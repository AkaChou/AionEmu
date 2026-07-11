package com.aionemu.gameserver.quest.handlers.shugo_imperial_tomb;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 术古帝国陵墓任务脚本：Empires Past（任务 ID 80275）。
 * Shugo Imperial Tomb quest script: Empires Past (quest ID 80275).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _80275Empires_Past extends QuestHandler {

    private final static int questId = 80275;
    public _80275Empires_Past() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(831117).addOnQuestStart(questId); //Indianerk.
		qe.registerQuestNpc(831117).addOnTalkEvent(questId); //Indianerk.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 831117) { //Indianerk.
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
				case 831117: { //Indianerk.
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
			if (targetId == 831117) { //Indianerk.
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
