package com.aionemu.gameserver.quest.handlers.update_intro;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;

public class _30810Ereshkigals_Resurrection extends QuestHandler {

	static final int MOVIE_ID = 36;
	private static final int MIN_LEVEL = 10;

	public _30810Ereshkigals_Resurrection() {
		super(30810);
	}

	@Override
	public void register() {
		qe.registerOnLevelUp(getQuestId());
		qe.registerOnEnterWorld(getQuestId());
		qe.registerOnMovieEndQuest(MOVIE_ID, getQuestId());
	}

	@Override
	public boolean onLvlUpEvent(QuestEnv env) {
		return startOrRecover(env);
	}

	@Override
	public boolean onEnterWorldEvent(QuestEnv env) {
		return startOrRecover(env);
	}

	@Override
	public boolean onMovieEndEvent(QuestEnv env, int movieId) {
		return movieId == MOVIE_ID && finish(env);
	}

	private boolean startOrRecover(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState state = player.getQuestStateList().getQuestState(getQuestId());
		return switch (nextAction(player.getLevel(), state)) {
			case START_MOVIE -> QuestService.startQuest(env) && playMovie(player);
			case FINISH -> finish(env);
			case NONE -> false;
		};
	}

	private boolean playMovie(Player player) {
		PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(1, MOVIE_ID));
		return true;
	}

	private boolean finish(QuestEnv env) {
		QuestState state = env.getPlayer().getQuestStateList().getQuestState(getQuestId());
		if (state == null || state.getStatus() != QuestStatus.START && state.getStatus() != QuestStatus.REWARD) {
			return false;
		}
		state.setStatus(QuestStatus.REWARD);
		return QuestService.finishQuest(env);
	}

	static Action nextAction(int level, QuestState state) {
		if (level < MIN_LEVEL || state != null && state.getStatus() != QuestStatus.NONE
			&& state.getStatus() != QuestStatus.START && state.getStatus() != QuestStatus.REWARD) {
			return Action.NONE;
		}
		return state == null || state.getStatus() == QuestStatus.NONE ? Action.START_MOVIE : Action.FINISH;
	}

	enum Action {
		NONE,
		START_MOVIE,
		FINISH
	}
}
