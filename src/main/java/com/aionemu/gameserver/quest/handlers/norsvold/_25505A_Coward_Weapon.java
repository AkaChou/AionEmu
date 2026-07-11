package com.aionemu.gameserver.quest.handlers.norsvold;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * 诺斯沃尔德任务脚本：A Coward Weapon（任务 ID 25505）。
 * Norsvold quest script: A Coward Weapon (quest ID 25505).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _25505A_Coward_Weapon extends QuestHandler {

    private final static int questId = 25505;
    public _25505A_Coward_Weapon() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(806102).addOnQuestStart(questId);
        qe.registerQuestNpc(806102).addOnTalkEvent(questId);
		qe.registerQuestNpc(703075).addOnTalkEvent(questId);
    }
	
    @Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 806102) {
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
		} else if (targetId == 703075) {
			if (env.getDialog() == QuestDialog.USE_OBJECT) {
				closeDialogWindow(env);
				return true;
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.START) {
			if (targetId == 806102) {
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
			if (targetId == 806102) {
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
