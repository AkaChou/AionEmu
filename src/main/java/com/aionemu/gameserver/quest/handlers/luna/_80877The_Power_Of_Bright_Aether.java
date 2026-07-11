package com.aionemu.gameserver.quest.handlers.luna;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 露娜任务脚本：The Power Of Bright Aether（任务 ID 80877）。
 * Luna quest script: The Power Of Bright Aether (quest ID 80877).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _80877The_Power_Of_Bright_Aether extends QuestHandler {

	private static final int questId = 80877;
	public _80877The_Power_Of_Bright_Aether() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(834463).addOnQuestStart(questId); //地下医疗军官。 / Underground Medic Officer.
		qe.registerQuestNpc(834463).addOnTalkEvent(questId); //地下医疗军官。 / Underground Medic Officer.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 834463) { //地下医疗军官。 / Underground Medic Officer.
				switch (env.getDialog()) {
					case START_DIALOG: {
						return sendQuestDialog(env, 4762);
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 834463) { //地下医疗军官。 / Underground Medic Officer.
				switch (env.getDialog()) {
					case START_DIALOG: {
                        return sendQuestDialog(env, 1011);
					} case CHECK_COLLECTED_ITEMS: {
						if (QuestService.collectItemCheck(env, true)) {
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							return sendQuestDialog(env, 10000);
						} else {
							return sendQuestDialog(env, 10001);
						}
					}
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 834463) { //地下医疗军官。 / Underground Medic Officer.
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
}
