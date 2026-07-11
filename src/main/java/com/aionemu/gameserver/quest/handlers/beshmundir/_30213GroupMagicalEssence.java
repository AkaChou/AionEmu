package com.aionemu.gameserver.quest.handlers.beshmundir;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 贝希蒙迪尔任务脚本：Group Magical Essence（任务 ID 30213）。
 * Beshmundir quest script: Group Magical Essence (quest ID 30213).
 *
 * @author Gigi
 */
public class _30213GroupMagicalEssence extends QuestHandler {

	private final static int questId = 30213;
	public _30213GroupMagicalEssence() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(798941).addOnQuestStart(questId);
		qe.registerQuestNpc(798941).addOnTalkEvent(questId);
		qe.registerQuestNpc(798926).addOnTalkEvent(questId);
/* 		qe.registerQuestNpc(730275).addOnTalkEvent(questId); */
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 798941) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				}
				else {
					return sendQuestStartDialog(env);
				}
			}
		}
		if (qs == null)
			return false;
/* 		if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 730275: {
					switch (env.getDialog()) {
					    case USE_OBJECT: {
					         return sendQuestDialog(env, 1011);
						}
						case CHECK_COLLECTED_ITEMS: {
						     return checkQuestItems(env, 0, 0, true, 10000, 10001);
						}
					}
				}
			}
		} */
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798926) {
				switch (env.getDialog()) {
					case USE_OBJECT: {
						removeQuestItem(env, 182209614, 1);
						return sendQuestDialog(env, 10002);
					}
					case SELECT_REWARD: {
						return sendQuestDialog(env, 5);
					}
					default:
						return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}
