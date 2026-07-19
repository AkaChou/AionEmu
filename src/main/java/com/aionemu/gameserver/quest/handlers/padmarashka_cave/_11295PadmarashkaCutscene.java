package com.aionemu.gameserver.quest.handlers.padmarashka_cave;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

public class _11295PadmarashkaCutscene extends QuestHandler {

	static final int WORLD_ID = 320150000;
	static final int MOVIE_ID = 488;
	static final int MIN_LEVEL = 50;
	static final int REQUIRED_ITEM_ID = 182215009;
	static final long REQUIRED_ITEM_COUNT = 2;

	public _11295PadmarashkaCutscene() {
		super(11295);
	}

	@Override
	public void register() {
		qe.registerOnEnterWorld(getQuestId());
		qe.registerOnMovieEndQuest(MOVIE_ID, getQuestId());
	}

	@Override
	public boolean onEnterWorldEvent(QuestEnv env) {
		Player player = env.getPlayer();
		long itemCount = player.getInventory().getItemCountByItemId(REQUIRED_ITEM_ID);
		return canStart(player.getWorldId(), player.getRace(), player.getLevel(), itemCount)
			&& QuestService.startQuest(env);
	}

	@Override
	public boolean onMovieEndEvent(QuestEnv env, int movieId) {
		Player player = env.getPlayer();
		QuestState state = player.getQuestStateList().getQuestState(getQuestId());
		QuestStatus status = state == null ? null : state.getStatus();
		if (!canFinish(player.getWorldId(), player.getRace(), player.getLevel(), movieId, status)) {
			return false;
		}
		state.setStatus(QuestStatus.REWARD);
		return QuestService.finishQuest(env);
	}

	static boolean canStart(int worldId, Race race, int level, long itemCount) {
		return worldId == WORLD_ID && race == Race.ELYOS && level >= MIN_LEVEL && itemCount >= REQUIRED_ITEM_COUNT;
	}

	static boolean canFinish(int worldId, Race race, int level, int movieId, QuestStatus status) {
		return worldId == WORLD_ID && race == Race.ELYOS && level >= MIN_LEVEL && movieId == MOVIE_ID
			&& status == QuestStatus.START;
	}
}
