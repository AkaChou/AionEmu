package com.aionemu.gameserver.quest.handlers.danuar_reliquary;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 达努阿尔圣物库任务脚本：Another Passage（任务 ID 26922）。
 * Danuar Reliquary quest script: Another Passage (quest ID 26922).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _26922Another_Passage extends QuestHandler {

	public static final int questId = 26922;
	public _26922Another_Passage() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(802433).addOnQuestStart(questId);
		qe.registerQuestNpc(804628).addOnTalkEvent(questId);
		qe.registerQuestNpc(804628).addOnTalkEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 802433) {
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
				case 804628: {
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
		    if (targetId == 804628) {
			    return sendQuestEndDialog(env);
		    }
		}
		return false;
	}
}
