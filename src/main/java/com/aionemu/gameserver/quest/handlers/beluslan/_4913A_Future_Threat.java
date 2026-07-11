package com.aionemu.gameserver.quest.handlers.beluslan;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 贝勒斯兰任务脚本：A Future Threat（任务 ID 4913）。
 * Beluslan quest script: A Future Threat (quest ID 4913).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _4913A_Future_Threat extends QuestHandler {

    private final static int questId = 4913;
    public _4913A_Future_Threat() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(204715).addOnQuestStart(questId); 
		qe.registerQuestNpc(204715).addOnTalkEvent(questId); 
		qe.registerQuestNpc(204837).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 204715) {
				switch (dialog) {
					case START_DIALOG: {
						return sendQuestDialog(env, 1011);
					}
					case ASK_ACCEPTION:
                        return sendQuestDialog(env, 4);
					case ACCEPT_QUEST:
						return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 204837: {
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
		    if (targetId == 204837) {
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
