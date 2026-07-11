package com.aionemu.gameserver.quest.handlers.pernon;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 佩尔农任务脚本：The Many Facets Of Friendship（任务 ID 28828）。
 * Pernon quest script: The Many Facets Of Friendship (quest ID 28828).
 *
 * @author Rolandas, bobobear
 */
public class _28828TheManyFacetsOfFriendship extends QuestHandler {

	private static final int questId = 28828;
	private static final Set<Integer> butlers;

	static {
		butlers = new HashSet<Integer>();
		butlers.add(810022);
		butlers.add(810023);
		butlers.add(810024);
		butlers.add(810025);
		butlers.add(810026);
	}

	public _28828TheManyFacetsOfFriendship() {
		super(questId);
	}

	@Override
	public void register() {
		Iterator<Integer> iter = butlers.iterator();
		while (iter.hasNext()) {
			int butlerId = iter.next();
			qe.registerQuestNpc(butlerId).addOnQuestStart(questId);
			qe.registerQuestNpc(butlerId).addOnTalkEvent(questId);
		}
		qe.registerQuestItem(182213205, questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = env.getTargetId();

		if (!butlers.contains(targetId))
			return false;

		House house = GameHousingServices.housingService().getPlayerStudio(player.getObjectId());
		if (house == null) {
			int address = GameHousingServices.housingService().getPlayerAddress(player.getObjectId());
			GameHousingServices.housingService().getHouseByAddress(address);
		}
		if (house == null || house.getButler() == null || house.getButler().getNpcId() != targetId)
			return false;

		QuestDialog dialog = env.getDialog();
		QuestState qs = player.getQuestStateList().getQuestState(questId);

		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			switch (dialog) {
				case START_DIALOG:
					return sendQuestDialog(env, 1011);
				case ACCEPT_QUEST_SIMPLE:
					return sendQuestStartDialog(env, 182213205, 1);
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			switch (dialog) {
				case USE_OBJECT:
					return sendQuestDialog(env, 2375);
				case SELECT_REWARD:
					removeQuestItem(env, 182213205, 1);
				case SELECT_NO_REWARD:
					sendQuestEndDialog(env);
					return true;
			}
		}
		return false;
	}

	@Override
	public HandlerResult onItemUseEvent(QuestEnv env, Item item) {
		final Player player = env.getPlayer();
		final int id = item.getItemTemplate().getTemplateId();
		if (id == 182213205) {
			QuestState qs = player.getQuestStateList().getQuestState(questId);
			if (qs != null && qs.getStatus() == QuestStatus.START) {
				qs.setQuestVar(1);
				qs.setStatus(QuestStatus.REWARD);
				updateQuestStatus(env);
			}
		}
		return HandlerResult.UNKNOWN;
	}
}
