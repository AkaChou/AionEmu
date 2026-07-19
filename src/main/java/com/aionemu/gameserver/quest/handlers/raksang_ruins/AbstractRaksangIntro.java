package com.aionemu.gameserver.quest.handlers.raksang_ruins;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;

abstract class AbstractRaksangIntro extends QuestHandler {

	static final int WORLD_ID = 300610000;
	static final int MIN_LEVEL = 60;
	private final Race race;
	private final int movieId;

	protected AbstractRaksangIntro(int questId, Race race, int movieId) {
		super(questId);
		this.race = race;
		this.movieId = movieId;
	}

	@Override
	public void register() {
		qe.registerOnEnterWorld(getQuestId());
		qe.registerOnMovieEndQuest(movieId, getQuestId());
	}

	@Override
	public boolean onEnterWorldEvent(QuestEnv env) {
		Player player = env.getPlayer();
		if (player.getWorldId() != WORLD_ID || player.getLevel() < MIN_LEVEL || player.getRace() != race) {
			return false;
		}
		QuestState state = player.getQuestStateList().getQuestState(getQuestId());
		return switch (nextAction(state)) {
			case START_MOVIE -> QuestService.startQuest(env) && playMovie(player);
			case REPLAY_MOVIE -> playMovie(player);
			case NONE -> false;
		};
	}

	@Override
	public boolean onMovieEndEvent(QuestEnv env, int movieId) {
		Player player = env.getPlayer();
		if (movieId != this.movieId || player.getWorldId() != WORLD_ID || player.getLevel() < MIN_LEVEL
			|| player.getRace() != race) {
			return false;
		}
		QuestState state = player.getQuestStateList().getQuestState(getQuestId());
		if (state == null || state.getStatus() != QuestStatus.START && state.getStatus() != QuestStatus.REWARD) {
			return false;
		}
		state.setStatus(QuestStatus.REWARD);
		return QuestService.finishQuest(env);
	}

	private boolean playMovie(Player player) {
		PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(1, movieId));
		return true;
	}

	static Action nextAction(QuestState state) {
		if (state == null || state.getStatus() == QuestStatus.NONE) {
			return Action.START_MOVIE;
		}
		return state.getStatus() == QuestStatus.START || state.getStatus() == QuestStatus.REWARD
			? Action.REPLAY_MOVIE
			: Action.NONE;
	}

	enum Action {
		NONE,
		START_MOVIE,
		REPLAY_MOVIE
	}
}
