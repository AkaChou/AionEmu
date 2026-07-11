package com.aionemu.gameserver.quest.handlers.eternal_bastion;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 永恒堡垒任务脚本：A Fierce Struggle（任务 ID 23305）。
 * Eternal Bastion quest script: A Fierce Struggle (quest ID 23305).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _23305A_Fierce_Struggle extends QuestHandler {

	public static final int questId = 23305;
	
	public _23305A_Fierce_Struggle() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(801280).addOnQuestStart(questId); //Lundvarr.
		qe.registerQuestNpc(801280).addOnTalkEvent(questId); //Lundvarr.
		qe.registerQuestNpc(801280).addOnTalkEvent(questId); //Lundvarr.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 801280) { //Lundvarr.
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
				case 801280: { //Lundvarr.
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
		    if (targetId == 801280) { //Lundvarr.
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
