package com.aionemu.gameserver.quest.handlers.oriel;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 奥里尔任务脚本：The Living Is Easy（任务 ID 18800）。
 * Oriel quest script: The Living Is Easy (quest ID 18800).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _18800The_Living_Is_Easy extends QuestHandler {

    private final static int questId = 18800;
	
    public _18800The_Living_Is_Easy() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(798458).addOnQuestStart(questId); //Harinus.
		qe.registerQuestNpc(798458).addOnTalkEvent(questId); //Harinus.
		qe.registerQuestNpc(830365).addOnTalkEvent(questId); //Izunius.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 798458) { //Harinus.
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE:
						return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 830365: { //Izunius.
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
		    if (targetId == 830365) { //Izunius.
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
