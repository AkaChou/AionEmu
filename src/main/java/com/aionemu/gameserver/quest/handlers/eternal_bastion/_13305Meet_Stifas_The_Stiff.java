package com.aionemu.gameserver.quest.handlers.eternal_bastion;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 永恒堡垒任务脚本：Meet Stifas The Stiff（任务 ID 13305）。
 * Eternal Bastion quest script: Meet Stifas The Stiff (quest ID 13305).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _13305Meet_Stifas_The_Stiff extends QuestHandler {

	public static final int questId = 13305;
	
	public _13305Meet_Stifas_The_Stiff() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(801281).addOnQuestStart(questId); //Demades.
		qe.registerQuestNpc(801281).addOnTalkEvent(questId); //Demades.
		qe.registerQuestNpc(801281).addOnTalkEvent(questId); //Demades.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 801281) { //Demades.
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
				case 801281: { //Demades.
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
		    if (targetId == 801281) { //Demades.
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
