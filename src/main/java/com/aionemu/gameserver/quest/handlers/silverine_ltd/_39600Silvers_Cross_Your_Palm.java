package com.aionemu.gameserver.quest.handlers.silverine_ltd;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 银线有限公司任务脚本：Silvers Cross Your Palm（任务 ID 39600）。
 * Silverine Ltd quest script: Silvers Cross Your Palm (quest ID 39600).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _39600Silvers_Cross_Your_Palm extends QuestHandler {

    private final static int questId = 39600;
    public _39600Silvers_Cross_Your_Palm() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(800939).addOnQuestStart(questId); //Danurinerk.
		qe.registerQuestNpc(800939).addOnTalkEvent(questId); //Danurinerk.
		qe.registerQuestNpc(800939).addOnTalkEvent(questId); //Danurinerk.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 800939) { //Danurinerk.
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
				case 800939: { //Danurinerk.
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
		    if (targetId == 800939) { //Danurinerk.
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
