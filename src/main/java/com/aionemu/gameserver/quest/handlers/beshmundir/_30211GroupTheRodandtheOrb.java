package com.aionemu.gameserver.quest.handlers.beshmundir;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 贝希蒙迪尔任务脚本：Group The Rodandthe Orb（任务 ID 30211）。
 * Beshmundir quest script: Group The Rodandthe Orb (quest ID 30211).
 *
 * @author Gigi
 */
public class _30211GroupTheRodandtheOrb extends QuestHandler {

	private final static int questId = 30211;
	public _30211GroupTheRodandtheOrb() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(798941).addOnQuestStart(questId);
		qe.registerQuestNpc(798941).addOnTalkEvent(questId);
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
						     return checkQuestItems(env, 0, 1, true, 10000, 10001);
						}
					}
				}
			}
		} */
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798941) {
				switch (env.getDialog()) {
					case USE_OBJECT: {
						removeQuestItem(env, 182209617, 1);
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
