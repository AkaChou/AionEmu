package com.aionemu.gameserver.quest.handlers.drakenspire_depths;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 龙脊深渊任务脚本：Kismeti Detachment Attachment（任务 ID 28950）。
 * Drakenspire Depths quest script: Kismeti Detachment Attachment (quest ID 28950).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _28950Kismeti_Detachment_Attachment extends QuestHandler {

    private final static int questId = 28950;
    public _28950Kismeti_Detachment_Attachment() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(804738).addOnQuestStart(questId);
		qe.registerQuestNpc(804738).addOnTalkEvent(questId);
		qe.registerQuestNpc(209743).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 804738) {
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
				case 209743: {
					switch (dialog) {
						case START_DIALOG: {
							return sendQuestDialog(env, 10002);
						} case SELECT_REWARD: {
							changeQuestStep(env, 0, 0, true);
							return sendQuestEndDialog(env);
						}
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
		    if (targetId == 209743) {
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
