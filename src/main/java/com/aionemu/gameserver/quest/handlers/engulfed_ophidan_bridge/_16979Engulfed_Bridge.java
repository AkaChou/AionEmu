package com.aionemu.gameserver.quest.handlers.engulfed_ophidan_bridge;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 淹没的奥菲丹桥任务脚本：Engulfed Bridge（任务 ID 16979）。
 * Engulfed Ophidan Bridge quest script: Engulfed Bridge (quest ID 16979).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _16979Engulfed_Bridge extends QuestHandler {

	public static final int questId = 16979;
	public _16979Engulfed_Bridge() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(802025).addOnQuestStart(questId); //Moireste.
		qe.registerQuestNpc(801762).addOnTalkEvent(questId); //Timarchus.
		qe.registerQuestNpc(801762).addOnTalkEvent(questId); //Timarchus.
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 802025) { //Moireste.
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				} else if (env.getDialog() == QuestDialog.ASK_ACCEPTION) {
                    return sendQuestDialog(env, 4);
				} else if (env.getDialog() == QuestDialog.ACCEPT_QUEST) {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 801762: { //Timarchus.
					switch (env.getDialog()) {
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
		    if (targetId == 801762) { //Timarchus.
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
