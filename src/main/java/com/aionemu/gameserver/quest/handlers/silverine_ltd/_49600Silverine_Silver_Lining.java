package com.aionemu.gameserver.quest.handlers.silverine_ltd;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 银线有限公司任务脚本：Silverine Silver Lining（任务 ID 49600）。
 * Silverine Ltd quest script: Silverine Silver Lining (quest ID 49600).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _49600Silverine_Silver_Lining extends QuestHandler {

    private final static int questId = 49600;
    public _49600Silverine_Silver_Lining() {
        super(questId);
    }

	@Override
	public void register() {
		qe.registerQuestNpc(800942).addOnQuestStart(questId); //Dumurinerk.
		qe.registerQuestNpc(800942).addOnTalkEvent(questId); //Dumurinerk.
		qe.registerQuestNpc(800942).addOnTalkEvent(questId); //Dumurinerk.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 800942) { //Dumurinerk.
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
				case 800942: { //Dumurinerk.
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
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 800942) { //Dumurinerk.
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
