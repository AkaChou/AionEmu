package com.aionemu.gameserver.quest.handlers.poeta;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 波伊塔任务脚本：Message To Madeline（任务 ID 1230）。
 * Poeta quest script: Message To Madeline (quest ID 1230).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _1230Message_To_Madeline extends QuestHandler
{
    private final static int questId = 1230;
	
    public _1230Message_To_Madeline() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(801032).addOnQuestStart(questId); //Ellino.
		qe.registerQuestNpc(801033).addOnTalkEvent(questId); //Madeline.
		qe.registerQuestNpc(801033).addOnTalkEvent(questId); //Madeline.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 801032) { //Ellino.
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
				case 801033: { //Madeline.
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
		    if (targetId == 801033) { //Madeline.
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
