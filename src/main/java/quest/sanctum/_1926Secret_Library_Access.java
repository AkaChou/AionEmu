/*
 * =====================================================================================*
 * This file is part of Aion-Unique (Aion-Unique Home Software Development)             *
 * Aion-Unique Development is a closed Aion Project that use Old Aion Project Base      *
 * Like Aion-Lightning, Aion-Engine, Aion-Core, Aion-Extreme, Aion-NextGen, ArchSoft,   *
 * Aion-Ger, U3J, Encom And other Aion project, All Credit Content                      *
 * That they make is belong to them/Copyright is belong to them. And All new Content    *
 * that Aion-Unique make the copyright is belong to Aion-Unique                         *
 * You may have agreement with Aion-Unique Development, before use this Engine/Source   *
 * You have agree with all of Term of Services agreement with Aion-Unique Development   *
 * =====================================================================================*
 */
package quest.sanctum;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * @author Ghostfur, Unknown (Aion-Unique)
 */
public class _1926Secret_Library_Access extends QuestHandler {

	private static final int questId = 1926;

	public _1926Secret_Library_Access() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203701).addOnTalkEvent(questId); // Lavirintos
		qe.registerQuestNpc(203894).addOnTalkEvent(questId); // Latri
		qe.registerOnLevelUp(questId);
	}

	@Override
	public boolean onLvlUpEvent(QuestEnv env) {
		return defaultOnLvlUpEvent(env);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		
		if (qs == null) {
			return false;
		}

		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		}

		if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 203701) { // Lavirintos
				switch (env.getDialog()) {
					case START_DIALOG:
						return sendQuestDialog(env, 1011);
					case SELECT_ACTION_1012:
						return sendQuestDialog(env, 1012);
					case SET_REWARD:
						giveQuestItem(env, 182206022, 1);
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return closeDialogWindow(env);
					default:
						break;
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203894) { // Latri
				switch (env.getDialog()) {
					case START_DIALOG:
						return sendQuestDialog(env, 10002);
					case SELECT_REWARD:
						removeQuestItem(env, 182206022, 1);
						return sendQuestDialog(env, 5);
					default:
						return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
}