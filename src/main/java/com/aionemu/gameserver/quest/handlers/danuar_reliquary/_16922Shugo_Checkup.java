package com.aionemu.gameserver.quest.handlers.danuar_reliquary;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 达努阿尔圣物库任务脚本：Shugo Checkup（任务 ID 16922）。
 * Danuar Reliquary quest script: Shugo Checkup (quest ID 16922).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _16922Shugo_Checkup extends QuestHandler {

	public static final int questId = 16922;
	public _16922Shugo_Checkup() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(802431).addOnQuestStart(questId);
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
			if (targetId == 802431) {
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
