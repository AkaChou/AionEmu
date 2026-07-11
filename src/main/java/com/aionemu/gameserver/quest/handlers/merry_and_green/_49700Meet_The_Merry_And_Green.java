package com.aionemu.gameserver.quest.handlers.merry_and_green;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 欢乐绿意任务脚本：Meet The Merry And Green（任务 ID 49700）。
 * Merry and Green quest script: Meet The Merry And Green (quest ID 49700).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _49700Meet_The_Merry_And_Green extends QuestHandler {

    private final static int questId = 49700;
	
    public _49700Meet_The_Merry_And_Green() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(800948).addOnQuestStart(questId); //Sununnerk.
		qe.registerQuestNpc(800948).addOnTalkEvent(questId); //Sununnerk.
		qe.registerQuestNpc(800948).addOnTalkEvent(questId); //Sununnerk.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 800948) { //Sununnerk.
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
				case 800948: { //Sununnerk.
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
		    if (targetId == 800948) { //Sununnerk.
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
