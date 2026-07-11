package com.aionemu.gameserver.quest.handlers.tiamat_stronghold;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 提亚马特要塞任务脚本：The Finer Things（任务 ID 30717）。
 * Tiamat Stronghold quest script: The Finer Things (quest ID 30717).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _30717The_Finer_Things extends QuestHandler {

    private final static int questId = 30717;
    public _30717The_Finer_Things() {
        super(questId);
    }

	@Override
	public void register() {
		qe.registerQuestNpc(800460).addOnQuestStart(questId);
		qe.registerQuestNpc(800460).addOnTalkEvent(questId);
		qe.registerQuestNpc(800460).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 800460) {
				switch (dialog) {
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
			switch (targetId) {
				case 800460: {
					switch (dialog) {
						case START_DIALOG: {
							return sendQuestDialog(env, 2375);
						} case SELECT_REWARD: {
							changeQuestStep(env, 0, 0, true);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 800460) {
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
