package com.aionemu.gameserver.quest.handlers.pernon;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 佩尔农任务脚本：Give My Regards To Pernon（任务 ID 28800）。
 * Pernon quest script: Give My Regards To Pernon (quest ID 28800).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _28800Give_My_Regards_To_Pernon extends QuestHandler {

    private final static int questId = 28800;
    public _28800Give_My_Regards_To_Pernon() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(798459).addOnQuestStart(questId); //Randiten.
		qe.registerQuestNpc(798459).addOnTalkEvent(questId); //Randiten.
		qe.registerQuestNpc(830532).addOnTalkEvent(questId); //Hariton.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 798459) { //Randiten.
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
				case 830532: { //Hariton.
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
		    if (targetId == 830532) { //Hariton.
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
