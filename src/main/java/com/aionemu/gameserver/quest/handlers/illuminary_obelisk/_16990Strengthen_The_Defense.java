package com.aionemu.gameserver.quest.handlers.illuminary_obelisk;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 光明方尖碑任务脚本：Strengthen The Defense（任务 ID 16990）。
 * Illuminary Obelisk quest script: Strengthen The Defense (quest ID 16990).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _16990Strengthen_The_Defense extends QuestHandler {

    private final static int questId = 16990;
    public _16990Strengthen_The_Defense() {
        super(questId);
    }
	
	@Override
	public void register() {
		qe.registerQuestNpc(802048).addOnQuestStart(questId); //Tolanda.
		qe.registerQuestNpc(802048).addOnTalkEvent(questId); //Tolanda.
		qe.registerQuestNpc(802048).addOnTalkEvent(questId); //Tolanda.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 802048) { //Tolanda.
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
				case 802048: { //Tolanda.
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
		    if (targetId == 802048) { //Tolanda.
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
