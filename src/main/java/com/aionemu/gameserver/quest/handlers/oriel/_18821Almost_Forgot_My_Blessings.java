package com.aionemu.gameserver.quest.handlers.oriel;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 奥里尔任务脚本：Almost Forgot My Blessings（任务 ID 18821）。
 * Oriel quest script: Almost Forgot My Blessings (quest ID 18821).
 *
 * @author Ghostfur & Unknown (Aion-Unique)
 */
public class _18821Almost_Forgot_My_Blessings extends QuestHandler
{
	private static final int questId = 18821;
	private static final Set<Integer> butlersElyos;
	
	public _18821Almost_Forgot_My_Blessings() {
		super(questId);
	}
	
	static {
		butlersElyos = new HashSet<Integer>();
		butlersElyos.add(810017);
		butlersElyos.add(810018);
		butlersElyos.add(810019);
		butlersElyos.add(810020);
		butlersElyos.add(810021);
	}
	
	@Override
	public void register() {
		Iterator<Integer> iter = butlersElyos.iterator();
		while (iter.hasNext()) {
			int butlerId = iter.next();
			qe.registerQuestNpc(butlerId).addOnQuestStart(questId);
			qe.registerQuestNpc(butlerId).addOnTalkEvent(questId);
		}
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = env.getTargetId();
		if (!butlersElyos.contains(targetId))
			return false;
		House house = player.getActiveHouse();
		if (house == null || house.getButler() == null || house.getButler().getNpcId() != targetId)
			return false;
		QuestDialog dialog = env.getDialog();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			switch (dialog) {
				case START_DIALOG:
					return sendQuestDialog(env, 1011);
				case ACCEPT_QUEST:
					return sendQuestStartDialog(env);
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			switch (dialog) {
				case START_DIALOG:
					return sendQuestDialog(env, 2375);
				case SELECT_REWARD:
					changeQuestStep(env, 0, 0, true);
					return sendQuestDialog(env, 5);
				case SELECT_NO_REWARD:
					return sendQuestEndDialog(env);
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			switch (dialog) {
				case USE_OBJECT:
					return sendQuestDialog(env, 5);
				case SELECT_NO_REWARD:
					sendQuestEndDialog(env);
					return true;
			}
		}
		return false;
	}
}
